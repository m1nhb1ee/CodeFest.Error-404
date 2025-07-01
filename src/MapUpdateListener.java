import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.npcs.Enemy;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.AttackRange;
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

            if (player == null || player.getHealth() == 0) {
                System.out.println("Player is dead or data is not available");
                return;
            }

            List<Node> nodesToAvoid = getNodeToAvoid(gameMap);
            List<Node> nodesToAvoid2 = getNodeToAvoid2(gameMap);

            boolean heroGun = hero.getInventory().getGun() == null;
            boolean heroMelee = hero.getInventory().getMelee() == null;
            boolean heroThrowable = hero.getInventory().getThrowable() == null;

            if (heroGun && heroThrowable) {
                handleSearchForWeapon(gameMap, player, nodesToAvoid);
            }
            if (!heroGun || !heroThrowable) {
                findByAttackRange(gameMap, player);
                handleAttackOtherPlayer(gameMap, player, nodesToAvoid2);
            }


        } catch (Exception e) {
            System.err.println("Critical error in call method: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<Node> getNodeToAvoid(GameMap gameMap) {
        List<Node> nodes = new ArrayList<>(gameMap.getListIndestructibles());
        nodes.removeAll(gameMap.getObstaclesByTag("CAN_GO_THROUGH"));
        nodes.addAll(gameMap.getOtherPlayerInfo());
        nodes.addAll(gameMap.getListTraps());
        nodes.addAll(gameMap.getListChests());
        nodes.addAll(gameMap.getListEnemies());
        return nodes;
    }

    private List<Node> getNodeToAvoid2(GameMap gameMap) {
        List<Node> nodes = new ArrayList<>(gameMap.getListIndestructibles());
        nodes.removeAll(gameMap.getObstaclesByTag("CAN_GO_THROUGH"));
        nodes.addAll(gameMap.getListTraps());
        nodes.addAll(gameMap.getListChests());
        nodes.addAll(gameMap.getListEnemies());
        return nodes;
    }

    private Weapon getNearWeapon(GameMap gameMap, Player player) {
        List<Weapon> weapons = gameMap.getListWeapons();
        Weapon nearWeapon = null;
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < weapons.size(); i++) {
            double distance = PathUtils.distance(player, weapons.get(i));
            if (distance < minDistance) {
                minDistance = distance;
                nearWeapon = weapons.get(i);
            }
        }
        return nearWeapon;
    }

    private String findPathToWeapon(GameMap gameMap, List<Node> nodesToAvoid, Player player) {
        Weapon nearWeapon = getNearWeapon(gameMap, player);
        if (nearWeapon == null) return null;
        return PathUtils.getShortestPath(gameMap, nodesToAvoid, player, nearWeapon, false);
    }

    private void handleSearchForWeapon(GameMap gameMap, Player player, List<Node> nodesToAvoid) throws IOException {
        System.out.println("No weapon found, searching for one...");
        String pathToWeapon = findPathToWeapon(gameMap, nodesToAvoid, player);
        if (pathToWeapon != null) {
            if (pathToWeapon.isEmpty()) {
                hero.pickupItem();
            } else {
                hero.move(pathToWeapon);
            }
        }
    }

    private void findByAttackRange(GameMap gameMap, Player player) {
        Weapon weapon = null;
        if (hero.getInventory().getGun() != null) {
            weapon = hero.getInventory().getGun();
        } else if (hero.getInventory().getMelee() != null) {
            weapon = hero.getInventory().getMelee();
        } else if (hero.getInventory().getThrowable() != null) {
            weapon = hero.getInventory().getThrowable();
        }
        if (weapon == null) return;
        AttackRange attackRange = weapon.getAttackRange();
        System.out.println("Tầm đánh vũ khí của bạn là : "+attackRange);
    }

    private Player getNearPlayer(GameMap gameMap, Player player) {
        List<Player> otherPlayer = gameMap.getOtherPlayerInfo();
        Player nearPlayer = null;
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < otherPlayer.size(); i++) {
            double distance = PathUtils.distance(player, otherPlayer.get(i));
            if (distance < minDistance) {
                minDistance = distance;
                nearPlayer = otherPlayer.get(i);
            }
        }
        return nearPlayer;
    }

    private String findPathToPlayer(GameMap gameMap, List<Node> nodesToAvoid2, Player player) {
        Player nearPlayer = getNearPlayer(gameMap, player);
        if (nearPlayer == null) return null;
        return PathUtils.getShortestPath(gameMap, nodesToAvoid2, player, nearPlayer, false);
    }

    private void handleAttackOtherPlayer(GameMap gameMap, Player player, List<Node> nodesToAvoid2) throws IOException {
        String pathToPlayer = findPathToPlayer(gameMap, nodesToAvoid2, player);
        System.out.println("In ra hướng đi : " + pathToPlayer);
        if (pathToPlayer != null) {
            if (pathToPlayer.isEmpty()) {
                if (hero.getInventory().getGun() != null) {
                    hero.shoot(pathToPlayer);
                } else if (hero.getInventory().getMelee() != null) {
                    hero.attack(pathToPlayer);
                }
            } else {
                hero.move(pathToPlayer);
            }
        }
    }


}
