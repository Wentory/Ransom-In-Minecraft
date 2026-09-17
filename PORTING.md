# Minecraft 26.2 port status

This branch is a work in progress and must not be published as a playable mod yet.

- `common/` contains the existing assets and data.
- `neoforge/` contains the migrated encounter implementation for 26.2. It compiles and builds.
- `fabric/` contains a Fabric implementation of the encounter, HUD, infected block and player rendering, inventory locking, networking, and saved player state. It compiles and builds.
- Fabric's configuration screen is available through ModMenu when ModMenu is installed. The development client includes ModMenu 20.0.2 for testing; the Ransom JAR does not require it.
- Fabric's development client reached the title screen with the mod loaded. NeoForge's development client is being smoke tested.

Both loaders still need in-game encounter, server, reconnect, and multiplayer checks before release.
The replacement translucent infection layer also needs an in-game depth and flicker check before it can be accepted.
