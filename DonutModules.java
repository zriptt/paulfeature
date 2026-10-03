package com.caleon.client.module;

import com.caleon.client.render.RenderUtil;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.block.entity.TrialSpawnerBlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** DonutSMP-flavoured helpers: spawners, RTP hunting loop, AFK, player alerts. */
public class DonutModules {
    public static SpawnerFinder spawnerFinder;

    public static class SpawnerFinder extends Module {
        public int count;
        final Setting.Num range = add(new Setting.Num("Range", 256, 32, 512));
        final Setting.Num maxY = add(new Setting.Num("Max Y", 320, -64, 320));
        final Setting.Bool labels = add(new Setting.Bool("Labels", true));
        final Setting.Bool byMob = add(new Setting.Bool("Color by mob", true));
        final Setting.Bool beam = add(new Setting.Bool("Beam", true));
        final Setting.Bool tracers = add(new Setting.Bool("Tracers", true));
        final Setting.Bool nearestOnly = add(new Setting.Bool("Nearest tracer", false));
        final Setting.Bool chat = add(new Setting.Bool("Chat alert", true));
        final Setting.Bool trial = add(new Setting.Bool("Trial spawners", false));

        private static final int PINK = 0xFF33DD;
        private static final Map<String, Integer> MOB_COLORS = new HashMap<>();
        static {
            MOB_COLORS.put("zombie", 0x55DD55); MOB_COLORS.put("skeleton", 0xE0E0E0);
            MOB_COLORS.put("spider", 0xCC4444); MOB_COLORS.put("cave_spider", 0x33CCCC);
            MOB_COLORS.put("blaze", 0xFFAA00); MOB_COLORS.put("magma_cube", 0xFF5500);
            MOB_COLORS.put("silverfish", 0x9999AA); MOB_COLORS.put("creeper", 0x33FF33);
            MOB_COLORS.put("enderman", 0xAA44FF); MOB_COLORS.put("witch", 0x9933CC);
        }

        private final Set<Long> alerted = new HashSet<>();
        private final Map<Long, String> types = new HashMap<>();   // raw id, "" = not known yet
        private ClientWorld lastWorld;
        private int t;

        public SpawnerFinder() { super("Spawner Finder", Category.DONUT); spawnerFinder = this; }

        private boolean spawner(BlockEntity be) {
            return be instanceof MobSpawnerBlockEntity || (trial.value && be instanceof TrialSpawnerBlockEntity);
        }

        /** Reads the spawner's mob type from the block entity data the server sent us. */
        private String readType(BlockEntity be) {
            if (!(be instanceof MobSpawnerBlockEntity)) return "trial";
            try {
                NbtCompound n = be.createNbt(mc.world.getRegistryManager());
                String id = n.getCompound("SpawnData").getCompound("entity").getString("id");
                return id.startsWith("minecraft:") ? id.substring(10) : id;
            } catch (Throwable e) { return ""; }
        }

        private static String pretty(String id) {
            if (id.isEmpty()) return "";
            StringBuilder sb = new StringBuilder();
            for (String w : id.split("_")) {
                if (w.isEmpty()) continue;
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(' ');
            }
            return sb.toString().trim();
        }

        private String type(BlockEntity be) {
            long k = be.getPos().asLong();
            String id = types.get(k);
            if (id == null || (id.isEmpty() && t % 40 == 0)) { id = readType(be); types.put(k, id); }
            return id;
        }

        @Override public void onTick() {
            t++;
            if (mc.world != lastWorld) { lastWorld = mc.world; alerted.clear(); types.clear(); }
            int n = 0;
            for (BlockEntity be : BaseScanner.bes()) {
                if (!spawner(be) || be.getPos().getY() > maxY.value) continue;
                n++;
                BlockPos p = be.getPos();
                if (alerted.add(p.asLong())) {
                    BaseScanner.finds++;
                    if (chat.value) {
                        String name = pretty(type(be));
                        mc.player.sendMessage(Text.literal("§c[Caleon] §f" + (name.isEmpty() ? "" : name + " ") + "Spawner at X "
                                + p.getX() + " Y " + p.getY() + " Z " + p.getZ()), false);
                    }
                }
            }
            count = n;
        }

        @Override public void onDisable() { count = 0; }

