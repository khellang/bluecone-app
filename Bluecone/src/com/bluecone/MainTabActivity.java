package com.bluecone;


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
import android.view.Window;
import android.widget.TabHost;
import android.widget.Toast;

public class MainTabActivity extends TabActivity {
	
	

	public static final String REFRESH_FILTER ="com.brownfield.bluecone.REFRESH";
	public static final String WRITE_FILTER = "com.brownfield.bluecone.WRITE";
	
	private static final String TAG = "Tabactivity";
	private static final boolean D = true;

	private static final int REQUEST_ENABLE = 1;
	private static final int REQUEST_DEVICE = 2;
	private BluetoothAdapter bluetoothAdapter;
	protected static DeviceConnector deviceConnector;	
	protected static TabHost tabHost;
	public static final String TRACK_WRITE="track_write";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(D)Log.d(TAG, "oncreate...");

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
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

		tabHost.setCurrentTab(0);
	}

	@Override
	public void onStart(){
		super.onStart();
		IntentFilter writeIntent = new IntentFilter(WRITE_FILTER);
		this.registerReceiver(receiver, writeIntent);
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
			
			//Intent refreshIntent = new Intent(REFRESH_FILTER);
			Intent refreshIntent = new Intent(REFRESH_FILTER);
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
			String path = "ADD#"+intent.getStringExtra(TRACK_WRITE);
			deviceConnector.write(path.getBytes());
		}
	};

}