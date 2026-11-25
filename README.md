# Actiary

> 离线优先的个人运动记录应用 · Offline-first personal workout journal for Android

Actiary 是一款面向日常运动与力量训练的轻量级 Android 应用，采用 Jetpack Compose 与动态液态玻璃（Liquid Glass）视觉语言构建。无需注册账号、无云端追踪，所有运动记录、照片与偏好设置均完全保存在你的设备本地（联网仅用于检查应用更新）。

---

## 核心特性

- **日历视图与当日详情**
  - 月历打卡一览与可折叠吸顶日历，首页顶部提供本周进度与连续打卡天数微胶囊，直观查看每日运动记录、肌群动作摘要与照片缩略图。
  - 支持拍照/选图自动压缩（`ImageDecoder` JPEG 优化）、系统「分享」接收运动截图自动离线 OCR 识别，以及分享/打开标准 `.gpx` / `.tcx` 运动轨迹文件精确导入数据。
- **灵活的运动类型与结构化力量训练**
  - 内置 6 种常用类型（跑步、骑行、游泳、徒步、球类、自由训练），支持在设置页按需隐藏或恢复，以及自定义运动类型（名称、图标、拖拽排序与触觉反馈）。
  - 选择「自由训练」时提供**自由分化训练计划模板**（支持一键多选追加/点击取消、循环推荐与一键清空）、**118 个中英文内置动作库**（覆盖器械与常见徒手动作，支持按次 / 按秒计时与自定义动作）、**动作 + 组数×次数 + 可选重量**单手步进微调、动作调序与点击「上次成绩」一键沿用。
  - 原生 Compose **正反面 14 块肌群人体仿真矢量图**，支持平滑色彩过渡与立体高光，录入与详情页实时高亮主/辅肌群（点击人体部位直达对应肌群动作筛选），支持点击动作查看历史训练记录与最佳成绩。
- **记录与统计图表**
  - 按周 / 月 / 年切换查看运动频次、时长、距离与热量统计，支持多配色方案与平滑柱状图过渡；运动类型筛选弹窗仅展示当前统计周期内进行过的类型。
  - 当统计周期与筛选条件下包含肌群训练记录时，自动展开**肌群分布热力图与各肌群统计排行**（含锻炼次数、总组数与距上次训练恢复天数，点击肌群可二次筛选日期列表）。
- **个性化外观与液态玻璃效果**
  - 深色 / 浅色 / 跟随系统平滑色彩过渡，支持 AMOLED 纯黑背景。
  - 柔雾 / 均衡 / 通透三档玻璃材质、动态壁纸取色（Material You）与自定义主题强调色。
  - 支持应用内直接切换简体中文 / English，并适配系统「减弱动态效果」与 Android 自适应矢量图标（含 Material You 单色主题层）。
- **桌面小组件、本地备份与应用更新**
  - 响应式桌面小组件（`2×1` / `2×2` / `4×2`），桌面一键记录今日运动，支持展示当日自由训练肌群/组数摘要、今日推荐训练计划与连续打卡天数。
  - 支持完整 ZIP 本地导出/导入（合并或覆盖模式）、存储空间明细查看、一键清理缓存与孤立照片清理。
  - 内置 GitHub Releases 联网检查更新：启动静默检测新版本（支持更新、忽略本次、取消），并支持在「设置 → 关于 → 检查更新」手动检测与端内下载安装。

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
