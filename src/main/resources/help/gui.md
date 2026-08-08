# Chests & GUIs

Chests are the simplest way to build a GUI in Hermes: fill a chest with
items, and script what happens when players interact with it.

## Filling a chest

```
put 5 diamonds in chest at 10 64 20
put 1 golden apple in chest at 10 64 20
```

A chest can hold any item the dictionary knows, in any quantity.

## Taking from a chest

```
take 2 diamonds from chest at 10 64 20
```

## Checking a chest

```
when player interacts with a chest
    if chest at 10 64 20 has 5 diamonds
        announce "The chest is full!"
```

Use it as a condition — for example, inside a trigger:

```
when player interacts with a chest
    if chest at 10 64 20 has 5 diamonds
        announce "The shop chest is full!"
```

## Interacting with a chest (or any block)

```
when player interacts with a chest
    tell player "You opened a chest!"

when player interacts with a stone button
    put 1 emerald in chest at 10 64 20
```

The block name after `interacts with` can be any block in the dictionary:
`chest`, `ender chest`, `stone button`, `oak door`, `anvil`, and so on.

## A full example: a shop chest

```
when player interacts with a chest
    if chest at 10 64 20 has 1 diamond
        take 1 diamond from chest at 10 64 20
        give player 3 emeralds
        play sound "level up" near player
        tell player "You bought 3 emeralds!"
    else
        tell player "The shop is out of stock."
```
## Extra GUI feel

- Titles and actionbars tell players what is happening:

```
when player interacts with a chest
    title player "You bought it!" with subtitle "3 emeralds"
    actionbar player "Thanks for shopping!"
```

- Signs label your chests:

```
write sign at 10 64 20 with "Shop" and "3 emeralds each"
```

- Particles and sounds draw attention:

```
when player joins
    spawn particles "happy villager" near player
    play sound "chest open" near player
    lightning at player
```

## Related

- `put`/`take` also work for player inventories: see `variables.md` and
  the `give`/`remove` actions in the README.
- Block positions are written as `X Y Z`: `10 64 20` is x=10, y=64, z=20.
  Use `mark home at 10 64 20` (see `regions.md`) so you don't have to
  remember coordinates.
