package com.bluecone;


import java.util.HashMap;

import com.bluecone.connect.DeviceConnector;
import com.bluecone.connect.DeviceFinder;
import android.app.TabActivity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

public class MainTabActivity extends TabActivity {
	
	

	public static final String REFRESH ="com.bluecone.REFRESH";
	public static final String REQUEST_WRITE = "com.bluecone.REQUEST_WRITE";
	public static final String DEVICE_CONNECTED = "com.bluecone.CONNECTED_FILTER";
	public static final String REQUEST_TRANSMITT  = "com.bluecone.REQUEST_TRANSMITT";
	public static final String START_TRANSMITT  = "com.bluecone.START_TRANSMITT";
	private static final int WRITE =0;
	private static final int CONNECTED = 1;
	private static final int TRANSMITT = 2;
	private static final int TRANSMITTING = 3;
	
	public static final String PROGRESS = "progress";
	private int max;
	private int  progress;
	private static final String TAG = "Tabactivity";
	private static final boolean D = true;

	private static final int REQUEST_ENABLE = 1;
	private static final int REQUEST_DEVICE = 2;
	private BluetoothAdapter bluetoothAdapter;
	protected static DeviceConnector deviceConnector;	
	protected static TabHost tabHost;
	public static final String TRACK_WRITE="track_write";
	private static final HashMap<String, Integer> actionMap;
	private ProgressBar progressHorizontal;
	private TextView title_right;
	private TextView title_left;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(D)Log.d(TAG, "oncreate...");

		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.main);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
		title_right = (TextView) findViewById(R.id.custom_title_right);
		title_right.setText(R.string.not_connected);
		title_left = (TextView) findViewById(R.id.custom_title_left);

		
		 progressHorizontal = (ProgressBar) findViewById(R.id.progress_horizontal);
		 progressHorizontal.setVisibility(View.GONE);
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		deviceConnector = new DeviceConnector();
		BlueconeContext.setBlueconeContext(this);
		if(bluetoothAdapter==null){
			Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show();
			finish();
		}

		tabHost = getTabHost();
		TabHost.TabSpec tabSpec;
		Resources resources = getResources();

		Intent tabIntent;
		tabIntent = new Intent().setClass(this,ArtistListActivity.class);

		tabSpec = tabHost.newTabSpec("artist").setIndicator("Artist",resources.getDrawable(R.drawable.ic_artist)).setContent(tabIntent);
		tabHost.addTab(tabSpec);

		tabIntent = new Intent().setClass(this, AlbumListActivity.class);
		tabSpec = tabHost.newTabSpec("album").setIndicator("Album",resources.getDrawable(R.drawable.ic_album)).setContent(tabIntent);
		tabHost.addTab(tabSpec);

		tabIntent = new Intent().setClass(this, TrackListActivity.class);
		tabSpec = tabHost.newTabSpec("track").setIndicator("Track",resources.getDrawable(R.drawable.ic_track)).setContent(tabIntent);
		tabHost.addTab(tabSpec);

		tabIntent = new Intent().setClass(this, QueueListActivity.class);
		tabSpec = tabHost.newTabSpec("queue").setIndicator("Queue",resources.getDrawable(R.drawable.ic_queue)).setContent(tabIntent);
		tabHost.addTab(tabSpec);

		tabHost.setCurrentTab(3);
		tabHost.setCurrentTab(2);
		tabHost.setCurrentTab(1);
		tabHost.setCurrentTab(0);
	}

	@Override
	public void onStart(){
		super.onStart();
		IntentFilter writeIntent = new IntentFilter(REQUEST_WRITE);
		IntentFilter connectedIntent = new IntentFilter(DEVICE_CONNECTED);
		IntentFilter transmittIntent = new IntentFilter(REQUEST_TRANSMITT);
		IntentFilter startTransmittIntent = new IntentFilter(START_TRANSMITT);
		this.registerReceiver(receiver, writeIntent);
		this.registerReceiver(receiver,connectedIntent);
		this.registerReceiver(receiver,transmittIntent);
		this.registerReceiver(receiver,startTransmittIntent);
		
		if(!bluetoothAdapter.isEnabled()){
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE);
		}
	}
	@Override
	public void onDestroy(){
		super.onDestroy();
		//finish();
	}
	@Override	
	public void onActivityResult(int requestCode,int resultCode,Intent data){
		switch(requestCode){
		case REQUEST_ENABLE:
			if(resultCode!=RESULT_OK)
				finish();
			break;
		case REQUEST_DEVICE:
			if(resultCode==RESULT_OK){
				String mac = data.getExtras().getString(DeviceFinder.EXTRA_UNIT_ADDRESS);
				deviceConnector.connect(mac);
			}
			break;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu){
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item){
		switch(item.getItemId()){
		case R.id.scan:
			Intent intent = new Intent(DeviceFinder.REQUEST_CONNECT);
			startActivityForResult(intent, REQUEST_DEVICE);
			break;
		case R.id.back:
			Intent refreshIntent = new Intent(REFRESH);
			sendBroadcast(refreshIntent);
			break;
		case R.id.search:
			break;
		}
		return true;
	}
	

	
	private BroadcastReceiver receiver = new BroadcastReceiver() {
		

		@Override
		public void onReceive(Context context, Intent intent) {
			switch(actionMap.get(intent.getAction())){
			case WRITE:
				String path = "ADD#"+intent.getStringExtra(TRACK_WRITE);
				deviceConnector.write(path.getBytes());
				break;
			case CONNECTED:
				title_right.setText(R.string.connected);
				break;
			case TRANSMITT:
				title_left.setText(R.string.transfer);
				 max = intent.getIntExtra(PROGRESS , 10000);
				 progressHorizontal.setMax(max);
				 progressHorizontal.setVisibility(View.VISIBLE);			 
				 progress = 0;				
				break;
			case TRANSMITTING:
				progressHorizontal.incrementProgressBy(1);
				Intent update_intent = new Intent(REFRESH);
				sendBroadcast(update_intent);
				if((++progress)>=max){
					title_left.setText("");
					progressHorizontal.setVisibility(View.GONE);
				max=0;
				progress =0;
				}
				break;
			}
		}
	};

	static{
		actionMap = new HashMap<String, Integer>();
		actionMap.put(REQUEST_WRITE, WRITE);
		actionMap.put(DEVICE_CONNECTED, CONNECTED);
		actionMap.put(REQUEST_TRANSMITT, TRANSMITT);
		actionMap.put(START_TRANSMITT, TRANSMITTING);
	}
}