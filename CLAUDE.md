# Actiary 项目规则

## 协作

- **所有面向用户的文字都用中文**：最终汇报、进度说明、提问及选项、计划与总结都包括在内；代码、命令、路径和标识符保持原文。先报结果。
- 在已有授权内持续完成任务，保留用户改动；当前明确要求优先于本文件。
- 按 superpowers 流程工作：新功能或行为变更先用 `brainstorming` 定设计，设计获用户批准后再用 `writing-plans` 写计划；小改动用简短设计。
- 设计存 `docs/superpowers/specs/`，计划存 `docs/superpowers/plans/`，执行进度记在 `.superpowers/sdd/` 的 ledger 里；这些文件只留本地、已忽略、不提交。总路线见 `docs/superpowers/plans/2026-11-19-actiary-roadmap.md`。
- 使用 `ponytail` full：先读真实入口、调用方和现有实现，再做最小正确改动；复用优先，不加无证据的防御、回退、依赖或架构层。涉及 UI 使用 `ui-ux-pro-max`。

### 会话分工（主会话只审查与规划）

- **主会话**（Opus 5.5，`claude-opus-5-5`）负责：设计、计划、关键决策、任务拆分与派工、审查子代理产出、最终验证与汇报。主会话**不直接编写或修改产品代码、测试和构建脚本**；只写规划类文档（specs / plans / ledger / 本文件）。
- **执行方**：实现、写测试、修 bug、格式化、探索代码库、跑长构建，一律交给子代理、后台任务或另起的子会话，**全部使用 Sonnet 5.5**（`claude-sonnet-5-5`）：
  - 调用 Agent 时显式传 `model: "sonnet"`；不要用 `fork` 类型（它继承主会话模型）。
  - 另起会话（`start_session`）同样指定 Sonnet；后台任务同理。
  - 用户当次明确指定模型时以其为准。
- **派工**：按 superpowers `subagent-driven-development` / `dispatching-parallel-agents` 执行，无需另行请求。每个任务的派工说明写清：目标、涉及文件、相关设计条款（引用 `workout_journal_v2_package/README.md` 章节号）、验收命令、禁止事项。互不依赖且不碰同一文件的任务可并行；会改同一文件的任务串行。
- **审查**：子代理完成后，主会话必须亲自读 diff 和验证命令输出再接受，不以子代理自述为准；不合格则带具体意见退回原子代理或新派一个 Sonnet 子代理修复。审查重点：是否偏离设计约束、是否引入未经同意的依赖 / 架构层、测试是否真的覆盖行为。
- **例外**：一两处的错别字、路径或配置值这类机械修正，主会话可直接改，但要在汇报中说明。

## 产品与界面

- 设计源是 `workout_journal_v2_package/README.md` 与 `assets/` 参考图（概念图，非像素稿）；只读不改。与设计冲突的实现先问用户。
- 应用名 **Actiary**（暂定）。当前形态：Android / Kotlin / Jetpack Compose，单 `app` 模块，包结构见总路线 §4；**minSdk 31，只支持 Android 12 及以上**，不写低版本兼容分支。ABI 仅保留 `arm64-v8a`；后续在 GitHub 发布 Release 时须在 Release 说明中明确标注适配范围为「Android 12+ (`arm64-v8a`)」。
- 首页方案 A：完整月历常驻顶部，不默认折叠成周历。三态“已锻炼 / 休息日 / 未记录”不得混淆，未记录 = 无记录；状态用形状 + 图标 + 文案表达，不只靠颜色。
- 积极使用液态玻璃：导航、标题栏、月历面板、详情卡、记录 Sheet、筛选栏、统计摘要、设置分组等容器都用玻璃，并配环境背景；日期格、列表逐项、长文本、照片、图表本体保持清晰实色。具体范围与同屏面数上限以 `docs/superpowers/specs/2026-11-19-glass-expansion-design.md` 为准（覆盖设计源 §2 / §5.3）。所有玻璃经项目内唯一 `GlassRenderer` 接入 Backdrop，不混装其他玻璃引擎；组件只接收已解析的 tokens / profile，不读 DataStore。
- 外观配置（DataStore）与业务数据（Room）分离；换主题、玻璃等级不得改变记录、照片或统计。
- 强调色保存用户原始 Seed，派生色按对比度校正（文本 ≥ 4.5:1，非文本 ≥ 3:1）；不静默改用户 HEX。自定义颜色走 draft，仅“应用”时持久化。
- 设置页预览与主界面使用同一 `ThemeTokens` 和玻璃实现。
- 只展示真实数据：统计不推算时长、卡路里；缺数据显示“暂无记录”。照片不加滤镜、不被强调色染色。
- **Avoid unnecessary secondary labels and descriptions. Let the layout do the explaining.** 简单、直观、短文案。
- 适配浅色 / 深色 / 跟随系统、小屏与系统“减少动画”；省电模式、减少动画或 GPU 效果异常时玻璃降级为半透明中性面 + 细高光，功能不受影响。
- 中英文案成对维护（`res/values/strings.xml` 中文默认、`res/values-en/strings.xml` 英文），键保持一致；不在代码里写死用户可见文案。
- 离线优先：不引入账号、云端配置、网络权限或联网依赖。
- 第三方库版本开工时验证后锁定在 `gradle/libs.versions.toml`；不虚构库 API 或参数名，接入前先查官方示例。
- **优先复用成熟开源项目**：所需功能若已有维护良好的开源库或参考实现，规划时先调研并优先引用 / 移植，不重复造轮子；自写须在设计里说明理由。引用的前提：许可证兼容（Apache-2.0 / MIT / BSD 等宽松许可）；符合离线优先（无网络权限、无遥测，必要时在清单里移除）；版本已验证并锁定；移植代码保留来源与许可说明。每引入或移植一项，同步登记到 `THIRD_PARTY_NOTICES.md` 与应用内 `res/raw/third_party_notices.txt`，并显示在设置“关于”页的开源信息里（库名、用途、许可证、项目地址）。新增依赖需联网下载时仍先问用户。

