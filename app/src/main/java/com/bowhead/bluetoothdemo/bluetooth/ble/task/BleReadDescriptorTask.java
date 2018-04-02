package com.bowhead.bluetoothdemo.bluetooth.ble.task;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattDescriptor;
import android.os.Handler;
import android.os.Looper;

import com.bowhead.bluetoothdemo.bluetooth.ble.BleGattCallbackDispatcher;
import com.bowhead.bluetoothdemo.bluetooth.ble.serialization.QueueSemaphore;
import com.bowhead.bluetoothdemo.bluetooth.ble.serialization.Task;

/**
 * Created by cld.
 */
public class BleReadDescriptorTask implements Task{

    private BluetoothGatt mBluetoothGatt;

    private BluetoothGattDescriptor mGattDescriptor;

    private BleGattCallbackDispatcher mCallbackDispatcher;

    private BluetoothGattCallback mCallback;

    public BleReadDescriptorTask(BluetoothGatt gatt,
                                 BluetoothGattDescriptor descriptor,
                                 BleGattCallbackDispatcher callbackDispatcher,
                                 BluetoothGattCallback callback) {
        mBluetoothGatt = gatt;
        mGattDescriptor = descriptor;
        mCallbackDispatcher = callbackDispatcher;
        mCallback = callback;
    }

    @Override
    public void run(final QueueSemaphore semaphore) {
        mCallbackDispatcher.setTaskCallback(new BluetoothGattCallback() {
            @Override
            public void onDescriptorRead(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onDescriptorRead(gatt, descriptor, status);
                    }
                });

                semaphore.release();
            }
        });
        mBluetoothGatt.readDescriptor(mGattDescriptor);
    }
}
