package com.bowhead.bluetoothdemo.bluetooth;

import android.bluetooth.le.ScanResult;

import java.util.ArrayList;

/**
 * Created by cld.
 */

public class ScanResultList extends ArrayList<ScanResult>{
    @Override
    public int indexOf(Object o) {
        if (o == null) {
            for (int i = 0; i < super.size(); i++)
                if (get(i) == null)
                    return i;
        } else {
            for (int i = 0; i < super.size(); i++)
                if (((ScanResult) o).getDevice().equals(super.get(i).getDevice()))
                    return i;
        }
        return -1;
    }
}
