# Build all modules → jars

## Requirements
- **JDK 21**
- Internet (first run downloads Minecraft + Fabric Loom deps)

## One command
```bash
cd Doodly-Client-enhanced
./build-all.sh
```
Jars appear in `Doodly-Client-enhanced/dist/`.

## Single module
```bash
cd PlayerEsp    # or BaseFinder, DoodlyCore, ChestEsp, FreeCam
./gradlew build
# jar: build/libs/*.jar  (ignore *-sources.jar)
```

## Install
1. Minecraft launcher → Fabric profile for **1.21.11**
2. Mods folder: Fabric API + Cloth Config + every jar from `dist/`
3. Launch → **Right Shift** = Doodly Core GUI

## Modules
| Jar from | What |
|----------|------|
| DoodlyCore | Right-Shift menu |
| PlayerEsp | player name / HP / held / radar |
| BaseFinder | light + storage clusters + sounds |
| ChestEsp | container ESP |
| FreeCam | freecam (optional) |
