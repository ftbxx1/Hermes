# Loops

Loops repeat a block of actions. The loop variable is a plain word you can
use inside the body (or in `${...}` templates).

## Loop over all players

```
loop over all players as p
    tell player "Hey, ${p}!"
```

## Loop over a list

```
loop over list "quests" as task
    announce "New quest: ${task}"
```

## Loop over numbers

```
when player joins
    loop over numbers from 1 to 10 as i
        tell player "Counting: ${i}"
```

## Loop over a player's inventory

```
when player joins
    loop over player's inventory as item
        tell player "You hold: ${item}"
```

## Repeat a fixed number of times

```
when player joins
    repeat 3 times
        give player 1 golden apple
```

## Loop variables

The variable is temporary: it only exists inside the loop, and you use it
as a bare word or inside `${...}`.

```
loop over all players as p
    tell player "Welcome back, ${p}!"
```

## Building lists

Lists are created and filled like this:

```
when player joins
    create list "quests"
    add "Find the treasure" to list "quests"
    add "Slay the dragon" to list "quests"

    loop over list "quests" as task
        tell player "Quest: ${task}"
```

See `variables.md` for everything lists can do (clear, delete, count, ...).
