package edu.hitsz.ui;

import edu.hitsz.dao.ScoreRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 把 {@link ScoreRecord} 映射成排行榜行数据。
 * <p>
 * 名次从 1 开始，顺序沿用 DAO 的排序结果，时间用固定格式。
 * 本类不依赖 Android，可在 JVM 单测里直接验证。
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
     * 回合结算后的成就描述：为空时给出可读的兜底文案。
     */
    public static String formatAchievementSummary(String achievementSummary) {
        if (achievementSummary == null || achievementSummary.trim().isEmpty()) {
            return "Achievements: No achievements unlocked";
        }
        return "Achievements: " + achievementSummary.trim();
    }
}
