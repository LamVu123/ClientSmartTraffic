package com.example.smartTraffic.common;

import com.example.smartTraffic.BuildConfig;
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
            mySocket = IO.socket(BuildConfig.GetDataIp);
            mySocket.connect();
        } catch (URISyntaxException e) {
            mySocket = null;
        }
    }

    public static void setInstance(MySocketFactory instance) {
        MySocketFactory.instance = instance;
    }

    public Socket getMySocket() {
        return mySocket;
    }

    public void setMySocket(Socket mySocket) {
        this.mySocket = mySocket;
    }
}
