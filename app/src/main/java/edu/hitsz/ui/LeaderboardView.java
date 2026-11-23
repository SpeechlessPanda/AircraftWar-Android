package edu.hitsz.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import edu.hitsz.dao.FileScoreRecordDao;
import edu.hitsz.dao.ScoreRecord;
import edu.hitsz.dao.ScoreRecordDao;

import java.util.ArrayList;
import java.util.List;

/**
 * 排行榜页，对应桌面版 {@code LeaderboardPanel}：录入本局成绩、按难度展示榜单、
 * 支持删除选中记录与返回菜单。
 */
public class LeaderboardView extends LinearLayout {

    public interface Listener {
        void onBackToMenu();
    }

    private final Listener listener;
    private final TextView titleLabel;
    private final TextView achievementLabel;
    private final TextView emptyLabel;
    private final ListView listView;
    private final ArrayAdapter<String> adapter;
    private final List<LeaderboardRows.Row> rows = new ArrayList<>();
    private final TextView statusLabel;

    private String currentDifficulty = "normal";

    public LeaderboardView(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        setOrientation(VERTICAL);
        setBackgroundColor(Color.BLACK);

        titleLabel = new TextView(context);
        titleLabel.setText("Leaderboard");
        titleLabel.setTextColor(Color.WHITE);
        titleLabel.setTextSize(24);
        titleLabel.setTypeface(Typeface.DEFAULT_BOLD);
        titleLabel.setGravity(Gravity.CENTER);
        addView(titleLabel, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        achievementLabel = new TextView(context);
        achievementLabel.setTextColor(Color.LTGRAY);
        achievementLabel.setTextSize(14);
        achievementLabel.setGravity(Gravity.CENTER);
        addView(achievementLabel, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        emptyLabel = new TextView(context);
        emptyLabel.setText("No records yet");
        emptyLabel.setTextColor(Color.GRAY);
        emptyLabel.setGravity(Gravity.CENTER);
        addView(emptyLabel, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        listView = new ListView(context);
        // getCheckedItemPosition() 只有在单选模式下才返回有效下标，否则永远 -1。
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_single_choice, new ArrayList<>());
        listView.setAdapter(adapter);
        LayoutParams listParams = new LayoutParams(LayoutParams.MATCH_PARENT, 0);
        listParams.weight = 1f;
        addView(listView, listParams);

        statusLabel = new TextView(context);
        statusLabel.setTextColor(Color.YELLOW);
        statusLabel.setTextSize(12);
        addView(statusLabel, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        addView(createButtonPanel());
    }

    private LinearLayout createButtonPanel() {
        LinearLayout panel = new LinearLayout(getContext());
        panel.setOrientation(HORIZONTAL);
        panel.setGravity(Gravity.CENTER);

        Button deleteButton = new Button(getContext());
        deleteButton.setText("Delete Selected");
        deleteButton.setOnClickListener((View v) -> deleteSelectedRow());

        Button menuButton = new Button(getContext());
        menuButton.setText("Back To Menu");
        menuButton.setOnClickListener((View v) -> listener.onBackToMenu());

        LayoutParams params = new LayoutParams(0, LayoutParams.WRAP_CONTENT);
        params.weight = 1f;
        // 两个按钮等宽平分底部区域，weight 必须各自设置到自己的 LayoutParams 上。
        panel.addView(deleteButton, params);
        LayoutParams menuParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT);
        menuParams.weight = 1f;
        panel.addView(menuButton, menuParams);
        return panel;
    }

    /**
     * 展示某一难度的榜单。{@code pendingScore} 为 null 表示只看榜、不录入成绩。
     */
    public void show(String difficulty, Integer pendingScore, String achievementSummary) {
        currentDifficulty = normalizeDifficulty(difficulty);
        achievementLabel.setText(LeaderboardRows.formatAchievementSummary(achievementSummary));
        titleLabel.setText("Leaderboard - " + currentDifficulty);
        // 先把已有榜单画出来，再弹录入框，避免弹窗背后是一张空列表。
        refresh();
        if (pendingScore != null) {
            askPlayerNameAndSave(pendingScore);
        }
    }

    private void askPlayerNameAndSave(int score) {
        EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("Player");
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("Enter player name")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = input.getText() == null ? "" : input.getText().toString().trim();
                    saveScore(name.isEmpty() ? "Player" : name, score);
                })
                .show();
    }

    private void saveScore(String playerName, int score) {
        ScoreRecordDao dao = new FileScoreRecordDao(currentDifficulty);
        dao.add(new ScoreRecord(playerName, score, java.time.LocalDateTime.now()));
        refresh();
    }

    private void deleteSelectedRow() {
        int position = listView.getCheckedItemPosition();
        if (position < 0) {
            statusLabel.setText("Select a row first");
            return;
        }
        ScoreRecordDao dao = new FileScoreRecordDao(currentDifficulty);
        boolean deleted = dao.deleteByRank(position + 1);
        statusLabel.setText(deleted ? "Deleted" : "Nothing to delete");
        refresh();
    }

    private void refresh() {
        ScoreRecordDao dao = new FileScoreRecordDao(currentDifficulty);
        List<ScoreRecord> records = dao.getAllSorted();
        rows.clear();
        rows.addAll(LeaderboardRows.fromRecords(records));

        List<String> lines = new ArrayList<>(rows.size());
        for (LeaderboardRows.Row row : rows) {
            lines.add(row.toDisplayLine());
        }
        adapter.clear();
        adapter.addAll(lines);
        adapter.notifyDataSetChanged();
        emptyLabel.setVisibility(rows.isEmpty() ? VISIBLE : GONE);
        listView.clearChoices();
    }

    private static String normalizeDifficulty(String difficulty) {
        if (difficulty == null || difficulty.trim().isEmpty()) {
            return "normal";
        }
        return difficulty.trim().toLowerCase();
    }
}
