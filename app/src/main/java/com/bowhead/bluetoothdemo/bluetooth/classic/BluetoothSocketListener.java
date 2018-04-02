package com.bowhead.bluetoothdemo.bluetooth.classic;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.orhanobut.logger.Logger;

import java.io.IOException;
import java.util.UUID;

/**
 * Created by cld.
 */

public class BluetoothSocketListener {

    private static final int MSG_ACCEPT = 0;

    private static final int MSG_ERROR = 1;

    private static final int MSG_CANCEL_LISTEN = 2;

    private static final UUID MY_UUID = UUID.fromString("79e6c97d-655f-4e5c-8cb5-c158de491150");

    private ListenerThread mListenerThread;

    private final BluetoothAdapter mBluetoothAdapter;

    public BluetoothSocketListener(BluetoothAdapter bluetoothAdapter) {
        mBluetoothAdapter = bluetoothAdapter;
    }

    public boolean listenConnect(final Callback callback){

        if (!mBluetoothAdapter.isEnabled()){
            return false;
        }

        if (mListenerThread != null && mListenerThread.isAlive()){
            return true;
        }

        mListenerThread = new ListenerThread(mBluetoothAdapter, new Handler(Looper.getMainLooper(), new Handler.Callback() {
            @Override
            public boolean handleMessage(Message msg) {
                switch (msg.what){
                    case MSG_ACCEPT:
                        try {
                            BluetoothSocketWrap socketWrap = new BluetoothSocketWrap((BluetoothSocket) msg.obj);
                            callback.onAcceptSocket(socketWrap);
                        } catch (IOException e) {
                            callback.onError(e);
                        }
                        break;
                    case MSG_ERROR:
                        callback.onError((IOException) msg.obj);
                        break;
                    case MSG_CANCEL_LISTEN:
                        callback.onStopListen();
                        break;
                }

                return true;
            }
        }));

        mListenerThread.start();

        return true;
    }

    public void stopListenConnect(){
        if (mListenerThread != null){
            mListenerThread.cancel();
            mListenerThread = null;
            Logger.d("ListenerThread cancel");
        }
    }

    private static class ListenerThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        private final Handler mmHandler;

        private ListenerThread(BluetoothAdapter adapter, Handler handler){
            BluetoothServerSocket temp = null;
            mmHandler = handler;
            try {
                temp = adapter.listenUsingRfcommWithServiceRecord("Gululu", MY_UUID);
            } catch (IOException e) {
                mmHandler.obtainMessage(MSG_ERROR, e).sendToTarget();
            }
            mmServerSocket = temp;
        }

        public void run() {
            BluetoothSocket socket;
            while (true) {
                try {
                    Log.d("CLD", "wait accept");
                    socket = mmServerSocket.accept();
                    mmHandler.obtainMessage(MSG_ACCEPT, socket).sendToTarget();
                } catch (IOException e) {
                    if (!mmHandler.hasMessages(MSG_CANCEL_LISTEN)){
                        mmHandler.obtainMessage(MSG_ERROR, e).sendToTarget();
                    }
                    break;
                }
            }
        }

        private void cancel() {
            try {
                mmHandler.sendEmptyMessageDelayed(MSG_CANCEL_LISTEN, 200);
                mmServerSocket.close();
            } catch (IOException e) {
                if (mmHandler.hasMessages(MSG_CANCEL_LISTEN)){
                    mmHandler.removeMessages(MSG_CANCEL_LISTEN);
                }
                Log.e("CLD", Log.getStackTraceString(e));
            }
        }
    }

    public interface Callback {
        void onAcceptSocket(BluetoothSocketWrap socket);
        void onError(IOException e);
        void onStopListen();
    }
}
