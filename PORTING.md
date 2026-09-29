# Minecraft 26.2 port status

Minecraft 26.2 ports of Ransom 1.1.0. Both loaders have received user in-game checks.

- `common/` contains the shared 1.1 assets and data.
- `neoforge/` contains Ransomware, Stealer, Hijacked mobs and player Hijack for 26.2. It builds and has received user in-game checks. Daylight protection now hooks the 26.2 `Mob.burnUndead` method.
- `fabric/` contains the same 1.1 mechanics, with Fabric networking, persistent attachments, entity callbacks, HUD/layer/PiP registration and vanilla mixins. Its build and access widener validation pass.
- Fabric's configuration screen is available through ModMenu when ModMenu is installed. The development client includes ModMenu 20.0.2 for testing; the Ransom JAR does not require it.
- The user tested the 1.1 Fabric port in game and reported that it appeared to work normally. Automated build, access widener and static mixin-target checks also passed.

Further regression checks can cover Ransomware/glitch-block damage and slot locking, all Stealer outcomes and chest destruction, mob AI/sunlight/conversion, player Hijack/death/reconnect, multiplayer and Sodium/Iris rendering. The user's checks do not establish exhaustive coverage.

Fabric settings live in `config/ransom_in_minecraft.properties` and `config/ransom_in_minecraft-common.properties`, outside world folders. Old Fabric 1.0.1 player encounter attachments migrate to the new persistent state on access.
