# Minecraft 26.2 port status

## Ransom 1.1.1 (2026-10-03)

Updated the existing `26.2` branch and checkout from the previously tested 1.1.0 port at `4abc042`. Behavior reference: `.worktrees/1.21.1`, main at `ba0511b` with the tested, uncommitted 1.1.1 release changes. No 1.2 features or development controls were copied.

Targets: Minecraft 26.2, Java 25, NeoForge 26.2.0.88, Fabric Loader 0.19.5 and Fabric API 0.160.0+26.2. Shared assets remain in `common/`; loader-specific Java remains in `neoforge/` and `fabric/`. Stonecutter/source-sharing migration is deferred until after 1.1.1.

### Changes and behavior contracts

- Dedicated server: NeoForge client setup lives in a separate client class. Fabric client packet receiver registration also lives in a separate client class; this fixes a server startup failure caused by loading LocalPlayer during common registration.
- Server settings: Ransomware settings are read on the server, synchronized on login and after authorized edits, and used for per-player natural spawn timers and item-loss policy. Clients do not independently trigger natural encounters. Edits require operator permission or singleplayer ownership. Stealer sessions suppress natural encounters.
- Config migration: copy the previous settings file once when the replacement does not exist. NeoForge uses `config/ransom_in_minecraft-ransomware.toml` and the existing common Stealer config. Fabric uses `config/ransom_in_minecraft-ransomware.properties` and `config/ransom_in_minecraft-common.properties`; the former migrates from `ransom_in_minecraft.properties`. Settings remain outside world folders.
- Flashback: skip live encounter/Hijack simulation on ReplayServer, suppress unsupported packets, release forced movement during playback, and keep the virtual selected slot out of vanilla carried-item packets.
- Underwater Ransomware: after the first jumpscare, check underwater status and water one or two blocks above the player's eye block. Exit without item loss, with a fast floating face, screen bubbles and vanilla bubble sound. The face floats in the HUD, not in the world.
- Underwater Stealer: freeze current air only while an alive underwater participant has the actual sealed chest interface open with an active session in the same level. Closing the interface resumes normal breathing while the game continues. NeoForge uses LivingBreatheEvent; Fabric hooks vanilla breathing and freezes both air consumption and refill.

### Checks performed

- Both release builds passed, including Fabric access-widener validation.
- Both development clients loaded resources and initialized the renderer/audio without a mod startup exception.
- NeoForge dedicated development server loaded the mod, bound port 25566 and reached `Done`. The early piped stop caused a vanilla command-shutdown exception after world saving; the test process was then terminated. The user's existing 1.21.1 test server was left running.
- Fabric dedicated development server loaded the mod and common mixins without the previous client-class exception, then stopped at the EULA gate. No EULA was accepted or changed for this port, so full Fabric world startup is not verified.
- JAR metadata, required new classes, absence of 1.2 item classes and shared asset parity were audited.

### Remaining in-game verification

The previous 1.1.0 ports received user gameplay checks; those do not validate the new 1.1.1 behavior. Check server config versus a guest's differing local config, natural spawn/whitelist behavior, underwater Ransomware exits, Stealer air freeze and resumed consumption after closing, Flashback playback, and dedicated multiplayer. Sodium/Iris rendering was not retested in this update.

### Release artifacts

- `neoforge/build/libs/ransom-neoforge-26.2-1.1.1.jar`
- `fabric/build/libs/ransom-fabric-26.2-1.1.1.jar`

Publication target: the existing `26.2` source branch. Development branches and 1.2 features are excluded. GitHub Release assets are separate from branch publication.
