# AGENTS.md

Instructions for AI coding agents working in this repository. `CLAUDE.md` imports this file, so keep all agent guidance here.

## Project

`airplace` is a client-side [Fabric](https://fabricmc.net/) mod for Minecraft: Java Edition that lets the local player place blocks in mid-air.

It is a sibling of [urntt/airjump](https://github.com/urntt/airjump), whose hold-a-key trigger it follows, and of [urntt/nojumpdelay](https://github.com/urntt/nojumpdelay), whose configuration, multiplayer rules and settings screens it follows. Both may be consulted for verified API usage and kept consistent with this mod.

## Project decisions

These decisions are settled. Do not deviate from them without the user's explicit approval.

### Minecraft and toolchain

- Development started on Minecraft 26.3 with Java 25. `gradle.properties` is the single source of truth for the mod, Minecraft, Fabric Loader, Loom, Fabric API, Mod Menu, and Java versions. `build.gradle`, `fabric.mod.json`, the mixin config, and the CI workflows read them from there; do not restate them elsewhere.
- Follow the latest official Fabric template ([FabricMC/fabric-example-mod](https://github.com/FabricMC/fabric-example-mod), also available from the [template generator](https://fabricmc.net/develop/template/)): the `net.fabricmc.fabric-loom` Gradle plugin, Mojang's official names with no `mappings` dependency, and `implementation` (not `modImplementation`) for dependencies. Do not use Yarn.
- Pin `loom_version` to a release version instead of the template's `-SNAPSHOT`, so builds are reproducible.
- Target only the latest stable (release) Minecraft version. Updates, fixes, and new features are always developed against it. Snapshots, pre-releases, and release candidates are not supported targets.
- Do not maintain older Minecraft versions and do not set up multi-version builds (no per-version branches, no Stonecutter or other preprocessors). When a new stable version is released, port the mod to it and drop the previous one.

### Identity

| Item | Value |
| --- | --- |
| Mod ID | `airplace` |
| Display name | `airplace` |
| Maven group | `com.urntt` |
| Base package | `com.urntt.airplace` |
| Version format | `<SemVer>+<Minecraft version>`, for example `1.0.0+26.3` |
| License | MIT |

The mod version itself follows [Semantic Versioning](https://semver.org/); the `+<Minecraft version>` suffix is build metadata naming the Minecraft version the build targets. Version numbers start at `1.0.0`.

### Distribution

- Releases are published only as GitHub Releases. Do not publish to Modrinth, CurseForge, or any other mod platform, and do not add publishing tooling for them.
- `README.md` and the configuration screen must clearly warn that placing blocks in mid-air is not possible in vanilla: servers that run anti-cheat systems may detect it, and using it in multiplayer may get the player kicked or banned. They must state that the mod does not try to bypass any anti-cheat, and that it is disabled in multiplayer by default and has to be enabled on the configuration screen.

### Scope and behavior

- Client-only: `fabric.mod.json` declares `"environment": "client"`. There is no server-side component and no networking of its own.
- The mod only adds air placement. It must not extend the reach of normal interactions, change block breaking, automate placement, or add anything that hides or disguises what the client sends (no packet or rotation spoofing). It does not try to bypass any anti-cheat.
- Only the local player's own interactions are affected.
- Air placement reuses vanilla instead of implementing placement itself. Do not change this approach without the user's explicit approval:
  - The Air Place mode is on while the **Air Place Modifier** key (default `R`) is held, `AirPlaceController` reports the feature active, and the player may build (`Player.mayBuild()`, which rules out spectator and adventure mode). Then the mod casts the same ray vanilla uses for the crosshair (`Entity.pick`, block outline shapes, no fluids) with the placement distance as its length.
  - If the ray hits a block, there is no air target and everything stays vanilla. If it misses, the resulting `BlockHitResult` miss carries the end point in the air, the face pointing back toward the player, and the block containing the end point. That block is the air target.
  - The air target is drawn with vanilla's block outline, and when vanilla starts using an item (`Minecraft.startUseItem`), it receives a regular (non-miss) copy of the air target instead of the crosshair target. Vanilla then handles everything else as if the player had clicked that face: hand order, what the held item does, placement rules, the arm swing, the use cooldown, and the `ServerboundUseItemOnPacket`.
- While the Air Place mode is on, the mouse wheel changes the placement distance by the scroll step per wheel notch instead of switching hotbar slots, and the action bar shows the new distance with two decimals. The distance stays within the configured minimum and maximum and is saved to the configuration file.
- Distance settings: the placement distance is measured from the player's eyes along the view direction. Defaults: distance `3.00`, minimum `0.00`, maximum `4.50`, scroll step `0.10`. All are kept to two decimals. The minimum and maximum accept up to `10.00`, and the scroll step `0.01` to `1.00`; raising the minimum above the maximum raises the maximum, and lowering the maximum below the minimum lowers the minimum. The configuration screen notes that vanilla servers reject block placements more than about 5 blocks away (vanilla's limit is the block interaction range plus one block, measured to the nearest point of the block: 5.5 in survival, 6 in creative).
- The configuration follows [urntt/nojumpdelay](https://github.com/urntt/nojumpdelay), and changes to one should be considered for the other:
  - `AirPlaceController` is the single owner of whether the feature is active: the toggle state is on and the current scene (singleplayer or a multiplayer server, determined on join) is allowed.
  - The toggle state is enabled by default. A configurable key binding toggles it. It is unbound by default. Each toggle shows the new state on the action bar and saves it to the configuration file, so it persists across game restarts.
  - Defaults depend on the scene: a singleplayer default (worlds hosted by this client, including ones opened to LAN) and a server default (servers the multiplayer mode allows). Both are on by default.
  - Reset rules restore the scene's default when the player joins an allowed scene: "reset on world exit" for every world, and "reset on game exit" for the first allowed world after the game starts. Both are off by default. Resets happen on join so that they use the next scene's default and still work after a crash.
  - The multiplayer mode is a hard limit: `DISABLED` (the default) rules out every server, `WHITELIST` allows only servers in the server list, and `BLACKLIST` allows every server except those in it. On a ruled-out server air placement stays off and the toggle key only reports that the mod is disabled there. Joining another player's LAN world or a Realm counts as multiplayer.
  - Server list entries match the connected address by host (case-insensitive, after IDN conversion, and required to be a valid domain name or IP address) and by port only when the entry specifies one.
  - The configuration screen is built from vanilla widgets and opens through Mod Menu or a second key binding, "open settings", which is also unbound by default.

### Localization

- All user-facing text, including key binding names, the key binding category, action bar messages, and the configuration screen, uses translation keys. Never hard-code display strings.
- Provide translations for `en_us` and `zh_cn`, and keep both complete whenever a translation key is added or changed.

### Dependencies

- Required: Fabric Loader and Fabric API.
- Optional: Mod Menu, declared under `suggests` in `fabric.mod.json`. The mod must load and work normally without it, so Mod Menu classes may only be referenced from the Mod Menu entrypoint.
- Configuration is hand-written without a config library: a JSON file in the Fabric config directory, serialized with Gson (bundled with Minecraft). Any configuration screen uses vanilla widgets.
- Do not add other dependencies without the user's explicit approval.

### Implementation

- Language: Java only.
- Source sets: `src/main` holds only `fabric.mod.json` and the icon. All code and client resources live in `src/client`, and the client game tests live in `src/gametest`.
- `AirPlacement` holds the air placement logic: whether the Air Place mode is on, finding the air target, the outline, and the mouse wheel. The outline uses Fabric API's `LevelExtractionEvents.AFTER_BLOCK_OUTLINE_EXTRACTION` to replace the `BlockOutlineRenderState` vanilla has just extracted, so vanilla draws it in its own style and under its own rules (for example not while the HUD is hidden).
- Mixins: Fabric API has no events for item use or in-game scrolling, so two mixins remain. `MinecraftMixin` finds the air target at the head of `Minecraft.startUseItem` and hands it to every read of `hitResult` in that method (`@ModifyExpressionValue` with a shared local). `MouseHandlerMixin` wraps the `ScrollWheelHandler.getNextScrollWheelSelection` call in `MouseHandler.onScroll`, so it reuses the wheel notches vanilla has already counted, including its sensitivity and discrete scrolling options. Prefer Fabric API events over new mixins. Where a mixin is necessary, prefer the MixinExtras injectors bundled with Fabric Loader (for example `@ModifyExpressionValue` and `@WrapOperation`) over `@Redirect` and `@Overwrite`, to stay compatible with other mods and keep porting work small.

### Testing

- The client game tests in `src/gametest` start Minecraft and check air placement by reading the placed block on the client and on the server (`GameTestSupport`). They cover the address matching, the defaults, the distance rules and the reset rules (`AirPlaceLogicGameTest`); the default modifier key binding, the air target and its outline, placement with and without the modifier key, the distance adjustment and its limits, the rejection of placements beyond vanilla's range, a ray blocked by a wall, the toggle key, reset on world exit and the settings screens in singleplayer (`AirPlaceClientGameTest`); and each multiplayer mode on a local dedicated server (`AirPlaceMultiplayerGameTest`). Keep them passing and extend them when behavior changes.
- The dedicated server needs `eula = true` in the `configureTests` block of `build.gradle`; it accepts the Minecraft EULA only for that local test server. The multiplayer test also starts it with `spawn-protection=0`, because the test places blocks next to the spawn point.
- After porting to a new Minecraft version, run the client game tests. A successful build does not prove that the mixins still have the intended effect.
- `README.md` describes how to run them, including on a headless machine.

### CI, releases, and changelog

- GitHub Actions (`.github/workflows/build.yml`) builds the project and runs the client game tests on every push and pull request.
- Maintain `CHANGELOG.md` following [Keep a Changelog](https://keepachangelog.com/). Record every user-visible change under `Unreleased` in the same change that introduces it.
- `.github/workflows/release.yml` builds the mod and publishes a GitHub Release for the project version, with the jar attached and the matching `CHANGELOG.md` section as release notes. It runs when a tag `v<version>` (for example `v1.0.0+26.3`) is pushed, or when started manually on a branch, in which case it creates that tag on the branch's latest commit. It fails if a pushed tag does not match the project version, if the changelog has no section for the version, or if the release already exists.
- Release only when the user asks. To release, set `mod_version` in `gradle.properties`, rename `Unreleased` in `CHANGELOG.md` to `[<version>] - <YYYY-MM-DD>` above a new empty `Unreleased` section, commit, and push. Then start the release workflow on `main`. Claude Code cloud sessions cannot push tags, so start the workflow through the GitHub Actions API instead.

## Engineering principles

- Fix root causes, not symptoms. Diagnose the underlying cause before implementing a permanent fix. If an immediate mitigation is necessary, treat it as temporary and follow through with a root-cause fix.
- Prefer configuration-driven design for values that are expected to vary by environment, deployment, or product requirements. Avoid unexplained or duplicated magic values, but do not introduce configuration where a well-named constant is the clearer source of truth.
- Preserve a single source of truth and clear ownership for data, state, configuration, business logic, and authoritative documentation. Avoid duplicating canonical information across multiple locations.
- Do not maintain parallel legacy and replacement implementations without an explicit migration and removal plan.

## Documentation

- Keep documentation aligned with the code. When a code change affects documented behavior, APIs, architecture, configuration, workflows, or usage, update the relevant documentation in the same change.
- Keep each document's responsibility clear. For example, use `README.md` for project overview and usage, and `VISION.md` for product direction, architectural principles, or long-term decisions.
- Always specify a language identifier for fenced code blocks in Markdown.

## Language

- Communicate with the user in Chinese, including explanations, progress updates, and user-facing planning.
- Use English for development artifacts, including source code, comments, docstrings, documentation, READMEs, Git branch names, commit messages, and other deliverables intended to live in the repository.

## Git

- Do not change or override the Git author or committer identity. When an identity must be configured for commits created during the task, use:
  - Name: `urntt`
  - Email: `urntts@gmail.com`
- Do all actions on the user's behalf. Do not rewrite existing commit authorship unless explicitly requested. Do not add `Co-Authored-By` trailers or session links to commit messages or pull request descriptions.
- Develop on `main` and push directly to it. Branches and pull requests are not required.
- Because changes land on `main` without review, make sure `./gradlew build` and the client game tests pass locally before pushing.
- If a branch is used, give it a category-based prefix that reflects the purpose of the change, such as `feat/`, `fix/`, `refactor/`, `docs/`, `test/`, or `chore/`.
- Follow the [Conventional Commits](https://www.conventionalcommits.org/) specification for commit messages.
