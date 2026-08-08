# Hermes

**A Skript-like scripting language for Paper servers. Write plain-text `.her` scripts — no Java, no plugin per feature.**

```hermes
when player joins
    welcome player with "Welcome to the server!"
    give player 1 bread
    set player's coins to 0

when player breaks diamond ore
    add 5 to player's coins
    announce "Someone mined a diamond!"

when player has 100 coins
    tell player "You reached 100 coins!"
    give player 1 netherite ingot

command "/kit" with argument <name>
    permission "kits.vip"
    give player 1 diamond
    tell player "Kit ${name} claimed!"
```

Hermes is an original, from-scratch implementation of Skript-style ideas — fast to learn, full of guard-rails, and free of Skript code.

---

## Features

| What | Examples |
| --- | --- |
| Event triggers | `when player joins`, `when player breaks diamond ore`, `when zombie dies`, `when player first joins` |
| State triggers | `when player is flying`, `when it is nighttime`, `when player has 100 coins`, `when player is in region "Castle"` |
| Custom commands | `command "/pay" with argument <amount>` with optional `permission "..."` |
| Reusable actions | `action reward the player` ... then call `reward the player` anywhere |
| Custom events | `fire event "boss_killed"` / `when custom event "boss_killed" fires` |
| Variables | player, world, scoreboard, lists, loop variables — saved across restarts |
| Loops | lists, all online players, numbers, player inventory |
| Timers | `every 5 seconds` |
| Regions & marks | `region "Castle" from 10 64 10 to 40 80 40`, `mark home at 100 64 200` |
| World | weather, time, blocks, signs, doors, buttons, levers, chests, mobs, particles, sounds, lightning, explosions |
| Players | teleport, damage, heal, feed, effects/potions, gamemode, XP, titles, actionbars, scoreboards, teams |
| Flow | `if / else`, `repeat N times`, `stop`, `and` / `or` / `not` conditions |
| Text interpolation | `tell player "You have ${player's coins} coins!"` |

## Why Hermes instead of Skript?

- **One jar, zero dependencies** — no Skript, no addons to install.
- **Clear errors** — when a line is wrong, Hermes tells you what it expected and shows a working example.
- **Everything is testable** — the parser ships with a 74-test suite; docs are verified against the real grammar.
- **Easy to extend** — add new actions, conditions and triggers by reading the source.

## Installation

1. Requires **Paper 1.21+** (Spigot is not supported) and **Java 21+**.
2. Drop `Hermes.jar` into your server's `plugins/` folder and restart.
3. Hermes creates `plugins/Hermes/hermes/` — put your `.her` scripts there.
4. Run `/hermes reload` after editing a script.

## Documentation

Full docs are generated into `plugins/Hermes/help/` on first run (variables, triggers, commands, loops, scoreboards, regions, effects, timers, world, chests & GUIs). Also in the [README](https://github.com/ftbxx1/Hermes#readme) and [examples](https://github.com/ftbxx1/Hermes/tree/master/examples).

## Links

- Source & releases: <https://github.com/ftbxx1/Hermes>
- Issues: <https://github.com/ftbxx1/Hermes/issues>
- License: MIT

Not affiliated with Mojang, Microsoft, or SkriptLang.
