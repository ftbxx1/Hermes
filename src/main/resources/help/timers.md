# Timers

Timers run a block of actions on a schedule. Write them at the top level
of a script:

```
every 5 seconds
    announce "Tick!"
```

Durations can be any number of seconds:

```
every 30 seconds
    announce "Thirty seconds have passed!"
```

You can also keep counters:

```
every 60 seconds
    add 1 to world's minute
```

## What timers are useful for

- Daily events, hourly announcements
- Restocking shops (see `gui.md` — fill chests back up)
- Clearing lists or resetting scores
- Weather control and time-of-day checks

```
every 300 seconds
    put 64 arrows in chest at 10 64 20
    announce "The shop restocked!"
```

## Rules

- `every` blocks can only appear at the top level of a script.
- If a script is reloaded, its timers are removed and re-created — you
  never get doubled timers.
