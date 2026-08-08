# Hermes — a scripting language inside Minecraft

> This is the plugin's README — see the repo root [README](../README.md) for
> the full documentation, legal info, and license.

Hermes is a Skript-like language for Paper servers. Scripts are plain-text
`.her` files — no plugins per feature. Write a trigger, drop it in the
scripts folder, run `/hermes reload`.

Not affiliated with Mojang/Microsoft or SkriptLang. MIT licensed.

## Install

1. Put `Hermes.jar` (from `plugin/target/`) into your server's `plugins/` folder.
2. Start the server. Hermes creates `plugins/Hermes/hermes/` — put your
   `.her` scripts there.
3. Run `/hermes reload` (or restart).

Requires Paper 1.21+.

## Commands

| Command | What it does |
| --- | --- |
| `/hermes reload` | Reload all scripts |
| `/hermes scripts` | List loaded scripts |
| `/hermes run <file>` | Load one script |
| `/hermes events` | List registered triggers |
| `/hermes vars` | Show variable counts |

All commands need `hermes.admin` (default: operators).

## Writing scripts

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

when player types "home"
    teleport player to home
    play sound "level up" at player

when it is nighttime
    spawn 3 zombies at player

when zombie dies
    announce "A zombie was slain!"

when player enters region "Arena"
    damage player by 1
```

Variables (player, world, scoreboard, lists and loop variables) are
documented in `plugins/Hermes/help/variables.md` — created automatically
on the first run.

### Triggers the plugin bridges

| Hermes trigger | Minecraft event |
| --- | --- |
| `when player joins / leaves / dies / respawns` | join, quit, death, respawn |
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

Regions come from `region "Arena" at 10 64 10 to 40 64 40` in a script, marks
from `mark home at 100 64 200`.

### Statements beyond the basics

```
feed player by 5                  # fill hunger
clear player's inventory
kick player because "Banned!"
lightning at player               # or: lightning at 10 64 20
explode at player with power 2
launch player by 5                # upward velocity
title player "Boss fight!" with subtitle "Survive!"
actionbar player "Press E to interact"
set player's gamemode to creative # survival / adventure / spectator
spawn particles "flame" near player
delete list "quests"
```

### Your own commands — `command`

Scripts can register real player commands:

```
command "/kit" with argument <name>
    permission "kits.vip"
    give player 1 diamond
    tell player "Kit ${name} claimed!"

command "/pay" with argument <amount> and argument <target>
    tell player "You paid ${amount} to ${target}!"
```

`with argument <name>` binds the typed argument as a temporary variable
(see `${...}` above), `permission "..."` is optional and limits who can use
the command. The command appears on the server the moment the script loads.

### Reusable actions with arguments

```
action greet <name> the player
    tell player "Hi ${name}!"

when player joins
    greet "Steve" the player
```

### Loops over everything

```
loop over list "quests" as task
    tell player "TODO: ${task}"

loop over all players as p
    give player 1 bread

loop over numbers from 1 to 10 as i
    tell player "Counting ${i}..."

loop over player's inventory as item
    tell player "You have ${item}"
```

### First joins

`when player first joins` runs only the first time a player ever joins the
server (tracked per player, survives restarts):

```
when player first joins
    give player 1 diamond sword
    announce "${player's name} joined for the first time!"
```

Values: `player's name`, `player's world`, `player's x/y/z`,
`player's gamemode`, `random number between 1 and 10`,
`number of players`, `count of "diamond" in player's inventory`.

Conditions: `player is sneaking / flying / wet / on the ground / op`,
`player is in creative mode`.

## How it's built

- `hermes-core` — the language: lexer, parser, dictionary, engine. No
  Minecraft dependency; runs anywhere (tests use a headless MockWorld).
- `hermes-plugin` — the Paper bridge: `BukkitWorld` implements the `WorldAPI`,
  `HermesListener` turns Bukkit events into triggers, `HermesPlugin` loads
  scripts and runs the engine.

## Building

```
mvn package
```

The plugin jar lands at `plugin/target/Hermes.jar`.
