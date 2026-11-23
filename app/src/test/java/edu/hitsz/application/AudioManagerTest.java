package edu.hitsz.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioManagerTest {

    private final RecordingSoundPlayer recorder = new RecordingSoundPlayer();
    private final AudioManager audioManager = AudioManager.getInstance();

    @BeforeEach
    void installRecorder() {
        audioManager.install(recorder);
    }

    @AfterEach
    void restoreSilence() {
        audioManager.install(null);
    }

    @Test
    void loopCallsCarryStableKeysAndAssetPaths() {
        audioManager.playLoop(AudioManager.BGM, AudioManager.BGM_PATH);
        audioManager.playLoop(AudioManager.BOSS_BGM, AudioManager.BOSS_BGM_PATH);

        assertEquals(List.of("bgm:sounds/bgm.wav", "boss_bgm:sounds/bgm_boss.wav"), recorder.events);
    }

    @Test
    void effectsUseSoundsFolderAndAreNotKeyed() {
        audioManager.playEffect(AudioManager.BULLET_HIT_PATH);
        audioManager.playEffect(AudioManager.GAME_OVER_PATH);

        assertEquals(List.of("effect:sounds/bullet_hit.wav", "effect:sounds/game_over.wav"), recorder.events);
    }

    @Test
    void stopAllLoopsAsksPlayerToStopEverything() {
        audioManager.stopAllLoops();

        assertEquals(List.of("stopAll"), recorder.events);
    }

    @Test
    void everyDeclaredAudioAssetExistsInBundledRawResources() {
        String[] paths = {
                AudioManager.BGM_PATH, AudioManager.BOSS_BGM_PATH, AudioManager.BULLET_HIT_PATH,
                AudioManager.BOMB_EXPLOSION_PATH, AudioManager.GET_SUPPLY_PATH, AudioManager.GAME_OVER_PATH
        };
        // 音频已按指导书迁到 res/raw；路径常量形如 sounds/bgm.wav，落盘名只取文件名。
        java.nio.file.Path rawDir = java.nio.file.Paths.get("src/main/res/raw");
        for (String path : paths) {
            String fileName = path.substring(path.lastIndexOf('/') + 1);
            assertTrue(java.nio.file.Files.exists(rawDir.resolve(fileName)),
                    "missing raw audio resource: " + fileName);
        }
    }

    private static final class RecordingSoundPlayer implements SoundPlayer {
        private final List<String> events = new ArrayList<>();

        @Override
        public void playEffect(String assetPath) {
            events.add("effect:" + assetPath);
        }

        @Override
        public void playLoop(String key, String assetPath) {
            events.add(key + ":" + assetPath);
        }

        @Override
        public void stop(String key) {
            events.add("stop:" + key);
        }

        @Override
        public void stopAllLoops() {
            events.add("stopAll");
        }
    }
}
