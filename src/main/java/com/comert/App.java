package com.comert;

import com.comert.gEmbedded.api.ApplicationContextFactory;
import com.comert.gEmbedded.api.device.DeviceContext;
import com.comert.gEmbedded.api.device.Pin;
import com.comert.gEmbedded.api.device.gpio.*;

class Radar implements ListenerCallBack {

    private static final Radar INSTANCE = new Radar();
    private final OutputPin transmitter;
    private final ListenerPin receiver;
    private final int timeoutInMilSec = 1000;

    private Radar() {
        DeviceContext deviceContext = ApplicationContextFactory.getDeviceContextInstance();
        GPIOFactory gpioFactory = deviceContext.getGPIOFactoryInstance();
        this.transmitter = gpioFactory.createOutputPin(
                OutputPinConfigurator
                        .getBuilder()
                        .pin(Pin.PIN_20)
                        .build()
        );
        this.receiver = gpioFactory.createListenerPin(
                ListenerPinConfigurator
                        .getBuilder()
                        .pin(Pin.PIN_21)
                        .eventStatus(Event.SYNCHRONOUS_BOTH)
                        .timeoutInMilSec(timeoutInMilSec)
                        .callBack(this)
                        .build()
        );
    }

    public static Radar getInstance() {
        return INSTANCE;
    }

    public void start() {
        receiver.start();
    }

    public void stop() {
        receiver.terminate();
    }

    private volatile long time;
    private volatile boolean completed;

    @Override
    public synchronized void onRising(long timeStamp) {
        time = timeStamp;
        completed = false;
    }

    @Override
    public synchronized void onFalling(long timeStamp) {
        time = timeStamp - time;
        completed = true;
    }

    @Override
    public void onTimeout() {
    }

    @Override
    public void onError() {
        throw new RuntimeException();
    }

    public int getDistanceInCm() {
        transmitter.setHigh();
        try {
            Thread.sleep(0, 10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        transmitter.setLow();

        try {
            Thread.sleep(timeoutInMilSec / 20);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (!completed) {
            return -1;
        } else {
            final double distanceInCm = time * 0.00003;
            final int realDistanceInCm = (int) (distanceInCm / 2);
            if (realDistanceInCm < 2 || realDistanceInCm > 400) {
                return 0;
            } else {
                return realDistanceInCm;
            }
        }

    }

}

public class App {
    public static void main(String[] args) {

        DeviceContext deviceContext = ApplicationContextFactory.getDeviceContextInstance();

        try {
            deviceContext.setupDevice();
            final Radar radar = Radar.getInstance();

            radar.start();
            for (int i = 0; i < 250; i++) {
                System.out.println(radar.getDistanceInCm());
            }
            radar.stop();

        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw throwable;
        } finally {
            deviceContext.shutdownDevice();
        }

    }
}