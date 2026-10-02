package com.urntt.airplace.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.urntt.airplace.AirPlacement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	/**
	 * Uses the mouse wheel notches vanilla has already counted for switching hotbar slots to change the placement
	 * distance instead, while the Air Place mode is on. The hotbar selection then stays unchanged. Scrolling in
	 * screens and in spectator mode is not affected.
	 */
	@WrapOperation(
			method = "onScroll",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ScrollWheelHandler;getNextScrollWheelSelection(DII)I"))
	private int airplace$scrollDistance(final double wheel, final int currentSelected, final int limit,
			final Operation<Integer> original) {
		if (AirPlacement.scroll(this.minecraft, (int) wheel)) {
			return currentSelected;
		}
		return original.call(wheel, currentSelected, limit);
	}
}
