package com.bowhead.bluetoothdemo.bluetooth.ble.serialization;

import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

public class QueueSemaphore implements QueueReleaseInterface, QueueAwaitReleaseInterface {

    private final AtomicBoolean isReleased = new AtomicBoolean(false);

    @Override
    public synchronized void awaitRelease(){
        while (!isReleased.get()) {
            try {
                wait();
            } catch (InterruptedException e) {
                if (!isReleased.get()) {
                    Log.w("CLD","Queue's awaitRelease() has been interrupted abruptly "
                            + "while it wasn't released by the release() method.");
                }
            }
        }
    }

    @Override
    public synchronized void release() {
        if (isReleased.compareAndSet(false, true)) {
            notify();
        }
    }
}