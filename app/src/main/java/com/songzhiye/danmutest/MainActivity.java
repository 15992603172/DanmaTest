package com.songzhiye.danmutest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.util.Random;

import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.IDanmakus;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.ui.widget.DanmakuView;

public class MainActivity extends AppCompatActivity {
    private boolean bShowDanma;
    private DanmakuView mDanmaView;
    private DanmakuContext mDanmaContext;
    private int mPlayingPos;
    private BaseDanmakuParser mParser = new BaseDanmakuParser() {
        @Override
        protected IDanmakus parse() {
            return new Danmakus();
        }
    };
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 200;
    private VideoView mVideo;
    private LinearLayout mLinearLayout;
    private EditText mEdit;
    private Button mBtnSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mVideo = (VideoView) findViewById(R.id.video_view);


        mDanmaView = (DanmakuView) findViewById(R.id.danmaku_view);
        mDanmaView.enableDanmakuDrawingCache(true);
        mDanmaView.setCallback(new DrawHandler.Callback() {
            @Override
            public void prepared() {
                bShowDanma = true;
                mDanmaView.start();
                generateDanmas();
            }

            @Override
            public void updateTimer(DanmakuTimer timer) {

            }

            @Override
            public void danmakuShown(BaseDanmaku danmaku) {

            }

            @Override
            public void drawingFinished() {

            }
        });

        mDanmaContext = DanmakuContext.create();
        mDanmaView.prepare(mParser, mDanmaContext);

        mLinearLayout = (LinearLayout) findViewById(R.id.operation_layout);
        mEdit = (EditText) findViewById(R.id.edit_text);
        mBtnSend = (Button) findViewById(R.id.btn_send);

        mDanmaView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mLinearLayout.getVisibility() == View.GONE)
                    mLinearLayout.setVisibility(View.VISIBLE);
                else
                    mLinearLayout.setVisibility(View.GONE);
            }
        });

        mBtnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String s = mEdit.getText().toString();
                if (!TextUtils.isEmpty(s)) {
                    addDanma(s, true);
                    mEdit.setText("");
                }
            }
        });
        //防止由于输入法的原因退出沉浸式
        getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        if (visibility == View.SYSTEM_UI_FLAG_VISIBLE) {
                            onWindowFocusChanged(true);
                        }
                    }
                });

    }

    private void generateDanmas() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (bShowDanma) {
                    int time = new Random().nextInt(300);
                    String content = "" + time + time;
                    addDanma(content, false);
                    try {
                        Thread.sleep(time);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void addDanma(String content, boolean withBorder) {
        BaseDanmaku baseDanmaku = mDanmaContext.mDanmakuFactory.createDanmaku(
                BaseDanmaku.TYPE_SCROLL_RL);
        baseDanmaku.text = content;
        baseDanmaku.padding = 5;
        baseDanmaku.textSize = sp2px(20);
        baseDanmaku.textColor = Color.WHITE;
        baseDanmaku.setTime(mDanmaView.getCurrentTime());
        if (withBorder) {
            baseDanmaku.borderColor = Color.GREEN;
        }
        mDanmaView.addDanmaku(baseDanmaku);
    }

    //????
    private int sp2px(float spValue) {
        //采用getResources().getDisplayMetrics()方式可以保证获取系统的metrics，这样获取
        //的scaledDensityk可以随着系统字体变化而变化
        //原本的计算公式是scaledDensity = density * fontScale。而fontScale受系统字体影响
        //此处应该是简化了，用scaledDensity代替fontScale
        final float fontScale = getResources().getDisplayMetrics().scaledDensity;
        return (int) (spValue * fontScale + 0.5f);
    }

    @Override
    protected void onPause() {
        if (mVideo != null && mVideo.isPlaying()) {
            mPlayingPos = mVideo.getCurrentPosition();
            mVideo.pause();
        }
        if (mDanmaView != null && mDanmaView.isPrepared())
            mDanmaView.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            Log.d("sss", "before 申请WRITE_EXTERNAL_STORAGE权限");
            //申请WRITE_EXTERNAL_STORAGE权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    WRITE_EXTERNAL_STORAGE_REQUEST_CODE);//WRITE_EXTERNAL_STORAGE_REQUEST_CODE为请求码
        } else {
            //此时已经获取读卡权限，根据进度值来决定是从头播放还是从pause处播放
            Log.d("sss", "in onResume else");
            if (mPlayingPos > 0) {
                Log.d("sss", "in onResume else if");
                mVideo.start();
                mVideo.seekTo(mPlayingPos);
                mPlayingPos = 0;
            } else {
                playVideo();
            }
        }
        if (mDanmaView != null && mDanmaView.isPrepared() && mDanmaView.isPaused())
            mDanmaView.resume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bShowDanma = false;
        if (mDanmaView != null) {
            mDanmaView.release();
            mDanmaView = null;
        }
    }

    private void playVideo() {
        String path = Environment.getExternalStorageDirectory() + "/Movies/ttt.mp4";
        Log.d("sss", "path: " + path);
        File videoFile = new File(path);
        Uri videoUri = Uri.fromFile(videoFile);
        if (videoFile.exists()) {
            Log.d("sss", "file exist!");
            mVideo.setVideoURI(videoUri);
            mVideo.start();
        } else {
            Toast.makeText(MainActivity.this, "File is not exist", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case WRITE_EXTERNAL_STORAGE_REQUEST_CODE:
                boolean sdAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                Log.d("sss", "in case:WRITE_EXTERNAL_STORAGE_REQUEST_CODE");
                if (sdAccepted) {
                    playVideo();
                } else {
                    Log.d("sss", "permission denied");
                }
                break;

        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus && Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView(); //????
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY  //????
            );
        }
    }

}
