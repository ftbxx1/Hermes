# Effects

Effects are potion-style buffs and debuffs. The dictionary knows friendly
names like `speed`, `strength`, `invisibility`, `fire resistance`,
`night vision`, `slowness`, `weakness`, `poison`, `regeneration`.

## Giving an effect

```
when player joins
    give player speed for 10 seconds
    give player fire resistance for 30 seconds
    give player strength
```

Without a duration, effects last 10 seconds.

## Removing an effect

```
when player joins
    remove speed from player
```

## Instant effects

```
when player joins
    make player stronger
```

`make player <adjective>` applies an effect instantly.

## Health, hunger and XP

```
when player joins
    heal player by 5
    damage player by 10
    feed player by 5
    kill player

when player joins
    give player 10 xp
    give player 5 levels
```

## A useful example

```
when player eats a golden apple
    give player regeneration for 10 seconds
    give player absorption
    heal player by 2
    tell player "Delicious!"
```
