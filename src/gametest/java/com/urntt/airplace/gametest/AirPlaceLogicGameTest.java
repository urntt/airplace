package com.urntt.airplace.gametest;

import static com.urntt.airplace.gametest.GameTestSupport.check;

import com.urntt.airplace.AirPlaceController;
import com.urntt.airplace.AirPlacement;
import com.urntt.airplace.Scene;
import com.urntt.airplace.ServerAddresses;
import com.urntt.airplace.config.AirPlaceConfig;
import com.urntt.airplace.config.MultiplayerMode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;

/**
 * Checks the address matching, the configuration defaults, the distance rules, and the controller's rules without a
 * world. Each check uses its own configuration file, so the game's configuration is left alone. Restarting the game
 * is not possible in a game test, so this is also where "reset on game exit" is covered.
 */
@SuppressWarnings("UnstableApiUsage")
public final class AirPlaceLogicGameTest implements FabricClientGameTest {
	private static final Scene SERVER_A = Scene.multiplayer("a.example.com");
	private static final Scene SERVER_B = Scene.multiplayer("b.example.com:25570");
	private static final Scene UNKNOWN_SERVER = Scene.multiplayer(null);

	@Override
	public void runTest(final ClientGameTestContext context) {
		Path directory = createTempDirectory();
		checkAddressMatching();
		checkDefaults(directory);
		checkUpgradeFromOlderConfig(directory);
		checkDistanceRules(directory);
		checkMultiplayerRules(directory);
		checkResetRules(directory);
		GameTestSupport.LOGGER.info("Logic checks passed");
	}

	private static void checkAddressMatching() {
		checkMatch("mc.example.com", "mc.example.com", true);
		checkMatch("MC.Example.com", "mc.example.COM:25565", true);
		checkMatch("mc.example.com", "mc.example.com:25566", true);
		checkMatch("mc.example.com:25565", "mc.example.com", true);
		checkMatch("mc.example.com:25566", "mc.example.com", false);
		checkMatch("mc.example.com", "play.example.com", false);
		checkMatch("  mc.example.com  ", "mc.example.com", true);
		checkMatch("192.168.1.5", "192.168.1.5:51234", true);
		checkMatch("[::1]:25565", "[::1]", true);
		checkMatch("::1", "[::1]:25570", true);
		checkMatch("bücher.example", "xn--bcher-kva.example", true);
		checkMatch("localhost", "localhost:41234", true);

		for (String valid : List.of("mc.example.com:25566", "localhost", "192.168.1.5", "[::1]:25565", "bücher.example")) {
			check(ServerAddresses.isValid(valid), "'" + valid + "' should be valid");
		}
		for (String invalid : List.of("", "   ", "mc.example.com:abc", "mc.example.com:99999", "[::1", "mc example.com",
				"not a host:port:x")) {
			check(!ServerAddresses.isValid(invalid), "'" + invalid + "' should be invalid");
		}
	}

	private static void checkMatch(final String entry, final String address, final boolean expected) {
		check(ServerAddresses.matches(entry, address) == expected,
				"'" + entry + "' should " + (expected ? "" : "not ") + "match '" + address + "'");
	}

	private static void checkDefaults(final Path directory) {
		AirPlaceConfig config = AirPlaceConfig.load(directory.resolve("defaults.json"));
		check(config.isEnabled(), "the feature should be enabled by default");
		check(config.singleplayerDefault(), "the singleplayer default should be on");
		check(config.multiplayerDefault(), "the server default should be on");
		check(!config.resetOnWorldExit(), "reset on world exit should be off by default");
		check(!config.resetOnGameExit(), "reset on game exit should be off by default");
		check(config.multiplayerMode() == MultiplayerMode.DISABLED, "multiplayer should be disabled by default");
		check(config.servers().isEmpty(), "the server list should be empty by default");
		check(config.distance() == 3.0, "the placement distance should default to 3.00");
		check(config.minDistance() == 0.0, "the minimum distance should default to 0.00");
		check(config.maxDistance() == 4.5, "the maximum distance should default to 4.50");
		check(config.scrollStep() == 0.1, "the scroll step should default to 0.10");
		check(Files.exists(directory.resolve("defaults.json")), "a missing config file should be created");
	}

	private static void checkUpgradeFromOlderConfig(final Path directory) {
		Path path = directory.resolve("upgrade.json");
		write(path, "{\"enabled\": false, \"multiplayerMode\": \"bogus\", \"servers\": [\" a.example.com \", \"\"]}");

		AirPlaceConfig config = AirPlaceConfig.load(path);
		check(!config.isEnabled(), "existing settings should be kept");
		check(config.multiplayerMode() == MultiplayerMode.DISABLED, "an unknown mode should fall back to disabled");
		check(config.servers().equals(List.of("a.example.com")), "entries should be trimmed and blanks dropped");
		check(read(path).contains("\"resetOnWorldExit\""), "missing settings should be written to the file");
		check(read(path).contains("\"scrollStep\""), "missing distance settings should be written to the file");

		Path distances = directory.resolve("distances.json");
		write(distances, "{\"distance\": 9.876, \"minDistance\": 6, \"maxDistance\": 12, \"scrollStep\": 0}");
		config = AirPlaceConfig.load(distances);
		check(config.maxDistance() == AirPlaceConfig.DISTANCE_LIMIT, "the maximum distance should be capped at the limit");
		check(config.minDistance() == 6.0, "a minimum within the maximum should be kept");
		check(config.distance() == 9.88, "the distance should be rounded to two decimals, got " + config.distance());
		check(config.scrollStep() == AirPlaceConfig.MIN_SCROLL_STEP, "a scroll step of zero should be raised to the smallest step");

		write(distances, "{\"distance\": 1, \"minDistance\": 4, \"maxDistance\": 2}");
		config = AirPlaceConfig.load(distances);
		check(config.minDistance() == 2.0 && config.maxDistance() == 2.0, "a minimum above the maximum should be lowered to it");
		check(config.distance() == 2.0, "the distance should be moved into the range");
	}

