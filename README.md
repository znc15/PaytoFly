# PayToFly

一个简单而强大的 Minecraft 飞行权限插件，支持按时间购买飞行权限。

## 功能特点

- 💰 支持使用游戏币购买飞行权限
- ⏱️ 灵活的时间计算系统
  - 支持现实时间计时
  - 支持游戏在线时间计时
- 📊 直观的 GUI 商店界面
- 🌍 多语言支持
  - 简体中文
  - English
- 💾 数据持久化存储
- ⚡ 性能优化，低资源占用
- 🔔 到期提醒系统
- 📝 PlaceholderAPI 支持
- 🌐 世界权限控制
  - 支持设置免费飞行世界
  - 支持设置禁止飞行世界
- 🔑 权限系统
  - 支持无限飞行权限
  - 支持在特定世界禁用无限飞行

## 指令

- `/fly` - 打开飞行商店
- `/fly time` - 查看剩余飞行时间
- `/fly help` - 显示帮助信息
- `/fly reload` - 重载插件配置 (需要权限)
- `/fly [时间]` - 购买飞行时间
- `/fly bypass [用户名]` - 绕过创造模式飞行限制
- `/fly disable [用户名]` - 关闭用户的飞行权限

## 权限

- `paytofly.use` - 允许使用基础功能
- `paytofly.infinite` - 无限飞行权限
- `paytofly.admin` - 管理员权限
- `paytofly.creative.bypass` - 绕过创造模式飞行限制的权限

## PlaceholderAPI 变量

- `%paytofly_remaining%` - 显示剩余飞行时间
- `%paytofly_status%` - 显示飞行状态 (已开启/已关闭)
- `%paytofly_expiretime%` - 显示到期时间
- `%paytofly_mode%` - 显示当前计时模式 (现实时间/游戏时间)

## 安装

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

## 未来规划

- [ ] 添加更多购买选项
- [ ] 支持多种经济系统
- [ ] 飞行特效系统
- [ ] 飞行速度控制
- [ ] Web 管理界面
- [ ] API 支持
- [ ] 统计系统
- [ ] 更多语言支持
- [ ] 更多 PAPI 变量支持

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
