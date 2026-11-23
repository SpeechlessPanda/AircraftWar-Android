package edu.hitsz.basic;

/**
 * 逻辑世界尺寸。渲染层负责把它等比映射到真实屏幕，游戏逻辑始终在
 * {@code 512 x 768} 坐标系内运行，因此所有位置、速度、碰撞与出界判定都按
 * 逻辑坐标书写，与具体设备分辨率无关。
 */
public final class World {

    public static final int WIDTH = 512;
    public static final int HEIGHT = 768;

    private World() {
    }
}
