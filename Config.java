package com.caleon.client.module;

import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Saves keybinds and setting values to config/caleonclient.properties. */
public class Config {
    private static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("caleonclient.properties"); }

    public static void load() {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(path())) { p.load(in); } catch (Exception e) { return; }
        for (Module m : ModuleManager.MODULES) {
            try {
                String k = p.getProperty(m.name + ".key");
                if (k != null) m.key = Integer.parseInt(k);
                for (Setting s : m.settings) {
                    String v = p.getProperty(m.name + "." + s.name);
                    if (v == null) continue;
                    if (s instanceof Setting.Bool b) b.value = Boolean.parseBoolean(v);
                    else if (s instanceof Setting.Num n) n.value = Double.parseDouble(v);
                }
            } catch (Exception ignored) {}
        }
    }

    public static void save() {
        Properties p = new Properties();
        for (Module m : ModuleManager.MODULES) {
            p.setProperty(m.name + ".key", String.valueOf(m.key));
            for (Setting s : m.settings) {
                if (s instanceof Setting.Bool b) p.setProperty(m.name + "." + s.name, String.valueOf(b.value));
                else if (s instanceof Setting.Num n) p.setProperty(m.name + "." + s.name, String.valueOf(n.value));
            }
        }
        try (OutputStream out = Files.newOutputStream(path())) { p.store(out, "Caleon Client"); } catch (Exception ignored) {}
    }
}
