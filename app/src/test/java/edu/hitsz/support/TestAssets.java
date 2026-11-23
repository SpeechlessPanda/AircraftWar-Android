package edu.hitsz.support;

import edu.hitsz.application.ImageManager;
import edu.hitsz.render.Sprite;
import edu.hitsz.render.SpriteLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 单元测试用的图片装配。
 * <p>
 * Android 单测的编译期 bootclasspath 是 {@code android.jar}，里面没有
 * {@code java.awt} 与 {@code javax.imageio}，所以这里不用 ImageIO，直接解析
 * PNG/JPEG 的文件结构拿宽高——这正是领域层唯一需要的信息。
 * <p>
 * 断言跑在真实 {@code res/drawable-nodpi} 文件上：一旦有人把图片挪进按密度
 * 缩放的目录，或换错文件，尺寸断言会立刻失败。
 */
public final class TestAssets {

    private static final Path DRAWABLE_ROOT = Paths.get("src/main/res/drawable-nodpi");
    private static boolean initialized;

    private TestAssets() {
    }

    /** 幂等初始化：所有测试类共享同一份图片注册表。 */
    public static synchronized void ensureInitialized() {
        if (initialized) {
            return;
        }
        ImageManager.init(new HeaderOnlySpriteLoader());
        initialized = true;
    }

    public static Path drawableRoot() {
        return DRAWABLE_ROOT;
    }

    /** 返回 {@code {width, height}}。图片都很小（≤512x768），整文件读入再解析。 */
    public static int[] readImageSize(String fileName) {
        Path path = DRAWABLE_ROOT.resolve(fileName);
        try {
            return parseSize(Files.readAllBytes(path), path.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read drawable: " + path.toAbsolutePath(), e);
        }
    }

    static int[] parseSize(byte[] data, String name) throws IOException {
        if (isPng(data)) {
            // IHDR 的宽高固定在大端字节序的 16..23。
            return new int[] { readIntBigEndian(data, 16), readIntBigEndian(data, 20) };
        }
        if (isJpeg(data)) {
            return parseJpegSize(data, name);
        }
        throw new IOException("Unsupported image header: " + name);
    }

    private static boolean isPng(byte[] data) {
        return data.length >= 24
                && (data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G';
    }

    private static boolean isJpeg(byte[] data) {
        return data.length >= 2 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8;
    }

    private static int readIntBigEndian(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }

    /**
     * 从 SOI 之后逐段扫描，遇到 SOFn（0xC0..0xCF，排除 C4/C8/CC）即为尺寸所在段。
     */
    private static int[] parseJpegSize(byte[] data, String name) throws IOException {
        int index = 2;
        while (index < data.length - 1) {
            if ((data[index] & 0xFF) != 0xFF) {
                index++;
                continue;
            }
            int marker = data[index + 1] & 0xFF;
            index += 2;
            // 0xFF 填充字节。
            while (marker == 0xFF && index < data.length) {
                marker = data[index] & 0xFF;
                index++;
            }
            // 无长度字段的独立标记。
            if (marker == 0x01 || (marker >= 0xD0 && marker <= 0xD9)) {
                continue;
            }
            if (index + 2 > data.length) {
                break;
            }
            int segmentLength = ((data[index] & 0xFF) << 8) | (data[index + 1] & 0xFF);
            if (marker >= 0xC0 && marker <= 0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC) {
                // 段体：精度(1) + 高度(2) + 宽度(2)
                int height = ((data[index + 3] & 0xFF) << 8) | (data[index + 4] & 0xFF);
                int width = ((data[index + 5] & 0xFF) << 8) | (data[index + 6] & 0xFF);
                return new int[] { width, height };
            }
            index += segmentLength;
        }
        throw new IOException("No SOF segment found in JPEG: " + name);
    }

    private static final class HeaderOnlySpriteLoader implements SpriteLoader {
        @Override
        public Sprite load(String key) throws IOException {
            String fileName = key + extensionFor(key);
            Path path = DRAWABLE_ROOT.resolve(fileName);
            if (!Files.exists(path)) {
                throw new IOException("Drawable not found: " + path.toAbsolutePath());
            }
            int[] size = readImageSize(fileName);
            return new HeaderSprite(size[0], size[1]);
        }

        /** 背景是 jpg，其余是 png；文件名与 res/drawable-nodpi 一致。 */
        private String extensionFor(String key) {
            return key.startsWith("bg") ? ".jpg" : ".png";
        }
    }

    /** 只带尺寸的精灵：领域层只用到宽高，绘制由 Android 侧负责。 */
    public static final class HeaderSprite implements Sprite {
        private final int width;
        private final int height;

        HeaderSprite(int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public Object nativeImage() {
            // 单测不绘制，没有平台位图可返回。
            return null;
        }
    }
}
