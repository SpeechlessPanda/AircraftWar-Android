package edu.hitsz;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

/**
 * 实验二第 1 步：空壳先跑起来。
 * <p>
 * 这一版只验证"能编译、能安装、能启动、Logcat 无红色错误"，
 * 游戏画面与循环在后续步骤接入。
 */
public class MainActivity extends Activity {

    private static final String TAG = "AircraftWar";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "MainActivity created (step 1: empty shell)");
    }
}
