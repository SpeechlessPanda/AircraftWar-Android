# AircraftWar2026


一款用 Java 和 Android Framework 原生 API 实现的竖屏飞机大战。

玩家拖动屏幕底部控制英雄机，自动射击；击毁敌机得分，拾取补给强化火力或回血，撞机即死。三档难度各自独立的敌机强度、成长曲线与补给掉落率，成绩按难度分别记录到排行榜。

## 环境要求

| 项 | 要求 |
|---|---|
| Android Studio | 任意自带 AGP 8.10.1 支持的版本（Ladybug 及以上） |
| Android SDK | compileSdk 35，需安装 Android 15（API 35）平台 |
| 运行设备 | Android 11（API 30）及以上的真机或模拟器 |
| JDK | **不需要单独安装**。Gradle 9.2.1 可以直接跑在 Android Studio 自带的 JBR 上 |

**用 Android Studio 打开本目录即可，不需要改任何 JDK 设置。** Studio 默认的 Gradle JDK 就是它自带的运行时，Gradle 9.2.1 已经支持它。

如果你习惯用命令行：

```bash
./gradlew assembleDebug        # Linux / macOS
gradlew.bat assembleDebug      # Windows cmd / PowerShell
```

**不要**在 `gradle.properties` 里写 `org.gradle.java.home`。那是本机路径，提交上去会让别人构建失败。确实需要指定 JDK 时，写到自己的 `~/.gradle/gradle.properties` 里。

`local.properties`（指向本机 SDK 路径）由 Android Studio 首次打开时自动生成，已在 `.gitignore` 中，**不要提交**。

## 开始

**最快路径**：Android Studio 里 `Open` 本目录 → 等 Gradle 同步完成 → Run `app`。

命令行：

```bash
./gradlew assembleDebug          # 产物：app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest      # 单元测试（58 个用例）
```

装到设备或模拟器（`adb` 在 Android SDK 的 `platform-tools` 目录）：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n edu.hitsz.aircraftwar/edu.hitsz.MainActivity
```

### 修改 Gradle / AGP 版本时注意

当前组合是 **Gradle 9.2.1 + AGP 8.10.1**。这个组合是刻意选的：它能在 Android Studio 自带的 JBR（目前是 25）上直接跑，所以协作者不需要装 JDK。

改版本前注意两点：

1. `gradle/wrapper/gradle-wrapper.properties` 的发行包地址指向腾讯云镜像（国内直连 `services.gradle.org` 常超时）。改回官方地址时，`distributionSha256Sum` 必须同步更新，否则 wrapper 会拒绝启动。
2. 降回 Gradle 8.x 会让 Studio 的默认 JBR 失效，构建在配置阶段报 `Could not create task` 这类完全看不出原因的错。真要降，必须让每个人自行指定 JDK 17–23。

## 代码结构

```
app/src/main/java/edu/hitsz/
├── MainActivity.java          唯一 Activity：难度页 / 游戏页 / 排行榜页之间切换
├── aircraft/                  英雄机、普通敌机、三类精英敌机、Boss
├── application/               游戏引擎与规则
│   ├── Game.java              纯逻辑引擎：tick() 推进一帧，不含任何 Android/UI 代码
│   ├── EasyGame / NormalGame / HardGame   三档难度（模板方法）
│   ├── GameFactory.java       按难度创建（工厂方法）
│   ├── Achievement*.java      成就定义与解锁记录
│   ├── AudioManager.java      音频门面
│   └── SoundPlayer.java       音频接缝（接口）
├── basic/                     飞行对象基类、逻辑世界尺寸 World
├── bullet/                    英雄子弹、敌机子弹
├── dao/                       排行榜记录与文本持久化
├── factory/                   敌机与道具工厂
├── platform/                  Android 实现：图片解码、音频播放
├── prop/                      补给道具、炸弹/冻结的观察者
├── render/                    Sprite / SpriteLoader 接缝
├── strategy/                  直射、扇射、环射、不射击（策略模式）
└── ui/                        BaseGame（SurfaceView）、难度页、排行榜页
app/src/main/res/
├── drawable-nodpi/            16 张游戏图
├── raw/                       6 个音频
├── layout/                    activity_main.xml
└── values/                    strings.xml
```

## 改代码前必须知道的四件事

**1. 游戏逻辑必须留在 `application/Game.java` 里，不要下沉到 UI。**

`Game` 不依赖任何 Android 类，因此可以在 JVM 里直接跑单元测试（`app/src/test` 里有 58 个用例覆盖它）。渲染与触摸在 `ui/BaseGame`，页面切换在 `MainActivity`。新增玩法逻辑放 `Game`，新增视觉效果放 `BaseGame`。

**2. 逻辑坐标固定 512×768，不要写死屏幕像素。**

`basic/World` 定义逻辑世界尺寸。`ui/BaseGame` 在 `surfaceChanged` 里算等比缩放与 letterbox 偏移，绘制时套变换，触摸时反向解算。任何位置、速度、碰撞尺寸都用逻辑坐标，换设备才不会跑偏。

**3. 图片必须放 `res/drawable-nodpi`，且解码时 `inScaled=false`。**

这些素材是按 512×768 逻辑世界设计的像素图。放进 `drawable` / `drawable-hdpi` 等带密度的目录会被系统按屏幕密度放大（实测 hero 100×83 会变成 263×218），逻辑坐标写死的碰撞盒会整体错位。`WorldTest` 会守住这条，别为了让测试变绿而改断言。

**4. 游戏逻辑跑在绘制线程里，主线程只碰 View。**

`ui/BaseGame` 是 `SurfaceView`，`run()` 里每帧「先 `Game.tick()` 再 `lockCanvas` 绘制」。因此游戏结束回调来自绘制线程，`MainActivity.onGameOver` 必须 `mainHandler.post(...)` 回主线程后再切页。`surfaceDestroyed` 会 `join` 等待线程真正退出——只置标志位不等，退出后仍有线程持有 Surface，再进入会黑屏或闪退。

## 测试

```bash
./gradlew testDebugUnitTest
```

58 个 JUnit 5 用例，覆盖飞行器、射击策略、敌机与道具工厂、观察者效果、DAO、难度模板、难度成长、计分与成就、排行榜行映射，以及引擎的帧契约（`GameTickTest`）与逻辑世界契约（`WorldTest`）。

**写新测试注意**：Android 单元测试的编译期 bootclasspath 是 `android.jar`，里面**没有** `java.awt` 和 `javax.imageio`。需要读图片尺寸时用 `support/TestAssets`（直接解析 PNG / JPEG 文件结构），不要 `import javax.imageio`，否则编译直接失败。图片表的初始化由 `TestAssetsExtension` 通过 JUnit 5 自动扩展完成，新测试类不必自己调 `ImageManager.init`。

游戏引擎的测试钩子是包内可见的（如 `enemyAircraftsForTest()`），只在 `edu.hitsz.application` 包里的测试能用，不要在业务代码里调用。

## 音频说明

音效走 `SoundPool`，背景音乐走 `MediaPlayer`。任何一次加载或播放失败只写日志、不抛异常——音频不可用不应让游戏崩溃。模拟器以 `-no-audio` 启动或系统静音时听不到声音，属正常现象，不影响游戏逻辑。

## 排行榜存储

成绩写到应用私有目录 `files/scores/records-{easy,normal,hard}.txt`，每行格式：

```
玩家名,分数,yyyy-MM-dd HH:mm:ss
```

三档难度各一个文件，互不影响。卸载应用会一并清除。
