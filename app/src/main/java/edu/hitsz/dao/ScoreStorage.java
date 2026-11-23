package edu.hitsz.dao;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 排行榜存储根目录。
 * <p>
 * 桌面版把榜单写到工作目录下的 {@code scores/}；Android 上工作目录不可写，
 * 必须在启动时把根目录改成 {@code context.getFilesDir()}。默认值保留
 * {@code scores}，让单元测试和不经过 Activity 的调用照旧可用。
 */
public final class ScoreStorage {

    private static volatile Path baseDir = Paths.get("scores");

    private ScoreStorage() {
    }

    public static Path baseDir() {
        return baseDir;
    }

    public static void setBaseDir(Path dir) {
        if (dir == null) {
            throw new IllegalArgumentException("baseDir cannot be null");
        }
        baseDir = dir;
    }
}
