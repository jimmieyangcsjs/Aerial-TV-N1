# Aerial TV N1 设计说明

## 目标

为斐讯 N1（`p230`、Android 7.1.2 / API 25、Mali-450 MP3、约 1.8 GB 内存）建立 Aerial TV 的非官方增强版。新项目保留 Aerial 的桌面守护、手机整理页、应用磁贴、屏保和控制中心，同时恢复原 N1 桌面版本已经验证的“无实体遥控器也能扫码控制”能力。

新仓库为 `Aerial-TV-N1`。`CleanYourTV-N1` 保持原样，用于回退和对比，不在本次改造中覆盖或归档。

## 非目标

- 不把 N1 Pocket Remote 打进 Aerial APK。
- 不在 Aerial 的安装脚本中下载、安装或推荐其他应用。
- 不修改 Aerial 的第三方应用边界、桌面守护策略或系统应用状态。
- 不承诺 4K UI。当前 N1 EDID 只提供 1920×1080@60。
- 不修改 N1 Remote 的按键、触控、应用管理、输入法或常驻服务实现。

## 集成架构

Aerial 作为独立 Home 应用运行，N1 Pocket Remote 继续作为独立前台服务运行。两者使用 N1 Remote 已有的同签名 `ContentProvider` 交换首页二维码所需信息：

```text
content://com.gathin.n1remote.status/status
```

字段为 `ip`、`port`、`pin`、`serviceOnline`、`url`。Aerial 只读该 Provider，不通过 Aerial 自己的手机页暴露 PIN，也不打开新的匿名网络端口。

两个 APK 必须使用同一证书签名。开发构建使用当前 N1 Remote 已安装包的调试证书；证书和密码保存在不提交的 `local/` 目录。

## 首页遥控卡

首页顶部中央增加一个不可聚焦的紧凑卡片，只有检测到同签名 Provider 时出现：

- 服务在线且地址有效：显示 N1 标志、`手机遥控 N1`、实际 `IP:端口`、动态 PIN 和自动登录二维码。
- 服务启动中：保留紧凑深色状态卡，显示 `控制服务启动中`，不显示二维码或明文 PIN。
- 网络未就绪或地址不完整：显示 `等待局域网`，不显示无效地址。
- Provider 不存在：完全隐藏，避免普通 Aerial 用户看到无意义占位。

卡片放在 Aerial 的 960×540 设计画布顶部中央，避开默认时钟和右上角状态胶囊，不改变应用首行焦点。Aerial 自带的 Control Center、整理页和屏保继续覆盖在 Home 之上。

## 网络与恢复

- Aerial 启动后立即读取一次状态。
- 服务不在线时每 1200 ms 重试；在线时每 10 秒刷新地址。
- 有线或 Wi-Fi 在启动后才连接、IP 改变、服务短暂重启时，卡片会自动更新，不需要重启 Aerial。
- HOME 页面重新进入前台时立即刷新。
- 查询失败、权限未授予或 Provider 升级期间按“未安装”处理，不抛异常。
- N1 Remote 自身继续负责开机启动、网络回调和 30 秒看门狗。

## 应用图标

使用 Aerial 的 `TileRecipes` 根据包名永久保持外观，不复制 APK，不依赖安装时间：

- `com.gathin.n1remote`：使用应用自身自适应图标，保留品牌色。
- `com.github.avbox.osc`：使用应用自身自适应图标，更新或重装后仍生效。
- `com.example.piliplus`：使用应用自身自适应图标。
- `com.android.tv.settings`：使用 Aerial 的 `Gear` 单色字形和暗色渐变，避免系统小图标在 16:9 磁贴中被拉伸。
- `com.oriontv`：保留当前表现，不覆盖已有优秀横幅。

所有磁贴保持 16:9，不通过强制拉伸改变 Logo 比例。

## 性能

- 不自动固定为 Lite；Aerial 现有 `Effects.Auto` 继续使用。
- 如果 N1 被识别为低端设备，先测真实帧表现，再决定是否在 N1 文档中建议切换效果；不通过隐藏配置悄悄降级。
- 遥控卡不做常驻动画、不创建额外全屏层，不在每一帧执行 Provider 查询。
- 以 1080p60 为验收目标，不测试或承诺 720p。
- 4K 只记录为当前硬件输出能力限制。

## 字体

Aerial 在 Android 7.1 上使用系统字体。N1 已全局安装霞鹜文楷 GB Screen，因此 Aerial 中文界面应直接继承该系统字体；不在 APK 内重复打包 26 MB 字体。Aerial 自带 Inter 只用于支持可变字体的新系统。

## 测试与验收

### 自动化

- 状态解析拒绝非法 IP、端口、PIN 和不匹配 URL。
- 在线、等待、Provider 缺失三种状态产生正确刷新间隔。
- 包名磁贴规则覆盖 N1 Remote、AVBox、PiliPlus 和设置。
- 现有 Aerial 单元测试继续通过。

### N1 真机

- release APK 可以安装、启动并设为默认 Home。
- 冷启动后第一行磁贴获得焦点，遥控卡不参与方向键导航。
- N1 Remote 在线时二维码可直接登录手机控制台。
- 停止后重启 N1 Remote 服务，二维码自动恢复。
- 重启 N1 后 Aerial 和 N1 Remote 都自动恢复。
- 断开网络时显示等待状态；网络恢复后显示新的实际 IP。
- 1080p60 页面导航没有持续卡顿或焦点错位。
- AVBox、PiliPlus、OrionTV、设置和 N1 Remote 图标不变形。

## 文档和许可

- 保留 Aerial 原始 MIT `LICENSE`、版权和上游提交。
- 新增简体中文 N1 说明、AI 构建声明、安装/升级/还原说明。
- 上游英文 README 保存在 `README.upstream.md`。
- 明确这是非官方衍生版，不代表上游作者。
- APK 分发目录包含 `AI-NOTICE.txt`。