## 本地环境

- 本机无全局 JDK / Android SDK / Gradle。**已确认**直接用 `F:\Program\AnPod\.tmp-jdk17\jdk-17.0.20.1+1`（JDK 17）与 `F:\Program\AnPod\.tmp-android-sdk35`（platform 35、build-tools 34.0.0）；`sdk.dir` 写在已忽略的 `local.properties`，`JAVA_HOME` 只在命令内设置。不得删除或修改 AnPod 下任何文件。需要新 SDK 组件时先问用户。
- `GRADLE_USER_HOME` 用默认 `~/.gradle`（已缓存 Gradle 8.9、AGP 8.7.3、Kotlin 2.1.21、compose-bom 2025.06.00），构建优先 `--offline`；升级版本或新增依赖需联网下载时先问用户。
- `adb` 在 `F:\Download\platform-tools-latest-windows\platform-tools\adb.exe`（用户提供）；只在用户当次明确授权时用于安装、运行或跑真机测试（`connectedDebugAndroidTest` 需把该目录加入命令内的 `Path`）。
- Gradle 一律通过仓库内 `gradlew` 运行。Node 为用户级安装，PowerShell 中先合并用户与系统 `Path`。
- 用 Bash heredoc 写入中文或反斜杠容易出错：多处替换写成脚本文件再运行，按文件实际换行符处理，每处替换断言恰好匹配一次。

## 验证与交付

- 实机测试由用户完成；代理不自行向真实手机安装、运行或调试应用，除非当次明确授权。离线验证不能替代实机的玻璃效果、滚动性能或观感结论。获授权装机时，优先构建并安装开启了 R8 混淆与资源裁剪 + `arm64-v8a` 的 Release APK（`app-release.apk`）。
- 先跑相关回归，再按影响扩大检查；验证真实入口和行为，不只验证 helper 或复述实现；不得用错误环境下的失败或模拟结果冒充通过。
- 常规检查：`gradlew testDebugUnitTest`、`gradlew lintDebug`（不新增 error / warning）、`gradlew assembleDebug`。色彩派生、对比度、日期状态映射、Repository 这类逻辑必须有 JVM 单测；Room schema 变更要导出 schema 并带迁移测试。
- **版本号**：`versionName` 采用 `主.次.修订`，从 `0.1.0` 开始，唯一来源是 `app/build.gradle.kts`。
  - 次版本 +1（修订归 0）：新增功能、行为变更、新页面或设置项、Room schema 变更、路线阶段（P1–P5）完成。
  - 修订 +1：bug 修复、文案、视觉微调、性能优化、依赖小版本升级等不改变功能的改动。
  - 主版本保持 `0`，直到用户宣布正式版 `1.0.0`；之后不兼容的数据或交互变更才升主版本，且需用户决定。
  - `versionCode` 每次改 `versionName` 都 +1，只增不减。
  - 按“交付”升版本：一组改动经主会话审查通过、获授权提交时升一次，而不是每个中间提交都升；同一次交付同时有功能和修复时只升次版本。
  - 版本号由主会话在派工说明中指定，子代理不自行改版本；提交信息注明新版本号。Room `version` 与应用版本号相互独立。
- 规划、日志、截图、测试证据存 `local/` 或 `docs/superpowers/`，只留本地并忽略；遵守 `.gitignore`。
- `git init`、提交、推送、发布、PR、合并及删分支都需用户明确授权。清理本轮过期临时文件，不删用户数据。
- 最终说明变更、检查结果和真实限制。
