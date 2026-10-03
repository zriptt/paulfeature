package com.caleon.client.module;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.BlockTags;

import java.util.HashMap;
import java.util.Map;

/** Only the ticked block groups are rendered; everything else is culled. Chunks reload when settings change. */
public class Xray extends Module {
    public static boolean active;
    private static final Map<Block, Setting.Bool> MAP = new HashMap<>();

    final Setting.Bool diamond = add(new Setting.Bool("Diamond", true, 0x33DDFF));
    final Setting.Bool debris = add(new Setting.Bool("Ancient debris", true, 0xAA5522));
    final Setting.Bool emerald = add(new Setting.Bool("Emerald", true, 0x33EE55));
    final Setting.Bool gold = add(new Setting.Bool("Gold", true, 0xFFCC22));
    final Setting.Bool iron = add(new Setting.Bool("Iron", true, 0xDDBBAA));
    final Setting.Bool redstone = add(new Setting.Bool("Redstone", true, 0xFF2222));
    final Setting.Bool lapis = add(new Setting.Bool("Lapis", true, 0x3355FF));
    final Setting.Bool copper = add(new Setting.Bool("Copper", false, 0xE07A4A));
    final Setting.Bool coal = add(new Setting.Bool("Coal", false, 0x444444));
    final Setting.Bool quartz = add(new Setting.Bool("Quartz", false, 0xEEEEEE));
    final Setting.Bool spawners = add(new Setting.Bool("Spawners", true, 0xFF33DD));
    final Setting.Bool storage = add(new Setting.Bool("Storage", true, 0xFF8800));
    final Setting.Bool base = add(new Setting.Bool("Base blocks", false, 0x4488FF));

    private int lastSig = -1;

    public Xray() {
        super("Xray", Category.RENDER);
        put(diamond, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        put(debris, Blocks.ANCIENT_DEBRIS);
        put(emerald, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
        put(gold, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE);
        put(iron, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
        put(redstone, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
        put(lapis, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
        put(copper, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
        put(coal, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
        put(quartz, Blocks.NETHER_QUARTZ_ORE);
        put(spawners, Blocks.SPAWNER, Blocks.TRIAL_SPAWNER, Blocks.VAULT);
        put(storage, Blocks.CHEST, Blocks.TRAPPED_CHEST, Blocks.ENDER_CHEST, Blocks.BARREL, Blocks.HOPPER,
                Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER, Blocks.DISPENSER, Blocks.DROPPER);
        put(base, Blocks.CRAFTING_TABLE, Blocks.ENCHANTING_TABLE, Blocks.BEACON, Blocks.TNT, Blocks.ANVIL,
                Blocks.END_PORTAL_FRAME, Blocks.BREWING_STAND, Blocks.LECTERN, Blocks.OBSERVER);
    }

    private static void put(Setting.Bool s, Block... blocks) { for (Block b : blocks) MAP.put(b, s); }

    /** Called from the shouldDrawSide mixin for every block face. */
    public static boolean isVisible(BlockState st) {
        Setting.Bool s = MAP.get(st.getBlock());
        if (s != null) return s.value;
        if (st.isIn(BlockTags.SHULKER_BOXES) || st.isIn(BlockTags.BEDS)) return instance().storage.value;
        return false;
    }

    private static Xray instance() {
        for (Module m : ModuleManager.MODULES) if (m instanceof Xray x) return x;
        return null;
    }

    private int signature() {
        int h = 0;
        for (Setting s : settings) h = h * 31 + (((Setting.Bool) s).value ? 1 : 0);
        return h;
    }

    private static void reload() {
        if (mc.world != null && mc.worldRenderer != null) mc.worldRenderer.reload();
    }

    @Override public void onEnable() { active = true; lastSig = signature(); reload(); }

    @Override public void onDisable() {
        active = false;
        reload();
        boolean fullbright = false;
        for (Module m : ModuleManager.MODULES) if (m.enabled && m.name.equals("Fullbright")) fullbright = true;
        if (!fullbright && mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
    }

    @Override public void onTick() {
        // lit up like Fullbright so ores are visible in dark caves
        StatusEffectInstance cur = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
        if (cur == null || cur.getDuration() < 220)
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1000, 0, false, false, false));
        int sig = signature();
        if (sig != lastSig) { lastSig = sig; reload(); }
    }
}
