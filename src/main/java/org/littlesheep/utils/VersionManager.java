package org.littlesheep.utils;

import org.bukkit.configuration.file.YamlConfiguration;
import org.littlesheep.paytofly;
import java.io.InputStream;
import java.io.InputStreamReader;

public class VersionManager {
    private final YamlConfiguration versions;

    public VersionManager(paytofly plugin) {
        InputStream resource = plugin.getResource("versions.yml");
        if (resource == null) {
            plugin.getLogger().severe("无法加载 versions.yml！使用默认版本号");
            this.versions = new YamlConfiguration();
            return;
        }
        this.versions = YamlConfiguration.loadConfiguration(new InputStreamReader(resource));
    }

    public double getConfigVersion() {
        return versions.getDouble("versions.config", 1.0);
    }

    public double getGUIVersion() {
        return versions.getDouble("versions.gui", 1.0);
    }

    public double getLanguageVersion() {
        return versions.getDouble("versions.language", 1.0);
    }
} 