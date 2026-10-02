package com.urntt.airplace.gametest;

import static com.urntt.airplace.gametest.GameTestSupport.airTarget;
import static com.urntt.airplace.gametest.GameTestSupport.bindKey;
import static com.urntt.airplace.gametest.GameTestSupport.check;
import static com.urntt.airplace.gametest.GameTestSupport.checkPlaced;
import static com.urntt.airplace.gametest.GameTestSupport.configure;
import static com.urntt.airplace.gametest.GameTestSupport.expectedTarget;
import static com.urntt.airplace.gametest.GameTestSupport.isEnabled;
import static com.urntt.airplace.gametest.GameTestSupport.modifierKey;
import static com.urntt.airplace.gametest.GameTestSupport.prepareWorld;
import static com.urntt.airplace.gametest.GameTestSupport.unbindKey;
import static com.urntt.airplace.gametest.GameTestSupport.use;

import com.urntt.airplace.AirPlaceClient;
import com.urntt.airplace.Scene;
import com.urntt.airplace.config.MultiplayerMode;
import java.util.List;
import java.util.Properties;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerConnection;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;

/**
 * Checks the multiplayer modes on a local dedicated server, which the client reaches as {@code localhost}. The
 * server is vanilla, so a successful placement also shows that it accepts what the mod sends.
 */
@SuppressWarnings("UnstableApiUsage")
public final class AirPlaceMultiplayerGameTest implements FabricClientGameTest {
	@Override
	public void runTest(final ClientGameTestContext context) {
		configure(context, config -> {
			config.setEnabled(true);
			config.setMultiplayerDefault(true);
			config.setResetOnWorldExit(false);
			config.setMultiplayerMode(MultiplayerMode.DISABLED);
			config.setServers(List.of());
			config.setDistance(3.0);
		});
		KeyMapping modifierKey = modifierKey();
		KeyMapping toggleKey = bindKey(context, AirPlaceClient.TOGGLE_KEY_NAME, "key.keyboard.j");

		// The test player places blocks next to the spawn point, which must not be protected.
		Properties serverProperties = new Properties();
		serverProperties.setProperty("spawn-protection", "0");
		try (TestDedicatedServerContext server = context.worldBuilder().createServer(serverProperties);
				TestDedicatedServerConnection connection = server.connect()) {
			connection.waitForChunksRender();
			prepareWorld(context, server);
			Scene scene = context.computeOnClient(client -> AirPlaceClient.controller().scene());
			GameTestSupport.LOGGER.info("Connected to {}", scene);
			check(scene instanceof Scene.Multiplayer, "a dedicated server should count as multiplayer, got " + scene);

			BlockPos south = expectedTarget(context, 0.0F);
			context.getInput().holdKey(modifierKey);
			check(airTarget(context) == null, "there should be no air target on a server with multiplayer disabled");
			context.getInput().releaseKey(modifierKey);
			use(context, modifierKey);
			checkPlaced(context, server, south, false, "using a block in mid-air on a server with multiplayer disabled");
			context.getInput().pressKey(toggleKey);
			context.waitTick();
			check(isEnabled(context), "a blocked toggle should leave the state unchanged");
			context.takeScreenshot("airplace-blocked-on-server");

			configure(context, config -> {
				config.setServers(List.of("localhost"));
				config.setMultiplayerMode(MultiplayerMode.WHITELIST);
			});
			BlockPos west = expectedTarget(context, 90.0F);
			use(context, modifierKey);
			checkPlaced(context, server, west, true, "using a block in mid-air on a whitelisted server");

			configure(context, config -> config.setMultiplayerMode(MultiplayerMode.BLACKLIST));
			BlockPos north = expectedTarget(context, 180.0F);
			use(context, modifierKey);
			checkPlaced(context, server, north, false, "using a block in mid-air on a blacklisted server");
		}

		configure(context, config -> {
			config.setMultiplayerMode(MultiplayerMode.DISABLED);
			config.setServers(List.of());
		});
		unbindKey(context, toggleKey);
	}
}
