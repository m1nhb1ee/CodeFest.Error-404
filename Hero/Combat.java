package Hero;

import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;

import java.util.*;

public class Combat{
    
    public static class CombatTarget {
        public final Player target;
        public final double distance;
//        public final double threat;
//        public final double opportunity;

        
        public CombatTarget(Player target, double distance /*double threat, double opportunity*/) {
            this.target = target;
            this.distance = distance;
//            this.threat = threat;
//            this.opportunity = opportunity;

        }
        
        public double getScore() {
            return 100/distance;
        }
    }
    

    public static CombatTarget findBestTarget(GameMap gameMap, Player currentPlayer, Inventory inventory) {
        List<Player> enemies = gameMap.getOtherPlayerInfo();
        List<CombatTarget> targets = new ArrayList<>();
        
        for (Player enemy : enemies) {
            if (enemy.getHealth() <= 0) continue;
            if (!PathUtils.checkInsideSafeArea(enemy, gameMap.getSafeZone(), gameMap.getMapSize())) continue;
            
            
            double distance = PathUtils.distance(currentPlayer, enemy);
            if (distance > Config.MAX_CHASE_DISTANCE) continue;
           
            
//            double threat = calculateThreat(enemy, distance);
//            double opportunity = calculateOpportunity(enemy, distance, currentPlayer);
            
            targets.add(new CombatTarget(enemy, distance/*threat, opportunity*/));
        }
        

        return targets.stream()
                /*.filter(t -> t.getScore() > 0.5)*/
                .max(Comparator.comparingDouble(CombatTarget::getScore))
                .orElse(null);
    }
    

//    private static double calculateThreat(Player enemy, double distance) {
//        double baseThreat = enemy.getHealth() / 100.0;
//    	double baseThreat = distance;
//        return baseThreat;
//    }

//    private static double calculateOpportunity(Player enemy, double distance, Player currentPlayer) {
//    	
//        double healthRatio = (100.0 - enemy.getHealth()) / 100.0;           
//        double myHealthRatio = currentPlayer.getHealth() / 100.0;    
//        
//        if (myHealthRatio >= (1-healthRatio)) return 2;
//        
//        double distanceFactor = 1.0 / distance ;     
//        double lowHpBonus;
//        if (enemy.getHealth() <= Config.CRIT_HP_TARGET) lowHpBonus = 0.5;
//        else if (enemy.getHealth() <= Config.LOW_HP_TARGET) lowHpBonus = 0.3;
//        else if (enemy.getHealth() <= Config.MED_HP_TARGET) lowHpBonus = 0.1;
//        else lowHpBonus = 0;
//
//        return (healthRatio * 0.6 + myHealthRatio * 0.4) * distanceFactor + lowHpBonus;
//    }

    

    public static String selectWeaponAction(Inventory inventory) {
        if (inventory.getGun() != null) {
            return "shoot";
        }

        if (inventory.getThrowable() != null) {
            return "throw";
        }

        if (inventory.getSpecial() != null) {
            return "special";
        }
        
        return "attack";
    }

//    public static boolean shouldRetreat(Player currentPlayer, GameMap gameMap) {
//
//        if (currentPlayer.getHealth() <= Config.HP_RETREAT_THRESHOLD) {
//            return true;
//        }
//
//        long nearbyEnemies = gameMap.getOtherPlayerInfo().stream()
//            .filter(p -> p.getHealth() > 0)
//            .filter(p -> PathUtils.distance(currentPlayer, p) <= 3)
//            .count();
//        
//        return nearbyEnemies >= 2;
//    }
    
    
    
    
    public static boolean hasWeapon(Inventory inventory) {
        return inventory.getGun() != null || inventory.getSpecial() != null || inventory.getThrowable() != null;
    }

    public static boolean needsBetterWeapon(Inventory inventory) {
        return inventory.getGun() == null && inventory.getSpecial() == null && inventory.getThrowable() == null;
    }
}