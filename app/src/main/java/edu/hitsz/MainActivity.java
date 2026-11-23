package edu.hitsz;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import edu.hitsz.application.Game;
import edu.hitsz.application.GameFactory;
import edu.hitsz.application.ImageManager;
import edu.hitsz.dao.ScoreStorage;
import edu.hitsz.platform.DrawableSpriteLoader;
import edu.hitsz.ui.BaseGame;

/**
 * 实验二第 3 步：游戏循环迁移。
 * <p>
 * 这一版把 {@link BaseGame}（SurfaceView + 绘制线程）挂到 Activity 上，
 * 验证点是"能看到一帧静止画面（背景 + 英雄机）"。
 * 交互与完整逻辑在步骤 4 接入。
 */
public class MainActivity extends Activity implements Game.GameOverListener {

    private static final String TAG = "AircraftWar";

    private BaseGame baseGame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ScoreStorage.setBaseDir(getFilesDir().toPath().resolve("scores"));
        ImageManager.init(new DrawableSpriteLoader(this));

        baseGame = new BaseGame(this);
        setContentView(baseGame);

        Game game = GameFactory.create("normal", this);
        baseGame.setGame(game);
        Log.i(TAG, "step 3: surface view attached, waiting for a static frame");
    }

    @Override
    public void onGameOver(String difficulty, int score, String achievementSummary) {
        Log.i(TAG, "game over " + difficulty + " score=" + score + " " + achievementSummary);
    }
}
