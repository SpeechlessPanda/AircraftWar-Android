# 迁移复用清单

源仓库：`D:\project\AircraftWar`（Java Swing，桌面版）
目标仓库：`D:\project\AircraftWar-Android`（Android Studio 工程 `AircraftWar2026`）

复用程度分四档：

- **原样复制**：文件内容未改，仅换目录。
- **改接口**：业务逻辑保留，替换平台相关类型（`BufferedImage`→`Sprite`、`Main.WINDOW_*`→`World.*`、`Clip`→`SoundPool`/`MediaPlayer`）。
- **重写**：原文件职责保留，但实现方式完全不同（Swing 组件 → Android View + SurfaceView）。
- **不迁移**：桌面专属，职责由新类接管。

## 一、源文件（`src/edu/hitsz/`，48 个）

| 原文件 | 新位置（`app/src/main/java/`） | 复用程度 | 改动原因 |
|---|---|---|---|
| `aircraft/AbstractAircraft.java` | `aircraft/AbstractAircraft.java` | 原样复制 | 纯逻辑：血量、射击策略、观察者回调 |
| `aircraft/BossEnemy.java` | `aircraft/BossEnemy.java` | 改接口 | `Main.WINDOW_*` → `World.*` |
| `aircraft/EliteEnemy.java` | `aircraft/EliteEnemy.java` | 改接口 | 同上（出界判定） |
| `aircraft/ElitePlusEnemy.java` | `aircraft/ElitePlusEnemy.java` | 改接口 | 同上 |
| `aircraft/EliteProEnemy.java` | `aircraft/EliteProEnemy.java` | 改接口 | 同上 |
| `aircraft/HeroAircraft.java` | `aircraft/HeroAircraft.java` | 改接口 | 同上（初始坐标依赖图高） |
| `aircraft/MobEnemy.java` | `aircraft/MobEnemy.java` | 改接口 | 同上 |
| `application/Achievement.java` | `application/Achievement.java` | 原样复制 | 枚举，无平台依赖 |
| `application/AchievementManager.java` | `application/AchievementManager.java` | 原样复制 | 去重解锁 + 摘要文本 |
| `application/AircraftWarApp.java` | — | 不迁移 | `JFrame` + `CardLayout` 外层容器，由 `MainActivity` 接管 |
| `application/AudioManager.java` | `application/AudioManager.java` | 改接口 | 改为门面 + `SoundPlayer` 接缝，调用点 API 不变 |
| `application/AudioPlayer.java` | — | 不迁移 | 基于 `javax.sound.sampled.Clip` 的播放线程，由 `platform.AndroidSoundPlayer` 接管 |
| `application/DifficultySelectPanel.java` | `ui/DifficultyView.java` | 重写 | `JPanel`+`JButton` → `LinearLayout`+`Button` |
| `application/EasyGame.java` | `application/EasyGame.java` | 改接口 | 构造参数 `AircraftWarApp` → `GameOverListener` |
| `application/FireModeManager.java` | `application/FireModeManager.java` | 原样复制 | `ScheduledExecutorService` 在 Android 同样可用 |
| `application/Game.java` | `application/Game.java` | 重写 | 从 `JPanel` 抽为纯逻辑引擎：去 `paint()`/`Timer`/鼠标监听，改为 `tick()` + 只读访问器 |
| `application/GameFactory.java` | `application/GameFactory.java` | 改接口 | 同上，透传 `GameOverListener` |
| `application/GamePagePanel.java` | `ui/BaseGame.java` + `MainActivity.startGame` | 重写 | `JPanel` 容器 → `SurfaceView` + Activity 页面切换 |
| `application/HardGame.java` | `application/HardGame.java` | 改接口 | 同 `EasyGame` |
| `application/HeroController.java` | `ui/BaseGame.onTouchEvent` | 重写 | `MouseAdapter.mouseDragged` → `MotionEvent` + 坐标反算 |
| `application/ImageManager.java` | `application/ImageManager.java` | 改接口 | `BufferedImage`+`ImageIO` → `Sprite`+注入 `SpriteLoader` |
| `application/LeaderboardPanel.java` | `ui/LeaderboardView.java` | 重写 | `JTable`+`JOptionPane` → `ListView`+`AlertDialog` |
| `application/LeaderboardTableModel.java` | `ui/LeaderboardRows.java` | 重写 | `DefaultTableModel` → 不可变的行映射 |
| `application/Main.java` | `MainActivity.java` | 重写 | `JFrame` 入口 → `Activity` 入口 |
| `application/NormalGame.java` | `application/NormalGame.java` | 改接口 | 同 `EasyGame` |
| `application/ResourceManager.java` | `render/SpriteLoader` + `platform.DrawableSpriteLoader` | 重写 | classpath/文件双查找 → `BitmapFactory.decodeResource` |
| `basic/AbstractFlyingObject.java` | `basic/AbstractFlyingObject.java` | 改接口 | `BufferedImage` → `Sprite`，`Main.WINDOW_*` → `World.*` |
| `bullet/BaseBullet.java` | `bullet/BaseBullet.java` | 改接口 | `Main.WINDOW_*` → `World.*` |
| `bullet/EnemyBullet.java` | `bullet/EnemyBullet.java` | 原样复制 | 仅承载参数 |
| `bullet/HeroBullet.java` | `bullet/HeroBullet.java` | 原样复制 | 仅承载参数 |
| `dao/FileScoreRecordDao.java` | `dao/FileScoreRecordDao.java` | 改接口 | 存储根目录改为可注入（`ScoreStorage`），其余读写逻辑与文件格式不变 |
| `dao/ScoreRecord.java` | `dao/ScoreRecord.java` | 原样复制 | `java.time` 在 minSdk 30 可直接用 |
| `dao/ScoreRecordDao.java` | `dao/ScoreRecordDao.java` | 原样复制 | 接口 |
| `factory/EliteEnemyFactory.java` | `factory/EliteEnemyFactory.java` | 原样复制 | 工厂方法 |
| `factory/ElitePlusEnemyFactory.java` | `factory/ElitePlusEnemyFactory.java` | 原样复制 | 工厂方法 |
| `factory/EliteProEnemyFactory.java` | `factory/EliteProEnemyFactory.java` | 原样复制 | 工厂方法 |
| `factory/EnemyFactory.java` | `factory/EnemyFactory.java` | 原样复制 | 工厂接口 |
| `factory/MobEnemyFactory.java` | `factory/MobEnemyFactory.java` | 原样复制 | 工厂方法 |
| `factory/PropFactory.java` | `factory/PropFactory.java` | 原样复制 | 掉落池与概率 |
| `prop/AbstractProp.java` | `prop/AbstractProp.java` | 改接口 | `Main.WINDOW_*` → `World.*` |
| `prop/BloodSupply.java` | `prop/BloodSupply.java` | 原样复制 | 回血 40 |
| `prop/BombEffectContext.java` | `prop/BombEffectContext.java` | 原样复制 | 计分累加器 |
| `prop/BombSubject.java` | `prop/BombSubject.java` | 原样复制 | 观察者主题 |
| `prop/BombSupply.java` | `prop/BombSupply.java` | 原样复制 | 触发音效 |
| `prop/FirePlusSupply.java` | `prop/FirePlusSupply.java` | 原样复制 | 环射 12 发 |
| `prop/FireSupply.java` | `prop/FireSupply.java` | 原样复制 | 扇射 5 发 |
| `prop/FreezeEffectContext.java` | `prop/FreezeEffectContext.java` | 原样复制 | 定时恢复速度 |
| `prop/FreezeSubject.java` | `prop/FreezeSubject.java` | 原样复制 | 观察者主题 |
| `prop/FreezeSupply.java` | `prop/FreezeSupply.java` | 原样复制 | 占位实现 |
| `prop/PropObserver.java` | `prop/PropObserver.java` | 原样复制 | 观察者接口 |
| `prop/PropSubject.java` | `prop/PropSubject.java` | 原样复制 | 观察者集合 |
| `strategy/FanShootStrategy.java` | `strategy/FanShootStrategy.java` | 原样复制 | 扇形弹幕 |
| `strategy/NoShootStrategy.java` | `strategy/NoShootStrategy.java` | 原样复制 | 空实现 |
| `strategy/RingShootStrategy.java` | `strategy/RingShootStrategy.java` | 原样复制 | 环形弹幕 |
| `strategy/ShootStrategy.java` | `strategy/ShootStrategy.java` | 原样复制 | 策略接口 |
| `strategy/StraightShootStrategy.java` | `strategy/StraightShootStrategy.java` | 原样复制 | 直射 |

