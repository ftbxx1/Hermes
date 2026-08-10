package dev.hermes.core;

import dev.hermes.demo.MockWorld;
import dev.hermes.demo.MockWorld.MockPlayer;
import dev.hermes.demo.MockWorld.MockScheduler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end proof that scripts genuinely parse and run in the newly added
 * languages, not just that the packs load. Each test writes a real "when
 * player joins / give player 1 bread" script in that language and checks the
 * item actually lands in the inventory.
 */
class LangScriptTest {

    private static void run(String code, String script, String item) {
        Lang.setLanguage(code);
        try {
            MockWorld world = new MockWorld();
            MockScheduler scheduler = new MockScheduler();
            TaleEngine engine = new TaleEngine(world, scheduler);
            MockPlayer p1 = world.addPlayer("PlayerOne");
            assertTrue(engine.loadString(script, "lang.Hermes"),
                    code + " should load; errors: " + engine.loadErrors());
            engine.playerEvent("joins", p1);
            assertEquals(1, p1.inventory.getOrDefault(item, 0),
                    code + " should have given a " + item);
        } finally {
            Lang.setLanguage("en");
        }
    }

    @Test
    void portuguese() {
        run("pt", "quando jogador se junta\n    dar jogador 1 pão", "bread");
    }

    @Test
    void italian() {
        run("it", "quando giocatore si unisce\n    dare giocatore 1 pane", "bread");
    }

    @Test
    void russian() {
        run("ru", "когда игрок присоединяется\n    дай игрок 1 хлеб", "bread");
    }

    @Test
    void chinese() {
        run("zh", "当 玩家 加入\n    给予 玩家 1 面包", "bread");
    }

    @Test
    void japanese() {
        run("ja", "とき プレイヤー 参加する\n    渡す プレイヤー 1 パン", "bread");
    }

    @Test
    void hindi() {
        run("hi", "जब खिलाड़ी आता है\n    दो खिलाड़ी 1 रोटी", "bread");
    }

    @Test
    void bengali() {
        run("bn", "যখন খেলোয়াড় যোগ দেয়\n    দাও খেলোয়াড় 1 রুটি", "bread");
    }

    @Test
    void tamil() {
        run("ta", "போது வீரர் சேர்கிறான்\n    கொடு வீரர் 1 ரொட்டி", "bread");
    }

    @Test
    void telugu() {
        run("te", "అప్పుడు ఆటగాడు చేరాడు\n    ఇవ్వు ఆటగాడు 1 రొట్టె", "bread");
    }

    @Test
    void arabic() {
        run("ar", "عندما لاعب ينضم\n    اعط لاعب 1 خبز", "bread");
    }

    @Test
    void polish() {
        run("pl", "kiedy gracz dołącza\n    daj gracz 1 chleb", "bread");
    }

    @Test
    void indonesian() {
        run("id", "ketika pemain bergabung\n    beri pemain 1 roti", "bread");
    }

    @Test
    void turkish() {
        run("tr", "eğer oyuncu katılıyor\n    ver oyuncu 1 ekmek", "bread");
    }

    @Test
    void spanishGiveAllPlayers() {
        Lang.setLanguage("es");
        try {
            MockWorld world = new MockWorld();
            MockScheduler scheduler = new MockScheduler();
            TaleEngine engine = new TaleEngine(world, scheduler);
            MockPlayer p1 = world.addPlayer("PlayerOne");
            MockPlayer p2 = world.addPlayer("PlayerTwo");
            assertTrue(engine.loadString("""
                    cuando jugador se une
                        dar todos jugadores 1 pan
                    """, "lang.Hermes"),
                    "es should load; errors: " + engine.loadErrors());
            engine.playerEvent("joins", p1);
            assertEquals(1, p1.inventory.getOrDefault("bread", 0));
            assertEquals(1, p2.inventory.getOrDefault("bread", 0));
        } finally {
            Lang.setLanguage("en");
        }
    }
}
