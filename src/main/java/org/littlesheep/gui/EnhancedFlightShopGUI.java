package org.littlesheep.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.littlesheep.paytofly;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 增强版飞行商店GUI
 * 支持多页面、特效购买、速度购买等功能
 */
public class EnhancedFlightShopGUI implements Listener {
    
    private final paytofly plugin;
    private FileConfiguration guiConfig;
    private final Map<UUID, String> playerCurrentGUI = new HashMap<>();
    
    // GUI类型枚举
    public enum GUIType {
        MAIN("main"),
        EFFECTS("effects"), 
        SPEEDS("speeds"),
        EFFECT_SWITCHER("effect-switcher"),
        SPEED_SWITCHER("speed-switcher");
        
        private final String configKey;
        
        GUIType(String configKey) {
            this.configKey = configKey;
        }
        
        public String getConfigKey() {
            return configKey;
        }
    }
    
    public EnhancedFlightShopGUI(paytofly plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    /**
     * 加载GUI配置文件
     */
    private void loadConfig() {
        File guiFile = new File(plugin.getDataFolder(), "gui.yml");
        if (!guiFile.exists()) {
            try {
                plugin.saveResource("gui.yml", false);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().severe("无法找到默认的 gui.yml 文件！");
                return;
            }
        }
        
        this.guiConfig = YamlConfiguration.loadConfiguration(guiFile);
        
        // 检查版本并更新
        double configVersion = guiConfig.getDouble("version", 1.0);
        if (configVersion < 2.0) {
            plugin.getLogger().info("检测到旧版GUI配置，正在备份并更新...");
            
            File backupFile = new File(plugin.getDataFolder(), "gui.yml.bak." + System.currentTimeMillis());
            try {
                java.nio.file.Files.copy(guiFile.toPath(), backupFile.toPath());
                plugin.saveResource("gui.yml", true);
                this.guiConfig = YamlConfiguration.loadConfiguration(guiFile);
                plugin.getLogger().info("GUI配置已更新到v2.0！旧配置已备份");
            } catch (IOException e) {
                plugin.getLogger().severe("GUI配置更新失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 打开指定类型的GUI
     */
    public void openGUI(Player player, GUIType type) {
        switch (type) {
            case MAIN:
                openMainGUI(player);
                break;
            case EFFECTS:
                openEffectsGUI(player);
                break;
            case SPEEDS:
                openSpeedsGUI(player);
                break;
            case EFFECT_SWITCHER:
                openEffectSwitcherGUI(player);
                break;
            case SPEED_SWITCHER:
                openSpeedSwitcherGUI(player);
                break;
        }
        
        playerCurrentGUI.put(player.getUniqueId(), type.getConfigKey());
    }
    
    /**
     * 打开主界面
     */
    public void openMainGUI(Player player) {
        String title = ChatColor.translateAlternateColorCodes('&', 
            guiConfig.getString("settings.main.title", "&b&l✈ &f飞行商店 &b&l✈"));
        int size = guiConfig.getInt("settings.main.size", 54);
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        // 填充背景
        fillBackground(gui, "settings.main");
        
        // 添加主界面物品
        addMainItems(gui, player);
        
        player.openInventory(gui);
    }
    
    /**
     * 打开特效商店
     */
    public void openEffectsGUI(Player player) {
        String title = ChatColor.translateAlternateColorCodes('&', 
            guiConfig.getString("settings.effects.title", "&d&l✨ &f特效商店 &d&l✨"));
        int size = guiConfig.getInt("settings.effects.size", 45);
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        // 填充背景
        fillBackground(gui, "settings.effects");
        
        // 添加特效物品
        addEffectItems(gui, player);
        
        player.openInventory(gui);
    }
    
    /**
     * 打开速度商店
     */
    public void openSpeedsGUI(Player player) {
        String title = ChatColor.translateAlternateColorCodes('&', 
            guiConfig.getString("settings.speeds.title", "&a&l⚡ &f速度商店 &a&l⚡"));
        int size = guiConfig.getInt("settings.speeds.size", 45);
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        // 填充背景
        fillBackground(gui, "settings.speeds");
        
        // 添加速度物品
        addSpeedItems(gui, player);
        
        player.openInventory(gui);
    }
    
    /**
     * 填充背景
     */
    private void fillBackground(Inventory gui, String settingsPath) {
        boolean fillEmpty = guiConfig.getBoolean(settingsPath + ".fill-empty", true);
        if (!fillEmpty) return;
        
        Material fillMaterial = Material.valueOf(
            guiConfig.getString(settingsPath + ".fill-material", "BLACK_STAINED_GLASS_PANE"));
        String fillName = ChatColor.translateAlternateColorCodes('&',
            guiConfig.getString(settingsPath + ".fill-name", " "));
        
        ItemStack fillItem = createItem(fillMaterial, fillName, null, false);
        
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, fillItem);
            }
        }
    }
    
    /**
     * 添加主界面物品
     */
    private void addMainItems(Inventory gui, Player player) {
        if (!guiConfig.contains("main-items")) return;
        
        for (String key : guiConfig.getConfigurationSection("main-items").getKeys(false)) {
            String path = "main-items." + key + ".";
            
            int slot = guiConfig.getInt(path + "slot", -1);
            if (slot < 0 || slot >= gui.getSize()) continue;
            
            ItemStack item = createMainItem(key, path, player);
            if (item != null) {
                gui.setItem(slot, item);
            }
        }
    }
    
    /**
     * 创建主界面物品
     */
    private ItemStack createMainItem(String key, String path, Player player) {
        try {
            Material material = Material.valueOf(guiConfig.getString(path + "material", "STONE"));
            String name = ChatColor.translateAlternateColorCodes('&', 
                guiConfig.getString(path + "name", ""));
            boolean glow = guiConfig.getBoolean(path + "glow", false);
            
            // 计算价格（如果是飞行时间购买项）
            String command = guiConfig.getString(path + "command", "");
            double cost = 0.0;
            if (command.startsWith("fly ") && command.split(" ").length >= 2) {
                String timeArg = command.split(" ")[1];
                cost = calculateFlightCost(timeArg);
            }
            
            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList(path + "lore")) {
                String processedLine = line.replace("{cost}", String.format("%.0f", cost));
                lore.add(ChatColor.translateAlternateColorCodes('&', 
                    replacePlaceholders(processedLine, player)));
            }
            
            ItemStack item = createItem(material, name, lore, glow);
            
            // 特殊处理玩家头颅
            if (material == Material.PLAYER_HEAD && guiConfig.contains(path + "skull-owner")) {
                String owner = replacePlaceholders(guiConfig.getString(path + "skull-owner"), player);
                setSkullOwner(item, owner);
            }
            
            return item;
        } catch (Exception e) {
            plugin.getLogger().warning("创建主界面物品失败: " + key + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 添加特效物品
     */
    private void addEffectItems(Inventory gui, Player player) {
        if (!guiConfig.contains("effect-items")) return;
        
        for (String key : guiConfig.getConfigurationSection("effect-items").getKeys(false)) {
            String path = "effect-items." + key + ".";
            
            int slot = guiConfig.getInt(path + "slot", -1);
            if (slot < 0 || slot >= gui.getSize()) continue;
            
            ItemStack item = createShopItem(key, path, player, "effect");
            if (item != null) {
                gui.setItem(slot, item);
            }
        }
    }
    
    /**
     * 添加速度物品
     */
    private void addSpeedItems(Inventory gui, Player player) {
        if (!guiConfig.contains("speed-items")) return;
        
        for (String key : guiConfig.getConfigurationSection("speed-items").getKeys(false)) {
            String path = "speed-items." + key + ".";
            
            int slot = guiConfig.getInt(path + "slot", -1);
            if (slot < 0 || slot >= gui.getSize()) continue;
            
            ItemStack item = createShopItem(key, path, player, "speed");
            if (item != null) {
                gui.setItem(slot, item);
            }
        }
    }
    
    /**
     * 创建商店物品
     */
    private ItemStack createShopItem(String key, String path, Player player, String type) {
        try {
            Material material = Material.valueOf(guiConfig.getString(path + "material", "STONE"));
            String name = ChatColor.translateAlternateColorCodes('&', 
                guiConfig.getString(path + "name", ""));
            boolean glow = guiConfig.getBoolean(path + "glow", false);
            double price = guiConfig.getDouble(path + "price", 0.0);
            
            // 检查拥有状态和当前选中状态
            boolean owned = checkOwnership(player, key, type);
            boolean isCurrent = isCurrentSelection(player, key, type);
            String status = getOwnershipStatus(owned, isCurrent, price);
            
            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList(path + "lore")) {
                String processedLine = line
                    .replace("{price}", String.format("%.0f", price))
                    .replace("{status}", status);
                lore.add(ChatColor.translateAlternateColorCodes('&', processedLine));
            }
            
            // 如果是当前选中项，添加发光效果
            if (isCurrent) {
                glow = true;
            }
            
            return createItem(material, name, lore, glow);
        } catch (Exception e) {
            plugin.getLogger().warning("创建商店物品失败: " + key + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 检查玩家是否拥有特效或速度
     */
    private boolean checkOwnership(Player player, String key, String type) {
        if ("effect".equals(type)) {
            // 检查特效权限或购买状态
            String permission = "paytofly.effects." + key;
            if (player.hasPermission(permission) || player.hasPermission("paytofly.effects.*")) {
                return true;
            }
            return plugin.getStorage().getPlayerEffects(player.getUniqueId()).contains(key);
        } else if ("speed".equals(type)) {
            // 检查速度权限或购买状态
            if ("normal".equals(key) || "slow".equals(key)) {
                return true; // 免费速度
            }
            String permission = "paytofly.speed." + key;
            if (player.hasPermission(permission) || player.hasPermission("paytofly.speed.*")) {
                return true;
            }
            return plugin.getStorage().getPlayerSpeeds(player.getUniqueId()).contains(key);
        }
        return false;
    }

    /**
     * 检查玩家当前选中的特效或速度
     */
    private boolean isCurrentSelection(Player player, String key, String type) {
        if ("effect".equals(type)) {
            var currentEffect = plugin.getEffectManager().getPlayerEffect(player);
            return currentEffect.getName().equals(key);
        } else if ("speed".equals(type)) {
            var currentSpeed = plugin.getSpeedManager().getPlayerSpeed(player);
            return currentSpeed.getName().equals(key);
        }
        return false;
    }
    
    /**
     * 获取拥有状态显示
     */
    private String getOwnershipStatus(boolean owned, boolean isCurrent, double price) {
        if (price == 0.0) {
            return isCurrent ? "&e★ 当前使用" : "&a✓ 点击使用";
        } else if (owned) {
            return isCurrent ? "&e★ 当前使用" : "&a✓ 点击切换";
        } else {
            return "&c➤ 点击购买";
        }
    }
    
    /**
     * 创建物品
     */
    private ItemStack createItem(Material material, String name, List<String> lore, boolean glow) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        if (name != null && !name.trim().isEmpty()) {
            meta.setDisplayName(name);
        }
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore);
        }
        
        // 添加发光效果
        if (glow) {
            meta.addEnchant(Enchantment.LURE, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        
        item.setItemMeta(meta);
        return item;
    }
    
    /**
     * 设置头颅所有者
     */
    private void setSkullOwner(ItemStack skull, String owner) {
        if (skull.getType() != Material.PLAYER_HEAD) return;
        
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta != null) {
            try {
                // 优先尝试通过玩家名获取在线玩家
                Player onlinePlayer = Bukkit.getPlayer(owner);
                if (onlinePlayer != null) {
                    meta.setOwningPlayer(onlinePlayer);
                } else {
                    // 如果不在线，尝试通过UUID获取
                    @SuppressWarnings("deprecation")
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(owner);
                    meta.setOwningPlayer(offlinePlayer);
                }
                skull.setItemMeta(meta);
            } catch (Exception e) {
                plugin.getLogger().warning("设置头颅所有者失败: " + owner + " - " + e.getMessage());
            }
        }
    }
    
    /**
     * 计算飞行时间价格
     */
    private double calculateFlightCost(String timeArg) {
        try {
            if (timeArg.endsWith("mo")) {
                String amount = timeArg.substring(0, timeArg.length() - 2);
                int time = Integer.parseInt(amount);
                return plugin.getConfig().getDouble("fly-cost.month", 1000.0) * time;
            } else if (timeArg.matches("\\d+[mhdw]")) {
                String lastChar = timeArg.substring(timeArg.length() - 1);
                String amount = timeArg.substring(0, timeArg.length() - 1);
                int time = Integer.parseInt(amount);
                
                switch (lastChar) {
                    case "m":
                        return plugin.getConfig().getDouble("fly-cost.minute", 10.0) * time;
                    case "h":
                        return plugin.getConfig().getDouble("fly-cost.hour", 100.0) * time;
                    case "d":
                        return plugin.getConfig().getDouble("fly-cost.day", 500.0) * time;
                    case "w":
                        return plugin.getConfig().getDouble("fly-cost.week", 2000.0) * time;
                    default:
                        return 0.0;
                }
            }
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("无效的时间格式: " + timeArg);
        }
        return 0.0;
    }
    
    /**
     * 替换占位符
     */
    private String replacePlaceholders(String text, Player player) {
        if (text == null) return "";
        
        // 基础占位符
        text = text.replace("{player}", player.getName());
        
        // 经济相关
        if (plugin.getEconomyManager() != null && plugin.getEconomyManager().isInitialized()) {
            double balance = plugin.getEconomyManager().getBalance(player);
            text = text.replace("{balance}", String.format("%.2f", balance));
        } else {
            text = text.replace("{balance}", "N/A");
        }
        
        // 飞行状态
        boolean isFlying = player.isFlying();
        text = text.replace("{flight_status}", isFlying ? "&a飞行中" : "&c未飞行");
        
        // 剩余时间 - 优先使用存储中的数据
        if (player.hasPermission("paytofly.infinite")) {
            text = text.replace("{remaining_time}", "&d无限");
        } else {
            // 先检查存储中的数据
            Long endTime = plugin.getStorage().getPlayerFlightTime(player.getUniqueId());
            if (endTime == null) {
                // 如果存储中没有，检查内存中的数据
                endTime = plugin.getFlyingPlayers().get(player.getUniqueId());
            }
            
            if (endTime != null && endTime > System.currentTimeMillis()) {
                long remaining = endTime - System.currentTimeMillis();
                text = text.replace("{remaining_time}", "&a" + formatTime(remaining));
            } else {
                text = text.replace("{remaining_time}", "&c已过期");
            }
        }
        
        // 价格相关
        double pricePerMinute = plugin.getConfig().getDouble("fly-cost.minute", 10.0);
        int minMinutes = plugin.getConfig().getInt("time-limits.minute.min", 5);
        text = text.replace("{price}", String.valueOf(pricePerMinute));
        text = text.replace("{min}", String.valueOf(minMinutes));
        
        // 动态价格计算（如果需要）
        // 这个方法会在创建物品时被调用，价格计算在那里处理
        
        return text;
    }
    
    /**
     * 格式化时间
     */
    private String formatTime(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "天" + (hours % 24) + "小时";
        } else if (hours > 0) {
            return hours + "小时" + (minutes % 60) + "分钟";
        } else if (minutes > 0) {
            return minutes + "分钟";
        } else {
            return seconds + "秒";
        }
    }
    
    /**
     * 处理GUI点击事件
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String title = event.getView().getTitle();
        
        // 检查是否是我们的GUI
        if (!isOurGUI(title)) return;
        
        event.setCancelled(true);
        
        if (event.getCurrentItem() == null) return;
        
        int slot = event.getSlot();
        String currentGUI = playerCurrentGUI.get(player.getUniqueId());
        
        try {
            handleGUIClick(player, slot, currentGUI);
        } catch (Exception e) {
            player.sendMessage(plugin.getPrefix() + "&c处理点击事件时出错，请联系管理员");
            plugin.getLogger().severe("GUI点击处理错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 检查是否是我们的GUI
     */
    private boolean isOurGUI(String title) {
        String cleanTitle = ChatColor.stripColor(title);
        return cleanTitle.contains("飞行商店") || 
               cleanTitle.contains("特效商店") || 
               cleanTitle.contains("速度商店");
    }
    
    /**
     * 处理GUI点击
     */
    private void handleGUIClick(Player player, int slot, String currentGUI) {
        if ("main".equals(currentGUI)) {
            handleMainGUIClick(player, slot);
        } else if ("effects".equals(currentGUI)) {
            handleEffectsGUIClick(player, slot);
        } else if ("speeds".equals(currentGUI)) {
            handleSpeedsGUIClick(player, slot);
        }
    }
    
    /**
     * 处理主界面点击
     */
    private void handleMainGUIClick(Player player, int slot) {
        // 查找对应的物品和命令
        for (String key : guiConfig.getConfigurationSection("main-items").getKeys(false)) {
            String path = "main-items." + key + ".";
            if (guiConfig.getInt(path + "slot", -1) == slot) {
                if ("effects-shop".equals(key)) {
                    player.closeInventory();
                    openGUI(player, GUIType.EFFECTS);
                } else if ("speeds-shop".equals(key)) {
                    player.closeInventory();
                    openGUI(player, GUIType.SPEEDS);
                } else if ("custom-time".equals(key)) {
                    handleCustomTimeClick(player);
                } else {
                    // 处理购买飞行时间
                    String command = guiConfig.getString(path + "command", "");
                    if (!command.isEmpty()) {
                        executeCommand(player, command);
                    }
                }
                break;
            }
        }
    }
    
    /**
     * 处理特效商店点击
     */
    private void handleEffectsGUIClick(Player player, int slot) {
        for (String key : guiConfig.getConfigurationSection("effect-items").getKeys(false)) {
            String path = "effect-items." + key + ".";
            if (guiConfig.getInt(path + "slot", -1) == slot) {
                if ("back".equals(key)) {
                    player.closeInventory();
                    openGUI(player, GUIType.MAIN);
                } else {
                    String command = guiConfig.getString(path + "command", "");
                    if (!command.isEmpty()) {
                        executeCommand(player, command);
                    }
                }
                break;
            }
        }
    }
    
    /**
     * 处理速度商店点击
     */
    private void handleSpeedsGUIClick(Player player, int slot) {
        for (String key : guiConfig.getConfigurationSection("speed-items").getKeys(false)) {
            String path = "speed-items." + key + ".";
            if (guiConfig.getInt(path + "slot", -1) == slot) {
                if ("back".equals(key)) {
                    player.closeInventory();
                    openGUI(player, GUIType.MAIN);
                } else {
                    String command = guiConfig.getString(path + "command", "");
                    if (!command.isEmpty()) {
                        executeCommand(player, command);
                    }
                }
                break;
            }
        }
    }
    
    /**
     * 处理自定义时间点击
     */
    private void handleCustomTimeClick(Player player) {
        if (!guiConfig.getBoolean("custom-time.enabled", true)) {
            player.sendMessage(plugin.getPrefix() + "&c自定义时间功能已禁用");
            return;
        }
        
        player.closeInventory();
        plugin.getCustomTimeManager().waitForInput(player);
        
        double pricePerMinute = plugin.getConfig().getDouble("fly-cost.minute", 10.0);
        int minMinutes = plugin.getConfig().getInt("time-limits.minute.min", 5);
        
        player.sendMessage(plugin.getPrefix() + plugin.getLang().getMessage("gui-input-time"));
        player.sendMessage(plugin.getPrefix() + plugin.getLang().getMessage("gui-price-per-minute", 
            "{price}", String.valueOf(pricePerMinute)));
        player.sendMessage(plugin.getPrefix() + plugin.getLang().getMessage("gui-min-time", 
            "{min}", String.valueOf(minMinutes)));
    }
    
    /**
     * 执行命令
     */
    private void executeCommand(Player player, String command) {
        player.closeInventory();
        
        try {
            if (command.startsWith("gui:")) {
                // 处理GUI导航命令
                String guiType = command.substring(4);
                if ("main".equals(guiType)) {
                    openGUI(player, GUIType.MAIN);
                }
            } else {
                // 执行普通命令
                plugin.getServer().dispatchCommand(player, command);
            }
        } catch (Exception e) {
            player.sendMessage(plugin.getPrefix() + "&c命令执行失败: " + e.getMessage());
            plugin.getLogger().severe("命令执行错误: " + command + " - " + e.getMessage());
        }
    }
    
    /**
     * 重载配置
     */
    public void reloadConfig() {
        loadConfig();
        playerCurrentGUI.clear(); // 清理玩家GUI状态
        plugin.getLogger().info("增强版GUI配置已重载");
    }
    
    /**
     * 清理玩家数据
     */
    public void removePlayer(Player player) {
        playerCurrentGUI.remove(player.getUniqueId());
    }
}