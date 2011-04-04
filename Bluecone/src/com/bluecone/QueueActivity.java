package com.bluecone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import com.bluecone.intent.Bluecone_intent;
import com.bluecone.storage.ArtistList.Track;
import debug.Debug;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class QueueActivity extends Activity {


	private static final int CLEAR = 0;
	private static final int UPDATE = 1;
	private static final int MASTER = 2;
	private static final int REMOVE = 3;
	private static final int DECODE = 4;
	private static final int LOST_CONNECTION = 5;
	private static final int RESET_PROGRESS = 6;
	private static final HashMap<String, Integer> actionMap;
	private LayoutInflater layoutInflater;
	private static List<String> trackHolder;
	private static List<String> pathHolder;
	private QueueBaseAdapter queueBaseAdapter;
	private Cursor cursor;
	private ListView listView;
	private ToggleButton pri;
	private ImageButton stop;
	private ImageButton play;
	private ImageButton next;
	private ImageButton volume_up;
	private ImageButton volume_down;
	private static SeekBar seekbar;
	private static int seconds;
	private static float percent;
	private String selection;
	private String[]selectionArgs;
	private boolean isMaster;





	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.queue_layout);
		isMaster = false;
		trackHolder = new ArrayList<String>();
		pathHolder = new ArrayList<String>();
		pri = (ToggleButton)findViewById(R.id.toggleButton1);
		stop = (ImageButton)findViewById(R.id.imageButton2);
		play = (ImageButton)findViewById(R.id.imageButton3);
		next = (ImageButton)findViewById(R.id.imageButton4);
		volume_up = (ImageButton) findViewById(R.id.volume_up);
		volume_down = (ImageButton) findViewById(R.id.volume_down);
		listView = (ListView) findViewById(R.id.queue_list);
		seekbar = (SeekBar) findViewById(R.id.seekBar1);
		seekbar.setEnabled(false);
		seekbar.setMax(100);
		queueBaseAdapter = new QueueBaseAdapter();
		layoutInflater = (LayoutInflater) BlueconeContext.getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		cursor = BlueconeContext.getContext().getContentResolver().query(Track.CONTENT_URI, new String[] { BaseColumns._ID, Track.TITLE, 
				Track.ALBUM_TITLE, Track.ARTIST_NAME,Track.PATH }, null, null, null);
		startManagingCursor(cursor);
		listView.setAdapter(queueBaseAdapter);
		registerForContextMenu(listView);
		setMaster(false,true);

	}

	@Override
	public void onStart(){
		super.onStart();
		IntentFilter clearQueueIntent = new IntentFilter(Bluecone_intent.RECONNECT);
		IntentFilter queueIntent = new IntentFilter(Bluecone_intent.UPDATE_QUEUE);
		IntentFilter masterIntent = new IntentFilter(Bluecone_intent.MASTER_MODE);
		IntentFilter removeIntent = new IntentFilter(Bluecone_intent.REMOVE);
		IntentFilter progressIntent = new IntentFilter(Bluecone_intent.DECODE);
		IntentFilter lostConnectionIntent = new IntentFilter(Bluecone_intent.CONNECTION_LOST);
		IntentFilter resetTrack_tick = new IntentFilter(Bluecone_intent.SET_NOW_PLAYING);
		this.registerReceiver(receiver, clearQueueIntent);
		this.registerReceiver(receiver, queueIntent);
		this.registerReceiver(receiver, masterIntent);
		this.registerReceiver(receiver, removeIntent);
		this.registerReceiver(receiver, progressIntent);
		this.registerReceiver(receiver, lostConnectionIntent);
		this.registerReceiver(receiver, resetTrack_tick);

	}

	@Override
	public void onResume(){
		super.onResume();

	}
	@Override
	public void onPause(){
		super.onPause();

	}
	/**Unregister receiver */
	@Override
	public void onDestroy() {
		super.onDestroy();
		this.unregisterReceiver(receiver);
		
		if(Debug.D)Log.d(Debug.TAG_MAIN, "onDestroy");
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {



		@Override
		public void onReceive(Context context, Intent intent) {
			switch(actionMap.get(intent.getAction())){
			case CLEAR:
				trackHolder.clear();
				pathHolder.clear();
				update();
				break;
			case UPDATE:
				selection = Track.PATH+"=? ";
				selectionArgs = new String[]{intent.getStringExtra(Track.PATH)};

				cursor = BlueconeContext.getContext().getContentResolver().query(Track.CONTENT_URI,
						new String[] {BaseColumns._ID,Track.TITLE,}
				, selection, selectionArgs, null);
				cursor.moveToFirst();

				int pos = Integer.parseInt(intent.getStringExtra(Bluecone_intent.EXTRA_POS));
				if(Debug.D)Log.d(Debug.TAG_QUEUE, "input: "+selectionArgs[0]+" pos= "+pos+" \ntrackholder.size= "+trackHolder.size()+
						"\npathHolder.size= "+pathHolder.size());
				
				if(pos<=trackHolder.size()){
					trackHolder.add(pos, cursor.getString(1));
					pathHolder.add(pos, selectionArgs[0]);
					
				}else{
					trackHolder.add(cursor.getString(1));
					pathHolder.add(selectionArgs[0]);
					
				}
				cursor.close();
				update();



				break;
			case MASTER:
				Log.d(Debug.TAG_QUEUE, "IS_MASTER= "+intent.getBooleanExtra(Bluecone_intent.EXTRA_IS_MASTER, false));
				setMaster(intent.getBooleanExtra(Bluecone_intent.EXTRA_IS_MASTER, false),intent.getBooleanExtra(Bluecone_intent.EXTRA_PRIORITY_ENABLED, true));
				break;
			case REMOVE:
				if(Debug.D)Log.d(Debug.TAG_QUEUE, "REMOVE");
				int removeIndex = intent.getIntExtra(Bluecone_intent.EXTRA_REMOVE_POS, 0);
				try{
					trackHolder.remove(removeIndex);
					pathHolder.remove(removeIndex);
				}catch(IndexOutOfBoundsException e){
					Log.d(Debug.TAG_QUEUE, "Exception in REMOVE Line: 167");
				}
				update();			
				break;
			case DECODE:
				if(Debug.D)Log.d(Debug.TAG_QUEUE, "SET_DPS");
				stopSeekBar();
				seconds = intent.getIntExtra(Bluecone_intent.EXTRA_CURRENT_SECONDS, 0);
				percent = intent.getFloatExtra(Bluecone_intent.EXTRA_CURRENT_PERCENT, 0);
				Log.d(Debug.TAG_QUEUE, "Second = "+seconds+"\npercent = "+percent);
				float totalTime = (100*seconds)/percent-seconds;
				float tickTime = totalTime/100.0f*1000.0f;
				Log.d(Debug.TAG_QUEUE, "TotalTime = "+totalTime);
				startSeekBar((int)tickTime);


				break;
			case LOST_CONNECTION:
				stopSeekBar();
				trackHolder.clear();
				pathHolder.clear();
				update();
				break;
			case RESET_PROGRESS:
				stopSeekBar();
				break;
			}


		}
		

		private volatile Thread seekbarProgressThread;
		private synchronized void startSeekBar(final int tickTime) {
			if(seekbarProgressThread==null){
				seekbarProgressThread = new Thread(new Runnable() {
					
					@Override
					public void run() {
						Log.d(Debug.TAG_QUEUE, "Tick= "+tickTime);
						while(Thread.currentThread() == seekbarProgressThread){
							seekBarHandler.sendMessage(seekBarHandler.obtainMessage(DECODE,1, 0));

							try {
								Thread.sleep(tickTime);
							} catch (InterruptedException e) {
								if(Debug.D)Log.d(Debug.TAG_QUEUE, "PROGRESSTHREAD "+ seconds);	
							}
						
					}
					}
				});
				seekbarProgressThread.start();
				
			}

		}
		
		private synchronized void stopSeekBar(){
			seekbar.setProgress(0);
			  if(seekbarProgressThread != null){
			    Thread interrupter = seekbarProgressThread;
			    seekbarProgressThread = null;
			    interrupter.interrupt();
			  }
			}

	};


	private static Handler seekBarHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			switch(msg.what){
			case DECODE:
				seekbar.incrementProgressBy(msg.arg1);
				break;
			}
		}
	};

	



	private void setMaster(boolean master,boolean priority){
		isMaster = master;
		if (!master) {
			pri.setVisibility(Button.GONE);
			stop.setVisibility(Button.GONE);
			play.setVisibility(Button.GONE);
			next.setVisibility(Button.GONE);
			volume_up.setVisibility(Button.GONE);
			volume_down.setVisibility(Button.GONE);
		} else {
			pri.setVisibility(Button.VISIBLE);
			pri.setChecked(priority);
			stop.setVisibility(Button.VISIBLE);
			play.setVisibility(Button.VISIBLE);
			next.setVisibility(Button.VISIBLE);
			volume_up.setVisibility(Button.VISIBLE);
			volume_down.setVisibility(Button.VISIBLE);
		}
	}

	private void update(){

		queueBaseAdapter.notifyDataSetChanged();

	}

	private class QueueBaseAdapter extends BaseAdapter{

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if(convertView==null){
				convertView = layoutInflater.inflate(R.layout.queue_entry, null);
				holder = new ViewHolder();
				holder.playing = (TextView) convertView.findViewById(R.id.queue_track_title);
				convertView.setTag(holder);

			}
			else{
				holder = (ViewHolder) convertView.getTag();
			}

			holder.playing.setText((CharSequence) trackHolder.toArray()[position]);
			if(Debug.D)Log.d(Debug.TAG_QUEUE,"getView: "+trackHolder.toArray()[position]);


			return convertView;

		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public Object getItem(int position) {

			return trackHolder.get(position);
		}

		@Override
		public int getCount() {
			return trackHolder.toArray().length;
		}

	};
	private class ViewHolder{
		TextView playing;
		

	}


	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.track_menu, menu);

	}
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.remove:
			if(!isMaster){
				Toast.makeText(BlueconeContext.getContext(), "Master mode required", Toast.LENGTH_SHORT).show();
				return true;
			}
			BlueconeHandler.getHandler().sendMessage(BlueconeHandler.getHandler().obtainMessage(BlueconeHandler.WRITE, "QUEUEREMOVE#"+pathHolder.get(info.position)));
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}


	public void play(View view){
		BlueconeHandler.getHandler().sendMessage(BlueconeHandler.getHandler().obtainMessage(BlueconeHandler.WRITE, "PLAY"));
	}
	public void stop(View view){

		BlueconeHandler.getHandler().sendMessage(BlueconeHandler.getHandler().obtainMessage(BlueconeHandler.WRITE, "STOP"));
	}
	public void next(View view){
		BlueconeHandler.getHandler().sendMessage(BlueconeHandler.getHandler().obtainMessage(BlueconeHandler.WRITE, "NEXT"));
	}
	public void pri(View view){
		int com = 0;
		if(pri.isChecked())
			com=1;
		BlueconeHandler.getHandler().sendMessage(BlueconeHandler.getHandler().obtainMessage(BlueconeHandler.WRITE, "PRI#"+com));
	}
	public void adjustVolumeUp(View view){
		BlueconeHandler.getHandler().sendMessage(BlueconeHandler.getHandler().obtainMessage(BlueconeHandler.WRITE, "VOLUP"));
	}
	public void adjustVolumeDown(View view){
		BlueconeHandler.getHandler().sendMessage(BlueconeHandler.getHandler().obtainMessage(BlueconeHandler.WRITE, "VOLDOWN"));
	}

	static{
		actionMap = new HashMap<String, Integer>();
		actionMap.put(Bluecone_intent.RECONNECT, CLEAR);
		actionMap.put(Bluecone_intent.UPDATE_QUEUE, UPDATE);
		actionMap.put(Bluecone_intent.MASTER_MODE, MASTER);
		actionMap.put(Bluecone_intent.MASTER_MODE, MASTER);
		actionMap.put(Bluecone_intent.REMOVE, REMOVE);
		actionMap.put(Bluecone_intent.DECODE, DECODE);
		actionMap.put(Bluecone_intent.CONNECTION_LOST, LOST_CONNECTION);
		actionMap.put(Bluecone_intent.SET_NOW_PLAYING, RESET_PROGRESS);

	}

}
