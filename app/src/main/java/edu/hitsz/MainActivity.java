package edu.hitsz;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.WindowManager;
import android.widget.FrameLayout;

import edu.hitsz.application.AudioManager;
import edu.hitsz.application.Game;
import edu.hitsz.application.GameFactory;
import edu.hitsz.application.ImageManager;
import edu.hitsz.dao.ScoreStorage;
import edu.hitsz.platform.AndroidSoundPlayer;
import edu.hitsz.platform.DrawableSpriteLoader;
import edu.hitsz.ui.BaseGame;
import edu.hitsz.ui.DifficultyView;
import edu.hitsz.ui.LeaderboardView;

/**
 * 应用唯一 Activity，负责三个页面之间的切换：
 * 难度选择 → 游戏 → 排行榜 → 菜单。
 * <p>
 * 线程边界：游戏逻辑与绘制都在 {@link BaseGame} 的子线程里跑，因此
 * {@link Game.GameOverListener} 回调来自绘制线程。所有页面切换都必须
 * {@code mainHandler.post(...)} 回主线程执行，子线程不碰任何 View。
 */
public class MainActivity extends Activity implements Game.GameOverListener,
        DifficultyView.DifficultyListener, LeaderboardView.Listener {

    private static final String TAG = "AircraftWar";

    private FrameLayout root;
    private BaseGame baseGame;
    private Game currentGame;
    private AndroidSoundPlayer soundPlayer;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 资源与存储路径必须先于任何游戏对象初始化。
        ScoreStorage.setBaseDir(getFilesDir().toPath().resolve("scores"));
        ImageManager.init(new DrawableSpriteLoader(this));
        soundPlayer = new AndroidSoundPlayer(this);
        AudioManager.getInstance().install(soundPlayer);

        root = new FrameLayout(this);
        setContentView(root);

        showDifficultySelection();
    }

    // ***********************
    // 页面切换（全部在主线程）
    // ***********************

    private void showDifficultySelection() {
        stopCurrentGame();
        root.removeAllViews();
        root.addView(new DifficultyView(this, this), matchParent());
    }

    private void startGame(String difficulty) {
        stopCurrentGame();
        root.removeAllViews();

        baseGame = new BaseGame(this);
        root.addView(baseGame, matchParent());

        currentGame = GameFactory.create(difficulty, this);
        baseGame.setGame(currentGame);
        // 绘制线程在 surfaceCreated 里启动，游戏逻辑随 tick() 一起推进。
        currentGame.start();
        Log.i(TAG, "game started: " + difficulty);
    }

    private void showLeaderboard(String difficulty, Integer pendingScore, String achievementSummary) {
        stopCurrentGame();
        root.removeAllViews();
        LeaderboardView leaderboardView = new LeaderboardView(this, this);
        root.addView(leaderboardView, matchParent());
        leaderboardView.show(difficulty, pendingScore, achievementSummary);
    }

    private void stopCurrentGame() {
        if (currentGame != null) {
            currentGame.stop();
            currentGame = null;
        }
        if (baseGame != null) {
            baseGame.setGame(null);
            baseGame.stopDrawingThread();
            baseGame = null;
        }
    }

    // ***********************
    // 回调
    // ***********************

    @Override
    public void onDifficultyChosen(String difficulty) {
        startGame(difficulty);
    }

    @Override
    public void onBackToMenu() {
        showDifficultySelection();
    }

    @Override
    public void onGameOver(String difficulty, int score, String achievementSummary) {
        // 来自绘制线程：必须切回主线程再改 View。
        mainHandler.post(() -> showLeaderboard(difficulty, score, achievementSummary));
    }

    // ***********************
    // 生命周期
    // ***********************

    @Override
    protected void onPause() {
        super.onPause();
        // 退到后台时不推进逻辑也不绘制，避免空转耗电。
        if (currentGame != null) {
            currentGame.stop();
        }
        if (baseGame != null) {
            baseGame.stopDrawingThread();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 回到前台且仍停在游戏页时恢复循环：surfaceCreated 会重新拉起绘制线程。
        if (currentGame != null && baseGame != null && !currentGame.isRunning()) {
            currentGame.start();
        }
    }

    @Override
    protected void onDestroy() {
        stopCurrentGame();
        if (soundPlayer != null) {
            soundPlayer.release();
            soundPlayer = null;
        }
        AudioManager.getInstance().install(null);
        super.onDestroy();
    }

    private static FrameLayout.LayoutParams matchParent() {
        return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
    }
}
