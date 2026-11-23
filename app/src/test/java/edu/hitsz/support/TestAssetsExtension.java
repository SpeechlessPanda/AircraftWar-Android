package edu.hitsz.support;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * 全局测试扩展：任何测试开始前先把图片注册表装好。
 * <p>
 * 通过 {@code META-INF/services} 自动注册（见
 * {@code src/test/resources/META-INF/services/org.junit.jupiter.api.extension.Extension}），
 * 因此每个测试类都不需要写 {@code @BeforeAll}。
 */
public class TestAssetsExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        TestAssets.ensureInitialized();
    }
}
