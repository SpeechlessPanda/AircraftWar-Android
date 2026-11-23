package edu.hitsz.dao;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 排行榜存储根目录。
 * <p>
 * Android 上工作目录不可写，必须在启动时把根目录改成
 * {@code context.getFilesDir()}（见 {@code MainActivity#onCreate}）。
 * 默认值是相对路径 {@code scores}，方便单元测试与不经过 Activity 的调用。
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
