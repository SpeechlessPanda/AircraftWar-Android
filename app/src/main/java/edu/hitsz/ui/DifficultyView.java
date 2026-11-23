package edu.hitsz.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * 难度选择页，对应桌面版的 {@code DifficultySelectPanel}。
 */
public class DifficultyView extends LinearLayout {

    /** 三个难度按钮的回调。 */
    public interface DifficultyListener {
        void onDifficultyChosen(String difficulty);
    }

    public DifficultyView(Context context, DifficultyListener listener) {
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        setBackgroundColor(Color.BLACK);

        TextView title = new TextView(context);
        title.setText("Aircraft War");
        title.setTextColor(Color.WHITE);
        title.setTextSize(32);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        addView(title, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        TextView hint = new TextView(context);
        hint.setText("Select Difficulty");
        hint.setTextColor(Color.LTGRAY);
        hint.setTextSize(18);
        hint.setGravity(Gravity.CENTER);
        LayoutParams hintParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        hintParams.topMargin = dp(8);
        hintParams.bottomMargin = dp(48);
        addView(hint, hintParams);

        addView(createButton("Easy", "easy", listener));
        addView(createButton("Normal", "normal", listener));
        addView(createButton("Hard", "hard", listener));
    }

    private Button createButton(String text, String difficulty, DifficultyListener listener) {
        Button button = new Button(getContext());
        button.setText(text);
        button.setTextSize(22);
        button.setOnClickListener((View v) -> listener.onDifficultyChosen(difficulty));
        LayoutParams params = new LayoutParams(dp(220), dp(64));
        params.topMargin = dp(12);
        button.setLayoutParams(params);
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
