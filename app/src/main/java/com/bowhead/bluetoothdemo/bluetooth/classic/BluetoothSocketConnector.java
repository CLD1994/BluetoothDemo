package com.bowhead.bluetoothdemo.bluetooth.classic;

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

    public void connect(BluetoothDevice device, final ConnectCallback connectCallback){

        mConnectThread = new ConnectThread(device, new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what){
                    case MESSAGE_CONNECTED:
                        try {
                            BluetoothSocketWrap socketWrap = new BluetoothSocketWrap((BluetoothSocket) msg.obj);
                            connectCallback.onConnected(socketWrap);
                        } catch (IOException e) {
                            connectCallback.onError(e);
                        }
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
    }

    public void cancelConnect(){
        if (mConnectThread != null){
            mConnectThread.cancel();
            mConnectThread = null;
        }
    }

    private static class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final Handler mmHandler;

        private ConnectThread(BluetoothDevice device, Handler handler){
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

        private void cancel() {
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
        void onConnected(BluetoothSocketWrap socket);
        void onError(IOException e);
        void onCancel();
    }
}
