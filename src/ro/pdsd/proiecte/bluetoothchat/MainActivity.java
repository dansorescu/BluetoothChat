package ro.pdsd.proiecte.bluetoothchat;

import android.annotation.SuppressLint;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import android.widget.TextView.*;

public class MainActivity extends Activity {
	// Tag for info messages
	private static final String TAG = "BluetoothChat";
	
	// Tags for pop-up messages
	public static final String DEVICE_NAME = "android";
	public static final String TOAST = "pop-up";
	
	private String connectedDevice = null;
	
	// Messages types
	public static final int MSG_STATE_CHANGE = 1;
    public static final int MSG_READ = 2;
    public static final int MSG_WRITE = 3;
    public static final int MSG_DEVICE_NAME = 4;
    public static final int MSG_TOAST = 5;
    
    // Internal codes for requests
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    
	private ListView convView;
	private Button sendBut;
	private EditText editText;
	
	private OnEditorActionListener textList = 
			new OnEditorActionListener() {
		
				@Override
				public boolean onEditorAction(TextView v, 
						int actionId, KeyEvent event) {
					if (actionId == EditorInfo.IME_NULL && 
							event.getKeyCode() == 
							KeyEvent.KEYCODE_ENTER){
						sendMsg(v.getText().toString());
					}
					Log.i(TAG, "onEditorAction()");
					return true;
				}
				
			};	
	
	
	@SuppressLint("HandlerLeak")
	private final Handler handler = new Handler(){
		
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_STATE_CHANGE:
					Log.i(TAG, "MSG_STATE_CHANGE" + msg.arg1);
					switch (msg.arg1) {
						case BluetoothChatService.STATE_CONNECTED:
							setTitle("Connected to: " + connectedDevice);
							convAdap.clear();
							break;
							
						case BluetoothChatService.STATE_CONNECTING:
							setTitle("Connecting...");
							break;
							
						case BluetoothChatService.STATE_LISTEN:
						case BluetoothChatService.STATE_NONE:
							setTitle("Not connected");
							break;
					}
					break;
					
				case MSG_READ:
					byte[] read = (byte[]) msg.obj;
					// The message from the bytes in the buffer
					String readMsg = new String(read, 0, msg.arg1);
					convAdap.add(connectedDevice + ": " + readMsg);
					break;
					
				case MSG_WRITE:
					byte[] write = (byte[]) msg.obj;
					// Return the string representation
					convAdap.add("Me: " + write.toString());
					break;
					
				case MSG_DEVICE_NAME:
					// Save connected device name
					connectedDevice = msg.getData().getString(DEVICE_NAME);
					Toast.makeText(getApplicationContext(), 
							"Connected to: " + connectedDevice, 
							Toast.LENGTH_SHORT).show();
					break;
					
				case MSG_TOAST:
					Toast.makeText(getApplicationContext(), msg.getData().
							getString(TOAST), Toast.LENGTH_SHORT).show();
					break;
				}
			
		}
	};
	
	private ArrayAdapter<String> convAdap;
	private BluetoothAdapter bAdapter;
	private BluetoothChatService chatServ;
	private StringBuffer outMsgs;
	
    @Override
	public void onCreate(Bundle savedInstanceState){
    	super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate()");

        // set window layout
        setContentView(R.layout.activity_main);
        setTitle(R.string.app_name);
        
        // Get local BT adapter
        bAdapter = BluetoothAdapter.getDefaultAdapter();

        // device is null => no BT available
        if (bAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", 
            		Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }
    
    protected void sendMsg(String string) {
    	// Verify if connected
    	if (chatServ.getState() != BluetoothChatService.
    			STATE_CONNECTED) {
    		Toast.makeText(this, R.string.not_connected, Toast.
    				LENGTH_SHORT).show();
    		return;
    	}
    	
    	// Verify sending message
    	if (string.length() != 0) {
    		// Get bytes from message
    		byte[] msgToSend = string.getBytes();
    		// Pass them to chat service for writing
    		chatServ.write(msgToSend);
    		
    		// EditText becomes void of text
    		outMsgs.setLength(0);
    		editText.setText("");
    	}
	}

	@Override
    public void onStart(){
    	super.onStart();
    	Log.i(TAG, "onStart()");
    	
    	if (bAdapter.isEnabled() != true){
    		Intent enableBlue = new Intent(BluetoothAdapter.
    				ACTION_REQUEST_ENABLE);
    		startActivityForResult(enableBlue, REQUEST_ENABLE_BT);
    	}
    	else if (chatServ == null)
    		setupChatService();    	
    }

	private void setupChatService() {
		Log.i(TAG, "setupChatService()");
		
		// Init adapter for service
		// TODO
		convAdap = new ArrayAdapter<String>(this, R.layout.activity_main);
		convView = (ListView) findViewById(R.id.convView);
		convView.setAdapter(convAdap);
		
		// Init EditText with listener
		editText = (EditText) findViewById(R.id.editText);
		editText.setOnEditorActionListener(textList);
		
		// Init sending button with listener
		sendBut = (Button) findViewById(R.id.sendBut);
		sendBut.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				TextView tv = (TextView) findViewById(R.id.editText);
				sendMsg(tv.getText().toString());
			}
		});
		
		chatServ = new BluetoothChatService(this, handler);
		
		outMsgs = new StringBuffer();
	}
    
	@Override
	public void onResume() {
		super.onResume();
		Log.i(TAG, "onResume()");
		// Verify if BT wasn't activated in onStart(), so we 
		// activate it now
		if (chatServ != null) {
			// Chat starts when state is STATE_NONE
			if (chatServ.getState() == BluetoothChatService.STATE_NONE)
				chatServ.start();
		}
	}
	
	@Override
    public synchronized void onPause(){
    	super.onPause();
    	Log.i(TAG, "onPause()");
    }
    
	@Override
	public void onStop(){
		super.onStop();
		Log.i(TAG, "onStop()");
	}
	
    @Override
    public void onDestroy(){
    	super.onDestroy();
    	chatServ.stop();
    	Log.i(TAG, "onDestroy()");
    }
    
    public void onActivity(int reqCode, int resCode, Intent data) {
    	Log.i(TAG, "onActivity() + resCode");
    	switch(reqCode) {
    		case REQUEST_CONNECT_DEVICE_SECURE:
    			if (resCode == Activity.RESULT_OK)
    				connectToDevice(data, true);
    			break;
    			
    		case REQUEST_CONNECT_DEVICE_INSECURE:
    			if (resCode == Activity.RESULT_OK)
    				connectToDevice(data, false);
    			break;
    			
    		case REQUEST_ENABLE_BT:
    			if (resCode == Activity.RESULT_OK)
    				setupChatService();
    			else {
    				Log.e(TAG, "BT not enabled!");
    				Toast.makeText(this, 
    						R.string.bt_not_enabled_leaving, 
    						Toast.LENGTH_SHORT).show();
                    finish();
    			}
    	}
    }
    
    public void connectToDevice(Intent data, boolean secure) {
    	// MAC address
    	String address = data.getExtras().getString(DeviceListActivity.
    			EXTRA_DEVICE_ADDRESS);
    	// Get device and connect to it
    	BluetoothDevice bd = bAdapter.getRemoteDevice(address);
    	chatServ.connect(bd, secure);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	MenuInflater mi = getMenuInflater();
    	mi.inflate(R.menu.main, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		Intent serverIntent = null;
    	switch (item.getItemId()) {
    		case R.id.secure:
    			// Scanning devices
    			serverIntent = new Intent(this, DeviceListActivity.class);
    			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
    			return true;
    		
    		case R.id.insecure:
    			// Scanning devices
    			serverIntent = new Intent(this, DeviceListActivity.class);
    			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
    			return true;
    	
    		case R.id.discoverable:
    			// Ensure device is discoverable
    			ensureDeviceDiscoverable();
    			return true;
    	}
    	return false;
    }

	private void ensureDeviceDiscoverable() {
		Log.i(TAG, "ensureDeviceDiscoverable()");
		if (bAdapter.getScanMode() != 
				BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discovery = new Intent(BluetoothAdapter.
					ACTION_REQUEST_DISCOVERABLE);
			discovery.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discovery);
		}
	}
    
}
