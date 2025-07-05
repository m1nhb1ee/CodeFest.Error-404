package Hero;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.players.Player;

public class Navigator {
    public static boolean checkObstacles(Player myHero, Node currentPlayer, GameMap gameMap, Inventory inventory) {
    	
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
    	}
    	else return false;
    }
    
public static boolean checkObstacles(GameMap gameMap, Player player, String direct) {
    	
    	List<Node> nodes = new ArrayList<>(gameMap.getListObstacles());
        nodes.removeAll(gameMap.getObstaclesByTag("CAN_GO_THROUGH"));
        
        int x,y;
        x = player.getX();
        y = player.getY();
        
        if (direct == "u") y=y+1;
        else if (direct == "d") y=y-1;
        else if (direct == "l") x=x-1;
        else if (direct == "r") x=x+1;
        else System.out.println("What the fvck direction");
        
    	for (Node node : nodes) {
            if (node.getX() == x && node.getY() == y) return true;
    	}
    	
    	return false;
    }
    
    public static String getDirection(Node from, Node to) {
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
            new String[]{"ud", "du"} :  
            new String[]{"lr", "rl"};   
        

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
}
