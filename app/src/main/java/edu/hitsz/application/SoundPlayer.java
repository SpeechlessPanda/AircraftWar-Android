package edu.hitsz.application;

/**
 * 音频输出接缝。桌面版基于 {@code javax.sound.sampled.Clip}，
 * Android 版基于 {@code SoundPool} + {@code MediaPlayer}，
 * 单元测试用记录型实现。
 */
public interface SoundPlayer {

    /** 播放一次性音效，允许同一时刻多个音效叠加。 */
    void playEffect(String assetPath);

    /** 播放或替换某个键对应的循环背景音；同一键始终只有一个实例。 */
    void playLoop(String key, String assetPath);

    /** 停止指定键的循环音，不存在时为空操作。 */
    void stop(String key);

    /** 停止全部循环音。 */
    void stopAllLoops();
}
