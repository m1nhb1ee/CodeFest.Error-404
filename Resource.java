package Hero;


import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.armors.Armor;
import jsclub.codefest.sdk.model.healing_items.HealingItem;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;
import Hero.Config;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;

import java.util.*;


public class Resource {

    public static class BestHealingItem {
        public final HealingItem item;
        public final int priority;
        public final double distance;
        
        public BestHealingItem(HealingItem item, int priority, double distance) {
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
        
        public BestWeaponItem(Weapon item, int priority, double distance) {
            this.item = item;
            this.priority = priority;
            this.distance = distance;
        }
        
        public double getScore() {
            return priority / (distance + 1);
        }
    }

    public static Weapon findWeapon(GameMap gameMap, Player currentPlayer) {
        List<BestWeaponItem> candidates = new ArrayList<>();

        addWeapon(candidates, gameMap, currentPlayer);

        if (candidates.isEmpty()) return null;
        
        return candidates.stream()
            .max(Comparator.comparingDouble(BestWeaponItem::getScore))
            .get().item;
    }
    
    public static HealingItem findHealing(GameMap gameMap, Player currentPlayer) {
        List<BestHealingItem> candidates = new ArrayList<>();
        
        addHealing(candidates, gameMap, currentPlayer);

        if (candidates.isEmpty()) return null;
        
        return candidates.stream()
            .max(Comparator.comparingDouble(BestHealingItem::getScore))
            .get().item;
    }
    
    public static Node findNearestChest(GameMap gameMap, Player currentPlayer) {
        return gameMap.getListChests().stream()
            .min(Comparator.comparingDouble(chest -> PathUtils.distance(currentPlayer, chest)))
            .orElse(null);
    }
    
    private static void addWeapon(List<BestWeaponItem> candidates, GameMap gameMap, Player currentPlayer) {

        for (Weapon gun : gameMap.getAllGun()) {
            double distance = PathUtils.distance(currentPlayer, gun);
            if (PathUtils.checkInsideSafeArea(gun, gameMap.getSafeZone(), gameMap.getMapSize()))
            	candidates.add(new BestWeaponItem(gun, Config.GUN_PRIORITY, distance));
        }
        

        for (Weapon special : gameMap.getAllSpecial()) {
            double distance = PathUtils.distance(currentPlayer, special);
            if (PathUtils.checkInsideSafeArea(special, gameMap.getSafeZone(), gameMap.getMapSize()))
            	candidates.add(new BestWeaponItem(special, Config.SPECIAL_WEAPON_PRIORITY, distance));
        }

        for (Weapon throwable : gameMap.getAllThrowable()) {
            double distance = PathUtils.distance(currentPlayer, throwable);
            if (PathUtils.checkInsideSafeArea(throwable, gameMap.getSafeZone(), gameMap.getMapSize()))
            	candidates.add(new BestWeaponItem(throwable, Config.THROWABLE_PRIORITY, distance));
        }
        
    }
    
    private static void addHealing(List<BestHealingItem> candidates, GameMap gameMap, Player currentPlayer) {
        for (HealingItem healing : gameMap.getListHealingItems()) {
        	
            double distance = PathUtils.distance(currentPlayer, healing);
            
            int priority = currentPlayer.getHealth() <= Config.HP_DANGER_THRESHOLD ? 
                Config.HEALING_PRIORITY * 2 : Config.HEALING_PRIORITY;
            
            if (PathUtils.checkInsideSafeArea(healing, gameMap.getSafeZone(), gameMap.getMapSize()))
            	candidates.add(new BestHealingItem(healing, priority, distance));
        }
    }

