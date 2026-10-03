package com.caleon.client.module;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;

/**
 * While you wear an iron chestplate, your player model is drawn with an elytra (PlayerRendererMixin)
 * and you can fly with your real character. Double-tap jump in the air to start/stop flying.
 *
 * Controls while flying: W/S = move along where you look, A/D = strafe, Space = up, Shift = down.
 * No input = hover. Taking the chestplate off stops flying.
 *
 * NOTE: this moves your real player, so servers with anti-cheat (or allow-flight=false) can rubber-band,
 * kick or ban you for it. You also take normal fall damage when you stop flying in mid-air.
 */
public class FakeElytra extends Module {
    public static FakeElytra instance;

    private boolean jumpDown, flying;
    private long lastPress;

    final Setting.Num speed = add(new Setting.Num("Speed", 0.8, 0.1, 3.0));
    final Setting.Bool doubleTap = add(new Setting.Bool("Double-tap jump", true));
    final Setting.Bool needAir = add(new Setting.Bool("Only in air", true));

    public FakeElytra() { super("Fake Elytra", Category.RENDER); instance = this; }

    public static boolean wearingIron() {
        return mc.player != null && mc.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.IRON_CHESTPLATE);
    }

    /** Used by the render mixin. */
    public static boolean showing() { return instance != null && instance.enabled && wearingIron(); }

    private static boolean down(net.minecraft.client.option.KeyBinding kb) {
        try {
            return InputUtil.isKeyPressed(mc.getWindow().getHandle(), KeyBindingHelper.getBoundKeyOf(kb).getCode());
        } catch (Throwable t) { return false; }
    }

    @Override public void onTick() {
        if (mc.player == null) return;
        if (!wearingIron()) { flying = false; jumpDown = false; return; }
        if (mc.currentScreen != null) { jumpDown = false; return; }

        boolean jump = down(mc.options.jumpKey);
        if (doubleTap.value && jump && !jumpDown) {
            long now = System.currentTimeMillis();
            if (now - lastPress < 300) {
                if (flying || !needAir.value || !mc.player.isOnGround()) { flying = !flying; lastPress = 0; }
            } else {
                lastPress = now;
            }
        }
        jumpDown = jump;

        if (!flying) return;

        double f = (down(mc.options.forwardKey) ? 1 : 0) - (down(mc.options.backKey) ? 1 : 0);
        double s = (down(mc.options.rightKey) ? 1 : 0) - (down(mc.options.leftKey) ? 1 : 0);
        double u = (jump ? 1 : 0) - (down(mc.options.sneakKey) ? 1 : 0);

        double yr = Math.toRadians(mc.player.getYaw()), pr = Math.toRadians(mc.player.getPitch());
        Vec3d look = new Vec3d(-Math.sin(yr) * Math.cos(pr), -Math.sin(pr), Math.cos(yr) * Math.cos(pr));
        Vec3d right = new Vec3d(-Math.cos(yr), 0, -Math.sin(yr));

        Vec3d v = look.multiply(f).add(right.multiply(s)).add(0, u, 0);
        if (v.lengthSquared() > 1e-6) v = v.normalize().multiply(speed.value);
        else v = Vec3d.ZERO;
        mc.player.setVelocity(v);
    }

    @Override public void onDisable() { flying = false; jumpDown = false; }
}
