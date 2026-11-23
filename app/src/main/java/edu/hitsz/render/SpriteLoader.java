package edu.hitsz.render;

import java.io.IOException;

/**
 * 按逻辑名把图片解码成 {@link Sprite}。
 * <p>
 * Android 侧用 {@code BitmapFactory.decodeResource(context.getResources(), R.drawable.xxx)} 实现，
 * 领域层只传 {@code "hero"}、{@code "bg"} 这类逻辑名，不接触 {@code R}。
 */
public interface SpriteLoader {

    Sprite load(String key) throws IOException;
}
