package com.bowhead.bluetoothdemo.bluetooth.ble.serialization;

/**
 * Created by cld.
 */
public interface Task {
    void run(QueueSemaphore semaphore);
}
