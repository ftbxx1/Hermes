# Hermes

[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Paper](https://img.shields.io/badge/Paper-1.21%2B-orange.svg)](#requirements)
[![Java](https://img.shields.io/badge/Java-21-blue.svg)](#compiling)
[![Build](https://github.com/ftbxx1/Hermes/actions/workflows/build.yml/badge.svg)](https://github.com/ftbxx1/Hermes/actions/workflows/build.yml)

**Hermes** is a Minecraft plugin for Paper which lets server owners script
their server with plain-text `.her` files — no Java required, and no plugin
per feature. It can also be useful if you *do* know Java: some tasks are
quicker to script than to code, and Hermes makes a great prototyping tool.

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

## Requirements

Hermes requires **Paper** to work. You heard it right — **Spigot** does *not*
work. Hermes also requires **Java 21** or newer.

## Download

You can find the downloads for each version with their release notes on the
[releases page](https://github.com/ftbxx1/Hermes/releases). You may also
build the latest source yourself — see [Compiling](#compiling) below.

1. Put `Hermes.jar` into your server's `plugins/` folder.
2. Start the server. Hermes creates `plugins/Hermes/hermes/` — put your
   `.her` scripts there.
3. Run `/hermes reload` (or restart).

## Documentation

The in-game docs are generated into `plugins/Hermes/help/variables.md` on
first run. The same content (variables, conditions, values, loops, commands)
is also in the [README](README.md) feature table below.

### What you can script

| Feature | Example |
| --- | --- |
| Event triggers | `when player joins`, `when player breaks diamond ore`, `when zombie dies`, `when player types "home"`, `when server starts` / `stops` |
| First joins | `when player first joins` — runs once per player, ever |
| State triggers | `when player is flying`, `when it is nighttime`, `when player has 100 coins` |
| Timers | `every 5 seconds` |
| Custom commands | `command "/pay" with argument <amount> and argument <target>` with optional `permission "..."` |
| Reusable actions | `action greet <name> the player` ... `greet "Steve" the player` |
| Loops | `loop over list "quests" as task`, `loop over all players as p`, `loop over numbers from 1 to 10 as i`, `loop over player's inventory as item`, `while player's health is above 5` |
| Delays | `wait 3 seconds` / `wait 20 ticks` — pauses the rest of the block |
| Math | `set player's coins to 3 plus 4 times 2` — `plus`, `minus`, `times`, `divided by` |
| Player stats | `set player's health to 20`, `set player's food to 10`, `set player's experience to 5`, `set player's level to 10` |
| Bossbars | `set player's bossbar to "The Queen" with progress 50`, `clear player's bossbar` |
| Item checks | `player's held item` as a value |
| Text interpolation | `tell player "You have ${player's coins} coins!"` |
| Variables | player, world, scoreboard, lists, loop variables — saved to `state.txt` on shutdown |
| Global variables | `world's flag` or `global "flag"` — one value for the whole server |
| Functions | `function "tax" with argument <amount>` ... `return <value>` — reusable, value-returning helpers callable in any expression |
| Effects | teleport, damage, heal, feed, give/remove items, potions, title/actionbar, sounds, particles, lightning, explosions, launch, gamemode, XP, scoreboards, teams, permissions |
| World | weather, time, regions, marks, signs, doors, buttons, levers, chests, blocks, mobs |
| Flow | `if / else`, `repeat 5 times`, `while`, `stop`, custom events (`fire event "boss_killed"`) |
| Forgiving English | case-insensitive keywords, filler words (`the`, `a`, `an`), and plain-English synonyms — `say player` = `tell player`, `grant` = `give`, `tp` = `teleport`, `each` = `every`, `broadcast` = `announce`, ... |
| Multi-language | scripts can be written in 45+ languages including Spanish, French, German, Russian, Arabic, Chinese, Japanese, Hindi, and more (see below) |

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

## Writing scripts in your language

Hermes keywords and events are written in English, but a translation layer lets
you write scripts in your own language:

```
cuando jugador se une
    dar jugador 1 pan
    poner jugador's monedas a 0

every 5 seconds
    anunciar "¡Hola a todos!"
```

Built-in languages (set `language` in `config.yml`, then `/hermes reload`):

| Region | `language` codes |
| --- | --- |
| Europe | `en`, `es`, `pt`, `fr`, `it`, `de`, `nl`, `ca`, `gl`, `pl`, `cs`, `sk`, `ru`, `uk`, `bg`, `hr`, `sr`, `sl`, `sv`, `da`, `no`, `fi`, `et`, `lt`, `lv`, `hu`, `ro`, `el`, `tr`, `is` |
| Middle East | `ar`, `he`, `fa`, `ur` |
| Asia | `zh`, `ja`, `ko`, `hi`, `bn`, `ta`, `te`, `id`, `ms`, `th`, `vi`, `fil` |
| Americas | covered by `en`, `es`, `fr`, `pt` |

```yaml
language: en
```

You can also add your own language: drop a `yourlang.lang` file into
`plugins/Hermes/lang/` (one mapping per line, `english=native`), set
`language: yourlang`, and reload. Anything the pack doesn't translate stays
in English, so your script can mix both freely.

## Reporting Issues

Please see our [contribution guidelines](.github/contributing.md) before
reporting issues. Use the [issue templates](.github/ISSUE_TEMPLATE/) so we
can reproduce problems quickly.

## Compiling

Hermes uses Maven for compilation:

```
mvn package
```

The plugin jar lands at `target/Hermes.jar`. Run the test suite with:

```
mvn test
```

## Contributing

Please review our [contribution guidelines](.github/contributing.md).

## Maven Repository

If you use Hermes as a (soft) dependency for your plugin, add JitPack and the
dependency:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependency>
    <groupId>com.github.ftbxx1</groupId>
    <artifactId>Hermes</artifactId>
    <version>2.4.1</version>
    <scope>provided</scope>
</dependency>
```

## Relevant Links

- [Releases](https://github.com/ftbxx1/Hermes/releases)
- [Issues](https://github.com/ftbxx1/Hermes/issues)
- [In-game docs: help/variables.md](src/main/resources/help/variables.md)

## Developers

You can find all contributors
[here](https://github.com/ftbxx1/Hermes/graphs/contributors).

## License

Licensed under the [MIT License](LICENSE) © 2026 ftbxx1. See [LICENSE](LICENSE)
for the full text.

**Not affiliated with Mojang or Microsoft.** Minecraft is a trademark of
Mojang Synergies AB / Microsoft. **Not affiliated with SkriptLang** — Hermes
is an original implementation of Skript-style ideas and contains no Skript
code.
