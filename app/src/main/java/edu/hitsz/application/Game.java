package edu.hitsz.application;

import edu.hitsz.aircraft.*;
import edu.hitsz.basic.AbstractFlyingObject;
import edu.hitsz.basic.World;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.dao.FileScoreRecordDao;
import edu.hitsz.dao.ScoreRecord;
import edu.hitsz.dao.ScoreRecordDao;
import edu.hitsz.factory.*;
import edu.hitsz.prop.AbstractProp;
import edu.hitsz.prop.BombEffectContext;
import edu.hitsz.prop.BombSubject;
import edu.hitsz.prop.BombSupply;
import edu.hitsz.prop.FreezeEffectContext;
import edu.hitsz.prop.FreezeSubject;
import edu.hitsz.prop.FreezeSupply;
import edu.hitsz.render.Sprite;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * 游戏主循环与规则引擎。
 * <p>
 * 与桌面版的区别：不再是 {@code JPanel}，也不再自己持有 {@code java.util.Timer}
 * 和鼠标监听。一次 {@link #tick()} 推进一帧，调用方（Android 侧的主线程
 * {@code Handler}，或单元测试）决定节奏；渲染与输入由 {@code GameView} 负责。
 * 这样 <b>所有</b> 游戏状态只在单一线程内变更，消灭了原实现里
 * "定时器线程改集合、EDT 读集合" 的数据竞争。
 *
 * @author hitsz
 */
public abstract class Game {

    /** 游戏结束回调，替代桌面版直接调用外层应用的页面切换。 */
    public interface GameOverListener {
        void onGameOver(String difficulty, int score, String achievementSummary);
    }

    private static final String DEFAULT_DIFFICULTY = "normal";
    private static final String DEFAULT_PLAYER_NAME = "Player";
    private static final int DEFAULT_MAX_ENEMY_COUNT = 7;
    private static final int DEFAULT_MAX_ELITE_ENEMY_COUNT = 1;
    private static final int DEFAULT_MAX_ELITE_PLUS_ENEMY_COUNT = 1;
    private static final int DEFAULT_MAX_ELITE_PRO_ENEMY_COUNT = 1;
    private static final int DEFAULT_MAX_TOTAL_ELITE_COUNT = 3;
    private static final int DEFAULT_MAX_BOSS_COUNT = 1;

    private static final double MOB_ENEMY_PROBABILITY = 0.40;
    private static final double ELITE_ENEMY_PROBABILITY = 0.25;
    private static final double ELITE_PLUS_ENEMY_PROBABILITY = 0.20;
    // 提高总掉落率，让玩家更频繁拿到补给，从资源侧进一步降低难度。
    private static final double SUPPLY_DROP_PROBABILITY = 0.30;
    private static final int BOSS_SCORE_THRESHOLD = 300;
    private static final int BOSS_SUPPLY_DROP_COUNT = 3;

    /** 一帧的固定时间步长（毫秒），与桌面版 {@code Timer} 周期一致。 */
    public static final int FRAME_INTERVAL_MILLIS = 40;

    private final Sprite backgroundImage;
    private int backgroundOffset;

    private final HeroAircraft heroAircraft;
    private final List<AbstractAircraft> enemyAircrafts;
    private final List<BaseBullet> heroBullets;
    private final List<BaseBullet> enemyBullets;
    private final List<AbstractProp> props;

    // 敌机生成周期
    protected double enemySpawnCycle = 20;
    private int enemySpawnCounter = 0;

    // 英雄机和敌机射击周期
    protected double heroShootCycle = 14;
    protected double enemyShootCycle = 28;
    private int heroShootCounter = 0;
    private int enemyShootCounter = 0;
    private int difficultyProgressCounter = 0;

    // 当前玩家分数
    private int score = 0;

    private final EnemyFactory mobEnemyFactory = new MobEnemyFactory();
    private final EnemyFactory eliteEnemyFactory = new EliteEnemyFactory();
    private final EnemyFactory elitePlusEnemyFactory = new ElitePlusEnemyFactory();
    private final EnemyFactory eliteProEnemyFactory = new EliteProEnemyFactory();

    private boolean bossAlive = false;
    private int nextBossScore;
    protected double enemySpeedMultiplier = 1.0;
    protected double enemyHpMultiplier = 1.0;

    private final String difficulty;
    private final ScoreRecordDao scoreRecordDao;
    private final GameOverListener gameOverListener;
    private final AudioManager audioManager;
    private final AchievementManager achievementManager;

    private boolean running;
    private boolean gameOverNotified;

    public Game() {
        this(DEFAULT_DIFFICULTY, null);
    }

    public Game(String difficulty) {
        this(difficulty, null);
    }

    public Game(String difficulty, GameOverListener gameOverListener) {
        this.difficulty = normalizeDifficulty(difficulty);
        this.backgroundImage = ImageManager.getBackgroundImageForDifficulty(this.difficulty);
        this.scoreRecordDao = new FileScoreRecordDao(this.difficulty);
        this.gameOverListener = gameOverListener;
        this.audioManager = AudioManager.getInstance();
        this.achievementManager = new AchievementManager();
        this.enemySpawnCycle = initialEnemySpawnCycle();
        this.heroShootCycle = initialHeroShootCycle();
        this.enemyShootCycle = initialEnemyShootCycle();
        this.nextBossScore = bossScoreThreshold();
        heroAircraft = HeroAircraft.getInstance();
        heroAircraft.resetForNewGame();

        enemyAircrafts = new LinkedList<>();
        heroBullets = new LinkedList<>();
        enemyBullets = new LinkedList<>();
        props = new LinkedList<>();
    }

    /**
     * 启动背景音乐并标记为运行中。真正的推进由外部循环调用 {@link #tick()} 完成。
     */
    public final void start() {
        if (running) {
            return;
        }
        running = true;
        audioManager.stopAllLoops();
        audioManager.playLoop(AudioManager.BGM, AudioManager.BGM_PATH);
    }

    /**
     * 停止接受 {@link #tick()} 并释放音频。多次调用安全。
     */
    public final void stop() {
        running = false;
        audioManager.stopAllLoops();
    }

    public final boolean isRunning() {
        return running;
    }

    /**
     * 推进一帧：生成敌机、射击、移动、碰撞、难度成长、清理与结束判定。
     * 游戏结束或未启动时为空操作，保证外部循环不需要额外判空。
     */
    public final void tick() {
        if (!running || gameOverNotified) {
            return;
        }

        enemySpawnCounter++;
        if (enemySpawnCounter >= enemySpawnCycle) {
            enemySpawnCounter = 0;
            boolean spawnedElite = false;
            for (int i = 0; i < 2; i++) {
                AbstractAircraft newEnemy = createEnemyAircraft(spawnedElite);
                if (newEnemy != null) {
                    enemyAircrafts.add(newEnemy);
                    if (isEliteEnemy(newEnemy)) {
                        spawnedElite = true;
                    }
                }
            }
        }
        createBossWhenNeeded();

        // 飞机发射子弹
        shootAction();
        // 子弹移动
        bulletsMoveAction();
        // 飞机移动
        aircraftsMoveAction();
        // 撞击检测
        crashCheckAction();
        updateDifficultyProgression();
        // 后处理
        postProcessAction();
        // 背景滚动
        backgroundOffset = (backgroundOffset + 1) % World.HEIGHT;
        // 游戏结束检查
        checkResultAction();
    }

    // ***********************
    // 渲染层读取的状态
    // ***********************

    public HeroAircraft getHeroAircraft() {
        return heroAircraft;
    }

    public List<AbstractAircraft> getEnemyAircrafts() {
        return Collections.unmodifiableList(enemyAircrafts);
    }

    public List<BaseBullet> getHeroBullets() {
        return Collections.unmodifiableList(heroBullets);
    }

    public List<BaseBullet> getEnemyBullets() {
        return Collections.unmodifiableList(enemyBullets);
    }

    public List<AbstractProp> getProps() {
        return Collections.unmodifiableList(props);
    }

    public Sprite getBackgroundImage() {
        return backgroundImage;
    }

    /** 背景图纵向偏移，渲染层据此画上下两张拼接的滚动背景。 */
    public int getBackgroundOffset() {
        return backgroundOffset;
    }

    public int getScore() {
        return score;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public AchievementManager getAchievementManager() {
        return achievementManager;
    }

    /**
     * 触摸/鼠标拖动：把英雄机直接放到世界坐标 (x, y)，超出世界范围时忽略。
     */
    public void moveHeroTo(int x, int y) {
        if (x < 0 || x > World.WIDTH || y < 0 || y > World.HEIGHT) {
            return;
        }
        heroAircraft.setLocation(x, y);
    }

    // ***********************
    // Action 各部分
    // ***********************

    private void shootAction() {
        // 将英雄机和敌机的开火节奏拆开，便于分别调节难度。
        heroShootCounter++;
        if (heroShootCounter >= heroShootCycle) {
            heroShootCounter = 0;
            heroBullets.addAll(heroAircraft.shoot());
        }

        enemyShootCounter++;
        if (enemyShootCounter >= enemyShootCycle) {
            enemyShootCounter = 0;
            for (AbstractAircraft enemyAircraft : enemyAircrafts) {
                if (enemyAircraft.notValid()) {
                    continue;
                }
                enemyBullets.addAll(enemyAircraft.shoot());
            }
        }
    }

    private void bulletsMoveAction() {
        for (BaseBullet bullet : heroBullets) {
            bullet.forward();
        }
        for (BaseBullet bullet : enemyBullets) {
            bullet.forward();
        }
    }

    private void aircraftsMoveAction() {
        for (AbstractAircraft enemyAircraft : enemyAircrafts) {
            enemyAircraft.forward();
        }
        for (AbstractProp prop : props) {
            prop.forward();
        }
    }

    /**
     * 碰撞检测：
     * 1. 敌机攻击英雄
     * 2. 英雄攻击/撞击敌机
     * 3. 英雄获得补给
     */
    private void crashCheckAction() {
        // 先处理子弹互相抵消，避免已经碰撞失效的子弹继续参与后续命中判定。
        resolveBulletCollisions(heroBullets, enemyBullets, enemyAircrafts);

        for (BaseBullet bullet : enemyBullets) {
            if (bullet.notValid()) {
                continue;
            }
            if (heroAircraft.crash(bullet) || bullet.crash(heroAircraft)) {
                heroAircraft.decreaseHp(bullet.getPower());
                bullet.vanish();
            }
        }

        for (BaseBullet bullet : heroBullets) {
            if (bullet.notValid()) {
                continue;
            }
            for (AbstractAircraft enemyAircraft : enemyAircrafts) {
                if (enemyAircraft.notValid()) {
                    // 已被其他子弹击毁的敌机，不再检测
                    // 避免多个子弹重复击毁同一敌机的判定
                    continue;
                }

                if (enemyAircraft.crash(bullet)) {
                    audioManager.playEffect(AudioManager.BULLET_HIT_PATH);
                    enemyAircraft.decreaseHp(bullet.getPower());
                    bullet.vanish();
                    if (enemyAircraft.notValid()) {
                        addScore(10);
                        if (enemyAircraft instanceof EliteProEnemy) {
                            achievementManager.unlock(Achievement.ACE_HUNTER);
                        }
                        createSupplyAfterEnemyCrash(enemyAircraft);
                    }
                }
            }
        }

        for (AbstractAircraft enemyAircraft : enemyAircrafts) {
            if (enemyAircraft.notValid()) {
                continue;
            }
            if (enemyAircraft.crash(heroAircraft) || heroAircraft.crash(enemyAircraft)) {
                enemyAircraft.vanish();
                createSupplyAfterEnemyCrash(enemyAircraft);
                heroAircraft.decreaseHp(Integer.MAX_VALUE);
            }
        }

        for (AbstractProp prop : props) {
            if (prop.notValid()) {
                continue;
            }
            if (heroAircraft.crash(prop) || prop.crash(heroAircraft)) {
                if (prop instanceof BombSupply) {
                    BombEffectContext context = triggerBombEffectForObservers(enemyAircrafts, enemyBullets);
                    addScore(context.getScoreBonus());
                    unlockBombSweep(context.getScoreBonus());
                } else if (prop instanceof FreezeSupply) {
                    FreezeEffectContext context = triggerFreezeEffectForObservers(enemyAircrafts, enemyBullets);
                    unlockFreezeControl(context.getAffectedTargetCount());
                }
                prop.active(heroAircraft);
                audioManager.playEffect(AudioManager.GET_SUPPLY_PATH);
                prop.vanish();
            }
        }

    }

    /**
     * 后处理：
     * 1. 删除无效的子弹
     * 2. 删除无效的敌机
     * 3. 删除无效的道具
     */
    private void postProcessAction() {
        boolean hadBossAlive = bossAlive;
        enemyBullets.removeIf(AbstractFlyingObject::notValid);
        heroBullets.removeIf(AbstractFlyingObject::notValid);
        enemyAircrafts.removeIf(AbstractFlyingObject::notValid);
        props.removeIf(AbstractFlyingObject::notValid);
        bossAlive = enemyAircrafts.stream().anyMatch(aircraft -> aircraft instanceof BossEnemy);
        if (hadBossAlive && !bossAlive) {
            // Boss 被击败后恢复普通 BGM，避免继续停留在 Boss 音乐状态。
            audioManager.stop(AudioManager.BOSS_BGM);
            audioManager.playLoop(AudioManager.BGM, AudioManager.BGM_PATH);
            achievementManager.unlock(Achievement.FIRST_BOSS_DOWN);
        }
    }

    /**
     * 检查英雄机是否阵亡；阵亡时只结算一次。
     */
    private void checkResultAction() {
        if (heroAircraft.getHp() > 0 || gameOverNotified) {
            return;
        }
        gameOverNotified = true;
        running = false;
        audioManager.stopAllLoops();
        audioManager.playEffect(AudioManager.GAME_OVER_PATH);
        String achievementSummary = achievementManager.summary();
        scoreRecordDao.add(buildDefaultScoreRecord(score));
        if (gameOverListener != null) {
            gameOverListener.onGameOver(difficulty, score, achievementSummary);
        }
    }

    static ScoreRecord buildDefaultScoreRecord(int score) {
        return ScoreRecord.ofNow(DEFAULT_PLAYER_NAME, score);
    }

    private String normalizeDifficulty(String rawDifficulty) {
        if (rawDifficulty == null || rawDifficulty.trim().isEmpty()) {
            return DEFAULT_DIFFICULTY;
        }
        return rawDifficulty.trim().toLowerCase();
    }

    private AbstractAircraft createEnemyAircraft(boolean forceMob) {
        int locationX = (int) (Math.random() * (World.WIDTH - ImageManager.MOB_ENEMY_IMAGE.getWidth()));
        int locationY = (int) (Math.random() * World.HEIGHT * 0.05);
        EnemyFactory enemyFactory = forceMob ? mobEnemyFactory : chooseEnemyFactory();
        AbstractAircraft candidate = enemyFactory.createEnemy(locationX, locationY);
        scaleEnemyForDifficulty(candidate);
        // 如果该轮随机到的敌机会突破共存上限，就直接跳过这次生成以降低屏幕压力。
        return canSpawnEnemyInCurrentDifficulty(candidate) ? candidate : null;
    }

    static boolean canSpawnEnemy(List<AbstractAircraft> currentEnemies, AbstractAircraft candidate) {
        if (candidate == null) {
            return false;
        }

        long aliveEnemyCount = countActiveEnemies(currentEnemies);
        if (aliveEnemyCount >= DEFAULT_MAX_ENEMY_COUNT) {
            return false;
        }
        if (candidate instanceof EliteEnemy) {
            return countActiveEnemiesOfType(currentEnemies, EliteEnemy.class) < DEFAULT_MAX_ELITE_ENEMY_COUNT
                    && countActiveEliteEnemies(currentEnemies) < DEFAULT_MAX_TOTAL_ELITE_COUNT;
        }
        if (candidate instanceof ElitePlusEnemy) {
            return countActiveEnemiesOfType(currentEnemies, ElitePlusEnemy.class) < DEFAULT_MAX_ELITE_PLUS_ENEMY_COUNT
                    && countActiveEliteEnemies(currentEnemies) < DEFAULT_MAX_TOTAL_ELITE_COUNT;
        }
        if (candidate instanceof EliteProEnemy) {
            return countActiveEnemiesOfType(currentEnemies, EliteProEnemy.class) < DEFAULT_MAX_ELITE_PRO_ENEMY_COUNT
                    && countActiveEliteEnemies(currentEnemies) < DEFAULT_MAX_TOTAL_ELITE_COUNT;
        }
        if (candidate instanceof BossEnemy) {
            return countActiveEnemiesOfType(currentEnemies, BossEnemy.class) < DEFAULT_MAX_BOSS_COUNT;
        }
        return true;
    }

    private boolean canSpawnEnemyInCurrentDifficulty(AbstractAircraft candidate) {
        if (candidate == null) {
            return false;
        }

        long aliveEnemyCount = countActiveEnemies(enemyAircrafts);
        if (aliveEnemyCount >= maxEnemyCount()) {
            return false;
        }
        if (candidate instanceof EliteEnemy) {
            return countActiveEnemiesOfType(enemyAircrafts, EliteEnemy.class) < maxEliteEnemyCount()
                    && countActiveEliteEnemies(enemyAircrafts) < maxTotalEliteCount();
        }
        if (candidate instanceof ElitePlusEnemy) {
            return countActiveEnemiesOfType(enemyAircrafts, ElitePlusEnemy.class) < maxElitePlusEnemyCount()
                    && countActiveEliteEnemies(enemyAircrafts) < maxTotalEliteCount();
        }
        if (candidate instanceof EliteProEnemy) {
            return countActiveEnemiesOfType(enemyAircrafts, EliteProEnemy.class) < maxEliteProEnemyCount()
                    && countActiveEliteEnemies(enemyAircrafts) < maxTotalEliteCount();
        }
        if (candidate instanceof BossEnemy) {
            return countActiveEnemiesOfType(enemyAircrafts, BossEnemy.class) < maxBossCount();
        }
        return true;
    }

    static boolean resolveBulletCollisions(List<BaseBullet> heroBullets, List<BaseBullet> enemyBullets) {
        return resolveBulletCollisions(heroBullets, enemyBullets, null);
    }

    static boolean resolveBulletCollisions(List<BaseBullet> heroBullets, List<BaseBullet> enemyBullets,
            List<? extends AbstractAircraft> enemies) {
        boolean collisionHappened = false;
        for (BaseBullet heroBullet : heroBullets) {
            if (heroBullet == null || heroBullet.notValid()) {
                continue;
            }
            for (BaseBullet enemyBullet : enemyBullets) {
                if (enemyBullet == null || enemyBullet.notValid()) {
                    continue;
                }
                if (enemyBullet.isFrozenBySupply() && overlapsActiveEnemy(enemyBullet, enemies)) {
                    continue;
                }
                if (heroBullet.crash(enemyBullet) || enemyBullet.crash(heroBullet)) {
                    heroBullet.vanish();
                    enemyBullet.vanish();
                    collisionHappened = true;
                    break;
                }
            }
        }
        return collisionHappened;
    }

    private static boolean overlapsActiveEnemy(BaseBullet bullet, List<? extends AbstractAircraft> enemies) {
        if (enemies == null || enemies.isEmpty()) {
            return false;
        }
        for (AbstractAircraft enemy : enemies) {
            if (enemy != null && !enemy.notValid() && (enemy.crash(bullet) || bullet.crash(enemy))) {
                return true;
            }
        }
        return false;
    }

    static BombEffectContext triggerBombEffectForObservers(List<? extends AbstractAircraft> enemies,
            List<? extends BaseBullet> bullets) {
        BombSubject subject = new BombSubject();
        registerActiveObservers(subject, enemies, bullets);
        return subject.trigger();
    }

    static FreezeEffectContext triggerFreezeEffectForObservers(List<? extends AbstractAircraft> enemies,
            List<? extends BaseBullet> bullets) {
        FreezeEffectContext context = new FreezeEffectContext();
        FreezeSubject subject = new FreezeSubject(context);
        registerActiveObservers(subject, enemies, bullets);
        return subject.trigger();
    }

    private static void registerActiveObservers(edu.hitsz.prop.PropSubject subject,
            List<? extends AbstractAircraft> enemies,
            List<? extends BaseBullet> bullets) {
        for (AbstractAircraft enemy : enemies) {
            if (enemy != null && !enemy.notValid()) {
                subject.addObserver(enemy);
            }
        }
        for (BaseBullet bullet : bullets) {
            if (bullet != null && !bullet.notValid()) {
                subject.addObserver(bullet);
            }
        }
    }

    private static boolean isEliteEnemy(AbstractAircraft enemy) {
        return enemy instanceof EliteEnemy || enemy instanceof ElitePlusEnemy || enemy instanceof EliteProEnemy;
    }

    private static long countActiveEnemies(List<AbstractAircraft> currentEnemies) {
        return currentEnemies.stream().filter(Objects::nonNull).filter(enemy -> !enemy.notValid()).count();
    }

    private static long countActiveEliteEnemies(List<AbstractAircraft> currentEnemies) {
        return currentEnemies.stream()
                .filter(Objects::nonNull)
                .filter(enemy -> !enemy.notValid())
                .filter(enemy -> enemy instanceof EliteEnemy
                        || enemy instanceof ElitePlusEnemy
                        || enemy instanceof EliteProEnemy)
                .count();
    }

    private static long countActiveEnemiesOfType(List<AbstractAircraft> currentEnemies,
            Class<? extends AbstractAircraft> enemyType) {
        return currentEnemies.stream()
                .filter(Objects::nonNull)
                .filter(enemy -> !enemy.notValid())
                .filter(enemyType::isInstance)
                .count();
    }

    private EnemyFactory chooseEnemyFactory() {
        double random = Math.random();
        // 概率分段只控制敌机类型，具体属性由各自工厂和类默认值决定。
        if (random < mobEnemyProbability()) {
            return mobEnemyFactory;
        }
        if (random < mobEnemyProbability() + eliteEnemyProbability()) {
            return eliteEnemyFactory;
        }
        if (random < mobEnemyProbability() + eliteEnemyProbability() + elitePlusEnemyProbability()) {
            return elitePlusEnemyFactory;
        }
        return eliteProEnemyFactory;
    }

    private void createBossWhenNeeded() {
        if (!canCreateBoss() || bossAlive || score < nextBossScore) {
            return;
        }
        // 使用 nextBossScore 逐段推进，避免一次高分跨越后重复刷出多个 Boss。
        int locationX = World.WIDTH / 2;
        int locationY = ImageManager.BOSS_ENEMY_IMAGE.getHeight() / 2;
        AbstractAircraft bossEnemy = createBossEnemy(locationX, locationY);
        if (!canSpawnEnemyInCurrentDifficulty(bossEnemy)) {
            return;
        }
        enemyAircrafts.add(bossEnemy);
        bossAlive = true;
        audioManager.stop(AudioManager.BGM);
        audioManager.playLoop(AudioManager.BOSS_BGM, AudioManager.BOSS_BGM_PATH);
        nextBossScore += bossScoreThreshold();
    }

    private void createSupplyAfterEnemyCrash(AbstractAircraft enemyAircraft) {
        int locationX = enemyAircraft.getLocationX();
        int locationY = enemyAircraft.getLocationY();
        if (enemyAircraft instanceof BossEnemy) {
            // Boss 固定掉落多个高级补给，和普通敌机的概率掉落逻辑区分开。
            int dropCount = bossSupplyDropCount();
            for (int i = 0; i < dropCount; i++) {
                int offsetX = (int) Math.round((i - (dropCount - 1) / 2.0) * 36);
                int offsetY = (i % 2 == 0) ? 0 : 32;
                int propX = Math.max(20, Math.min(World.WIDTH - 20, locationX + offsetX));
                int propY = Math.max(20, Math.min(World.HEIGHT - 20, locationY + offsetY));
                props.add(PropFactory.createRandomForAce(propX, propY));
            }
            return;
        }
        if (!props.isEmpty()) {
            return;
        }
        if (Math.random() >= supplyDropProbability()) {
            return;
        }
        if (enemyAircraft instanceof EliteProEnemy) {
            props.add(PropFactory.createRandomForAce(locationX, locationY));
        } else if (enemyAircraft instanceof ElitePlusEnemy) {
            props.add(PropFactory.createRandomForElite(locationX, locationY));
        } else if (enemyAircraft instanceof EliteEnemy) {
            props.add(PropFactory.createRandomForBasicElite(locationX, locationY));
        }
    }

    private void scaleEnemyForDifficulty(AbstractAircraft candidate) {
        if (candidate == null || candidate instanceof BossEnemy) {
            return;
        }
        candidate.multiplySpeed(enemySpeedMultiplier);
        candidate.increaseMaxHp(enemyHpMultiplier);
    }

    private void updateDifficultyProgression() {
        difficultyProgressCounter++;
        if (difficultyProgressCounter >= difficultyProgressCycle()) {
            difficultyProgressCounter = 0;
            applyDifficultyProgression();
        }
    }

    protected double initialEnemySpawnCycle() {
        return 20;
    }

    protected double initialHeroShootCycle() {
        return 14;
    }

    protected double initialEnemyShootCycle() {
        return 28;
    }

    protected int maxEnemyCount() {
        return DEFAULT_MAX_ENEMY_COUNT;
    }

    protected int maxEliteEnemyCount() {
        return DEFAULT_MAX_ELITE_ENEMY_COUNT;
    }

    protected int maxElitePlusEnemyCount() {
        return DEFAULT_MAX_ELITE_PLUS_ENEMY_COUNT;
    }

    protected int maxEliteProEnemyCount() {
        return DEFAULT_MAX_ELITE_PRO_ENEMY_COUNT;
    }

    protected int maxTotalEliteCount() {
        return DEFAULT_MAX_TOTAL_ELITE_COUNT;
    }

    protected int maxBossCount() {
        return DEFAULT_MAX_BOSS_COUNT;
    }

    protected double mobEnemyProbability() {
        return MOB_ENEMY_PROBABILITY;
    }

    protected double eliteEnemyProbability() {
        return ELITE_ENEMY_PROBABILITY;
    }

    protected double elitePlusEnemyProbability() {
        return ELITE_PLUS_ENEMY_PROBABILITY;
    }

    protected int bossScoreThreshold() {
        return BOSS_SCORE_THRESHOLD;
    }

    protected double supplyDropProbability() {
        return SUPPLY_DROP_PROBABILITY;
    }

    protected int bossSupplyDropCount() {
        return BOSS_SUPPLY_DROP_COUNT;
    }

    protected boolean canCreateBoss() {
        return true;
    }

    protected AbstractAircraft createBossEnemy(int locationX, int locationY) {
        return BossEnemy.createDefault(locationX, locationY);
    }

    protected int difficultyProgressCycle() {
        return 250;
    }

    protected void applyDifficultyProgression() {
    }

    private void addScore(int value) {
        score += value;
        if (score >= 1000) {
            achievementManager.unlock(Achievement.SCORE_1000);
        }
    }

    private void unlockBombSweep(int scoreBonus) {
        if (scoreBonus >= 30) {
            achievementManager.unlock(Achievement.BOMB_SWEEP);
        }
    }

    private void unlockFreezeControl(int affectedTargetCount) {
        if (affectedTargetCount >= 3) {
            achievementManager.unlock(Achievement.FROST_CONTROL);
        }
    }

    // ***********************
    // 测试钩子：仅同包测试可见
    // ***********************

    AbstractAircraft createBossForTest(int score) {
        if (!canCreateBoss() || score < bossScoreThreshold()) {
            return null;
        }
        return createBossEnemy(World.WIDTH / 2, ImageManager.BOSS_ENEMY_IMAGE.getHeight() / 2);
    }

    void applyDifficultyProgressionForTest() {
        applyDifficultyProgression();
    }

    double getEnemySpawnCycleForTest() {
        return enemySpawnCycle;
    }

    double getHeroShootCycleForTest() {
        return heroShootCycle;
    }

    void scaleEnemyForTest(AbstractAircraft aircraft) {
        scaleEnemyForDifficulty(aircraft);
    }

    double getSupplyDropProbabilityForTest() {
        return supplyDropProbability();
    }

    int getBossSupplyDropCountForTest() {
        return bossSupplyDropCount();
    }

    AchievementManager getAchievementManagerForTest() {
        return achievementManager;
    }

    void addScoreForTest(int value) {
        addScore(value);
    }

    void unlockBombSweepForTest(int scoreBonus) {
        unlockBombSweep(scoreBonus);
    }

    void unlockFreezeControlForTest(int affectedTargetCount) {
        unlockFreezeControl(affectedTargetCount);
    }

    List<AbstractAircraft> enemyAircraftsForTest() {
        return enemyAircrafts;
    }

    List<BaseBullet> heroBulletsForTest() {
        return heroBullets;
    }

    List<BaseBullet> enemyBulletsForTest() {
        return enemyBullets;
    }

    List<AbstractProp> propsForTest() {
        return props;
    }

    void createSupplyAfterEnemyCrashForTest(AbstractAircraft enemyAircraft) {
        createSupplyAfterEnemyCrash(enemyAircraft);
    }

    void crashCheckForTest() {
        crashCheckAction();
    }
}
