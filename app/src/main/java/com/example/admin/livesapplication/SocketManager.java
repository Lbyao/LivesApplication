package com.example.admin.livesapplication;

import android.util.Log;

import com.example.admin.livesapplication.callback.OnSocketReceiveCallBack;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.socket.client.IO;
import io.socket.emitter.Emitter;

public class SocketManager {

    private OnSocketReceiveCallBack mReceiveCallBack;
    private Socket socket;
    private Disposable mSocketDisposable;
    private io.socket.client.Socket socket1;
    private String s;

    public SocketManager(OnSocketReceiveCallBack callBack) {
        this.mReceiveCallBack = callBack;
    }

    public Socket getSocket() {
        synchronized (this){
            if (socket==null){
                try {
//                192.168.0.103  11312
                    InetSocketAddress ipAddress = new InetSocketAddress("192.168.0.103",11312);
//                    InetSocketAddress ipAddress = new InetSocketAddress("192.168.0.124",12345);
                    socket = new Socket();
                    socket.connect(ipAddress,10000);
                    Log.e("socket",socket.toString());
                } catch (UnknownHostException e) {

//                    e.printStackTrace();
                    Log.e("UnknownHostException","UnknownHostException:"+e.getMessage());
                    socket = null;
//                    release();
                } catch (IOException e) {
//                    e.printStackTrace();
                    Log.e("IOException","IOException:"+e.getMessage());
                    socket = null;
//                    release();
                }
            }
            return socket;
        }
    }

    /**
     * 发送消息
     *
     * @param socket
     * @param message
     */
    public void sendReceiveTcpMsg(Socket socket, String message) {
        Log.e("message",message);
        mSocketDisposable = Observable.create((ObservableOnSubscribe<String>) e -> {
            PrintStream writer = null;
            try {
                writer = new PrintStream(socket.getOutputStream());
                writer.println(message);
                writer.flush();
            } catch (IOException es) {
                es.printStackTrace();
            }
            e.onComplete();
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .unsubscribeOn(Schedulers.io())
                .subscribe();
    }

    /**
     * 接收消息
     *
     * @param socket
     */
    public void startReceiveTcpMsg(Socket socket) {
        mSocketDisposable = Observable.create((ObservableOnSubscribe<String>) e -> {
            try {
                s = null;
                InputStream in = socket.getInputStream();

                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    s = new String(buffer, 0, len, "UTF-8");
                    e.onNext(s);
                }

            } catch (IOException es) {
                // TODO Auto-generated catch block
                es.printStackTrace();
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(msg -> {
                    if (mReceiveCallBack != null) {
                        mReceiveCallBack.receiveMsg(msg);
                    }
                });
    }


    public void connect(){
        try {
            socket1 = IO.socket("http://localhost");
            Emitter on = socket1.on(io.socket.client.Socket.EVENT_CONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
//                    连接成功的操作
                }
            });
//            监听event消息
            on.on("event", new Emitter.Listener() {
                @Override
                public void call(Object... args) {

                }
            }).on(io.socket.client.Socket.EVENT_DISCONNECT, new Emitter.Listener() {
                @Override
                public void call(Object... args) {
//                    断开连接
                }
            });

            socket1.connect();

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg(String msg){
        socket1.emit("event",msg);
    }

    public void release(){
        try {
            if (socket!=null){
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mSocketDisposable!=null){
            mSocketDisposable.dispose();
            mSocketDisposable = null;
        }
    }
}
