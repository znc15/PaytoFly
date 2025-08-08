# PayToFly

一个功能完整的 Minecraft 飞行权限插件，支持飞行时间购买、华丽特效系统和多等级速度控制。

## 功能特点

- 💰 支持使用游戏币购买飞行权限
- ⏱️ 灵活的时间计算系统（支持永久和时间限制购买）
- 📊 直观的 GUI 商店界面（全新时间购买模式）
- 🌍 多语言支持
- 💾 数据持久化存储（支持时间限制数据）
- ⚡ 性能优化，低资源占用
- 🔔 到期提醒系统
- 📝 PlaceholderAPI 支持（新增特效和速度变量）
- 🌐 世界权限控制
- 🔑 权限系统
- 🎨 飞行特效系统（7种特效等级，支持时间购买）
- 🚀 飞行速度控制（7个速度等级，支持时间购买）
- 🎵 音效支持
- 🌊 平滑速度过渡
- 💸 特效和速度购买系统（永久+时间购买双模式）
- 🔒 权限和购买双重验证
- 🔄 智能特效和速度切换器
- 👤 详细的个人信息显示

## 指令

- `/fly` - 打开飞行商店
- `/fly time` - 查看剩余飞行时间
- `/fly help` - 显示帮助信息
- `/fly reload` - 重载插件配置 (需要权限)
- `/fly [时间]` - 购买飞行时间
- `/fly bypass [用户名]` - 绕过创造模式飞行限制
- `/fly disable [用户名]` - 关闭用户的飞行权限
- `/fly give <玩家名> <时间>` - 管理员直接给予玩家飞行权限
- `/fly test` - 测试Papi输出内容

### 飞行特效命令
- `/fly effect` - 查看当前特效信息
- `/fly effect list` - 显示可用特效列表
- `/fly effect set <特效名>` - 设置飞行特效
- `/fly effect buy <特效名>` - 永久购买飞行特效
- `/fly effect buy <特效名> <时间> <单位>` - 时间限制购买特效（如：1 hour, 3 day, 1 week）
- `/fly effect off` - 禁用飞行特效

### 飞行速度命令
- `/fly speed` - 查看当前速度信息
- `/fly speed list` - 显示可用速度列表
- `/fly speed set <速度名>` - 设置飞行速度
- `/fly speed buy <速度名>` - 永久购买飞行速度
- `/fly speed buy <速度名> <时间> <单位>` - 时间限制购买速度（如：2 hour, 5 day, 2 week）
- `/fly speed up/down` - 升级/降级速度
- `/fly speed reset` - 重置到默认速度

## 权限

- `paytofly.use` - 允许使用基础功能
- `paytofly.infinite` - 无限飞行权限
- `paytofly.admin` - 管理员权限
- `paytofly.creative.bypass` - 绕过创造模式飞行限制的权限

### 飞行特效权限
- `paytofly.effects.*` - 所有特效权限
- `paytofly.effects.<effect_name>` - 特定特效权限

### 飞行速度权限
- `paytofly.speed.*` - 所有速度权限
- `paytofly.speed.<speed_name>` - 特定速度权限
- `paytofly.speed.level.<level>` - 基于等级的权限
- `paytofly.speed.max.<level>` - 最大允许等级

## PlaceholderAPI 变量

### 基础飞行变量
- `%paytofly_remaining%` - 显示剩余飞行时间
- `%paytofly_status%` - 显示飞行状态 (已启用/已禁用)
- `%paytofly_expiretime%` - 显示到期时间
- `%paytofly_mode%` - 显示当前计时模式 (真实时间/游戏时间)
- `%paytofly_flight_active%` - 玩家是否正在飞行 (是/否)
- `%paytofly_allow_flight%` - 玩家是否允许飞行 (是/否)

### 特效相关变量
- `%paytofly_effect%` - 当前特效显示名称
- `%paytofly_effect_name%` - 当前特效内部名称
- `%paytofly_effect_level%` - 当前特效等级
- `%paytofly_has_effect_<特效名>%` - 是否拥有指定特效 (是/否)

