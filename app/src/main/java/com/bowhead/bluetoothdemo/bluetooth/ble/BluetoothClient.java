package com.bowhead.bluetoothdemo.bluetooth.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelUuid;

import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cld.
 */

public class BluetoothClient implements Handler.Callback{

    private static final int MSG_STOP_SCAN = 0;

    private final BluetoothAdapter mBluetoothAdapter;

    private final BluetoothLeScanner mBluetoothLeScanner;

    private final List<ScanFilter> mScanFilters = new ArrayList<>();

    private final ScanSettings mScanSettings;

    private Handler mHandler;

    public BluetoothClient(BluetoothManager bluetoothManager) {
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        ScanFilter scanFilter = new ScanFilter.Builder()
                .setServiceUuid(new ParcelUuid(GululuProfile.PAIR_SERVICE))
                .build();

        mScanFilters.add(scanFilter);

        mScanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
                .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
                .setReportDelay(1000)
                .build();

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

    public boolean startScan(ScanCallback scanCallback){
        BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();

        if (scanner == null){
            return false;
        }

        scanner.startScan(mScanFilters, mScanSettings, scanCallback);

        mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_STOP_SCAN, scanCallback), 60000);

        return true;
    }

    public void stopScan(ScanCallback scanCallback){
        if (mHandler.hasMessages(MSG_STOP_SCAN)){
            mHandler.removeMessages(MSG_STOP_SCAN);
        }
        mBluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
        Logger.d("stopScan");
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what){
            case MSG_STOP_SCAN:
                ScanCallback callback = (ScanCallback) msg.obj;
                mBluetoothAdapter.getBluetoothLeScanner().stopScan(callback);
                break;
        }
        return true;
    }
}
