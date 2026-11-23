package edu.hitsz.platform;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;

import edu.hitsz.R;
import edu.hitsz.application.AudioManager;
import edu.hitsz.application.SoundPlayer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Android 音频实现：短音效走 {@link SoundPool}（低延迟、可叠加），
 * 背景音乐走 {@link MediaPlayer}（支持长音频循环）。
 * <p>
 * <p>
 * 任何单次播放失败都只记日志，不向上抛异常：音频不是可玩性的前置条件，
 * 一处解码失败不应该让整局游戏崩掉。
 */
public class AndroidSoundPlayer implements SoundPlayer {

    private static final String TAG = "AircraftWarAudio";
    private static final int MAX_STREAMS = 8;

    private final Context context;
    private final SoundPool soundPool;
    /** 逻辑路径（与 {@link AudioManager} 常量一致）→ res/raw 资源 ID。 */
    private final Map<String, Integer> rawIds = new HashMap<>();
    private final Map<String, Integer> loadedEffects = new HashMap<>();
    private final Map<String, MediaPlayer> loopPlayers = new HashMap<>();

    public AndroidSoundPlayer(Context context) {
        this.context = context.getApplicationContext();
        this.soundPool = new SoundPool.Builder()
                .setMaxStreams(MAX_STREAMS)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .build();
        rawIds.put(AudioManager.BGM_PATH, R.raw.bgm);
        rawIds.put(AudioManager.BOSS_BGM_PATH, R.raw.bgm_boss);
        rawIds.put(AudioManager.BULLET_HIT_PATH, R.raw.bullet_hit);
        rawIds.put(AudioManager.BOMB_EXPLOSION_PATH, R.raw.bomb_explosion);
        rawIds.put(AudioManager.GET_SUPPLY_PATH, R.raw.get_supply);
        rawIds.put(AudioManager.GAME_OVER_PATH, R.raw.game_over);
    }

    @Override
    public void playEffect(String assetPath) {
        Integer soundId = loadedEffects.get(assetPath);
        if (soundId == null) {
            soundId = load(assetPath);
            if (soundId == null) {
                return;
            }
        }
        soundPool.play(soundId, 1f, 1f, 1, 0, 1f);
    }

    private Integer load(String assetPath) {
        Integer resId = rawIds.get(assetPath);
        if (resId == null) {
            Log.w(TAG, "Unknown audio path: " + assetPath);
            return null;
        }
        int soundId = soundPool.load(context, resId, 1);
        if (soundId == 0) {
            Log.w(TAG, "SoundPool refused resource: " + assetPath);
            return null;
        }
        loadedEffects.put(assetPath, soundId);
        return soundId;
    }

    @Override
    public void playLoop(String key, String assetPath) {
        stop(key);
        Integer resId = rawIds.get(assetPath);
        if (resId == null) {
            Log.w(TAG, "Unknown loop path: " + assetPath);
            return;
        }
        try {
            MediaPlayer player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            player.setDataSource(context.getResources().openRawResourceFd(resId));
            player.setLooping(true);
            player.setOnErrorListener((mp, what, extra) -> {
                Log.w(TAG, "MediaPlayer error " + what + "/" + extra + " on " + assetPath);
                releaseQuietly(mp);
                return true;
            });
            player.prepare();
            player.start();
            loopPlayers.put(key, player);
        } catch (IOException | IllegalStateException e) {
            Log.w(TAG, "Failed to play loop audio: " + assetPath, e);
        }
    }

    @Override
    public void stop(String key) {
        MediaPlayer player = loopPlayers.remove(key);
        if (player != null) {
            releaseQuietly(player);
        }
    }

    @Override
    public void stopAllLoops() {
        for (String key : new HashMap<>(loopPlayers).keySet()) {
            stop(key);
        }
    }

    /** 释放全部原生资源，Activity 销毁时调用。 */
    public void release() {
        stopAllLoops();
        loadedEffects.clear();
        soundPool.release();
    }

    private static void releaseQuietly(MediaPlayer player) {
        try {
            if (player.isPlaying()) {
                player.stop();
            }
        } catch (IllegalStateException ignored) {
            // 已释放或未准备完成，直接进入 release。
        }
        player.release();
    }
}
