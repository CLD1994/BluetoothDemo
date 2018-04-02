package com.bowhead.bluetoothdemo.bluetooth.ble.task;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import android.os.Looper;

import com.bowhead.bluetoothdemo.bluetooth.ble.BleGattCallbackDispatcher;
import com.bowhead.bluetoothdemo.bluetooth.ble.serialization.QueueSemaphore;
import com.bowhead.bluetoothdemo.bluetooth.ble.serialization.Task;

/**
 * Created by cld.
 */
public class BleWriteCharacteristicTask implements Task{

    private BluetoothGatt mBluetoothGatt;

    private BluetoothGattCharacteristic mGattCharacteristic;

    private BleGattCallbackDispatcher mCallbackDispatcher;

    private BluetoothGattCallback mCallback;

    public BleWriteCharacteristicTask(BluetoothGatt gatt,
                                     BluetoothGattCharacteristic characteristic,
                                     BleGattCallbackDispatcher callbackDispatcher,
                                     BluetoothGattCallback callback) {
        mBluetoothGatt = gatt;
        mGattCharacteristic = characteristic;
        mCallbackDispatcher = callbackDispatcher;
        mCallback = callback;
    }

    @Override
    public void run(final QueueSemaphore semaphore) {
        mCallbackDispatcher.setTaskCallback(new BluetoothGattCallback() {
            @Override
            public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mCallback.onCharacteristicRead(gatt, characteristic, status);
                    }
                });

                semaphore.release();
            }
        });

        mBluetoothGatt.writeCharacteristic(mGattCharacteristic);
    }
}
