package com.bowhead.bluetoothdemo.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.bowhead.bluetoothdemo.bluetooth.ble.BleConnector;
import com.bowhead.bluetoothdemo.bluetooth.classic.BluetoothSocketConnector;
import com.orhanobut.logger.Logger;

import java.util.List;

/**
 * Created by cld.
 */

public class BluetoothClient implements Handler.Callback{

    private static final int MSG_STOP_SCAN = 0;

    private BluetoothAdapter mBluetoothAdapter;

    private BleConnector mBleConnector;

    private BluetoothSocketConnector mSocketConnector;

    private Handler mHandler;

    private volatile static BluetoothClient instance;

    public static BluetoothClient getInstance(Context context){
        if (instance == null) {
            synchronized (BluetoothClient.class){
                if (instance == null){
                    instance = new BluetoothClient(context);
                }
            }
        }
        return instance;
    }

    private BluetoothClient(Context context) {

        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);

        if (bluetoothManager == null){
            return;
        }

        mBluetoothAdapter = bluetoothManager.getAdapter();

        mBleConnector = new BleConnector();

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


    public boolean startLeScan(ScanCallback scanCallback, int timeout){
        return startLeScan(null, new ScanSettings.Builder().build(), scanCallback, timeout);
    }

    public boolean startLeScan(List<ScanFilter> filters, ScanSettings settings, ScanCallback scanCallback, int timeout){
        BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (scanner == null){
            return false;
        }

        scanner.startScan(filters, settings, scanCallback);

        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_STOP_SCAN, scanCallback), timeout);

        return true;
    }

    public void stopLeScan(ScanCallback scanCallback){
        if (mHandler.hasMessages(MSG_STOP_SCAN)){
            mHandler.removeMessages(MSG_STOP_SCAN);
        }
        mBluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
        Logger.d("stopScan");
    }

    public boolean connectGatt(Context context, BluetoothDevice device, BleConnector.GattConnectCallback gattConnectCallback){
        if (mBleConnector != null){
            return false;
        }

        mBleConnector = new BleConnector();
        mBleConnector.connectGatt(context.getApplicationContext(), device, gattConnectCallback);

        return true;
    }

    public void cancelConnectGatt(){
        mBleConnector.cancelConnect();
        mBleConnector = null;
    }


    public void startDiscovery(){
        mBluetoothAdapter.startDiscovery();
    }

    public void cancelDiscovery(){
        mBluetoothAdapter.cancelDiscovery();
    }

    public boolean connect(BluetoothDevice device, BluetoothSocketConnector.ConnectCallback connectCallback) {
        if (mSocketConnector != null){
            return false;
        }

        if (mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
        }

        mSocketConnector = new BluetoothSocketConnector();
        mSocketConnector.connect(device, connectCallback);

        return true;
    }

    public void cancelConnect(){
        mSocketConnector.cancelConnect();
        mSocketConnector = null;
    }

    @Override
    public boolean handleMessage(final Message msg) {
        switch (msg.what){
            case MSG_STOP_SCAN:
                ScanCallback callback = (ScanCallback) msg.obj;
                mBluetoothAdapter.getBluetoothLeScanner().stopScan(callback);
                Logger.d("stopScan");
                break;

        }
        return true;
    }
}
