package com.bowhead.bluetoothdemo.bluetooth.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;

/**
 * Created by cld.
 */

public class GululuProfile {
    public static UUID PAIR_SERVICE = UUID.fromString("a5704a97-1ede-425a-b9b2-060b26a314bc");


    public static UUID CUP_SN = UUID.fromString("a5704a97-1ede-425a-b9b2-060b26a314bd");

    public static BluetoothGattService createPairService() {
        BluetoothGattService service = new BluetoothGattService(PAIR_SERVICE, BluetoothGattService.SERVICE_TYPE_PRIMARY);

        BluetoothGattCharacteristic cupSN = new BluetoothGattCharacteristic(CUP_SN,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ);

        service.addCharacteristic(cupSN);

        return service;
    }
}
