package com.comert;

import com.comert.gEmbedded.api.ApplicationContextFactory;
import com.comert.gEmbedded.api.device.DeviceContext;
import com.comert.gEmbedded.api.device.Pin;
import com.comert.gEmbedded.api.device.gpio.*;

class CallBack implements ListenerCallBack{
    private volatile long time;

    @Override
    public void onFalling(long timeStamp) {

        synchronized (this){
            time = timeStamp - time;
        }

    }

    @Override
    public void onRising(long timeStamp) {

        time = timeStamp;

    }

    @Override
    public void onTimeout() {
        time = 0;
    }

    @Override
    public void onError() {

    }

    public long getTime(){
        long _time = time;
        time=0;
        return _time;

    }

}

public class App {

    private final static Pin outputPin = Pin.PIN_20;
    private final static Pin listenerPin = Pin.PIN_21;
    private final static CallBack callBack = new CallBack();

    public static void main(String[] args) throws InterruptedException {

        DeviceContext deviceContext = ApplicationContextFactory.getDeviceContextInstance();

        try {

            deviceContext.setupDevice();



            GPIOFactory gpioFactory = deviceContext.getGPIOFactoryInstance();

            DigitalOutputPin transmitter = gpioFactory.createDigitalOutputPin(
                    DigitalOutputPinConfigurator
                            .getBuilder()
                            .pin(outputPin)
                            .build()
            );
            ListenerPin receiver = gpioFactory.createListenerPin(
                    ListenerPinConfigurator
                            .getBuilder()
                            .pin(listenerPin)
                            .eventStatus(Event.SYNCHRONOUS_BOTH)
                            .timeoutInMilSec(1000)
                            .callBack(callBack)
                            .build()
            );

            receiver.startListener();

            for (int i = 0; i < 50; i++) {
                transmitter.write();
                Thread.sleep(0, 10000);
                transmitter.clear();
                Thread.sleep(50);
                receiver.suspendListener();
                final double timeInNs = callBack.getTime();
                final double distanceInCm = timeInNs * 0.00003;
                final double realDistanceInCm = distanceInCm / 2;
                System.out.println(realDistanceInCm);
                receiver.resumeListener();
            }

            receiver.terminateListener();

        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw throwable;
        } finally {
            deviceContext.shutdownDevice();
        }

    }
}