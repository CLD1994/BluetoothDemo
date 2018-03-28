package com.bowhead.bluetoothdemo.bluetooth.classic;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by cld.
 */

public class BluetoothSocketConnector {

    private static final int MESSAGE_CONNECTED = 0;
    private static final int MESSAGE_CONNECT_ERROR = 1;
    private static final int MESSAGE_CANCEL = 2;

    private static final UUID MY_UUID = UUID.fromString("79e6c97d-655f-4e5c-8cb5-c158de491150");

    private ConnectThread mConnectThread;

    private final BluetoothAdapter mBluetoothAdapter;

    public BluetoothSocketConnector(BluetoothAdapter bluetoothAdapter) {
        mBluetoothAdapter = bluetoothAdapter;
    }

    public boolean connect(BluetoothDevice device, final ConnectCallback connectCallback){

        if (!mBluetoothAdapter.isEnabled()) {
            return false;
        }

        mConnectThread = new ConnectThread(device, mBluetoothAdapter, new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what){
                    case MESSAGE_CONNECTED:
                        connectCallback.onConnected((BluetoothSocket) msg.obj);
                        break;
                    case MESSAGE_CONNECT_ERROR:
                        connectCallback.onError((IOException) msg.obj);
                        break;
                    case MESSAGE_CANCEL:
                        connectCallback.onCancel();
                        break;
                }

                return true;
            }
        }));
        mConnectThread.start();

        return true;
    }

    public void cancelConnect(){
        if (mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }
    }

    private static class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothAdapter mmAdapter;
        private final Handler mmHandler;

        public ConnectThread(BluetoothDevice device, BluetoothAdapter adapter, Handler handler){
            mmAdapter = adapter;
            mmHandler = handler;
            BluetoothSocket temp = null;
            try {
                temp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                mmHandler.obtainMessage(MESSAGE_CONNECT_ERROR).sendToTarget();
            }
            mmSocket = temp;
        }

        public void run() {
            mmAdapter.cancelDiscovery();

            try {
                Log.d("CLD" , "wait connect");
                mmSocket.connect();
                mmHandler.obtainMessage(MESSAGE_CONNECTED, mmSocket).sendToTarget();
            } catch (IOException connectException) {
                if (!mmHandler.hasMessages(MESSAGE_CANCEL)){
                    mmHandler.obtainMessage(MESSAGE_CONNECT_ERROR, connectException).sendToTarget();
                }

                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e("CLD", Log.getStackTraceString(closeException));
                }
            }
        }

        public void cancel() {
            try {
                mmHandler.sendEmptyMessageDelayed(MESSAGE_CANCEL, 200);
                mmSocket.close();
            } catch (IOException e) {
                if (mmHandler.hasMessages(MESSAGE_CANCEL)){
                    mmHandler.removeMessages(MESSAGE_CANCEL);
                }
                Log.e("CLD", Log.getStackTraceString(e));
            }
        }
    }

    public interface ConnectCallback {
        void onConnected(BluetoothSocket socket);
        void onError(IOException e);
        void onCancel();
    }
}
