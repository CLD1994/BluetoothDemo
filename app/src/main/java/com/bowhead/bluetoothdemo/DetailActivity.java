package com.bowhead.bluetoothdemo;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.orhanobut.logger.Logger;

import java.lang.reflect.Method;
import java.util.Locale;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;

public class DetailActivity extends AppCompatActivity {

    private ScanResult mDevice;

    private BluetoothGatt mBluetoothGatt;

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onPhyUpdate(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {

        }

        @Override
        public void onPhyRead(BluetoothGatt gatt, int txPhy, int rxPhy, int status) {

        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            Logger.d("status = %d", status);

            BluetoothDevice device = gatt.getDevice();
            String name = device.getName();

            if (name == null || name.equals("")){
                name = device.getAddress();
            }

            if (status == GATT_SUCCESS){
                if (newState == STATE_CONNECTED){
                    Logger.d("%s connected", name);
                }else if (newState == BluetoothProfile.STATE_DISCONNECTED){
                    Logger.d("%s disconnected", name);
                }
            }else {
//                closeGatt();
                mBluetoothGatt = mDevice.getDevice().connectGatt(DetailActivity.this, true, this, TRANSPORT_LE);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Logger.d("status = %d", status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {

        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {

        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {

        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
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
                mBluetoothGatt = mDevice.getDevice().connectGatt(DetailActivity.this, false, mGattCallback, TRANSPORT_LE);
//                refreshDeviceCache(mBluetoothGatt);
            }
        });
    }

    private boolean refreshDeviceCache(BluetoothGatt gatt){
        try {
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                boolean bool = ((Boolean) localMethod.invoke(localBluetoothGatt, new Object[0])).booleanValue();
                return bool;
            }
        }
        catch (Exception localException) {
            Log.e("CLD", "An exception occured while refreshing device");
        }
        return false;
    }

    private void closeGatt(){
        mBluetoothGatt.disconnect();
        refreshDeviceCache(mBluetoothGatt);
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetoothGatt != null){
            closeGatt();
        }
    }
}
