package edu.hitsz.render;

/**
 * 位图资源的最小抽象。
 * <p>
 * 领域层只用尺寸做碰撞与摆位，不关心像素格式，也不依赖 {@code android.graphics}；
 * 真正的绘制由 {@code ui.BaseGame} 通过 {@link #nativeImage()} 取平台位图完成。
 */
public interface Sprite {

    int getWidth();

    int getHeight();

    /**
     * 平台原生位图对象。Android 上是 {@code android.graphics.Bitmap}。
     */
    Object nativeImage();
}
