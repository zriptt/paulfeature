package com.caleon.client.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();
    public final String name;
    public final Category category;
    public final List<Setting> settings = new ArrayList<>();
    public boolean enabled;
    public boolean expanded;
    /** GLFW key code, -1 = unbound. */
    public int key = -1;
    public boolean keyDown;
    public float anim;

    protected Module(String name, Category category) { this.name = name; this.category = category; }

    protected <T extends Setting> T add(T s) { settings.add(s); return s; }

    public void toggle() {
        enabled = !enabled;
        if (enabled) onEnable(); else onDisable();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
    public void onRender(Matrix4f matrix, Vec3d cam) {}
}
