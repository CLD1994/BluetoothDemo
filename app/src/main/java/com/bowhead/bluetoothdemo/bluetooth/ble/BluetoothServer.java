package com.bowhead.bluetoothdemo.bluetooth.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;

import com.orhanobut.logger.Logger;

import java.nio.charset.StandardCharsets;

/**
 * Created by cld.
 */

public class BluetoothServer {

    private final Context mAppContext;

    private final BluetoothManager mBluetoothManager;

    private BluetoothGattServer mBluetoothGattServer;

    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Logger.d("%s is connected", device.getName());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Logger.d("%s is disconnected", device.getName());
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            Logger.d("status = %d, %s is add", status, service.getUuid().toString());
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            Logger.d("%s request read, requestId = %d, offset = %d", device.getName(), requestId, offset);

            if (characteristic.getUuid().equals(GululuProfile.CUP_SN)){

                Logger.d("read cupSn");

                byte[] cupSn = "20170514006295".getBytes(StandardCharsets.UTF_8);
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        cupSn);
            }else {
                Logger.w("not found characteristic");

                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Logger.d("%d CharacteristicWriteRequest", device.getName());
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            Logger.d("%d DescriptorReadRequest", device.getName());
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Logger.d("%d DescriptorWriteRequest", device.getName());
        }

        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            Logger.d("%s onExecuteWrite, requestId = %d, execute = " + execute, device.getName(), requestId);
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status) {
            Logger.d("%s is onNotificationSent, status is %d", status);
        }

        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            Logger.d("%s onMtuChanged, mtu = %d", device.getName(), mtu);
        }

        @Override
        public void onPhyUpdate(BluetoothDevice device, int txPhy, int rxPhy, int status) {
            Logger.d("%s onPhyUpdate, txPhy = %d, rxPhy = %d, status = %d", device.getName(), txPhy, rxPhy, status);
        }

        @Override
        public void onPhyRead(BluetoothDevice device, int txPhy, int rxPhy, int status) {
            Logger.d("%s onPhyRead, txPhy = %d, rxPhy = %d, status = %d", device.getName(), txPhy, rxPhy, status);
        }
    };

    private BluetoothLeAdvertiser mBleAdvertiser;

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Logger.d("Advertise onStartSuccess");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Logger.e("Advertise onStartFailure, errorCode = %d", errorCode);
        }
    };

    public BluetoothServer(Context context, BluetoothManager bluetoothManager) {
        mAppContext = context.getApplicationContext();
        mBluetoothManager = bluetoothManager;
    }

    public boolean startServer(){
        mBluetoothGattServer = mBluetoothManager.openGattServer(mAppContext, mGattServerCallback);
        if (mBluetoothGattServer == null) {
            return false;
        }

        mBluetoothGattServer.addService(GululuProfile.createPairService());

        return true;
    }

    public void stopServer(){
        if (mBluetoothGattServer == null){
            return;
        }

        mBluetoothGattServer.close();
        mBluetoothGattServer = null;
    }

    public boolean startAdvertising(){
        mBleAdvertiser =  mBluetoothManager.getAdapter().getBluetoothLeAdvertiser();
        if (mBleAdvertiser == null) {
            return false;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .build();

        AdvertiseData scanResponseData = new AdvertiseData.Builder()
                .addServiceUuid(new ParcelUuid(GululuProfile.PAIR_SERVICE))
                .build();

        mBleAdvertiser.startAdvertising(settings, data, scanResponseData, mAdvertiseCallback);

        return true;
    }

    public void stopAdvertising(){
        if (mBleAdvertiser == null){
            return;
        }
        mBleAdvertiser.stopAdvertising(mAdvertiseCallback);
    }
}
