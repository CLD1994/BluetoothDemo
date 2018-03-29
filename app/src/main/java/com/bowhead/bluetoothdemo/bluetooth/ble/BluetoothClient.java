package com.bowhead.bluetoothdemo.bluetooth.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.orhanobut.logger.Logger;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;

/**
 * Created by cld.
 */

public class BluetoothClient implements Handler.Callback{

    private static final int MSG_STOP_SCAN = 0;

    private static final int MSG_CONNECT_GATT = 1;

    private static final int MSG_GATT_CONNECTED = 2;

    private static final int MSG_GATT_DISCONNECTED = 3;

    private static final int MSG_GATT_SERVICES_DISCOVERED = 4;

    private static final int MSG_GATT_ERROR = 5;

    private Context mContext;

    private BluetoothAdapter mBluetoothAdapter;

    private CharacteristicResponse mCharacteristicResponse;

    private DescriptorResponse mDescriptorResponse;

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String name = gatt.getDevice().getName();

            if (status == GATT_SUCCESS){
                if (newState == STATE_CONNECTED){
                    Logger.d("%s connected", name);
                    if (mHandler.hasMessages(MSG_CONNECT_GATT)){
                        mHandler.removeMessages(MSG_CONNECT_GATT);
                    }
                    mHandler.obtainMessage(MSG_GATT_CONNECTED).sendToTarget();
                }else if (newState == BluetoothProfile.STATE_DISCONNECTED){
                    Logger.d("%s disconnected", name);
                    mHandler.obtainMessage(MSG_GATT_DISCONNECTED).sendToTarget();
                }
            }else {
                Logger.e("status = %d", status);
                String errMsg;
                switch (status){
                    case 133:
                        errMsg = "status is 133, normal error";
                        break;
                    case 19:
                        errMsg = "The target closes the connection";
                        break;
                    default:
                        errMsg = "unknown error";
                }
                mHandler.obtainMessage(MSG_GATT_ERROR, errMsg).sendToTarget();

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == GATT_SUCCESS){
                mHandler.obtainMessage(MSG_GATT_SERVICES_DISCOVERED).sendToTarget();
            }else {
                String errMsg = String.format(Locale.getDefault(),"services discover error, status is %d", status);
                mHandler.obtainMessage(MSG_GATT_ERROR, errMsg).sendToTarget();
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            mCharacteristicResponse.onResponse(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            mCharacteristicResponse.onResponse(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
           Logger.d("onCharacteristicChanged");
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            mDescriptorResponse.onResponse(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            mDescriptorResponse.onResponse(gatt, descriptor, status);
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {

        }


    };

    private BluetoothGatt mBluetoothGatt;

    private Handler mHandler;

    public BluetoothClient(Context context) {

        mContext = context.getApplicationContext();

        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        if (bluetoothManager == null){
            return;
        }

        mBluetoothAdapter = bluetoothManager.getAdapter();

        mHandler = new Handler(Looper.getMainLooper(), this);
    }

    public boolean enableBluetooth(){
        return mBluetoothAdapter.enable();
    }

    public boolean disableBluetooth(){
        return mBluetoothAdapter.disable();
    }

    public boolean isBluetoothEnabled(){
        return mBluetoothAdapter.isEnabled();
    }

    public boolean startScan(ScanCallback scanCallback, int timeout){
        return startScan(null, new ScanSettings.Builder().build(), scanCallback, timeout);
    }

    public boolean startScan(List<ScanFilter> filters, ScanSettings settings, ScanCallback scanCallback, int timeout){
        BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (scanner == null){
            return false;
        }

        scanner.startScan(filters, settings, scanCallback);

        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_STOP_SCAN, scanCallback), timeout);

        return true;
    }

    public void stopScan(ScanCallback scanCallback){
        if (mHandler.hasMessages(MSG_STOP_SCAN)){
            mHandler.removeMessages(MSG_STOP_SCAN);
        }
        mBluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
        Logger.d("stopScan");
    }

    private GattConnectCallback mGattConnectCallback;

    public void connectGatt(BluetoothDevice device, GattConnectCallback gattConnectCallback){
        mGattConnectCallback = gattConnectCallback;
        mBluetoothGatt = device.connectGatt(mContext, false, mGattCallback, TRANSPORT_LE);
        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_CONNECT_GATT, 4000, 3), 4000);
    }

    public void reConnectGatt(){
        if (mBluetoothGatt != null){
            mBluetoothGatt.connect();
        }
    }

    public void disconnectGatt(){
        if (mBluetoothGatt != null){
            mBluetoothGatt.disconnect();
        }
    }

    public void closeGatt(){
        if (mBluetoothGatt != null){
            mBluetoothGatt.close();
            mGattConnectCallback = null;
        }
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

    public boolean readCharacteristic(UUID serviceId, UUID characteristicId, CharacteristicResponse response) {
        boolean ret = false;

        BluetoothGattCharacteristic characteristic = getCharacteristic(serviceId, characteristicId);
        if (characteristic != null && mBluetoothGatt.readCharacteristic(characteristic)){
            mCharacteristicResponse = response;
            ret = true;
        }
        return ret;
    }

    public boolean writeCharacteristic(UUID serviceId, UUID characteristicId, byte[] value, CharacteristicResponse response){
        boolean ret = false;

        BluetoothGattCharacteristic characteristic = getCharacteristic(serviceId, characteristicId);

        if (characteristic != null){
            characteristic.setValue(value != null ? value : new byte[]{});
            mBluetoothGatt.writeCharacteristic(characteristic);
            mCharacteristicResponse = response;
            ret = true;
        }

        return ret;
    }

    public boolean readDescriptor(UUID serviceId, UUID characteristicId, UUID descriptorId, DescriptorResponse response) {
        boolean ret = false;

        BluetoothGattCharacteristic characteristic = getCharacteristic(serviceId, characteristicId);

        if (characteristic != null){
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descriptorId);
            if (descriptor != null){
                mBluetoothGatt.readDescriptor(descriptor);
                mDescriptorResponse = response;
                ret = true;
            }
        }
        return ret;
    }

    public boolean writeDescriptor(UUID serviceId, UUID characteristicId, UUID descriptorId, byte[] value, DescriptorResponse response) {
        boolean ret = false;

        BluetoothGattCharacteristic characteristic = getCharacteristic(serviceId, characteristicId);

        if (characteristic != null){
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descriptorId);
            if (descriptor != null){
                descriptor.setValue(value);
                mBluetoothGatt.writeDescriptor(descriptor);
                mDescriptorResponse = response;
                ret = true;
            }
        }

        return ret;
    }

    @Override
    public boolean handleMessage(final Message msg) {
        switch (msg.what){
            case MSG_STOP_SCAN:
                ScanCallback callback = (ScanCallback) msg.obj;
                mBluetoothAdapter.getBluetoothLeScanner().stopScan(callback);
                Logger.d("stopScan");
                break;

            case MSG_CONNECT_GATT:
                final int timeout = msg.arg1;
                final int retry = msg.arg2;
                mBluetoothGatt.disconnect();
                mBluetoothGatt.connect();
                if (retry > 0){
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_CONNECT_GATT, timeout, retry - 1), timeout);
                }
                break;
            case MSG_GATT_CONNECTED:
                mBluetoothGatt.discoverServices();
                break;
            case MSG_GATT_DISCONNECTED:
                mGattConnectCallback.onGattDisconnected(mBluetoothGatt);
                break;
            case MSG_GATT_SERVICES_DISCOVERED:
                mGattConnectCallback.onGattReady(mBluetoothGatt);
                break;
            case MSG_GATT_ERROR:
                mGattConnectCallback.onError((String) msg.obj);
                break;

        }
        return true;
    }

    public interface GattConnectCallback {
        void onGattReady(BluetoothGatt gatt);
        void onGattDisconnected(BluetoothGatt gatt);
        void onError(String errorMsg);
    }

    public interface CharacteristicResponse{
        void onResponse(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status);
    }

    public interface DescriptorResponse{
        void onResponse(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status);
    }
}
