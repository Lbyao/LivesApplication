package com.example.admin.livesapplication;

import android.util.Log;

import com.example.admin.livesapplication.callback.OnSocketReceiveCallBack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
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

    public SocketManager(OnSocketReceiveCallBack callBack) {
        this.mReceiveCallBack = callBack;
    }

    public Socket getSocket() {
        if (socket==null){
            try {
                InetAddress ipAddress = InetAddress.getByName("192.168.0.124");
                socket = new Socket(ipAddress, 12346);
                socket.setSoTimeout(10000);
                Log.e("socket",socket.toString());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return socket;
    }

    public void sendReceiveTcpMsg(Socket socket, String message) {
        Log.e("message",message);
        mSocketDisposable = Observable.create((ObservableOnSubscribe<String>) e -> {
            PrintStream writer;
            try {
                writer = new PrintStream(socket.getOutputStream());
                writer.println(message.toString().getBytes("UTF-8"));
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

    public void startReceiveTcpMsg(Socket socket) {
        mSocketDisposable = Observable.create((ObservableOnSubscribe<String>) e -> {
            try {

                InputStream in = socket.getInputStream();
                String str = "ok";
                int len = 0;
                byte[] buffer = new byte[1024];
                while ((len = in.read(buffer)) != -1) {
                    e.onNext(new String(buffer, 0, len, "UTF-8"));
//                    mReceiveCallBack.receiveMsg(new String(buffer, 0, len, "UTF-8"));
                }
            } catch (IOException es) {
                // TODO Auto-generated catch block
                es.printStackTrace();
            }
        }).subscribeOn(Schedulers.io())
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
