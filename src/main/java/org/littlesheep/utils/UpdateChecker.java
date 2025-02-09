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
    private String latestVersion;

    public UpdateChecker(paytofly plugin) {
        this.plugin = plugin;
        this.currentVersion = plugin.getDescription().getVersion();
    }

    public String getLatestVersion() {
        return latestVersion;
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
                            String rawVersion = (String) latestRelease.get("tag_name");
                            plugin.getLogger().info("获取到的原始版本号: " + rawVersion);
                            
                            latestVersion = rawVersion.replace("v", "")
                                                    .replace("build", "")
                                                    .trim();
                            plugin.getLogger().info("处理后的版本号: " + latestVersion);
                            plugin.getLogger().info("当前版本号: " + plugin.getDescription().getVersion());
                            
                            if (!latestVersion.matches("\\d+(\\.\\d+)*")) {
                                plugin.getLogger().warning("无法解析版本号格式");
                                return;
                            }

                            String mainCurrentVersion = currentVersion.split("-")[0];
                            String mainLatestVersion = latestVersion.split("-")[0];

                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (mainCurrentVersion.equals(mainLatestVersion)) {
                                        plugin.getLogger().info("插件已是最新版本！");
                                    } else {
                                        plugin.getLogger().warning("发现新版本！");
                                        plugin.getLogger().warning("当前版本: " + mainCurrentVersion);
                                        plugin.getLogger().warning("最新版本: " + mainLatestVersion);
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