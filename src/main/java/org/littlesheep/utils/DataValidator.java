package org.littlesheep.utils;

import java.util.regex.Pattern;
import java.util.concurrent.TimeUnit;
import java.util.UUID;

/**
 * 数据验证工具类
 * 提供安全的输入验证和数据转换功能
 */
public class DataValidator {
    
    // 时间格式正则表达式
    private static final Pattern TIME_PATTERN = Pattern.compile("^\\d{1,6}[mhdw]$|^\\d{1,3}mo$");
    private static final Pattern POSITIVE_INTEGER_PATTERN = Pattern.compile("^[1-9]\\d{0,8}$");
    
    // 时间限制常量（毫秒）
    private static final long MAX_TIME_MILLISECONDS = TimeUnit.DAYS.toMillis(365 * 10); // 最大10年
    private static final long MIN_TIME_MILLISECONDS = TimeUnit.MINUTES.toMillis(1); // 最小1分钟
    
    // 价格限制
    private static final double MAX_PRICE = 1_000_000.0; // 最大价格
    private static final double MIN_PRICE = 0.01; // 最小价格
    
    /**
     * 验证时间格式字符串
     * @param timeStr 时间字符串 (例如: "30m", "5h", "2d", "1w", "3mo")
     * @return 验证结果
     */
    public static ValidationResult<TimeData> validateTimeFormat(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return ValidationResult.error("时间格式不能为空");
        }
        
        String trimmed = timeStr.trim().toLowerCase();
        
        // 检查格式
        if (!TIME_PATTERN.matcher(trimmed).matches()) {
            return ValidationResult.error("时间格式无效，支持格式：数字+单位(m/h/d/w/mo)，例如：30m, 5h, 2d");
        }
        
        // 检查长度限制
        if (trimmed.length() > 10) {
            return ValidationResult.error("时间格式过长");
        }
        
