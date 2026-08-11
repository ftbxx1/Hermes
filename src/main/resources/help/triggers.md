# Triggers

Triggers make something happen. Every trigger has a body of indented
actions that run when the trigger fires.

```
when player joins
    tell player "Welcome!"
```

## Player events

| Trigger | Example |
| --- | --- |
| joins | `when player joins` |
| leaves | `when player leaves` |
| dies | `when player dies` |
| respawns | `when player respawns` |
| first joins (ever) | `when player first joins` |
| breaks a block | `when player breaks diamond ore` |
| places a block | `when player places dirt` |
| types a message | `when player types "home"` |
| chats | `when player chats` |
| uses an item | `when player uses a fishing rod` |
| interacts with a block | `when player interacts with a chest` |
| picks up an item | `when player picks up a diamond` |
| drops an item | `when player drops a diamond` |
| eats | `when player eats a golden apple` |
| fishes | `when player fishes` |
| levels up | `when player levels up` |
| kills a mob | `when player kills a zombie` / `when player kills any mob` |
| starts sprinting | `when player starts sprinting` |
| stops sprinting | `when player stops sprinting` |
| attacks | `when player attacks` |
| takes damage | `when player takes damage` |
| jumps | `when player jumps` |
| sneaks | `when player sneaks` |
| moves | `when player moves` |
| enters a region | `when player enters region "Castle"` |
| a projectile hits | `when a projectile hits` |
| a projectile hits a player | `when a projectile hits player` |

## Mob events

| Trigger | Example |
| --- | --- |
| a mob dies | `when zombie dies` |
| a mob spawns | `when creeper spawns` |
| a mob attacks | `when zombie attacks` |
| any mob dies | `when mob dies` |
| a named mob dies | `when mob named "Boss" dies` |
| a named mob spawns | `when mob named "Boss" spawns` |

## State triggers

State triggers run once when a state becomes true, and never again until it
becomes false first.

```
when player is flying
    tell player "You're flying!"

when it is nighttime
    announce "The night falls..."
```

| State | Example |
| --- | --- |
| flying / sneaking / wet | `when player is flying` |
| sprinting / swimming / sleeping / burning / blocking | `when player is sprinting` |
| on the ground | `when player is on the ground` |
| operator | `when player is op` |
| in a gamemode | `when player is in creative` |
| in a dimension | `when player is in the nether` |
| in a biome | `when player is in biome desert` |
| in a region | `when player is in region "Castle"` |
| holding something | `when player is holding a diamond` |
| nighttime | `when it is nighttime` |
| raining / storming / clear | `when it is raining` |

## Timers

```
every 5 seconds
    announce "Tick!"
```

## Extra conditions

Any trigger can take extra conditions joined with `and`:

```
when player breaks diamond ore and player has 10 diamonds
    tell player "That's a lot of diamonds!"
```

## Custom events

Scripts can fire their own events, and other triggers can listen:

```
fire event "boss_killed"
```

```
when custom event "boss_killed" fires
    announce "The boss was killed!"
```
