package edu.hitsz.application;

import edu.hitsz.aircraft.BossEnemy;
import edu.hitsz.aircraft.EliteEnemy;
import edu.hitsz.aircraft.ElitePlusEnemy;
import edu.hitsz.aircraft.EliteProEnemy;
import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.aircraft.MobEnemy;
import edu.hitsz.bullet.EnemyBullet;
import edu.hitsz.bullet.HeroBullet;
import edu.hitsz.prop.BloodSupply;
import edu.hitsz.prop.BombSupply;
import edu.hitsz.prop.FirePlusSupply;
import edu.hitsz.prop.FireSupply;
import edu.hitsz.prop.FreezeSupply;
import edu.hitsz.render.Sprite;
import edu.hitsz.render.SpriteLoader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 综合管理图片的加载与访问。
 * <p>
 * 与桌面版的差别只在"怎么解码"：{@code ImageIO.read(文件)} 换成由调用方注入的
 * {@link SpriteLoader}（Android 侧是 {@code BitmapFactory.decodeResource}）。
 * 注册表、懒加载与按难度取背景的接口全部保留。
 *
 * @author hitsz
 */
public final class ImageManager {

    /** 逻辑资源名，与 {@code res/drawable} 下的小写文件名一一对应。 */
    public static final String KEY_BACKGROUND = "bg";
    public static final String KEY_BACKGROUND_2 = "bg2";
    public static final String KEY_BACKGROUND_3 = "bg3";
    public static final String KEY_HERO = "hero";
    public static final String KEY_MOB_ENEMY = "mob";
    public static final String KEY_ELITE_ENEMY = "elite";
    public static final String KEY_ELITE_PLUS_ENEMY = "elite_plus";
    public static final String KEY_ELITE_PRO_ENEMY = "elite_pro";
    public static final String KEY_BOSS_ENEMY = "boss";
    public static final String KEY_HERO_BULLET = "bullet_hero";
    public static final String KEY_ENEMY_BULLET = "bullet_enemy";
    public static final String KEY_BLOOD_SUPPLY = "prop_blood";
    public static final String KEY_FIRE_SUPPLY = "prop_bullet";
    public static final String KEY_FIRE_PLUS_SUPPLY = "prop_bullet_plus";
    public static final String KEY_BOMB_SUPPLY = "prop_bomb";
    public static final String KEY_FREEZE_SUPPLY = "prop_freeze";

    /**
     * 类名-图片 映射，存储各基类的图片 <br>
     * 可使用 CLASSNAME_IMAGE_MAP.get( obj.getClass().getName() ) 获得 obj 所属基类对应的图片
     */
    private static final Map<String, Sprite> CLASSNAME_IMAGE_MAP = new HashMap<>();

    public static Sprite BACKGROUND_IMAGE;
    public static Sprite BACKGROUND_IMAGE_2;
    public static Sprite BACKGROUND_IMAGE_3;
    public static Sprite HERO_IMAGE;
    public static Sprite HERO_BULLET_IMAGE;
    public static Sprite ENEMY_BULLET_IMAGE;
    public static Sprite MOB_ENEMY_IMAGE;
    public static Sprite ELITE_ENEMY_IMAGE;
    public static Sprite ELITE_PLUS_ENEMY_IMAGE;
    public static Sprite ELITE_PRO_ENEMY_IMAGE;
    public static Sprite BOSS_ENEMY_IMAGE;
    public static Sprite BLOOD_SUPPLY_IMAGE;
    public static Sprite FIRE_SUPPLY_IMAGE;
    public static Sprite FIRE_PLUS_SUPPLY_IMAGE;
    public static Sprite BOMB_SUPPLY_IMAGE;
    public static Sprite FREEZE_SUPPLY_IMAGE;

    private static volatile boolean initialized;

    private ImageManager() {
    }

