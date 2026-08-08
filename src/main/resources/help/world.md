# The World

Weather, time, blocks, doors, levers, signs, and special effects.

## Weather & time

```
set weather to rain
set weather to storm
set weather to clear

set time to noon
set time to night
set time to dawn
```

React to the sky:

```
when it is raining
    announce "It's raining!"

when night falls
    warn player "Get inside!"
```

## Blocks

```
set block at 10 64 20 to diamond block
```

## Doors, buttons, levers

Operate the block nearest the player:

```
when player joins
    open door near player
    close door near player
    press button near player
    pull lever near player
```

Power blocks directly (e.g. redstone lamps):

```
power block at 10 64 20
unpower block at 10 64 20
```

## Signs

```
write sign at 10 64 20 with "Shop" and "3 emeralds"
```

Up to 4 lines, each an `and "..."` clause.

## Sound & particles

```
when player joins
    play sound "level up" near player
    spawn particles "happy villager" near player
```

## Dramatic moments

```
when player joins
    lightning at player
    explode at player with power 2
    launch player by 5
```

## Effects on players

```
when player joins
    title player "You won!" with subtitle "Nice work"
    actionbar player "Careful!"
```

## Spawning mobs

```
when player joins
    spawn zombie near player
    spawn 5 zombies at 100 64 100
    spawn zombie at 100 64 100 named "Boss"
```
