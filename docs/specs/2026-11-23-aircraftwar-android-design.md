# AircraftWar → Android 迁移设计

## 意图

把 `D:\project\AircraftWar`（Java Swing 飞机大战，49 个 JUnit 5 测试）迁移为可在 `D:\androidstudio` 中打开、构建、在模拟器运行的 Android 工程，产物落在 `D:/project/AircraftWar-Android`。语言保持 Java，最大化复用原有源码、包结构与资源；同时补强测试与整体架构。

## 目标 / 非目标

目标：
- 保留 `edu.hitsz` 包名与 `aircraft / basic / bullet / dao / factory / prop / strategy / application` 分层。
- 游戏规则、数值平衡、设计模式（单例、工厂、策略、模板方法、观察者、DAO）原样保留。
- 关注点分离：领域逻辑不依赖 Android SDK，可在 JVM 单测中直接运行。
- 全屏渲染 + 触摸操控，替代 Swing `JPanel` + 鼠标拖拽。
- 排行榜持久化落在应用私有目录。
- 单元测试覆盖领域层，可在无模拟器时运行。

非目标：
- 不改写玩法与平衡数值。
- 不引入 AndroidX / Material / Kotlin；只用 Android Framework API + JUnit 5。
- 不做联网、账号、云同步。
- 不复用 Windows 打包产物（exe/jar 启动脚本）。

## 约束

- 原仓库 `D:\project\AircraftWar` 只读。
- 构建：Gradle 8.11.1 + AGP 8.10.1 + JDK 21，`compileSdk 35`，`minSdk 26`，`targetSdk 35`。
- `minSdk 26` 是为了保留 `java.nio.file` 与 `java.time` 的原样使用，DAO 与 `ScoreRecord` 几乎零改动。
- 依赖只来自 Google Maven 与 Maven Central。

## 方案对比

1. **Robolectric/Compose 重写**：用现代 UI 重做，测试跑 Robolectric。代价：引入大量新依赖与新范式，复用率低，偏离“最大复用”目标。否决。
2. **保留 Swing 逻辑，Activity 内嵌 `BufferedImage` 渲染**：Android 无 `java.awt`，`BufferedImage` 不可用，需自行造位图类型；且 Swing 组件无法在 Android 上存在。否决。
3. **领域层去 AWT 化 + 平台适配层（选中）**：领域类仅保留自研 `Sprite` 尺寸抽象、`AssetSource` 流抽象、`SoundPlayer` 音频抽象；Android 侧提供 `Bitmap`/`AssetManager`/`SoundPool`+`MediaPlayer` 实现；`Game` 从 `JPanel` 抽成纯逻辑引擎，`GameView` 负责渲染与触摸。代价：需改 `AbstractFlyingObject`、`ImageManager`、`AudioManager`、`ResourceManager`、`Game` 的少量 API；收益：复用 76 个源文件中的绝大多数，单测不需要模拟器。

## 关键设计

### 逻辑坐标与渲染

- `edu.hitsz.basic.World` 固定逻辑世界 `512 x 768`，替代 `Main.WINDOW_WIDTH/HEIGHT`。原有碰撞、出界、生成位置计算全部沿用。
- `GameView` 用等比 `fit` 缩放把 `512x768` 映射到屏幕，letterbox 区域绘制背景图铺底，触摸坐标反向映射到世界坐标后直接设置英雄机位置（等价原 `HeroController` 的鼠标拖拽）。

### 平台抽象（三个 seam）

| seam | 领域侧 | Android 实现 | 测试实现 |
|---|---|---|---|
| `Sprite` | `getWidth()/getHeight()/nativeImage()` | `BitmapSprite`（`BitmapFactory` 解码 assets） | `TestSprite`（读 PNG IHDR 尺寸） |
| `AssetSource` | `InputStream open(String path)` | `AndroidAssetSource`（`AssetManager`） | `FileAssetSource`（模块目录下 `src/main/assets`） |
| `SoundPlayer` | `playEffect/playLoop/stop/stopAllLoops` | `AndroidSoundPlayer`（`SoundPool` 音效 + `MediaPlayer` BGM） | `RecordingSoundPlayer` |

