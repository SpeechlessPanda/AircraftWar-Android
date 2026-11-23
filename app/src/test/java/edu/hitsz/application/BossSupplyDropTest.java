package edu.hitsz.application;

import edu.hitsz.aircraft.BossEnemy;
import edu.hitsz.aircraft.EliteEnemy;
import edu.hitsz.prop.AbstractProp;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossSupplyDropTest {

    @Test
    void bossSuppliesAreSpreadAroundBossPosition() {
        // 原测试用反射读 Game.props；迁移后 Game 暴露了包内访问器，测试不再依赖字段名。
        HardGame game = new HardGame(null);
        BossEnemy boss = BossEnemy.createDefault(240, 80);

        game.createSupplyAfterEnemyCrashForTest(boss);

        List<AbstractProp> props = game.propsForTest();
        Set<String> locations = props.stream()
                .map(prop -> prop.getLocationX() + ":" + prop.getLocationY())
                .collect(Collectors.toSet());
        assertTrue(locations.size() > 1, "Boss drops should not all overlap at the same location");
        assertEquals(4, props.size(), "HardGame drops four supplies for a boss");
    }

    @Test
    void basicEliteEnemyCanDropSupply() {
        Game game = new NormalGame(null) {
            @Override
            protected double supplyDropProbability() {
                return 1.0;
            }
        };

        game.createSupplyAfterEnemyCrashForTest(EliteEnemy.createDefault(120, 80));

        assertEquals(1, game.propsForTest().size(), "Basic elite enemies should be eligible to drop one supply");
    }
}
