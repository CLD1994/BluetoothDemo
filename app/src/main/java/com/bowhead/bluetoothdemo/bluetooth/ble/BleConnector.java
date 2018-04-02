package com.bowhead.bluetoothdemo.bluetooth.ble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.orhanobut.logger.Logger;

import java.util.Locale;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;

/**
 * Created by cld.
 */
public class BleConnector implements Handler.Callback{

    private static final int MSG_CONNECT_GATT = 0;

    private static final int MSG_GATT_CONNECTED = 1;

    private static final int MSG_GATT_DISCONNECTED = 2;

    private static final int MSG_GATT_SERVICES_DISCOVERED = 3;

    private static final int MSG_GATT_ERROR = 4;

    private static final int GATT_CONNECT_TIMEOUT = 4000;

    private static final int GATT_CONNECT_RETRY = 4;

    private Handler mHandler;

    private BleGattCallbackDispatcher mGattCallbackDispatcher = new BleGattCallbackDispatcher();

    private BluetoothGattWrap mBluetoothGatt;

    private GattConnectCallback mGattConnectCallback;

    public BleConnector() {
        mHandler = new Handler(Looper.getMainLooper(), this);
    }

    public void connectGatt(Context context, BluetoothDevice device, GattConnectCallback gattConnectCallback){
        mGattConnectCallback = gattConnectCallback;

        mGattCallbackDispatcher.setTaskCallback(new BluetoothGattCallback() {
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
        });

        BluetoothGatt gatt = device.connectGatt(context, false, mGattCallbackDispatcher.getGattCallback(), TRANSPORT_LE);

        mBluetoothGatt = new BluetoothGattWrap(mGattCallbackDispatcher, gatt);

        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_CONNECT_GATT, GATT_CONNECT_TIMEOUT, GATT_CONNECT_RETRY), GATT_CONNECT_TIMEOUT);
    }

    public void cancelConnect() {
        mBluetoothGatt.close();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what){
            case MSG_CONNECT_GATT:
                final int timeout = msg.arg1;
                final int retry = msg.arg2;
                mBluetoothGatt.disconnect();

                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mBluetoothGatt.connect();
                    }
                },500);

                if (retry > 0){
                    mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_CONNECT_GATT, timeout, retry - 1), timeout);
                }
                break;
            case MSG_GATT_CONNECTED:
                mGattCallbackDispatcher.setTaskCallback(new BluetoothGattCallback() {
                    @Override
                    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                        if (status == GATT_SUCCESS){
                            mHandler.obtainMessage(MSG_GATT_SERVICES_DISCOVERED).sendToTarget();
                        }else {
                            String errMsg = String.format(Locale.getDefault(),"services discover error, status is %d", status);
                            mHandler.obtainMessage(MSG_GATT_ERROR, errMsg).sendToTarget();
                        }
                    }
                });
                boolean isSucceed = mBluetoothGatt.discoverServices();
                if (!isSucceed){
                    mGattConnectCallback.onError("discoverServices fail");
                }
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
        void onGattReady(BluetoothGattWrap gatt);
        void onGattDisconnected(BluetoothGattWrap gatt);
        void onError(String errorMsg);
    }
}
