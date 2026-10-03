package com.caleon.client.gui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Tiny standalone command box. Doesn't use the vanilla chat screen, so other mods that mess with chat can't block it. */
public class CommandScreen extends Screen {
    private TextFieldWidget field;

    public CommandScreen() { super(Text.literal("Command")); }

    @Override protected void init() {
        field = new TextFieldWidget(textRenderer, width / 2 - 150, height - 40, 300, 20, Text.literal("Command"));
        field.setMaxLength(256);
        field.setText("/");
        addDrawableChild(field);
        setInitialFocus(field);
    }

    private void send() {
        String t = field.getText().trim();
        if (t.startsWith("/")) t = t.substring(1);
        if (!t.isEmpty() && client != null && client.getNetworkHandler() != null)
            client.getNetworkHandler().sendChatCommand(t);
        close();
    }

    @Override public boolean keyPressed(int key, int scan, int mods) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { send(); return true; }
        return super.keyPressed(key, scan, mods);
    }

    @Override public boolean shouldPause() { return false; }
}
