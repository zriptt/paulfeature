package com.caleon.client.mixin;

import com.caleon.client.module.Freecam;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "changeLookDirection", at = @At("HEAD"), cancellable = true)
    private void caleon$look(double dx, double dy, CallbackInfo ci) {
        if (Freecam.active && (Object) this == MinecraftClient.getInstance().player) {
            Freecam.look(dx, dy);
            ci.cancel();
        }
    }
}
