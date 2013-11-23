package com.psbelov.rpi.leds.server;

import com.psbelov.rpi.leds.LedHelper;
import com.psbelov.rpi.leds.spi.SPI;

public class Main {
    private static void println(String str) {
        System.out.println(str);
    }

    public static void main(String[] args) {
        int port;               // Server port
        String device;          // SPI device that used for leds control
        int ledsCount;          // leds count. default is 50.
        long delay;             // delay between writing data to leds. should be minimum 1
        String passwd;          // simple defence of unauthorised access.

        SPI spi;
        LedHelper leds;

        if (args.length != 5) {
            println("Using: [port] [device] [leds_count] [delay] [passwd]");
            return;
        }

        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException nfe) {
            println("Incorrect port");
            return;
        }

        if (port <= 1024 || port >= 65536) {
            println("Incorrect port [" + port + "]. Should be more then 1024 and less then 65535");
            return;
        }

        try {
            ledsCount = Integer.parseInt(args[3]);
        } catch (NumberFormatException nfe) {
            println("Incorrect LEDs count format");
            return;
        }

        if (ledsCount < 0) {
            println("Leds count should be more than 0");
            return;
        } else if (ledsCount == 0) {
            println("0 leds. NO WAY!");
            return;
        }

        try {
            delay = Long.parseLong(args[2]);
        } catch (NumberFormatException nfe) {
            println("Incorrect delay");
            return;
        }

        if (delay <= 0) {
            println("Delay should be more than 0");
            return;
        } else if (delay >= 1000) {
            println("Delay is " + delay + ". Are you sure? Okay");
        }

        device = args[1];

        spi = new SPI(device, delay);
        if (spi.isOpened() == false) {
            println("Something went wrong with the device " + spi);
            return;
        }

        leds = new LedHelper(spi, delay, ledsCount);

        passwd = args[4];

        try {
            println("port = " + port + "\ndevice = " + device + "\ndelay = " + delay + "\nleds count = " + ledsCount + "\npasswd = " + passwd);
            Server s = new Server(port, leds, passwd);
            new Thread(s).start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