`ImageManager` 保留静态字段与 `get(className)` / `getBackgroundImageForDifficulty()` 签名，仅把 `BufferedImage` 换成 `Sprite`，由 `ImageManager.init(SpriteLoader)` 注入。

### 引擎与循环

- `Game` 去掉 `extends JPanel`、`paint()`、`Timer`、鼠标监听；新增 `tick()` 执行原来 `TimerTask.run()` 的一帧逻辑，并提供只读访问器给渲染层（敌机/子弹/道具/英雄/分数/背景）。
- `GameLoop` 用主线程 `Handler` 每 40ms 调 `tick()`，与渲染同线程，消除原 Swing 版“后台 Timer 改集合、EDT 读集合”的数据竞争。
- `Game` 增加 `stop()` 释放循环与音频；`GameActivity` 在 `onPause` 停、`onResume` 续，`onDestroy` 释放。

### UI 结构（对应原 CardLayout）

单 `MainActivity` + `FrameLayout` 根容器，三个 `View`：

- `DifficultyView`：Easy/Normal/Hard 三按钮（原 `DifficultySelectPanel`）。
- `GameView`：全屏渲染 + 触摸（原 `GamePagePanel` + `Game.paint` + `HeroController`）。
- `LeaderboardView`：榜单列表 + 删除 + 返回菜单（原 `LeaderboardPanel` + `JTable`）。玩家名用 `AlertDialog` + `EditText`（原 `JOptionPane` 输入）。

页面切换由 `MainActivity.showXxx()` 等价 `CardLayout.show()`。

### 存储

- `FileScoreRecordDao` 增加 `(Path baseDir, String difficulty)` 构造器；生产用 `context.getFilesDir()/scores`，测试用 `@TempDir`。
- 保留原文件格式 `玩家名,分数,yyyy-MM-dd HH:mm:ss`，`scores/records-<difficulty>.txt` 三档独立。

### 资源

- 图片 19 个 → `app/src/main/assets/images/`，音频 6 个 → `app/src/main/assets/sounds/`，文件名保持不变，路径常量改为 assets 相对路径。
- 不转换 WAV 格式（WAV PCM 由 `SoundPool`/`MediaPlayer` 直接支持）；若某音效在模拟器解码失败，仅记录日志，不影响游戏主流程。

### 已知原仓库缺陷的处理

- `GameBalanceAchievementTest.difficultiesExposeDifferentSupplyAndBossRewardBalance` 在 HEAD 即失败：测试断言 `easy>0.85 / normal>0.70 / hard>0.55`，实际常量为 `0.55 / 0.40 / 0.30`。迁移时把断言改为“三档单调递减且互不相同”，保留代码实际平衡，不放宽到恒真。

## 测试策略

- 20 个测试类全部迁移到 `app/src/test/java`，包名不变。
- `ImageManager`/`ResourceManager` 的 Swing/JAR 假设替换为测试适配器；`TestImagesExtension` 自动初始化图片注册表，测试体不改。
- 删除 Swing 专属测试（`AudioPlayerTest`、`LeaderboardTableModelTest`），替换为等价领域测试：`AudioManagerTest`（键语义）、`LeaderboardRowsTest`（行映射）。
- 新增测试覆盖迁移新增的接缝：`WorldTest`（坐标映射）、`GameTickTest`（单帧推进不崩溃且状态改变）、`SoundPlayerContractTest`。
- 验收命令：`gradlew assembleDebug`、`gradlew testDebugUnitTest`。

## 交付

- 完整 Gradle 工程 + `gradle/wrapper`。
- `docs/migration-reuse.md`：原文件 → 新位置的复用清单（逐文件）。
- `evidence/`：模拟器截图与 logcat。
- 本地 git 提交。
