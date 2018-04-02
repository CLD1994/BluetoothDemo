package com.bowhead.bluetoothdemo;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.bowhead.bluetoothdemo.bluetooth.BluetoothClient;
import com.bowhead.bluetoothdemo.bluetooth.ble.BleConnector;
import com.bowhead.bluetoothdemo.bluetooth.ble.BluetoothGattWrap;
import com.orhanobut.logger.Logger;

import java.util.Locale;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

public class DetailActivity extends AppCompatActivity{

    private ScanResult mDevice;

    private BluetoothGattWrap mGatt;

    private BleConnector.GattConnectCallback mGattConnectCallback = new BleConnector.GattConnectCallback() {
        @Override
        public void onGattReady(BluetoothGattWrap gatt) {
            Logger.d("%s is ready", gatt.getDevice().getName());
            mGatt = gatt;
            CupPair(gatt);
        }

        @Override
        public void onGattDisconnected(BluetoothGattWrap gatt) {
            Logger.d("%s is disconnected", gatt.getDevice().getName());
        }

        @Override
        public void onError(String errorMsg) {
            Logger.e(errorMsg);
        }
    };

    private BluetoothClient mBluetoothClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        mBluetoothClient = BluetoothClient.getInstance(this);

        mDevice = getIntent().getParcelableExtra("BluetoothDevice");

        TextView tvName = findViewById(R.id.tv_name);
        TextView rssi = findViewById(R.id.tv_rssi);
        TextView address = findViewById(R.id.tv_address);

        BluetoothDevice device = mDevice.getDevice();
        String name = device.getName();
        ScanRecord scanRecord = mDevice.getScanRecord();

        if (name == null || name.equals("")){
            if (scanRecord != null){
                Logger.d(scanRecord);
                name = scanRecord.getDeviceName();
            }else {
                name = device.getAddress();
            }
        }
        tvName.setText(String.format("蓝牙名称：%s", name));
        rssi.setText(String.format(Locale.CHINA,"信号强度：%d", mDevice.getRssi()));
        address.setText(String.format("MAC地址：%s", mDevice.getDevice().getAddress()));

        findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isSucceed = mBluetoothClient.connectGatt(DetailActivity.this, mDevice.getDevice(), mGattConnectCallback);
                if (!isSucceed){
                    Logger.d("already connected");
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGatt.close();
        mGatt = null;
    }

    private void CupPair(BluetoothGattWrap gatt){
        for (int i = 0; i < 10; i++){
            boolean isSucceed = gatt.readCharacteristic(GululuProfile.PAIR_SERVICE, GululuProfile.CUP_SN, new BluetoothGattCallback() {
                @Override
                public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                    if (status == GATT_SUCCESS){
                        final String cupSn = characteristic.getStringValue(0);
                        Logger.d(cupSn);
                    }
                }
            });

            if (!isSucceed){
                Logger.e("readCharacteristic fail");
            }
        }
    }
}
