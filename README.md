# AircraftWar2026

Windows 版 Java Swing 飞机大战（`https://github.com/…/AircraftWar`）迁移到 Android 的工程。语言保持 Java，包名沿用 `edu.hitsz`，游戏规则与数值平衡不变。

## 环境

| 项 | 值 |
|---|---|
| Gradle | 8.11.1（wrapper，发行包走腾讯镜像） |
| Android Gradle Plugin | 8.10.1 |
| JDK | 21（`gradle.properties` 用 `org.gradle.java.home` 固定） |
| compileSdk / targetSdk | 35 |
| minSdk | 30（课程要求） |
| 语言 | Java 17 源级 |

`gradle.properties` 里的 JDK 路径是本机绝对路径。换机器时改成该机的 JDK 21，或在 Android Studio 的 *Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK* 里覆盖。

## 构建与运行

```bash
./gradlew assembleDebug          # 产物 app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # 单元测试（58 个用例）
```

安装到已连接的设备/模拟器：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n edu.hitsz.aircraftwar/edu.hitsz.MainActivity
```

或用 Android Studio 直接打开本目录（Gradle 工程），Run `app`。

## 结构

```
app/src/main/java/edu/hitsz/
├── MainActivity.java          # 唯一 Activity：难度页 / 游戏页 / 排行榜页切换
├── aircraft/                  # 英雄机、普通敌机、精英敌机、王牌、Boss
├── application/               # 纯逻辑引擎 Game、难度子类、成就、音频门面
├── basic/                     # 飞行对象基类、逻辑世界尺寸 World
├── bullet/                    # 英雄子弹、敌机子弹
├── dao/                       # 排行榜记录与文本持久化
├── factory/                   # 敌机与道具工厂
├── platform/                  # Android 实现：drawable 解码、SoundPool/MediaPlayer
├── prop/                      # 补给道具、炸弹/冻结观察者
├── render/                    # Sprite / SpriteLoader 接缝
├── strategy/                  # 直射、扇射、环射、不射击
└── ui/                        # BaseGame（SurfaceView）、难度页、排行榜页
app/src/main/res/
├── drawable-nodpi/            # 16 张游戏图（必须 nodpi，见下）
├── raw/                       # 6 个音频
├── layout/                    # activity_main.xml
└── values/                    # strings.xml
```

## 三个必须知道的实现约束

1. **图片必须放 `res/drawable-nodpi` 且解码时 `inScaled=false`。**
   素材按 512×768 逻辑世界设计。放进 density 目录会被按屏幕密度放大（实测 hero 100×83 → 263×218），逻辑坐标写死的碰撞与摆位会整体错位。`WorldTest.backgroundImageIsExactlyOneWorldTall` 会守住这条。

2. **游戏逻辑跑在绘制线程里，主线程只碰 View。**
   `ui.BaseGame` 是 `SurfaceView`，`run()` 里每帧「先 `Game.tick()` 再 `lockCanvas` 绘制」。游戏结束回调来自该线程，`MainActivity` 必须 `mainHandler.post(...)` 回主线程再切页。`surfaceDestroyed` 会 `join` 等待线程真正退出。

3. **排行榜写到应用私有目录。**
   桌面版写工作目录下的 `scores/`；Android 用 `ScoreStorage.setBaseDir(getFilesDir()/scores)`，文件格式仍是 `玩家名,分数,yyyy-MM-dd HH:mm:ss`。

## 测试

58 个 JUnit 5 用例，覆盖飞行器、策略、工厂、道具观察者、DAO、难度模板、难度成长、计分成就、排行榜行映射，以及迁移新增的帧契约（`GameTickTest`）与逻辑世界契约（`WorldTest`）。

Android 单测的编译期 bootclasspath 是 `android.jar`，**没有 `java.awt` / `javax.imageio`**。测试加载图片走 `support/TestAssets`，直接解析 PNG IHDR 与 JPEG SOFn 段取宽高；`TestAssetsExtension` 通过 JUnit 5 自动扩展在所有测试前初始化图片表。

## 文档

- `docs/specs/2026-11-23-aircraftwar-android-design.md`：设计（方案对比、接缝、UI、存储、资源）
- `docs/plans/2026-11-23-aircraftwar-android.md`：四步迁移路线的实施与验证记录
- `docs/migration-reuse.md`：原文件 → 新位置逐文件复用清单、不迁移原因、行为等价性
- `docs/ai-usage-log.md`：AI 使用日志
- `evidence/`：模拟器截图与 logcat

## 与桌面版的已知差异

| 项 | 桌面版 | 本工程 |
|---|---|---|
| 输入 | 鼠标拖拽 | 触屏拖动（抬手后停在最后位置） |
| 音频 | `javax.sound.sampled.Clip` | `SoundPool`（音效）+ `MediaPlayer`（BGM），失败只记日志 |
| 线程 | Timer 线程改集合、EDT 读集合 | 逻辑与绘制同线程，无并发读写 |
| 后台 | 无概念 | `onPause` 停绘制线程，`onResume` 由 `surfaceCreated` 重启 |
| 难度页/排行榜 | Swing 组件 | `LinearLayout` + `ListView` + `AlertDialog` |

音频在模拟器上可能因系统静音或无音频设备而听不到，不影响游戏逻辑。
