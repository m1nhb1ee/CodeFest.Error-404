package Hero;

import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;

import java.util.*;

public class Combat{
    
	public static double gunCD, throwCD, meleeCD, specialCD, shootCount;
	
    public static class CombatTarget {
        public final Player target;
        public final double distance;
        
        public CombatTarget(Player target, double distance) {
            this.target = target;
            this.distance = distance;


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
           
            targets.add(new CombatTarget(enemy, distance));
        }
        

        return targets.stream()
                .max(Comparator.comparingDouble(CombatTarget::getScore))
                .orElse(null);
    }

    public static String selectWeaponAction(Inventory inventory, Player player, Player enemy) {
    	
    	double meleeD = 0, throwableD = 0;
    	double distance = PathUtils.distance(player, enemy);
    	
        if (inventory.getThrowable() != null) throwableD = inventory.getThrowable().getDamage();
        if (inventory.getMelee() != null) meleeD = inventory.getMelee().getDamage();

        if (inventory.getThrowable() != null && throwableD >= enemy.getHealth() && inThrowRange(player, enemy, inventory) && throwCD ==  0) 
        {
        	throwCD = inventory.getThrowable().getCooldown();
        	return "throw";
        }
        
        if (inventory.getMelee() != null && meleeD >= enemy.getHealth() && meleeCD == 0) 
        {
        	System.out.println("Enough Dame to kill");
        	if (distance < 4) {
        		meleeCD = inventory.getMelee().getCooldown();
        		return "attack";
        	}
        }
        
        
        if (gunCD > 0 && throwCD > 0 && meleeCD > 0 && specialCD > 0 && inAttackRange(player, enemy, inventory)) return "dodge";
         
        if (inventory.getGun() != null && gunCD == 0) {
        	if (inventory.getGun().getId().equals("SHOTGUN") && !inventory.getMelee().getId().equals("HAND") && meleeCD == 0) {
        		meleeCD = inventory.getMelee().getCooldown();
	        	return "attack";
        	}
        	
        	if (inShootRange(player, enemy, inventory)) {
	        	gunCD = inventory.getGun().getCooldown();
	        	return "shoot";
        	}
        }
        if (inventory.getThrowable() != null && throwCD == 0) {
        	System.out.println("Throw ready");
        	if (inThrowRange(player, enemy, inventory)) {
	        	throwCD = inventory.getThrowable().getCooldown();
	            return "throw";
        	}
        	System.out.println("Out of throw range");
        }
        if (inventory.getSpecial() != null && specialCD == 0) {
        	if (inSpecialRange(player, enemy, inventory)) {
	        	specialCD = inventory.getSpecial().getCooldown();
	            return "special";
        	}
        }
        
        System.out.println("none wp is ready");
        meleeCD = inventory.getMelee().getCooldown();
        return "attack";

    }

    public static void updateCD() {
    	gunCD = Math.max(0, gunCD-1);
    	throwCD = Math.max(0, throwCD-1);
    	meleeCD = Math.max(0, meleeCD-1);
    	specialCD = Math.max(0, specialCD-1);
    	System.out.println("CoolDown : Gun-"+gunCD+" Throw-"+throwCD+" SpecialCD-"+specialCD+" MeleeCD-"+meleeCD+" " );
    }
    
    public static void resetCD() {
    	specialCD=0;
    	gunCD=0;
    	throwCD=0;
    	meleeCD=0;
    	System.out.println("CoolDown : Gun-"+gunCD+" Throw-"+throwCD+" SpecialCD-"+specialCD+" MeleeCD-"+meleeCD+" " );
    }
    
