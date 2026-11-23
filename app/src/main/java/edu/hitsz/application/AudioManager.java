package edu.hitsz.application;

/**
 * 音频门面，集中维护资源路径并把调用转发给当前安装的 {@link SoundPlayer}。
 * <p>
 * 上层（{@code Game}、{@code BombSupply}）只认这里的常量与门面方法，
 * 不直接接触 {@code SoundPool} / {@code MediaPlayer}，因此单元测试可以替换成静音实现。
 */
public class AudioManager {

    public static final String BGM = "bgm";
    public static final String BOSS_BGM = "boss_bgm";
    public static final String BGM_PATH = "sounds/bgm.wav";
    public static final String BOSS_BGM_PATH = "sounds/bgm_boss.wav";
    public static final String BULLET_HIT_PATH = "sounds/bullet_hit.wav";
    public static final String BOMB_EXPLOSION_PATH = "sounds/bomb_explosion.wav";
    public static final String GET_SUPPLY_PATH = "sounds/get_supply.wav";
    public static final String GAME_OVER_PATH = "sounds/game_over.wav";

    private static final AudioManager INSTANCE = new AudioManager();

    /** 未安装实现前的空播放器，避免测试与预览因音频未就绪而崩溃。 */
    private static final SoundPlayer SILENT = new SoundPlayer() {
        @Override
        public void playEffect(String assetPath) {
        }

        @Override
        public void playLoop(String key, String assetPath) {
        }

        @Override
        public void stop(String key) {
        }

        @Override
        public void stopAllLoops() {
        }
    };

    private volatile SoundPlayer player = SILENT;

    private AudioManager() {
    }

    public static AudioManager getInstance() {
        return INSTANCE;
    }

    /** 安装平台播放器。传入 null 表示恢复静音实现。 */
    public void install(SoundPlayer soundPlayer) {
        this.player = soundPlayer == null ? SILENT : soundPlayer;
    }

    public void playEffect(String filePath) {
        // 一次性音效互不覆盖，不需要纳入循环键管理。
        player.playEffect(filePath);
    }

    public void playLoop(String key, String filePath) {
        player.playLoop(key, filePath);
    }

    public void stop(String key) {
        player.stop(key);
    }

    public void stopAllLoops() {
        player.stopAllLoops();
    }
}
