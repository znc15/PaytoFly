package org.littlesheep.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.littlesheep.paytofly;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlightShopGUI {
    private final paytofly plugin;
    private FileConfiguration guiConfig;
    private String guiTitle;
    private int guiSize;
    private final Map<Integer, String> slotCommands;

    public FlightShopGUI(paytofly plugin) {
        this.plugin = plugin;
        this.slotCommands = new HashMap<>();
        
        // 加载GUI配置
        File guiFile = new File(plugin.getDataFolder(), "gui.yml");
        if (!guiFile.exists()) {
            try {
                plugin.saveResource("gui.yml", false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().severe("无法找到默认的 gui.yml 文件！");
                plugin.getLogger().severe("请确保插件正确安装！");
                return;
            }
        }
        
        this.guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        
        // 检查配置文件版本
        double configVersion = guiConfig.getDouble("version", 1.0);
        double requiredVersion = plugin.getVersionManager().getGUIVersion();
        
        if (configVersion < requiredVersion) {
            plugin.getLogger().warning("GUI配置文件版本过低！");
            plugin.getLogger().warning("当前版本: " + configVersion);
            plugin.getLogger().warning("最新版本: " + requiredVersion);
            plugin.getLogger().warning("正在备份并更新配置文件...");
            
            // 备份旧配置
            File backupFile = new File(plugin.getDataFolder(), "gui.yml.bak");
            try {
                java.nio.file.Files.copy(
                    guiFile.toPath(),
                    backupFile.toPath(),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
                plugin.saveResource("gui.yml", true);
                this.guiConfig = YamlConfiguration.loadConfiguration(guiFile);
                plugin.getLogger().info("GUI配置文件已更新！旧配置已备份为 gui.yml.bak");
            } catch (IOException e) {
                plugin.getLogger().severe("配置文件更新失败！");
                e.printStackTrace();
            }
        }
        
        // 读取基本设置
        this.guiTitle = guiConfig.getString("settings.title", "&b&l飞行商店").replace("&", "§");
        this.guiSize = guiConfig.getInt("settings.size", 27);
    }

    public void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(null, guiSize, guiTitle);

        // 添加商品
        for (String key : guiConfig.getConfigurationSection("items").getKeys(false)) {
            String path = "items." + key + ".";
            int slot = guiConfig.getInt(path + "slot");
            Material material = Material.valueOf(guiConfig.getString(path + "material"));
            String name = guiConfig.getString(path + "name", "").replace("&", "§");
            List<String> lore = new ArrayList<>();
            
            // 获取物品价格
            String command = guiConfig.getString(path + "command", "").split(" ")[1];
            double cost = getCost(command);
            
            // 处理lore
            for (String line : guiConfig.getStringList(path + "lore")) {
                lore.add(line.replace("&", "§")
                           .replace("{cost}", String.format("%.2f", cost)));
            }

            ItemStack item = createItem(material, name, lore);
            gui.setItem(slot, item);
            slotCommands.put(slot, guiConfig.getString(path + "command"));
        }

        // 填充空位
        if (guiConfig.getBoolean("settings.fill-empty", true)) {
            Material fillMaterial = Material.valueOf(
                guiConfig.getString("settings.fill-material", "BLACK_STAINED_GLASS_PANE"));
            ItemStack fill = createItem(fillMaterial, " ", null);
            
            for (int i = 0; i < gui.getSize(); i++) {
                if (gui.getItem(i) == null) {
                    gui.setItem(i, fill);
                }
            }
        }

        player.openInventory(gui);
    }

    private double getCost(String timeCommand) {
        String lastChar = timeCommand.substring(timeCommand.length() - 1);
        String amount = timeCommand.substring(0, timeCommand.length() - 1);
        int time = Integer.parseInt(amount);
        
        switch (lastChar) {
            case "h": return plugin.getConfig().getDouble("fly-cost.hour") * time;
            case "d": return plugin.getConfig().getDouble("fly-cost.day") * time;
            case "w": return plugin.getConfig().getDouble("fly-cost.week") * time;
            case "M": return plugin.getConfig().getDouble("fly-cost.month") * time;
            default: return 0.0;
        }
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (name != null) meta.setDisplayName(name);
        if (lore != null) meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void handleClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(guiTitle)) {
            return;
        }

        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        int slot = event.getSlot();
        String command = slotCommands.get(slot);
        
        if (command != null) {
            player.closeInventory();
            player.performCommand(command);
        }
    }
} 