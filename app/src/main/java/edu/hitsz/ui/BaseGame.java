package edu.hitsz.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import edu.hitsz.application.Game;
import edu.hitsz.basic.AbstractFlyingObject;
import edu.hitsz.basic.World;
import edu.hitsz.render.Sprite;

import java.util.List;

/**
 * 游戏画面容器：{@link SurfaceView} + 独立绘制线程。
 * <p>
 * 为什么不用普通 View：普通 View 的绘制发生在主线程，一帧画得慢就会阻塞触摸
 * 与生命周期回调；SurfaceView 有独立的 Surface 和双缓冲，可以在子线程 lockCanvas
 * 绘制后再 unlockCanvasAndPost 提交，主线程只负责生命周期与页面切换。
 * <p>
 * 线程边界（指导书"子线程不能碰 UI"）：本类只在绘制线程里读游戏状态、画 Canvas，
 * 不改任何控件；需要通知 Activity 的事件通过主线程 Handler 转发，见
 * {@code MainActivity}。
 */
public class BaseGame extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private static final String TAG = "AircraftWar";

    /** 一帧的目标间隔，与桌面版 40ms 定时器一致。 */
    private static final long FRAME_INTERVAL_MILLIS = Game.FRAME_INTERVAL_MILLIS;

    private final SurfaceHolder holder;
    private final Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect sourceRect = new Rect();
    private final Rect targetRect = new Rect();

    private Thread drawThread;
    /** 绘制标志位：surfaceDestroyed 置 false，run() 循环据此退出。 */
    private volatile boolean drawing;
    private volatile Game game;

    /** 逻辑世界到屏幕的等比变换。 */
    private float scale = 1f;
    private float offsetX;
    private float offsetY;

    public BaseGame(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);
        setFocusable(true);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);
        textPaint.setTextSize(22f);
    }

    public void setGame(Game game) {
        this.game = game;
    }

    // ***********************
    // SurfaceHolder.Callback
    // ***********************

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surfaceCreated");
        drawing = true;
        drawThread = new Thread(this, "aircraft-war-draw");
        drawThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        // 取真实宽高做比例换算，不写死 1080x1920，换机器也不会跑出屏幕。
        scale = Math.min((float) width / World.WIDTH, (float) height / World.HEIGHT);
        offsetX = (width - World.WIDTH * scale) / 2f;
        offsetY = (height - World.HEIGHT * scale) / 2f;
        Log.d(TAG, "surfaceChanged " + width + "x" + height + " scale=" + scale);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.d(TAG, "surfaceDestroyed");
        stopDrawingThread();
    }

    /**
     * 停止绘制线程并等待它真正结束。
     * <p>
     * 只置标志位不等线程退出，会让 Activity 销毁后仍有线程持有 Surface，
     * 再进入时黑屏或闪退。
     */
    public void stopDrawingThread() {
        drawing = false;
        Thread thread = drawThread;
        drawThread = null;
        if (thread != null) {
            thread.interrupt();
            boolean joined = false;
            while (!joined) {
                try {
                    thread.join(200);
                    joined = true;
                } catch (InterruptedException e) {
                    // 自己被中断时继续等，直到绘制线程真正退出。
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    // ***********************
    // 绘制线程
    // ***********************

    @Override
    public void run() {
        long nextFrame = System.currentTimeMillis();
        while (drawing) {
            Canvas canvas = null;
            try {
                canvas = holder.lockCanvas();
                if (canvas != null) {
                    drawFrame(canvas);
                }
            } catch (IllegalStateException e) {
                // Surface 已被系统回收，正常退出循环，不当作崩溃。
                Log.w(TAG, "surface lost while drawing", e);
                break;
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }

            nextFrame += FRAME_INTERVAL_MILLIS;
            long sleep = nextFrame - System.currentTimeMillis();
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } else {
                // 掉帧时重置节拍，避免追赶式疯狂绘制。
                nextFrame = System.currentTimeMillis();
            }
        }
        Log.d(TAG, "draw loop exited");
    }

    private void drawFrame(Canvas canvas) {
        Game current = game;
        canvas.drawColor(Color.BLACK);
        if (current == null) {
            return;
        }

        // letterbox 之外用黑色铺底，内侧按比例缩放绘制逻辑世界。
        canvas.save();
        canvas.translate(offsetX, offsetY);
        canvas.scale(scale, scale);

        drawBackground(canvas, current);
        drawObjects(canvas, current.getEnemyBullets());
        drawObjects(canvas, current.getHeroBullets());
        drawObjects(canvas, current.getEnemyAircrafts());
        drawObjects(canvas, current.getProps());
        drawSprite(canvas, current.getHeroAircraft().getImage(),
                current.getHeroAircraft().getLocationX(), current.getHeroAircraft().getLocationY());
        drawScoreAndLife(canvas, current);

        canvas.restore();
    }

    private void drawBackground(Canvas canvas, Game current) {
        Bitmap bitmap = bitmapOf(current.getBackgroundImage());
        if (bitmap == null) {
            return;
        }
        // 上下两张拼接实现无缝滚动，与桌面版两次 drawImage 等价。
        int offset = current.getBackgroundOffset();
        drawBackgroundTile(canvas, bitmap, offset - World.HEIGHT);
        drawBackgroundTile(canvas, bitmap, offset);
    }

    private void drawBackgroundTile(Canvas canvas, Bitmap bitmap, int worldTop) {
        targetRect.set(0, worldTop, World.WIDTH, worldTop + World.HEIGHT);
        canvas.drawBitmap(bitmap, null, targetRect, paint);
    }

    private void drawObjects(Canvas canvas, List<? extends AbstractFlyingObject> objects) {
        for (AbstractFlyingObject object : objects) {
            drawSprite(canvas, object.getImage(), object.getLocationX(), object.getLocationY());
        }
    }

    private void drawSprite(Canvas canvas, Sprite sprite, int centerX, int centerY) {
        Bitmap bitmap = bitmapOf(sprite);
        if (bitmap == null) {
            return;
        }
        int halfWidth = sprite.getWidth() / 2;
        int halfHeight = sprite.getHeight() / 2;
        sourceRect.set(0, 0, bitmap.getWidth(), bitmap.getHeight());
        targetRect.set(centerX - halfWidth, centerY - halfHeight,
                centerX - halfWidth + sprite.getWidth(), centerY - halfHeight + sprite.getHeight());
        canvas.drawBitmap(bitmap, sourceRect, targetRect, paint);
    }

    private void drawScoreAndLife(Canvas canvas, Game current) {
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(22f);
        canvas.drawText("SCORE: " + current.getScore(), 10, 25, textPaint);
        canvas.drawText("LIFE: " + current.getHeroAircraft().getHp(), 10, 45, textPaint);
    }

    private static Bitmap bitmapOf(Sprite sprite) {
        if (sprite == null || !(sprite.nativeImage() instanceof Bitmap)) {
            return null;
        }
        return (Bitmap) sprite.nativeImage();
    }

    // ***********************
    // 坐标映射（供触摸与测试使用）
    // ***********************

    public float getRenderScale() {
        return scale;
    }

    public int toWorldX(float screenX) {
        return Math.round((screenX - offsetX) / scale);
    }

    public int toWorldY(float screenY) {
        return Math.round((screenY - offsetY) / scale);
    }
}
