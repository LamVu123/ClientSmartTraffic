package com.duytry.smarttraffic.common;

import com.duytry.smarttraffic.BuildConfig;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import java.net.URISyntaxException;

public class MySocketFactory {

    private static MySocketFactory instance = null;

    public static MySocketFactory getInstance() {
        if (instance == null) {
            instance = new MySocketFactory();
        }
        return instance;
    }

    private Socket mySocket;
    {
        try {
            mySocket = IO.socket(BuildConfig.SocketIp);
            mySocket.connect();
        } catch (URISyntaxException e) {
            mySocket = null;
        }
    }

    private Socket myBackUpSocket;
    {
        try {
            myBackUpSocket = IO.socket(BuildConfig.SocketBackUpIp);
            myBackUpSocket.connect();
        } catch (URISyntaxException e) {
            myBackUpSocket = null;
        }
    }

    public static void setInstance(MySocketFactory instance) {
        MySocketFactory.instance = instance;
    }

    public Socket getMySocket() {
        return mySocket;
    }

    public Socket getMyBackUpSocket() {
        return myBackUpSocket;
    }

    public void setMySocket(Socket mySocket) {
        this.mySocket = mySocket;
    }
}
