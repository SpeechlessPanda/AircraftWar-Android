package edu.hitsz.ui;

import edu.hitsz.dao.ScoreRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 把 {@link ScoreRecord} 映射成排行榜行数据。
 * <p>
 * 桌面版用 {@code DefaultTableModel} 承载这个映射，Android 上表格换成列表，
 * 但"名次从 1 开始、分数降序、时间用固定格式"的展示契约保持一致。
 */
public final class LeaderboardRows {

    private LeaderboardRows() {
    }

    /** 一行排行榜的展示数据。 */
    public static final class Row {
        private final int rank;
        private final String playerName;
        private final int score;
        private final String formattedTime;

        Row(int rank, String playerName, int score, String formattedTime) {
            this.rank = rank;
            this.playerName = playerName;
            this.score = score;
            this.formattedTime = formattedTime;
        }

        public int getRank() {
            return rank;
        }

        public String getPlayerName() {
            return playerName;
        }

        public int getScore() {
            return score;
        }

        public String getFormattedTime() {
            return formattedTime;
        }

        public String toDisplayLine() {
            return rank + ".  " + playerName + "    " + score + "    " + formattedTime;
        }
    }

    public static List<Row> fromRecords(List<ScoreRecord> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        List<Row> rows = new ArrayList<>(records.size());
        for (int i = 0; i < records.size(); i++) {
            ScoreRecord record = records.get(i);
            rows.add(new Row(i + 1, record.getPlayerName(), record.getScore(), record.getFormattedTime()));
        }
        return rows;
    }

    /**
     * 回合结算后的成就描述，对应桌面版 {@code LeaderboardPanel.formatAchievementSummaryForDisplay}。
     */
    public static String formatAchievementSummary(String achievementSummary) {
        if (achievementSummary == null || achievementSummary.trim().isEmpty()) {
            return "Achievements: No achievements unlocked";
        }
        return "Achievements: " + achievementSummary.trim();
    }
}
