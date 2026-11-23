# AircraftWar Android 迁移实施计划

**Goal：** 把 Swing 版飞机大战迁移为可构建、可运行、可单测的 Android Java 工程，按四步迁移路线逐步验证提交。
**Architecture：** 领域层（`aircraft/basic/bullet/dao/factory/prop/strategy` + `application` 的非 UI 部分）去 AWT 化，只依赖 `render.Sprite`/`SpriteLoader` 与 `application.SoundPlayer` 两个接缝；`application.Game` 是纯逻辑引擎，`ui.BaseGame`（SurfaceView）负责绘制与触摸，`MainActivity` 负责三页切换与生命周期。
**Spec：** `docs/specs/2026-11-23-aircraftwar-android-design.md`

## Task 1：工程骨架与工具链验证（step1 提交）
**Files：** `settings.gradle`、`build.gradle`、`gradle.properties`、`app/build.gradle`、`app/src/main/AndroidManifest.xml`、`app/src/main/res/layout/activity_main.xml`、`app/src/main/java/edu/hitsz/MainActivity.java`、`gradle/wrapper/*`、`gradlew`/`gradlew.bat`

- [x] 固定 JDK 21（`org.gradle.java.home`），Gradle 8.11.1 + AGP 8.10.1，minSdk 30 / compileSdk 35
- [x] wrapper 复用本机已有工程（`services.gradle.org` 不可达），`gradlew` 转 LF
- [x] `assembleDebug` 期望 PASS
- [x] 模拟器安装启动空壳，`pidof` 有输出，logcat 无 FATAL

## Task 2：资源与图片加载（step2 提交）
**Files：** `app/src/main/res/drawable-nodpi/*`（16 张图）、`app/src/main/res/raw/*`（6 个音频）、`render/Sprite.java`、`render/SpriteLoader.java`、`basic/World.java`、`application/ImageManager.java`、`platform/DrawableSpriteLoader.java`、`aircraft/*`、`bullet/*`、`prop/*`

- [x] `BitmapFactory.decodeResource` + `inScaled=false` 替换 `ImageIO.read`
- [x] `Main.WINDOW_*` → `World.*`，领域层不再依赖 Swing 入口
- [x] 解码尺寸与原始文件一致（hero 100×83、bg 512×768、boss 300×200）
- [x] 无 FATAL

## Task 3：游戏循环迁移（step3 提交）
**Files：** `application/Game.java`、`GameFactory`、`EasyGame`/`NormalGame`/`HardGame`、`Achievement*`、`ui/BaseGame.java`

- [x] `Game` 去 `JPanel`/`paint()`/`Timer`/鼠标监听，新增 `tick()` + 只读访问器 + `start()`/`stop()`
- [x] `BaseGame` 实现 `SurfaceHolder.Callback` + `Runnable`：`lockCanvas → draw → unlockCanvasAndPost`
- [x] `surfaceDestroyed` 置标志并 `join`；`surfaceChanged` 取真实宽高算 scale
- [x] 模拟器可见静止帧（背景 + 英雄机 + SCORE/LIFE）
- [x] 退出后 logcat 出现 `draw loop exited`

## Task 4：交互与逻辑收尾（step4 提交）
**Files：** `ui/BaseGame.onTouchEvent`、`ui/DifficultyView.java`、`ui/LeaderboardView.java`、`ui/LeaderboardRows.java`、`platform/AndroidSoundPlayer.java`、`MainActivity.java`

- [x] 触屏拖动驱动英雄机；坐标反算回 512×768
- [x] 敌机生成 / 子弹 / 碰撞 / 计分接回循环（SCORE 10→50 实测递增）
- [x] 三页流转：难度 → 游戏 → 排行榜 → 菜单
- [x] 音频 `SoundPool` + `MediaPlayer`
- [x] 修 `ListView` 未设 `CHOICE_MODE_SINGLE` 导致删除永远无效
- [x] 修结算页先弹窗后刷新导致背后空列表

## Task 5：测试迁移与补强（test 提交）
**Files：** `app/src/test/java/edu/hitsz/**`、`app/src/test/resources/**`

- [x] 20 个原测试类迁移；4 个按新接缝改写
- [x] `support/TestAssets` 用 PNG/JPEG 头解析替代 AWT（Android 单测 bootclasspath 限制）
- [x] `TestAssetsExtension` 自动全局初始化
- [x] 修正原仓库已失败的 `GameBalanceAchievementTest` 断言
- [x] 新增 `GameTickTest`、`WorldTest`、`LeaderboardRowsTest`
- [x] `testDebugUnitTest` 期望 PASS（58 tests / 0 failures）

## Task 6：端到端验收与提交
**Files：** `evidence/*`、`docs/migration-reuse.md`、`docs/ai-usage-log.md`

- [x] `assembleDebug` + `testDebugUnitTest` 双绿（`--rerun-tasks` 无缓存重跑）
- [x] 干净 clone 后独立构建与测试通过
- [x] 模拟器全链路取证：菜单 → 游戏 → 排行榜（录入 "Zoe"）→ 删除一条 → 返回菜单
- [x] 落盘验证 `Player,20,... / Zoe,20,...`，删除后文件行数减少
- [x] logcat 0 个 `FATAL EXCEPTION`
- [x] 按四步路线完成本地提交
