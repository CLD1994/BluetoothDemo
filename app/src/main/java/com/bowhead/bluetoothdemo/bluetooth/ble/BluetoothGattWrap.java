package com.bowhead.bluetoothdemo.bluetooth.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import com.bowhead.bluetoothdemo.bluetooth.ble.serialization.TaskExecutor;
import com.bowhead.bluetoothdemo.bluetooth.ble.task.BleReadCharacteristicTask;
import com.bowhead.bluetoothdemo.bluetooth.ble.task.BleReadDescriptorTask;
import com.bowhead.bluetoothdemo.bluetooth.ble.task.BleWriteCharacteristicTask;
import com.bowhead.bluetoothdemo.bluetooth.ble.task.BleWriteDescriptorTask;

import java.util.UUID;

/**
 * Created by cld.
 */
public class BluetoothGattWrap {
    private BluetoothGatt mBluetoothGatt;

    private BleGattCallbackDispatcher mGattCallbackDispatcher;

    private TaskExecutor mTaskExecutor = new TaskExecutor();

    BluetoothGattWrap(BleGattCallbackDispatcher gattCallbackDispatcher, BluetoothGatt bluetoothGatt) {
        mGattCallbackDispatcher = gattCallbackDispatcher;
        mBluetoothGatt = bluetoothGatt;
    }

    public boolean connect(){
        return mBluetoothGatt.connect();
    }

    public void disconnect(){
        mBluetoothGatt.disconnect();
    }

    public boolean discoverServices(){
        return mBluetoothGatt.discoverServices();
    }

    public void close(){
        mTaskExecutor.close();
        mBluetoothGatt.close();
    }

    public BluetoothDevice getDevice() {
        return mBluetoothGatt.getDevice();
    }

    private BluetoothGattCharacteristic getCharacteristic(UUID serviceId, UUID characteristicId){
        if (mBluetoothGatt == null){
            return null;
        }

        BluetoothGattService service = mBluetoothGatt.getService(serviceId);

        if (service == null){
            return null;
        }

        return service.getCharacteristic(characteristicId);
    }

    public boolean readCharacteristic(UUID serviceId, UUID characteristicId, BluetoothGattCallback callback) {
        boolean ret = false;

        BluetoothGattCharacteristic characteristic = getCharacteristic(serviceId, characteristicId);
        if (characteristic != null){
            ret = mTaskExecutor.addTask(new BleReadCharacteristicTask(mBluetoothGatt, characteristic, mGattCallbackDispatcher, callback));
        }
        return ret;
    }

    public boolean writeCharacteristic(UUID serviceId, UUID characteristicId, byte[] value, BluetoothGattCallback callback){
        boolean ret = false;

        BluetoothGattCharacteristic characteristic = getCharacteristic(serviceId, characteristicId);

        if (characteristic != null){
            characteristic.setValue(value != null ? value : new byte[]{});
            ret = mTaskExecutor.addTask(
                    new BleWriteCharacteristicTask(
                            mBluetoothGatt,
                            characteristic,
                            mGattCallbackDispatcher,
                            callback)
            );
        }

        return ret;
    }

    public boolean readDescriptor(UUID serviceId, UUID characteristicId, UUID descriptorId, BluetoothGattCallback callback) {
        boolean ret = false;

        BluetoothGattCharacteristic characteristic = getCharacteristic(serviceId, characteristicId);

        if (characteristic != null){
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descriptorId);
            if (descriptor != null){
                ret = mTaskExecutor.addTask(new BleReadDescriptorTask(mBluetoothGatt, descriptor, mGattCallbackDispatcher, callback));
            }
        }
        return ret;
    }

    public boolean writeDescriptor(UUID serviceId, UUID characteristicId, UUID descriptorId, byte[] value, BluetoothGattCallback callback) {
        boolean ret = false;

        BluetoothGattCharacteristic characteristic = getCharacteristic(serviceId, characteristicId);

        if (characteristic != null){
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descriptorId);
            if (descriptor != null){
                descriptor.setValue(value != null ? value : new byte[]{});
                ret = mTaskExecutor.addTask(new BleWriteDescriptorTask(mBluetoothGatt, descriptor, mGattCallbackDispatcher, callback));
            }
        }

        return ret;
    }


}
