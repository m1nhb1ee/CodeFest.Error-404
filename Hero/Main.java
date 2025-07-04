package Hero;

import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.support_items.SupportItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "181626";
    private static final String PLAYER_NAME = "b1e";
    private static final String SECRET_KEY = "sk-TN9xLbiuTbyZXILhvyWJbw:skQHC0vsqGEWmjjNlB_mLLiRl1z-BUJf_OjRgcRWtGoWpxTBp9hvQ-0qqmD3BZCppSfa8wHyysKVkG5j06qwzQ";

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
        	
        	System.out.println("\n=== Processing game update == =");
        	
            GameMap gameMap = hero.getGameMap();
            gameMap.updateOnUpdateMap(args[0]);
            Player player = gameMap.getCurrentPlayer();
            Inventory inventory = hero.getInventory();
            
            updateState(player, inventory);
            
            System.out.println("=== Current state: " + state + " ===");
            
            if (!inventory.getListSupportItem().isEmpty()) {
            	for (SupportItem spItem : inventory.getListSupportItem()) {
            		System.out.println("SPItem" + spItem);
            	}
            }
            
            Node chest = Resource.findChest(gameMap, player);
            if (chest != null) {
	            double distance = PathUtils.distance(player, chest);
	            System.out.println("Checking resoure ");
	            if (distance <= 3) searchChest(gameMap, player, inventory);
            }
            
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

    private void updateState(Player player, Inventory inventory) {

        if (player.getHealth() <= 0) {
            state = "WEAPON_SEARCH";
            System.out.println("Hero died, switching to WEAPON_SEARCH");

        }
        

        if (player.getHealth() < Config.HP_DANGER_THRESHOLD) {
            state = "HEALING";
            System.out.println("HP critical, switching to HEALING");

        }

        if (player.getHealth() <= Config.HP_MEDIUM_THRESHOLD && !inventory.getListSupportItem().isEmpty()) {
            try {
                SupportItem healing = inventory.getListSupportItem().get(0);
                hero.useItem(healing.getId());
                System.out.println("Used healing item: " + healing.getId());
            } catch (IOException e) {
                System.err.println("Error using healing item: " + e.getMessage());
            }

        }

        System.out.println("State remains: " + state);
    }

    private void searchWeapon(GameMap gameMap, Player player, Inventory inventory) throws IOException {
    	
        Weapon weapon = Resource.findWeapon(gameMap, player, inventory);
    	
        if (Combat.hasWeapon(inventory)) {
            state = "COMBAT";
            return;
        }
        
        if (weapon == null) System.out.print("no weapon found");
        else if (moveToTarget(gameMap, player, weapon, "weapon")) return;

        searchChest(gameMap, player, inventory);
    }

    private void searchCombat(GameMap gameMap, Player player, Inventory inventory) throws IOException {
    	
    	Combat.CombatTarget enemy = Combat.findBestTarget(gameMap, player, inventory);
    	
        if (enemy == null) {
            state = "RESOURCE_GATHER";
            System.out.println("no enemy");
            return;
        }

    	double hitsToKill = enemy.target.getHealth() / inventory.getMelee().getDamage() ;
    	double timeToKill = hitsToKill * inventory.getMelee().getCooldown();
    	
    	if (timeToKill > 2.0) {
	    	Weapon weapon = Resource.findWeapon(gameMap, player, inventory);
	    	if (weapon != null) {
		        if (Combat.needsBetterWeapon(inventory)) {
		            state = "WEAPON_SEARCH";
		            System.out.println("no weapon");
		            return;
		        }
	    	}
    	}

        double distance = PathUtils.distance(player, enemy.target);
        System.out.println("Target: " + enemy.target.getID() + " | Distance: " + distance);

        if (Combat.inAttackRange(player, enemy.target, inventory))
        	System.out.println("In atk range " + enemy.target +" "+ distance);
        	if (tryAttack(player, enemy.target, inventory, distance, gameMap)) {
	            return;
        }

        String path = PathUtils.getShortestPath(gameMap, Navigator.getObstacles(gameMap), player, enemy.target, true);
        if (path != null && !path.isEmpty()) {
            hero.move(path);
            System.out.println("Moving to enemy: " + path);
        } else {
        	System.out.println("Cannot reach enemy, switching to resource gathering");
            state = "RESOURCE_GATHER";
        }
    }

    private void searchResources(GameMap gameMap, Player player, Inventory inventory) throws IOException {



        if (player.getHealth() < Config.HP_DANGER_THRESHOLD) {
        	
        	SupportItem healing = Resource.findHealing(gameMap, player);
            if (healing != null && moveToTarget(gameMap, player, healing, "healing")) return;
            
            state = "HEALING";
            return;
        }
        
        if (Combat.needsBetterWeapon(inventory)) {
            Weapon weapon = Resource.findWeapon(gameMap, player, inventory);
            System.out.println("Aim to " + weapon);
            if (weapon != null && moveToTarget(gameMap, player, weapon, "better weapon")) return;
            else searchChest(gameMap, player, inventory);
        }
        System.out.println("Cant reach any Weapon, switch to Combat");
        state = "COMBAT";
    }

    private void searchHealing(GameMap gameMap, Player player, Inventory inventory) throws IOException {
    	
    	if (player.getHealth() > Config.HP_MEDIUM_THRESHOLD) {
        	state = "COMBAT";
        	return;
        }
    	
    	if (!inventory.getListSupportItem().isEmpty()) { 
    		
    		System.out.println("HP low, try to use" + inventory.getListSupportItem().get(0).getId());
    		hero.useItem(inventory.getListSupportItem().get(0).getId());
    	
    	}
    	
        SupportItem healing = Resource.findHealing(gameMap, player);
        System.out.print("Healing found : " + healing);
        if (healing == null)	searchChest(gameMap, player, inventory);
        else if (moveToTarget(gameMap, player, healing, "healing")) return;

        state = "RESOURCE_GATHER";
    }

    private void searchChest(GameMap gameMap, Player player, Inventory inventory) throws IOException {
        Node chest = Resource.findChest(gameMap, player);
        if (chest == null) {
        	state = "Combat";
        	return;
        }
        
        // attack
        double distance = PathUtils.distance(player, chest);
        String direction = Navigator.getDirection(player, chest);

        if (Combat.inAttackRange(player, chest, inventory) && tryAttack(player, chest, inventory, distance, gameMap)) {
            return;
        }

        // move to
        String path = PathUtils.getShortestPath(gameMap, Navigator.getObstacles(gameMap), player, chest, true);
        if (path != null && distance > 1) {
            hero.move(path);
            System.out.println("Moving to chest: " + path);
        } else {
            hero.attack(direction);
        }
    }



    private boolean tryAttack(Player player, Node target, Inventory inventory, double distance, GameMap gameMap) throws IOException {
        String direction = Navigator.getDirection(player, target);
        String action = Combat.selectWeaponAction(inventory);
        
        switch (action) {
            case "shoot" -> {
            	int[] attackRange =  inventory.getGun().getRange();
            	System.out.print("Atk Range " + attackRange[0] + " " + attackRange[1] + " ");
                if (inventory.getGun() != null &&  Navigator.checkObstacles(player, target, gameMap, inventory)){
                    hero.shoot(direction);
                    System.out.println("Shooting " + direction);
                    return true;
                }
                System.out.print("cant attack " + direction);
            }
            case "throw" -> {
            	
            	int[] attackRange =  inventory.getThrowable().getRange();
            	System.out.print("Atk Range " + attackRange[0] + " " + attackRange[1] + " ");
                if (inventory.getThrowable() != null) {
                	
                	distance = PathUtils.distance(player, target);
                	
                    hero.throwItem(direction);
                    hero.move(Navigator.getDodgeDirection(player, target, gameMap));
                    System.out.println("Throwing " + direction);
                    return true;
                }
            }
            case "special" -> {

            	int[] attackRange =  inventory.getSpecial().getRange();
            	System.out.print("Atk Range " + attackRange[0] + " " + attackRange[1] + " ");
                if (inventory.getSpecial() != null) {
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
                    
                    System.out.println("State "+state+" Attacking " + direction);
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean tryAttack(Player player, Player target, Inventory inventory, double distance, GameMap gameMap) throws IOException {
        String direction = Navigator.getDirection(player, target);
        String action = Combat.selectWeaponAction(inventory, target);
        double hp = target.getHealth();
        
        switch (action) {
            case "shoot" -> {
            	int[] attackRange =  inventory.getGun().getRange();
            	System.out.print("Atk Range " + attackRange[0] + " " + attackRange[1] + " ");
                if (inventory.getGun() != null &&  Navigator.checkObstacles(player, target, gameMap, inventory)){
                    hero.shoot(direction);
                    System.out.println("Shooting " + direction);
                    if (hp == target.getHealth()) action = "attack";
                    else return true;
                }
                System.out.print("cant attack " + direction);
            }
            case "throw" -> {
            	
            	int[] attackRange =  inventory.getThrowable().getRange();
            	System.out.print("Atk Range " + attackRange[0] + " " + attackRange[1] + " ");
                if (inventory.getThrowable() != null) {
                	
                	distance = PathUtils.distance(player, target);
                	
                    hero.throwItem(direction);
                    hero.move(Navigator.getDodgeDirection(player, target, gameMap));
                    System.out.println("Throwing " + direction);
                    return true;
                }
            }
            case "special" -> {

            	int[] attackRange =  inventory.getSpecial().getRange();
            	System.out.print("Atk Range " + attackRange[0] + " " + attackRange[1] + " ");
                if (inventory.getSpecial() != null) {
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
                    System.out.println("State "+state+" Attacking " + direction);
                    return true;
                }
            }
        }
        return false;
    }
    
    private boolean moveToTarget(GameMap gameMap, Player player, Node target, String targetType) throws IOException {
        String path = PathUtils.getShortestPath(gameMap, Navigator.getObstacles(gameMap), player, target, true);
        if (path == null) return false;

        if (path.isEmpty()) {
            hero.pickupItem();
            System.out.println("Picked up " + targetType + target);
        } else {
        	hero.move(path);
        	System.out.println("Moving to " + targetType + ": " + path);
        	try {
            hero.pickupItem();
        	} catch(Exception e) {
        		System.out.println("Nothing to pick up " + targetType + " at " + path);
        	}
        }
        return true;
    }

}