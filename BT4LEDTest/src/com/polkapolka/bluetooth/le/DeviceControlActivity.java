/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.polkapolka.bluetooth.le;

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.os.Handler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import android.widget.SeekBar;

/**
 * For a given BLE device, this Activity provides the user interface to connect,
 * display data, and display GATT services and characteristics supported by the
 * device. The Activity communicates with {@code BluetoothLeService}, which in
 * turn interacts with the Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity implements OnCancelListener {
	private final static String TAG = DeviceControlActivity.class
			.getSimpleName();

	public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
	public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
	private int[] RGBFrame = { 0, 0, 0 };
	private TextView isSerial;
	private TextView mConnectionState;
	private TextView mDataField;
	private SeekBar mRed, mGreen, mBlue;
	private String mDeviceName;
	private String mDeviceAddress;
	private TextView mmajor_field;
	private TextView mminor_field;
	private SeekBar moutput_power_slider;
	private TextView moutput_power;
	private TextView mpin_field_input;
	private Button mread_values;

	private SeekBar mtransmit_interval_slider;
	private TextView mtransmit_interval;
	private TextView mname_field_input;
	private LinearLayout mlayout_container;
	private TextView mversion;
	// private ExpandableListView mGattServicesList;
	private BluetoothLeService mBluetoothLeService;
	private boolean mConnected = false;
	private BluetoothGattCharacteristic characteristicTX;
	private BluetoothGattCharacteristic characteristicRX;
	private String txString = "";
	private ArrayList<String> txQueue = new ArrayList();
	private boolean mustConnect = true;
	final String actionPinRequested = "android.bluetooth.device.action.PAIRING_REQUEST";

	public final static UUID HM_RX_TX = UUID
			.fromString(SampleGattAttributes.HM_RX_TX);

	private final String LIST_NAME = "NAME";
	private final String LIST_UUID = "UUID";
	
	private boolean go_back = false;
	
	private boolean pin_required = false;

	// Code to manage Service lifecycle.
	private final ServiceConnection mServiceConnection = new ServiceConnection() {
   
		@Override
		public void onServiceConnected(ComponentName componentName,
				IBinder service) {
			mBluetoothLeService = ((BluetoothLeService.LocalBinder) service)
					.getService();
			if (!mBluetoothLeService.initialize()) {
				Log.e(TAG, "Unable to initialize Bluetooth");
				finish();
			}
			// Automatically connects to the device upon successful start-up
			// initialization.
			
			mBluetoothLeService.connect(mDeviceAddress);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mBluetoothLeService = null;
		}
	};

	private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
				mConnected = true;
				
				invalidateOptionsMenu();
			} else if (BluetoothLeService.ACTION_GATT_DISCONNECTED
					.equals(action)) {
				mConnected = false;
			//	updateConnectionState(false);
				invalidateOptionsMenu();
			} else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED
					.equals(action)) {
				// Show all the supported services and characteristics on the
				// user interface.
				
				if (mBluetoothLeService != null) {
					displayGattServices(mBluetoothLeService
							.getSupportedGattServices());
					enableDisableView(mlayout_container, true);
					updateConnectionState(true);
					readAllValues();
					System.out.println("CONNECTED TO DEVICE");
					if(mydialog!=null)mydialog.dismiss();
				} else {
					//resetConnection();
				}
			} else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
				displayData(intent
						.getStringExtra(mBluetoothLeService.EXTRA_DATA));
			}
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.gatt_services_characteristics);

		final Intent intent = getIntent();
		mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
		mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
		mConnectionState = (TextView) findViewById(R.id.connection_state);
		mmajor_field = (TextView) findViewById(R.id.major_field_input);
		mminor_field = (TextView) findViewById(R.id.minor_field_input);
		moutput_power = (TextView) findViewById(R.id.output_power);
		moutput_power_slider = (SeekBar) findViewById(R.id.output_power_slider);
		mtransmit_interval = (TextView) findViewById(R.id.transmit_interval);
		mtransmit_interval_slider = (SeekBar) findViewById(R.id.transmit_interval_slider);
		mname_field_input = (TextView) findViewById(R.id.name_field_input);
		mpin_field_input = (TextView) findViewById(R.id.pin_field_input);
		mlayout_container = (LinearLayout) findViewById(R.id.layout_container);
		mread_values = (Button) findViewById(R.id.readsettings);
		mread_values.setVisibility(View.GONE);
		mversion = (TextView) findViewById(R.id.device_version);
		
		// Sets up UI references.
		((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
		mminor_field.setText("0");
		mmajor_field.setText("0");
		moutput_power_slider
				.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

					int value = 0;

					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						value = progress;
						onStopTrackingTouch(seekBar);
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						String output = "";
						if (value == 0)
							output = "<2 meters";
						if (value == 1)
							output = "<10 meters";
						if (value == 2)
							output = "<50 meters";
						if (value == 3)
							output = "<100 meters";
						moutput_power.setText("Beacon Range: " + output);

					}

				});
		mtransmit_interval_slider
				.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

					int value = 0;

					@Override
					public void onProgressChanged(SeekBar seekBar,
							int progress, boolean fromUser) {
						value = progress;
						onStopTrackingTouch(seekBar);
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
						// TODO Auto-generated method stub
					}

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						String output = "";
						switch (value) {
						case 0:
							output = "100";
							break;
						case 1:
							output = "152.5";
							break;
						case 2:
							output = "211.25 ";
							break;
						case 3:
							output = "318.75";
							break;
						case 4:
							output = "417.5";
							break;
						case 5:
							output = "546.25";
							break;
						case 6:
							output = "760";
							break;
						case 7:
							output = "852.5";
							break;
						case 8:
							output = "1022.4";
							break;
						case 9:
							output = "1285";
							break;
						case 10:
							output = "2000";
							break;
						case 11:
							output = "3000";
							break;
						case 12:
							output = "4000";
							break;
						case 13:
							output = "5000";
							break;
						case 14:
							output = "6000";
							break;
						case 15:
							output = "7000";
							break;
						}
						output += " ms";
						mtransmit_interval.setText("Transmit Interval: "
								+ output);
					}
				});

		IntentFilter intentFilterPinRequested = new IntentFilter(actionPinRequested);
        registerReceiver(mReceiverRequiresPin, intentFilterPinRequested);

		getActionBar().setTitle(mDeviceName);
		getActionBar().setDisplayHomeAsUpEnabled(true);
		
		Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
		bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
		enableDisableView(mlayout_container, false);
	    registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

	    mlayout_container.setBackgroundResource(R.drawable.grid);
	    
	    mydialog = new Dialog(this);
        mydialog.addContentView(new ProgressBar(this),
                new LayoutParams(40, 40));
        mydialog.setTitle("Connecting");
        mydialog.show();
        
        
        
	}

	private void enableDisableView(View view, boolean enabled) {
		view.setEnabled(enabled);

		if (view instanceof ViewGroup) {
			ViewGroup group = (ViewGroup) view;

			for (int idx = 0; idx < group.getChildCount(); idx++) {
				enableDisableView(group.getChildAt(idx), enabled);
			}
		}
	}

	private final BroadcastReceiver mReceiverRequiresPin = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			enableDisableView(mlayout_container, false);
			mread_values.setVisibility(1);
			mread_values.setEnabled(true);
			//pin_required=true;
			// try {
			/*
			BluetoothDevice newDevice = intent
					.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			Class<?> btDeviceInstance = Class.forName(BluetoothDevice.class
					.getCanonicalName());

			Method convert = btDeviceInstance.getMethod(
					"convertPinToBytes", String.class);

			byte[] pin = (byte[]) convert.invoke(newDevice, "001245");

			Method setPin = btDeviceInstance.getMethod("setPin",
					byte[].class);

			boolean success = (Boolean) setPin.invoke(newDevice, pin);
			System.out.println("PIN ENTERED" + success);
			} catch (Exception e) {
			e.printStackTrace();
			}
			*/
		}
	};

	private final BroadcastReceiver mReceiverBonded = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			System.out
					.println("HHHHHHHHHHHHHHEEEEEEEEEEEEERRRRRRRRRRRRRREEEEEEEEEEEEEE");
			readAllValues();
			// try {
			/*
			BluetoothDevice newDevice = intent
					.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			Class<?> btDeviceInstance = Class.forName(BluetoothDevice.class
					.getCanonicalName());

			Method convert = btDeviceInstance.getMethod(
					"convertPinToBytes", String.class);

			byte[] pin = (byte[]) convert.invoke(newDevice, "001245");

			Method setPin = btDeviceInstance.getMethod("setPin",
					byte[].class);

			boolean success = (Boolean) setPin.invoke(newDevice, pin);
			System.out.println("PIN ENTERED" + success);
			} catch (Exception e) {
			e.printStackTrace();
			}
			*/
		}
	};

	@Override
	protected void onResume() {
		super.onResume();
		registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
		IntentFilter intentFilterPinRequested = new IntentFilter(actionPinRequested);
        registerReceiver(mReceiverRequiresPin, intentFilterPinRequested);
		if (mBluetoothLeService != null) {
			final boolean result = mBluetoothLeService.connect(mDeviceAddress);
			Log.d(TAG, "Connect request result=" + result);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		unregisterReceiver(mGattUpdateReceiver);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unbindService(mServiceConnection);
		mBluetoothLeService = null;
		unregisterReceiver(mReceiverRequiresPin);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.gatt_services, menu);
		if (mConnected) {
			menu.findItem(R.id.menu_connect).setVisible(false);
			menu.findItem(R.id.menu_disconnect).setVisible(true);
		} else {
			menu.findItem(R.id.menu_connect).setVisible(true);
			menu.findItem(R.id.menu_disconnect).setVisible(false);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_connect:
			mustConnect = true;

			if (mBluetoothLeService != null) {
				System.out.println("Connecting");
				unbindService(mServiceConnection);

				mBluetoothLeService = null;
				Intent gattServiceIntent = new Intent(this,
						BluetoothLeService.class);
				bindService(gattServiceIntent, mServiceConnection,
						BIND_AUTO_CREATE);
						
			}
			return true;
		case R.id.menu_disconnect:
			mustConnect = false;
			if (mBluetoothLeService != null) 
				{
				txQueue.add("AT+RESET");
				transmitQueue();
				}
			return true;
		case android.R.id.home:
		//	unregisterReceiver(mReceiverBonded);
			if (mBluetoothLeService != null) 
			{
		//	mBluetoothLeService.disconnect();
				go_back=true;
				txQueue.add("AT+RESET");
				transmitQueue();
			}
			if(!mConnected)
				{
				onBackPressed();
				}
			
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void updateConnectionState(final boolean connected) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (connected) {
					mConnectionState.setText("Connected");
				} else {
				//	enableDisableView(mlayout_container, false);
					mConnectionState.setText("Disconnected");
					// if (mustConnect)
				//	 resetConnection();

				}

			}
		});
	}

	boolean passRequired = false;
	String beacon_name = "";

	private void displayData(String data) {
		if (data != null) {
			if (txQueue.get(0).startsWith("AT+MARJ") && (data.startsWith("OK+Get:")||data.startsWith("OK+Set:"))) {
				mmajor_field.setText(String.valueOf(Integer.decode(data
						.substring(7))));
				txQueue.remove(0);
				enableDisableView(mmajor_field, true);
			}

			else if (txQueue.get(0).startsWith("AT+MINO")&&  (data.startsWith("OK+Get:")||data.startsWith("OK+Set:"))) {
				mminor_field.setText(String.valueOf(Integer.decode(data
						.substring(7))));
				txQueue.remove(0);
				enableDisableView(mminor_field, true);
			} else if (txQueue.get(0).startsWith("AT+POWE")&& (data.startsWith("OK+Get:")||data.startsWith("OK+Set:"))) {
				String output = "";
				int value = Integer.parseInt(data.substring(7));
				moutput_power_slider.setProgress(value);
				txQueue.remove(0);
				enableDisableView(moutput_power_slider, true);
			} else if (txQueue.get(0).startsWith("AT+ADVI")&& (data.startsWith("OK+Get:")||data.startsWith("OK+Set:"))) {
				String output = "";
				int value = Integer.parseInt(data.substring(7));
				mtransmit_interval_slider.setProgress(value);
				txQueue.remove(0);
				enableDisableView(mtransmit_interval_slider, true);
			} else if (txQueue.get(0).startsWith("AT+NAME")&& data.startsWith("OK+NAME")) {
				beacon_name = data.substring(8);
				beacon_name = beacon_name.trim();
				
				mname_field_input.setText(beacon_name);
				txQueue.remove(0);
				enableDisableView(mname_field_input, true);
			} else if (txQueue.get(0).startsWith("AT+NAME")&& data.startsWith("OK+Set:")) {
				
				beacon_name = data.substring(7);
				beacon_name = beacon_name.trim();
				mname_field_input.setText(beacon_name);
				txQueue.remove(0);
				enableDisableView(mname_field_input, true);
			} else if (txQueue.get(0).startsWith("AT+TYPE")&& (data.startsWith("OK+Get:")||data.startsWith("OK+Set:"))) {
				if (data.substring(7).equals("0"))
					passRequired = false;
				else
					passRequired = true;
				txQueue.remove(0);
				enableDisableView(mname_field_input, true);
			} else if (txQueue.get(0).startsWith("AT+PASS") && (data.startsWith("OK+Get:")||data.startsWith("OK+Set:"))) {
				if (passRequired)
					mpin_field_input.setText(data.substring(7));
				else
					mpin_field_input.setText("");
				txQueue.remove(0);
				enableDisableView(mpin_field_input, true);
			} else if (txQueue.get(0).startsWith("AT+VERS?")) {
				mversion.setText(data);
				txQueue.remove(0);
			} else if (txQueue.get(0).equals("AT+RESET")) {
				System.out
						.println("REEEEEEEEEEEEEEEEEEEEEEESSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSSEEEEEEEEEEEEEEETTTTTT DDDDDDDDOOOOOOOOOOOOOONNNNNNNNNNNEEEEEE");
				txQueue.remove(0);
				enableDisableView(mlayout_container, false);
				mBluetoothLeService.disconnect();
				if(go_back)
					{
					go_back = false;
					
					onBackPressed();
					
					}
			//	resetConnection();

			} else {
				txQueue.remove(0);
			}
			// mDataField.setText(data);
		//	sleep(100);
			if(mydialog!=null && txQueue.size()==0)
			{
				mydialog.dismiss(); 
				timeout=false;
			}
			transmitQueue();
		}
	}

	private void displayGattServices(List<BluetoothGattService> gattServices) {
		if (gattServices == null)
			return;
		String uuid = null;
		String unknownServiceString = getResources().getString(
				R.string.unknown_service);
		ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();

		// Loops through available GATT Services.
		for (BluetoothGattService gattService : gattServices) {
			HashMap<String, String> currentServiceData = new HashMap<String, String>();
			uuid = gattService.getUuid().toString();
			currentServiceData.put(LIST_NAME,
					SampleGattAttributes.lookup(uuid, unknownServiceString));

			// If the service exists for HM 10 Serial, say so.
			/*
			if (SampleGattAttributes.lookup(uuid, unknownServiceString) == "HM 10 Serial") {
				isSerial.setText("Yes, serial :-)");
			} else {
				isSerial.setText("No, serial :-(");
			}
			*/
			currentServiceData.put(LIST_UUID, uuid);
			gattServiceData.add(currentServiceData);
		
			// get characteristic when UUID matches RX/TX UUID
			characteristicTX = gattService
					.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
			characteristicRX = gattService
					.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
		}

	}

	private static IntentFilter makeGattUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter
				.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		return intentFilter;
	}

	private void sleep(int mills) {
		try {
			Thread.sleep(mills);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void readAllValues(View v)
	{
		enableDisableView(mlayout_container, true);
		mread_values.setEnabled(false);
		mread_values.setVisibility(View.GONE);
		readAllValues();
	}

	public void readAllValues() {
		txQueue.add("AT+NAME?");
		txQueue.add("AT+MARJ?");
		txQueue.add("AT+MINO?");
		txQueue.add("AT+ADVI?");
		txQueue.add("AT+POWE?");	
		txQueue.add("AT+TYPE?");
		txQueue.add("AT+PASS?");
		txQueue.add("AT+VERS?");
		transmitQueue();
	}

	public void transmitQueue() {
		if (txQueue.size() < 1)
			return;
		
		if (mBluetoothLeService != null && characteristicTX != null) {
			characteristicTX.setValue(txQueue.get(0));
			mBluetoothLeService.writeCharacteristic(characteristicTX);
			mBluetoothLeService.setCharacteristicNotification(characteristicRX,
					true);
			
		}
	}
	
	
	boolean timeout = false;
	Dialog mydialog;
	Handler handler = new Handler();
	Runnable ru = new Runnable() {
        public void run() {    
        	if(timeout){
            mydialog.dismiss();   
            connectionTimedOut();
        	}
        }
    }; 
	
	public void timerDelayRemoveDialog(long time, final Dialog d,boolean on){
 
	if(on)	handler.postDelayed(ru,time);
	else handler.removeCallbacks(ru);
	}
	
	public void connectionTimedOut()
	{
		mydialog = new Dialog(this);
        mydialog.addContentView(new ProgressBar(this),
                new LayoutParams(40, 40));
        mydialog.setTitle("Connection timeout");
        mydialog.setCancelable(true);
        mydialog.setCanceledOnTouchOutside(true);
        mydialog.show();
        mydialog.setOnCancelListener(this);
        /*
		if (mBluetoothLeService != null) 
		{
		mBluetoothLeService.disconnect();
		}
		onBackPressed();
		*/
	}

	public void setSettings(View view) {
		mydialog = new Dialog(this);
        mydialog.addContentView(new ProgressBar(this),
                new LayoutParams(40, 40));
        mydialog.setTitle("Saving Settings");
        mydialog.setCancelable(false);
        mydialog.setCanceledOnTouchOutside(false);
        timerDelayRemoveDialog(5000, mydialog,false);
        timerDelayRemoveDialog(5000, mydialog,true);
        timeout=true;
        mydialog.show();
		int minor = Integer.parseInt(mminor_field.getText().toString());
		String minor_string = Integer.toHexString(minor);
		while (minor_string.length() < 4) {
			minor_string = "0" + minor_string;
		}

		int major = Integer
				.parseInt((String) mmajor_field.getText().toString());
		String major_string = Integer.toHexString(major);
		while (major_string.length() < 4) {
			major_string = "0" + major_string;
		}
		System.out.println("MMMMMMMMAAAAAAAAAAJJJJJJJJOOOOOOOORRRRR:::::::"+ minor_string);
		txQueue.add("AT+MARJ0x" + major_string.toUpperCase());
		txQueue.add("AT+MINO0x" + minor_string.toUpperCase());
		txQueue.add("AT+ADVI" + mtransmit_interval_slider.getProgress());
		txQueue.add("AT+POWE" + moutput_power_slider.getProgress());
		txQueue.add("AT+BATC1");
		
		String new_name = mname_field_input.getText().toString();
		for(int i=new_name.length();i<11;i++)
		{
			new_name +=" ";
		}
		//txQueue.add("AT+NAMEGLBeacon");
		
		if (mpin_field_input.getText().length() >= 4 && mpin_field_input.getText().length() <= 6  ) {
			txQueue.add("AT+TYPE2");
			txQueue.add("AT+PASS" + mpin_field_input.getText());
		} else {
			txQueue.add("AT+TYPE0");
			txQueue.add("AT+PASS000000");
		}
		//System.out.println("String: "+ new_name + " Length: "+ new_name.length());
		if(!mname_field_input.getText().toString().equals(beacon_name)) txQueue.add("AT+NAME" + new_name);
		transmitQueue();
	}
	
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        System.out.println("BLLLLLLAAA: "+ requestCode + " lol: "+resultCode );
        super.onActivityResult(requestCode, resultCode, data);
    }

	public void resetAllValues(View view) {
		mydialog = new Dialog(this);
        mydialog.addContentView(new ProgressBar(this),
                new LayoutParams(40, 40));
        mydialog.setTitle("Saving Settings");
        mydialog.setCancelable(false);
        mydialog.setCanceledOnTouchOutside(false);
        timerDelayRemoveDialog(5000, mydialog,false);
        timerDelayRemoveDialog(5000, mydialog,true);
        timeout=true;
        mydialog.show();
		txQueue.add("AT+MARJ0x0001");
		txQueue.add("AT+MINO0x0001");
		txQueue.add("AT+ADVI9");
		txQueue.add("AT+POWE2");
		txQueue.add("AT+BATC1");
//		txQueue.add("AT+MEAS0x0001");
		txQueue.add("AT+NAMEGLBeacon");
		txQueue.add("AT+TYPE0");
		txQueue.add("AT+PASS000000");
		transmitQueue();
	}

	public void resetConnection() {
		if (mBluetoothLeService != null) {
			mBluetoothLeService.disconnect();
		
			mBluetoothLeService.connect(mDeviceAddress);
		//	unbindService(mServiceConnection);
		//	mBluetoothLeService = null;
		}
		
	//	Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
	//	bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

	}

	@Override
	public void onCancel(DialogInterface dialog) {
        
		if (mBluetoothLeService != null) 
		{
		mBluetoothLeService.disconnect();
		}
		onBackPressed();
		
		
	}

}