# PVP Optimizer (Forge 1.16.5)
Client-side PVP helpers + FPS features. Entity culling is event-based; block-entity culling is one defensive mixin (require=0).
PVP part modeled on XLTQ's Fabric "PVP Optimizer" 1.4 (MIT).

## Features
- Crystal Optimizer, Anchor Optimizer, Cart PVP
- Entity culling: skips rendering mobs/players fully hidden behind opaque blocks (never within 4 blocks, never glowing)
- Far-entity cull for non-players, far mob nameplate hiding

## Build
Push to GitHub -> Actions tab -> `build` -> download the jar artifact. (Or `gradle build` locally with JDK 8.)

## Config
`config/pvpoptimizer-client.toml`. Optional keybind in Controls toggles everything.
