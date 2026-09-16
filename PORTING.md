# Minecraft 26.2 port status

This branch is a work in progress and must not be published as a playable mod.

- `common/` contains the existing assets and data.
- `neoforge/` contains the original 1.21.1 implementation as the migration baseline. It does not compile against 26.2 yet. The latest compiler run reports 86 API errors, mainly in client rendering, GUI, and inventory interactions. The 26.2 inventory setter rejects the hidden slot, so the port uses a dedicated accessor for that slot.
- `fabric/` currently has loader metadata and a minimal entry point. It compiles, but contains no encounter gameplay yet.

Both loaders must have complete gameplay and pass client, server, reconnect, and multiplayer checks before release.
The replacement translucent infection layer also needs an in-game depth and flicker check before it can be accepted.
