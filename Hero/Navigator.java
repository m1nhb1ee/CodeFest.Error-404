package Hero;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.players.Player;
import lastSrc.Combat;

public class Navigator {
    public static boolean checkAttack(Node myHero, Node currentPlayer, GameMap gameMap, Inventory inventory) {
    	
    	List<Node> nodes = new ArrayList<>(gameMap.getListObstacles());
        nodes.removeAll(gameMap.getObstaclesByTag("CAN_GO_THROUGH"));
        nodes.removeAll(gameMap.getObstaclesByTag("CAN_SHOOT_THROUGH"));
        
        if (Combat.inAttackRange(myHero, currentPlayer, inventory)) {
	    	for (Node node : nodes) {
	            if (node.getX() == myHero.getX() && (
	            	(node.getY() < currentPlayer.getY() && node.getY() > myHero.getY()) ||
	            	(node.getY() > currentPlayer.getY() && node.getY() < myHero.getY())
	            	)) return false;
	            else if (node.getY() == myHero.getY() && (
	                	(node.getX() < currentPlayer.getX() && node.getX() > myHero.getX()) ||
	                	(node.getX() > currentPlayer.getX() && node.getX() < myHero.getX())
	                	)) return false;
	    	}
	    	return true;
        } else return false;
    }
    
public static boolean checkObstacles(GameMap gameMap, int x, int y) {
    	
    	List<Node> nodes = new ArrayList<>(gameMap.getListObstacles());
        nodes.removeAll(gameMap.getObstaclesByTag("CAN_GO_THROUGH"));
        nodes.addAll(gameMap.getOtherPlayerInfo());
        
    	for (Node node : nodes) {
            if (node.getX() == x && node.getY() == y) return true;
    	}
    	
    	return false;
    }
    
    public static String getAttackDirection(Node from, Node to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        return Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? "r" : "l") : (dy > 0 ? "u" : "d");
    }
    

    public static List<Node> getObstacles(GameMap gameMap) {
        List<Node> obstacles = new ArrayList<>(gameMap.getListObstacles());
        obstacles.removeAll(gameMap.getObstaclesByTag("CAN_GO_THROUGH"));
        obstacles.addAll(gameMap.getObstaclesByTag("TRAP"));
        obstacles.addAll(gameMap.getListEnemies());
        obstacles.addAll(gameMap.getOtherPlayerInfo());
        return obstacles;
    }
    
    public static String getDodgeDirection(Player player, Node enemy, GameMap gameMap) {

        int dx = player.getX() - enemy.getX();
        int dy = player.getY() - enemy.getY();
        

        String[] dodgeOptions = Math.abs(dx) > Math.abs(dy) ? 
            new String[]{"u", "d"} :  
            new String[]{"l", "r"};   
        

        for (String dodge : dodgeOptions) {
            if (isSafeDodge(dodge, player.getX(), player.getY(), gameMap)) {
                return dodge;
            }
        }
        
        return dodgeOptions[new Random().nextInt(2)];
    }


    private static boolean isSafeDodge(String dodge, int x, int y, GameMap gameMap) {
    	
    	int xx = x;
    	int yy = y;
    	
        for (char dir : dodge.toCharArray()) {

            switch (dir) {
                case 'u' -> yy--;
                case 'd' -> yy++;
                case 'l' -> xx--;
                case 'r' -> xx++;
            }
            
            if (xx < 0 || xx >= gameMap.getMapSize() || 
                yy < 0 || yy >= gameMap.getMapSize()) {
                return false;
            }

            if (gameMap.getListObstacles().stream().anyMatch(obs -> 
                obs.getX() == x && obs.getY() == y && 
                !gameMap.getObstaclesByTag("CAN_GO_THROUGH").contains(obs))) {
                return false;
            }
        }
        return true;
    }


    public static boolean shouldLootChest(Inventory inventory, double chestDistance, double enemyDistance) {

        if (chestDistance >= enemyDistance) {
            return false;
        }
        
        if ("HAND".equals(inventory.getMelee().getId()) && inventory.getGun() != null) {
            return chestDistance <= 7;
        }
        else return chestDistance <= 5;
        
    }
    
    public static String fixedPath(String path, List<String> lastPath) {
    	
    	if (path.equals(lastPath.get(1))) return new StringBuilder(path).reverse().toString();
    	
        else if (path.equals(lastPath.get(0))) return lastPath.get(1);
        
        else return path;
    	
    }
    
    public static Node bestPosition(GameMap gameMap, Node player, Node target, Inventory inventory) {
    	Node lastPos = target;
    	String direction = getAttackDirection(player, target);
    	int distance = PathUtils.distance(lastPos, player);
    	
    	if (direction == "r") {
    		for (int i=1; i<=distance; i++) {
    			Node currentPos = gameMap.getElementByIndex(target.getX()-i, target.getY());
    			if (!checkAttack(currentPos, target, gameMap, inventory) || checkObstacles(gameMap, currentPos.getX(), currentPos.getY()) || PathUtils.distance(player, currentPos) >= PathUtils.distance(player, lastPos)) break;
    			lastPos = currentPos;
    		}
    	} else if (direction == "l") {
    		for (int i=1; i<=distance; i++) {
    			Node currentPos = gameMap.getElementByIndex(target.getX()+i, target.getY());
    			if (!checkAttack(currentPos, target, gameMap, inventory) || checkObstacles(gameMap, currentPos.getX(), currentPos.getY()) || PathUtils.distance(player, currentPos) >= PathUtils.distance(player, lastPos)) break;
    			lastPos = currentPos;
    		}
    	} else if (direction == "u") {
    		for (int i=1; i<=distance; i++) {
    			Node currentPos = gameMap.getElementByIndex(target.getX(), target.getY()-i);
    			if (!checkAttack(currentPos, target, gameMap, inventory) || checkObstacles(gameMap, currentPos.getX(), currentPos.getY()) || PathUtils.distance(player, currentPos) >= PathUtils.distance(player, lastPos)) break;
    			lastPos = currentPos;
    		}
    	} else if (direction == "d") {
    		for (int i=1; i<=distance; i++) {
    			Node currentPos = gameMap.getElementByIndex(target.getX(), target.getY()+i);
    			if (!checkAttack(currentPos, target, gameMap, inventory)  || PathUtils.distance(player, currentPos) >= PathUtils.distance(player, lastPos)) break;
    			lastPos = currentPos;
    		}
    	}
    	return lastPos;
    }

        /// has bug here
//    public static boolean keepLooting(GameMap gameMap, Player player, Inventory inventory) {
//    	
//    	Combat.CombatTarget enemy = Combat.findBestTarget(gameMap, player, inventory);
//    	if (enemy == null) return true;
//    	
//    	double distance = enemy.distance;
//        return distance > 1 ;
//        
//    }   
}
