package com.bluecone;
import java.util.HashMap;
import com.bluecone.connect.DeviceConnector;
import com.bluecone.intent.Bluecone_intent;
import debug.Debug;
import android.app.AlertDialog;
import android.app.TabActivity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

public class MainTabActivity extends TabActivity {



	/**Receiver handling*/
	private static final int WRITE = 0;
	private static final int CONNECTED = 1;
	private static final int TRANSMITT = 2;
	private static final int TRANSMITTING = 3;
	private static final int DISCONNECTED = 4;
	private static final int MASTER = 5;
	private static final int NOW_PLAYING = 6;

	/**Activity result handling*/
	private static final int REQUEST_ENABLE = 1;
	private static final int REQUEST_DEVICE = 2;

	/**For internal use only*/
	private static final HashMap<String, Integer> actionMap;
	private BluetoothAdapter bluetoothAdapter;
	private ProgressBar progressHorizontal;
	private TextView title_right;
	private TextView title_left;
	private static TextView title_center;
	private int max;
	private int progress;

	/**Instances to be used by other classes in this package */
	protected static DeviceConnector deviceConnector;
	protected static TabHost tabHost;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(Debug.D)
			Log.d(Debug.TAG_MAIN, "oncreate...");

		requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
		setContentView(R.layout.main);
		getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE,
				R.layout.custom_title);
		title_left = (TextView) findViewById(R.id.custom_title_left);
		title_left.setText(R.string.app_name);
		title_right = (TextView) findViewById(R.id.custom_title_right);
		title_right.setText(R.string.not_connected);
		title_center = (TextView) findViewById(R.id.custom_title_center);
		progressHorizontal = (ProgressBar) findViewById(R.id.progress_horizontal);
		progressHorizontal.setVisibility(View.GONE);
		bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		deviceConnector = new DeviceConnector();
		BlueconeContext.setBlueconeContext(this);
		if (bluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG)
			.show();
			finish();
		}
		tabHost = getTabHost();
		TabHost.TabSpec tabSpec;
		Resources resources = getResources();

		Intent tabIntent;
		tabIntent = new Intent().setClass(this, ArtistListActivity.class);
		tabSpec = tabHost
		.newTabSpec("artist")
		.setIndicator("Artist",
				resources.getDrawable(R.drawable.ic_artist))
				.setContent(tabIntent);
		tabHost.addTab(tabSpec);

		tabIntent = new Intent().setClass(this, AlbumListActivity.class);
		tabSpec = tabHost
		.newTabSpec("album")
		.setIndicator("Album",
				resources.getDrawable(R.drawable.ic_album))
				.setContent(tabIntent);
		tabHost.addTab(tabSpec);

		tabIntent = new Intent().setClass(this, TrackListActivity.class);
		tabSpec = tabHost
		.newTabSpec("track")
		.setIndicator("Track",
				resources.getDrawable(R.drawable.ic_track))
				.setContent(tabIntent);
		tabHost.addTab(tabSpec);

		tabIntent = new Intent().setClass(this, QueueActivity.class);
		tabSpec = tabHost
		.newTabSpec("queue")
		.setIndicator("Queue",
				resources.getDrawable(R.drawable.ic_queue))
				.setContent(tabIntent);
		tabHost.addTab(tabSpec);
		tabHost.setCurrentTab(3);
		tabHost.setCurrentTab(2);
		tabHost.setCurrentTab(1);
		tabHost.setCurrentTab(0);

	}


	/**Register receiver with necessary intent filters   */
	@Override
	public void onStart() {
		super.onStart();
		Log.d(Debug.TAG_MAIN, "onStart");
		IntentFilter writeIntent = new IntentFilter(Bluecone_intent.REQUEST_WRITE);
		IntentFilter connectedIntent = new IntentFilter(Bluecone_intent.DEVICE_CONNECTED);
		IntentFilter transmittIntent = new IntentFilter(Bluecone_intent.REQUEST_TRANSMITT);
		IntentFilter startTransmittIntent = new IntentFilter(Bluecone_intent.START_TRANSMITT);
		IntentFilter disconnectedIntent = new IntentFilter(Bluecone_intent.CONNECTION_LOST);
		IntentFilter masterIntent = new IntentFilter(Bluecone_intent.REQUEST_MASTER);
		IntentFilter currentTrackIntent = new IntentFilter(Bluecone_intent.SET_NOW_PLAYING);
		this.registerReceiver(receiver, writeIntent);
		this.registerReceiver(receiver, connectedIntent);
		this.registerReceiver(receiver, transmittIntent);
		this.registerReceiver(receiver, startTransmittIntent);
		this.registerReceiver(receiver, disconnectedIntent);
		this.registerReceiver(receiver, masterIntent);
		this.registerReceiver(receiver, currentTrackIntent);

		if (!bluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE);
		}
	}

	/**Unregister receiver */
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.unregisterReceiver(receiver);

		if(Debug.D)Log.d(Debug.TAG_MAIN, "onDestroy");
	}

	/**Handle callback from sent intents*/
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_ENABLE:
			Log.d(Debug.TAG_MAIN, "onActivityResult: REQUEST_ENABLE");
			if (resultCode != RESULT_OK)
				finish();
			break;
		case REQUEST_DEVICE:
			Log.d(Debug.TAG_MAIN, "onActivityResult: REQUEST_DEVICE");
			if (resultCode == RESULT_OK) {
				String mac = data.getExtras().getString(
						Bluecone_intent.EXTRA_UNIT_ADDRESS);
				deviceConnector.connect(mac);
			}
			break;
		}
	}

	/**Create menu*/
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(Debug.TAG_MAIN, "onCreateOptionsMenu");
		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.menu, menu);
		return true;
	}

	/**Perform described actions in menu*/
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.scan:
			Log.d(Debug.TAG_MAIN, "MenueSelected: scan");
			Intent intent = new Intent(Bluecone_intent.REQUEST_CONNECT);
			startActivityForResult(intent, REQUEST_DEVICE);
			break;
		case R.id.back:
			Log.d(Debug.TAG_MAIN, "MenueSelected: back");
			Intent refreshIntent = new Intent(Bluecone_intent.REFRESH);
			sendBroadcast(refreshIntent);
			break;
		case R.id.master:
			Log.d(Debug.TAG_MAIN, "MenueSelected: master");
			AlertDialog.Builder alert = new AlertDialog.Builder(this);

			alert.setTitle("Master Mode");
			alert.setMessage("Password:");

			// Set an EditText view to get user input
			final EditText input = new EditText(this);
			alert.setView(input);

			alert.setPositiveButton("Log in",
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog,
						int whichButton) {
					String value = input.getText().toString();
					Intent writeIntent = new Intent(
							Bluecone_intent.REQUEST_MASTER);
					writeIntent.putExtra(
							Bluecone_intent.EXTRA_MASTER_COMMAND, "MASTER#"
							+ value);
					sendBroadcast(writeIntent);
				}
			});

			alert.setNegativeButton("Cancel",
					new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog,
						int whichButton) {
					// Canceled.
				}
			});

			alert.show();
			break;
		case R.id.exit:
			android.os.Process.killProcess(android.os.Process.myPid());
			break;
		}
		return true;
	}

	/**Treat relevant broadcasts*/
	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			switch (actionMap.get(intent.getAction())) {
			case WRITE:
				if(Debug.D)
					Log.d(Debug.TAG_MAIN, "BroadcastReceiver: WRITE");
				String write = intent.getStringExtra(Bluecone_intent.EXTRA_COMMAND) + intent.getStringExtra(Bluecone_intent.EXTRA_BLUECONE_WRITE);
				deviceConnector.write(write.getBytes());
				break;
			case CONNECTED:
				if(Debug.D)
					Log.d(Debug.TAG_MAIN, "BroadcastReceiver: CONNECTED");
				title_right.setText(R.string.connected);
				break;
			case TRANSMITT:
				if(Debug.D)
					Log.d(Debug.TAG_MAIN, "BroadcastReceiver: Transmitt");
				title_left.setText(R.string.transfer);
				max = intent.getIntExtra(Bluecone_intent.EXTRA_PROGRESS_MAX, 100);
				progressHorizontal.setMax(max);
				progressHorizontal.setVisibility(View.VISIBLE);
				progress = 0;
				break;
			case TRANSMITTING:
				if(Debug.D)
					Log.d(Debug.TAG_MAIN, "BroadcastReceiver: Transmitting");
				progressHorizontal.incrementProgressBy(1);


				if ((++progress) >= max) {
					title_left.setText(R.string.app_name);
					progressHorizontal.setVisibility(View.GONE);
					max = 0;
					progress = 0;
				}
				break;
			case DISCONNECTED:
				if(Debug.D)
					Log.d(Debug.TAG_MAIN, "BroadcastReceiver: Disconnected");
				title_right.setText(R.string.not_connected);
				title_center.setText("");
				stopCenterTicker();
				break;
			case MASTER:
				if(Debug.D)
					Log.d(Debug.TAG_MAIN, "BroadcastReceiver: MASTER");
				String masterCommand = intent.getStringExtra(Bluecone_intent.EXTRA_MASTER_COMMAND);
				deviceConnector.write(masterCommand.getBytes());
				break;
			case NOW_PLAYING:
				String textTicker = intent.getStringExtra(Bluecone_intent.EXTRA_NOW_PLAYING_ARTIST)+" - "+
				intent.getStringExtra(Bluecone_intent.EXTRA_NOW_PLAYING_TRACK);
				if(Debug.D)Log.d(Debug.TAG_MAIN, "NOW PLAYING: "+textTicker);
				stopCenterTicker();
				centerTickHandler.sendMessage(centerTickHandler.obtainMessage(0,textTicker.substring(0, textTicker.length())));
				if(textTicker.length()>15)
					inputText(textTicker);
			}
		}
	};

	private static Handler centerTickHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			title_center.setText((CharSequence)msg.obj);
		}
	};


	private volatile Thread centerTicker;
	private synchronized void inputText(final String track_info) {

		if(Debug.D)Log.d(Debug.TAG_MAIN, "inputText()");
		if(centerTicker==null){
			centerTicker = new Thread(new Runnable() {
				@Override
				public void run() {
					Log.d(Debug.TAG_MAIN, "RUN:::");
					int pos = 0;
					String extended_string = track_info;


					while(Thread.currentThread() == centerTicker){
						if((pos+15)<extended_string.length()){
							centerTickHandler.sendMessage(centerTickHandler.obtainMessage(0,extended_string.substring(++pos, (pos+15))));

							try {
								Thread.sleep(450);
							} catch (InterruptedException e) {
								Log.d(Debug.TAG_MAIN, "Interrupted exception");
							}

						}
						else{
							extended_string = extended_string.subSequence(pos, pos+15)+" - "+track_info;
							pos = 0;
						}		
					}


				}
			});
			centerTicker.start();

		}

	}

	private synchronized void stopCenterTicker(){
		if(centerTicker != null){
			Thread interrupter = centerTicker;
			centerTicker = null;
			interrupter.interrupt();
		}
	}




	/**Fill hashmap*/
	static {
		actionMap = new HashMap<String, Integer>();
		actionMap.put(Bluecone_intent.REQUEST_WRITE, WRITE);
		actionMap.put(Bluecone_intent.DEVICE_CONNECTED, CONNECTED);
		actionMap.put(Bluecone_intent.REQUEST_TRANSMITT, TRANSMITT);
		actionMap.put(Bluecone_intent.START_TRANSMITT, TRANSMITTING);
		actionMap.put(Bluecone_intent.CONNECTION_LOST, DISCONNECTED);
		actionMap.put(Bluecone_intent.REQUEST_MASTER, MASTER);
		actionMap.put(Bluecone_intent.SET_NOW_PLAYING, NOW_PLAYING);
	}
}