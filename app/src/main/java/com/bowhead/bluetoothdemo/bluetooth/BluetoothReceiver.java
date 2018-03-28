package com.bowhead.bluetoothdemo.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;

public class BluetoothReceiver extends BroadcastReceiver{

    private Listener mListener;

    public BluetoothReceiver(Listener listener) {
        mListener = listener;
    }

    public static IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        return filter;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent == null) {
            return;
        }

        String action = intent.getAction();

        if (TextUtils.isEmpty(action)) {
            return;
        }

        switch (action){
            case BluetoothAdapter.ACTION_STATE_CHANGED:
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                int previousState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_STATE, 0);
                mListener.onStateChange(state, previousState);
                break;
        }
    }

    public interface Listener{
        void onStateChange(int state, int previousState);
    }

}
