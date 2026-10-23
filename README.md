# Doodly Client (enhanced)

Separate Fabric modules + unified Right-Shift ClickGUI.

| Folder | Mod id | Role |
|--------|--------|------|
| `DoodlyCore/` | `doodly_core` | **Right Shift** menu — opens each module's config |
| `PlayerEsp/` | `doodly_player_awareness` | Players: name / HP / held item / radar / sound phantoms |
| `BaseFinder/` | `doodly_base_scan` | Storage clusters + **artificial light** scan + dig sounds |
| `ChestEsp/` | `doodly_utils` / `chestesp` | Container boxes (loaded chunks) + density helper |
| `FreeCam/` | `wi_freecam` | Optional free camera |

## GitHub audit (vs doodlyyousuf/Doodly-Client)

- PlayerEsp / ChestEsp / FreeCam match upstream structure.
- Tracker UUID phantom uses valid hex `0x534F554E44000000L` (not broken `SOUND` literal).
- Clear name via `GameProfile.getName()`; multi-line name / HP / held labels above head.
- No portal / end-gateway markers (by design).
- ChestEsp has no PlayerESP coupling.

## Light detection (BaseFinder)

Scans **already loaded** chunks for `LightLayer.BLOCK >= min` and (optionally) `block > sky`, Y ≤ 60. Artificial light underground is a strong base cue when the server has streamed those sections. Does **not** invent deepslate outside received chunks.

## Build each module

```
cd PlayerEsp && ./gradlew build
cd BaseFinder && ./gradlew build
cd DoodlyCore && ./gradlew build
```

Drop jars into mods with Fabric API + Cloth Config. Open menu with **Right Shift**.

