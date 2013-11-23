package com.psbelov.rpi.leds.server;

import com.psbelov.rpi.leds.LedHelper;
import com.psbelov.rpi.leds.data.Command;
import com.psbelov.rpi.leds.data.CommandsEnum;
import com.psbelov.rpi.leds.utils.ColorUtils;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


class ClientHandler implements Runnable {
    private Socket mClient;
    private boolean IO;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private String mClientAddress;
    
    private String mPass;
    private LedHelper mLEDs;

    private Thread tRun;
    private boolean isStarted;
    private int mNumber;
    
    public ClientHandler(Socket client, LedHelper leds, String pass, int number) {
        mClient = client;
        mLEDs = leds;
        mPass = pass;
        mNumber = number;
        IO = false;
    }
    
    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(mClient.getOutputStream());
            in = new ObjectInputStream(mClient.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
            disconnect(e.toString());
        }
        mClientAddress = mClient.getInetAddress().toString();
        System.out.println("New mClient #" + mNumber + " at " + mClientAddress);
        Command c;
        while (!IO) {
            c = null;
            try {
                c = (Command) in.readObject();
            } catch (EOFException eofe) {
                // eofe.printStackTrace(); // no needs to print stack trace when used disconnects SUDDENLY
                mLEDs.off();
                IO = true;
                System.out.println("User down");
            } catch (Exception e) {
                e.printStackTrace();
                disconnect(e.toString());
            }

            if (null != c) {
                processCommand(c);
            }
        }
    }
    
    private void processCommand(final Command c) {
        System.out.println("Command " + c.getCommand().toString() + " from " + mClientAddress + " data = " + c.getData());
        
        switch(c.getCommand()) {
            case CONNECT:
                if (c.getData() == null || mPass.equals(c.getData()) == false) {
                    System.out.println("Unauthorised access from " + mClientAddress + ". Used mPass = \"" + c.getData() + "\"");

                    disconnect("Incorrect password"); //disconnecting if CONNECT command doesn't contain correct mPass
                } else { // sending server version to mClient
                    sendCommand(new Command(CommandsEnum.VERSION, Server.VERSION));
                }
            break;

            case DISCONNECT:
                if (c.getData() == null) {
                    System.out.println("Incorrect Disconnect command");
                } else {
                    disconnect(c.getData().toString());
                }
                break;

            case KILL:
                return;

            case LENGTH:
                sendCommand(new Command(CommandsEnum.LENGTH, mLEDs.getCount()));

                break;
            case ON:
                mLEDs.on();
                break;

            case OFF:
                mLEDs.off();
                break;

            case COLOR:
                if (c.getData() == null) {
                    System.out.println("Incorrect COLOR command");
                } else {
                    mLEDs.on(c.getStringData());
                }
                break;

            case INDEX: {
                String data = c.getStringData();
                int i = Integer.parseInt(data.split(":")[0]);
                String rgbColor = data.split(":")[1];
                mLEDs.index(i, rgbColor, false);
            }
                break;

            case INDEX_REVERTED: {
                String data = c.getStringData();
                int i = Integer.parseInt(data.split(":")[0]);
                String rgbColor = data.split(":")[1];
                mLEDs.index(i, rgbColor, true);
            }
            break;

            case FILL: {
                String data = c.getStringData();
                int i = Integer.parseInt(data.split(":")[0]);
                String rgbColor = data.split(":")[1];
                mLEDs.fill(i, rgbColor, false);
            }
            break;

            case FILL_REVERTED: {
                String data = c.getStringData();
                int i = Integer.parseInt(data.split(":")[0]);
                String rgbColor = data.split(":")[1];
                mLEDs.fill(i, rgbColor, true);
            }

            break;

            case CANCEL: {
                isStarted = false;
                if (tRun != null && tRun.isAlive()) {
                   // tRun.interrupt();
                    tRun.stop();
                }

                mLEDs.off();
            }

            break;

            case CHASE: {
                isStarted = true;
                if (tRun != null && tRun.isAlive()) {
                    //tRun.interrupt();
                    tRun.stop();
                }
                tRun = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isStarted) {
                            String rgbColor = c.getStringData();
                            mLEDs.drawMode(LedHelper.MODE.CHASE, rgbColor);
                        }
                    }
                });

                tRun.start();
            }
            break;

            case RANDOM: {
                isStarted = true;

                tRun = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isStarted) {
                            mLEDs.drawMode(LedHelper.MODE.RANDOM, 0, 100);
                        }
                    }
                });

                tRun.start();
            }
            break;

            case RANDOM_SOLID: {
                isStarted = true;

                tRun = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isStarted) {
                            mLEDs.drawMode(LedHelper.MODE.RANDOM_SOLID, 0, 1000);
                        }
                    }
                });

                tRun.start();
            }
            break;

            case CHASE_CYCLED: {
                isStarted = true;
                if (tRun != null && tRun.isAlive()) {
                //    tRun.interrupt();
                    tRun.stop();
                }
                tRun = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isStarted) {
                            String rgbColor = c.getStringData();
                            mLEDs.drawMode(LedHelper.MODE.CHASE2, rgbColor);
                        }
                    }
                });

                tRun.start();
            }
            break;

            case STARS: {
                isStarted = true;
                final int rgbColor = ColorUtils.parseColor(c.getData().toString());
                tRun = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (isStarted) {

                            mLEDs.drawMode(LedHelper.MODE.STARS, rgbColor, 1);
                        }
                    }
                });

                tRun.start();
            }
            break;

            case STARS2: {
                isStarted = false;
                final int rgbColor = ColorUtils.parseColor(c.getData().toString());
                mLEDs.drawMode(LedHelper.MODE.STARS2, 0, 20, 49, rgbColor, 1);
            }
            break;

            case RAINBOW2: {
                mLEDs.drawMode(LedHelper.MODE.RAINBOW2, 0);
            }
            break;

            case RAINBOW_DYNAMIC: {
                isStarted = false;
                mLEDs.drawMode(LedHelper.MODE.RAINBOW_DYNAMIC, 0, 100);
            }
            break;

            default:
                System.out.println("Incorrect command from " + mClientAddress);
                break;
        }
    }
    
    public synchronized boolean sendCommand(Command c) {
        try {
            out.writeObject(c);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
            IO = true;
            mLEDs.off();
            return false;
        }
        return true;        
    }
    
    private void disconnect(String reason) {
        sendCommand(new Command(CommandsEnum.DISCONNECT, reason));
        mLEDs.off();
        IO = true;

        System.out.println("mClient " + mClientAddress + " disconnected (" + reason + ")");
    }
}
