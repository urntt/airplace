package com.urntt.airplace.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.urntt.airplace.AirPlaceClient;
import com.urntt.airplace.AirPlacement;
import com.urntt.airplace.config.AirPlaceConfig;
import java.util.Objects;
import java.util.function.Consumer;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared helpers for the client game tests.
 *
 * <p>Air placement is checked by its result: the test looks in a direction where nothing is in reach, presses the
 * use key, and reads the block at the position the placement distance points to, on the client and on the server.
 */
@SuppressWarnings("UnstableApiUsage")
final class GameTestSupport {
	static final Logger LOGGER = LoggerFactory.getLogger("airplace-gametest");

	/** The block the player holds and places. */
	static final Block PLACED_BLOCK = Blocks.STONE;
	/** Ticks to wait for the server's answer to a placement, generous even for a dedicated server. */
	private static final int SERVER_ANSWER_TICKS = 20;

	private GameTestSupport() {
	}

	/**
	 * Puts the player in survival mode on the ground with a stack of {@link #PLACED_BLOCK} in the first hotbar slot.
	 */
	static void prepareWorld(final ClientGameTestContext context, final TestServerContext server) {
		server.runCommand("gamemode survival @a");
		server.runCommand("clear @a");
		server.runCommand("give @a minecraft:stone 64");
		context.waitFor(client -> client.player != null && client.player.onGround()
				&& client.player.getInventory().getItem(0).is(PLACED_BLOCK.asItem()));
		context.runOnClient(client -> client.player.getInventory().setSelectedSlot(0));
	}

	static KeyMapping modifierKey() {
		return Objects.requireNonNull(KeyMapping.get(AirPlaceClient.MODIFIER_KEY_NAME), "modifier key mapping");
	}

	/**
	 * Returns the block the placement distance points to when looking straight along {@code yaw}, computed
	 * independently of the mod from the player's eyes and view direction.
	 */
	static BlockPos expectedTarget(final ClientGameTestContext context, final float yaw) {
		context.getInput().lookAt(yaw, 0.0F);
		context.waitTick();
		return context.computeOnClient(client -> {
			Vec3 eyes = client.player.getEyePosition();
			Vec3 view = client.player.getViewVector(1.0F);
			return BlockPos.containing(eyes.add(view.scale(AirPlaceClient.config().distance())));
		});
	}

	/**
	 * Returns the mod's current air target, or {@code null} if it has none.
	 */
	static @Nullable BlockPos airTarget(final ClientGameTestContext context) {
		return context.computeOnClient(client -> {
			BlockHitResult target = AirPlacement.findTarget(client, 1.0F);
			return target != null ? target.getBlockPos() : null;
		});
	}

	/**
	 * Returns the position of the block outline vanilla is about to draw, or {@code null} if it draws none.
	 */
	static @Nullable BlockPos outlinePos(final ClientGameTestContext context) {
		context.waitTick();
		return context.computeOnClient(client -> {
			BlockOutlineRenderState outline = client.gameRenderer.gameRenderState().levelRenderState.blockOutlineRenderState;
			return outline != null ? outline.pos() : null;
		});
	}

	/**
	 * Presses the use key once, with the modifier key held if {@code modifierKey} is not {@code null}, and waits for
	 * the server to answer.
	 */
	static void use(final ClientGameTestContext context, final @Nullable KeyMapping modifierKey) {
		if (modifierKey != null) {
			context.getInput().holdKey(modifierKey);
		}
		context.getInput().pressKey(options -> options.keyUse);
		if (modifierKey != null) {
			context.getInput().releaseKey(modifierKey);
		}
		context.waitTicks(SERVER_ANSWER_TICKS);
	}

	/**
	 * Checks whether {@link #PLACED_BLOCK} is at {@code pos}, both on the client and on the server.
	 */
	static void checkPlaced(final ClientGameTestContext context, final TestServerContext server, final BlockPos pos,
			final boolean expected, final String situation) {
		BlockState clientState = context.computeOnClient(client -> client.level.getBlockState(pos));
		BlockState serverState = server.computeOnServer(s -> s.overworld().getBlockState(pos));
		LOGGER.info("{}: block at {} is {} on the client and {} on the server", situation, pos.toShortString(),
				clientState.getBlock().getName().getString(), serverState.getBlock().getName().getString());
		check(clientState.is(PLACED_BLOCK) == expected && serverState.is(PLACED_BLOCK) == expected,
				situation + ": expected " + (expected ? "" : "no ") + PLACED_BLOCK + " at " + pos.toShortString()
						+ ", but the client has " + clientState + " and the server has " + serverState);
	}

	/**
	 * Binds {@code mapping} to the key named {@code key} for the test.
	 */
	static void bindKey(final ClientGameTestContext context, final KeyMapping mapping, final String key) {
		context.runOnClient(client -> {
			mapping.setKey(InputConstants.getKey(key));
			KeyMapping.resetMapping();
		});
	}

	/**
	 * Returns the mod's key mapping registered under {@code name}, bound to {@code key} for the test.
	 */
	static KeyMapping bindKey(final ClientGameTestContext context, final String name, final String key) {
		KeyMapping mapping = Objects.requireNonNull(KeyMapping.get(name), name);
		bindKey(context, mapping, key);
		return mapping;
	}

	static void unbindKey(final ClientGameTestContext context, final KeyMapping mapping) {
		context.runOnClient(client -> {
			mapping.setKey(mapping.getDefaultKey());
			KeyMapping.resetMapping();
		});
	}

	/**
	 * Changes the mod's configuration on the client thread.
	 */
	static void configure(final ClientGameTestContext context, final Consumer<AirPlaceConfig> change) {
		context.runOnClient(client -> change.accept(AirPlaceClient.config()));
	}

	static boolean isEnabled(final ClientGameTestContext context) {
		return context.computeOnClient(client -> AirPlaceClient.config().isEnabled());
	}

	static double distance(final ClientGameTestContext context) {
		return context.computeOnClient(client -> AirPlaceClient.config().distance());
	}

	static AirPlaceConfig loadSavedConfig() {
		return AirPlaceConfig.load(AirPlaceConfig.defaultPath());
	}

	static void check(final boolean condition, final String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
