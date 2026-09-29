# Changelog

- Added Hijacked drowned: 5% natural spawn chance, Worm infection, retained infection when a Hijacked zombie converts, shared glitch visuals/STOP behavior and fast underwater swimming. Hijacked zombies retain sinking but move underwater without the usual slowdown. Added `/ransom drowned` for testing; drowned use custom land/underwater ambient, hurt and death sounds with randomized pitch, and keep vanilla trident attacks pending their own design.

- Added the supplied zombie/death.ogg as the Hijacked zombie's death sound with randomized pitch (0.8–1.2).

- Hijacked zombies now spam randomized footstep sounds every 3–6 ticks while moving on the ground, instead of spamming growls during pursuit. Growls remain occasional (3–7 seconds); STOP pauses suppress footsteps and vanilla footsteps are disabled to avoid duplicates.

- Damage now triggers the Hijacked zombie's existing 1–2 second STOP pause, sprite and sound, interrupting any active mob infection. Repeated damage restarts the pause.

- Connected the supplied Hijacked zombie growls, hurt variants, footsteps and STOP sound with vanilla-style randomized pitch (0.8–1.2). Converted hurt2 from MP3 to OGG for Minecraft while retaining the source file.

- Hijacked zombies now select only supported, uninfected mobs (ordinary zombies and creepers) for their 30-second infection process. They attack players only and ignore unsupported/already hijacked mobs. Creeper infection victims freeze, shake and emit glitch particles with the same interruption rules.
- Fixed the false Worm health/food layers failing to reserve HUD space, which caused armor and air icons to overlap the replaced rows.
- Added hidden Ransom: Worm player infection (0–100), cumulative HUD deception, screen interference, visual mob illusions, a 10% animated fullscreen glitch2 overlay from level 3, and rare movement impulses/single-click input glitches at level 4. Level 5 retains the prior effects without an additional penalty.
- Added `/ransom hijack get [player]`, `set <player> <0–100>` and `add <player> <amount>`. Infection gains: glitch-block damage +5, Hijacked zombie damage +2, exposed Hijacked creeper blast +25 even with a shield. Golden apples remove 20, enchanted apples 50, and killing a Hijacked mob removes 2. Death resets infection.
- Added server-controlled Worm enable/disable and passive recovery interval (default one point per 60 ticks; 20 ticks = one second). Disabling clears infection. The config screen now scrolls to expose the added settings.
- Explosion glitch fragments now spread slightly outward with a slowing expansion while fading; burst anchors remain stable instead of respawning in place.
- The Hijacked creeper's explosion now emits a brief 0.6-second fading glitch burst instead of a lingering cloud. Nearby mobs are infected once at the explosion; no ongoing infection zone remains.
- Added reusable `GlitchParticles` visuals: stationary shuffled clusters mixed with solitary flickering lines, red/black coloring and rare white fragments (2%). Hijacked mobs, the flying face trail and the Worm cloud now share this style.
- Fixed zombie infection shaking accumulating torso/head roll across renders and leaving the torso tilted after infection. Temporary shake is now reset before the next model animation.
- The Hijacked creeper face now predicts the player's movement once at launch and follows a fixed straight trajectory instead of homing. A missed shot keeps going until collision or its ten-second flight limit.
- Increased Worm zombie speed to three times vanilla and added random 1–2 second freezes during player pursuit, displaying a square STOP sign on the zombie before it resumes chasing.
- The flying Worm face now uses a centered 1×1×1 collision box, independent visual shaking, stronger sliced distortions and a fading red/black glitch trail.
- Replaced the Worm creeper's potion-cloud entity with a dedicated server infection zone and a separately synchronized, dense cloud of red/black glitch fragments. The cloud no longer emits potion particles.
- Worm creepers now detect players within 40 blocks, rise for three seconds (about six blocks in open space), and retain their acquired target beyond detection range or lost line of sight.
- Worm zombies now step directly onto one-block ledges without a ground-jump impulse. Ordinary zombies keep their normal jumping behavior.
- Added the infected Worm creeper: vanilla player detection, an idle corrupted model, a random 5–10 second levitating wind-up, and a fast direct attack using the five supplied animated face frames. Its shield-blockable vanilla-strength explosion preserves blocks and leaves a five-second, three-block glitch cloud that infects ordinary zombies and creepers. Added `/ransom creeper` for testing and a 5% natural creeper infection chance. Player infection is not implemented yet.
- Added Ransom: Worm behavior. Infected zombies wait until they find a target, prioritize players over hostile mobs, and infect ordinary zombies after 30 seconds of immobility, shaking and glitch fragments. Damage to the infector or a player within 10 blocks interrupts infection and frees the victim.
- Moved the infected zombie's animated glitch-noise surface outside its red tint so the noise is visible. Its torso now faces actual travel instead of turning with the head.
- Increased the infected zombie's animated glitch-noise overlay opacity to 80%.
- Infected zombies no longer burn in sunlight; ordinary zombies keep their vanilla sunlight behavior.
- Added `/ransom zombie` to spawn one infected zombie three blocks in front of the command player for testing, without changing the 5% natural infection chance.
- Added the first infected zombie variant. Five percent of naturally spawned vanilla zombies become persistently corrupted, move twice as fast with frozen arms and legs, turn their head toward visible players up to 48 blocks away, and retain vanilla 35-block target acquisition. Their model gains the infected player's red noise, brief 3D slicing and glitch clusters; pursuit growls stutter at irregular intervals and pitch, while hurt/death sounds distort.
- The STEALER perfect-win jumpscare now begins as its window starts collapsing. Ransom keeps screaming visually until the window disappears, while the jumpscare sound fades with the collapse instead of cutting off early.
- Added the supplied laugh sound to the complete STEALER QTE failure ending, playing once as the HAHAHAH finale settles in the center and stopping when the window closes.
- The partial-success WELL DONE ASCII art now plays the existing randomized terminal typing clicks while its letters print, at a softer volume than the terminal itself.
- Replaced the partial-success WELL DONE graphic in RANSOM: STEALER with the requested eight-line ASCII art. The compact finale window scales the art uniformly while it prints, without extra empty space below.
- Fixed infected-player glitch effects lingering on a dead player or carrying over to the replacement player entity after respawn.
- Taking damage from a glitch block now triggers a short burst of much stronger 3D player-model slicing, with more bands, wider displacement, stronger stretching, and more dropouts; the existing intense texture and particles remain.
- Replaced the infected player's temporary flat-skin glitch with short screen-space horizontal slices of the normal animated 3D player render. The effect keeps skin layers and equipment and clips each slice during rendering so world depth remains active. Increased the slice frequency and displacement after the first pass proved too subtle, and removed a render-buffer type check that could skip the effect entirely.
- Added world/server config options to disable RANSOM: STEALER and set the infection chance for unopened loot-table chests from 0 to 100 percent (default 30). World owners and server operators can change them directly in the mod's config screen. Disabling it also ends active STEALER sessions and clears infection from chests when encountered.
- Breaking an infected chest by hand or explosion now triggers the regular Ransomware encounter for the nearest non-spectator player within 12 blocks. Explosion handling also aborts an active STEALER session and avoids duplicate attacks from a double chest.
- The STEALER QTE timer now starts locally when the keys appear, without relying on matching client and server clocks. The server allows the measured round-trip latency plus a small margin for input delivery, while a resumed chest keeps its running server deadline.
- RANSOM: STEALER now picks each QTE length randomly from 3 to 10 keys instead of increasing it every round. Extra time per key begins with the sixth key.
- When loot is lost, the chest opens in the world and broadcasts the flying, glitching item effect to nearby observers as well as the player using the chest.
- Reworked the printed WELL DONE ending so its gray dotted blocks form the depth of the letters instead of filling the empty background.
- Replaced the individual encrypted chest-slot markers with one animated `glitch2` panel, a single frame around the chest slots, and a centered square STOP sign that extends slightly past the panel.
- RANSOM: STEALER now accepts only letter and digit keys for QTE input; other keys and mouse clicks no longer fail a round, so screenshots can be taken during play.
- Added `/ransom cancel [players]` to stop an active or queued Ransomware encounter without applying its win or loss effects.
- Infected-chest QTEs continue against a real-time deadline after the player closes the chest or disconnects. Reopening the chest resumes the current round without scanning again.
- Replaced the chest finale spam QTE with outcome-specific chest glitches: a green flash after saving all loot, a stronger glitch after saving some, or a violent particle burst and chest opening after losing everything. On total loss, a glitch flies to the nearest nearby player and starts the regular Ransomware encounter.
- Added an optional biome whitelist for natural Ransom encounters. It is disabled by default and accepts comma-separated biome IDs in the config screen.
- Extended the existing `/ransom` command with `disable`, `enable`, and `status` subcommands for per-player natural spawning. The setting persists in the world.
- Added infected loot-table chests. On their first opening attempt they have a 30% infection chance and replace the chest menu with a silent Ransom QTE over grouped loot types.
- Added one cumulative QTE timer per loot group (2.6 seconds for five keys, 7.1 for ten), visible key replacement tricks, final-position ALT+F4 traps, lost-item glitch reveals, and three chest outcomes.
- Added persistent red and black glitch slices and particles for infected chests without replacing their normal model or lid animation.
- Added `/ransom chest` for spawning an infected chest with the End City treasure loot table for testing.
- Breaking an infected chest now summons the regular Ransom encounter for the nearest non-spectator player and ends any QTE running on that chest.
- Reworked the chest encounter to open a normal chest interface with sealed, encrypted chest slots first; the Ransom window then grows from the center of the chest area before scanning and QTE.
- Lost loot now briefly appears above the chest block in the world and breaks apart with red-black glitch fragments.
- Redesigned the infected-chest overlay as separate RANSOM: STEALER, TERMINAL, and CHEST windows. The terminal types commands and round results as play progresses; the chest preview rotates, opens on lost loot, and glitches the item away.
- The three infected-chest windows now independently jump to new screen positions and jitter using the same timing and motion style as the regular Ransomware popup.
- Made QTE keys square, with a wider key only for ALT+F4; centered the rotating chest preview and moved lost-item details into the CHEST window instead of the main QTE window.
- Split chest scanning into its own window before the three game windows appear, sped up terminal typing, and placed RANSOM: STEALER above the other windows.
- Added chest endings: the side windows shrink away while Ransom moves to the center. Saving all loot cuts off his jumpscare, saving some shows WELL DONE, and saving none leads to a one-key spam resistance challenge; failure starts the regular Ransomware encounter.
- The full-success ending uses the final jumpscare sound and stops it when the window cuts the scare off.
- Suspended natural and queued Ransom summons during the chest game, and prevented an infected chest game from starting during an active Ransom encounter.
- Sped up scan and game-window opening, side-window closing, and the final-window movement and collapse. The first four QTE keys now add 0.5 seconds each; each key from the fifth adds another 0.1 seconds.
- Key replacement now targets only the third or later key, preserves the ALT+F4 trap, and gives only the changing key a brief two-tick glitch flash.
- Reduced the ALT+F4 trap chance from 18% to 2% per chest QTE round.
- Removed the ten empty placeholder keys shown between chest QTE rounds.
- Halved chest scan time. The upcoming replacement key briefly glitches while the preceding key is active, then shows its new letter without pausing input or the QTE timer.
- Breaking an infected chest or failing the chest's infection-resistance challenge now directly triggers the jumpscare and Ransomware coin game, bypassing the avoidable STOP warning and empty-area exit.
- Added glitch2 for key replacement, stone-button clicks for QTE input, glitch for lost-item deletion, and failture for wrong or timed-out input.
- Added stealer_success when a chest key combination is completed successfully.
- Added looping soundtrack2 for the chest QTE. It starts when the three game windows appear and stops on completion, screen closure, or the start of infection resistance.
- Added spam_encounter for the infection-resistance stage. It replaces soundtrack2, and the resistance timer now follows the track's 9.3802-second duration instead of a fixed four seconds.
- Expiring the infection-resistance timer now starts the real Ransom jumpscare immediately on the client, with server confirmation and chest cleanup following without an extra ending animation.
- `/ransom chest` now accepts any available loot table ID, with command suggestions and validation; omitting the ID still uses End City treasure.
- Lost chest items now ease out over one second, then break into small red, black, and white glitch fragments and disappear over half a second in both the CHEST window and the world.
- Moved chest glitch overlays in front of the chest model in both the CHEST window and the world.
- `/ransom chest` now defaults to the vanilla dungeon chest loot table (`minecraft:chests/simple_dungeon`); a specified loot table still overrides it.
- Added `/ransom chest infect` to infect an existing unopened loot-table chest without generating or changing its loot. Infected chests now have a subtle persistent red mark and more visible glitch bursts drawn after block entities.
- Limited `/ransom chest` suggestions and validation to chest loot tables, so item loot tables no longer appear in the command list.
- Rendered chest-surface glitch strips in a dedicated overlay pass so the chest model no longer hides them; surrounding particles keep their normal depth behavior.
- Kept the full RANSOM: STEALER window frame and title during all chest-game endings instead of replacing it with a plain black rectangle.
- Added window_open, window_close, and scanning sounds to chest-game window transitions; scanning now lasts 1.9 seconds (38 ticks).
- Replaced the scan progress bar with a clockwise radial reveal of scan.png starting at 3 o'clock, with cycling Scanning. / Scanning.. / Scanning... text below it.
- Fixed a render-thread crash when infected chest overlays and particles used the same buffer source at once.
- Replaced oversized chest-spanning red bands with small, brief glitches on camera-facing chest surfaces and a subtle infection mark.
- Kept the chest QTE soundtrack looping until the ending begins, including the no-loot ending, then stopped it for every outcome.
- Removed permanent chest-surface marks, moved sparse glitch particles into the air around infected chests, and added rare two-tick UV shifts to the chest model itself.
- Fixed soundtrack2 ending after one playback during the chest QTE by enabling the sound instance's actual looping flag.
- Changed the partial-loot WELL DONE ending: Ransom's face disappears after centering, and a large block-and-dither message prints across the window in scanline order.
- Added random click1–click4 sounds as the terminal prints, with a random pitch within two semitones of normal.

