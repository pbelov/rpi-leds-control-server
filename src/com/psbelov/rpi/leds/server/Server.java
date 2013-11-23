package com.psbelov.rpi.leds.server;

import com.psbelov.rpi.leds.LedHelper;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

class Server implements Runnable {
    public static final String VERSION = "0.4.2";

    private int mPort;
    private String mPass;

    private LedHelper mLEDs;
    private boolean run;
    private ServerSocket mServerSocker;

    private int clientCount = 0;
    
    public Server(int port, LedHelper leds, String pass) throws Exception {
        mPort = port;
        mLEDs = leds;
        mPass = pass;
        run = true;
    }

    @Override
    public void run() {
        try {
            mServerSocker = new ServerSocket(mPort);
            System.out.println("Server started on port " + mPort);
        } catch (IOException e) {
            e.printStackTrace();
            stop();
        }
        
        Socket client;
        while (run) {
            client = null;
            try {
                client = mServerSocker.accept();
            } catch (IOException e) {
                e.printStackTrace();
                stop();
            }

            if (null != client) {
                Thread t = new Thread(new ClientHandler(client, mLEDs, mPass, clientCount));
                t.start();
                clientCount++;
            }
        }
    }
    
    public void stop() {
        System.out.println("Server stopped");
        run = false;
        if (null != mServerSocker) {
            try {
                mServerSocker.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        mServerSocker = null;
    }
}