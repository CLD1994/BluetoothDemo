package com.bowhead.bluetoothdemo.bluetooth.classic;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.bowhead.bluetoothdemo.BluetoothDataOuterClass;
import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by cld.
 */

public class BluetoothSocketWrap {
    private static final int MSG_WRITE = 1;
    private static final int MSG_RECEIVE_DATA = 2;
    private static final int MSG_ERROR = 3;
    private static final int MSG_DISCONNECT = 4;

    public interface Callback {
        void onReceive(BluetoothDataOuterClass.BluetoothData data);
        void onError(Exception e);
        void onDisconnect();
    }

    private Handler mUIHandler;

    private HandlerThread mHandlerThread;

    private Handler mWorkHandler;

    private BluetoothSocket mSocket;

    private InputStream mInputStream;

    private OutputStream mOutputStream;

    private int mMaxPacketSize;

    public BluetoothSocketWrap(BluetoothSocket socket, final Callback callback) {
        try {
            mSocket = socket;
            mInputStream = mSocket.getInputStream();
            mOutputStream = mSocket.getOutputStream();
            mMaxPacketSize = mSocket.getMaxTransmitPacketSize();

            mUIHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
                @Override
                @SuppressWarnings("unchecked")
                public boolean handleMessage(Message msg) {
                    switch (msg.what){
                        case MSG_RECEIVE_DATA:
                            BluetoothDataOuterClass.BluetoothData data = (BluetoothDataOuterClass.BluetoothData) msg.obj;
                            Log.d("CLD", "onReceive");
                            callback.onReceive(data);
                            break;
                        case MSG_ERROR:
                            Log.d("CLD", "onError");
                            callback.onError((Exception) msg.obj);
                            break;
                        case MSG_DISCONNECT:
                            Log.d("CLD", "onDisconnect");
                            callback.onDisconnect();
                            break;
                    }
                    return true;
                }
            });

            mHandlerThread = new HandlerThread("socketWriteThread");
            mHandlerThread.start();
            mWorkHandler = new Handler(mHandlerThread.getLooper(), new Handler.Callback() {
                @Override
                @SuppressWarnings("unchecked")
                public boolean handleMessage(Message msg) {
                    switch (msg.what){
                        case MSG_WRITE:
                            try {
                                BluetoothDataOuterClass.BluetoothData data = (BluetoothDataOuterClass.BluetoothData) msg.obj;
                                int serialized = data.getSerializedSize();
                                Log.d("CLD", "data size = " + serialized);
                                final CodedOutputStream codedOutput = CodedOutputStream.newInstance(mOutputStream, mMaxPacketSize);
                                codedOutput.writeUInt32NoTag(serialized);
                                data.writeTo(codedOutput);
                                codedOutput.flush();
                                mOutputStream.flush();
                                Log.d("CLD", "write ok");
                            } catch (IOException e) {
                                mUIHandler.obtainMessage(MSG_ERROR, e).sendToTarget();
                            }
                            break;
                    }
                    return true;
                }
            });

        } catch (IOException e) {
            callback.onError(e);
        }
    }

    public void startRead(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Log.d("CLD", "wait read");
                        BluetoothDataOuterClass.BluetoothData data = BluetoothDataOuterClass.BluetoothData.parseDelimitedFrom(mInputStream);
                        Log.d("CLD", "read ok");
                        mUIHandler.obtainMessage(MSG_RECEIVE_DATA, data).sendToTarget();
                    } catch (IOException e) {
                        if (e instanceof InvalidProtocolBufferException){
                            if (e.getCause() instanceof IOException){
                                Log.d("CLD", Log.getStackTraceString(e.getCause()));
                                mHandlerThread.quit();
                                mUIHandler.obtainMessage(MSG_DISCONNECT).sendToTarget();
                            }else {
                                mUIHandler.obtainMessage(MSG_ERROR, e).sendToTarget();
                            }
                        }
                        break;
                    }
                }
            }
        }).start();
    }

    public void write(BluetoothDataOuterClass.BluetoothData data){
        Log.d("CLD", "sent write msg");
        mWorkHandler.obtainMessage(MSG_WRITE, data).sendToTarget();
    }

    public void closeSocket(){
        try {
            mSocket.close();
            mSocket = null;
            mInputStream = null;
            mOutputStream = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        mHandlerThread.quit();
        mHandlerThread = null;
        mWorkHandler = null;
        mUIHandler = null;
    }
}
