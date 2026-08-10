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

## Loop while a condition holds

```
when player joins
    while player's health is above 5
        damage player by 1
        wait 2 seconds
```

The body keeps running as long as the condition is true. Be careful: if the
condition never becomes false the loop never stops, so make sure the body
changes something the condition checks.

## Wait before continuing

```
when player joins
    tell player "Countdown..."
    wait 3 seconds
    tell player "Go!"
```

`wait` pauses the rest of the block. Use seconds (or `ticks` for 1/20th of a
second). `wait` is only allowed directly inside a `when`/`every`/`action`/
`command` block, not nested inside another loop.

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