### 速度相关变量
- `%paytofly_speed%` - 当前速度显示名称
- `%paytofly_speed_name%` - 当前速度内部名称
- `%paytofly_speed_level%` - 当前速度等级
- `%paytofly_speed_value%` - 当前速度数值
- `%paytofly_has_speed_<速度名>%` - 是否拥有指定速度 (是/否)

### 使用示例
```
玩家信息:
- 飞行状态: %paytofly_status%
- 剩余时间: %paytofly_remaining%
- 当前特效: %paytofly_effect%
- 当前速度: %paytofly_speed%
- 拥有彩虹特效: %paytofly_has_effect_rainbow%
- 拥有超速: %paytofly_has_speed_super_fast%
```

## 快速开始

### 🚀 基础安装
1. 下载最新版本的插件
2. 将插件放入服务器的 `plugins` 文件夹
3. 安装 Vault 和经济插件（如 EssentialsX）
4. 重启服务器
5. 编辑 `config.yml` 配置文件

### ⚡ 5分钟配置
```yaml
# 1. 设置语言（可选）
language: zh_CN  # 或 en_US

# 2. 配置经济系统
fly-cost:
  minute: 10.0   # 每分钟10金币
  hour: 100.0    # 每小时100金币

# 3. 启用特效和速度购买（永久+时间购买）
flight-effects:
  purchase:
    enabled: true
  time-purchase:
    enabled: true
    hour-multiplier: 0.1    # 每小时价格倍数
    day-multiplier: 2.0     # 每天价格倍数
    week-multiplier: 12.0   # 每周价格倍数

flight-speed:
  purchase:
    enabled: true
  time-purchase:
    enabled: true
    hour-multiplier: 0.1
    day-multiplier: 2.0
    week-multiplier: 12.0
```

### 🎮 立即体验
1. 玩家输入 `/fly` 打开飞行商店购买飞行时间
2. 点击个人信息查看当前特效和速度
3. 输入 `/fly effect buy rainbow` 永久购买彩虹特效
4. 输入 `/fly effect buy dragon 3 day` 购买3天龙息特效
5. 输入 `/fly speed buy fast` 永久购买快速飞行
6. 输入 `/fly speed buy light_speed 1 week` 购买1周光速
7. 在GUI中右键点击特效/速度进行时间购买
8. 开始飞行享受华丽特效！

## 详细安装

1. 下载最新版本的插件
2. 将插件放入服务器的 `plugins` 文件夹
3. 重启服务器或重载插件
4. 编辑 `config.yml` 配置文件

## 依赖

- Vault (经济系统支持)
- PlaceholderAPI (可选，变量支持)

## 支持的版本
- 需要Java17及以上版本
- Minecraft 1.21.x（已测试）
- Minecraft 1.20.x（已测试）
- Minecraft 1.19.x（未测试）
- Minecraft 1.18.x（未测试）
- Minecraft 1.17.x（未测试）
- Minecraft 1.16.x（未测试）

## 飞行特效系统

支持7种特效等级，从简单到华丽：
- **无特效** - 不显示任何特效
- **基础特效** - 简单的白色粒子
- **彩虹特效** - 彩色尾迹效果
- **星星特效** - 闪烁的星星粒子
- **火焰特效** - 火花和烟雾效果
- **魔法特效** - 魔法粒子和符文
- **龙息特效** - 高级龙息效果

## 飞行速度控制

支持7个速度等级，满足不同需求：
- **缓慢** (0.05) - 适合精细操作
- **普通** (0.1) - 默认飞行速度
- **快速** (0.2) - 提升移动效率
- **极速** (0.3) - 高速飞行
- **超速** (0.5) - 极快移动
- **光速** (0.8) - 接近光速
- **曲速** (1.0) - 最快速度

## 购买系统

