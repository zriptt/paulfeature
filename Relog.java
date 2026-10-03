package com.caleon.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

/** Button-style module: bind a key, press it, you get kicked and instantly reconnect. No cooldown of its own. */
public class Relog extends Module {
    private static ServerInfo target;
    private static int state, wait;

    public Relog() { super("Relog", Category.MISC); }

    @Override public void onEnable() { enabled = false; trigger(); }

    public static void trigger() {
        MinecraftClient mc = MinecraftClient.getInstance();
        ServerInfo si = mc.getCurrentServerEntry();
        if (si == null || mc.getNetworkHandler() == null) {
            if (mc.player != null) mc.player.sendMessage(Text.literal("§c[Caleon] §fRelog only works on a multiplayer server."), false);
            return;
        }
        target = si;
        state = 1;
        wait = 0;
        mc.getNetworkHandler().getConnection().disconnect(Text.literal("Relog"));
    }

    /** Runs every client tick, also while no world is loaded. */
    public static void tickGlobal() {
        if (state != 1) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        wait++;
        if (mc.world == null || wait > 60) {
            state = 0;
            ConnectScreen.connect(new MultiplayerScreen(new TitleScreen()), mc, ServerAddress.parse(target.address), target, false, null);
        }
    }
}
