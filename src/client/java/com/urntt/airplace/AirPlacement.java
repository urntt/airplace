package com.urntt.airplace;

import com.urntt.airplace.config.AirPlaceConfig;
import java.util.Locale;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.Shapes;
import org.jspecify.annotations.Nullable;

/**
 * Air placement, built on vanilla's own interaction code.
 *
 * <p>While the Air Place mode is on, the mod casts the same ray vanilla uses for the crosshair, with the placement
 * distance as its length. When the ray hits nothing, vanilla still returns a {@link BlockHitResult} miss that carries
 * the end point in the air, the face pointing back toward the player, and the block containing the end point. That
 * block is the air target: it is outlined like a block under the crosshair, and a regular (non-miss) copy of the
 * result is handed to vanilla's item use, which then places the block as if the player had clicked that face.
 */
public final class AirPlacement {
	/** Translation key of the placement distance's name, shared by the action bar message and the settings. */
	public static final String DISTANCE_NAME_KEY = "options.airplace.distance";
	public static final Component DISTANCE_NAME = Component.translatable(DISTANCE_NAME_KEY);

	private AirPlacement() {
	}

	/**
	 * Returns whether the Air Place mode is on: the modifier key is held, the feature is active in the current
	 * scene, and the player may build (not in spectator or adventure mode).
	 */
	public static boolean isModeOn(final Minecraft minecraft) {
		LocalPlayer player = minecraft.player;
		return player != null && player.mayBuild() && AirPlaceClient.modifierKey().isDown()
				&& AirPlaceClient.controller().isActive();
	}

	/**
	 * Returns the air target as a regular block hit, or {@code null} if the Air Place mode is off, the ray hits a
	 * block within the placement distance, or the block at its end is not empty space that a placement would
	 * replace (air, a fluid, or a plant such as short grass).
	 *
	 * @param partialTicks the same partial tick vanilla uses for its crosshair target at this point
	 */
	public static @Nullable BlockHitResult findTarget(final Minecraft minecraft, final float partialTicks) {
		Entity cameraEntity = minecraft.getCameraEntity();
		if (cameraEntity == null || minecraft.level == null || !isModeOn(minecraft)) {
			return null;
		}

		HitResult hitResult = cameraEntity.pick(AirPlaceClient.config().distance(), partialTicks, false);
		if (!(hitResult instanceof BlockHitResult miss) || miss.getType() != HitResult.Type.MISS) {
			return null;
		}
		BlockPos pos = miss.getBlockPos();
		if (!minecraft.level.getBlockState(pos).canBeReplaced() || !minecraft.level.getWorldBorder().isWithinBounds(pos)) {
			return null;
		}
		return new BlockHitResult(miss.getLocation(), miss.getDirection(), pos, false);
	}

	/**
	 * Replaces the block outline vanilla has just extracted with a full block outline around the air target, so
	 * vanilla draws it in its own style. Vanilla's rules for showing the outline, such as a hidden HUD, still apply.
	 */
	public static void extractOutline(final LevelExtractionContext context) {
		Minecraft minecraft = Minecraft.getInstance();
		BlockHitResult target = findTarget(minecraft, context.deltaTracker().getGameTimeDeltaPartialTick(false));
		if (target != null) {
			boolean highContrast = minecraft.options.highContrastBlockOutline().get();
			context.levelState().blockOutlineRenderState =
					new BlockOutlineRenderState(target.getBlockPos(), false, highContrast, Shapes.block());
		}
	}

	/**
	 * Handles mouse wheel notches that vanilla would use to switch hotbar slots. While the Air Place mode is on, they
	 * change the placement distance by the scroll step each, and the action bar shows the new distance.
	 *
	 * @return whether the notches were used, so the hotbar selection must stay unchanged
	 */
	public static boolean scroll(final Minecraft minecraft, final int wheel) {
		LocalPlayer player = minecraft.player;
		if (player == null || !isModeOn(minecraft)) {
			return false;
		}

		AirPlaceConfig config = AirPlaceClient.config();
		double distance = config.setDistance(config.distance() + wheel * config.scrollStep());
		player.sendOverlayMessage(Options.genericValueLabel(DISTANCE_NAME, Component.literal(format(distance))));
		return true;
	}

	/**
	 * Formats a distance in blocks with two decimals, the precision of every distance setting.
	 */
	public static String format(final double blocks) {
		return String.format(Locale.ROOT, "%.2f", blocks);
	}
}
