package com.bowhead.bluetoothdemo;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.bowhead.bluetoothdemo.bluetooth.ble.BluetoothClient;
import com.bowhead.bluetoothdemo.bluetooth.ble.GululuProfile;
import com.orhanobut.logger.Logger;

import java.util.Locale;

import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

public class DetailActivity extends AppCompatActivity implements Handler.Callback{

    public static final int MSG_START_PAIR = 0;

    private ScanResult mDevice;

    private BluetoothClient.GattConnectCallback mGattConnectCallback = new BluetoothClient.GattConnectCallback() {
        @Override
        public void onGattReady(BluetoothGatt gatt) {
            Logger.d("%s is ready", gatt.getDevice().getName());
            new Handler(Looper.getMainLooper(), DetailActivity.this).obtainMessage(MSG_START_PAIR).sendToTarget();
        }

        @Override
        public void onGattDisconnected(BluetoothGatt gatt) {
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



        mBluetoothClient = new BluetoothClient(this);

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
                mBluetoothClient.connectGatt(mDevice.getDevice(), mGattConnectCallback);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothClient.closeGatt();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what){
            case MSG_START_PAIR:
                boolean isSucceed = mBluetoothClient.readCharacteristic(GululuProfile.PAIR_SERVICE, GululuProfile.CUP_SN, new BluetoothClient.CharacteristicResponse() {
                    @Override
                    public void onResponse(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                        if (status == GATT_SUCCESS){
                            final String cupSn = characteristic.getStringValue(0);
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(DetailActivity.this.getApplicationContext(), cupSn, Toast.LENGTH_LONG).show();
                                }
                            });
                        }
                    }
                });
                if (!isSucceed){
                    Logger.e("readCharacteristic fail");
                }
                break;
        }
        return true;
    }
}
