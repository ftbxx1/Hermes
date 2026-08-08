# Scoreboards & Teams

Scoreboards track numbers per player (kills, deaths, quests done, ...) and
teams group players together. Both are real Minecraft scoreboards/teams.

## Scores

Scores are named and stored per player:

```
when player joins
    set player's score "kills" to 5
    add 1 to player's score "kills"
```

Read them back in conditions:

```
when player's score "kills" is at least 10
    give player 1 diamond
```

Check them in `if` blocks too:

```
when player joins
    if player's score "kills" is at least 10
        give player 1 diamond
```

## Teams

Create a team and put players in it:

```
when player joins
    create team "red"
    put player in team "red"
```

Teams are useful for pvp arenas, minigames, or just grouping:

```
when player joins
    create team "guests"
    put player in team "guests"
    add 1 to player's score "joins"
```

## A small example

```
when player kills a zombie
    add 1 to player's score "kills"
    if player's score "kills" is at least 10
        give player 1 diamond
        tell player "You hit 10 kills! Have a diamond."
```
