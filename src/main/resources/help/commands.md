# Commands

Scripts can define their own player commands. Commands must be written at
the top level of a script (not inside a trigger).

## A simple command

```
command "/kit"
    give player 1 diamond
    tell player "Here's your diamond!"
```

## Commands with arguments

Arguments become variables you can use in the body inside `${...}`
templates in quoted text.

```
command "/pay" with argument <amount>
    tell player "You paid ${amount} coins!"
```

```
command "/hello" with argument <name>
    tell player "Hello, ${name}!"
```

## Commands with a permission

The permission is checked before the body runs. Anyone without it gets
refused.

```
command "/vip" permission "kits.vip"
    give player 1 netherite ingot
```

With no `permission "..."`, anyone can use the command.

## Command rules

- The name must be a single word, e.g. `/kit`, not `/my kit`.
- Arguments are used as `${name}` templates inside quoted text — check
  `variables.md` for more.
- Commands can do anything a trigger can: give, teleport, loop, call
  actions, and so on.

## Example: a full kit shop style command

```
command "/fly" permission "fly.use"
    if player is in creative
        set player's gamemode to survival
        tell player "Flight disabled."
    else
        set player's gamemode to creative
        tell player "Flight enabled!"
```