    public static Boolean gatherResources(GameMap gameMap, Player currentPlayer, Inventory inventory, Hero hero) {
        try {
            Navigator navigator = new Navigator();

            if (inventory.getHelmet() == null) {
                for (Armor helmet : gameMap.getListArmors()) {
                	if ("MAGIC_HELMET".equals(helmet.getId()) || "HELMET".equals(helmet.getId())  ) {
	                    double distance = PathUtils.distance(currentPlayer, helmet);
	                    if (distance <= 3.0) { 
	                        if (distance == 0) {
	                            hero.pickupItem();
	                            System.out.println("Picked up helmet at current position");
	                            return true;
	                        } else {
	                        	String path = PathUtils.getShortestPath(gameMap, Navigator.getObstacles(gameMap), currentPlayer, helmet, false);
	                            hero.move(path);
	                            distance = PathUtils.distance(currentPlayer, helmet);
	                            if (distance == 0) {
		                            hero.pickupItem();
		                            System.out.println("Moved " + path + " and picked up helmet");
	                            }
	                            return true;
	                        }
	                    }
                	}
                }
            }
            
            if (inventory.getArmor() == null) {
                for (Armor armor : gameMap.getListArmors()) {
                	if ("MAGIC_ARMOR".equals(armor.getId()) || "ARMOR".equals(armor.getId())  ) {
	                    double distance = PathUtils.distance(currentPlayer, armor);
	                    if (distance <= 3.0) {
	                        if (distance == 0) {
	                            hero.pickupItem();
	                            System.out.println("Picked up armor at current position");
	                            return true;
	                        } else {
	                        	String path = PathUtils.getShortestPath(gameMap, Navigator.getObstacles(gameMap), currentPlayer, armor, false);
	                            hero.move(path);
	                            distance = PathUtils.distance(currentPlayer, armor);
	                            if (distance == 0) {
		                            hero.pickupItem();
		                            System.out.println("Moved " + path + " and picked up armor");
	                            }
	                            return true;
	                        }
	                    }
                	}
                }
            }

            if (inventory.getGun() == null) {
                for (Weapon gun : gameMap.getAllGun()) {
                    double distance = PathUtils.distance(currentPlayer, gun);
                    if (distance <= 3.0) {
                        if (distance == 0) {
                            hero.pickupItem();
                            System.out.println("Picked up gun at current position");
                            return true;
                        } else {
                        	String path = PathUtils.getShortestPath(gameMap, Navigator.getObstacles(gameMap), currentPlayer, gun, false);
                            hero.move(path);
                            distance = PathUtils.distance(currentPlayer, gun);
                            if (distance == 0) {
	                            hero.pickupItem();
	                            System.out.println("Moved " + path + " and picked up gun");
                            }
                            return true;
                        }
                    }
                }
            }

            if (inventory.getSpecial() == null) {
                for (Weapon special : gameMap.getAllSpecial()) {
                    double distance = PathUtils.distance(currentPlayer, special);
                    if (distance <= 3.0) {
                        if (distance == 0) {
                            hero.pickupItem();
                            System.out.println("Picked up special weapon at current position");
                            return true;
                        } else {
                        	String path = PathUtils.getShortestPath(gameMap, Navigator.getObstacles(gameMap), currentPlayer, special, false);
                            hero.move(path);
                            distance = PathUtils.distance(currentPlayer, special);
                            if (distance == 0) {
	                            hero.pickupItem();
	                            System.out.println("Moved " + path + " and picked up special weapon");
                            }
                            return true;
                            
                        }
                    }
                }
            }
            
            if (inventory.getThrowable() == null) {
                for (Weapon throwable : gameMap.getAllThrowable()) {
                    double distance = PathUtils.distance(currentPlayer, throwable);
                    if (distance <= 3.0) {
                        if (distance == 0) {
                            hero.pickupItem();
                            System.out.println("Picked up throwable at current position");
                            return true;
                        } else {
                        	String path = PathUtils.getShortestPath(gameMap, Navigator.getObstacles(gameMap), currentPlayer, throwable, false);
                            hero.move(path);
                            distance = PathUtils.distance(currentPlayer, throwable);
                            if (distance == 0) {
	                            hero.pickupItem();
	                            System.out.println("Moved " + path + " and picked up throwable");
                            }
                            return true;
                        }
                    }
                }
            }

            if (inventory.getListHealingItem().size() < 4) {
                for (HealingItem healing : gameMap.getListHealingItems()) {
                    double distance = PathUtils.distance(currentPlayer, healing);
                    if (distance <= 5.0) {
                        if (distance == 0) {
                            hero.pickupItem();
                            System.out.println("Picked up healing item at current position");
                            return true;
                        } else {
                        	String path = PathUtils.getShortestPath(gameMap, Navigator.getObstacles(gameMap), currentPlayer, healing, false);
                            hero.move(path);
                            distance = PathUtils.distance(currentPlayer, healing);
                            if (distance == 0) {
	                            hero.pickupItem();
	                            System.out.println("Moved " + path + " and picked up healing item");
                            }
                            return true;
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

}