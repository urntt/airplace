package com.urntt.airplace.gametest;

import static com.urntt.airplace.gametest.GameTestSupport.airTarget;
import static com.urntt.airplace.gametest.GameTestSupport.bindKey;
import static com.urntt.airplace.gametest.GameTestSupport.check;
import static com.urntt.airplace.gametest.GameTestSupport.checkPlaced;
import static com.urntt.airplace.gametest.GameTestSupport.configure;
import static com.urntt.airplace.gametest.GameTestSupport.distance;
import static com.urntt.airplace.gametest.GameTestSupport.expectedTarget;
import static com.urntt.airplace.gametest.GameTestSupport.isEnabled;
import static com.urntt.airplace.gametest.GameTestSupport.loadSavedConfig;
import static com.urntt.airplace.gametest.GameTestSupport.modifierKey;
import static com.urntt.airplace.gametest.GameTestSupport.outlinePos;
import static com.urntt.airplace.gametest.GameTestSupport.prepareWorld;
import static com.urntt.airplace.gametest.GameTestSupport.unbindKey;
import static com.urntt.airplace.gametest.GameTestSupport.use;

import com.mojang.blaze3d.platform.InputConstants;
import com.urntt.airplace.AirPlaceClient;
import com.urntt.airplace.config.AirPlaceConfigScreen;
import com.urntt.airplace.config.MultiplayerMode;
import com.urntt.airplace.config.ServerListScreen;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Checks air placement, the distance adjustment, the toggle key, the reset on world exit, and the settings screens
 * in singleplayer worlds.
 */
@SuppressWarnings("UnstableApiUsage")
public final class AirPlaceClientGameTest implements FabricClientGameTest {
	/** View directions (yaw) with nothing in reach, one for each check that places a block. */
	private static final float SOUTH = 0.0F;
	private static final float WEST = 90.0F;
	private static final float NORTH = 180.0F;
	private static final float EAST = -90.0F;
	private static final float SOUTH_WEST = 45.0F;
	/** A placement distance vanilla servers reject: the target block is more than 5.5 blocks from the eyes. */
	private static final double OUT_OF_RANGE_DISTANCE = 7.0;
	/** Placement distance in the obstruction check, and the distance from the eyes' block to the wall in front of it. */
	private static final double OBSTRUCTED_DISTANCE = 4.0;
	private static final int WALL_DISTANCE = 3;
	/** Cursor position, in window pixels from the left, over the settings list but beside its widgets. */
	private static final double LIST_MARGIN_CURSOR_X = 10.0;
	/** Ticks to wait after scrolling the settings list before taking a screenshot. */
	private static final int SCROLL_SETTLE_TICKS = 5;

	@Override
	public void runTest(final ClientGameTestContext context) {
		KeyMapping modifierKey = modifierKey();
		// The run directory is deleted before each run, so these are the defaults of a fresh installation.
		check(modifierKey.getDefaultKey().equals(InputConstants.getKey("key.keyboard.r")), "the modifier key should default to R");
		check(context.computeOnClient(client -> modifierKey.isDefault()), "the modifier key should be bound to its default");
		check(distance(context) == 3.0, "the placement distance should default to 3.00");

		KeyMapping toggleKey = bindKey(context, AirPlaceClient.TOGGLE_KEY_NAME, "key.keyboard.j");
		KeyMapping openSettingsKey = bindKey(context, AirPlaceClient.OPEN_SETTINGS_KEY_NAME, "key.keyboard.k");

		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();
			TestServerContext server = singleplayer.getServer();
			prepareWorld(context, server);

			checkAirPlacement(context, server, modifierKey);
			checkDistanceAdjustment(context, server, modifierKey);
			checkOutOfVanillaRange(context, server, modifierKey);
			checkObstructedRay(context, server, modifierKey);
			checkToggle(context, server, modifierKey, toggleKey);
			checkSettingsScreens(context, openSettingsKey);

			// Leave this world disabled to check the reset on world exit below.
			configure(context, config -> config.setResetOnWorldExit(true));
			context.getInput().pressKey(toggleKey);
			context.waitTick();
			check(!isEnabled(context), "toggle key should disable air placement before leaving");
		}

		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();
			check(isEnabled(context), "reset on world exit should restore the singleplayer default");

