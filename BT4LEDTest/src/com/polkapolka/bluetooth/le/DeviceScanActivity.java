
package com.polkapolka.bluetooth.le;

import android.app.ActionBar;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.UUID;

public class DeviceScanActivity extends ListActivity {
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;

    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 1000;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle(R.string.title_devices);
        ActionBar bar = getActionBar();
        ColorDrawable cd = new ColorDrawable();
        cd.setColor(Color.WHITE);
        bar.setBackgroundDrawable(cd); 
        mHandler = new Handler();
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        ListView lv = getListView();
        lv.setCacheColorHint(0);
        lv.setBackgroundResource(R.drawable.grid);
 
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }
    


    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        // Initializes list view adapter.
        boolean hasList = false;
        if(mLeDeviceListAdapter!=null) hasList=true;
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position).getDevice();
        if (device == null) return;
        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if (mScanning) {
        	mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            
        }
        startActivity(intent);
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                   // mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                 //   mBluetoothAdapter.startLeScan(mLeScanCallback);
                 if(mScanning)   scanLeDevice(true);
                 else invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<IBeacon> ibeacons = new ArrayList<IBeacon>();
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mInflator = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(IBeacon b) {
        	if(ibeacons.contains(b))
        	{
        		int i = ibeacons.indexOf(b);
        		ibeacons.get(i).updateMeasuredPower(b.getMeasuredPower());
        		ibeacons.get(i).setMajor(b.getMajor());
        		ibeacons.get(i).setMinor(b.getMinor());
        		ibeacons.get(i).setName(b.getName());
        	}
        	else ibeacons.add(b);
        }

        public IBeacon getDevice(int position) {
            return ibeacons.get(position);
        }

        public void clear() {
            ibeacons.clear();
        }

        @Override
        public int getCount() {
            return ibeacons.size();
        }

        @Override
        public Object getItem(int i) {
            return ibeacons.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                viewHolder.rssi = (TextView) view.findViewById(R.id.rssi);
                viewHolder.major = (TextView) view.findViewById(R.id.major);
                viewHolder.minor = (TextView) view.findViewById(R.id.minor);
                viewHolder.battery = (TextView) view.findViewById(R.id.battery);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            IBeacon beacon = ibeacons.get(i);           
            final String deviceName = beacon.getDevice().getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(beacon.getDevice().getAddress());

            viewHolder.rssi.setText(String.valueOf(beacon.getDistance(100)));
            viewHolder.major.setText(String.valueOf(beacon.getMajor()));
            viewHolder.minor.setText(String.valueOf(beacon.getMinor()));
            viewHolder.battery.setText(String.valueOf(beacon.getBatteryLevel())+"%");
            return view;
        }
    }

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, final  byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    

					int i;
					int startByte = 5;
					int battery = 0;
					boolean glimwormDevice = false;
					for (i = 0; i < scanRecord.length; i++) {
						if (scanRecord[i] == 0x07) {
							String outstr = "";
							for (int j = i; j < scanRecord.length; j++) {
								int l = (int) scanRecord[j] & 0x000000FF;
								outstr += l + " ";
							}
							battery = scanRecord[i + 7];
							System.out.println(outstr);
							glimwormDevice = true;
							break;
						}
					}
					int power = (int) scanRecord[startByte + 24];
					int major = (scanRecord[startByte + 20] & 0xff) * 0x100
							+ (scanRecord[startByte + 21] & 0xff);
					int minor = (scanRecord[startByte + 22] & 0xff) * 0x100
							+ (scanRecord[startByte + 23] & 0xff);
					
					IBeacon ib = new IBeacon(device,device.getAddress(),device.getName(),major,minor,power,rssi,battery);
				
					mLeDeviceListAdapter.addDevice(ib);
                    mLeDeviceListAdapter.notifyDataSetChanged();
				
                }
            });
        }
    };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView major;
        TextView minor;
        TextView rssi;
        TextView battery;
    }
}


