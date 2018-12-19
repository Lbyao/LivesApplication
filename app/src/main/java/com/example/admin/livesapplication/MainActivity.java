package com.example.admin.livesapplication;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.example.admin.livesapplication.callback.OnSocketReceiveCallBack;
import com.google.gson.Gson;
import com.pili.pldroid.player.AVOptions;
import com.pili.pldroid.player.PLOnImageCapturedListener;
import com.pili.pldroid.player.widget.PLVideoTextureView;
import com.pili.pldroid.player.widget.PLVideoView;

import java.net.Socket;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

@SuppressLint("CheckResult")
public class MainActivity extends AppCompatActivity implements View.OnClickListener, PLOnImageCapturedListener, View.OnTouchListener {

    private PLVideoTextureView mVideoView1;
    private PLVideoTextureView mVideoView2;
    private EditText urls;
    private EditText urls2;
    private SocketManager socketManager;
    private Gson gson = new Gson();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initPlayer();
//        rtmp://live.hkstv.hk.lxdns.com/live/hks2
//        rtmp://192.168.0.149:1935/live
    }

    /**
     * 初始化控件
     */
    private void initView() {


        mVideoView1 = findViewById(R.id.PLVideoView1);
        mVideoView2 = findViewById(R.id.PLVideoTextureView);
        urls = findViewById(R.id.url);
        urls2 = findViewById(R.id.url2);
        Button up = findViewById(R.id.btn_up);
        Button down = findViewById(R.id.btn_down);
        Button left = findViewById(R.id.btn_left);
        Button right = findViewById(R.id.btn_right);

        Button play = findViewById(R.id.play);
        Button play2 = findViewById(R.id.play2);

        ImageView ivIcon = findViewById(R.id.ivIcon);

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
//        ivIcon.setImageBitmap();
        socketManager = new SocketManager(callBack);
//        up.setOnClickListener(this);
//        down.setOnClickListener(this);
//        left.setOnClickListener(this);
//        right.setOnClickListener(this);
        play.setOnClickListener(this);
        play2.setOnClickListener(this);
        up.setOnTouchListener(this);
        down.setOnTouchListener(this);
        left.setOnTouchListener(this);
        right.setOnTouchListener(this);
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

        if (socketManager!=null){
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
        }
    }

    @Override
    public void onImageCaptured(byte[] bytes) {
        // TODO: 2018/12/18 图片的byte数组
//        BitmapFactory.Options options= new BitmapFactory.Options();
        if (bytes != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        }
//            if (opts != null)
//                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length,
//                        opts);
//            else
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (v.getId()) {
            case R.id.btn_up:
                getAction(event.getAction(), "up");
                break;
            case R.id.btn_down:
                getAction(event.getAction(), "down");
                break;
            case R.id.btn_left:
                getAction(event.getAction(), "left");
                break;
            case R.id.btn_right:
                getAction(event.getAction(), "right");
                break;
        }
        return true;
    }

    private void getAction(int action, String key) {
        Log.e("action", "action:" + action);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
//                sendDownMessage(key);
//                break;
            case MotionEvent.ACTION_MOVE:
                sendDownMessage(key);
                break;
            case MotionEvent.ACTION_UP:
                sendUpMessage(key);
                break;
//
        }
    }

    private void sendDownMessage(String key) {
        if (key.equals("up")) {
            Log.e("up", "up down");
            sendMoveMessage(1);
        } else if (key.equals("down")) {
            Log.e("down", "down down");
            sendMoveMessage(5);
        } else if (key.equals("left")) {
            Log.e("left", "left down");
            sendMoveMessage(2);
        } else if (key.equals("right")) {
            Log.e("right", "right down");
            sendMoveMessage(3);
        }
    }

    private void sendUpMessage(String key) {
        if (key.equals("up")) {
            Log.e("up", "up up");
        } else if (key.equals("down")) {
            Log.e("down", "down up");
        } else if (key.equals("left")) {
            Log.e("left", "left up");
        } else if (key.equals("right")) {
            Log.e("right", "right up");
        }
    }

    private void sendMoveMessage(int action) {
        Observable.create((ObservableOnSubscribe<Socket>) e -> {
            Socket socket = socketManager.getSocket();

            e.onNext(socket);
            e.onComplete();
        }).observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(socket -> {
                    if (socket != null) {
                        socketManager.sendReceiveTcpMsg(socket, DeviceMessaheUtils.getMoveMessage(action));
                        socketManager.startReceiveTcpMsg(socket);
                    } else {
                        Toast.makeText(MainActivity.this, "连接失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void sendModeChangeMessage(int type, int action) {
        Observable.create((ObservableOnSubscribe<Socket>) e -> {
            Socket socket = socketManager.getSocket();
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

    OnSocketReceiveCallBack callBack = msg -> {
        Log.e("msg", msg);
        Result result = gson.fromJson(msg, Result.class);
        if (result != null) {
            Result.DataBean data = result.getData();
//        Log.e("result",result.toString());
            if (result.getType() == 1001) {
                if (data != null && data.getResult().equals("ok")) {
                    Toast.makeText(MainActivity.this, "切换手动模式成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "切换手动模式失败", Toast.LENGTH_SHORT).show();
                }
            }
            if (result.getType() == 1002) {
                if (data != null && data.getResult().equals("ok")) {
                    Toast.makeText(MainActivity.this, "切换自动模式成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "切换自动模式失败", Toast.LENGTH_SHORT).show();
                }
            }

            if (result.getType() == 1003) {
                if (data != null && data.getResult().equals("ok")) {
                    Toast.makeText(MainActivity.this, "发送移动命令成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "发送移动命令失败", Toast.LENGTH_SHORT).show();
                }
            }
        }

    };

}
