package dev.hermes.demo;

import dev.hermes.core.Dictionary;
import dev.hermes.core.TaleEngine;
import dev.hermes.core.VerseError;
import dev.hermes.core.WorldAPI;

import java.nio.file.Path;
import java.util.List;

/**
 * A command-line way to run Hermes scripts without Minecraft.
 *
 * <pre>
 *   java -jar Hermes-core.jar examples/game.Hermes        # just parse + check
 *   java -jar Hermes-core.jar --demo examples/game.Hermes # run a scripted demo
 * </pre>
 *
 * The demo plays a little story against the MockWorld: a player joins,
 * mines diamond ore, touches water, night falls, and time passes â€” so
 * every trigger in a typical script gets a chance to respond.
 */
public final class Console {

    private Console() {}

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Hermes console â€” run Hermes scripts without Minecraft.");
            System.out.println();
            System.out.println("Usage:");
            System.out.println("    java -jar Hermes-core.jar <script.Hermes>");
            System.out.println("    java -jar Hermes-core.jar --demo <script.Hermes>");
            System.out.println();
            System.out.println("Without --demo, the script is loaded and checked. With");
            System.out.println("--demo, a scripted story plays out so you can see the");
            System.out.println("script respond to events.");
            return;
        }

        boolean demo = false;
        String file = args[0];
        if (file.equals("--demo")) {
            demo = true;
            if (args.length < 2) {
                System.out.println("Which script should I demo?");
                return;
            }
            file = args[1];
        }

        MockWorld world = new MockWorld();
        MockWorld.MockScheduler scheduler = new MockWorld.MockScheduler();
        TaleEngine engine = new TaleEngine(world, scheduler);

        if (!engine.load(Path.of(file))) {
            for (VerseError e : engine.loadErrors()) {
                System.out.println(e.pretty(file));
                System.out.println();
            }
            System.out.println("The script was not loaded.");
            return;
        }

        if (!demo) {
            System.out.println("Loaded " + file + " â€” no errors. Registered triggers:");
            engine.eventCounts().forEach((event, count) ->
                    System.out.println("    when " + event + "  (" + count + " handler" + (count == 1 ? "" : "s") + ")"));
            System.out.println("State triggers: " + engine.scripts().stream()
                    .mapToInt(s -> (int) s.script.body.stream()
                            .filter(w -> w instanceof dev.hermes.core.Ast.WhenBlock wb && wb.trigger.kind == dev.hermes.core.Ast.Trigger.Kind.STATE)
                            .count()).sum());
            return;
        }

        // ------------------------------------------------------------
        // the demo story
        // ------------------------------------------------------------
        System.out.println("=== Hermes demo: " + file + " ===");
        System.out.println();

        MockWorld.MockPlayer p1 = world.addPlayer("PlayerOne");
        p1.location = new WorldAPI.Vec3(8, 64, 8);

        System.out.println("[story] PlayerOne joins the world");
        engine.playerEvent("joins", p1);

        System.out.println("[story] PlayerOne breaks a diamond ore block");
        engine.vars.setPlayer("PlayerOne", "coins", dev.hermes.core.Value.number(95));
        engine.playerEventBlock("breaks", p1, "diamond ore");

        System.out.println("[story] The engine checks the situation (player has 100 coins? touching water? night?)");
        engine.tick();

        System.out.println("[story] PlayerOne walks into water");
        world.putBlockAt(p1.location, "water");
        engine.tick();
        engine.tick(); // edge-triggered: only fires once

        System.out.println("[story] Night falls");
        world.setTime("night");
        engine.tick();

        System.out.println("[story] PlayerOne types \"home\"");
        engine.playerEventText("types", p1, "home");

        System.out.println("[story] 12 seconds pass (every 10 seconds trigger)");
        scheduler.advance(12_000);

        System.out.println("[story] PlayerOne gets a custom event");
        engine.fireCustomEvent("boss_killed");

        System.out.println();
        System.out.println("=== what happened ===");
        for (String line : world.log) {
            System.out.println("  " + line);
        }
        System.out.println();
        if (!p1.messages.isEmpty()) {
            System.out.println("PlayerOne heard:");
            for (String m : p1.messages) System.out.println("    " + m);
        }
        if (!p1.warnings.isEmpty()) {
            System.out.println("PlayerOne was warned:");
            for (String m : p1.warnings) System.out.println("    " + m);
        }
        if (!world.broadcastLog.isEmpty()) {
            System.out.println("The world heard:");
            for (String m : world.broadcastLog) System.out.println("    " + m);
        }
        if (!world.mobs.isEmpty()) {
            System.out.println("Mobs in the world:");
            for (MockWorld.MockMob m : world.mobs) {
                System.out.println("    " + m.type + (m.customName != null ? " named \"" + m.customName + "\"" : "")
                        + " at " + m.location.x() + ", " + m.location.y() + ", " + m.location.z());
            }
        }
        System.out.println();
        System.out.println("PlayerOne stats: health=" + p1.health + " coins="
                + engine.vars.getPlayer("PlayerOne", "coins").display()
                + " inventory=" + p1.inventory);
        System.out.println();
        System.out.println("Done. Now install the Paper plugin and this same script runs in Minecraft.");
        System.out.println("Check: " + Dictionary.canonicalItems().size() + " items known by Hermes.");
    }
}