        try {
            TimeData timeData = parseTimeString(trimmed);
            
            // 检查数值范围
            if (timeData.amount <= 0) {
                return ValidationResult.error("时间数量必须大于0");
            }
            
            // 检查时间范围
            if (timeData.milliseconds > MAX_TIME_MILLISECONDS) {
                return ValidationResult.error("时间过长，最大支持10年");
            }
            
            if (timeData.milliseconds < MIN_TIME_MILLISECONDS) {
                return ValidationResult.error("时间过短，最小1分钟");
            }
            
            return ValidationResult.success(timeData);
            
        } catch (NumberFormatException e) {
            return ValidationResult.error("时间数值无效：" + e.getMessage());
        } catch (ArithmeticException e) {
            return ValidationResult.error("时间计算溢出：" + e.getMessage());
        }
    }
    
    /**
     * 解析时间字符串
     */
    private static TimeData parseTimeString(String timeStr) throws NumberFormatException, ArithmeticException {
        String unit;
        int amount;
        
        if (timeStr.endsWith("mo")) {
            unit = "mo";
            amount = Integer.parseInt(timeStr.substring(0, timeStr.length() - 2));
        } else {
            unit = timeStr.substring(timeStr.length() - 1);
            amount = Integer.parseInt(timeStr.substring(0, timeStr.length() - 1));
        }
        
        long milliseconds = calculateMilliseconds(amount, unit);
        return new TimeData(amount, unit, milliseconds);
    }
    
    /**
     * 安全地计算毫秒数
     */
    private static long calculateMilliseconds(int amount, String unit) throws ArithmeticException {
        switch (unit) {
            case "m":
                return Math.multiplyExact(amount * 60L, 1000L);
            case "h":
                return Math.multiplyExact(amount * 3600L, 1000L);
            case "d":
                return Math.multiplyExact(amount * 86400L, 1000L);
            case "w":
                return Math.multiplyExact(amount * 604800L, 1000L);
            case "mo":
                return Math.multiplyExact(amount * 2592000L, 1000L); // 30天
            default:
                throw new IllegalArgumentException("不支持的时间单位: " + unit);
        }
    }
    
    /**
     * 验证价格
     */
    public static ValidationResult<Double> validatePrice(double price) {
        if (Double.isNaN(price) || Double.isInfinite(price)) {
            return ValidationResult.error("价格数值无效");
        }
        
        if (price < MIN_PRICE) {
            return ValidationResult.error("价格过低，最小" + MIN_PRICE);
        }
        
        if (price > MAX_PRICE) {
            return ValidationResult.error("价格过高，最大" + MAX_PRICE);
        }
        
        return ValidationResult.success(price);
    }
    
    /**
     * 验证正整数
     */
    public static ValidationResult<Integer> validatePositiveInteger(String input, int min, int max) {
        if (input == null || input.trim().isEmpty()) {
            return ValidationResult.error("数值不能为空");
        }
        
        String trimmed = input.trim();
        
        if (!POSITIVE_INTEGER_PATTERN.matcher(trimmed).matches()) {
            return ValidationResult.error("请输入有效的正整数");
        }
        
        try {
            int value = Integer.parseInt(trimmed);
            
            if (value < min) {
                return ValidationResult.error("数值过小，最小值为" + min);
            }
            
            if (value > max) {
                return ValidationResult.error("数值过大，最大值为" + max);
            }
            
            return ValidationResult.success(value);
            
        } catch (NumberFormatException e) {
            return ValidationResult.error("数值格式错误");
        }
    }
    
    /**
     * 验证UUID格式
     */
    public static ValidationResult<UUID> validateUUID(String uuidStr) {
        if (uuidStr == null || uuidStr.trim().isEmpty()) {
            return ValidationResult.error("UUID不能为空");
        }
        
        try {
            UUID uuid = UUID.fromString(uuidStr.trim());
            return ValidationResult.success(uuid);
        } catch (IllegalArgumentException e) {
            return ValidationResult.error("UUID格式无效");
        }
    }
    
    /**
     * 验证玩家名称
     */
    public static ValidationResult<String> validatePlayerName(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return ValidationResult.error("玩家名称不能为空");
        }
        
        String trimmed = playerName.trim();
        
        // Minecraft玩家名称规则：3-16个字符，只能包含字母数字和下划线
        if (trimmed.length() < 3 || trimmed.length() > 16) {
            return ValidationResult.error("玩家名称长度必须在3-16个字符之间");
        }
        
        if (!trimmed.matches("^[a-zA-Z0-9_]+$")) {
            return ValidationResult.error("玩家名称只能包含字母、数字和下划线");
        }
        
        return ValidationResult.success(trimmed);
    }
    
    /**
     * 检查时间是否已过期
     */
    public static boolean isExpired(long endTime) {
        return endTime <= System.currentTimeMillis();
    }
    
    /**
     * 安全地添加时间
     */
    public static ValidationResult<Long> addTimeToEndTime(long currentEndTime, long additionalTime) {
        try {
            long now = System.currentTimeMillis();
            long baseTime = Math.max(currentEndTime, now);
            long newEndTime = Math.addExact(baseTime, additionalTime);
            
            // 检查结果是否合理
            if (newEndTime > now + MAX_TIME_MILLISECONDS) {
                return ValidationResult.error("总飞行时间超过最大限制（10年）");
            }
            
            return ValidationResult.success(newEndTime);
            
        } catch (ArithmeticException e) {
            return ValidationResult.error("时间计算溢出");
        }
    }
    
    /**
     * 时间数据类
     */
    public static class TimeData {
        public final int amount;
        public final String unit;
        public final long milliseconds;
        
        public TimeData(int amount, String unit, long milliseconds) {
            this.amount = amount;
            this.unit = unit;
            this.milliseconds = milliseconds;
        }
        
        @Override
        public String toString() {
            return String.format("TimeData{amount=%d, unit='%s', milliseconds=%d}", 
                amount, unit, milliseconds);
        }
    }
    
    /**
     * 验证结果类
     */
    public static class ValidationResult<T> {
        private final boolean success;
        private final T data;
        private final String errorMessage;
        
        private ValidationResult(boolean success, T data, String errorMessage) {
            this.success = success;
            this.data = data;
            this.errorMessage = errorMessage;
        }
        
        public static <T> ValidationResult<T> success(T data) {
            return new ValidationResult<>(true, data, null);
        }
        
        public static <T> ValidationResult<T> error(String message) {
            return new ValidationResult<>(false, null, message);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public T getData() {
            return data;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public T getDataOrThrow() throws ValidationException {
            if (!success) {
                throw new ValidationException(errorMessage);
            }
            return data;
        }
    }
    
    /**
     * 验证异常
     */
    public static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }
}