# Hermes variables — the complete guide

Hermes has **five kinds of variables**. Every one of them is a word, a
quoted name, or a scoreboard objective. There are no types to declare:
numbers, "text", true/false and lists all live in the same place.

| Kind | Example | Lives | Shared by |
| --- | --- | --- | --- |
| Player | `player's coins` | per player | one player |
| World | `world's flag` | the server | everyone |
| Score | `player's score "kills"` | scoreboard | the server |
| List | `list "quests"` | the server | everyone |
| Temporary | `task` (from a loop) | one trigger run | the loop body |

---

## 1. Player variables — `player's <name>`

Each player has their own copy. `PlayerOne`'s coins and `PlayerTwo`'s coins
are completely separate.

```
set player's coins to 0
add 5 to player's coins
add 100 to player's coins
remove 2 from player's coins
set player's coins to 7

when player joins
    set player's coins to 0

when player breaks diamond ore
    add 5 to player's coins
```

Special player values you can read anywhere:

| Value | What it is |
| --- | --- |
| `player's health` | hearts, 0–20 |
| `player's hunger` | food, 0–20 |
| `player's xp` | experience, 0–100 |
| `player's level` | XP level |

Examples:

```
when player's health is below 5
    warn player "You are almost dead!"

when player's level is at least 10
    give player 1 diamond
```

---

## 2. World variables — `world's <name>`

One value for the whole server, shared by every player and every script.

```
set world's flag to true
add 1 to world's kills
set world's greeting to "Welcome back!"

when world's flag is true
    announce "The event has started!"
```

Common uses: server-wide events, minigame state, quest progress shared
between players, counters that survive restarts (Hermes saves variables to
a file on shutdown).

---

## 3. Score variables — `player's score "<name>"`

Real Minecraft scoreboard objectives. Visible with the scoreboard command,
persisted by the scoreboard, and usable in Hermes conditions.

```
set player's score "kills" to 0
add 5 to player's score "kills"

when player kills a mob
    add 1 to player's score "kills"

when player's score "kills" is above 50
    announce "A hunter has passed 50 kills!"
```

---

## 4. Lists — `list "<name>"`

An ordered collection of numbers, texts, or truths. Lists are shared by
everyone, like world variables.

### Creating and filling

```
create list "quests"
add "Find the key" to list "quests"
add "Defeat the dragon" to list "quests"
add 10 to list "points"
```

### Reading them

```
tell player length of list "quests"
loop over list "quests" as task
    tell player task
```

Every item of the list appears in the loop, one at a time, as a temporary
variable named after the `as <name>`.

### Conditions

```
if list "quests" contains "Find the key"
    tell player "That quest is still open!"
```

### Removing and clearing

```
remove "Defeat the dragon" from list "quests"
clear list "quests"
```

---

## 5. Temporary variables — from loops

When a loop runs, each item is placed in a temporary variable you choose
with `as <name>`. It only exists inside that loop.

```
loop over list "quests" as task
    tell player "Next up: "
    tell player task
```

If the list holds text, `task` is text; if it holds numbers, `task` is a
number.

---

## Conditions on variables

Every variable can be compared with the obvious words:

| Word | Meaning |
| --- | --- |
| `is 5` / `is equal to 5` | equals |
| `is above 5` / `is more than 5` | greater than |
| `is below 5` / `is less than 5` | less than |
| `is at least 5` | greater or equal |
| `is at most 5` | less or equal |
| `is not equal to 5` | different |
| `is true` / `is false` | booleans |
| `contains "<text>"` | lists |
| `has 5 diamonds` | items in the inventory |

Examples:

```
when player's coins is above 100
    announce "Someone got rich!"

when world's flag is true and player's score "kills" is at least 10
    give player 1 netherite ingot
```

Combining conditions:

```
when player has 5 diamonds and player's level is above 20
    tell player "A wealthy high-level player!"
```

You can also use `not` and `or`:

```
when not player has 10 diamonds
    tell player "Keep mining!"

when player's health is below 5 or player's hunger is below 5
    warn player "You need to take care of yourself!"
```

---

## Special player values

Besides variables, `player's ...` can read facts about the player:

| Value | What it is |
| --- | --- |
| `player's name` | the player's name |
| `player's world` | overworld / nether / the end |
| `player's x` / `y` / `z` | coordinates |
| `player's gamemode` | survival / creative / adventure / spectator |
| `player's health` / `hunger` / `xp` / `level` | stats |

```
tell player player's name
set player's home x to player's x
if player's gamemode is creative
    give player a netherite sword
```

## Handy values

| Value | What it is |
| --- | --- |
| `random number between 1 and 10` | a random whole number in range |
| `number of players` | players online |
| `count of "diamond" in player's inventory` | how many the player carries |
| `length of list "quests"` | list size |

```
set world's price to random number between 10 and 100
if count of "emerald" in player's inventory is at least 5
    announce "Rich player!"
```

## Player state conditions

```
player is sneaking
player is on the ground
player is wet
player is flying
player is op
player is in creative mode        # or survival / adventure / spectator
```

They work in `when` headers, `if` blocks and `and` / `or` chains:

```
when player is sneaking and player is in the nether
    warn player "Careful down here!"

when player is flying
    give player 1 elytra
```

## Showing variables

`show` prints a variable's value to the player.

```
show player's coins
show world's kills
show length of list "quests"
```

The player sees:

```
coins: 12
```

---

## Text with variables inside — `${...}`

Any quoted text can mix plain text with a value using `${...}`:

```
set player's coins to 5

tell player "You have ${player's coins} coins!"
announce "${player's name} joined the server"
set world's greeting to "Welcome back, ${player's name}!"
```

The player sees:

```
You have 5 coins!
```

Inside `${...}` can go any value — variables, special values like
`player's health`, or whole phrases like `random number between 1 and 10`:

```
tell player "A wild ${random number between 1 and 10} appeared!"
title player "HP: ${player's health} — Run!"
```

Lists print as `[a, b, c]` when they land inside `${...}`.

---

## Persistence

World variables, player variables and lists are saved to
`plugins/Hermes/state.txt` when the server stops and loaded again when it
starts, so progress survives restarts.

---

## The rules in one picture

```
set  player's coins   to 5        # player variable
add  5                to player's coins
set  world's flag     to true     # world variable
add  1                to world's kills
set  player's score "kills" to 0  # scoreboard
add  "a"              to list "quests"   # list
loop over list "quests" as task          # temporary
    tell player task
show player's coins
```

Every variable is one word (or a quoted name for lists and scores). If you
want a two-word name, use a score or a list — or combine with `_` in your
head, Hermes variables are words.
