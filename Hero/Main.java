package lastSrc;

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
import java.util.Arrays;
import java.util.List;

public class Main {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "167353";
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
    private String previousState = "";
    private List<String> lastPath = new ArrayList<>(Arrays.asList("",""));
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
            
            updateState(gameMap, player, inventory);
            
            System.out.println("=== Current state: " + state + " ===");

            if (handleLooting(gameMap, player, inventory)) return;

            
            System.out.println("HP: " + player.getHealth() + " | State: " + state);

            switch (state) {
                case "WEAPON_SEARCH" -> searchWeapon(gameMap, player, inventory);
                case "COMBAT" -> searchCombat(gameMap, player, inventory);
                case "RESOURCE_GATHER" -> searchResources(gameMap, player, inventory);
                case "HEALING" -> searchHealing(gameMap, player, inventory);
                default -> {
                    System.out.println("Unknown state: " + state + ", defaulting to WEAPON_SEARCH");
                    state = "WEAPON_SEARCH";
                }
            }

        } catch (Exception e) {
            System.err.println("Error in MapUpdateListener: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateState(GameMap gameMap, Player player, Inventory inventory) {
        previousState = state;
        Node chest = Resource.findChest(gameMap, player);
        SupportItem spItem = Resource.findHealing(gameMap, player);
        
        if (player.getHealth() <= 0) {
            state = "WEAPON_SEARCH";
            System.out.println("Hero died, switching to WEAPON_SEARCH");
            return;
        }
        if (chest != null || spItem != null) {
	        if (player.getHealth() < Config.HP_DANGER_THRESHOLD) {
	            state = "HEALING";
	            System.out.println("HP critical (" + player.getHealth() + "), switching to HEALING");
	            return;
	        }
        }

        if (player.getHealth() <= Config.HP_MEDIUM_THRESHOLD && !inventory.getListSupportItem().isEmpty()) {
            try {
                SupportItem healing = inventory.getListSupportItem().get(0);
                hero.useItem(healing.getId());
                Combat.updateCD();
                System.out.println("Used healing item: " + healing.getId());
            } catch (IOException e) {
                System.err.println("Error using healing item: " + e.getMessage());
            }
        }

        if (Combat.hasWeapon(inventory)) {
            System.out.println("Found weapon in inventory, switching to COMBAT");
            state = "COMBAT";
            return;
        }
        
        if (!state.equals(previousState)) {
            System.out.println("State changed from " + previousState + " to " + state);
        } else {
            System.out.println("State remains: " + state);
        }
    }
    private boolean handleLooting(GameMap gameMap, Player player, Inventory inventory) throws IOException {
        if (!Navigator.keepLooting(gameMap, player, inventory)) return false;
        
        Node chest = Resource.findChest(gameMap, player);
        if (chest != null) {
            double chestDistance = PathUtils.distance(player, chest);

            Combat.CombatTarget nearestEnemy = Combat.findBestTarget(gameMap, player, inventory);
            double enemyDistance = (nearestEnemy != null) ? nearestEnemy.distance : Double.MAX_VALUE;
            
            System.out.println("Chest distance: " + chestDistance + ", Enemy distance: " + enemyDistance);
            
            if (Navigator.shouldLootChest(inventory, chestDistance, enemyDistance)) {
                searchChest(gameMap, player, inventory);
                return true;
            }
        }
        
        if (Resource.gatherResources(gameMap, player, inventory, hero)) {
            state = "COMBAT";
            return true;
        }
        
        return false;
    }
    private void searchWeapon(GameMap gameMap, Player player, Inventory inventory) throws IOException {
        System.out.println("Searching for weapon...");

        Weapon weapon = Resource.findWeapon(gameMap, player, inventory);
        
        if (weapon == null) {
            System.out.println("No weapon found on map, searching chest");
            searchChest(gameMap, player, inventory);
            return;
        }
        
        if (moveToTarget(gameMap, player, weapon, "weapon")) {
            return;
        }

        System.out.println("Cannot reach weapon, searching chest");
        searchChest(gameMap, player, inventory);
    }

    private void searchCombat(GameMap gameMap, Player player, Inventory inventory) throws IOException {
        System.out.println("Searching for combat...");
        
        Combat.CombatTarget enemy = Combat.findBestTarget(gameMap, player, inventory);
        
        if (enemy == null) {
            System.out.println("No enemy found, switching to RESOURCE_GATHER");
            state = "RESOURCE_GATHER";
            return;
        }
        
        double distance = PathUtils.distance(player, enemy.target);

        //combat efficiency
        double damage = inventory.getMelee().getDamage();
        double cooldown = inventory.getMelee().getCooldown();
        double hitsToKill = Math.ceil(enemy.target.getHealth() / damage);
        double timeToKill = (hitsToKill - 1) * cooldown + distance; 
        
        System.out.println("Combat analysis - Target: " + enemy.target.getID() + 
                          ", Distance: " + distance + 
                          ", Hits to kill: " + hitsToKill + 
                          ", Time to kill: " + timeToKill + "s");
        
        
        Weapon weapon = Resource.findWeapon(gameMap, player, inventory);
        Node chest = Resource.findChest(gameMap, player);
        
        if ((weapon != null || chest != null) && timeToKill > 4.0) {
            if (!Combat.hasWeapon(inventory)) {
                System.out.println("Combat will take too long and no weapon available, switching to WEAPON_SEARCH");
                state = "WEAPON_SEARCH";
                return;
            }
        }

        if (Combat.inAttackRange(player, enemy.target, inventory)) {
            System.out.println("In attack range - distance: " + distance);
            if (tryAttack(player, enemy.target, inventory, distance, gameMap)) {
                Combat.updateCD();
                return;
            }
        }
        
        String path = PathUtils.getShortestPath(gameMap, Navigator.getObstacles(gameMap), player, enemy.target, true);
        
        if (path != null && !path.isEmpty()) {
        	if (path.equals(lastPath.get(1))) {
        		lastPath.remove(0);
        		lastPath.add(new StringBuilder(path).reverse().toString());
        		path = lastPath.get(1);
        	}
            else if (lastPath.contains(path)) {
            	path = lastPath.get(1);
            	lastPath.remove(0);
        		lastPath.add(path);
            }
            else {
            	lastPath.remove(0);
        		lastPath.add(path);
                path = lastPath.get(1);
            }
            
            hero.move(path);
            Combat.updateCD();
            System.out.println("Moving to enemy: " + path);
        } else {
            System.out.println("Cannot reach enemy, switching to RESOURCE_GATHER");
            state = "RESOURCE_GATHER";
        }
    }

    private void searchResources(GameMap gameMap, Player player, Inventory inventory) throws IOException {
        System.out.println("Searching for resources...");
        
        if (player.getHealth() < Config.HP_DANGER_THRESHOLD) {
            SupportItem healing = Resource.findHealing(gameMap, player);
            if (healing != null && moveToTarget(gameMap, player, healing, "healing")) {
                return;
            }
            
            System.out.println("Low health but no healing found, switching to HEALING");
            state = "HEALING";
            return;
        }
        
        if (Combat.needsBetterWeapon(inventory)) {
            Weapon weapon = Resource.findWeapon(gameMap, player, inventory);
            System.out.println("Looking for better weapon: " + weapon);
            
            if (weapon != null && moveToTarget(gameMap, player, weapon, "better weapon")) {
                return;
            }

            System.out.println("No better weapon found, searching chest");
            searchChest(gameMap, player, inventory);
            return;
        }
        
        System.out.println("No resources needed, switching to COMBAT");
        state = "COMBAT";
    }

    private void searchHealing(GameMap gameMap, Player player, Inventory inventory) throws IOException {
        System.out.println("Searching for healing...");
        

        if (player.getHealth() > Config.HP_MEDIUM_THRESHOLD) {
            System.out.println("Health recovered, switching to COMBAT");
            state = "COMBAT";
            return;
        }
        

        if (!inventory.getListSupportItem().isEmpty()) { 
            SupportItem healingItem = inventory.getListSupportItem().get(0);
            System.out.println("HP low, using healing item: " + healingItem.getId());
            try {
                hero.useItem(healingItem.getId());
                Combat.updateCD();
                return;
            } catch (IOException e) {
                System.err.println("Error using healing item: " + e.getMessage());
            }
        }
        

        SupportItem healing = Resource.findHealing(gameMap, player);
        System.out.println("Healing item found on map: " + healing);
        
        if (healing == null) {
            System.out.println("No healing items found, searching chest");
            searchChest(gameMap, player, inventory);
            return;
        }
        
        if (moveToTarget(gameMap, player, healing, "healing")) {
            return;
        }

        System.out.println("Cannot reach healing, switching to RESOURCE_GATHER");
        state = "RESOURCE_GATHER";
    }

    private void searchChest(GameMap gameMap, Player player, Inventory inventory) throws IOException {
        Node chest = Resource.findChest(gameMap, player);
        
        System.out.println("Chest found: " + chest);
        
        if (chest == null) {
            System.out.println("No chest found, switching to COMBAT");
            state = "COMBAT";
            return;
        }
        
        double distance = PathUtils.distance(player, chest);
        String direction = Navigator.getAttackDirection(player, chest);


        if (Combat.inAttackRange(player, chest, inventory)) {
            if (tryAttack(player, chest, inventory, distance, gameMap)) {
                Combat.updateCD();
                return;
            }
        }

        String path = PathUtils.getShortestPath(gameMap, Navigator.getObstacles(gameMap), player, chest, true);
        if (path != null && !path.isEmpty() && distance > 1) {
            hero.move(path);
            Combat.updateCD();
            System.out.println("Moving to chest: " + path);
        } else if (path != null && !path.isEmpty()) {
            hero.attack(direction);
            Combat.updateCD();
            System.out.println("Attacking chest: " + direction);
        } else {
            System.out.println("Cannot reach chest, switching to COMBAT");
            state = "COMBAT";
        }
    }

    private boolean tryAttack(Player player, Node target, Inventory inventory, double distance, GameMap gameMap) throws IOException {
        String direction = Navigator.getAttackDirection(player, target);
        String action = Combat.selectWeaponAction(inventory);
        
        switch (action) {
            case "shoot" -> {
                if (inventory.getGun() != null) {
                    int[] attackRange = inventory.getGun().getRange();
                    System.out.println("Shoot range: " + attackRange[0] + "-" + attackRange[1]);
                    
                    if (Navigator.checkObstacles(player, target, gameMap, inventory)) {
                        hero.shoot(direction);
                        System.out.println("Shooting " + direction);
                        return true;
                    }
                }
                System.out.println("Cannot shoot at " + direction);
                return false;
            }
            case "throw" -> {
                if (inventory.getThrowable() != null) {
                    int[] attackRange = inventory.getThrowable().getRange();
                    System.out.println("Throw range: " + attackRange[0] + "-" + attackRange[1]);
                    
                    hero.throwItem(direction);
                    System.out.println("Throwing " + direction);
                    return true;
                }
                return false;
            }
            case "special" -> {
                if (inventory.getSpecial() != null) {
                    int[] attackRange = inventory.getSpecial().getRange();
                    System.out.println("Special range: " + attackRange[0] + "-" + attackRange[1]);
                    
                    hero.useSpecial(direction);
                    if (distance <= 1) {
                        hero.attack(direction);
                    }
                    System.out.println("Using special " + direction);
                    return true;
                }
                return false;
            }
            case "attack" -> {
                if (inventory.getMelee() != null && distance <= inventory.getMelee().getRange()[1]) {
                    hero.attack(direction);
                    System.out.println("Attacking " + direction + " (state: " + state + ")");
                    return true;
                }
                return false;
            }
        }
        
        return false;
    }
    
    private boolean tryAttack(Player player, Player target, Inventory inventory, double distance, GameMap gameMap) throws IOException {
        String direction = Navigator.getAttackDirection(player, target);
        String action = Combat.selectWeaponAction(inventory, player, target);
        
        switch (action) {
            case "shoot" -> {
            	System.out.println("Case: " + action);
                if (inventory.getGun() != null) {
                    int[] attackRange = inventory.getGun().getRange();
                    System.out.println("Shoot range: " + attackRange[0] + "-" + attackRange[1]);
                    
                    if (Navigator.checkObstacles(player, target, gameMap, inventory)) {
                        hero.shoot(direction);
                        System.out.println("Shooting " + direction);
                        return true;
                    }
                }
                System.out.println("Cannot shoot at " + direction);
                return false;
            }
            case "throw" -> {
            	System.out.println("Case: " + action);
                if (inventory.getThrowable() != null) {
                    int[] attackRange = inventory.getThrowable().getRange();
                    System.out.println("Throw range: " + attackRange[0] + "-" + attackRange[1]);
                    
                    hero.throwItem(direction);
                    System.out.println("Throwing " + direction);
                    return true;
                }
                return false;
            }
            case "special" -> {
            	System.out.println("Case: " + action);
                if (inventory.getSpecial() != null) {
                    int[] attackRange = inventory.getSpecial().getRange();
                    System.out.println("Special range: " + attackRange[0] + "-" + attackRange[1]);
                    
                    hero.useSpecial(direction);
                    if (distance <= 1) {
                        hero.attack(direction);
                    }
                    System.out.println("Using special " + direction);
                    return true;
                }
                return false;
            }
            case "attack" -> {
                if (distance <= 1) {
                    hero.attack(direction);
                    System.out.println("Attacking " + direction + " (state: " + state + ")");
                    return true;
                }
                return false;
            }
            case "dodge" -> {
            	System.out.println("Case: " + action);
                String dodgeDirection = Navigator.getDodgeDirection(player, target, gameMap);
                if (dodgeDirection != null && !dodgeDirection.isEmpty()) {
                    hero.move(dodgeDirection);
                    System.out.println("Dodging " + dodgeDirection);
                    return true;
                } else {
                    System.out.println("Cannot find dodge direction");
                    return false;
                }
            }
        }
        return false;
    }
    
    private boolean moveToTarget(GameMap gameMap, Player player, Node target, String targetType) throws IOException {
        String path = PathUtils.getShortestPath(gameMap, Navigator.getObstacles(gameMap), player, target, true);
        
        if (path == null) {
            System.out.println("Cannot find path to " + targetType);
            return false;
        }
        if (path.isEmpty()) {
            hero.pickupItem();
            Combat.resetCD();
            System.out.println("Picked up " + targetType + ": " + target);
        } else {

            if (path.equals(lastPath.get(1))) {
            	lastPath.remove(0);
            	lastPath.add(new StringBuilder(path).reverse().toString());
            	path = lastPath.get(1);
            } else if (lastPath.contains(path)) {
                path = lastPath.get(1);
                lastPath.remove(0);
            	lastPath.add(path);
            } else {
                lastPath.remove(0);
                lastPath.add(path);
                path = lastPath.get(1);
            }
            
            hero.move(path);
            Combat.updateCD();
            System.out.println("Moving to " + targetType + ": " + path);
        }
        return true;
    }
}