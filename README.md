# MiAudioPlay

MiAudioPlay 是一款基于现代 Android 开发堆栈构建的高颜值音乐播放器。它不仅支持本地音频播放，还配备了强大的多源歌词搜索与动态显示功能，旨在为用户提供极致的听歌体验。

## 🌟 主要功能

- **核心播放能力**：
  - 基于 AndroidX Media3 (ExoPlayer) 构建，支持 MP3, FLAC, WAV 等主流音频格式。
  - 支持后台播放、通知栏控制及锁屏界面播放控制。
- **强大的歌词系统**：
  - **多源搜索**：集成了网易云音乐、QQ 音乐、LrcLib、Happi、Lyrics.ovh 等多个歌词 API。
  - **动态同步**：高精度的 LRC 歌词解析与实时滚动显示。
  - **智能缓存**：自动缓存已下载的歌词，节省流量并提升二次加载速度。
- **播放列表管理**：
  - 支持创建自定义播放列表。
  - 轻松实现歌曲的添加、移除及顺序调整。
- **现代化 UI/UX**：
  - 全面采用 **Jetpack Compose** 构建，拥有流畅的动画和响应式布局。
  - 遵循 Material 3 设计规范，界面简洁、高级。
  - 实时毛玻璃效果（Glassmorphism）与动态配色。

## 🛠️ 技术实现

### 架构设计
项目遵循 **MVVM (Model-View-ViewModel)** 架构模式，确保代码的高内聚、低耦合。

- **UI 层 (UI Layer)**：使用 Jetpack Compose 实现。界面通过订阅 ViewModel 的 State 来响应数据变化。
- **领域/数据层 (Data Layer)**：
  - **Repository 模式**：封装了数据来源（本地数据库或远程 API）。
  - **Room Database**：用于存储播放列表信息、歌曲元数据及歌词缓存。
  - **Retrofit**：处理与多个歌词后端 API 的网络通信。
- **服务层 (Service Layer)**：
  - **Media3 SessionService**：在后台长期运行，管理播放状态并与系统介质控制器交互。

### 核心技术点
- **Media3 & ExoPlayer**：利用 Google 最新的统一媒体框架处理复杂的音频播放逻辑。
- **LRC 解析算法**：自定义实现的 `LrcParser` 能够兼容多种格式的歌词文件，并精确计算时间戳偏移。
- **多源并行检索**：在搜索歌词时，策略性地在多个 API 之间轮询或并发请求，显著提高歌词匹配率。

## 📂 项目结构

```text
com.miaudioplay
├── data           # 数据持久化与网络请求 logic
│   ├── api        # 歌词 API 定义 (Netease, QQ, LrcLib 等)
│   ├── models     # 实体类 (Song, Playlist, LyricLine)
│   └── repository # 数据仓库
├── service        # Media3 播放服务实现
├── ui             # UI 界面与组件
│   ├── components # 通用 UI 单元 (播放列表项, 迷你播放器)
│   ├── screens    # 页面级 Compose 视图 (播放页, 首页)
│   └── theme      # 主题色彩与排版
├── utils          # 工具类 (日志, 文件处理)
└── viewmodel      # 核心业务逻辑与 UI 状态管理
```

## 🚀 运行环境

- Android API 级别：24+ (Android 7.0)
- 开发工具：Android Studio Ladybug 或更高版本
- 语言：Kotlin (1.9+)

---

*MiAudioPlay - 听你所爱。*