## 1.1.0
- Added brief camera-facing 2D skin silhouette distortions to infected players while preserving the existing red noise layer.
- Added intermittent player glitch bursts and a stronger one-second glitch response visible to everyone after glitch-block damage.
- Increased the frequency of infected-player silhouette distortions and added stationary animated glitch clusters that fade in and out around the model.
- Added the animated STOP sign directly over the player's body for observers, frantic model corruption during jumpscares/downloads, and a green success flash on victory.
- Increased glitch zones to 128 blocks per game and up to 24 blocks per cluster.
- Added glitch fade-in, impact, chromatic split, and safe fade-out animations to the STOP warning.
- Sped up the STOP warning's glitch appearance and disappearance animations.
- Moved spawn.ogg to the exact moment the STOP sign appears and shortened its appearance to two ticks.
- Animated the DOWNLOADING label through zero to three dots.
- Added animated gold, red, and glitching nugget bursts that fly into the ransom counter, with staged pickup and arrival sounds.
- Added a random -2 to +2 semitone pitch shift to each coin pickup sound.
- Changed repeated coin pickup sounds to a fixed two-tick interval so they finish before the arrival sound.
- Removed the coin pickup action-bar text while keeping coin credit and victory logic immediate.
- Stopped the Ransom soundtrack as soon as victory is secured and synchronized the black victory window with the final burst reaching the counter.
- Raised both the main Ransom window and its smaller popups above encrypted-slot overlays, synchronized the success sound with the Thank You image, and shortened the STOP reaction grace period.
- Removed knockback from all Ransom damage.

