# AircraftWar Android 迁移实施计划

**Goal:** 把 Swing 版飞机大战迁移为可构建、可运行、可单测的 Android Java 工程。
**Architecture:** 领域层（`edu.hitsz.*` 除 `application` 的 UI 部分）去 AWT 化，仅依赖自定义 `Sprite`/`AssetSource`/`SoundPlayer` 三个接缝；`Game` 为纯逻辑引擎，`GameView` 负责渲染与触摸，`MainActivity` 负责页面切换。
**Spec:** `docs/specs/2026-11-23-aircraftwar-android-design.md`

## Task 1: 工程骨架与工具链验证
**Files:** Create `settings.gradle`、`build.gradle`、`gradle.properties`、`app/build.gradle`、`app/src/main/AndroidManifest.xml`、`app/src/main/java/edu/hitsz/MainActivity.java`、`gradle/wrapper/*`
- [ ] 用 JDK 21 + 本地 Gradle 8.11.1 生成 wrapper
- [ ] `gradlew assembleDebug` 期望 PASS（空壳 APK）
- [ ] 模拟器安装启动空壳，`pidof` 有输出

## Task 2: 领域层去 AWT 化
**Files:** Modify `basic/AbstractFlyingObject.java`、`application/ImageManager.java`、`application/ResourceManager.java`；Create `render/Sprite.java`、`basic/World.java`
- [ ] `Sprite` 接口 + `SpriteLoader`
- [ ] `AbstractFlyingObject` 用 `Sprite` 替代 `BufferedImage`，`World.WIDTH/HEIGHT` 替代 `Main.WINDOW_*`
- [ ] 原 17 个领域类（aircraft/bullet/prop/strategy/factory/dao）编译通过

## Task 3: 引擎抽取
**Files:** Modify `application/Game.java` 及三个难度子类；Create `application/GameLoop.java`
- [ ] `Game` 去掉 JPanel/paint/Timer/鼠标监听，新增 `tick()` 与只读访问器
- [ ] 单测：连续 `tick()` 后敌机/子弹集合有变化，英雄机被击中后 hp 下降

## Task 4: Android 平台实现
**Files:** Create `platform/AndroidAssetSource.java`、`platform/BitmapSprite.java`、`platform/AndroidSoundPlayer.java`、`application/AudioManager.java`（改为委托 `SoundPlayer`）
- [ ] assets 拷贝（19 图 + 6 音频）
- [ ] `MainActivity.onCreate` 顺序：AssetSource → ImageManager.init → ScoreDao baseDir

## Task 5: UI 三个页面
**Files:** Create `ui/DifficultyView.java`、`ui/GameView.java`、`ui/LeaderboardView.java`、`ui/LeaderboardRows.java`、`MainActivity.java`
- [ ] 难度选择 → 游戏 → 结算/排行榜 → 返回菜单全链路
- [ ] 触摸拖动英雄机；`GameView` 等比缩放与坐标映射单测

## Task 6: 测试迁移与补强
**Files:** Create `app/src/test/java/edu/hitsz/**`（20 个原测试类 + 新测试）
- [ ] `TestImagesExtension` 自动初始化
- [ ] 修正原仓库已失败的 `GameBalanceAchievementTest` 断言
- [ ] `gradlew testDebugUnitTest` 期望 PASS

## Task 7: 端到端验收与提交
**Files:** Create `evidence/*.png`、`evidence/*.txt`、`docs/migration-reuse.md`
- [ ] assembleDebug + testDebugUnitTest 双绿
- [ ] 模拟器安装、启动、触摸、截图、logcat 无崩溃
- [ ] 本地 git 提交
