# OTA 降级工具（Downgrader）

基于 [Jetpack Compose](https://developer.android.com/compose) 的 Android 系统降级包查询客户端，同时支持 **vivo / iQOO** 与 **OPPO / 一加 / 真我**（欧加真）两条通道。

## 功能特性

- **机型自动识别** — 读取系统属性判断机型归属，vivo / iQOO 走 vivo 通道，OPPO / 一加 / 真我走欧加真通道；识别结果与判断依据在界面上直接可见，识别不对可手动指定
- **降级凭据读取（vivo 通道）** — 读取本机 `sys.vivo.dota_info` 系列凭据，判断是否具备查询条件
- **官方升级服务查询（vivo 通道）** — 使用降级凭据请求 `sysupgrade.vivo.com.cn`，查询可用目标版本
- **官方 OTA 组件查询（欧加真通道）** — 请求厂商 OTA 组件服务，获取目标版本链
- **售后降级包查询（欧加真通道）** — 请求官方降级服务，获取可用的售后降级包及直链
- **下载与校验** — 应用内后台下载、进度显示、取消与重试；按服务端提供的大小与 SHA-256 校验后保存
- **dota_sig 管理** — 支持复制与保存服务返回的 `dota_sig`
- **界面上** — 液态玻璃视觉，边框与背景随时间流动变色，列表入场 / 滚动 / 加载均有动效

## 使用说明

- 查询结果来自厂商官方公开接口，本工具不托管、不分发任何固件文件，仅提供查询与直链下载
- vivo 通道的查询取决于设备是否提供降级凭据；未检测到相关凭据时应用会提示无法查询
- 跨 Android 大版本回刷会触发防回滚（ARB）。刷机前请确认目标版本的 ARB 等级不高于当前设备，否则可能无法开机

## 编译

环境要求：

- JDK 21
- Android SDK（platform 37、build-tools 37.0.0）
- Gradle 9.x（或使用仓库内的 wrapper）

```bash
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleRelease
```

发布版签名通过环境变量注入，未配置时使用调试签名：

```bash
export KEYSTORE_PATH=/path/to/your.jks
export KEYSTORE_PASSWORD=******
export KEY_ALIAS=your_alias
export KEY_PASSWORD=******
```

构建完成后可对产物做 4 字节对齐并签名：

```bash
$ANDROID_HOME/build-tools/37.0.0/zipalign -f -p 4 app-release-unsigned.apk aligned.apk
$ANDROID_HOME/build-tools/37.0.0/apksigner sign --ks your.jks --v1-signing-enabled false \
  --v2-signing-enabled true --v3-signing-enabled true --out signed.apk aligned.apk
```

## 许可证

本项目采用 GNU General Public License v3.0 发布，详情请参阅 [LICENSE](LICENSE)。

## 致谢

- [Miuix](https://github.com/compose-miuix-ui/miuix)
- [Payload Extract GUI](https://github.com/YuKongA/payload_extract_gui)
- 原项目：[Chumor/Downgrader](https://github.com/Chumor/Downgrader)
