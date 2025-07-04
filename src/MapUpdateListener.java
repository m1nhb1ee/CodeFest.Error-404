import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.Element;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.npcs.Enemy;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
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

            List<Obstacle> chestList = gameMap.getObstaclesByTag("DESTRUCTIBLE");


            if (player == null || player.getHealth() == 0) return;

            Weapon myGun = hero.getInventory().getGun();
            Weapon myMelee = hero.getInventory().getMelee();
            Weapon myThrowable = hero.getInventory().getThrowable();


            List<Node> avoid = getNodesToAvoid(gameMap);
            List<Node> avoid2 = getNodesToAvoid2(gameMap);

            //lay sung truoc tien, co sung roi thi bot gan thi danh bot truoc, ruong gan thi nhat ruong truoc

            if (myGun == null) {
                handleSearchForGun(gameMap, player, avoid);
                return;
            }

            if (hasUsableWeapon()) {
                //lay sung truoc tien, co sung roi thi bot gan thi danh bot truoc, ruong gan thi nhat ruong truoc
                Player nearestEnemy = getNearestPlayer(gameMap, player);
                Obstacle nearestChest = getNearestChest(gameMap, player);
                Node nearestChestNode = new Node(nearestChest.getX(), nearestChest.getY());
                Node nearestEnemyNode = new Node(nearestEnemy.getX(), nearestEnemy.getY());
                Node myPos = new Node(player.getX(), player.getY());
                int distChest = PathUtils.distance(myPos, nearestChestNode);
                int distEnemy = PathUtils.distance(myPos, nearestEnemyNode);
                if (distChest < distEnemy) {
                    int range = findRangeWeapon(hero.getInventory().getGun());
                    int range2 = findRangeWeapon(hero.getInventory().getMelee());
                    int range3 = findRangeWeapon(hero.getInventory().getThrowable());
                    System.out.println(range);
                    System.out.println(range2);
                    System.out.println(range3);
                    if (distChest <= range) {
                        String dir = getDirection(myPos, nearestChestNode);
                        if (dir != null) {
                            if (hero.getInventory().getGun() != null) {
                                hero.shoot(dir);
                            } else if (hero.getInventory().getMelee() != null) {
                                hero.attack(dir);
                            }
                        }
                    } else {
                        String path = PathUtils.getShortestPath(gameMap, avoid, myPos, nearestChestNode, true);
                        if (!path.isEmpty()) {
                            hero.move(path);
                        }
                    }

                } else {
                    handleAttackEnemy(gameMap, player, avoid2);
                }

            }


        } catch (Exception e) {
            System.err.println("Critical error in call method: " + e.getMessage());
            e.printStackTrace();
        }
    }

    //kiếm tra điều kiện có vũ khí và vũ khí còn số lần sử dụng
    private boolean hasUsableWeapon() {
        return (hero.getInventory().getGun() != null && hero.getInventory().getGun().getUseCount() > 0) || (hero.getInventory().getMelee() != null && hero.getInventory().getMelee().getUseCount() > 0) || (hero.getInventory().getThrowable() != null && hero.getInventory().getThrowable().getUseCount() > 0);
    }

    //lấy danh sách các vật cản phải tránh khi đi tìm súng, bao gồm: vật cản không thể đi qua, rương, người chơi khác, quái
    private List<Node> getNodesToAvoid(GameMap gameMap) {
        List<Node> nodes = new ArrayList<>(gameMap.getListIndestructibles());
        nodes.removeAll(gameMap.getObstaclesByTag("CAN_GO_THROUGH"));
        nodes.addAll(gameMap.getObstaclesByTag("DESTRUCTIBLE"));
        nodes.addAll(gameMap.getOtherPlayerInfo());
        nodes.addAll(gameMap.getListEnemies());
        return nodes;
    }

    //lấy danh sách các vật cản phải tránh khi đi tìm người chơi khác, bao gồm: vật cản không thể đi qua, rương, quái
    private List<Node> getNodesToAvoid2(GameMap gameMap) {
        List<Node> nodes = new ArrayList<>(gameMap.getListIndestructibles());
        nodes.removeAll(gameMap.getObstaclesByTag("CAN_GO_THROUGH"));
        nodes.addAll(gameMap.getObstaclesByTag("DESTRUCTIBLE"));
        nodes.addAll(gameMap.getListEnemies());
        return nodes;
    }


    //hàm tìm vũ khí gần nhất so với vị trí hiện tại
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

    //hàm tìm ruong gần nhất so với vị trí hiện tại
    private Obstacle getNearestChest(GameMap gameMap, Player player) {
        List<Obstacle> listChest = gameMap.getObstaclesByTag("DESTRUCTIBLE");
        Obstacle nearestChest = null;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < listChest.size(); i++) {
            double distance = PathUtils.distance(player, listChest.get(i));
            if (distance < minDistance) {
                minDistance = distance;
                nearestChest = listChest.get(i);
            }
        }
        return nearestChest;
    }

    //  tìm người chơi khác gần nhất so với vị trí hiện tại
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

    //hàm đi tìm vũ khí bằng cách tìm vũ khí gần nhất so với vị trí hiện tại,
    //tìm đường đi gần nhất đến vũ khí đó và gọi hàm move để đi tìm
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


    // hàm tìm tọa độ của người chơi khác và trả về hướng đi của người chơi khác
    private String getDirection(Node from, Node to) {
        if (from.getX() == to.getX()) {
            // cùng cột → bắn lên hoặc xuống
            return (to.getY() > from.getY()) ? "u" : "d";
        }
        if (from.getY() == to.getY()) {
            // cùng hàng → bắn phải hoặc trái
            return (to.getX() > from.getX()) ? "r" : "l";
        }
        return null; // không cùng hàng hoặc cột → không bắn được
    }


    //  hàm tìm phạm vi sử dụng vũ khí
    private int findRangeWeapon(Weapon weapon) {
        if (weapon == null || weapon.getRange() == null) return 0;
        int range = 1;
        int[] rangeWeapon = weapon.getRange();
        for (int i = 0; i < rangeWeapon.length; i++) {
            range *= rangeWeapon[i];
        }
        return range;
    }

    //    hàm tấn công người chơi khác
    private void handleAttackEnemy(GameMap gameMap, Player player, List<Node> avoid) throws IOException {
        Player nearestEnemy = getNearestPlayer(gameMap, player);
        if (nearestEnemy != null) {
            Node enemyPos = new Node(nearestEnemy.getX(), nearestEnemy.getY());
            Node myPos = new Node(player.getX(), player.getY());
            int dist = PathUtils.distance(myPos, enemyPos);
            int range = findRangeWeapon(hero.getInventory().getGun());
            System.out.println(range);
            if (dist <= range) {
                String dir = getDirection(myPos, enemyPos);
                if (dir != null) {
                    if (hero.getInventory().getGun() != null) {
                        hero.shoot(dir);
                    } else if (hero.getInventory().getMelee() != null) {
                        hero.attack(dir);
                    }
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
