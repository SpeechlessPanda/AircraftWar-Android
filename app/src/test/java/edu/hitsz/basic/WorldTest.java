package edu.hitsz.basic;

import edu.hitsz.support.TestAssets;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 逻辑世界尺寸契约。
 * <p>
 * 整个坐标体系都建立在这两个常量上：碰撞、出界判定、敌机生成位置、
 * 触摸坐标反算、渲染缩放比例。改动它们等于改动全部游戏手感。
 */
class WorldTest {

    @Test
    void worldMatchesDesktopWindowSize() {
        assertEquals(512, World.WIDTH);
        assertEquals(768, World.HEIGHT);
    }

    @Test
    void backgroundImageIsExactlyOneWorldTall() {
        // 背景按"上下两张拼接无缝滚动"实现，前提是背景图尺寸等于逻辑世界尺寸；
        // 一旦有人换图或让 density 缩放生效，这里会失败。
        assertArrayEquals(new int[] { 512, 768 }, TestAssets.readImageSize("bg.jpg"));
        assertArrayEquals(new int[] { 512, 768 }, TestAssets.readImageSize("bg2.jpg"));
        assertArrayEquals(new int[] { 512, 768 }, TestAssets.readImageSize("bg3.jpg"));
    }
}