	private static void checkDistanceRules(final Path directory) {
		AirPlaceConfig config = AirPlaceConfig.load(directory.resolve("distance.json"));
		double distance = config.distance();
		for (int notch = 0; notch < 7; notch++) {
			distance = config.setDistance(distance + config.scrollStep());
		}
		check(distance == 3.7, "seven 0.10 steps from 3.00 should reach exactly 3.70, got " + distance);
		check(AirPlacement.format(distance).equals("3.70"), "the distance should be shown with two decimals");
		check(config.setDistance(4.51) == 4.5, "the distance should stop at the maximum");
		check(config.setDistance(-1.0) == 0.0, "the distance should stop at the minimum");
		check(config.setDistance(Double.NaN) == 3.0, "a distance that is not a number should fall back to the default");

		config.setDistance(4.0);
		config.setMaxDistance(3.5);
		check(config.distance() == 3.5, "lowering the maximum should pull the distance down");
		config.setMinDistance(3.75);
		check(config.maxDistance() == 3.75 && config.distance() == 3.75, "raising the minimum should push the maximum and the distance up");
		config.setMinDistance(AirPlaceConfig.DISTANCE_LIMIT + 1.0);
		check(config.minDistance() == AirPlaceConfig.DISTANCE_LIMIT, "the minimum distance should be capped at the limit");

		config.setScrollStep(2.0);
		check(config.scrollStep() == AirPlaceConfig.MAX_SCROLL_STEP, "the scroll step should be capped");
		config.setScrollStep(0.254);
		check(config.scrollStep() == 0.25, "the scroll step should be rounded to two decimals, got " + config.scrollStep());

		AirPlaceConfig reloaded = AirPlaceConfig.load(directory.resolve("distance.json"));
		check(reloaded.scrollStep() == 0.25 && reloaded.minDistance() == AirPlaceConfig.DISTANCE_LIMIT,
				"distance settings should be saved immediately");
	}

	private static void checkMultiplayerRules(final Path directory) {
		AirPlaceConfig config = AirPlaceConfig.load(directory.resolve("rules.json"));
		AirPlaceController controller = new AirPlaceController(config);

		check(!controller.isActive(), "nothing should be active outside a world");
		controller.onJoin(Scene.SINGLEPLAYER);
		check(controller.isActive(), "singleplayer should always be allowed");

		controller.onJoin(SERVER_A);
		check(!controller.isAllowed(), "the disabled mode should rule out every server");
		check(controller.toggle() == AirPlaceController.ToggleResult.BLOCKED, "toggling should be blocked");
		check(config.isEnabled(), "a blocked toggle should leave the state unchanged");

		config.setServers(List.of("a.example.com"));
		config.setMultiplayerMode(MultiplayerMode.WHITELIST);
		check(controller.isActive(), "a whitelisted server should be allowed");
		controller.onJoin(SERVER_B);
		check(!controller.isAllowed(), "a server missing from the whitelist should be ruled out");
		controller.onJoin(UNKNOWN_SERVER);
		check(!controller.isAllowed(), "an unknown address should not count as whitelisted");

		config.setMultiplayerMode(MultiplayerMode.BLACKLIST);
		check(controller.isAllowed(), "an unknown address should not count as blacklisted");
		controller.onJoin(SERVER_B);
		check(controller.isAllowed(), "a server missing from the blacklist should be allowed");
		controller.onJoin(SERVER_A);
		check(!controller.isAllowed(), "a blacklisted server should be ruled out");

		controller.onDisconnect();
		check(!controller.isActive(), "nothing should be active after disconnecting");
	}

	private static void checkResetRules(final Path directory) {
		AirPlaceConfig config = AirPlaceConfig.load(directory.resolve("reset.json"));
		config.setResetOnGameExit(true);
		config.setEnabled(false);

		// A fresh controller stands for a game that has just started.
		AirPlaceController controller = new AirPlaceController(config);
		controller.onJoin(SERVER_A);
		check(!config.isEnabled(), "a ruled-out server should not apply the game exit reset");
		controller.onDisconnect();
		controller.onJoin(Scene.SINGLEPLAYER);
		check(config.isEnabled(), "the first allowed world after starting should restore the default");
		controller.onDisconnect();
		config.setEnabled(false);
		controller.onJoin(Scene.SINGLEPLAYER);
		check(!config.isEnabled(), "later worlds should keep the state without reset on world exit");
		controller.onDisconnect();

		config.setResetOnWorldExit(true);
		controller.onJoin(Scene.SINGLEPLAYER);
		check(config.isEnabled(), "reset on world exit should restore the singleplayer default");
		controller.onDisconnect();

		config.setMultiplayerMode(MultiplayerMode.WHITELIST);
		config.setServers(List.of("a.example.com"));
		config.setMultiplayerDefault(false);
		controller.onJoin(SERVER_A);
		check(!config.isEnabled(), "an allowed server should restore the server default");
		controller.onDisconnect();
	}

	private static Path createTempDirectory() {
		try {
			return Files.createTempDirectory("airplace-gametest");
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static void write(final Path path, final String content) {
		try {
			Files.writeString(path, content);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private static String read(final Path path) {
		try {
			return Files.readString(path);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
