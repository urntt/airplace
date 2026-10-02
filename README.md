# airplace

A simple client-side Fabric mod for Minecraft: Java Edition that lets you place blocks in mid-air.

Hold the **Air Place Modifier** key and a block outline appears in mid-air in front of you. Press the use key to place the block you are holding there. Scroll the mouse wheel while holding the key to move the outline closer or farther. Only your own interactions are affected.

## Multiplayer warning

**This mod is disabled in multiplayer by default. To use it in multiplayer, change the settings on its configuration screen.**

Placing blocks in mid-air is not possible in vanilla Minecraft. Servers that run anti-cheat systems may detect it, which can get you kicked or banned, and using it may break a server's rules. This mod does not try to bypass any anti-cheat. Check each server's rules before enabling it there, and use it in multiplayer at your own risk.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://modrinth.com/mod/fabric-api).
2. Download the jar from this repository's [Releases](https://github.com/urntt/airplace/releases) page. Each release supports a single Minecraft version, shown after the `+` in its version number. For example, `1.0.0+26.3` is for Minecraft 26.3.
3. Put the jar into your `.minecraft/mods` folder.

[Mod Menu](https://modrinth.com/mod/modmenu) is optional. When installed, it opens the mod's configuration screen from its mod list.

## Usage

Air placement is enabled by default in singleplayer and disabled on multiplayer servers.

- Hold **Air Place Modifier** (`R` by default). If nothing is in the way, the block at the placement distance in front of your eyes is outlined like a block under the crosshair. Press the use key (right mouse button by default) to place the block you are holding there.
- The mod does not place blocks itself. It hands the outlined spot to vanilla as if you had clicked one of its faces, the one facing you, so vanilla decides what the held item does, which hand is used, and how the block is oriented.
- If a block is in the way, or the spot at the placement distance holds something a placement would not replace (anything but air, water, lava, or a plant such as short grass), there is no outline in mid-air, and the use key works as usual.
- While you hold **Air Place Modifier**, each notch of the mouse wheel changes the placement distance by the scroll step instead of switching hotbar slots. The action bar shows the new distance with two decimals, for example `Placement Distance: 3.10`. The distance stays between the minimum and maximum distance and is saved, so it persists across game restarts.
- Air placement needs a game mode that allows building, so it does nothing in spectator and adventure mode.

The mod adds three key bindings in **Options → Controls → Key Binds**, in the **airplace** category:

- **Air Place Modifier**, bound to `R` by default, is the key you hold to place blocks in mid-air and to change the distance. Unbinding it turns air placement off.
- **Toggle Air Place**, unbound by default, turns air placement on or off and shows the new state on the action bar. On a server that the multiplayer settings rule out, it only shows that the mod is disabled there.
- **Open airplace Settings**, unbound by default, opens the configuration screen. With Mod Menu installed, you can also open it from the mod list.

### Placement distance and vanilla servers

The placement distance is measured from your eyes along your view direction. Vanilla servers reject a block placement when the block is farther from your eyes than your block interaction range plus one block, which with default attributes is 5.5 blocks in survival and 6 in creative, measured to the nearest point of the block. Placements more than about 5 blocks away may therefore be rejected: the block appears for a moment and disappears again. The default maximum distance of 4.50 always stays within that limit.

### Settings

All settings are saved to `config/airplace.json` as soon as you change them.

| Setting | Default | Meaning |
| --- | --- | --- |
| Air Place | On | The current state, the same one the toggle key switches. |
| Minimum Distance | 0.00 | The shortest placement distance the mouse wheel can select, in blocks. |
| Maximum Distance | 4.50 | The longest placement distance the mouse wheel can select, in blocks, up to 10. |
| Scroll Step | 0.10 | How many blocks one notch of the mouse wheel changes the placement distance, from 0.01 to 1.00. |
| Singleplayer Default | On | The state a reset restores in singleplayer worlds, including worlds you open to LAN. |
| Server Default | On | The state a reset restores on servers that the multiplayer mode allows. |
| Reset on World Exit | Off | Every world starts from its default state instead of keeping the last state. |
| Reset on Game Exit | Off | After restarting the game, the first world where the mod is allowed starts from its default state. |
| Multiplayer mode | Disabled | **Disabled**: never active on servers. **Whitelist**: active only on servers in the server list. **Blacklist**: active on all servers except those in the server list. |
| Server List | Empty | The addresses the whitelist and blacklist modes use. |

The placement distance itself, `3.00` at first, is changed with the mouse wheel and saved in the same file. Raising the minimum above the maximum raises the maximum too, and lowering the maximum below the minimum lowers the minimum.

The multiplayer mode is a hard limit: on a server it rules out, air placement stays off whatever the current state is. Joining another player's LAN world or a Realm counts as multiplayer.

Server list entries are compared with the address you connect to, ignoring upper and lower case. An entry without a port, such as `mc.example.com`, matches the server on any port, while an entry with a port, such as `mc.example.com:25566`, matches only that port. The server list screen marks invalid addresses in red and does not save until they are fixed or removed.

## Development

Building requires the JDK version set by `java_version` in `gradle.properties`.

Build the mod:

```bash
./gradlew build
```

The jar is written to `build/libs/`.

Run the client game tests, which start Minecraft and check air placement in singleplayer and on a local dedicated server, the outline, the distance adjustment and its limits, the key bindings, the reset rules, the multiplayer modes, and the saved configuration:

```bash
./gradlew runClientGameTest
```

The game tests need a display. On a headless Linux machine, run them under Xvfb. Xvfb offers no sRGB-capable OpenGL visuals, so install Mesa's Vulkan driver (`mesa-vulkan-drivers` on Ubuntu) for the game to fall back to:

```bash
xvfb-run -a -s "-screen 0 1920x1080x24" ./gradlew runClientGameTest
```

Screenshots taken by the tests are saved to `build/run/clientGameTest/screenshots/`.

The multiplayer tests start a local dedicated server, so the test setup in `build.gradle` accepts the [Minecraft EULA](https://aka.ms/MinecraftEULA) for that test server.

## License

[MIT](LICENSE)
