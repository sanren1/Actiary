# Actiary

> 离线优先的个人运动记录应用 · Offline-first personal workout journal for Android

Actiary 是一款面向日常运动与力量训练的轻量级离线 Android 应用，采用 Jetpack Compose 与动态液态玻璃（Liquid Glass）视觉语言构建。无需注册账号、无云端追踪，所有运动记录、照片与偏好设置均完全保存在你的设备本地。

---

## 核心特性

- **日历视图与当日详情**
  - 月历打卡一览与可折叠吸顶日历，直观查看每日运动轨迹与照片缩略图。
  - 支持拍照/选图自动压缩（`ImageDecoder` JPEG 优化）及本地截图识别（ML Kit 离线 OCR）快速导入运动数据。
- **丰富的运动与力量分化类型**
  - 内置 12 种常用类型：跑步、力量训练、瑜伽、游泳、骑行、徒步，以及 **胸 / 背 / 肩 / 臂 / 腿 / 腹** 6 大部位肌理特写图标。
  - 支持自定义运动类型（名称、图标、拖拽排序与触觉反馈），并可按需隐藏或恢复内置类型。
- **记录与统计图表**
  - 按周 / 月 / 年切换查看运动频次、时长、距离与热量统计，支持多配色方案与平滑柱状图过渡。
- **个性化外观与液态玻璃效果**
  - 深色 / 浅色 / 跟随系统平滑过渡，支持 AMOLED 纯黑背景。
  - 柔雾 / 均衡 / 通透三档玻璃材质、动态壁纸取色（Material You）与自定义主题强调色。
  - 支持应用内直接切换简体中文 / English，并适配系统「减弱动态效果」。
- **桌面小组件与本地备份**
  - 响应式桌面小组件（`2×1` / `2×2` / `4×2`），桌面一键记录今日运动。
  - 支持完整 ZIP 本地导出/导入（合并或覆盖模式）、存储空间明细查看、一键清理缓存与孤立照片清理。

---

## 下载与运行环境

前往 [GitHub Releases](https://github.com/sanren1/Actiary/releases) 下载最新安装包：

- **系统要求**：Android 12（API 31）及以上
- **CPU 架构**：`arm64-v8a`（64 位 ARM 设备，安装包文件名带 `arm64-v8a` 后缀）

---

## 本地构建

```bash
# 克隆仓库
git clone https://github.com/sanren1/Actiary.git
cd Actiary

# 运行单元测试、Lint 检查与 Release APK 构建（需 JDK 17 与 Android SDK 35）
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
```

构建产物位于 `app/build/outputs/apk/release/`。

---

## 技术栈

- **语言与 UI**：Kotlin、Jetpack Compose、Material 3
- **数据存储**：Room（含 Schema 迁移测试）、DataStore Preferences
- **图形与动效**：AGSL RuntimeShader 液态玻璃折射/色散、自定义 Compose 动效与预测性返回
- **图片与识别**：Coil Compose、Android `ImageDecoder`、Google ML Kit 中文离线文本识别
- **桌面小组件**：Android AppWidgetProvider + RemoteViews

---

## 开源许可与第三方声明

本项目使用的第三方开源组件及许可声明详见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)，亦可在应用内 **设置 → 关于 → 开源许可** 中随时查看。
