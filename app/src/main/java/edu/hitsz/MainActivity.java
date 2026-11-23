package edu.hitsz;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import edu.hitsz.application.ImageManager;
import edu.hitsz.platform.DrawableSpriteLoader;
import edu.hitsz.render.Sprite;

/**
 * 实验二第 2 步：资源与图片加载。
 * <p>
 * 这一步把桌面版 {@code ImageIO.read(文件)} 换成
 * {@code BitmapFactory.decodeResource(getResources(), R.drawable.xxx)}，
 * 验证点是 16 张图全部能解码并且尺寸与原始资源一致。游戏循环在第 3 步接入。
 */
public class MainActivity extends Activity {

    private static final String TAG = "AircraftWar";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        ImageManager.init(new DrawableSpriteLoader(this));
        logSprite("hero", ImageManager.HERO_IMAGE);
        logSprite("mob", ImageManager.MOB_ENEMY_IMAGE);
        logSprite("elite", ImageManager.ELITE_ENEMY_IMAGE);
        logSprite("elite_plus", ImageManager.ELITE_PLUS_ENEMY_IMAGE);
        logSprite("elite_pro", ImageManager.ELITE_PRO_ENEMY_IMAGE);
        logSprite("boss", ImageManager.BOSS_ENEMY_IMAGE);
        logSprite("bullet_hero", ImageManager.HERO_BULLET_IMAGE);
        logSprite("bullet_enemy", ImageManager.ENEMY_BULLET_IMAGE);
        logSprite("prop_blood", ImageManager.BLOOD_SUPPLY_IMAGE);
        logSprite("prop_bullet", ImageManager.FIRE_SUPPLY_IMAGE);
        logSprite("prop_bullet_plus", ImageManager.FIRE_PLUS_SUPPLY_IMAGE);
        logSprite("prop_bomb", ImageManager.BOMB_SUPPLY_IMAGE);
        logSprite("prop_freeze", ImageManager.FREEZE_SUPPLY_IMAGE);
        logSprite("bg", ImageManager.BACKGROUND_IMAGE);
        logSprite("bg2", ImageManager.BACKGROUND_IMAGE_2);
        logSprite("bg3", ImageManager.BACKGROUND_IMAGE_3);
        Log.i(TAG, "step 2: all drawables decoded");
    }

    private void logSprite(String key, Sprite sprite) {
        if (sprite == null) {
            Log.e(TAG, "sprite missing: " + key);
            return;
        }
        Log.i(TAG, "sprite " + key + " = " + sprite.getWidth() + "x" + sprite.getHeight());
    }
}
