package edu.hitsz.application;

import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.basic.World;
import edu.hitsz.dao.ScoreStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 迁移新增的"引擎单帧推进"契约。
 * <p>
 * 桌面版一帧由 {@code java.util.Timer} 的 TimerTask 触发；迁移后改由渲染线程
 * 每帧调用 {@link Game#tick()}。这些测试守住三件事：没 start 不能动、start 后
 * 逻辑真的在推进、结束只结算一次。
 */
class GameTickTest {

    @TempDir
    Path tempDir;

    private final AtomicInteger gameOverCallbacks = new AtomicInteger();

    @BeforeEach
    void redirectStorage() {
        // Game 构造会直接 new FileScoreRecordDao，必须先把存储指到临时目录。
        ScoreStorage.setBaseDir(tempDir);
        HeroAircraft.getInstance().resetForNewGame();
    }

    @AfterEach
    void restoreHero() {
        HeroAircraft.getInstance().resetForNewGame();
    }

    @Test
    void tickDoesNothingBeforeStart() {
        NormalGame game = new NormalGame(null);

        int enemiesBefore = game.getEnemyAircrafts().size();
        int bulletsBefore = game.getHeroBullets().size();
        for (int i = 0; i < 60; i++) {
            game.tick();
        }

        assertEquals(enemiesBefore, game.getEnemyAircrafts().size(), "未 start 时不应生成敌机");
        assertEquals(bulletsBefore, game.getHeroBullets().size(), "未 start 时不应发射子弹");
        assertFalse(game.isRunning());
    }

    @Test
    void startAndTicksProduceHeroBullets() {
        NormalGame game = new NormalGame(null);
        game.start();

        for (int i = 0; i < 60; i++) {
            game.tick();
        }

        assertTrue(game.isRunning(), "存活时应保持运行");
        // 默认英雄机射击周期 12 帧/次，60 帧足够发射，且子弹速度 -14 仍在屏幕内。
        assertFalse(game.getHeroBullets().isEmpty(), "start 后 tick 应把射击链路接回循环");
    }

    @Test
    void ticksAdvanceBackgroundOffsetWithinWorldHeight() {
        NormalGame game = new NormalGame(null);
        game.start();
        int before = game.getBackgroundOffset();

        for (int i = 0; i < 10; i++) {
            game.tick();
            assertTrue(game.getBackgroundOffset() >= 0 && game.getBackgroundOffset() < World.HEIGHT,
                    "背景偏移必须始终落在 [0, World.HEIGHT)");
        }

        assertTrue(game.getBackgroundOffset() != before, "10 帧后背景偏移应发生变化");
    }

    @Test
    void gameOverFiresExactlyOnceAndStopsFurtherTicks() {
        NormalGame game = new NormalGame((difficulty, score, summary) -> gameOverCallbacks.incrementAndGet());
        game.start();
        game.getHeroAircraft().decreaseHp(Integer.MAX_VALUE);

        game.tick();
        assertEquals(1, gameOverCallbacks.get(), "第一次 tick 应触发一次结算");
        assertFalse(game.isRunning(), "阵亡后不应继续运行");

        // 再 tick 多次，不应重复结算。
        for (int i = 0; i < 5; i++) {
            game.tick();
        }
        assertEquals(1, gameOverCallbacks.get(), "结算回调必须只触发一次");
    }

    @Test
    void moveHeroToIgnoresOutOfBoundsAndAppliesValidCoordinates() {
        NormalGame game = new NormalGame(null);
        HeroAircraft hero = game.getHeroAircraft();
        int startX = hero.getLocationX();

        game.moveHeroTo(-10, 100);
        assertEquals(startX, hero.getLocationX(), "负坐标应被忽略");
        game.moveHeroTo(World.WIDTH + 1, 100);
        assertEquals(startX, hero.getLocationX(), "超出世界宽度应被忽略");
        game.moveHeroTo(100, World.HEIGHT + 1);
        assertEquals(startX, hero.getLocationX(), "超出世界高度应被忽略");

        game.moveHeroTo(200, 300);
        assertEquals(200, hero.getLocationX());
        assertEquals(300, hero.getLocationY());
    }

    @Test
    void newGameStartsWithFreshHeroAndZeroScore() {
        NormalGame first = new NormalGame(null);
        first.start();
        first.getHeroAircraft().decreaseHp(Integer.MAX_VALUE);
        first.tick();

        NormalGame second = new NormalGame(null);

        assertEquals(0, second.getScore(), "新一局分数应从 0 开始");
        assertEquals(100, second.getHeroAircraft().getHp(), "新一局英雄机应满血");
        assertFalse(second.getHeroAircraft().notValid(), "新一局英雄机应有效");
    }
}
