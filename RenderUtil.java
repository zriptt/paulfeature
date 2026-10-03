package com.caleon.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

/** Through-wall filled boxes, outlines and tracers. */
public class RenderUtil {
    private static final BufferAllocator FILL_ALLOC = new BufferAllocator(1 << 16);
    private static final BufferAllocator LINE_ALLOC = new BufferAllocator(1 << 16);
    private static BufferBuilder fill, lines;
    private static Vec3d look = Vec3d.ZERO;
    public static float tickDelta;
    public static Matrix4f proj = new Matrix4f();

    public record Label(String text, float x, float y, int argb) {}
    public static final List<Label> LABELS = new ArrayList<>();

    /** Queues a text label at a world position; drawn by the HUD callback. */
    public static void label(Matrix4f view, Vec3d cam, double x, double y, double z, String text, int argb) {
        if (LABELS.size() > 300) return;
        Vector4f v = new Vector4f((float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z), 1f);
        v.mul(view);
        v.mul(proj);
        if (v.w <= 0.05f) return;
        float nx = v.x / v.w, ny = v.y / v.w;
        if (Math.abs(nx) > 1.3f || Math.abs(ny) > 1.3f) return;
        var w = MinecraftClient.getInstance().getWindow();
        LABELS.add(new Label(text, (nx * 0.5f + 0.5f) * w.getScaledWidth(), (1f - (ny * 0.5f + 0.5f)) * w.getScaledHeight(), argb | 0xFF000000));
    }

    /** @param lookRel unit look vector; tracers start one block in front of the camera. */
    public static void begin(Vec3d lookRel) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        fill = null; lines = null; look = lookRel;
        LABELS.clear();
    }

    public static void end() {
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        if (fill != null) {
            BuiltBuffer b = fill.endNullable();
            if (b != null) BufferRenderer.drawWithGlobalProgram(b);
            fill = null;
        }
        if (lines != null) {
            RenderSystem.lineWidth(1.6f);
            BuiltBuffer b = lines.endNullable();
            if (b != null) BufferRenderer.drawWithGlobalProgram(b);
            RenderSystem.lineWidth(1.0f);
            lines = null;
        }
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /** Filled box + solid outline + optional tracer, all from one RGB colour. */
    public static void esp(Matrix4f m, Vec3d cam, double x1, double y1, double z1, double x2, double y2, double z2, int rgb, boolean tracer) {
        int lum = ((rgb >> 16) & 255) + ((rgb >> 8) & 255) + (rgb & 255);
        box(m, cam, x1, y1, z1, x2, y2, z2, (rgb & 0xFFFFFF) | (lum < 60 ? 0x70000000 : 0x38000000));
        outline(m, cam, x1, y1, z1, x2, y2, z2, rgb | 0xFF000000);
        if (tracer) tracer(m, cam, (x1 + x2) / 2, (y1 + y2) / 2, (z1 + z2) / 2, rgb | 0xFF000000);
    }

    public static void tracer(Matrix4f m, Vec3d cam, double x, double y, double z, int argb) {
        line(m, (float) look.x, (float) look.y, (float) look.z,
                (float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z), argb);
    }

    public static void box(Matrix4f m, Vec3d cam, double x1, double y1, double z1, double x2, double y2, double z2, int argb) {
        if (fill == null) fill = new BufferBuilder(FILL_ALLOC, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        float ax = (float) (x1 - cam.x), ay = (float) (y1 - cam.y), az = (float) (z1 - cam.z);
        float bx = (float) (x2 - cam.x), by = (float) (y2 - cam.y), bz = (float) (z2 - cam.z);
        quad(m, ax, ay, az, bx, ay, az, bx, ay, bz, ax, ay, bz, argb);
        quad(m, ax, by, az, ax, by, bz, bx, by, bz, bx, by, az, argb);
        quad(m, ax, ay, az, ax, by, az, bx, by, az, bx, ay, az, argb);
        quad(m, ax, ay, bz, bx, ay, bz, bx, by, bz, ax, by, bz, argb);
        quad(m, ax, ay, az, ax, ay, bz, ax, by, bz, ax, by, az, argb);
        quad(m, bx, ay, az, bx, by, az, bx, by, bz, bx, ay, bz, argb);
    }

    public static void outline(Matrix4f m, Vec3d cam, double x1, double y1, double z1, double x2, double y2, double z2, int argb) {
        float ax = (float) (x1 - cam.x), ay = (float) (y1 - cam.y), az = (float) (z1 - cam.z);
        float bx = (float) (x2 - cam.x), by = (float) (y2 - cam.y), bz = (float) (z2 - cam.z);
        line(m, ax, ay, az, bx, ay, az, argb); line(m, bx, ay, az, bx, ay, bz, argb);
        line(m, bx, ay, bz, ax, ay, bz, argb); line(m, ax, ay, bz, ax, ay, az, argb);
        line(m, ax, by, az, bx, by, az, argb); line(m, bx, by, az, bx, by, bz, argb);
        line(m, bx, by, bz, ax, by, bz, argb); line(m, ax, by, bz, ax, by, az, argb);
        line(m, ax, ay, az, ax, by, az, argb); line(m, bx, ay, az, bx, by, az, argb);
        line(m, bx, ay, bz, bx, by, bz, argb); line(m, ax, ay, bz, ax, by, bz, argb);
    }

    private static void line(Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, int argb) {
        if (lines == null) lines = new BufferBuilder(LINE_ALLOC, VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        lines.vertex(m, x1, y1, z1).color(argb);
        lines.vertex(m, x2, y2, z2).color(argb);
    }

    private static void quad(Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2,
                             float x3, float y3, float z3, float x4, float y4, float z4, int argb) {
        fill.vertex(m, x1, y1, z1).color(argb);
        fill.vertex(m, x2, y2, z2).color(argb);
        fill.vertex(m, x3, y3, z3).color(argb);
        fill.vertex(m, x4, y4, z4).color(argb);
    }
}
