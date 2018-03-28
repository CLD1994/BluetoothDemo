package com.bowhead.bluetoothdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.bowhead.bluetoothdemo.bluetooth.BluetoothReceiver;
import com.bowhead.bluetoothdemo.bluetooth.ScanResultList;
import com.bowhead.bluetoothdemo.bluetooth.ble.BluetoothClient;
import com.bowhead.bluetoothdemo.bluetooth.ble.BluetoothServer;
import com.bowhead.bluetoothdemo.bluetooth.classic.BluetoothSocketListener;
import com.bowhead.bluetoothdemo.bluetooth.classic.BluetoothSocketWrap;
import com.orhanobut.logger.AndroidLogAdapter;
import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity{

    private boolean isLogInit = false;

    private MyAdapter mAdapter;

    private ScanResultList mData = new ScanResultList();

    private BluetoothClient mBluetoothClient;

    private BluetoothSocketWrap mServerBluetoothSocketWrap;

    private BluetoothServer mBluetoothServer;

    private BluetoothSocketListener mSocketListener;

    private BluetoothReceiver mBluetoothReceiver;

    private IntentFilter mIntentFilter = BluetoothReceiver.getIntentFilter();

    private boolean needOnBluetoothOpenScan = false;

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Logger.d("callbackType = %d", callbackType);
            int index = mData.indexOf(result);
            if (index < 0){
                mData.add(result);
            }else {
                mData.set(index, result);
            }
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            Logger.d("batch size = %d", results.size());
            for (int i = 0; i < results.size(); i++){
                ScanResult scanResult = results.get(i);
                int index = mData.indexOf(scanResult);
                if (index < 0){
                    mData.add(scanResult);
                }else {
                    mData.set(index, scanResult);
                }
            }
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onScanFailed(int errorCode) {
            Logger.e("errorCode = %d", errorCode);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!isLogInit){
            Logger.addLogAdapter(new AndroidLogAdapter());
            isLogInit = true;
        }

        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        if (bluetoothManager != null){
            mBluetoothServer = new BluetoothServer(this, bluetoothManager);

            mBluetoothClient = new BluetoothClient(bluetoothManager);

            mSocketListener = new BluetoothSocketListener(bluetoothManager.getAdapter());

            mBluetoothReceiver = new BluetoothReceiver(new BluetoothReceiver.Listener() {
                @Override
                public void onStateChange(int state, int previousState) {
                    if (state == BluetoothAdapter.STATE_ON){
                        Logger.d("bluetooth on");
                        mBluetoothServer.startServer();
                        mBluetoothServer.startAdvertising();
                        if (needOnBluetoothOpenScan){
                            mBluetoothClient.startScan(mScanCallback);
                        }
                    }else if (state == BluetoothAdapter.STATE_TURNING_OFF){
                        Logger.d("bluetooth off");
                        mBluetoothServer.stopAdvertising();
                        mBluetoothServer.stopServer();
                        mSocketListener.stopListenConnect();
                        if (mServerBluetoothSocketWrap != null){
                            mServerBluetoothSocketWrap.closeSocket();
                        }
                    }

                }
            });

            if (mBluetoothClient.isBluetoothEnabled()){
                mBluetoothServer.startServer();
                mBluetoothServer.startAdvertising();
            }

            initView();
        }else {
            finish();
        }
    }

    private void initView(){
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(recyclerView.getContext());
        DividerItemDecoration itemDecoration = new DividerItemDecoration(recyclerView.getContext(), linearLayoutManager.getOrientation());
        mAdapter = new MyAdapter();

        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.addItemDecoration(itemDecoration);
        recyclerView.setAdapter(mAdapter);


        Button listenButton = findViewById(R.id.btn_listen);
        listenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSocketListener.listenConnect(new BluetoothSocketListener.Callback() {
                    @Override
                    public void onAcceptSocket(BluetoothSocket socket) {
                        Log.d("CLD", "accept ok");

                        mServerBluetoothSocketWrap = new BluetoothSocketWrap(socket, new BluetoothSocketWrap.Callback() {
                            @Override
                            public void onReceive(BluetoothDataOuterClass.BluetoothData data) {
                                Log.d("CLD", "receive name = " + data.getName());
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e("CLD", Log.getStackTraceString(e));
                            }

                            @Override
                            public void onDisconnect() {
                                Log.d("CLD", "server : socket disconnect");
                            }
                        });

                        mServerBluetoothSocketWrap.startRead();
                    }

                    @Override
                    public void onError(IOException e) {
                        Log.d("CLD", Log.getStackTraceString(e));
                    }

                    @Override
                    public void onStopListen() {
                        Log.d("CLD", "stopListen");
                    }
                });
            }
        });

        Button scanButton = findViewById(R.id.btn_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mBluetoothClient.isBluetoothEnabled()){
                    Logger.d("scanButton click");
                    mBluetoothClient.startScan(mScanCallback);
                }else {
                    mBluetoothClient.enableBluetooth();
                    needOnBluetoothOpenScan = true;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mBluetoothReceiver, mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBluetoothReceiver);
        if (mBluetoothClient.isBluetoothEnabled()){
            mSocketListener.stopListenConnect();
            if (mServerBluetoothSocketWrap != null){
                mServerBluetoothSocketWrap.closeSocket();
            }
            mBluetoothServer.stopAdvertising();
            mBluetoothServer.stopServer();
            mBluetoothClient.stopScan(mScanCallback);
        }
    }

    class MyAdapter extends RecyclerView.Adapter<MyViewHolder>{

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bluetooth, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final MyViewHolder holder, int position) {

            ScanResult data = mData.get(position);

            BluetoothDevice device = data.getDevice();

            String name = device.getName();

            if (name == null || name.equals("")){
                ScanRecord scanRecord = data.getScanRecord();
                if (scanRecord != null){
                    name = data.getScanRecord().getDeviceName();
                    if (name == null || name.equals("")){
                        name = device.getAddress();
                    }
                }else {
                    name = device.getAddress();
                }
            }

            holder.mTextView.setText(name);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ScanResult result = mData.get(holder.getLayoutPosition());
                    Intent intent = new Intent(MainActivity.this, DetailActivity.class);
                    intent.putExtra("BluetoothDevice", result);
                    mBluetoothServer.stopAdvertising();
                    mBluetoothServer.stopServer();
                    mBluetoothClient.stopScan(mScanCallback);
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }
    }

    class MyViewHolder extends RecyclerView.ViewHolder{

        final TextView mTextView;

        MyViewHolder(View itemView) {
            super(itemView);
            mTextView = itemView.findViewById(R.id.textView);
        }
    }
}