    /**
     * 注册全部图片。必须在使用任何飞行对象之前调用一次；重复调用是空操作，
     * 保证 Activity 重建与单元测试多次初始化都安全。
     */
    public static synchronized void init(SpriteLoader loader) {
        if (initialized) {
            return;
        }
        if (loader == null) {
            throw new IllegalArgumentException("loader cannot be null");
        }
        try {
            BACKGROUND_IMAGE = loader.load(KEY_BACKGROUND);
            BACKGROUND_IMAGE_2 = loader.load(KEY_BACKGROUND_2);
            BACKGROUND_IMAGE_3 = loader.load(KEY_BACKGROUND_3);

            HERO_IMAGE = loader.load(KEY_HERO);
            MOB_ENEMY_IMAGE = loader.load(KEY_MOB_ENEMY);
            ELITE_ENEMY_IMAGE = loader.load(KEY_ELITE_ENEMY);
            ELITE_PLUS_ENEMY_IMAGE = loader.load(KEY_ELITE_PLUS_ENEMY);
            ELITE_PRO_ENEMY_IMAGE = loader.load(KEY_ELITE_PRO_ENEMY);
            BOSS_ENEMY_IMAGE = loader.load(KEY_BOSS_ENEMY);
            HERO_BULLET_IMAGE = loader.load(KEY_HERO_BULLET);
            ENEMY_BULLET_IMAGE = loader.load(KEY_ENEMY_BULLET);
            BLOOD_SUPPLY_IMAGE = loader.load(KEY_BLOOD_SUPPLY);
            FIRE_SUPPLY_IMAGE = loader.load(KEY_FIRE_SUPPLY);
            FIRE_PLUS_SUPPLY_IMAGE = loader.load(KEY_FIRE_PLUS_SUPPLY);
            BOMB_SUPPLY_IMAGE = loader.load(KEY_BOMB_SUPPLY);
            FREEZE_SUPPLY_IMAGE = loader.load(KEY_FREEZE_SUPPLY);
        } catch (IOException e) {
            // 资源缺失属于部署错误，必须立刻暴露，不能带着半张资源表继续跑。
            throw new UncheckedIOException("Failed to load image resources", e);
        }

        CLASSNAME_IMAGE_MAP.put(HeroAircraft.class.getName(), HERO_IMAGE);
        CLASSNAME_IMAGE_MAP.put(MobEnemy.class.getName(), MOB_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(EliteEnemy.class.getName(), ELITE_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(ElitePlusEnemy.class.getName(), ELITE_PLUS_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(EliteProEnemy.class.getName(), ELITE_PRO_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BossEnemy.class.getName(), BOSS_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(HeroBullet.class.getName(), HERO_BULLET_IMAGE);
        CLASSNAME_IMAGE_MAP.put(EnemyBullet.class.getName(), ENEMY_BULLET_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BloodSupply.class.getName(), BLOOD_SUPPLY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(FireSupply.class.getName(), FIRE_SUPPLY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(FirePlusSupply.class.getName(), FIRE_PLUS_SUPPLY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BombSupply.class.getName(), BOMB_SUPPLY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(FreezeSupply.class.getName(), FREEZE_SUPPLY_IMAGE);

        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static Sprite get(String className) {
        return CLASSNAME_IMAGE_MAP.get(className);
    }

    public static Sprite get(Object obj) {
        if (obj == null) {
            return null;
        }
        return get(obj.getClass().getName());
    }

    public static Sprite getBackgroundImageForDifficulty(String difficulty) {
        if (difficulty == null) {
            return BACKGROUND_IMAGE_2;
        }
        // 难度与背景图保持一一对应，便于界面层只传入难度字符串。
        switch (difficulty.trim().toLowerCase()) {
            case "easy":
                return BACKGROUND_IMAGE;
            case "hard":
                return BACKGROUND_IMAGE_3;
            case "normal":
            default:
                return BACKGROUND_IMAGE_2;
        }
    }

}
