package com.urntt.airplace.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.urntt.airplace.AirPlacement;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	/**
	 * Looks for the air target once per item use, with the partial tick vanilla's tick uses for its crosshair target.
	 */
	@Inject(method = "startUseItem", at = @At("HEAD"))
	private void airplace$findAirTarget(final CallbackInfo ci, @Share("airTarget") final LocalRef<BlockHitResult> airTarget) {
		airTarget.set(AirPlacement.findTarget((Minecraft) (Object) this, 1.0F));
	}

	/**
	 * Lets vanilla's item use see the air target instead of the crosshair target, so it uses the held item on that
	 * block exactly as if the player had clicked it. Without an air target, item use stays vanilla.
	 */
	@ModifyExpressionValue(
			method = "startUseItem",
			at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;hitResult:Lnet/minecraft/world/phys/HitResult;",
					opcode = Opcodes.GETFIELD))
	private HitResult airplace$useAirTarget(final HitResult hitResult,
			@Share("airTarget") final LocalRef<BlockHitResult> airTarget) {
		BlockHitResult target = airTarget.get();
		return target != null ? target : hitResult;
	}
}