- Fixed infected block overlays disappearing with the Fabulous! graphics mode.
- Changed the game area from a sphere to a height-independent cylinder.
- Failure now clears the hotbar before dealing damage.
- Added a proximity-based cylindrical boundary visible within 19 blocks, with a soft animated glitch background and brighter world-anchored procedural corrupted fragments.
- Added personal hazardous glitch zones that generate before collectible infected blocks and regenerate with the game area.
- Added animated glitch-block overlays with sparse red and black particles that emerge from the blocks and fade while rising.
- Touching a glitch zone now deals armor-bypassing damage at most once every two seconds.
- Glitch-zone hits now encrypt random main-inventory slots, including empty slots; encrypted slots cannot be used and are deleted on failure when item deletion is enabled.
- Added the glitch-hit sound and a non-stacking screen-noise compression effect.
- Optimized glitch-zone contact detection to inspect only blocks touching the player's hitbox.

## 1.0.1

- Fixed infected block overlays disappearing when Sodium is installed.
- Fixed infected block overlays rendering through terrain or at incorrect positions without Sodium.
- Improved compatibility with shader rendering by using a dedicated depth-aware overlay layer.
- Fixed saved encounters carrying over into newly created or unrelated worlds.
- Encounters and pending punishments now resume only in the world where they were started.
- Added the in-game project icon.

## 1.0.0

- Initial release.
- Added natural Ransom encounters with the STOP reaction sequence.
- Added jumpscares, a timed debt challenge, infected blocks, and coin collection.
- Added hotbar encryption and configurable item deletion after failure.
- Added penalties for leaving the game area, dying, and disconnecting.
- Added multiplayer infection visuals and an in-game configuration screen.