### 💸 特效购买价格（永久）
- **基础特效** - 100金币
- **彩虹特效** - 500金币  
- **星星特效** - 800金币
- **火焰特效** - 1200金币
- **魔法特效** - 2000金币
- **龙息特效** - 5000金币

### 🚀 速度购买价格（永久）
- **缓慢速度** - 50金币
- **普通速度** - 免费
- **快速速度** - 200金币
- **极速** - 600金币
- **超速** - 1500金币
- **光速** - 3000金币
- **曲速** - 8000金币

### ⏰ 时间购买模式
除了永久购买，现在还支持时间限制购买：

#### 价格计算公式
- **每小时** = 永久价格 × 0.1（如彩虹特效1小时 = 500 × 0.1 = 50金币）
- **每天** = 永久价格 × 2.0（如彩虹特效1天 = 500 × 2.0 = 1000金币）
- **每周** = 永久价格 × 12.0（如彩虹特效1周 = 500 × 12.0 = 6000金币）

#### 使用方式
1. **命令购买**：`/fly effect buy rainbow 3 day`（购买3天彩虹特效）
2. **GUI购买**：右键点击特效/速度物品进入时间购买界面
3. **时间叠加**：多次购买时间会自动叠加

### 🔒 获取方式总结
玩家可以通过以下方式获得特效和速度：

1. **权限方式**（管理员设置）
   - 拥有对应权限可直接使用
   - 管理员可设置 `paytofly.effects.*` 获得所有特效
   - 管理员可设置 `paytofly.speed.*` 获得所有速度

2. **永久购买**（玩家自购）
   - 使用游戏币一次性购买，永久拥有
   - 购买后无需权限即可使用
   - 支持所有经济系统（Vault、MHDF-Tools）

3. **时间购买**（玩家自购）
   - 按需购买指定时长
   - 价格更低，适合体验或短期使用
   - 时间到期后自动失效

4. **混合使用**
   - 权限优先，其次永久购买，最后时间购买
   - 系统会自动选择最高等级的可用特效/速度
   - 所有购买记录永久保存

## 配置说明

### 价格配置
在 `config.yml` 中可以自定义所有价格：

```yaml
# 飞行特效价格设置
flight-effects:
  # 永久购买设置
  purchase:
    enabled: true
    prices:
      basic: 100.0
      rainbow: 500.0
      star: 800.0
      fire: 1200.0
      magic: 2000.0
      dragon: 5000.0
  
  # 时间购买设置
  time-purchase:
    enabled: true
    hour-multiplier: 0.1    # 每小时价格倍数
    day-multiplier: 2.0     # 每天价格倍数
    week-multiplier: 12.0   # 每周价格倍数

# 飞行速度价格设置
flight-speed:
  # 永久购买设置
  purchase:
    enabled: true
    prices:
      slow: 50.0
      normal: 0.0        # 免费
      fast: 200.0
      very_fast: 600.0
      super_fast: 1500.0
      light_speed: 3000.0
      warp_speed: 8000.0
  
  # 时间购买设置
  time-purchase:
    enabled: true
    hour-multiplier: 0.1
    day-multiplier: 2.0
    week-multiplier: 12.0
```

### 存储系统
支持三种存储方式，永久和时间购买记录都会保存：
- **JSON存储** - 轻量级文件存储（支持时间限制数据）
- **SQLite存储** - 本地数据库存储（支持时间限制数据）
- **MySQL存储** - 远程数据库存储（支持时间限制数据）

存储内容包括：
- 永久购买的特效和速度记录
- 时间限制购买的特效和速度记录（包含到期时间）
- 自动清理过期的时间限制记录

## 使用指南

### 基础使用
1. 玩家输入 `/fly` 打开飞行商店购买飞行时间
2. 点击个人信息查看当前特效和速度状态
3. 使用 `/fly effect list` 查看可用特效
4. 使用 `/fly speed list` 查看可用速度
5. 通过购买或权限获得特效和速度

