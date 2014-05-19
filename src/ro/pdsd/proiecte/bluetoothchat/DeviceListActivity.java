package ro.pdsd.proiecte.bluetoothchat;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.*;
import android.content.*;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

public class DeviceListActivity extends Activity{
	// Tag for info messages
	private static final String TAG = "BluetoothChat";
	
	// Return for intent
	public static final String EXTRA_DEVICE_ADDRESS = "android";

	// 
	private ArrayAdapter<String> pairedDevicesAdap;
	private ArrayAdapter<String> newDevicesAdap;
	private BluetoothAdapter BTAdap;
	
	private BroadcastReceiver receiver = new BroadcastReceiver(){

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			// Finding a device 
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// Get BT device
				BluetoothDevice bd = intent.getParcelableExtra
						(BluetoothDevice.EXTRA_DEVICE);
				// If already paired, we don't do anything
				if (bd.getBondState() != BluetoothDevice.BOND_BONDED)
					newDevicesAdap.add(bd.getName() + "\t" + bd.getAddress());
			}
			// Done scanning
			else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				setProgressBarIndeterminateVisibility(false);
				setTitle(R.string.select_device);
				
				// If we didn't find anything, set a string to show the state
				if (newDevicesAdap.getCount() == 0) {
					String noDevices = getResources().getText(R.string.none_found).toString();
					newDevicesAdap.add(noDevices);
				}				
			}
		}
	};
	
	private OnItemClickListener onClickList = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			// We are connecting now
			BTAdap.cancelDiscovery(); 
			
			// Get MAC address of device (last 17 characters)
			String info = ((TextView) view).getText().toString();
			String address = null;
			try{
				address = info.substring(info.length() - 17);
			}
			catch(Exception e){
				e.printStackTrace();
			}
			
			// Create intent
			Intent intent = new Intent();
			intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
			
			// Set result
			setResult(RESULT_OK, intent);
			finish();
		}
	};

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Set content for window
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.device_list);
		
		// Set result as canceled if user cancels action
		setResult(RESULT_CANCELED);
		
		// Init button for device discovery
		Button scanBut = (Button) findViewById(R.id.button_scan);
		scanBut.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				doDiscovery();
				v.setVisibility(View.GONE);
			}
		});
		
		// Set array adapters
		pairedDevicesAdap = new ArrayAdapter<String>(this, R.layout.device_name);
		newDevicesAdap = new ArrayAdapter<String>(this, R.layout.device_name);
		
		// Set up the list view for paired devices
		ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
		pairedListView.setAdapter(pairedDevicesAdap);
		pairedListView.setOnItemClickListener(onClickList);
		
		// Set up the list view for newly discovered devices
		ListView newListView = (ListView) findViewById(R.id.new_devices);
		newListView.setAdapter(newDevicesAdap);
		newListView.setOnItemClickListener(onClickList);
		
		// Register broadcasts when device is discovered
		IntentFilter inf = new IntentFilter(BluetoothDevice.ACTION_FOUND); 
		this.registerReceiver(receiver, inf);
		
		// Register broadcasts when discovery finished
		inf = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED); 
		this.registerReceiver(receiver, inf);
		
		// Acquire local BT adapter
		BTAdap = BluetoothAdapter.getDefaultAdapter();
		
		// Get the currentyly paired devices
		Set<BluetoothDevice> setPairedDevices = BTAdap.getBondedDevices(); 
		
		if (setPairedDevices.size() > 0) {
			findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
			for (BluetoothDevice bd : setPairedDevices)
				pairedDevicesAdap.add(bd.getName() + "\t" + bd.getAddress());
		}
		else {
			String noDevices = getResources().getText(R.string.none_paired).toString();
			pairedDevicesAdap.add(noDevices);			
		}
	}
	
	protected void doDiscovery() {
		Log.i(TAG, "doDiscovery()");
		
		// Indicate scanning in the title
		setProgressBarIndeterminateVisibility(true);
		setTitle(R.string.scanning);
		
		// Turn title for new devices
		findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);
		
		// If discovering is in progress, stop it
		if (BTAdap.isDiscovering())
			BTAdap.cancelDiscovery();
		
		// Start discovery
		BTAdap.startDiscovery();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		// If enabled, verify to stop discovery
		if (BTAdap.isEnabled())
			BTAdap.cancelDiscovery();
		
		this.unregisterReceiver(receiver);
		
	}
	
}
