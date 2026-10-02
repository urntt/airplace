# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). Each version carries the targeted Minecraft version as build metadata, for example `1.0.0+26.3`.

## [Unreleased]

### Added

- Place blocks in mid-air: while the Air Place Modifier key (`R` by default) is held, the block at the placement distance in front of the player is outlined, and the use key places the held block there. Vanilla handles the placement as if the player had clicked that block's face.
- Change the placement distance with the mouse wheel while the modifier key is held. The action bar shows the new distance with two decimals. The distance starts at 3.00 and stays between a minimum (0.00 by default) and a maximum (4.50 by default), and each wheel notch changes it by the scroll step (0.10 by default).
- Add a configuration screen built from vanilla widgets, with the current state, the distance limits and scroll step, separate defaults for singleplayer worlds and allowed servers, and options to reset to the default on world exit or game exit. It notes that vanilla servers reject block placements more than about 5 blocks away. Settings are saved to `config/airplace.json`.
- Add multiplayer modes (disabled, whitelist, blacklist) and a server list screen for editing the addresses they use. Air placement is disabled on multiplayer servers by default.
- Add "Toggle Air Place" and "Open airplace Settings" key bindings, both unbound by default. The toggle key shows the new state on the action bar.
- Open the configuration screen from Mod Menu, which is optional.
- Add English and Simplified Chinese translations.
