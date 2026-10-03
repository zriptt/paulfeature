package com.caleon.client.mixin;

import com.caleon.client.module.Freecam;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void caleon$tick(CallbackInfo ci) {
        if (!Freecam.active) return;
        Input in = (Input) (Object) this;
        in.playerInput = PlayerInput.DEFAULT;
        in.movementForward = 0;
        in.movementSideways = 0;
    }
}
