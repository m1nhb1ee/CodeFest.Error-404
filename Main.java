import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;
import jsclub.codefest.sdk.algorithm.PathUtils;
import jsclub.codefest.sdk.base.Node;
import jsclub.codefest.sdk.model.GameMap;
import jsclub.codefest.sdk.model.obstacles.Obstacle;
import jsclub.codefest.sdk.model.players.Player;
import jsclub.codefest.sdk.model.weapon.Weapon;
import jsclub.codefest.sdk.model.Inventory;
import jsclub.codefest.sdk.model.equipments.HealingItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "116323";
    private static final String PLAYER_NAME = "KOver";
    private static final String SECRET_KEY = "sk-jRQqh61dSy-pq4q8Kaz_WA:kCfJPiZVKJOydwSswgC-XOuGdRYVKh6xdjqzkqjAWOLeYMiV9tMpm5gnLDkhcRbIiHhjaYzmqTHa-6BkDo5uUw";

    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME, SECRET_KEY);
        hero.setOnMapUpdate(new MapUpdateListener(hero));
        hero.start(SERVER_URL);
    }
}

class MapUpdateListener implements Emitter.Listener {
    private final Hero hero;

    public MapUpdateListener(Hero hero) {
        this.hero = hero;
    }

    @Override
    public void call(Object... args) {
        try {
            if (args == null || args.length == 0) return;

            GameMap gameMap = hero.getGameMap();
            gameMap.updateOnUpdateMap(args[0]);
            Player me = gameMap.getCurrentPlayer();
            if (me == null || me.getHealth() == 0) return;

            Node myPos = new Node(me.getX(), me.getY());
            List<Node> avoid = getAvoidNodes(gameMap);

            Weapon currentGun = hero.getInventory().getGun();
            boolean needGun = (currentGun == null || currentGun.getUseCounts() <= 0);

            // Nếu không có vũ khí hoặc hết đạn → đi nhặt
            if (needGun) {
                Weapon nearestGun = getNearestWeapon(gameMap.getAllGun(), myPos);
                if (nearestGun != null) {
                    Node gunPos = new Node(nearestGun.getX(), nearestGun.getY());
                    String path = PathUtils.getShortestPath(gameMap, avoid, myPos, gunPos, true);
                    if (path.isEmpty()) {
                        hero.pickupItem();
                    } else {
                        hero.move(path);
                    }
                    return;
                }
            }

            // Có súng → tìm đối thủ
            Player nearestEnemy = getNearestEnemy(gameMap.getOtherPlayerInfo(), myPos);
            if (nearestEnemy != null) {
                Node enemyPos = new Node(nearestEnemy.getX(), nearestEnemy.getY());
                int dist = PathUtils.distance(myPos, enemyPos);

                if (dist == 1) {
                    String dir = getDirection(myPos, enemyPos);
                    hero.shoot(dir);
                } else {
                    String path = PathUtils.getShortestPath(gameMap, avoid, myPos, enemyPos, true);
                    if (!path.isEmpty()) {
                        hero.move(path);
                    }
                }
                return;
            }

            // Không có đối thủ → tiếp tục đi nhặt súng
            Weapon anotherGun = getNearestWeapon(gameMap.getAllGun(), myPos);
            if (anotherGun != null) {
                Node gunPos = new Node(anotherGun.getX(), anotherGun.getY());
                String path = PathUtils.getShortestPath(gameMap, avoid, myPos, gunPos, true);
                if (path.isEmpty()) {
                    hero.pickupItem();
                } else {
                    hero.move(path);
                }
            }

        } catch (Exception e) {
            System.err.println("Error in bot logic: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<Node> getAvoidNodes(GameMap gameMap) {
        List<Node> avoid = new ArrayList<>();
        for (Obstacle obs : gameMap.getListIndestructibles()) {
            avoid.add(new Node(obs.getX(), obs.getY()));
        }
        return avoid;
    }

    private Weapon getNearestWeapon(List<Weapon> weapons, Node myPos) {
        Weapon nearest = null;
        int minDist = Integer.MAX_VALUE;
        for (Weapon w : weapons) {
            int d = PathUtils.distance(myPos, new Node(w.getX(), w.getY()));
            if (d < minDist) {
                minDist = d;
                nearest = w;
            }
        }
        return nearest;
    }

    private Player getNearestEnemy(List<Player> players, Node myPos) {
        Player nearest = null;
        int minDist = Integer.MAX_VALUE;
        for (Player p : players) {
            int d = PathUtils.distance(myPos, new Node(p.getX(), p.getY()));
            if (d < minDist) {
                minDist = d;
                nearest = p;
            }
        }
        return nearest;
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
}