    public static String selectWeaponAction(Inventory inventory) {

        
        if (inventory.getGun() != null && gunCD == 0) {
        	gunCD = inventory.getGun().getCooldown();
        	System.out.print("Shoot chest, cool down: " + gunCD);
        	return "shoot";
        }

        meleeCD = inventory.getMelee().getCooldown();
        return "attack";
    }
    public static boolean inAttackRange(Node attacker, Node target, Inventory inventory) {
    	
    	int width,length;
    	int[] shootRange, throwRange, specialRange, attackRange;
    	
    	shootRange = new int[2];
    	throwRange = new int[2];
    	specialRange = new int[2];
    	
        int dx = target.getX() - attacker.getX();
        int dy = target.getY() - attacker.getY();

        String direction = Navigator.getAttackDirection(attacker, target);
        
        if (inventory.getGun() != null) shootRange =  inventory.getGun().getRange();

        if (inventory.getThrowable() != null) throwRange =  inventory.getThrowable().getRange();

        if (inventory.getSpecial() != null) specialRange =  inventory.getSpecial().getRange();
        
        attackRange =  inventory.getMelee().getRange();
        
        int[][] ranges = {shootRange, throwRange, specialRange};
        int[] bestRange = attackRange;

        for (int[] range : ranges) {
            if (range[1] > bestRange[1]) {
                bestRange = range;
            }
        }

        width = bestRange[0];
        length = bestRange[1];
        
        return switch (direction) {
            case "u" -> dy > 0 && dy <= length && Math.abs(dx) <= width / 2;
            case "d" -> dy < 0 && -dy <= length && Math.abs(dx) <= width / 2;
            case "l" -> dx < 0 && -dx <= length && Math.abs(dy) <= width / 2;
            case "r" -> dx > 0 && dx <= length && Math.abs(dy) <= width / 2;
            default -> false;
        };
    }
    
    public static boolean inShootRange(Node attacker, Node target, Inventory inventory) {
    	
    	int width,length;
    	int[] shootRange;

        int dx = target.getX() - attacker.getX();
        int dy = target.getY() - attacker.getY();

        String direction = Navigator.getAttackDirection(attacker, target);
        
        shootRange =  inventory.getGun().getRange();
        width = shootRange[0];
        length = shootRange[1];

        return switch (direction) {
            case "u" -> dy > 0 && dy <= length && Math.abs(dx) <= width / 2;
            case "d" -> dy < 0 && -dy <= length && Math.abs(dx) <= width / 2;
            case "l" -> dx < 0 && -dx <= length && Math.abs(dy) <= width / 2;
            case "r" -> dx > 0 && dx <= length && Math.abs(dy) <= width / 2;
            default -> false;
        };
    }

    public static boolean inThrowRange(Node attacker, Node target, Inventory inventory) {
    	
    	int width,length;

        int dx = target.getX() - attacker.getX();
        int dy = target.getY() - attacker.getY();
        int range;
        
        range = inventory.getThrowable().getExplodeRange();
        
        String direction = Navigator.getAttackDirection(attacker, target);

        int[] attackRange =  inventory.getThrowable().getRange();
        width = attackRange[0];
        length = attackRange[1];
        
        
        return switch (direction) {
            case "u" -> dy > 0 && dy <= length+range && dy >= length-range && Math.abs(dx) <= (width+range)/2;
            case "d" -> dy < 0 && -dy <= length+range && -dy >= length-range && Math.abs(dx) <= (width+range)/2;
            case "l" -> dx < 0 && -dx <= length+range && -dx >= length-range && Math.abs(dy) <= (width+range)/2;
            case "r" -> dx > 0 && dx <= length+range && dx >= length-range && Math.abs(dy) <= (width+range)/2;
            default -> false;
        };
    }
    
    public static boolean inSpecialRange(Node attacker, Node target, Inventory inventory) {
    	
    	int width,length;

        int dx = target.getX() - attacker.getX();
        int dy = target.getY() - attacker.getY();

        
        String direction = Navigator.getAttackDirection(attacker, target);
        

        int[] attackRange =  inventory.getSpecial().getRange();
        width = attackRange[0];
        length = attackRange[1];
        
        
        return switch (direction) {
            case "u" -> dy > 0 && dy <= length && Math.abs(dx) <= width/2;
            case "d" -> dy < 0 && -dy <= length && Math.abs(dx) <= width/2;
            case "l" -> dx < 0 && -dx <= length && Math.abs(dy) <= width/2;
            case "r" -> dx > 0 && dx <= length && Math.abs(dy) <= width/2;
            default -> false;
        };
    }
    
    
    public static boolean hasWeapon(Inventory inventory) {
        return inventory.getGun() != null ;
    }

    public static boolean needsBetterWeapon(Inventory inventory) {
        return inventory.getGun() == null || inventory.getMelee().getId() == "HAND" || inventory.getThrowable() == null;
    }
    

}