package com.example.glimwormbeacons.configuring;

import com.polkapolka.bluetooth.le.BluetoothLeService;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class BeaconServiceConnection {
	
	private Activity activity;
	private String deviceAddress;

	private BluetoothLeService mBluetoothLeService;
	
	public BeaconServiceConnection(Activity activity,String deviceAddress)
	{
		this.activity = activity;
		this.deviceAddress = deviceAddress;
		Intent gattServiceIntent = new Intent(activity, BluetoothLeService.class);
		activity.bindService(gattServiceIntent, mServiceConnection, activity.BIND_AUTO_CREATE);		
	}
	
	
	private final ServiceConnection mServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
					.getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(ConfigureBeacon.class.getName(), "Unable to initialize Bluetooth");
				activity.finish();
			}
			mBluetoothLeService.connect(deviceAddress);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
		}
	};
	
	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter
				.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		return intentFilter;
	}
}
