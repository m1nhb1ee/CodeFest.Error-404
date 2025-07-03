import io.socket.emitter.Emitter;
import jsclub.codefest.sdk.Hero;


import java.io.IOException;


public class Main {
    private static final String SERVER_URL = "https://cf25-server.jsclub.dev";
    private static final String GAME_ID = "129210";
    private static final String PLAYER_NAME = "ERROR404";
    private static final String SECRET_KEY = "sk-TN9xLbiuTbyZXILhvyWJbw:skQHC0vsqGEWmjjNlB_mLLiRl1z-BUJf_OjRgcRWtGoWpxTBp9hvQ-0qqmD3BZCppSfa8wHyysKVkG5j06qwzQ";


    public static void main(String[] args) throws IOException {
        Hero hero = new Hero(GAME_ID, PLAYER_NAME, SECRET_KEY);
        Emitter.Listener onMapUpdate = new MapUpdateListener(hero);

        hero.setOnMapUpdate(onMapUpdate);
        hero.start(SERVER_URL);
    }
}

