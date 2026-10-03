package com.caleon.client.module;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Detaches the camera from the player. Movement/look are handled by the camera + input mixins. */
public class Freecam extends Module {
    public static boolean active;
    public static double x, y, z;
    public static float yaw, pitch;
    public static Freecam instance;
    private static long last;

    final Setting.Num speed = add(new Setting.Num("Speed", 1.0, 0.1, 5.0));

    public Freecam() { super("Freecam", Category.RENDER); instance = this; }

    @Override public void onEnable() {
        if (mc.player == null) { enabled = false; return; }
        Vec3d p = mc.gameRenderer.getCamera().getPos();
        x = p.x; y = p.y; z = p.z;
        yaw = mc.player.getYaw(); pitch = mc.player.getPitch();
        last = System.nanoTime();
        active = true;
    }

    @Override public void onDisable() { active = false; }

    public static void look(double dx, double dy) {
        yaw += (float) dx * 0.15f;
        pitch = MathHelper.clamp(pitch + (float) dy * 0.15f, -90f, 90f);
    }

    private static double key(KeyBinding kb) {
        try {
            long h = mc.getWindow().getHandle();
            return InputUtil.isKeyPressed(h, KeyBindingHelper.getBoundKeyOf(kb).getCode()) ? 1 : 0;
        } catch (Throwable t) { return 0; }
    }

    /** Called once per frame from the camera mixin. */
    public static void step() {
        long now = System.nanoTime();
        double dt = Math.min((now - last) / 1e9, 0.1);
        last = now;
        if (mc.currentScreen != null || instance == null) return;
        double f = key(mc.options.forwardKey) - key(mc.options.backKey);
        double s = key(mc.options.rightKey) - key(mc.options.leftKey);
        double u = key(mc.options.jumpKey) - key(mc.options.sneakKey);
        double sp = instance.speed.value * 10.0 * dt * (key(mc.options.sprintKey) > 0 ? 3 : 1);
        double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
        double fx = -Math.sin(yr) * Math.cos(pr), fy = -Math.sin(pr), fz = Math.cos(yr) * Math.cos(pr);
        double rx = -Math.cos(yr), rz = -Math.sin(yr);
        x += (fx * f + rx * s) * sp;
        y += (fy * f + u) * sp;
        z += (fz * f + rz * s) * sp;
    }
}