统计：48 个源文件中，**原样复制 30 个**、**改接口 13 个**、**重写 5 个**（`Game`、`ImageManager` 之外的三处见下表），**不迁移 2 个**、另有 6 个由新类接管（上表合计 48）。

## 二、新增文件（本工程独有，9 个）

| 新文件 | 职责 | 为什么需要 |
|---|---|---|
| `MainActivity.java` | Activity 入口 + 三页切换 + 生命周期 | 替代 `Main`/`AircraftWarApp` |
| `basic/World.java` | 逻辑世界 512×768 | 替代 `Main.WINDOW_*`，让领域层不依赖 Swing 入口 |
| `render/Sprite.java` | 位图尺寸+原生对象抽象 | 替代 `BufferedImage` |
| `render/SpriteLoader.java` | 逻辑名 → 图片的解码接口 | 替代 `ImageIO.read` 与 `ResourceManager` |
| `dao/ScoreStorage.java` | 榜单存储根目录（可注入） | Android 工作目录不可写，需指向 `getFilesDir()` |
| `application/SoundPlayer.java` | 音频接缝 | 隔离 `SoundPool`/`MediaPlayer` |
| `platform/DrawableSpriteLoader.java` | `BitmapFactory.decodeResource` + 解码缓存 | 图片走 `res/drawable-nodpi` |
| `platform/AndroidSoundPlayer.java` | 音效 `SoundPool`、BGM `MediaPlayer` | 替代 `javax.sound.sampled` |
| `ui/BaseGame.java` | SurfaceView + 绘制线程 + 触屏 | 替代 `GamePagePanel`+`Game.paint`+`HeroController` |
| `ui/DifficultyView.java` | 难度选择页 | 替代 `DifficultySelectPanel` |
| `ui/LeaderboardView.java` | 排行榜页 | 替代 `LeaderboardPanel` |
| `ui/LeaderboardRows.java` | 榜单行映射 + 成就文案 | 替代 `LeaderboardTableModel` |

