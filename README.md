# OTA 降级工具

**Android OTA downgrade finder for vivo / iQOO & OPPO / OnePlus / realme** — auto device detection, official channel lookup, SHA-256 verified download. Kotlin + Jetpack Compose.

安卓 OTA 降级包查询工具，双通道支持 vivo / iQOO 与 OPPO / 一加 / 真我。
不用到处翻论坛帖子和网盘链接，打开就能看到本机有哪些官方降级包可下。

> 当前版本 v1.5.0 · 遵循 GPL-3.0 · 基于 [Chumor/Downgrader](https://github.com/Chumor/Downgrader) 二次开发

---

## 它做什么

- **自动识别机型** — 读一次系统属性，判断该走 vivo 还是欧加真通道，不用手动切；识别错了可以手动指定，也能随时交还自动
- **vivo / iQOO 通道** — 读取本机 `sys.vivo.dota_info` 系列降级凭据，向 vivo 官方升级服务查询可用降级包
- **欧加真通道** — 同时查询官方 OTA 组件版本与售后降级包，版本链一目了然
- **直链下载 + 完整性校验** — 每个包给出 SHA-256，下完自己核对，避免下到魔改包
- **dota_sig 管理** — 支持复制与保存服务返回的 `dota_sig`
- **防回滚提示** — 跨 Android 大版本回刷会触发防回滚（ARB）等级提升，界面上有明确提醒

## 为什么会有这个

官方给的降级入口要么藏在售后系统里，要么得靠论坛里的野链接。
这个工具把两条通道的查询合并到一个界面：凭据从本机读，请求直连官方服务，中间不经过任何第三方服务器。
不托管、不分发任何固件文件，只提供查询和官方直链。

## 界面

液态玻璃风格：卡片边缘是流动的液态玻璃，颜色沿边缘缓慢行走（蓝 → 紫 → 青 → 粉），背后透出随时间变色的光斑，列表入场 / 滚动 / 加载都有动效，跟随系统深浅色。

## 技术栈

| 层 | 用了什么 |
|---|---|
| 语言 | Kotlin |
| UI | Jetpack Compose · [Miuix](https://github.com/compose-miuix-ui/miuix) 设计组件 |
| 构建 | Gradle 9 · AGP · minSdk 26 / targetSdk 36 / compileSdk 37 |
| 架构 | 单 Activity + Compose，domain / data / presentation 分层 |

## 编译

需要 **JDK 21** 与 **Android SDK 37**（platform 37、build-tools 37.0.0）：

```bash
git clone https://github.com/<你的用户名>/OTA-Downgrader.git
cd OTA-Downgrader
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleRelease
```

产物在 `app/build/outputs/apk/release/`。

发布版签名通过环境变量注入（不配置则用调试签名）：

```bash
export KEYSTORE_PATH=/path/to/your.jks
export KEYSTORE_PASSWORD=******
export KEY_ALIAS=your_alias
export KEY_PASSWORD=******
```

## 安装

从 [Releases](../../releases) 下载 APK 直接安装。
`minSdk 26`，支持 Android 8.0 及以上；仅需读取系统属性和网络权限。

## 声明

本项目基于 [Chumor/Downgrader](https://github.com/Chumor/Downgrader) 二次开发，遵循 **GNU General Public License v3.0**，详见 [LICENSE](LICENSE)。

降级有风险。跨大版本回刷前请确认目标版本的 ARB 等级不高于当前设备，数据自行备份。

## 致谢

- [Chumor/Downgrader](https://github.com/Chumor/Downgrader) — 原项目
- [Miuix](https://github.com/compose-miuix-ui/miuix)
- [Payload Extract GUI](https://github.com/YuKongA/payload_extract_gui)