			configure(context, config -> config.setResetOnWorldExit(false));
			context.getInput().pressKey(toggleKey);
			context.waitTick();
			check(!isEnabled(context), "toggle key should disable air placement before leaving");
		}

		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			singleplayer.getConnection().waitForChunksRender();
			check(!isEnabled(context), "without reset on world exit the state should carry over");
		}

		configure(context, config -> config.setEnabled(true));
		unbindKey(context, toggleKey);
		unbindKey(context, openSettingsKey);
		screenshotKeyBinds(context);
	}

	/**
	 * Checks that nothing happens in mid-air without the modifier key, and that with it the target is outlined and
	 * the use key places the held block there.
	 */
	private static void checkAirPlacement(final ClientGameTestContext context, final TestServerContext server,
			final KeyMapping modifierKey) {
		BlockPos target = expectedTarget(context, SOUTH);
		check(airTarget(context) == null, "there should be no air target without the modifier key");
		check(outlinePos(context) == null, "no outline should be drawn when looking into the air without the modifier key");
		use(context, null);
		checkPlaced(context, server, target, false, "using a block in mid-air without the modifier key");

		context.getInput().holdKey(modifierKey);
		check(target.equals(airTarget(context)), "the air target should be " + target.toShortString() + ", got " + airTarget(context));
		BlockPos outline = outlinePos(context);
		check(target.equals(outline), "the outline should surround the air target " + target.toShortString() + ", got " + outline);
		context.takeScreenshot("airplace-target-outline");
		context.getInput().releaseKey(modifierKey);

		use(context, modifierKey);
		checkPlaced(context, server, target, true, "using a block in mid-air with the modifier key held");
		BlockState below = context.computeOnClient(client -> client.level.getBlockState(target.below()));
		check(below.isAir(), "the placed block should float in mid-air, but the block below it is " + below);

		context.getInput().holdKey(modifierKey);
		check(airTarget(context) == null, "the ray should hit the placed block, which leaves no air target");
		context.getInput().releaseKey(modifierKey);
	}

	/**
	 * Checks that the mouse wheel changes the placement distance instead of the hotbar slot while the modifier key
	 * is held, that the distance stays within its limits and is saved, and that a placement uses it.
	 */
	private static void checkDistanceAdjustment(final ClientGameTestContext context, final TestServerContext server,
			final KeyMapping modifierKey) {
		int slot = selectedSlot(context);
		context.getInput().holdKey(modifierKey);
		context.getInput().scroll(1);
		context.waitTick();
		check(distance(context) == 3.1, "scrolling up with the modifier key should add the 0.10 scroll step, got " + distance(context));
		check(selectedSlot(context) == slot, "scrolling with the modifier key should not change the hotbar slot");
		context.takeScreenshot("airplace-distance-message");

		context.getInput().scroll(-2);
		context.waitTick();
		check(distance(context) == 2.9, "scrolling down two notches should subtract two scroll steps, got " + distance(context));

		context.getInput().scroll(100);
		context.waitTick();
		check(distance(context) == 4.5, "the distance should stop at the 4.50 maximum, got " + distance(context));
		context.getInput().releaseKey(modifierKey);
		check(loadSavedConfig().distance() == 4.5, "the distance should be saved to the config file");

		context.getInput().scroll(1);
		context.waitTick();
		check(selectedSlot(context) != slot, "scrolling without the modifier key should change the hotbar slot");
		check(distance(context) == 4.5, "scrolling without the modifier key should not change the distance");
		context.getInput().scroll(-1);
		context.waitTick();
		check(selectedSlot(context) == slot, "scrolling back should restore the hotbar slot");

		BlockPos target = expectedTarget(context, WEST);
		use(context, modifierKey);
		checkPlaced(context, server, target, true, "placing at the 4.50 maximum distance");

		context.getInput().holdKey(modifierKey);
		context.getInput().scroll(-100);
		context.waitTick();
		check(distance(context) == 0.0, "the distance should stop at the 0.00 minimum, got " + distance(context));
		context.getInput().releaseKey(modifierKey);
		configure(context, config -> config.setDistance(3.0));
	}

	/**
	 * Checks the limit the configuration screen notes: a vanilla server rejects a placement more than about 5 blocks
	 * away, even though the mod sends it.
	 */
	private static void checkOutOfVanillaRange(final ClientGameTestContext context, final TestServerContext server,
			final KeyMapping modifierKey) {
		configure(context, config -> {
			config.setMaxDistance(OUT_OF_RANGE_DISTANCE);
			config.setDistance(OUT_OF_RANGE_DISTANCE);
		});
		BlockPos target = expectedTarget(context, NORTH);
		context.getInput().holdKey(modifierKey);
		check(target.equals(airTarget(context)), "there should be an air target at the out-of-range distance");
		context.getInput().releaseKey(modifierKey);
		use(context, modifierKey);
		checkPlaced(context, server, target, false, "placing " + OUT_OF_RANGE_DISTANCE + " blocks away");

		configure(context, config -> {
			config.setMaxDistance(4.5);
			config.setDistance(3.0);
		});
	}

	/**
	 * Checks that a block within the placement distance leaves no air target, so the outline and the use key stay
	 * vanilla: vanilla outlines the block and places against it.
	 */
	private static void checkObstructedRay(final ClientGameTestContext context, final TestServerContext server,
			final KeyMapping modifierKey) {
		configure(context, config -> config.setDistance(OBSTRUCTED_DISTANCE));
		BlockPos behindWall = expectedTarget(context, EAST);
		BlockPos eyes = context.computeOnClient(client -> BlockPos.containing(client.player.getEyePosition()));
		BlockPos wall = eyes.relative(Direction.EAST, WALL_DISTANCE);
		server.runCommand("fill %d %d %d %d %d %d minecraft:stone".formatted(
				wall.getX(), wall.getY() - 2, wall.getZ() - 1, wall.getX(), wall.getY() + 1, wall.getZ() + 1));
		context.waitFor(client -> !client.level.getBlockState(wall).isAir());

		context.getInput().holdKey(modifierKey);
		check(airTarget(context) == null, "a block in front of the target should leave no air target");
		BlockPos outline = outlinePos(context);
		check(wall.equals(outline), "vanilla should outline the wall at " + wall.toShortString() + ", got " + outline);
		context.getInput().releaseKey(modifierKey);

		use(context, modifierKey);
		checkPlaced(context, server, behindWall, false, "using a block at a wall with the modifier key held, behind the wall");
		checkPlaced(context, server, wall.relative(Direction.WEST), true, "using a block at a wall with the modifier key held, in front of the wall");
		configure(context, config -> config.setDistance(3.0));
	}

	/**
	 * Checks that the toggle key turns air placement and the distance adjustment off and on, and saves the state.
	 */
	private static void checkToggle(final ClientGameTestContext context, final TestServerContext server,
			final KeyMapping modifierKey, final KeyMapping toggleKey) {
		check(isEnabled(context), "air placement should be enabled by default");
		context.getInput().pressKey(toggleKey);
		context.waitTick();
		check(!isEnabled(context), "toggle key should disable air placement");
		check(!loadSavedConfig().isEnabled(), "disabled state should be saved to the config file");
		context.takeScreenshot("airplace-toggled-off");

		BlockPos target = expectedTarget(context, SOUTH_WEST);
		context.getInput().holdKey(modifierKey);
		check(airTarget(context) == null, "there should be no air target while air placement is off");
		int slot = selectedSlot(context);
		context.getInput().scroll(1);
		context.waitTick();
		check(selectedSlot(context) != slot, "scrolling should change the hotbar slot while air placement is off");
		context.getInput().scroll(-1);
		context.waitTick();
		context.getInput().releaseKey(modifierKey);
		use(context, modifierKey);
		checkPlaced(context, server, target, false, "using a block in mid-air while air placement is off");

		context.getInput().pressKey(toggleKey);
		context.waitTick();
		check(isEnabled(context), "toggle key should enable air placement again");
		check(loadSavedConfig().isEnabled(), "enabled state should be saved to the config file");
		use(context, modifierKey);
		checkPlaced(context, server, target, true, "using a block in mid-air after enabling air placement again");
	}

	/**
	 * Opens the settings with the key binding, checks that the minimum and maximum distance keep each other in
	 * order, then takes screenshots of both settings screens in English and in Simplified Chinese.
	 */
	private static void checkSettingsScreens(final ClientGameTestContext context, final KeyMapping openSettingsKey) {
		context.getInput().pressKey(openSettingsKey);
		context.waitForScreen(AirPlaceConfigScreen.class);
		context.setScreen(() -> null);

		configure(context, config -> config.setMinDistance(5.0));
		check(context.computeOnClient(client -> AirPlaceClient.config().maxDistance()) == 5.0,
				"raising the minimum above the maximum should raise the maximum");
		check(distance(context) == 5.0, "the distance should follow the minimum");
		configure(context, config -> config.setMaxDistance(2.0));
		check(context.computeOnClient(client -> AirPlaceClient.config().minDistance()) == 2.0,
				"lowering the maximum below the minimum should lower the minimum");
		check(distance(context) == 2.0, "the distance should follow the maximum");
		configure(context, config -> {
			config.setMinDistance(0.0);
			config.setMaxDistance(4.5);
			config.setDistance(3.0);
		});

		configure(context, config -> {
			config.setMultiplayerMode(MultiplayerMode.WHITELIST);
			config.setServers(List.of("mc.example.com", "192.168.1.5"));
		});
		takeSettingsScreenshots(context, "en_us");
		switchLanguage(context, "zh_cn");
		takeSettingsScreenshots(context, "zh_cn");
		switchLanguage(context, "en_us");

		configure(context, config -> {
			config.setMultiplayerMode(MultiplayerMode.DISABLED);
			config.setServers(List.of());
		});
	}

	private static void takeSettingsScreenshots(final ClientGameTestContext context, final String language) {
		context.setScreen(() -> new AirPlaceConfigScreen(null));
		// Opening a screen centers the cursor. Move it to the list's empty left margin, where it still scrolls the
		// list but no tooltip covers the widgets.
		int height = context.computeOnClient(client -> client.getWindow().getScreenHeight());
		context.getInput().setCursorPos(LIST_MARGIN_CURSOR_X, height / 2.0);
		context.waitTick();
		context.takeScreenshot("airplace-config-screen-" + language);
		context.getInput().scroll(-5);
		context.waitTicks(SCROLL_SETTLE_TICKS);
		context.takeScreenshot("airplace-config-screen-middle-" + language);
		context.getInput().scroll(-Integer.MAX_VALUE);
		context.waitTicks(SCROLL_SETTLE_TICKS);
		context.takeScreenshot("airplace-config-screen-bottom-" + language);

		context.setScreen(() -> new ServerListScreen(new AirPlaceConfigScreen(null), AirPlaceClient.config()));
		context.takeScreenshot("airplace-server-list-" + language);
		context.setScreen(() -> null);
	}

	private static void switchLanguage(final ClientGameTestContext context, final String language) {
		CompletableFuture<Void> reload = context.computeOnClient(client -> {
			client.getLanguageManager().setSelected(language);
			client.options.languageCode = language;
			return client.reloadResourcePacks();
		});
		context.waitFor(client -> reload.isDone() && client.gui.overlay() == null);
	}

	/** Shows the mod's key binding category, which vanilla lists last, so the screenshot shows its translations. */
	private static void screenshotKeyBinds(final ClientGameTestContext context) {
		Options options = context.computeOnClient(client -> client.options);
		context.setScreen(() -> new KeyBindsScreen(null, options));
		int width = context.computeOnClient(client -> client.getWindow().getScreenWidth());
		int height = context.computeOnClient(client -> client.getWindow().getScreenHeight());
		context.getInput().setCursorPos(width / 2.0, height / 2.0);
		context.getInput().scroll(-Integer.MAX_VALUE);
		context.takeScreenshot("airplace-key-binds");
		context.setScreen(() -> null);
	}

	private static int selectedSlot(final ClientGameTestContext context) {
		return context.computeOnClient(client -> Objects.requireNonNull(client.player).getInventory().getSelectedSlot());
	}
}
