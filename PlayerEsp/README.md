# Doodly PlayerESP — standalone

Fabric 1.21.11 client mod. Separate from ChestESP.

## Why separate

DonutSMP (and similar) run Grim/Vulcan + custom packet filters + translation-key fingerprinting.
A combined chest+player jar is a larger signature surface. This module is a single purpose client overlay:

- tracks **only** players the server already sent as entities
- plus **positional sound** side-channel (step/dig/hurt/place/elytra) for underground phantoms when entity packets are stripped
- **zero C2S packets**
- **zero chunk requests**
- mod id `doodly_player_awareness` (no "esp"/"chest"/"hack" strings in fabric.mod.json)

Server-side chunk section stripping still means you never receive hidden block data — this mod does not invent blocks.

## Features

| Feature | Notes |
|---------|--------|
| Boxes / fill | player AABB, through-walls optional |
| Tracers | full or nearest-only |
| Labels | name, distance, HP, armor count, held item, speed |
| 2D radar | top-right minimap, friend-colored |
| Friends | comma list → cyan |
| Sound phantoms | `~confidence` markers underground |
| Motion predict | short velocity lead on tracers |

## Build

```
cd PlayerEsp
./gradlew build   # needs Fabric Loom + Java 21
```

Drop the jar in `.minecraft/mods` with Fabric API + Cloth Config.

## Keybind

Controls → Player Awareness → Toggle (unbound by default).

## Recommended Donut profile

- enabled, tracers nearest-only, radar on, through-walls on
- underground-only while base-hunting
- pair with a translation-fingerprint blocker (ExploitPreventer family) if the server probes anvil/sign keys
- do **not** run PacketIntel-style dump tools while playing

