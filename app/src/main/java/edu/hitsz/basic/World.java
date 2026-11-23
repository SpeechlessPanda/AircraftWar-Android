package edu.hitsz.basic;

/**
 * 逻辑世界尺寸。渲染层负责把它等比映射到真实屏幕，游戏逻辑始终在
 * {@code 512 x 768} 坐标系内运行，因此桌面版的碰撞、出界与生成位置计算
 * 可以原样保留。
 */
public final class World {

    public static final int WIDTH = 512;
    public static final int HEIGHT = 768;

    private World() {
    }
}
