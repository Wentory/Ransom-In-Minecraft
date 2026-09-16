# Minecraft 26.2 port status

This branch is a work in progress and must not be published as a playable mod.

- `common/` contains the existing assets and data.
- `neoforge/` contains the original 1.21.1 implementation as the migration baseline. It does not compile against 26.2 yet. The first compiler run stopped after 100 API errors, mainly in client rendering, GUI, networking, and inventory access.
- `fabric/` currently has loader metadata and a minimal entry point. It compiles, but contains no encounter gameplay yet.

Both loaders must have complete gameplay and pass client, server, reconnect, and multiplayer checks before release.
