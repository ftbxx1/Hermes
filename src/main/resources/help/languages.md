# Writing scripts in your language

Hermes is written in English, but keywords, items, mobs and effects can all
be translated. Set `language` in `config.yml` to pick the language your
scripts are written in, then run `/hermes reload`:

```yaml
language: es
```

## Example

With `language: es`, this Spanish script works exactly like its English
counterpart:

```
cuando jugador se une
    dar jugador 1 pan
    poner jugador's monedas a 0

every 5 seconds
    anunciar "¡Hola a todos!"
```

## Built-in languages

| Region | `language` codes |
| --- | --- |
| Europe | `en`, `es`, `pt`, `fr`, `it`, `de`, `nl`, `ca`, `gl`, `eu`, `cy`, `ga`, `gd`, `gv`, `fy`, `fo`, `lb`, `co`, `la`, `pl`, `cs`, `sk`, `ru`, `uk`, `bg`, `hr`, `sr`, `sl`, `mk`, `bs`, `sq`, `mt`, `sv`, `da`, `no`, `fi`, `et`, `lt`, `lv`, `hu`, `ro`, `el`, `tr`, `is`, `be`, `eo`, `tt` |
| Middle East | `ar`, `he`, `fa`, `ur`, `hy`, `ka`, `ku`, `ps` |
| Asia | `zh`, `ja`, `ko`, `hi`, `bn`, `ta`, `te`, `id`, `ms`, `th`, `vi`, `fil`, `uz`, `kk`, `az`, `ceb`, `gu`, `jv`, `km`, `kn`, `ky`, `lo`, `ml`, `mn`, `mr`, `my`, `ne`, `pa`, `sa`, `sd`, `si`, `su`, `tg`, `tk`, `ug` |
| Africa | `af`, `sw`, `am`, `ha`, `ig`, `mg`, `sn`, `so`, `st`, `ti`, `wo`, `xh`, `yo`, `zu` |
| Americas | `ht`, `haw`, plus `en`, `es`, `fr`, `pt` |
| Oceania | `mi`, `sm` |

## Your own language

Drop a `yourlang.lang` file into `plugins/Hermes/lang/`, one mapping per
line, `english=native` (`#` starts a comment):

```
# My language pack
when=wanne
player=spiller
give=geef
diamond sword=diamantswaard
```

Then set `language: yourlang` and reload. Words your pack doesn't cover stay
in English, so scripts can mix your language and English freely.

## Things to know

- The possessive `'s` and structure tokens stay English: write
  `jugador's monedas`, not a translated version of `'s`.
- Multi-word phrases like `espada de diamante` are matched as a whole.
- Verbs such as `when`, `every`, `give` and `set` must be a single word in
  your pack; event words like `joins` can be a short phrase (`se une`).
- If a word isn't recognized it stays exactly as typed.
