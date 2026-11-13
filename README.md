# Aerial TV N1

> **AI 构建声明：** 本项目由 AI 协助完成代码修改、界面设计、测试和文档。虽然已在真实斐讯 N1 上验证，使用前仍应自行审计源码、备份数据并保留恢复通道。

把斐讯 N1 变成一台干净、安静、可直接用手机接管的 Android TV 桌面。Aerial TV N1 基于开源项目 [Aerial](https://github.com/pengchujin/aerial-tv) 的 N1 适配分支，面向 Android 7.1.2 / API 25 的 `p230` 设备，重点解决旧系统兼容、1080p TV 布局、应用图标、远控二维码和桌面稳定性。

这不是上游作者的官方发行版，也不依赖上游安装脚本或预设配置。本仓库可以独立构建、安装和维护。

当前配套手机控制端：[N1 Pocket Remote 0.1.1 APK](https://github.com/jimmieyangcsjs/N1-Pocket-Remote/blob/main/dist/N1Remote-v0.1.1.apk)。两个项目可以独立升级，但远程二维码联动要求使用同一套签名。

## 当前形态

实测设备：

- 斐讯 N1 `p230`
- Android 7.1.2 / API 25
- 1920×1080 / 240 dpi
- 已开放无线 ADB
- 不需要 Play 服务

当前桌面仍使用包名 `app.orchard.tv`，这样覆盖安装时可以保留布局、壁纸、桌面选择和已有授权。应用名称、关于页、更新地址和维护文档均以本 N1 分支为准。

## 功能

- tvOS 风格的大屏桌面，常用应用放在底部玻璃 Dock，其余应用在下方网格。
- 首次启动和从其他应用按主页键都会回到 Aerial TV N1。
- 支持应用排序、文件夹、隐藏、长按菜单、应用启动和信号源入口。
- 为本机 App 绘制 16:9 电视磁贴，覆盖 AVBox、设置、文件管理器、N1 Remote 等非电视版图标。
- 图标按矢量或高分辨率方式生成，1080p 下清晰，在 4K 界面路径中也不会依赖低清手机图标放大。
- 内置 Aerial 航拍、照片、时钟和本地视频等屏保能力。
- 首页和屏保页可以显示 N1 Pocket Remote 的登录二维码。
- 首页二维码缩为 `56dp`，信息只用轻量两行文字排布；进入应用网格或滚动离开 Dock 后自动隐藏，不覆盖应用图标。
- 屏保二维码缩为 `132dp`，取消大块深色外框，只保留高对比码面和一条半透明信息胶囊。
- 二维码只在遥控服务在线、PIN 有效且服务地址与电视当前局域网地址一致时显示；断网、IP 变化或服务重启后自动隐藏并在恢复时重新出现。
- 远端控制不可用时不会用“等待控制服务”永久占据桌面。

## N1 Remote 联动

Aerial TV N1 与 [N1 Pocket Remote](https://github.com/jimmieyangcsjs/N1-Pocket-Remote) 组成完整的无遥控器方案：

1. N1 Pocket Remote 以常驻前台服务运行，生成动态 PIN 和二维码。
2. Aerial TV N1 通过受同签名权限保护的 `ContentProvider` 读取一次状态，不开放匿名 PIN 接口。
3. 手机扫码后直接进入中文 Web/PWA 控制台，可按键、触控、输入文字、启动或卸载应用、上传 APK 和截图。
4. 服务或网络中断后，两边都会各自恢复，不要求重启桌面。

两个 APK 必须使用同一套签名，首页二维码才能读取遥控状态。

当前验证组合为 Aerial TV N1 `1.99 (142)` 与 N1 Pocket Remote `0.1.1 (2)`。N1 Remote 的实时镜像、竖屏/横屏控制器和网络恢复可与桌面独立运行。

## 从 CleanYourTV N1 迁移

[CleanYourTV-N1](https://github.com/jimmieyangcsjs/CleanYourTV-N1) 保留为可回退版本，不再是本机推荐桌面。

迁移到 Aerial TV N1 时：

1. 先安装并验证 Aerial TV N1 能启动。
2. 将 Aerial TV N1 设为默认 Home，并按一次主页键确认。
3. 再卸载 CleanYourTV N1 的包 `com.gathin.tvhome`。
4. 删除 `/sdcard/Android/data/com.gathin.tvhome` 残留。
5. 不要先卸载旧桌面再安装新桌面，避免设备短暂没有可用 Home。

CleanYourTV N1 和 Aerial TV N1 使用不同包名，可以不覆盖安装；但同一时间只应把其中一个设为默认桌面。

## 构建

需要 JDK 17 和 Android SDK 36：

```powershell
$env:JAVA_HOME="C:\path\to\jdk17"
$env:ANDROID_HOME="C:\path\to\android-sdk"
$env:GRADLE_USER_HOME="C:\path\to\gradle-home"

.\gradlew.bat testStandardDebugUnitTest testGuardDebugUnitTest assembleStandardRelease
```

APK 输出：

```text
app\build\outputs\apk\standard\release\app-standard-release.apk
```

Android 9 及以上可以同时安装 APK 对应的 `.dm`；N1 的 Android 7 不支持该安装方式，使用普通覆盖安装，并在桌面首次启动后执行 speed 编译。

## 安装到 N1

```powershell
adb connect <N1-IP>:5555
adb install -r app\build\outputs\apk\standard\release\app-standard-release.apk
adb shell am start -n app.orchard.tv/.MainActivity
adb shell cmd package compile -m speed -f app.orchard.tv
```

设为默认桌面：

```powershell
adb shell cmd package set-home-activity app.orchard.tv/.MainActivity
adb shell input keyevent 3
adb shell dumpsys window windows | Select-String "mCurrentFocus"
```

最后一行应包含 `app.orchard.tv/app.orchard.tv.MainActivity`。

## 设计与兼容

- 桌面按项目的 960×540 逻辑画布排版，由系统缩放到 1080p 或 4K 界面。
- N1 当前输出为 1080p60；项目不会为了“看起来像 4K”而强制高成本绘制。
- 1024 宽度等常见 TV 安全区、1080p 和 4K UI 路径均保留。
- Release APK 必须经过 `compile -m speed`，debug 构建在 N1 上会明显变慢。
- 桌面控件保持不可聚焦，唤醒屏保的按键仍然用于退出屏保。
- 无网启动、后联网、中途断网和 IP 变化都由状态轮询与地址一致性检查处理。

## 目录

```text
app/                       完整版 Android TV 桌面
lite/                      Android 4.0–4.4 使用的 Aerial Lite
docs/                      架构、设备说明和 N1 设计记录
site/                      安装/升级页面资源
tools/                     构建、运营和诊断脚本
```

重点文档：

- [N1 设计记录](docs/superpowers/specs/2026-11-13-aerial-tv-n1-design.md)
- [架构说明](docs/ARCHITECTURE.md)
- [交接说明](docs/HANDOFF.md)
- [AI 使用与验证边界](AI-NOTICE.md)

## 性能与验证

本项目在 N1 上验证以下路径：

- Release 覆盖升级与默认 Home 设置。
- 主页键从其他应用回到 Aerial TV N1。
- 1080p60 桌面交互、屏保和 N1 Remote 二维码。
- 无网启动、后联网、IP 变化和服务恢复。
- 普通应用图标与 AVBox、设置、N1 Remote 等重绘图标。
- 1080p 截图中的二维码识别，以及缩放、压缩和低对比度压力测试。
- 首页二维码在 Dock 可见时显示，进入网格后立即退出，避免遮挡应用。
- 屏保二维码的缩小版布局、三秒延迟、服务离线隐藏和恢复。

测试结果会记录在设计文档和提交说明中；没有在真机上复测的路径不会标记为已验证。

变更摘要见 [CHANGELOG.md](CHANGELOG.md)。

## 安全

- Aerial TV N1 只适合可信家庭局域网。
- 不要把 N1 Remote、ADB 或电视控制端口直接暴露到公网。
- 需要远程访问时使用 Tailscale、WireGuard 等私有网络。
- 项目不会下载、安装或推荐第三方 App；具体软件由设备所有者决定。

## 来源与许可

- 上游项目：[pengchujin/aerial-tv](https://github.com/pengchujin/aerial-tv)
- 本 N1 分支：[jimmieyangcsjs/Aerial-TV-N1](https://github.com/jimmieyangcsjs/Aerial-TV-N1)
- 上游许可证和版权声明按 [LICENSE](LICENSE) 保留。
- 本仓库的修改同样按 MIT License 分发。

Aerial TV N1 与 Apple、斐讯、小米、TCL 等公司没有关联。航拍影片、第三方应用图标和字体的权利归各自作者，详见 [NOTICE.md](NOTICE.md) 或仓库内对应许可文件。
