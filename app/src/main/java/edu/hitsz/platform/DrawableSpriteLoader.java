package edu.hitsz.platform;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import edu.hitsz.R;
import edu.hitsz.application.ImageManager;
import edu.hitsz.render.Sprite;
import edu.hitsz.render.SpriteLoader;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 用 {@link BitmapFactory#decodeResource} 把 {@code res/drawable} 下的图片解码成精灵。
 * <p>
 * 逻辑名到资源 ID 的映射集中在这里，领域层因此不需要 import {@code R}。
 * <p>
 * <b>素材必须固定在 {@code res/drawable-nodpi}，并且这里设 {@code inScaled=false}：</b>
 * 这些图是按 512x768 逻辑世界设计的像素素材，只有在 nodpi 下解码才能得到与
 * 原始文件一致的位图尺寸（hero 100x83）。放进带密度的目录会被按屏幕密度放大到
 * 263x218，逻辑坐标写死的碰撞与摆位会整体错位。
 */
public class DrawableSpriteLoader implements SpriteLoader {

    private static final String TAG = "AircraftWar";

    private final Context context;
    private final Map<String, Integer> resourceIds = new HashMap<>();
    private final Map<String, Sprite> cache = new HashMap<>();

    public DrawableSpriteLoader(Context context) {
        // 只持有 application context，避免静态资源表把 Activity 钉在内存里。
        this.context = context.getApplicationContext();
        resourceIds.put(ImageManager.KEY_BACKGROUND, R.drawable.bg);
        resourceIds.put(ImageManager.KEY_BACKGROUND_2, R.drawable.bg2);
        resourceIds.put(ImageManager.KEY_BACKGROUND_3, R.drawable.bg3);
        resourceIds.put(ImageManager.KEY_HERO, R.drawable.hero);
        resourceIds.put(ImageManager.KEY_MOB_ENEMY, R.drawable.mob);
        resourceIds.put(ImageManager.KEY_ELITE_ENEMY, R.drawable.elite);
        resourceIds.put(ImageManager.KEY_ELITE_PLUS_ENEMY, R.drawable.elite_plus);
        resourceIds.put(ImageManager.KEY_ELITE_PRO_ENEMY, R.drawable.elite_pro);
        resourceIds.put(ImageManager.KEY_BOSS_ENEMY, R.drawable.boss);
        resourceIds.put(ImageManager.KEY_HERO_BULLET, R.drawable.bullet_hero);
        resourceIds.put(ImageManager.KEY_ENEMY_BULLET, R.drawable.bullet_enemy);
        resourceIds.put(ImageManager.KEY_BLOOD_SUPPLY, R.drawable.prop_blood);
        resourceIds.put(ImageManager.KEY_FIRE_SUPPLY, R.drawable.prop_bullet);
        resourceIds.put(ImageManager.KEY_FIRE_PLUS_SUPPLY, R.drawable.prop_bullet_plus);
        resourceIds.put(ImageManager.KEY_BOMB_SUPPLY, R.drawable.prop_bomb);
        resourceIds.put(ImageManager.KEY_FREEZE_SUPPLY, R.drawable.prop_freeze);
    }

    @Override
    public Sprite load(String key) throws IOException {
        Sprite cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        Integer resourceId = resourceIds.get(key);
        if (resourceId == null) {
            throw new IOException("Unknown drawable key: " + key);
        }
        BitmapFactory.Options options = new BitmapFactory.Options();
        // inScaled=false 等价于"按像素 1:1 解码"，即使资源被误放到 density 目录也不会被缩放。
        options.inScaled = false;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
        if (bitmap == null) {
            throw new IOException("Failed to decode drawable: " + key);
        }
        Log.d(TAG, "decoded " + key + " " + bitmap.getWidth() + "x" + bitmap.getHeight());
        Sprite sprite = new BitmapSprite(bitmap);
        cache.put(key, sprite);
        return sprite;
    }

    /** {@link Bitmap} 支撑的精灵。 */
    public static final class BitmapSprite implements Sprite {

        private final Bitmap bitmap;

        BitmapSprite(Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        @Override
        public int getWidth() {
            return bitmap.getWidth();
        }

        @Override
        public int getHeight() {
            return bitmap.getHeight();
        }

        @Override
        public Object nativeImage() {
            return bitmap;
        }
    }
}