### GUI操作
1. **主界面**：显示飞行时间购买和个人信息
2. **个人信息**：
   - 左键点击：打开特效切换器
   - 右键点击：打开速度切换器
3. **特效商店**：
   - 左键点击：永久购买特效
   - 右键点击：时间购买特效
4. **速度商店**：
   - 左键点击：永久购买速度
   - 右键点击：时间购买速度
5. **切换器**：快速切换已拥有的特效/速度

### 购买流程

#### 永久购买
1. 检查余额：`/fly effect buy rainbow`
2. 自动扣款并记录购买
3. 立即可用，永久拥有
4. 设置使用：`/fly effect set rainbow`

#### 时间购买
1. 命令购买：`/fly effect buy dragon 3 day`
2. GUI购买：右键点击特效物品
3. 选择时长：1小时/1天/1周或自定义
4. 自动扣款，按时间计费
5. 到期自动失效

### 切换使用
1. 使用命令：`/fly effect set <特效名>`
2. 使用GUI：点击个人信息 → 选择特效/速度
3. 系统自动选择最高等级可用项

### 管理员操作
- 给予权限：直接设置对应权限节点
- 调整价格：修改 `config.yml` 配置文件
- 时间倍数：调整 `time-purchase` 配置
- 查看统计：使用 `/fly stats` 命令

## 特色功能

### 🎨 视觉体验
- **7种特效等级**：从简单粒子到华丽龙息
- **动态音效**：特效启动和停止音效
- **平滑过渡**：无缝的速度变化体验
- **性能优化**：智能调度，静止时减少特效

### 💰 经济系统
- **双重获取方式**：权限或购买均可获得
- **灵活定价**：管理员可自定义所有价格
- **防重复购买**：智能检查已拥有状态
- **经济兼容**：支持Vault和MHDF-Tools

### 💾 数据管理
- **多存储支持**：JSON/SQLite/MySQL三种选择
- **向后兼容**：自动升级旧数据格式
- **数据持久化**：购买记录永久保存
- **性能优化**：批量写入，连接池管理

### 🔧 管理功能
- **权限细分**：精细的权限控制系统
- **统计监控**：系统性能和使用统计
- **多语言**：中英文双语支持
- **热重载**：配置修改即时生效

## 常见问题

### Q: 如何给玩家永久的飞行特效？
A: 管理员可以设置权限 `paytofly.effects.*` 或让玩家购买对应特效。

### Q: 玩家购买后能否退款？
A: 目前不支持退款功能，购买前请确认。管理员可手动删除购买记录。

### Q: 如何调整特效和速度的价格？
A: 修改 `config.yml` 中的 `flight-effects.purchase.prices` 和 `flight-speed.purchase.prices` 配置。

### Q: 支持哪些经济插件？
A: 支持所有兼容Vault的经济插件，以及MHDF-Tools内置经济。

### Q: 数据会丢失吗？
A: 不会，购买记录保存在持久化存储中，支持备份和恢复。

## 未来规划

- [x] 飞行特效系统 ✅
- [x] 飞行速度控制 ✅
- [x] 特效和速度购买系统 ✅
- [x] 时间限制购买功能 ✅
- [x] GUI商店界面优化 ✅
- [x] 个人信息显示 ✅
- [x] 特效和速度切换器 ✅
- [x] PlaceholderAPI扩展 ✅
- [ ] API接口开放
- [ ] 更多特效类型
- [ ] 速度预设方案
- [ ] 统计和排行榜
- [ ] 更多语言支持
- [ ] 时间购买GUI完善

## 问题反馈

如果您在使用过程中遇到任何问题，请通过以下方式反馈：
1. 在 GitHub Issues 中提交问题

## 开源协议

本项目采用 MIT 协议开源。

## 支持作者

如果您觉得这个插件对您有帮助，欢迎：
1. 给项目一个 Star ⭐
2. 向他人推荐本插件
3. 参与项目开发 