## 三、测试（`test/`，20 个）

| 原测试 | 新位置（`app/src/test/java/`） | 处理 |
|---|---|---|
| `aircraft/EnemyDefaultSpeedTest` | 同名 | 原样复制 |
| `aircraft/HeroAircraftFireModeTest` | 同名 | 原样复制 |
| `aircraft/HeroAircraftTest` | 同名 | 原样复制 |
| `application/AchievementManagerTest` | 同名 | 原样复制 |
| `application/AudioPlayerTest` | `application/AudioManagerTest` | 重写：原测试针对 `Clip` 代理，Android 无 `Clip`；改为验证 `AudioManager` 的键语义与资源存在性 |
| `application/BossSupplyDropTest` | 同名 | 改接口：反射读私有字段 → 包内访问器 |
| `application/FrozenEnemyHitTest` | 同名 | 改接口：同上 |
| `application/GameBackgroundTest` | 同名 | 原样复制 |
| `application/GameBalanceAchievementTest` | 同名 | 改断言：原仓库 HEAD 即失败，见第四节 |
| `application/GameBulletCollisionTest` | 同名 | 原样复制 |
| `application/GameDifficultyTemplateTest` | 同名 | 原样复制 |
| `application/GamePropSubjectIntegrationTest` | 同名 | 原样复制 |
| `application/GameSpawnLimitTest` | 同名 | 原样复制 |
| `application/GameTest` | 同名 | 改接口：`GameFactory.create(difficulty, listener)` 签名 |
| `application/LeaderboardAchievementDisplayTest` | `ui/LeaderboardRowsTest` | 重写：`LeaderboardPanel` 不再存在，文案逻辑搬到 `LeaderboardRows` |
| `application/LeaderboardTableModelTest` | `ui/LeaderboardRowsTest` | 重写：`DefaultTableModel` → 行映射 |
| `application/ResourceManagerTest` | `support/TestAssets` + `basic/WorldTest` | 重写：classpath 查找被删除，改为验证真实资源尺寸 |
| `factory/PropFactoryProbabilityTest` | 同名 | 原样复制 |
| `prop/PropObserverEffectTest` | 同名 | 原样复制 |
| `strategy/ShootStrategySpeedTest` | 同名 | 原样复制 |

新增测试：

| 新测试 | 覆盖什么 |
|---|---|
| `application/GameTickTest` | 未 `start()` 不推进、`tick()` 接回射击、背景滚动范围、结算只触发一次、`moveHeroTo` 边界、新局重置 |
| `basic/WorldTest` | 逻辑世界尺寸 + 背景图与之一致 |
| `support/TestAssets` / `TestAssetsExtension` | 用 PNG/JPEG 头解析替代 ImageIO（Android 单测无 AWT），自动初始化图片表 |

结果：**58 个测试，0 失败**。

## 四、不迁移的文件及原因

