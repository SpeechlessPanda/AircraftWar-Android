package edu.hitsz.application;

/**
 * 按难度创建对应 {@link Game} 子类，保持桌面版的工厂入口签名。
 */
public final class GameFactory {

    private GameFactory() {
    }

    public static Game create(String difficulty) {
        return create(difficulty, null);
    }

    public static Game create(String difficulty, Game.GameOverListener listener) {
        if (difficulty == null) {
            return new NormalGame(listener);
        }
        switch (difficulty.trim().toLowerCase()) {
            case "easy":
                return new EasyGame(listener);
            case "hard":
                return new HardGame(listener);
            case "normal":
            default:
                return new NormalGame(listener);
        }
    }
}
