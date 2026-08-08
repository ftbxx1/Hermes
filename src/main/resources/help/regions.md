# Regions & Marks

## Regions

A region is a rectangular box of the world. Define it at the top of a
script (top level, like commands):

```
region "Castle" from 10 64 10 to 40 80 40
```

Then react to players crossing in, or being inside:

```
when player enters region "Castle"
    tell player "Welcome to the castle!"

when player is in region "Castle"
    announce "Someone is in the castle!"
```

Check a region in an `if`:

```
if player is in region "SafeZone"
    tell player "You can't fight here."
```

## Marks

A mark remembers a spot so you don't have to type coordinates. Define it
at the top level:

```
mark home at 100 64 200
mark shop at -50 64 30
```

Then teleport to it by name:

```
when player joins
    teleport player to home
```

`teleport player to spawn` and `teleport player to 100 64 200` also work.

## Coordinate reminder

Coordinates are written `X Y Z`:

```
region "Castle" from 10 64 10 to 40 80 40
```

The Y value is height — the ground is usually around y=64.
