package com.caleon.client.module;

import com.caleon.client.render.RenderUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** ESP modules that draw their own boxes (no glow flag, so they can't be overwritten by the server). */
public class EntityModules {
    public static boolean inRange(Entity e, Vec3d cam, double r) {
        double dx = e.getX() - cam.x, dy = e.getY() - cam.y, dz = e.getZ() - cam.z;
        return dx * dx + dy * dy + dz * dz <= r * r;
    }

    public static void draw(Matrix4f m, Vec3d cam, Entity e, int rgb, boolean tracer) {
        Vec3d lp = e.getLerpedPos(RenderUtil.tickDelta);
        Box b = e.getBoundingBox().offset(lp.x - e.getX(), lp.y - e.getY(), lp.z - e.getZ());
        RenderUtil.esp(m, cam, b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ, rgb, tracer);
    }

    public static class PlayerEsp extends Module {
        final Setting.Num range = add(new Setting.Num("Range", 256, 32, 512));
        final Setting.Bool tracers = add(new Setting.Bool("Tracers", true));
        public PlayerEsp() { super("Player ESP", Category.RENDER); }
        @Override public void onRender(Matrix4f m, Vec3d cam) {
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player && !Freecam.active) continue;
                if (!p.isAlive() || !inRange(p, cam, range.value)) continue;
                draw(m, cam, p, p == mc.player ? 0xFFFFFF : 0xFF2D3D, tracers.value && p != mc.player);
            }
        }
    }

    public static class MobEsp extends Module {
        final Setting.Bool hostile = add(new Setting.Bool("Hostile", true));
        final Setting.Bool passive = add(new Setting.Bool("Passive", false));
        final Setting.Bool tracers = add(new Setting.Bool("Tracers", false));
        final Setting.Num range = add(new Setting.Num("Range", 96, 16, 256));
        public MobEsp() { super("Mob ESP", Category.RENDER); }
        @Override public void onRender(Matrix4f m, Vec3d cam) {
            for (Entity e : mc.world.getEntities()) {
                if (!(e instanceof LivingEntity le) || e instanceof PlayerEntity || !le.isAlive()) continue;
                if (!inRange(e, cam, range.value)) continue;
                if (hostile.value && e instanceof Monster) draw(m, cam, e, 0xFF5522, tracers.value);
                else if (passive.value && e instanceof PassiveEntity) draw(m, cam, e, 0x55FF66, tracers.value);
            }
        }
    }

    public static class ItemEsp extends Module {
        final Setting.Num range = add(new Setting.Num("Range", 64, 16, 128));
        final Setting.Bool tracers = add(new Setting.Bool("Tracers", false));
        public ItemEsp() { super("Item ESP", Category.RENDER); }
        @Override public void onRender(Matrix4f m, Vec3d cam) {
            for (Entity e : mc.world.getEntities())
                if (e instanceof ItemEntity && inRange(e, cam, range.value)) draw(m, cam, e, 0xFFDD33, tracers.value);
        }
    }
}
