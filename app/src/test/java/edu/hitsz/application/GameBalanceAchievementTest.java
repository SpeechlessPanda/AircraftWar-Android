package edu.hitsz.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 三档难度的平衡参数。
 * <p>
 * 补给掉落率必须三档严格递减且等于既定数值；难度区分若被抹平，这里会失败。
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
