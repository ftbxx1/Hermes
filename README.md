# Hermes

A Skript-like scripting language for Paper servers. Write plain-text `.her`
files, drop them in the scripts folder, and Hermes turns them into real
gameplay: triggers, commands, loops, variables, and everything in between —
no plugins per feature.

```
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

## Important legal note

- **Not affiliated with Mojang or Microsoft.** Minecraft is a trademark of
  Mojang Synergies AB / Microsoft, and this project is not endorsed by them.
- **Not affiliated with SkriptLang.** Hermes is an original implementation of
  Skript-style *ideas* (event-driven scripting of a server). It contains no
  Skript code. Skript itself is MIT-licensed by Peter Güttinger et al.
- This project is licensed under the **MIT License** (see [LICENSE](LICENSE)).

## Install

1. Put `Hermes.jar` (from `target/`) into your server's `plugins/` folder.
2. Start the server. Hermes creates `plugins/Hermes/hermes/` — put your
   `.her` scripts there.
3. Run `/hermes reload` (or restart).

Requires Paper 1.21+.

## What you can script

| Feature | Example |
| --- | --- |
| Event triggers | `when player joins`, `when player breaks diamond ore`, `when zombie dies`, `when player types "home"` |
| First joins | `when player first joins` — runs once per player, ever |
| State triggers | `when player is flying`, `when it is nighttime`, `when player has 100 coins` |
| Timers | `every 5 seconds` |
| Custom commands | `command "/pay" with argument <amount> and argument <target>` with optional `permission "..."` |
| Reusable actions | `action greet <name> the player` ... `greet "Steve" the player` |
| Loops | `loop over list "quests" as task`, `loop over all players as p`, `loop over numbers from 1 to 10 as i`, `loop over player's inventory as item` |
| Text interpolation | `tell player "You have ${player's coins} coins!"` |
| Variables | player, world, scoreboard, lists, and loop variables — saved to `state.txt` on shutdown |
| Effects | teleport, damage, heal, feed, give/remove items, potions, title/actionbar, sounds, particles, lightning, explosions, launch, gamemode, XP, scoreboards, teams, permissions |
| World | weather, time, regions, marks, signs, doors, buttons, levers, chests, blocks, mobs |
| Flow | `if / else`, `repeat 5 times`, `stop`, custom events (`fire event "boss_killed"`) |

Full docs (variables, conditions, values) are generated into
`plugins/Hermes/help/variables.md` on first run.

### Triggers the plugin bridges

| Hermes trigger | Minecraft event |
| --- | --- |
| `when player joins / leaves / dies / respawns` | join, quit, death, respawn |
| `when player first joins` | first join ever (tracked per player) |
| `when player breaks <block>` / `places <block>` | block break / place |
| `when player types "<text>"` / `chats` | chat message |
| `when player uses <item>` | right-click with an item |
| `when player interacts with <block>` | right-click on a block |
| `when player picks up <item>` / `drops <item>` | item pickup / drop |
| `when player eats <item>` | eating food / drinking |
| `when player fishes` | catching a fish |
| `when player levels up` | gaining an XP level |
| `when player kills <mob>` / `kills any mob` | killing a mob |
| `when player starts sprinting` / `stops sprinting` | sprint toggling |
| `when player attacks` / `takes damage` | dealing / taking damage |
| `when player jumps` / `sneaks` / `moves` | movement |
| `when player enters region "<name>"` | crossing into a region |
| `when <mob> dies / spawns / attacks` | mob lifecycle |
| `when it is nighttime` and other states | polled every 10 ticks |

## Commands

| Command | What it does |
| --- | --- |
| `/hermes reload` | Reload all scripts |
| `/hermes scripts` | List loaded scripts |
| `/hermes run <file>` | Load one script |
| `/hermes events` | List registered triggers |
| `/hermes vars` | Show variable counts |

All need `hermes.admin` (default: operators).

## How it's built

One project, two layers:

- The **language** — `dev.hermes.core`: lexer, parser, dictionary, interpreter,
  engine. No Minecraft dependency; runs anywhere (tests use a headless
  `MockWorld`, and `java -jar Hermes.jar` runs a demo console).
- The **Paper bridge** — `dev.hermes.plugin`: `BukkitWorld` implements the
  `WorldAPI`, `HermesListener` turns Bukkit events into triggers,
  `HermesPlugin` loads scripts and registers script commands.

## Building

```
mvn package
```

The plugin jar lands at `target/Hermes.jar`.

## License

[MIT](LICENSE) © 2026 ftbxx1. You are free to use, modify, and redistribute —
attribution required. The project is provided "as is", without warranty of any
kind; the author is not liable for anything that happens using it.
