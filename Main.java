package Hero;

import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.healing_items.HealingItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "120669";
    private static final String PLAYER_NAME = "b1e";
    private static final String SECRET_KEY = "sk-lm7r1q-4SY2kMbP5IM6Iig:jYNA5bwJze6Vrce3KPnRwW8aDd84TZa391PwaBS64NA3Pj025Z9ohZaMcJjnMSsFXpHUV10i5SKrgkWvSgmUpA";

    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME, SECRET_KEY);
        hero.setOnMapUpdate(new MapUpdateListener(hero));
        hero.start(SERVER_URL);
    }
}

class MapUpdateListener implements Emitter.Listener {
    private final Hero hero;
    private String state = "WEAPON_SEARCH";

    public MapUpdateListener(Hero hero) {
        this.hero = hero;
    }

    @Override
    public void call(Object... args) {
        try {
        	
        	System.out.println("\n=== Processing game update ===");
        	
            GameMap gameMap = hero.getGameMap();
            gameMap.updateOnUpdateMap(args[0]);
            Player player = gameMap.getCurrentPlayer();
            Inventory inventory = hero.getInventory();
            
            System.out.println("=== Current state: " + state + " ===");
            
            if (player.getHealth() == 0) state = "WEAPON_SEARCH";
            else if (player.getHealth() <= Config.HP_DANGER_THRESHOLD) searchHealing(gameMap, player, inventory);
            
            Node chest = Resource.findNearestChest(gameMap, player);
            double distance = PathUtils.distance(player, chest);
            
            if (distance <= 3) searchChest(gameMap, player, inventory);
            
            if(Resource.gatherResources(gameMap, player, inventory, hero)) return;
            
            System.out.println("HP: " + player.getHealth() + " | State: " + state);

            switch (state) {
                case "WEAPON_SEARCH" -> searchWeapon(gameMap, player, inventory);
                case "COMBAT" -> searchCombat(gameMap, player, inventory);
                case "RESOURCE_GATHER" -> searchResources(gameMap, player, inventory);
                case "HEALING" -> searchHealing(gameMap, player, inventory);
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void searchWeapon(GameMap gameMap, Player player, Inventory inventory) throws IOException {
        if (Combat.hasWeapon(inventory)) {
            state = "COMBAT";
            return;
        }

        Weapon weapon = Resource.findWeapon(gameMap, player);
        if (weapon == null) System.out.print("no weapon found");
        else if (moveToTarget(gameMap, player, weapon, "weapon")) return;

        searchChest(gameMap, player, inventory);
    }

    private void searchCombat(GameMap gameMap, Player player, Inventory inventory) throws IOException {
    	
    	Weapon weapon = Resource.findWeapon(gameMap, player);
    	if (weapon != null) {
	        if (!Combat.hasWeapon(inventory)) {
	            state = "WEAPON_SEARCH";
	            System.out.println("no weapon");
	            return;
	        }
    	}

        Combat.CombatTarget enemy = Combat.findBestTarget(gameMap, player, inventory);
        if (enemy == null) {
            state = "RESOURCE_GATHER";
            System.out.println("no enemy");
            return;
        }

        double distance = PathUtils.distance(player, enemy.target);
        System.out.println("Target: " + enemy.target.getID() + " | Distance: " + distance);

        if (Navigator.checkDirection(player, enemy.target, gameMap) && tryAttack(player, enemy.target, inventory, distance, gameMap)) {
            return;
        }

        String path = PathUtils.getShortestPath(gameMap, Navigator.getObstacles(gameMap), player, enemy.target, false);
        if (path != null && !path.isEmpty()) {
            hero.move(path);
            System.out.println("Moving to enemy: " + path);
        } else {
        	System.out.println("Cannot reach enemy, switching to resource gathering");
            state = "RESOURCE_GATHER";
        }
    }

    private void searchResources(GameMap gameMap, Player player, Inventory inventory) throws IOException {

        if (Combat.needsBetterWeapon(inventory)) {
            Weapon weapon = Resource.findWeapon(gameMap, player);
            if (weapon != null && moveToTarget(gameMap, player, weapon, "better weapon")) return;
        }

        if (player.getHealth() < Config.HP_DANGER_THRESHOLD) {
        	
            HealingItem healing = Resource.findHealing(gameMap, player);
            if (healing != null && moveToTarget(gameMap, player, healing, "healing")) return;
            
            state = "HEALING";
            return;
        }
        
        state = "COMBAT";
    }

    private void searchHealing(GameMap gameMap, Player player, Inventory inventory) throws IOException {
    	
    	if (player.getHealth() > Config.HP_MEDIUM_THRESHOLD) {
        	state = "COMBAT";
        	return;
        }
    	
    	if (!inventory.getListHealingItem().isEmpty()) hero.useItem(inventory.getListHealingItem().get(0).getId());
    	
        HealingItem healing = Resource.findHealing(gameMap, player);
        if (healing == null)	searchChest(gameMap, player, inventory);
        else if (moveToTarget(gameMap, player, healing, "healing")) return;

        state = "RESOURCE_GATHER";
    }

    private void searchChest(GameMap gameMap, Player player, Inventory inventory) throws IOException {
        Node chest = Resource.findNearestChest(gameMap, player);
        if (chest == null) return;
        // attack
        double distance = PathUtils.distance(player, chest);
        String direction = Navigator.getDirection(player, chest);

        if (Navigator.checkDirection(player, chest, gameMap) && tryAttack(player, chest, inventory, distance, gameMap)) {
            return;
        }

        // move to
        String path = PathUtils.getShortestPath(gameMap, Navigator.getObstacles(gameMap), player, chest, false);
        if (path != null && distance > 1) {
            hero.move(path);
            hero.pickupItem();
            System.out.println("Moving to chest: " + path);
        } else {
            hero.attack(direction);
            hero.pickupItem();
        }
    }



    private boolean tryAttack(Player player, Node target, Inventory inventory, double distance, GameMap gameMap) throws IOException {
        String direction = Navigator.getDirection(player, target);
        String action = Combat.selectWeaponAction(inventory);

        
        switch (action) {
            case "shoot" -> {
                if (inventory.getGun() != null && distance <= inventory.getGun().getRange()) {
                    hero.shoot(direction);
                    hero.move(Navigator.getDodgeDirection(player, target, gameMap));
                    System.out.println("Shooting " + direction);
                    return true;
                }
            }
            case "throw" -> {
                if (inventory.getThrowable() != null && distance <= inventory.getThrowable().getRange()) {
                	
                	distance = PathUtils.distance(player, target);
                	
                    hero.throwItem(direction, (int)distance);
                    hero.move(Navigator.getDodgeDirection(player, target, gameMap));
                    System.out.println("Throwing " + direction);
                    return true;
                }
            }
            case "special" -> {
                if (inventory.getSpecial() != null && distance <= inventory.getSpecial().getRange()) {
                    hero.useSpecial(direction);
                    if (distance <= 1) hero.attack(direction);
                    hero.move(Navigator.getDodgeDirection(player, target, gameMap));
                    System.out.println("Using special " + direction);
                    return true;
                }
            }
            case "attack" -> {
                if (distance <= 1) {
                    hero.attack(direction);
                    System.out.println("Attacking " + direction);
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean moveToTarget(GameMap gameMap, Player player, Node target, String targetType) throws IOException {
        String path = PathUtils.getShortestPath(gameMap, Navigator.getObstacles(gameMap), player, target, false);
        if (path == null) return false;

        if (path.isEmpty()) {
            hero.pickupItem();
            System.out.println("Picked up " + targetType);
        } else {
            hero.move(path);
            System.out.println("Moving to " + targetType + ": " + path);
        }
        return true;
    }

}