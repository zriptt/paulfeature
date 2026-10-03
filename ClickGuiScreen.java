package com.caleon.client.gui;

import com.caleon.client.module.Category;
import com.caleon.client.module.Config;
import com.caleon.client.module.Module;
import com.caleon.client.module.ModuleManager;
import com.caleon.client.module.Setting;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClickGuiScreen extends Screen {
    public static final int ACCENT = 0xFFFF2D3D;
    private static final int GAP = 10, ROW = 15, HEAD = 20, TOP = 32, FOOT = 20;
    private static final int LINE = 0xFF3A1219, ROW_BG = 0xF0170B0E, ROW_HOVER = 0xF0281216, SET_BG = 0xF00E0709;
    private static final int TRACK = 0xFF3A2226;
    private static int scroll;

    private enum Kind { MODULE, BIND, SETTING }
    private record Hit(int x, int y, Module m, Setting s, Kind kind) {}

    private int W = 118, startX;
    private Module binding;
    private Setting.Num dragging;
    private int dragX;

    public ClickGuiScreen() { super(Text.literal("Caleon Client")); }

    @Override public boolean shouldPause() { return false; }

    private void layout() {
        int n = Category.values().length;
        W = Math.max(96, Math.min(122, (width - 16 - GAP * (n - 1)) / n));
        startX = Math.max(4, (width - (W * n + GAP * (n - 1))) / 2);
    }

    private int colX(int i) { return startX + i * (W + GAP); }

    private int rowsIn(Category c) {
        int rows = 0;
        for (Module m : ModuleManager.MODULES) {
            if (m.category != c) continue;
            rows++;
            if (m.expanded) rows += 1 + m.settings.size();
        }
        return rows;
    }

    private void clampScroll() {
        int most = 0;
        for (Category c : Category.values()) most = Math.max(most, rowsIn(c));
        int min = Math.min(0, (height - FOOT) - (TOP + HEAD) - most * ROW - 6);
        scroll = Math.max(min, Math.min(0, scroll));
    }

    private List<Hit> hits() {
        List<Hit> out = new ArrayList<>();
        int i = 0;
        for (Category c : Category.values()) {
            int x = colX(i++), y = TOP + HEAD + scroll;
            for (Module m : ModuleManager.MODULES) {
                if (m.category != c) continue;
                out.add(new Hit(x, y, m, null, Kind.MODULE)); y += ROW;
                if (m.expanded) {
                    out.add(new Hit(x, y, m, null, Kind.BIND)); y += ROW;
                    for (Setting s : m.settings) { out.add(new Hit(x, y, m, s, Kind.SETTING)); y += ROW; }
                }
            }
        }
        return out;
    }

    private static String keyName(int key) {
        if (key < 0) return "None";
        String s = InputUtil.fromKeyCode(key, 0).getLocalizedText().getString().toUpperCase(Locale.ROOT);
        return s.length() > 8 ? s.substring(0, 8) : s;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        layout();
        clampScroll();
        int viewTop = TOP + HEAD, viewBottom = height - FOOT;

        // backdrop
        ctx.fillGradient(0, 0, width, height, 0xC0160005, 0xE8040001);
        ctx.fillGradient(0, height - 80, width, height, 0x00FF2D3D, 0x26FF2D3D);

        // title
        var ms = ctx.getMatrices();
        ms.push();
        ms.translate(width / 2f, 8, 0);
        ms.scale(1.6f, 1.6f, 1f);
        int tw = textRenderer.getWidth("CALEON CLIENT");
        ctx.drawTextWithShadow(textRenderer, "CALEON", -tw / 2, 0, ACCENT);
        ctx.drawTextWithShadow(textRenderer, "CLIENT", -tw / 2 + textRenderer.getWidth("CALEON "), 0, 0xFFFFFFFF);
        ms.pop();

        // rows (clipped below the headers and above the footer)
        ctx.enableScissor(0, viewTop, width, viewBottom);
        for (Hit h : hits()) {
            if (h.y + ROW < viewTop || h.y > viewBottom) continue;
            drawRow(ctx, h, mouseX, mouseY, viewTop, viewBottom);
        }
        // bottom cap for each column
        int i = 0;
        for (Category c : Category.values()) {
            int x = colX(i++), y = viewTop + scroll + rowsIn(c) * ROW;
            ctx.fill(x - 1, y, x + W + 1, y + 2, LINE);
        }
        ctx.disableScissor();

        // headers on top
        i = 0;
        for (Category c : Category.values()) {
            int x = colX(i++);
            int on = 0, total = 0;
            for (Module m : ModuleManager.MODULES) if (m.category == c) { total++; if (m.enabled) on++; }
            ctx.fillGradient(x - 1, TOP, x + W + 1, TOP + HEAD, 0xFF5A1220, 0xFF2A0A12);
            ctx.fill(x - 1, TOP, x + W + 1, TOP + 2, ACCENT);
            ctx.drawTextWithShadow(textRenderer, c.label.toUpperCase(Locale.ROOT), x + 7, TOP + 7, 0xFFFFFFFF);
            String cnt = on + "/" + total;
            ctx.drawTextWithShadow(textRenderer, cnt, x + W - 6 - textRenderer.getWidth(cnt), TOP + 7, on > 0 ? ACCENT : 0xFF7A5A5A);
        }

        // footer
        String hint = binding != null
                ? "Press a key to bind " + binding.name + "   (Esc cancel, Backspace clears)"
                : "LMB toggle   RMB settings   MMB bind   Scroll move   RShift close";
        int hw = textRenderer.getWidth(hint);
        ctx.fill(width / 2 - hw / 2 - 8, height - FOOT + 2, width / 2 + hw / 2 + 8, height - 3, 0xC0100507);
        ctx.fill(width / 2 - hw / 2 - 8, height - 4, width / 2 + hw / 2 + 8, height - 3, ACCENT);
        ctx.drawTextWithShadow(textRenderer, hint, width / 2 - hw / 2, height - FOOT + 6, binding != null ? ACCENT : 0xFFA88080);
    }

    private void drawRow(DrawContext ctx, Hit h, int mx, int my, int viewTop, int viewBottom) {
        int x = h.x, y = h.y;
        boolean over = my >= viewTop && my < viewBottom && mx >= x && mx < x + W && my >= y && my < y + ROW;
        ctx.fill(x - 1, y, x, y + ROW, LINE);
        ctx.fill(x + W, y, x + W + 1, y + ROW, LINE);

        if (h.kind == Kind.MODULE) {
            Module m = h.m;
            m.anim += ((m.enabled ? 1f : 0f) - m.anim) * 0.3f;
            ctx.fill(x, y, x + W, y + ROW, over ? ROW_HOVER : ROW_BG);
            if (m.anim > 0.02f) {
                ctx.fill(x, y, x + (int) (W * m.anim), y + ROW, ((int) (0x55 * m.anim) << 24) | 0xFF2D3D);
                ctx.fill(x, y, x + 2, y + ROW, ACCENT);
            }
            ctx.drawTextWithShadow(textRenderer, m.name, x + 7, y + 4, m.enabled ? 0xFFFFFFFF : 0xFFB09999);
            String sign = m.expanded ? "-" : "+";
            ctx.drawTextWithShadow(textRenderer, sign, x + W - 9, y + 4, m.expanded ? ACCENT : 0xFF806060);
            if (m.key >= 0) {
                String k = "[" + keyName(m.key) + "]";
                ctx.drawTextWithShadow(textRenderer, k, x + W - 14 - textRenderer.getWidth(k), y + 4, 0xFF9A7070);
            }
        } else if (h.kind == Kind.BIND) {
            ctx.fill(x, y, x + W, y + ROW, over ? ROW_HOVER : SET_BG);
            ctx.fill(x + 3, y, x + 4, y + ROW, 0x66FF2D3D);
            ctx.drawTextWithShadow(textRenderer, "Keybind", x + 9, y + 4, 0xFFBBA0A0);
            boolean listening = binding == h.m;
            String v = listening ? ((System.currentTimeMillis() / 400) % 2 == 0 ? "> <" : ">  <") : keyName(h.m.key);
            ctx.drawTextWithShadow(textRenderer, v, x + W - 6 - textRenderer.getWidth(v), y + 4, listening ? ACCENT : (h.m.key >= 0 ? 0xFFFFFFFF : 0xFF775555));
        } else {
            Setting s = h.s;
            ctx.fill(x, y, x + W, y + ROW, over ? ROW_HOVER : SET_BG);
            ctx.fill(x + 3, y, x + 4, y + ROW, 0x66FF2D3D);
            int nameX = x + 9;
            if (s.color >= 0) {
                ctx.fill(nameX, y + 3, nameX + 5, y + ROW - 3, 0xFF7A6666);
                ctx.fill(nameX + 1, y + 4, nameX + 4, y + ROW - 4, 0xFF000000 | s.color);
                nameX += 9;
            }
            if (s instanceof Setting.Bool b) {
                ctx.drawTextWithShadow(textRenderer, b.name, nameX, y + 4, b.value ? 0xFFFFFFFF : 0xFF9A8080);
                int px = x + W - 22, py = y + 4;
                ctx.fill(px, py, px + 16, py + 7, b.value ? ACCENT : TRACK);
                int kx = b.value ? px + 10 : px + 1;
                ctx.fill(kx, py + 1, kx + 5, py + 6, 0xFFFFFFFF);
            } else if (s instanceof Setting.Num n) {
                ctx.drawTextWithShadow(textRenderer, n.name, nameX, y + 2, 0xFFBBA0A0);
                String v = String.format(Locale.ROOT, "%.1f", n.value);
                ctx.drawTextWithShadow(textRenderer, v, x + W - 6 - textRenderer.getWidth(v), y + 2, 0xFFFFFFFF);
                int tx = x + 9, tw = W - 18, ty = y + ROW - 4;
                double t = (n.value - n.min) / (n.max - n.min);
                ctx.fill(tx, ty, tx + tw, ty + 2, TRACK);
                ctx.fill(tx, ty, tx + (int) (tw * t), ty + 2, ACCENT);
                ctx.fill(tx + (int) (tw * t) - 1, ty - 1, tx + (int) (tw * t) + 1, ty + 3, 0xFFFFFFFF);
            }
        }
    }

    private void setNum(Setting.Num n, double mx, int x) {
        double t = Math.max(0, Math.min(1, (mx - (x + 9)) / (double) (W - 18)));
        n.value = Math.round((n.min + t * (n.max - n.min)) * 10.0) / 10.0;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (binding != null) { binding = null; return true; }
        if (my < TOP + HEAD || my >= height - FOOT) return true;
        for (Hit h : hits()) {
            if (mx < h.x || mx >= h.x + W || my < h.y || my >= h.y + ROW) continue;
            switch (h.kind) {
                case MODULE -> {
                    if (button == 0) h.m.toggle();
                    else if (button == 1) h.m.expanded = !h.m.expanded;
                    else if (button == 2) binding = h.m;
                }
                case BIND -> {
                    if (button == 0) binding = h.m;
                    else if (button == 1) { h.m.key = -1; Config.save(); }
                }
                case SETTING -> {
                    if (h.s instanceof Setting.Bool b) { if (button == 0) b.value = !b.value; }
                    else if (h.s instanceof Setting.Num n && button == 0) { dragging = n; dragX = h.x; setNum(n, mx, h.x); }
                }
            }
            return true;
        }
        return true;
    }

    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging != null && button == 0) setNum(dragging, mx, dragX);
        return true;
    }

    @Override public boolean mouseReleased(double mx, double my, int button) {
        dragging = null;
        return true;
    }

    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        scroll += (int) (vertical * 24);
        clampScroll();
        return true;
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { /* cancel */ }
            else if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) binding.key = -1;
            else binding.key = keyCode;
            binding = null;
            Config.save();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override public void removed() { Config.save(); }
}
