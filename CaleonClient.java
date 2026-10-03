package com.caleon.client;

import com.caleon.client.gui.ClickGuiScreen;
import com.caleon.client.gui.CommandScreen;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import com.caleon.client.module.Config;
import com.caleon.client.module.DonutModules;
import com.caleon.client.module.Module;
import com.caleon.client.module.ModuleManager;
import com.caleon.client.module.Relog;
import com.caleon.client.render.RenderUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.Camera;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CaleonClient implements ClientModInitializer {
    private static final int RED = ClickGuiScreen.ACCENT;
    private static KeyBinding openGui, openCmd;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        Config.load();
        openGui = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.caleonclient.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.caleonclient"));

        openCmd = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.caleonclient.cmd", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_BACKSLASH, "category.caleonclient"));

        // Typing ".home" in normal chat sends "/home" (works even if another mod eats the slash).
        ClientSendMessageEvents.ALLOW_CHAT.register(msg -> {
            var mc = MinecraftClient.getInstance();
            if (msg.length() > 1 && msg.charAt(0) == '.' && mc.getNetworkHandler() != null) {
                mc.getNetworkHandler().sendChatCommand(msg.substring(1));
                return false;
            }
            return true;
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openGui.wasPressed()) if (mc.currentScreen == null) mc.setScreen(new ClickGuiScreen());
            while (openCmd.wasPressed()) if (mc.currentScreen == null && mc.player != null) mc.setScreen(new CommandScreen());
            ModuleManager.pollKeys();
            ModuleManager.tick();
            Relog.tickGlobal();
        });

        WorldRenderEvents.LAST.register(ctx -> {
            var ms = ctx.matrixStack();
            if (ms == null) return;
            RenderUtil.proj = new Matrix4f(ctx.projectionMatrix());
            Camera cam = ctx.camera();
            double yaw = Math.toRadians(cam.getYaw()), pitch = Math.toRadians(cam.getPitch());
            Vec3d look = new Vec3d(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
            ModuleManager.render(ms.peek().getPositionMatrix(), cam.getPos(), look);
        });

        HudRenderCallback.EVENT.register((ctx, tick) -> {
            var hud = ModuleManager.hud;
            MinecraftClient mc = MinecraftClient.getInstance();
            var tr = mc.textRenderer;
            if (!mc.options.hudHidden) {
                for (RenderUtil.Label l : RenderUtil.LABELS) {
                    int lw = tr.getWidth(l.text()), lx = (int) l.x() - lw / 2, ly = (int) l.y() - 4;
                    ctx.fill(lx - 3, ly - 2, lx + lw + 3, ly + 10, 0xA0000000);
                    ctx.fill(lx - 3, ly + 10, lx + lw + 3, ly + 11, l.argb());
                    ctx.drawTextWithShadow(tr, l.text(), lx, ly, l.argb());
                }
            }
            RenderUtil.LABELS.clear();
            if (hud == null || !hud.enabled || mc.options.hudHidden) return;
            int y = 4;
            if (hud.watermark.value) {
                ctx.fill(2, 2, 4, 13, RED);
                ctx.drawTextWithShadow(tr, "Caleon Client", 7, 4, RED);
                y = 17;
            }
            if (hud.info.value && mc.player != null) {
                ctx.drawTextWithShadow(tr, "XYZ " + mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ(), 4, y, 0xFFFFC0C0);
                ctx.drawTextWithShadow(tr, mc.getCurrentFps() + " FPS", 4, y + 10, 0xFFFFC0C0);
                y += 20;
                var sf = DonutModules.spawnerFinder;
                if (sf != null && sf.enabled) ctx.drawTextWithShadow(tr, "Spawners loaded: " + sf.count, 4, y, 0xFFFF77DD);
            }
            if (hud.arrayList.value) {
                List<Module> on = new ArrayList<>();
                for (Module m : ModuleManager.MODULES) if (m.enabled && m != hud) on.add(m);
                on.sort(Comparator.comparingInt((Module m) -> -tr.getWidth(m.name)));
                int ry = 4, w = ctx.getScaledWindowWidth();
                for (Module m : on) {
                    int tw = tr.getWidth(m.name);
                    ctx.fill(w - 2, ry - 1, w, ry + 9, RED);
                    ctx.drawTextWithShadow(tr, m.name, w - tw - 5, ry, 0xFFFF7B86);
                    ry += 10;
                }
            }
        });
    }
}
