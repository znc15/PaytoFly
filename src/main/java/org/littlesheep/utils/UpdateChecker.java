package org.littlesheep.utils;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.littlesheep.paytofly;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.stream.Collectors;

public class UpdateChecker {
    private final paytofly plugin;
    private static final String GITHUB_API_URL = 
        "https://api.github.com/repos/znc15/paytofly/releases";
    private String latestVersion;

    public UpdateChecker(paytofly plugin) {
        this.plugin = plugin;
    }

    public String getLatestVersion() {
        return latestVersion;
    }

    public void checkForUpdates() {
        plugin.getLogger().info("正在检查更新...");
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URI(GITHUB_API_URL).toURL();
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream())
                    );
                    String response = reader.lines().collect(Collectors.joining());
                    reader.close();

                    JSONArray releases = (JSONArray) new JSONParser().parse(response);

                    if (!releases.isEmpty()) {
                        boolean foundSnapshot = false;
                        for (Object obj : releases) {
                            JSONObject release = (JSONObject) obj;
                            String tagName = ((String) release.get("tag_name"))
                                .replace("v", "")
                                .replace("build", "")
                                .trim();
                            
                            if (tagName.contains("SNAPSHOT")) {
                                foundSnapshot = true;
                                latestVersion = tagName;
                                String currentVersion = plugin.getDescription().getVersion();
                                
                                plugin.getLogger().info("获取到的原始版本号: " + tagName);
                                plugin.getLogger().info("当前版本号: " + currentVersion);

                                if (!currentVersion.equals(latestVersion)) {
                                    plugin.getLogger().warning("发现新版本！");
                                    plugin.getLogger().warning("当前版本: " + currentVersion);
                                    plugin.getLogger().warning("最新版本: " + latestVersion);
                                    plugin.getLogger().warning(
                                        "下载地址: https://github.com/znc15/paytofly/releases"
                                    );
                                } else {
                                    plugin.getLogger().info("当前已是最新版本！");
                                }
                                break;
                            }
                        }
                        if (!foundSnapshot) {
                            plugin.getLogger().info("未找到正式版本信息");
                        }
                    } else {
                        plugin.getLogger().info("未获取到版本信息");
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("检查更新失败: " + e.getMessage());
            }
        });
    }
} 