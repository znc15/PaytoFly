package org.littlesheep.utils;

import org.bukkit.scheduler.BukkitRunnable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.littlesheep.paytofly;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class UpdateChecker {
    private final paytofly plugin;
    private final String currentVersion;
    private static final String GITHUB_API_URL = 
        "https://api.github.com/repos/znc15/paytofly/releases";

    public UpdateChecker(paytofly plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
    }

    public void checkForUpdates() {
        if (!plugin.getConfig().getBoolean("update-checker.enabled", true)) {
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    URL url = URI.create(GITHUB_API_URL).toURL();
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    
                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream())
                        );
                        StringBuilder response = new StringBuilder();
                        String line;
                        
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();

                        JSONParser parser = new JSONParser();
                        JSONArray releases = (JSONArray) parser.parse(response.toString());
                        
                        if (!releases.isEmpty()) {
                            JSONObject latestRelease = (JSONObject) releases.get(0);
                            String latestVersion = ((String) latestRelease.get("tag_name"))
                                .replace("v", "");

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (currentVersion.equals(latestVersion)) {
                                        plugin.getLogger().info("插件已是最新版本！");
                                    } else {
                                        plugin.getLogger().warning("发现新版本！");
                                        plugin.getLogger().warning("当前版本: " + currentVersion);
                                        plugin.getLogger().warning("最新版本: " + latestVersion);
                                        plugin.getLogger().warning(
                                            "下载地址: https://github.com/znc15/paytofly/releases"
                                        );
                                    }
                                }
                            }.runTask(plugin);
                        }
                    }
                } catch (Exception e) {
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            plugin.getLogger().warning("无法检查更新！");
                            if (plugin.getConfig().getBoolean("update-checker.debug", false)) {
                                e.printStackTrace();
                            }
                        }
                    }.runTask(plugin);
                }
            }
        }.runTaskAsynchronously(plugin);
    }
} 