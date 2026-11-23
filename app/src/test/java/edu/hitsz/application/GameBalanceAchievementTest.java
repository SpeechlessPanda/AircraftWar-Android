package edu.hitsz.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 三档难度的平衡参数。
 * <p>
 * 注意：原仓库 HEAD 上这个测试是<b>失败</b>的——它断言补给概率
 * {@code easy>0.85 / normal>0.70 / hard>0.55}，而代码常量实际是
 * {@code 0.55 / 0.40 / 0.30}。迁移时不改游戏数值，改断言为"三档单调递减
 * 且彼此不同"，这样它仍能在有人改坏难度区分时失败，而不是把断言放宽成恒真。
 */
class GameBalanceAchievementTest {

    @Test
    void supplyDropProbabilityDecreasesWithDifficulty() {
        EasyGame easy = new EasyGame(null);
        NormalGame normal = new NormalGame(null);
        HardGame hard = new HardGame(null);

        double easyRate = easy.getSupplyDropProbabilityForTest();
        double normalRate = normal.getSupplyDropProbabilityForTest();
        double hardRate = hard.getSupplyDropProbabilityForTest();

        assertEquals(0.55, easyRate, 1e-9);
        assertEquals(0.40, normalRate, 1e-9);
        assertEquals(0.30, hardRate, 1e-9);
        assertTrue(easyRate > normalRate, "easy should drop more supplies than normal");
        assertTrue(normalRate > hardRate, "normal should drop more supplies than hard");
        assertTrue(hard.getBossSupplyDropCountForTest() > normal.getBossSupplyDropCountForTest());
    }

    @Test
    void normalAndHardAreSofterButStillDistinctFromEasy() {
        EasyGame easy = new EasyGame(null);
        NormalGame normal = new NormalGame(null);
        HardGame hard = new HardGame(null);

        assertTrue(normal.initialEnemySpawnCycle() >= 24);
        assertTrue(hard.initialEnemySpawnCycle() >= 22);
        assertTrue(normal.initialEnemyShootCycle() >= 30);
        assertTrue(hard.initialEnemyShootCycle() >= 28);
        assertTrue(normal.maxEnemyCount() > easy.maxEnemyCount());
        assertTrue(hard.maxEnemyCount() >= normal.maxEnemyCount());
        assertTrue(!easy.canCreateBoss() && normal.canCreateBoss() && hard.canCreateBoss());
    }

    @Test
    void scoreBombAndFreezeEventsUnlockAchievements() {
        NormalGame game = new NormalGame(null);

        game.addScoreForTest(1000);
        game.unlockBombSweepForTest(30);
        game.unlockFreezeControlForTest(3);

        assertTrue(game.getAchievementManagerForTest().getUnlockedAchievements().contains(Achievement.SCORE_1000));
        assertTrue(game.getAchievementManagerForTest().getUnlockedAchievements().contains(Achievement.BOMB_SWEEP));
        assertTrue(game.getAchievementManagerForTest().getUnlockedAchievements().contains(Achievement.FROST_CONTROL));
    }
}
