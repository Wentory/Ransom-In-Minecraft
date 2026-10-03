# Forge 1.20.1 port status

## Ransom 1.1.1 (2026-10-03)

Updated the existing `forge-1.20.1` branch in `.worktrees/1.20.1`; no new branch or checkout was created. Baseline: `065ded1`, with 1.1.0 mechanics and the earlier client-only config-screen registration fix. Source: `.worktrees/1.21.1`, main at `ba0511b` plus tested, uncommitted 1.1.1 release changes.

Targets: Minecraft 1.20.1, Forge 47.4.20, Java 17. The existing `launchers/1.20.1 Forge.bat` still launches this checkout and uses `run-1.20.1`. User files in that run directory were preserved. Stonecutter/common-source migration and 1.2 features are not part of this release port.

### Ported behavior

- Server authority: COMMON Ransomware config, login/config-edit synchronization, per-player natural encounter timers, biome whitelist and server-controlled failure item deletion. Natural spawning is suppressed while a Stealer session exists. Configuration edits require operator permission or singleplayer ownership.
- One-time migration from `config/ransom_in_minecraft-client.toml` to `config/ransom_in_minecraft-ransomware.toml` if the replacement file does not exist. Existing Stealer/Hijack settings stay in `config/ransom_in_minecraft-common.toml`. Both files remain outside world folders.
- Replay safety: skip live simulation on optional ReplayServer, release forced movement during playback, guard outbound packets against unsupported channels, and keep the virtual selected slot out of vanilla carried-item packets. Forge SimpleChannel protocol is now 3 because registration IDs changed; client and server must both use this build.
- Underwater Ransomware: check underwater state or water one or two blocks above the eye block after the first jumpscare. Exit without item loss, with a fast upward-floating HUD face, bubbles and bubble-column sound. Preserve ordinary no-block exits and respawn recovery behavior.
- Underwater Stealer: server and client LivingBreatheEvent handlers freeze current air only while an alive underwater participant has the actual sealed-chest UI open, with a matching active server session. Closing the UI resumes normal breathing while the mini-game continues. No air refill or potion bubbles are introduced.

### Checks performed

- Java 17 Forge release build passed, including mixin annotation processing and reobfuscation.
- Development client reached resource/renderer/audio initialization without a mod startup exception. SceneCarriedItemMixin applied to MultiPlayerGameMode.
- Dedicated-server launch reached the EULA gate. In Forge 1.20.1 this gate precedes mod construction, so this does not verify the dedicated-server client-class fix or full world startup. EULA was not accepted; a separate request for test authorization is pending.
- JAR contains version 1.1.1, Java 17 bytecode, mapped mixin targets, new config/network classes, and no stale ClientConfig or 1.2 item classes.
- All image/audio assets match the 1.21.1 reference. Four JSON files differ only in formatting; parsed content matches.
- Existing launcher path was retained. Publication target: the existing `forge-1.20.1` source branch; development branches and 1.2 features are excluded.

Logs and temporary port scripts are under `build/port-validation/`.

### Remaining verification

Full dedicated-server startup and a guest connection, then server config versus a differing client config, per-player natural spawn/whitelist behavior, underwater Ransomware and Stealer oxygen/close-resume behavior, death/reconnect, and gameplay/rendering regressions. Replay guard code is present, but Flashback playback is not validated on Forge 1.20.1. Startup checks do not establish behavior parity in-game.

Release artifact: `build/libs/ransom-forge-1.20.1-1.1.1.jar`.

### API references

Forge-specific side separation follows [Forge 1.20.x sides documentation](https://docs.minecraftforge.net/en/1.20.x/concepts/sides/). The installed Forge 47.4.20 source was checked for LivingBreatheEvent before choosing the oxygen hooks.
