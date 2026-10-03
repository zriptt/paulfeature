package com.caleon.client.mixin;

import com.caleon.client.module.Xray;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Xray hook: decides per block face whether it gets drawn (1.21.4 signature).
 */
@Mixin(Block.class)
public class BlockMixin {
    @Inject(method = "shouldDrawSide(Lnet/minecraft/block/BlockState;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/Direction;)Z",
            at = @At("HEAD"), cancellable = true, require = 0)
    private static void caleon$sideA(BlockState state, BlockState other, Direction side, CallbackInfoReturnable<Boolean> cir) {
        if (Xray.active) cir.setReturnValue(Xray.isVisible(state));
    }
}
