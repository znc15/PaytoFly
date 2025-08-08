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
        SPEED_SWITCHER("speed-switcher"),
        EFFECT_TIME_PURCHASE("effect-time-purchase"),
        SPEED_TIME_PURCHASE("speed-time-purchase");
        
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
        openGUI(player, type, null);
    }
    
    public void openGUI(Player player, GUIType type, String extraData) {
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
            case EFFECT_TIME_PURCHASE:
                openEffectTimePurchaseGUI(player, extraData);
                break;
            case SPEED_TIME_PURCHASE:
                openSpeedTimePurchaseGUI(player, extraData);
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
     * 打开特效切换器
     */
    public void openEffectSwitcherGUI(Player player) {
        String title = ChatColor.translateAlternateColorCodes('&', 
            guiConfig.getString("settings.effect-switcher.title", "&d&l✨ &f特效切换器"));
        int size = guiConfig.getInt("settings.effect-switcher.size", 45);
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        // 填充背景
        fillBackground(gui, "settings.effect-switcher");
        
        // 添加特效切换器物品
        addEffectSwitcherItems(gui, player);
        
        player.openInventory(gui);
    }
    
    /**
     * 打开速度切换器
     */
    public void openSpeedSwitcherGUI(Player player) {
        String title = ChatColor.translateAlternateColorCodes('&', 
            guiConfig.getString("settings.speed-switcher.title", "&a&l⚡ &f速度切换器"));
        int size = guiConfig.getInt("settings.speed-switcher.size", 45);
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        // 填充背景
        fillBackground(gui, "settings.speed-switcher");
        
        // 添加速度切换器物品
        addSpeedSwitcherItems(gui, player);
        
        player.openInventory(gui);
    }
    
    /**
     * 打开特效时间购买界面
     */
    public void openEffectTimePurchaseGUI(Player player, String effectName) {
        if (effectName == null) {
            player.sendMessage(plugin.getPrefix() + "&c特效名称不能为空！");
            return;
        }
        
        String effectDisplayName = getEffectDisplayName(effectName);
        String title = ChatColor.translateAlternateColorCodes('&', 
            guiConfig.getString("settings.effect-time-purchase.title", "&d&l✨ &f{effect_name} &d&l- 时间购买")
                .replace("{effect_name}", effectDisplayName));
        int size = guiConfig.getInt("settings.effect-time-purchase.size", 45);
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        // 填充背景
        fillBackground(gui, "settings.effect-time-purchase");
        
        // 添加时间购买选项
        addEffectTimePurchaseItems(gui, player, effectName);
        
        player.openInventory(gui);
    }
    
    /**
     * 打开速度时间购买界面
     */
    public void openSpeedTimePurchaseGUI(Player player, String speedName) {
        if (speedName == null) {
            player.sendMessage(plugin.getPrefix() + "&c速度名称不能为空！");
            return;
        }
        
        String speedDisplayName = getSpeedDisplayName(speedName);
        String title = ChatColor.translateAlternateColorCodes('&', 
            guiConfig.getString("settings.speed-time-purchase.title", "&a&l⚡ &f{speed_name} &a&l- 时间购买")
                .replace("{speed_name}", speedDisplayName));
        int size = guiConfig.getInt("settings.speed-time-purchase.size", 45);
        
        Inventory gui = Bukkit.createInventory(null, size, title);
        
        // 填充背景
        fillBackground(gui, "settings.speed-time-purchase");
        
        // 添加时间购买选项
        addSpeedTimePurchaseItems(gui, player, speedName);
        
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
     * 添加特效切换器物品
     */
    private void addEffectSwitcherItems(Inventory gui, Player player) {
        if (!guiConfig.contains("effect-switcher-items")) return;
        
        for (String key : guiConfig.getConfigurationSection("effect-switcher-items").getKeys(false)) {
            String path = "effect-switcher-items." + key + ".";
            
            int slot = guiConfig.getInt(path + "slot", -1);
            if (slot < 0 || slot >= gui.getSize()) continue;
            
            ItemStack item = createSwitcherItem(key, path, player, "effect");
            if (item != null) {
                gui.setItem(slot, item);
            }
        }
    }
    
    /**
     * 添加速度切换器物品
     */
    private void addSpeedSwitcherItems(Inventory gui, Player player) {
        if (!guiConfig.contains("speed-switcher-items")) return;
        
        for (String key : guiConfig.getConfigurationSection("speed-switcher-items").getKeys(false)) {
            String path = "speed-switcher-items." + key + ".";
            
            int slot = guiConfig.getInt(path + "slot", -1);
            if (slot < 0 || slot >= gui.getSize()) continue;
            
            ItemStack item = createSwitcherItem(key, path, player, "speed");
            if (item != null) {
                gui.setItem(slot, item);
            }
        }
    }
    
    /**
     * 添加特效时间购买物品
     */
    private void addEffectTimePurchaseItems(Inventory gui, Player player, String effectName) {
        // 获取基础价格
        double basePrice = guiConfig.getDouble("effect-items." + effectName + ".price", 100.0);
        if (basePrice == 0.0) {
            basePrice = plugin.getConfig().getDouble("flight-effects.purchase.prices." + effectName, 100.0);
        }
        
        // 获取时间倍数
        double hourMultiplier = plugin.getConfig().getDouble("flight-effects.time-purchase.hour-multiplier", 0.1);
        double dayMultiplier = plugin.getConfig().getDouble("flight-effects.time-purchase.day-multiplier", 2.0);
        double weekMultiplier = plugin.getConfig().getDouble("flight-effects.time-purchase.week-multiplier", 12.0);
        
        // 1小时选项
        ItemStack hourItem = createTimePurchaseItem(
            Material.CLOCK, "&e&l⏰ 1小时 " + getEffectDisplayName(effectName),
            basePrice * hourMultiplier, "1小时后自动失效",
            "fly effect buy " + effectName + " 1 hour"
        );
        gui.setItem(11, hourItem);
        
        // 1天选项
        ItemStack dayItem = createTimePurchaseItem(
            Material.SUNFLOWER, "&6&l☀ 1天 " + getEffectDisplayName(effectName),
            basePrice * dayMultiplier, "1天后自动失效",
            "fly effect buy " + effectName + " 1 day"
        );
        gui.setItem(13, dayItem);
        
        // 1周选项
        ItemStack weekItem = createTimePurchaseItem(
            Material.DIAMOND, "&d&l💎 1周 " + getEffectDisplayName(effectName),
            basePrice * weekMultiplier, "1周后自动失效",
            "fly effect buy " + effectName + " 1 week"
        );
        gui.setItem(15, weekItem);
        
        // 返回按钮
        ItemStack backItem = createItem(Material.BARRIER, "&c&l← 返回特效商店", 
            List.of("&7点击返回特效商店"), false);
        gui.setItem(40, backItem);
    }
    
    /**
     * 添加速度时间购买物品
     */
    private void addSpeedTimePurchaseItems(Inventory gui, Player player, String speedName) {
        // 获取基础价格
        double basePrice = guiConfig.getDouble("speed-items." + speedName + ".price", 100.0);
        if (basePrice == 0.0) {
            basePrice = plugin.getConfig().getDouble("flight-speed.purchase.prices." + speedName, 100.0);
        }
        
        // 获取时间倍数
        double hourMultiplier = plugin.getConfig().getDouble("flight-speed.time-purchase.hour-multiplier", 0.1);
        double dayMultiplier = plugin.getConfig().getDouble("flight-speed.time-purchase.day-multiplier", 2.0);
        double weekMultiplier = plugin.getConfig().getDouble("flight-speed.time-purchase.week-multiplier", 12.0);
        
        // 1小时选项
        ItemStack hourItem = createTimePurchaseItem(
            Material.CLOCK, "&e&l⏰ 1小时 " + getSpeedDisplayName(speedName),
            basePrice * hourMultiplier, "1小时后自动失效",
            "fly speed buy " + speedName + " 1 hour"
        );
        gui.setItem(11, hourItem);
        
        // 1天选项
        ItemStack dayItem = createTimePurchaseItem(
            Material.SUNFLOWER, "&6&l☀ 1天 " + getSpeedDisplayName(speedName),
            basePrice * dayMultiplier, "1天后自动失效",
            "fly speed buy " + speedName + " 1 day"
        );
        gui.setItem(13, dayItem);
        
        // 1周选项
        ItemStack weekItem = createTimePurchaseItem(
            Material.DIAMOND, "&d&l💎 1周 " + getSpeedDisplayName(speedName),
            basePrice * weekMultiplier, "1周后自动失效",
            "fly speed buy " + speedName + " 1 week"
        );
        gui.setItem(15, weekItem);
        
        // 返回按钮
        ItemStack backItem = createItem(Material.BARRIER, "&c&l← 返回速度商店", 
            List.of("&7点击返回速度商店"), false);
        gui.setItem(40, backItem);
    }
    
    /**
     * 创建时间购买物品
     */
    private ItemStack createTimePurchaseItem(Material material, String name, double price, String duration, String command) {
        List<String> lore = new ArrayList<>();
        lore.add("&f━━━━━━━━━━━━━━━━");
        lore.add("&7▸ 价格: &6" + String.format("%.2f", price) + " &7金币");
        lore.add("&7▸ 时长: &e" + duration);
        lore.add("&7▸ 到期后自动失效");
        lore.add("&f━━━━━━━━━━━━━━━━");
        lore.add("&a➤ 点击购买");
        
        ItemStack item = createItem(material, name, lore, true);
        
        // 存储命令到物品的 NBT 中（用于点击处理）
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            // 可以在这里添加自定义标签来存储命令
            item.setItemMeta(meta);
        }
        
        return item;
    }
    
    /**
     * 获取特效显示名称
     */
    private String getEffectDisplayName(String effectName) {
        String configName = guiConfig.getString("effect-items." + effectName + ".name");
        if (configName != null) {
            return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', configName));
        }
        
        // 如果配置中没有，使用默认名称
        switch (effectName.toLowerCase()) {
            case "basic": return "基础特效";
            case "rainbow": return "彩虹特效";
            case "star": return "星星特效";
            case "fire": return "火焰特效";
            case "magic": return "魔法特效";
            case "dragon": return "龙息特效";
            default: return effectName;
        }
    }
    
    /**
     * 获取速度显示名称
     */
    private String getSpeedDisplayName(String speedName) {
        String configName = guiConfig.getString("speed-items." + speedName + ".name");
        if (configName != null) {
            return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', configName));
        }
        
        // 如果配置中没有，使用默认名称
        switch (speedName.toLowerCase()) {
            case "slow": return "缓慢速度";
            case "normal": return "普通速度";
            case "fast": return "快速";
            case "very_fast": return "极速";
            case "super_fast": return "超速";
            case "light_speed": return "光速";
            case "warp_speed": return "曲速";
            default: return speedName;
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
     * 创建切换器物品
     */
    private ItemStack createSwitcherItem(String key, String path, Player player, String type) {
        try {
            Material material = Material.valueOf(guiConfig.getString(path + "material", "STONE"));
            String name = ChatColor.translateAlternateColorCodes('&', 
                guiConfig.getString(path + "name", ""));
            boolean glow = guiConfig.getBoolean(path + "glow", false);
            
            // 检查是否拥有此特效/速度
            boolean owned = checkOwnership(player, key, type);
            boolean isCurrent = isCurrentSelection(player, key, type);
            
            List<String> lore = new ArrayList<>();
            for (String line : guiConfig.getStringList(path + "lore")) {
                if (line.contains("{status}")) {
                    String status = getSwitcherStatus(owned, isCurrent);
                    line = line.replace("{status}", status);
                }
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
            
            // 如果是当前选择的，添加光效
            if (isCurrent) {
                glow = true;
            }
            
            return createItem(material, name, lore, glow);
        } catch (Exception e) {
            plugin.getLogger().warning("创建切换器物品失败: " + key + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取切换器状态显示
     */
    private String getSwitcherStatus(boolean owned, boolean isCurrent) {
        if (isCurrent) {
            return "&e★ 当前使用";
        } else if (owned) {
            return "&a✓ 点击切换";
        } else {
            return "&c✗ 未拥有";
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
            // 检查永久购买
            if (plugin.getStorage().getPlayerEffects(player.getUniqueId()).contains(key)) {
                return true;
            }
            // 检查时间限制购买
            Long effectTime = plugin.getStorage().getPlayerEffectTime(player.getUniqueId(), key);
            return effectTime != null && effectTime > System.currentTimeMillis();
        } else if ("speed".equals(type)) {
            // 检查速度权限或购买状态
            if ("normal".equals(key) || "slow".equals(key)) {
                return true; // 免费速度
            }
            String permission = "paytofly.speed." + key;
            if (player.hasPermission(permission) || player.hasPermission("paytofly.speed.*")) {
                return true;
            }
            // 检查永久购买
            if (plugin.getStorage().getPlayerSpeeds(player.getUniqueId()).contains(key)) {
                return true;
            }
            // 检查时间限制购买
            Long speedTime = plugin.getStorage().getPlayerSpeedTime(player.getUniqueId(), key);
            return speedTime != null && speedTime > System.currentTimeMillis();
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
        
        // 当前特效和速度
        if (plugin.getEffectManager() != null) {
            var currentEffect = plugin.getEffectManager().getPlayerEffect(player);
            String effectDisplayName = getEffectDisplayName(currentEffect.getName());
            text = text.replace("{current_effect}", "&d" + effectDisplayName);
        } else {
            text = text.replace("{current_effect}", "&7未知");
        }
        
        if (plugin.getSpeedManager() != null) {
            var currentSpeed = plugin.getSpeedManager().getPlayerSpeed(player);
            text = text.replace("{current_speed}", "&a" + currentSpeed.getDisplayName());
        } else {
            text = text.replace("{current_speed}", "&7未知");
        }
        
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
        boolean isRightClick = event.getClick().isRightClick();
        
        try {
            handleGUIClick(player, slot, currentGUI, isRightClick);
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
               cleanTitle.contains("速度商店") ||
               cleanTitle.contains("特效切换器") ||
               cleanTitle.contains("速度切换器") ||
               cleanTitle.contains("时间购买");
    }
    
    /**
     * 处理GUI点击
     */
    private void handleGUIClick(Player player, int slot, String currentGUI, boolean isRightClick) {
        if ("main".equals(currentGUI)) {
            handleMainGUIClick(player, slot, isRightClick);
        } else if ("effects".equals(currentGUI)) {
            handleEffectsGUIClick(player, slot, isRightClick);
        } else if ("speeds".equals(currentGUI)) {
            handleSpeedsGUIClick(player, slot, isRightClick);
        } else if ("effect-switcher".equals(currentGUI)) {
            handleEffectSwitcherGUIClick(player, slot);
        } else if ("speed-switcher".equals(currentGUI)) {
            handleSpeedSwitcherGUIClick(player, slot);
        } else if ("effect-time-purchase".equals(currentGUI)) {
            handleEffectTimePurchaseGUIClick(player, slot);
        } else if ("speed-time-purchase".equals(currentGUI)) {
            handleSpeedTimePurchaseGUIClick(player, slot);
        }
    }
    
    /**
     * 处理主界面点击
     */
    private void handleMainGUIClick(Player player, int slot, boolean isRightClick) {
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
                } else if ("player-info".equals(key)) {
                    // 个人信息点击处理
                    if (isRightClick) {
                        // 右键打开速度切换器
                        String rightCommand = guiConfig.getString(path + "right-command", "");
                        if (!rightCommand.isEmpty()) {
                            if (rightCommand.equals("gui:speed-switcher")) {
                                player.closeInventory();
                                openGUI(player, GUIType.SPEED_SWITCHER);
                            } else {
                                executeCommand(player, rightCommand);
                            }
                        }
                    } else {
                        // 左键打开特效切换器
                        String leftCommand = guiConfig.getString(path + "command", "");
                        if (!leftCommand.isEmpty()) {
                            if (leftCommand.equals("gui:effect-switcher")) {
                                player.closeInventory();
                                openGUI(player, GUIType.EFFECT_SWITCHER);
                            } else {
                                executeCommand(player, leftCommand);
                            }
                        }
                    }
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
    private void handleEffectsGUIClick(Player player, int slot, boolean isRightClick) {
        for (String key : guiConfig.getConfigurationSection("effect-items").getKeys(false)) {
            String path = "effect-items." + key + ".";
            if (guiConfig.getInt(path + "slot", -1) == slot) {
                if ("back".equals(key)) {
                    player.closeInventory();
                    openGUI(player, GUIType.MAIN);
                } else {
                    if (isRightClick) {
                        // 右键进入时间购买界面
                        String rightCommand = guiConfig.getString(path + "right-command", "");
                        if (!rightCommand.isEmpty() && rightCommand.startsWith("gui:effect-time:")) {
                            String effectName = rightCommand.substring("gui:effect-time:".length());
                            player.closeInventory();
                            openGUI(player, GUIType.EFFECT_TIME_PURCHASE, effectName);
                        }
                    } else {
                        // 左键永久购买
                        String command = guiConfig.getString(path + "command", "");
                        if (!command.isEmpty()) {
                            executeCommand(player, command);
                        }
                    }
                }
                break;
            }
        }
    }
    
    /**
     * 处理速度商店点击
     */
    private void handleSpeedsGUIClick(Player player, int slot, boolean isRightClick) {
        for (String key : guiConfig.getConfigurationSection("speed-items").getKeys(false)) {
            String path = "speed-items." + key + ".";
            if (guiConfig.getInt(path + "slot", -1) == slot) {
                if ("back".equals(key)) {
                    player.closeInventory();
                    openGUI(player, GUIType.MAIN);
                } else {
                    if (isRightClick) {
                        // 右键进入时间购买界面
                        String rightCommand = guiConfig.getString(path + "right-command", "");
                        if (!rightCommand.isEmpty() && rightCommand.startsWith("gui:speed-time:")) {
                            String speedName = rightCommand.substring("gui:speed-time:".length());
                            player.closeInventory();
                            openGUI(player, GUIType.SPEED_TIME_PURCHASE, speedName);
                        }
                    } else {
                        // 左键永久购买
                        String command = guiConfig.getString(path + "command", "");
                        if (!command.isEmpty()) {
                            executeCommand(player, command);
                        }
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
     * 处理特效切换器点击
     */
    private void handleEffectSwitcherGUIClick(Player player, int slot) {
        if (!guiConfig.contains("effect-switcher-items")) return;
        
        for (String key : guiConfig.getConfigurationSection("effect-switcher-items").getKeys(false)) {
            String path = "effect-switcher-items." + key + ".";
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
     * 处理速度切换器点击
     */
    private void handleSpeedSwitcherGUIClick(Player player, int slot) {
        if (!guiConfig.contains("speed-switcher-items")) return;
        
        for (String key : guiConfig.getConfigurationSection("speed-switcher-items").getKeys(false)) {
            String path = "speed-switcher-items." + key + ".";
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
     * 处理特效时间购买点击
     */
    private void handleEffectTimePurchaseGUIClick(Player player, int slot) {
        // 处理时间购买选项点击
        if (slot == 11) {
            // 1小时购买
            String effectName = extractEffectNameFromTitle(player.getOpenInventory().getTitle());
            if (effectName != null) {
                executeCommand(player, "fly effect buy " + effectName + " 1 hour");
            }
        } else if (slot == 13) {
            // 1天购买
            String effectName = extractEffectNameFromTitle(player.getOpenInventory().getTitle());
            if (effectName != null) {
                executeCommand(player, "fly effect buy " + effectName + " 1 day");
            }
        } else if (slot == 15) {
            // 1周购买
            String effectName = extractEffectNameFromTitle(player.getOpenInventory().getTitle());
            if (effectName != null) {
                executeCommand(player, "fly effect buy " + effectName + " 1 week");
            }
        } else if (slot == 40) {
            // 返回按钮
            player.closeInventory();
            openGUI(player, GUIType.EFFECTS);
        }
    }
    
    /**
     * 处理速度时间购买点击
     */
    private void handleSpeedTimePurchaseGUIClick(Player player, int slot) {
        // 处理时间购买选项点击
        if (slot == 11) {
            // 1小时购买
            String speedName = extractSpeedNameFromTitle(player.getOpenInventory().getTitle());
            if (speedName != null) {
                executeCommand(player, "fly speed buy " + speedName + " 1 hour");
            }
        } else if (slot == 13) {
            // 1天购买
            String speedName = extractSpeedNameFromTitle(player.getOpenInventory().getTitle());
            if (speedName != null) {
                executeCommand(player, "fly speed buy " + speedName + " 1 day");
            }
        } else if (slot == 15) {
            // 1周购买
            String speedName = extractSpeedNameFromTitle(player.getOpenInventory().getTitle());
            if (speedName != null) {
                executeCommand(player, "fly speed buy " + speedName + " 1 week");
            }
        } else if (slot == 40) {
            // 返回按钮
            player.closeInventory();
            openGUI(player, GUIType.SPEEDS);
        }
    }
    
    /**
     * 从GUI标题中提取特效名称
     */
    private String extractEffectNameFromTitle(String title) {
        // 从标题中提取特效名称，例如从"✨ 彩虹特效 - 时间购买"中提取"rainbow"
        String cleanTitle = ChatColor.stripColor(title);
        
        // 简单的映射，实际应用中可能需要更复杂的逻辑
        if (cleanTitle.contains("基础特效")) return "basic";
        if (cleanTitle.contains("彩虹特效")) return "rainbow";
        if (cleanTitle.contains("星星特效")) return "star";
        if (cleanTitle.contains("火焰特效")) return "fire";
        if (cleanTitle.contains("魔法特效")) return "magic";
        if (cleanTitle.contains("龙息特效")) return "dragon";
        
        return null;
    }
    
    /**
     * 从GUI标题中提取速度名称
     */
    private String extractSpeedNameFromTitle(String title) {
        // 从标题中提取速度名称
        String cleanTitle = ChatColor.stripColor(title);
        
        if (cleanTitle.contains("缓慢速度")) return "slow";
        if (cleanTitle.contains("普通速度")) return "normal";
        if (cleanTitle.contains("快速")) return "fast";
        if (cleanTitle.contains("极速")) return "very_fast";
        if (cleanTitle.contains("超速")) return "super_fast";
        if (cleanTitle.contains("光速")) return "light_speed";
        if (cleanTitle.contains("曲速")) return "warp_speed";
        
        return null;
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