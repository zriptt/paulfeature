package com.caleon.client.mixin;

import com.caleon.client.module.FakeElytra;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Makes the local player's worn iron chestplate render as an elytra (client-side only). */
@Mixin(PlayerEntityRenderer.class)
public class PlayerRendererMixin {
    private static ItemStack caleon$elytra;

    // require = 0: if this target ever stops matching, the feature just does nothing instead of crashing the game.
    @Inject(method = "updateRenderState(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;F)V",
            at = @At("TAIL"), require = 0)
    private void caleon$fakeElytra(AbstractClientPlayerEntity entity, PlayerEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (entity != MinecraftClient.getInstance().player || !FakeElytra.showing()) return;
        if (caleon$elytra == null) caleon$elytra = new ItemStack(Items.ELYTRA);
        state.equippedChestStack = caleon$elytra;
    }
}