        @Override public void onRender(Matrix4f m, Vec3d cam) {
            BlockPos nearest = null; double nd = Double.MAX_VALUE;
            if (nearestOnly.value)
                for (BlockEntity be : BaseScanner.bes()) {
                    if (!spawner(be)) continue;
                    BlockPos p = be.getPos();
                    double d = p.getSquaredDistance(cam.x, cam.y, cam.z);
                    if (p.getY() <= maxY.value && d < nd) { nd = d; nearest = p; }
                }
            for (BlockEntity be : BaseScanner.bes()) {
                if (!spawner(be)) continue;
                BlockPos p = be.getPos();
                if (p.getY() > maxY.value || !BaseModules.near(p, cam, range.value)) continue;
                String id = type(be);
                int rgb = be instanceof TrialSpawnerBlockEntity ? 0x33FFDD : (byMob.value ? MOB_COLORS.getOrDefault(id, PINK) : PINK);
                boolean tr = tracers.value && (!nearestOnly.value || p.equals(nearest));
                BaseModules.cube(m, cam, p, rgb, tr);
                if (beam.value)
                    RenderUtil.box(m, cam, p.getX() + 0.45, p.getY() + 1, p.getZ() + 0.45, p.getX() + 0.55, p.getY() + 80, p.getZ() + 0.55, 0x60000000 | rgb);
                if (labels.value) {
                    String name = pretty(id);
                    double d = Math.sqrt(p.getSquaredDistance(cam.x, cam.y, cam.z));
                    String text = (be instanceof TrialSpawnerBlockEntity ? "Trial " : (name.isEmpty() ? "" : name + " ")) + "Spawner  " + (int) d + "m";
                    RenderUtil.label(m, cam, p.getX() + 0.5, p.getY() + 1.4, p.getZ() + 0.5, text, rgb);
                }
            }
        }
    }

    /** Sends /rtp on a timer; by default stops itself as soon as any finder reports something. */
    public static class AutoRtp extends Module {
        final Setting.Num delay = add(new Setting.Num("Delay (s)", 30, 5, 300));
        final Setting.Bool stopOnFind = add(new Setting.Bool("Stop on find", true));
        private int timer, lastFinds;
        public AutoRtp() { super("Auto RTP", Category.DONUT); }
        @Override public void onEnable() { timer = 0; lastFinds = BaseScanner.finds; }
        @Override public void onTick() {
            if (stopOnFind.value && BaseScanner.finds != lastFinds) {
                mc.player.sendMessage(Text.literal("§c[Caleon] §fAuto RTP stopped: something was found."), false);
                toggle();
                return;
            }
            if (++timer >= delay.value * 20) {
                timer = 0;
                if (mc.getNetworkHandler() != null) mc.getNetworkHandler().sendChatCommand("rtp");
            }
        }
    }

    public static class AntiAfk extends Module {
        final Setting.Num interval = add(new Setting.Num("Interval (s)", 20, 5, 120));
        private int timer; private boolean flip;
        public AntiAfk() { super("Anti AFK", Category.DONUT); }
        @Override public void onTick() {
            if (Freecam.active) return;
            if (++timer < interval.value * 20) return;
            timer = 0; flip = !flip;
            if (mc.player.isOnGround()) mc.player.jump();
            mc.player.setYaw(mc.player.getYaw() + (flip ? 20 : -20));
            mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
        }
    }

    public static class PlayerAlert extends Module {
        private final Set<UUID> seen = new HashSet<>();
        private int t;
        public PlayerAlert() { super("Player Alert", Category.DONUT); }
        @Override public void onEnable() { seen.clear(); }
        @Override public void onTick() {
            if (++t % 10 != 0) return;
            Set<UUID> now = new HashSet<>();
            for (PlayerEntity p : mc.world.getPlayers()) {
                if (p == mc.player) continue;
                now.add(p.getUuid());
                if (seen.add(p.getUuid()))
                    mc.player.sendMessage(Text.literal("§c[Caleon] §f" + p.getName().getString() + " is nearby (" + (int) mc.player.distanceTo(p)
                            + "m) at X " + p.getBlockX() + " Z " + p.getBlockZ()), false);
            }
            seen.retainAll(now);
        }
    }
}
