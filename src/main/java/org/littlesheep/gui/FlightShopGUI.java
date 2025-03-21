package org.littlesheep.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.littlesheep.paytofly;
import org.bukkit.ChatColor;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlightShopGUI implements Listener {
    private final paytofly plugin;
    private FileConfiguration guiConfig;
    private String guiTitle;
    private int guiSize;
    private final Map<Integer, String> slotCommands;

    public FlightShopGUI(paytofly plugin) {
        this.plugin = plugin;
        this.slotCommands = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);  // 注册监听器
        
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
            try {
                String path = "items." + key + ".";
                int slot = guiConfig.getInt(path + "slot");
                Material material;
                try {
                    material = Material.valueOf(guiConfig.getString(path + "material"));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("无效的物品材质: " + guiConfig.getString(path + "material") + " 在 " + path + "material");
                    material = Material.STONE;
                }
                String name = guiConfig.getString(path + "name", "").replace("&", "§");
                List<String> lore = new ArrayList<>();
                
                // 获取物品价格和时间信息
                String command = guiConfig.getString(path + "command", "");
                String[] cmdParts = command.split(" ");
                if (cmdParts.length < 2) {
                    plugin.getLogger().warning("无效的命令格式: " + command + " 在 " + path + "command");
                    continue;
                }
                
                String timeArg = cmdParts[1];
                if (!timeArg.matches("\\d+[mhdwM]")) {
                    plugin.getLogger().warning("无效的时间格式: " + timeArg + " 在 " + path + "command");
                    continue;
                }
                
                String timeUnit = timeArg.substring(timeArg.length() - 1);
                double cost = getCost(timeArg);
                int minTime = plugin.getConfig().getInt("time-limits." + getTimeUnitName(timeUnit) + ".min", 1);
                
                // 处理lore
                for (String line : guiConfig.getStringList(path + "lore")) {
                    lore.add(line.replace("&", "§")
                            .replace("{cost}", String.format("%.2f", cost))
                            .replace("{min_time}", String.valueOf(minTime))
                            .replace("{unit}", getTimeUnitDisplay(timeUnit)));
                }

                ItemStack item = createItem(material, name, lore);
                gui.setItem(slot, item);
                slotCommands.put(slot, command);
            } catch (Exception e) {
                plugin.getLogger().warning("处理商品时出错: " + key);
                plugin.getLogger().warning(e.getMessage());
            }
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

        createCustomTimeItem(gui);

        player.openInventory(gui);
    }

    private double getCost(String timeCommand) {
        try {
            if (!timeCommand.matches("\\d+[mhdwM]")) {
                plugin.getLogger().warning("无效的时间格式: " + timeCommand);
                return 0.0;
            }
            
            String lastChar = timeCommand.substring(timeCommand.length() - 1);
            String amount = timeCommand.substring(0, timeCommand.length() - 1);
            int time;
            
            try {
                time = Integer.parseInt(amount);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("无效的时间数值: " + amount + " 在 " + timeCommand);
                return 0.0;
            }
            
            switch (lastChar) {
                case "m": return plugin.getConfig().getDouble("fly-cost.minute") * time;
                case "h": return plugin.getConfig().getDouble("fly-cost.hour") * time;
                case "d": return plugin.getConfig().getDouble("fly-cost.day") * time;
                case "w": return plugin.getConfig().getDouble("fly-cost.week") * time;
                case "M": return plugin.getConfig().getDouble("fly-cost.month") * time;
                default: return 0.0;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("计算价格时出错: " + timeCommand);
            plugin.getLogger().warning(e.getMessage());
            return 0.0;
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

    private void createCustomTimeItem(Inventory inv) {
        if (!guiConfig.getBoolean("custom-time.enabled", true)) return;
        
        try {
            ItemStack customTimeItem;
            try {
                Material material = Material.valueOf(guiConfig.getString("custom-time.item", "CLOCK"));
                customTimeItem = new ItemStack(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的自定义时间项材质: " + guiConfig.getString("custom-time.item", "CLOCK"));
                customTimeItem = new ItemStack(Material.CLOCK);
            }
            
            ItemMeta meta = customTimeItem.getItemMeta();
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', 
                guiConfig.getString("custom-time.name", "&e自定义购买时间")));
            
            List<String> lore = new ArrayList<>();
            double pricePerMinute = plugin.getConfig().getDouble("fly-cost.minute", 10.0);
            for (String line : guiConfig.getStringList("custom-time.lore")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', 
                    line.replace("%price%", String.valueOf(pricePerMinute))));
            }
            
            meta.setLore(lore);
            customTimeItem.setItemMeta(meta);
            
            int slot = guiConfig.getInt("custom-time.slot", 22);
            if (slot >= 0 && slot < inv.getSize()) {
                inv.setItem(slot, customTimeItem);
            } else {
                plugin.getLogger().warning("自定义时间项槽位无效: " + slot + ", 必须在 0-" + (inv.getSize() - 1) + " 之间");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("创建自定义时间项时出错");
            plugin.getLogger().warning(e.getMessage());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(guiTitle)) return;
        event.setCancelled(true);
        
        if (event.getCurrentItem() == null) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        
        try {
            // 处理自定义时间项
            int customTimeSlot = guiConfig.getInt("custom-time.slot", 22);
            if (slot == customTimeSlot && guiConfig.getBoolean("custom-time.enabled", true)) {
                double pricePerMinute = plugin.getConfig().getDouble("fly-cost.minute", 10.0);
                int minMinutes = plugin.getConfig().getInt("time-limits.minute.min", 5);
                player.closeInventory();
                player.sendMessage(plugin.getPrefix() + "§e请在聊天框中输入想要购买的时间（分钟）");
                player.sendMessage(plugin.getPrefix() + "§7每分钟需要 §6" + pricePerMinute + " §7金币");
                player.sendMessage(plugin.getPrefix() + "§7最低购买时间: §6" + minMinutes + " §7分钟");
                player.sendMessage(plugin.getPrefix() + "§7示例: §e输入 §6" + minMinutes + " §e购买 §6" + minMinutes + "分钟 §e飞行时间，需要 §6" + 
                    (pricePerMinute * minMinutes) + " §e金币");
                plugin.getCustomTimeManager().waitForInput(player);
                return;
            }
            
            // 处理其他商品项
            String command = slotCommands.get(slot);
            if (command != null) {
                // 检查时间限制
                String[] cmdParts = command.split(" ");
                if (cmdParts.length >= 2) {
                    String timeArg = cmdParts[1];
                    if (timeArg.matches("\\d+[mhdwM]")) {
                        String timeUnit = timeArg.substring(timeArg.length() - 1);
                        int amount;
                        try {
                            amount = Integer.parseInt(timeArg.substring(0, timeArg.length() - 1));
                            
                            // 获取对应时间单位的最小值
                            int minTime = plugin.getConfig().getInt("time-limits." + getTimeUnitName(timeUnit) + ".min", 1);
                            
                            if (amount < minTime) {
                                String message = plugin.getLang().getMessage("min-time-limit")
                                    .replace("{amount}", String.valueOf(minTime))
                                    .replace("{unit}", getTimeUnitDisplay(timeUnit));
                                player.sendMessage(plugin.getPrefix() + message);
                                return;
                            }
                        } catch (NumberFormatException e) {
                            plugin.getLogger().warning("无效的时间数值: " + timeArg);
                            return;
                        }
                    }
                }
                
                player.closeInventory();
                player.performCommand(command);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("处理GUI点击事件时出错");
            plugin.getLogger().warning(e.getMessage());
            player.sendMessage(plugin.getPrefix() + "§c处理请求时出错，请联系管理员");
        }
    }

    private String getTimeUnitName(String unit) {
        switch (unit) {
            case "h": return "hour";
            case "d": return "day";
            case "w": return "week";
            case "M": return "month";
            default: return "minute";
        }
    }

    private String getTimeUnitDisplay(String unit) {
        switch (unit) {
            case "m": return plugin.getLang().getMessage("time-format.minute");
            case "h": return plugin.getLang().getMessage("time-format.hour");
            case "d": return plugin.getLang().getMessage("time-format.day");
            case "w": return plugin.getLang().getMessage("time-format.week");
            case "M": return plugin.getLang().getMessage("time-format.month");
            default: return "";
        }
    }
    
    /**
     * 重新加载GUI配置文件
     */
    public void reloadConfig() {
        // 重新加载 GUI 配置
        File guiFile = new File(plugin.getDataFolder(), "gui.yml");
        if (guiFile.exists()) {
            try {
                this.guiConfig = YamlConfiguration.loadConfiguration(guiFile);
                
                // 更新基本设置
                this.guiTitle = guiConfig.getString("settings.title", "&b&l飞行商店").replace("&", "§");
                int newSize = guiConfig.getInt("settings.size", 27);
                // 确保尺寸是9的倍数
                if (newSize % 9 == 0 && newSize > 0 && newSize <= 54) {
                    this.guiSize = newSize;
                } else {
                    plugin.getLogger().warning("GUI尺寸必须是9的倍数且在1-54之间，使用默认值: 27");
                    this.guiSize = 27;
                }
                
                // 清除旧命令映射
                this.slotCommands.clear();
                
                plugin.getLogger().info("GUI配置文件已重新加载: 标题 = " + guiTitle + ", 尺寸 = " + guiSize);
                
                // 验证物品配置
                if (guiConfig.contains("items")) {
                    for (String key : guiConfig.getConfigurationSection("items").getKeys(false)) {
                        String path = "items." + key + ".";
                        int slot = guiConfig.getInt(path + "slot");
                        
                        // 验证槽位是否在合法范围内
                        if (slot < 0 || slot >= guiSize) {
                            plugin.getLogger().warning("物品 '" + key + "' 的槽位 (" + slot + ") 超出范围 (0-" + (guiSize - 1) + ")");
                        }
                        
                        // 验证材质是否有效
                        try {
                            Material.valueOf(guiConfig.getString(path + "material"));
                        } catch (IllegalArgumentException e) {
                            plugin.getLogger().warning("物品 '" + key + "' 使用了无效的材质: " + guiConfig.getString(path + "material"));
                        }
                        
                        // 验证命令格式
                        String command = guiConfig.getString(path + "command", "");
                        String[] cmdParts = command.split(" ");
                        if (cmdParts.length < 2) {
                            plugin.getLogger().warning("物品 '" + key + "' 的命令格式无效: " + command);
                        } else if (!cmdParts[1].matches("\\d+[mhdwM]")) {
                            plugin.getLogger().warning("物品 '" + key + "' 的时间格式无效: " + cmdParts[1]);
                        }
                    }
                } else {
                    plugin.getLogger().warning("GUI配置文件中缺少 'items' 节点");
                }
                
                // 验证自定义时间项配置
                if (guiConfig.getBoolean("custom-time.enabled", true)) {
                    int slot = guiConfig.getInt("custom-time.slot", 22);
                    if (slot < 0 || slot >= guiSize) {
                        plugin.getLogger().warning("自定义时间项的槽位 (" + slot + ") 超出范围 (0-" + (guiSize - 1) + ")");
                    }
                    
                    try {
                        Material.valueOf(guiConfig.getString("custom-time.item", "CLOCK"));
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("自定义时间项使用了无效的材质: " + guiConfig.getString("custom-time.item"));
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("重新加载GUI配置文件时出错");
                plugin.getLogger().severe(e.getMessage());
            }
        } else {
            plugin.getLogger().warning("无法找到 gui.yml 文件，无法重新加载 GUI 配置");
        }
    }
} 