package lastSrc;


import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.armors.Armor;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.support_items.SupportItem;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;

import java.util.*;


public class Resource {

    public static class BestHealingItem {
        public final SupportItem item;
        public final int priority;
        public final double distance;
        
        public BestHealingItem(SupportItem item, int priority, double distance) {
            this.item = item;
            this.priority = priority;
            this.distance = distance;
        }
        
        public double getScore() {
            return priority / (distance + 1);
        }
    }
    
    public static class BestWeaponItem {
        public final Weapon item;
        public final int priority;
        public final double distance;
        public final double dps;
        
        public BestWeaponItem(Weapon item, int priority, double distance, double dps) {
            this.item = item;
            this.priority = priority;
            this.distance = distance;
            this.dps = dps;
        }
        
        public double getScore() {
            return priority * dps / (distance + 1);
        }
    }
    
    public static class BestChestItem {
        public final Obstacle item;
        public final int priority;
        public final double distance;
        
        public BestChestItem(Obstacle item, int priority, double distance) {
            this.item = item;
            this.priority = priority;
            this.distance = distance;
        }
        
        public double getScore() {
            return priority / (distance + 1);
        }
    }

    public static Weapon findWeapon(GameMap gameMap, Player currentPlayer, Inventory inventory) {
        List<BestWeaponItem> candidates = new ArrayList<>();
        
        addWeapon(candidates, gameMap, currentPlayer, inventory);

        if (candidates.isEmpty()) return null;
        
        
        return candidates.stream()
            .max(Comparator.comparingDouble(BestWeaponItem::getScore))
            .get().item;
    }
    
    public static SupportItem findHealing(GameMap gameMap, Player currentPlayer) {
        List<BestHealingItem> candidates = new ArrayList<>();
        
        addHealing(candidates, gameMap, currentPlayer);

        if (candidates.isEmpty()) return null;
        
        return candidates.stream()
            .max(Comparator.comparingDouble(BestHealingItem::getScore))
            .get().item;
    }
    
    public static Node findChest(GameMap gameMap, Player currentPlayer) {
    	
    	List<BestChestItem> candidates = new ArrayList<>();
        
        addChest(candidates, gameMap, currentPlayer);

        if (candidates.isEmpty()) return null;
    	
        return candidates.stream()
            .max(Comparator.comparingDouble(BestChestItem::getScore))
            .get().item;
    }
    
    private static void addChest(List<BestChestItem> candidates, GameMap gameMap, Player currentPlayer) {
        for (Obstacle chest : gameMap.getObstaclesByTag("DESTRUCTIBLE")) {
	            int priority = chest.getId() == "CHEST" ? 
	                Config.CHEST_PRIORITY : Config.DRAGON_EGG_PRIORITY;
	            double distance = PathUtils.distance(currentPlayer, chest);
	            
	            
	            if (PathUtils.checkInsideSafeArea(chest, gameMap.getSafeZone(), gameMap.getMapSize()))
	            	candidates.add(new BestChestItem(chest, priority, distance));
        	
        }
    }
    
    private static void addWeapon(List<BestWeaponItem> candidates, GameMap gameMap, Player currentPlayer, Inventory inventory) {
    	if (inventory.getGun() == null) {
	        for (Weapon gun : gameMap.getAllGun()) {
	            double distance = PathUtils.distance(currentPlayer, gun);
	            double dps = gun.getDamage() / gun.getCooldown();
	            if (PathUtils.checkInsideSafeArea(gun, gameMap.getSafeZone(), gameMap.getMapSize()) && !Navigator.checkObstacles(gameMap, gun, null))
	            	candidates.add(new BestWeaponItem(gun, Config.GUN_PRIORITY, distance, dps));
	        }
    	}
    	
        if (inventory.getThrowable() == null) {
	        for (Weapon throwable : gameMap.getAllThrowable()) {
	            double distance = PathUtils.distance(currentPlayer, throwable);
	            double dps = throwable.getDamage() / throwable.getCooldown();
	            if (PathUtils.checkInsideSafeArea(throwable, gameMap.getSafeZone(), gameMap.getMapSize()) && !Navigator.checkObstacles(gameMap, throwable, null))
	            	candidates.add(new BestWeaponItem(throwable, Config.THROWABLE_PRIORITY, distance, dps));
	        }
        }
    	
		if (inventory.getMelee().getId() == "HAND" && "HAND".equals(inventory.getMelee().getId()) && inventory.getMelee().getDamage() <=5 ) {
	        for (Weapon melee : gameMap.getAllMelee()) {
	            double distance = PathUtils.distance(currentPlayer, melee);
	            double dps = melee.getDamage() / melee.getCooldown();
	            if (PathUtils.checkInsideSafeArea(melee, gameMap.getSafeZone(), gameMap.getMapSize()) && !Navigator.checkObstacles(gameMap, melee, null))
	            	candidates.add(new BestWeaponItem(melee, Config.THROWABLE_PRIORITY, distance, dps));
	        }
        }
    }
    
