import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapUpdateListener implements Emitter.Listener {
    private Hero hero;

    public MapUpdateListener(Hero hero) {
        this.hero = hero;
    }

    @Override
    public void call(Object... args) {
        try {
            if (args == null || args.length == 0) return;

            GameMap gameMap = hero.getGameMap();
            gameMap.updateOnUpdateMap(args[0]);

            Player player = gameMap.getCurrentPlayer();
            List<Player> enemies = gameMap.getOtherPlayerInfo();

            if (player == null || player.getHealth() == 0) return;

            Weapon myGun = hero.getInventory().getGun();
            Weapon myMelee = hero.getInventory().getMelee();
            Weapon myThrowable = hero.getInventory().getThrowable();


            List<Node> avoid = getNodesToAvoid(gameMap);
            List<Node> avoid2 = getNodesToAvoid2(gameMap);

            if (myGun == null) {
                handleSearchForGun(gameMap, player, avoid);
            }
            if (hasUsableWeapon()) {
                handleAttackEnemy(gameMap, player, avoid2);
            }


        } catch (Exception e) {
            System.err.println("Critical error in call method: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean hasUsableWeapon() {
        return (hero.getInventory().getGun() != null && hero.getInventory().getGun().getUseCount() > 0)
                || (hero.getInventory().getMelee() != null && hero.getInventory().getMelee().getUseCount() > 0)
                || (hero.getInventory().getThrowable() != null && hero.getInventory().getThrowable().getUseCount() > 0);
    }

    private List<Node> getNodesToAvoid(GameMap gameMap) {
        List<Node> nodes = new ArrayList<>(gameMap.getListIndestructibles());
        nodes.removeAll(gameMap.getObstaclesByTag("CAN_GO_THROUGH"));
        nodes.addAll(gameMap.getObstaclesByTag("DESTRUCTIBLE"));
        nodes.addAll(gameMap.getOtherPlayerInfo());
        nodes.addAll(gameMap.getListEnemies());
        return nodes;
    }

    private List<Node> getNodesToAvoid2(GameMap gameMap) {
        List<Node> nodes = new ArrayList<>(gameMap.getListIndestructibles());
        nodes.removeAll(gameMap.getObstaclesByTag("CAN_GO_THROUGH"));
        nodes.addAll(gameMap.getObstaclesByTag("DESTRUCTIBLE"));
        nodes.addAll(gameMap.getListEnemies());
        return nodes;
    }

    private Weapon getNearestWeapon(GameMap gameMap, Player player) {
        List<Weapon> weapons = gameMap.getListWeapons();
        Weapon nearestWeapon = null;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < weapons.size(); i++) {
            double distance = PathUtils.distance(player, weapons.get(i));
            if (distance < minDistance) {
                minDistance = distance;
                nearestWeapon = weapons.get(i);
            }
        }
        return nearestWeapon;
    }

    private void handleSearchForGun(GameMap gameMap, Player player, List<Node> avoid) throws IOException {
        Weapon nearestWeapon = getNearestWeapon(gameMap, player);
        if (nearestWeapon == null) return;
        String pathToWeapon = PathUtils.getShortestPath(gameMap, avoid, player, nearestWeapon, false);
        if (pathToWeapon != null) {
            if (pathToWeapon.isEmpty()) {
                hero.pickupItem();
            } else {
                hero.move(pathToWeapon);
            }
        }
    }

    private Player getNearestPlayer(GameMap gameMap, Player player) {
        List<Player> enemies = gameMap.getOtherPlayerInfo();
        Player nearestEnemy = null;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < enemies.size(); i++) {
            double distance = PathUtils.distance(player, enemies.get(i));
            if (distance < minDistance) {
                minDistance = distance;
                nearestEnemy = enemies.get(i);
            }
        }
        return nearestEnemy;
    }

    private String getDirection(Node from, Node to) {
        int dx = to.getX() - from.getX();
        int dy = to.getY() - from.getY();
        if (dx == 1) return "r";
        if (dx == -1) return "l";
        if (dy == 1) return "u";
        if (dy == -1) return "d";
        return "";
    }

    private void handleAttackEnemy(GameMap gameMap, Player player, List<Node> avoid) throws IOException {
        Player nearestEnemy = getNearestPlayer(gameMap, player);
        if (nearestEnemy != null) {
            Node enemyPos = new Node(nearestEnemy.getX(), nearestEnemy.getY());
            Node myPos = new Node(player.getX(), player.getY());
            int dist = PathUtils.distance(myPos, enemyPos);
            if (dist == 1) {
                String dir = getDirection(myPos, enemyPos);
                if (hero.getInventory().getGun() != null) {
                    hero.shoot(dir);
                } else if (hero.getInventory().getMelee() != null) {
                    hero.attack(dir);
                }
            } else {
                String path = PathUtils.getShortestPath(gameMap, avoid, myPos, enemyPos, true);
                if (!path.isEmpty()) {
                    hero.move(path);
                }
            }
        }
    }


}
