package edu.hitsz.ui;

import edu.hitsz.dao.ScoreRecord;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 排行榜行映射与成就文案。
 * <p>
 * 这个类与桌面版 {@code LeaderboardTableModel} 承担同样的职责：名次从 1 开始、
 * 顺序沿用 DAO 的排序结果、时间用固定格式。它是纯数据映射，不碰 Android，
 * 所以能直接在 JVM 单测里跑。
 */
class LeaderboardRowsTest {

    @Test
    void mapsRecordsToRowsStartingAtRankOne() {
        List<ScoreRecord> records = List.of(
                new ScoreRecord("Alice", 300, LocalDateTime.of(2026, 4, 24, 10, 0, 0)),
                new ScoreRecord("Bob", 200, LocalDateTime.of(2026, 4, 24, 10, 1, 0)));

        List<LeaderboardRows.Row> rows = LeaderboardRows.fromRecords(records);

        assertEquals(2, rows.size());
        assertEquals(1, rows.get(0).getRank());
        assertEquals("Alice", rows.get(0).getPlayerName());
        assertEquals(300, rows.get(0).getScore());
        assertEquals("2026-04-24 10:00:00", rows.get(0).getFormattedTime());
        assertEquals(2, rows.get(1).getRank());
        assertEquals("Bob", rows.get(1).getPlayerName());
    }

    @Test
    void rowDisplayLineCarriesRankNameScoreAndTime() {
        List<LeaderboardRows.Row> rows = LeaderboardRows.fromRecords(
                List.of(new ScoreRecord("Zoe", 42, LocalDateTime.of(2026, 11, 23, 1, 2, 3))));

        String line = rows.get(0).toDisplayLine();

        assertTrue(line.contains("1."), "rank should be visible");
        assertTrue(line.contains("Zoe"), "player name should be visible");
        assertTrue(line.contains("42"), "score should be visible");
        assertTrue(line.contains("2026-11-23 01:02:03"), "timestamp should be visible");
    }

    @Test
    void emptyAndNullInputsProduceNoRows() {
        assertEquals(0, LeaderboardRows.fromRecords(null).size());
        assertEquals(0, LeaderboardRows.fromRecords(new ArrayList<>()).size());
    }

    @Test
    void achievementSummaryFallsBackWhenEmpty() {
        assertEquals("Achievements: Score 1000, Bomb Sweep",
                LeaderboardRows.formatAchievementSummary("Score 1000, Bomb Sweep"));
        assertEquals("Achievements: No achievements unlocked", LeaderboardRows.formatAchievementSummary(""));
        assertEquals("Achievements: No achievements unlocked", LeaderboardRows.formatAchievementSummary(null));
        assertEquals("Achievements: No achievements unlocked", LeaderboardRows.formatAchievementSummary("   "));
    }

    @Test
    void rankOrderFollowsInputOrderNotScoreOrder() {
        // 排序是 DAO 的职责；这里只做映射，不应该偷偷重排，否则名次会与榜单显示不一致。
        List<ScoreRecord> records = List.of(
                new ScoreRecord("Low", 10, LocalDateTime.of(2026, 1, 1, 0, 0, 0)),
                new ScoreRecord("High", 999, LocalDateTime.of(2026, 1, 2, 0, 0, 0)));

        List<LeaderboardRows.Row> rows = LeaderboardRows.fromRecords(records);

        assertEquals("Low", rows.get(0).getPlayerName());
        assertEquals(1, rows.get(0).getRank());
        assertEquals("High", rows.get(1).getPlayerName());
        assertEquals(2, rows.get(1).getRank());
    }
}