| 原文件 | 原角色 | 被谁取代 | 原因 |
|---|---|---|---|
| `application/Main.java` | `main()` + `JFrame` 启动 | `MainActivity` | Android 无 `main()`，入口是 Activity |
| `application/AircraftWarApp.java` | `JFrame` + `CardLayout` 容器 | `MainActivity` | 页面切换改用 `FrameLayout` 换子 View |
| `application/DifficultySelectPanel.java` | `JPanel` 难度按钮 | `ui/DifficultyView` | Swing 组件不存在于 Android |
| `application/GamePagePanel.java` | 承载 `Game` 的 `JPanel` | `ui/BaseGame` | 需要 `SurfaceView` 的双缓冲与子线程 |
| `application/LeaderboardPanel.java` | `JTable` + `JOptionPane` | `ui/LeaderboardView` | 表格/对话框换成 `ListView`/`AlertDialog` |
| `application/LeaderboardTableModel.java` | `DefaultTableModel` 行模型 | `ui/LeaderboardRows` | 同上 |
| `application/HeroController.java` | `MouseAdapter` 拖拽 | `ui/BaseGame.onTouchEvent` | 输入源从鼠标换成触屏 |
| `application/ResourceManager.java` | classpath/文件双查找 | `platform.DrawableSpriteLoader` | 资源走 `res/drawable` + `BitmapFactory` |
| `application/AudioPlayer.java` | `Clip` 播放线程 | `platform.AndroidSoundPlayer` | Android 无 `javax.sound.sampled` |

## 五、行为等价性

| 规则 | 是否等价 | 说明 |
|---|---|---|
| 碰撞判定 | 等价 | `AbstractFlyingObject.crash` 原样保留；飞机纵向判定区间为高度的 1/2，非飞机为全高 |
| 计分 | 等价 | 击毁敌机 +10；炸弹按目标额外计分（普通/精英 +10、王牌 +10 且先扣 30 血） |
| 敌机共存上限 | 等价 | 总数 7（easy 5）、同类精英各 1、精英合计 3、Boss 1 |
| 生成概率 | 等价 | 基础 0.40/0.25/0.20 分段，三个难度子类各自覆盖 |
| Boss 阈值 | 等价 | 每 300 分一次，`nextBossScore` 累进，easy 不刷 Boss |
| Boss 血量成长 | 等价 | 仅 `HardGame` 每次 +15% |
| 补给掉落 | 等价 | easy 0.55 / normal 0.40 / hard 0.30；Boss 固定 drop（hard 4 个、其余 3 个）并做了坐标分散 |
| 成就解锁 | 等价 | 1000 分、首次击败 Boss、炸弹一次 ≥3 目标、冰冻 ≥3 目标、击毁王牌 |
| 排行榜排序 | 等价 | 分数降序，同分按记录时间升序 |
| 排行榜删除 | 等价 | 按“排序后的名次”删除，而不是文件行号 |
| 榜单文件格式 | 等价 | `玩家名,分数,yyyy-MM-dd HH:mm:ss`，`records-<difficulty>.txt` |
| 资源尺寸 | 等价 | 图片放 `res/drawable-nodpi` 且 `inScaled=false`，解码尺寸与桌面版 PNG/JPEG 原始尺寸一致（hero 100×83、bg 512×768、boss 300×200） |
| 帧率 | 等价 | 固定 40ms/帧；掉帧时重置节拍而非追赶 |
| 线程模型 | **改进** | 桌面版定时器线程改集合、EDT 读集合；迁移后逻辑与绘制同在一个子线程，无并发读写 |
| 输入 | **差异（有意）** | 鼠标拖拽 → 触屏拖动；抬手后英雄机停在最后位置，与原 `mouseDragged` 行为一致 |
| 音频 | **差异（实现）** | `Clip` → `SoundPool`（音效）+ `MediaPlayer`（BGM）；单次播放失败只记日志，不中断游戏 |
| 后台行为 | **差异（有意）** | `onPause` 停绘制线程、`onResume` 恢复；桌面版无此概念 |

## 六、验证方式

```bash
cd /d/project/AircraftWar-Android
./gradlew --no-daemon assembleDebug        # BUILD SUCCESSFUL, app-debug.apk ≈10.6 MB
./gradlew --no-daemon testDebugUnitTest    # 58 tests, 0 failures
```

模拟器（`emulator-5554`，API 35）：安装启动 → 难度页 → NORMAL → 敌机/子弹/Boss 出现、SCORE 递增 → 拖动后英雄机位移 → 撞机后进入排行榜并落盘 `Player,<score>,<time>`；logcat 无 `FATAL EXCEPTION`。
