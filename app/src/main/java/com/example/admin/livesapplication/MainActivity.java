package com.example.admin.livesapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.example.admin.livesapplication.callback.OnSocketReceiveCallBack;
import com.gcssloop.widget.RockerView;
import com.google.gson.Gson;
import com.pili.pldroid.player.AVOptions;
import com.pili.pldroid.player.PlayerState;
import com.pili.pldroid.player.widget.PLVideoTextureView;
import com.pili.pldroid.player.widget.PLVideoView;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

@SuppressLint("CheckResult")
public class MainActivity extends AppCompatActivity implements View.OnClickListener, RockerView.RockerListener {

    private PLVideoTextureView mVideoView1;
    private PLVideoTextureView mVideoView2;
    private EditText urls;
    private EditText urls2;
    private SocketManager socketManager;
    private Gson gson = new Gson();
    private boolean isMove;
    private int action = -1;
    private boolean isOk = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        new RxPermissions(this).request(Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe(aBoolean -> initPlayer());

//        rtmp://live.hkstv.hk.lxdns.com/live/hks2
//        rtmp://192.168.0.149:1935/live
    }

    /**
     * 初始化控件
     */
    private void initView() {
        RockerView rockerView = findViewById(R.id.rocker);
        rockerView.setListener(this);
        rockerView.setCallbackCycle(200);
        mVideoView1 = findViewById(R.id.PLVideoView1);
        mVideoView2 = findViewById(R.id.PLVideoTextureView);
        urls = findViewById(R.id.url);
        urls2 = findViewById(R.id.url2);
        Button up = findViewById(R.id.btn_up);
        Button play = findViewById(R.id.play);
        Button play2 = findViewById(R.id.play2);

        Switch auto = findViewById(R.id.auto);
        auto.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                sendModeChangeMessage(1001, 1);
                Toast.makeText(MainActivity.this, "手动:" + DeviceMessaheUtils.getModeChangeMessage(1001, 1), Toast.LENGTH_SHORT).show();
            } else {
                sendModeChangeMessage(1002, 0);
                Toast.makeText(MainActivity.this, "自动:" + DeviceMessaheUtils.getModeChangeMessage(1002, 0), Toast.LENGTH_SHORT).show();
            }
        });

        socketManager = new SocketManager(callBack);
        up.setOnClickListener(this);
        play.setOnClickListener(this);
        play2.setOnClickListener(this);
    }

    /**
     * 初始化播放器
     */
    private void initPlayer() {
        mVideoView1.setSplitMode(PLVideoTextureView.SPLIT_MODE_HORIZONTAL, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        mVideoView2.setSplitMode(PLVideoTextureView.SPLIT_MODE_HORIZONTAL, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        MediaController mMediaController = new MediaController(this);
        mVideoView1.setMediaController(mMediaController);
        mVideoView2.setMediaController(mMediaController);
//      画面预览
        mVideoView1.setDisplayAspectRatio(PLVideoView.ASPECT_RATIO_FIT_PARENT);
        mVideoView2.setDisplayAspectRatio(PLVideoView.ASPECT_RATIO_FIT_PARENT);
//      反转
//        mVideoView1.setMirror(true);
//        mVideoView2.setMirror(true);
//        拖动进度条跳转到拖动的位置
//        mMediaController.setInstantSeeking(true);
//        不启用
        mMediaController.setEnabled(false);
//        视频流的设置
        AVOptions options = new AVOptions();
//        自动解码 硬解优先，失败后自动切换到软解
        options.setInteger(AVOptions.KEY_MEDIACODEC, AVOptions.MEDIA_CODEC_AUTO);
        // 若设置为 1，则底层会进行一些针对直播流的优化
        options.setInteger(AVOptions.KEY_LIVE_STREAMING, 1);
        // 默认的缓存大小，单位是 ms
        // 默认值是：500
        options.setInteger(AVOptions.KEY_CACHE_BUFFER_DURATION, 500);
        // 最大的缓存大小，单位是 ms
        // 默认值是：2000，若设置值小于 KEY_CACHE_BUFFER_DURATION 则不会生效
        options.setInteger(AVOptions.KEY_MAX_CACHE_BUFFER_DURATION, 4000);
        // 是否开启直播优化，1 为开启，0 为关闭。若开启，视频暂停后再次开始播放时会触发追帧机制
        // 默认为 0
        options.setInteger(AVOptions.KEY_LIVE_STREAMING, 1);
        // 快开模式，启用后会加快该播放器实例再次打开相同协议的视频流的速度
        options.setInteger(AVOptions.KEY_FAST_OPEN, 1);
        mVideoView1.setAVOptions(options);
        mVideoView2.setAVOptions(options);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVideoView1 != null) {
//            mVideoView.pause();
            mVideoView1.stopPlayback();
        }
        if (mVideoView2 != null) {
//            mVideoView.pause();
            mVideoView2.stopPlayback();
        }

        if (socketManager != null) {
            socketManager.release();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play:
                String url = urls.getText().toString();
                mVideoView1.setVideoPath(url);
                mVideoView1.start();
                break;
            case R.id.play2:
                String url2 = urls2.getText().toString();
                mVideoView2.setVideoPath(url2);
                mVideoView2.start();
                break;
            case R.id.btn_up:
                Log.e("click", "click" + mVideoView1.getRtmpVideoTimestamp());
//                mVideoView1.captureImage(mVideoView1.getRtmpVideoTimestamp());
                if (mVideoView1.getRtmpVideoTimestamp() != -1) {
                    Bitmap bitmap = mVideoView1.getTextureView().getBitmap();
                    saveImg(bitmap, mVideoView1.getRtmpVideoTimestamp() + ".jpg");
                } else {
                    Toast.makeText(MainActivity.this, "请先播放视频", Toast.LENGTH_SHORT).show();
                }

//                ivIcon.setImageBitmap(bitmap);
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mVideoView1.isPlaying()) {
            Log.e("isPlaying", "isPlaying");
            mVideoView1.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mVideoView1.getPlayerState() == PlayerState.PAUSED) {
            mVideoView1.start();
        }
    }

    private void sendMoveMessage(int action) {

        Observable.create((ObservableOnSubscribe<Socket>) e -> {
            Socket socket = socketManager.getSocket();
            if (socket != null)
                e.onNext(socket);
            e.onComplete();
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(socket -> {
                    if (socket != null) {
                        if (isOk) {
                            socketManager.sendReceiveTcpMsg(socket, DeviceMessaheUtils.getMoveMessage(action));
                            socketManager.startReceiveTcpMsg(socket);
                            isOk = false;
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void sendModeChangeMessage(int type, int action) {
        Observable.create((ObservableOnSubscribe<Socket>) e -> {
            Socket socket = socketManager.getSocket();
            if (socket != null)
                e.onNext(socket);
            e.onComplete();
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(socket -> {
                    if (socket != null) {
                        socketManager.sendReceiveTcpMsg(socket, DeviceMessaheUtils.getModeChangeMessage(type, action));
                        socketManager.startReceiveTcpMsg(socket);
                    } else {
                        Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /**
     * 回调
     */
    OnSocketReceiveCallBack callBack = msg -> {
        Log.e("msg", msg);
        Toast.makeText(MainActivity.this, "msg:" + msg, Toast.LENGTH_SHORT).show();
        Result result = gson.fromJson(msg, Result.class);
        if (result != null) {
            Result.DataBean data = result.getData();
//        Log.e("result",result.toString());
            if (result.getType() == 1001) {
//                data.getResult().equals("ok")
                if (data != null && data.getResult() == 1) {
                    Toast.makeText(MainActivity.this, "切换手动模式成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "切换手动模式失败", Toast.LENGTH_SHORT).show();
                }
            }
            if (result.getType() == 1002) {
                if (data != null && data.getResult() == 1) {

                    Toast.makeText(MainActivity.this, "切换自动模式成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "切换自动模式失败", Toast.LENGTH_SHORT).show();
                }
            }

            if (result.getType() == 1003) {
                if (data != null && data.getResult() == 1) {
                    Log.e("move", "move");
//                    Toast.makeText(MainActivity.this, "发送移动命令成功", Toast.LENGTH_SHORT).show();
                    isOk = true;
                } else {
                    Log.e("move", "move");
//                    Toast.makeText(MainActivity.this, "发送移动命令失败", Toast.LENGTH_SHORT).show();
                    isMove = false;
                }
            }
        } else {
            Log.e("msg", "msg:" + msg);
        }

    };

    /**
     * 摇杆监听回调
     *
     * @param eventType       事件类型
     * @param currentAngle    当前角度
     * @param currentDistance 当前距离
     */
    @Override
    public void callback(int eventType, int currentAngle, float currentDistance) {
        switch (eventType) {
            case RockerView.EVENT_ACTION:
                // 触摸事件回调
//                Log.e("EVENT_ACTION-------->", "angle="+currentAngle+" - distance"+currentDistance);
                if (currentAngle != -1 && currentDistance != 0.0) {
                    isMove = true;
                    if (currentAngle >= 337 || currentAngle < 23) {
//                        右
                        action = 3;
                    }
                    if (currentAngle >= 23 && currentAngle < 68) {
//                        右前
                        action = 3;
                    }
                    if (currentAngle >= 68 && currentAngle < 113) {
//                        前
                        action = 1;
                    }
                    if (currentAngle >= 113 && currentAngle < 158) {
//                        左前
                        action = 2;
                    }
                    if (currentAngle >= 158 && currentAngle < 203) {
//                        左
                        action = 2;
                    }
                    if (currentAngle >= 203 && currentAngle < 248) {
//                        左后
                        action = 4;
                    }
                    if (currentAngle >= 248 && currentAngle < 293) {
//                        后
                        action = 5;
                    }
                    if (currentAngle >= 293 && currentAngle < 337) {
//                        右后
                        action = 4;
                    }
                } else {
                    isMove = false;
                }

                break;
            case RockerView.EVENT_CLOCK:
                if (isMove && action != -1) {
                    sendMoveMessage(action);
                    Log.e("EVENT_CLOCK", "angle=" + currentAngle + " - distance" + currentDistance);
                }

                // 定时回调
                break;
        }
    }

    public static final String GAME_PHOTO_DIR = Environment.getExternalStorageDirectory() +
            File.separator + "benbaba" + File.separator + "Lives" + File.separator + "photo";

    /**
     * @param bitmap   图片bitmap
     * @param fileName 文件名
     */
    public File saveImg(Bitmap bitmap, String fileName) {
        File dir = new File(GAME_PHOTO_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File file = new File(GAME_PHOTO_DIR, fileName);
        FileOutputStream out;
        try {
            out = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            Toast.makeText(MainActivity.this, "保存图片成功.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri uri = Uri.fromFile(file);
            intent.setData(uri);
            this.sendBroadcast(intent);

            return file;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 判断网络状态
     *
     * @param context
     * @return
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

}
