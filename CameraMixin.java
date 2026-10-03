package com.caleon.client.mixin;

import com.caleon.client.module.Freecam;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPos(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void caleon$update(BlockView area, Entity focused, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        if (!Freecam.active) return;
        Freecam.step();
        setPos(Freecam.x, Freecam.y, Freecam.z);
        setRotation(Freecam.yaw, Freecam.pitch);
    }
}
