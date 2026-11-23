package edu.hitsz.application;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.MobEnemy;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.EnemyBullet;
import edu.hitsz.bullet.HeroBullet;
import edu.hitsz.prop.FreezeEffectContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FrozenEnemyHitTest {

    @Test
    void frozenEnemyCanStillBeDestroyedWhenFrozenEnemyBulletOverlapsIt() {
        NormalGame game = new NormalGame(null);
        MobEnemy enemy = MobEnemy.createDefault(120, 120);
        enemy.onFreeze(new FreezeEffectContext(20, 20, 20, 20));
        HeroBullet heroBullet = new HeroBullet(120, 120, 0, -10, 40);
        EnemyBullet frozenEnemyBullet = new EnemyBullet(120, 120, 0, 5, 10);
        frozenEnemyBullet.onFreeze(new FreezeEffectContext(20, 20, 20, 20));

        List<AbstractAircraft> enemies = game.enemyAircraftsForTest();
        List<BaseBullet> heroBullets = game.heroBulletsForTest();
        List<BaseBullet> enemyBullets = game.enemyBulletsForTest();
        enemies.add(enemy);
        heroBullets.add(heroBullet);
        enemyBullets.add(frozenEnemyBullet);

        game.crashCheckForTest();

        assertTrue(enemy.notValid(), "frozen enemy should not be protected by frozen enemy bullets");
    }
}