    private static void addHealing(List<BestHealingItem> candidates, GameMap gameMap, Player currentPlayer) {
        for (SupportItem healing : gameMap.getListSupportItems()) {
        	
            double distance = PathUtils.distance(currentPlayer, healing);
            
            int priority = currentPlayer.getHealth() <= Config.HP_DANGER_THRESHOLD ? 
                Config.HEALING_PRIORITY * 2 : Config.HEALING_PRIORITY;
            
            if (PathUtils.checkInsideSafeArea(healing, gameMap.getSafeZone(), gameMap.getMapSize()))
            	candidates.add(new BestHealingItem(healing, priority, distance));
        }
    }

    public static Boolean gatherResources(GameMap gameMap, Player currentPlayer, Inventory inventory, Hero hero) {
        try {
            if (inventory.getGun() == null) {
                for (Weapon gun : gameMap.getAllGun()) {
                    double distance = PathUtils.distance(currentPlayer, gun);
                    if (distance <= 2) {
                    	System.out.println("detect gun");
                        if (distance == 0) {
                            hero.pickupItem();
                            Combat.resetCD();
                            System.out.println("Picked up gun at current position");
                            return true;
                        } else {
                        	String path = PathUtils.getShortestPath(gameMap, Navigator.getObstacles(gameMap), currentPlayer, gun, true);
                        	System.out.println("Path to gun: " + path);
                        	if (path != null && Navigator.checkObstacles(gameMap, currentPlayer, path)) {
                        		return false;
                        	}
                            if (path != null && !path.isEmpty()) {
                                hero.move(path);
                                Combat.updateCD();
                                System.out.println("Moving to gun with path: " + path);
                                return true;
                            }
                            return false;
                        }
                    }
                }
            }
            if (inventory.getHelmet() == null) {
                for (Armor helmet : gameMap.getListArmors()) {
                	if ("MAGIC_HELMET".equals(helmet.getId()) || "WOODEN_HELMET".equals(helmet.getId())  ) {
	                    double distance = PathUtils.distance(currentPlayer, helmet);
	                    if (distance <= 5.0) { 
	                    	System.out.println("detect helmet");
	                        if (distance == 0) {
	                            hero.pickupItem();
	                            Combat.resetCD();
	                            System.out.println("Picked up helmet at current position");
	                            return true;
	                        } else {
	                        	String path = PathUtils.getShortestPath(gameMap, Navigator.getObstacles(gameMap), currentPlayer, helmet, true);
	                        	System.out.println("Path to helmet: " + path);
	                            
	                        	if (path != null && Navigator.checkObstacles(gameMap, currentPlayer, path)) {
	                        		return false;
	                        	}
	                        	
	                            if (path != null && !path.isEmpty()) {
	                                hero.move(path);
	                                Combat.updateCD();
	                                System.out.println("Moving to helmet with path: " + path);
	                                return true;
	                            }
	                            return false;
	                        }
	                    }
                	}
                }
            }
            if (inventory.getArmor() == null) {
                for (Armor armor : gameMap.getListArmors()) {
                	if ("MAGIC_ARMOR".equals(armor.getId()) || "ARMOR".equals(armor.getId())  ) {
	                    double distance = PathUtils.distance(currentPlayer, armor);
	                    if (distance <= 5.0) {
	                    	System.out.println("detect armor");
	                        if (distance == 0) {
	                            hero.pickupItem();
	                            Combat.resetCD();
	                            System.out.println("Picked up armor at current position");
	                            return true;
	                        } else {
	                        	String path = PathUtils.getShortestPath(gameMap, Navigator.getObstacles(gameMap), currentPlayer, armor, true);
	                        	System.out.println("Path to armor: " + path);	
	                        	if (path != null && Navigator.checkObstacles(gameMap, currentPlayer, path)) {
	                        		return false;
	                        	}
	                            if (path != null && !path.isEmpty()) {
	                                hero.move(path);
	                                Combat.updateCD();
	                                System.out.println("Moving to armor with path: " + path);
	                                return true;
	                            }
	                            return false;
	                        }
	                    }
                	}
                }
            }
            if (inventory.getListSupportItem().size() < 4) {
                for (SupportItem healing : gameMap.getListSupportItems()) {
                    double distance = PathUtils.distance(currentPlayer, healing);
                    if (distance <= 5) {
                    	System.out.println("detect healing");
                        if (distance == 0) {
                            hero.pickupItem();
                            Combat.resetCD();
                            System.out.println("Picked up healing item at current position");
                            return true;
                        } else {
                        	String path = PathUtils.getShortestPath(gameMap, Navigator.getObstacles(gameMap), currentPlayer, healing, true);
                        	System.out.println("Path to healing: " + path);
                        	if (path != null && Navigator.checkObstacles(gameMap, currentPlayer, path)) {
                        		return false;
                        	}
                            if (path != null && !path.isEmpty()) {
                                hero.move(path);
                                Combat.updateCD();
                                System.out.println("Moving to healing with path: " + path);
                                return true;
                            } 
                            return false;
                        }
                    }
                }
            }
            if ( inventory.getMelee().getDamage() <= 30 )  {
                for (Weapon melee : gameMap.getAllMelee()) {
                    double distance = PathUtils.distance(currentPlayer, melee);
                    if (distance <= 8 && ((double)(inventory.getMelee().getDamage() / inventory.getMelee().getCooldown())) < ((double)(melee.getDamage() / melee.getCooldown()))) {
                    	System.out.println("detect melee " + inventory.getMelee().getDamage() + " " + melee.getDamage());
                        if (distance == 0) {
                            hero.pickupItem();
                            if (!"HAND".equals(inventory.getMelee().getId())) {
                            	hero.revokeItem(inventory.getMelee().getId());
                            	Combat.updateCD();
                            	System.out.println("Revoked melee at current position");
                            }
                            Combat.resetCD();
                            System.out.println("Picked up melee at current position");
                            return true;
                        } else {
                        	String path = PathUtils.getShortestPath(gameMap, Navigator.getObstacles(gameMap), currentPlayer, melee, true);
                        	System.out.println("Path to melee: " + path);
                        	if (path != null && Navigator.checkObstacles(gameMap, currentPlayer, path)) {
                        		return false;
                        	}
                            if (path != null && !path.isEmpty()) {
                                hero.move(path);
                                Combat.updateCD();
                                System.out.println("Moving to melee with path: " + path);
                                return true;
                            }
                            return false;
                        }
                    }
                }
            }       
            if (inventory.getThrowable() == null) {
                for (Weapon throwable : gameMap.getAllThrowable()) {
                    double distance = PathUtils.distance(currentPlayer, throwable);
                    if (distance <= 5) {
                    	System.out.println("detect throwable");
                        if (distance == 0) {
                            hero.pickupItem();
                            Combat.resetCD();
                            System.out.println("Picked up throwable at current position");
                            return true;
                        } else {
                        	String path = PathUtils.getShortestPath(gameMap, Navigator.getObstacles(gameMap), currentPlayer, throwable, true);
                        	System.out.println("Path to throwable: " + path);
                        	if (path != null && Navigator.checkObstacles(gameMap, currentPlayer, path)) {
                        		return false;
                        	}
                            if (path != null && !path.isEmpty()) {
                                hero.move(path);
                                Combat.updateCD();
                                System.out.println("Moving to throwable with path: " + path);
                                return true;
                            }
                            return false;
                        }
                    }
                }
            }
            if (inventory.getSpecial() == null) {
                for (Weapon special : gameMap.getAllSpecial()) {
                    double distance = PathUtils.distance(currentPlayer, special);
                    if (distance <= 5.0) {
                    	System.out.println("detect special");
                        if (distance == 0) {
                            hero.pickupItem();
                            Combat.resetCD();
                            System.out.println("Picked up special weapon at current position");
                            return true;
                        } else {
                        	String path = PathUtils.getShortestPath(gameMap, Navigator.getObstacles(gameMap), currentPlayer, special, true);
                            System.out.println("Path to special: " + path);
                            if (path != null && Navigator.checkObstacles(gameMap, currentPlayer, path)) {
                        		return false;
                        	}
                            if (path != null && !path.isEmpty()) {
                                hero.move(path);
                                Combat.updateCD();
                                System.out.println("Moving to special with path: " + path);
                                return true;
                            }
                            return false;
                            
                        }
                    }
                }
            }
            
            System.out.println("No resources found in range or inventory full");
            return false;
        } catch (Exception e) {
            System.err.println("Error gathering resources: " + e.getMessage());
            return false;
        }
    }
    
    public static String uselessItem(Inventory inventory, SupportItem item) {
        List<SupportItem> check = new ArrayList<>(inventory.getListSupportItem());
        check.add(item);
        
        return check.stream()
                .max(Comparator.comparingDouble(SupportItem::getHealingHP))
                .map(SupportItem::getId)
                .orElse(null);
    }



}