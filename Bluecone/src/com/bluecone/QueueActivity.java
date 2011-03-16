package com.bluecone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class QueueActivity extends Activity {


	public static final  String MASTER_MODE = "com.bluecone.MASTER_MODE";
	public static final String REMOVE_FIRST_IN_QUEUE = "com.bluecone.REMOVE_FIRST_IN_QUEUE";
	public static final String START_UPDATE_QUEUE = "com.bluecone.START_UPDATE_QUEUE";
	public static final String UPDATE_QUEUE = "com.bluecone.UPDATE_QUEUE";
	public static final String QUEUE_ELEMENTS="elements";
	public static final String PROGRESS="com.bluecone.queueactivity.PROGRESS";
	public static final String CURRENT_PROGRESS="com.bluecone.queueactivity.CURRENT_PROGRESS";
	public static final String POS = "position";
	public static final String MAX = "max";
	public static final String IS_MASTER = "is_master";
	private static final int START = 0;
	private static final int UPDATE = 1;
	private static final int MASTER = 2;
	private static final int REMOVE = 3;
	private static final int SET_PROGRESS = 4;
	private static boolean queuestart_initiated ;
	protected static String NOW_PLAYING  ="com.bluecone.NOW_PLAYING";
	private int max;
	private static final HashMap<String, Integer> actionMap;
	private LayoutInflater layoutInflater;
	private List< String> DATA = new ArrayList<String>();
	private QueueBaseAdapter queueBaseAdapter;
	private Cursor cursor;
	private ListView listView;
	private ImageButton prev;
	private ImageButton stop;
	private ImageButton play;
	private ImageButton next;
	private ImageButton volume_up;
	private ImageButton volume_down;
	private SeekBar seekbar;
	private String nowPlaying;
	private int currentProgress;
	private String selection;
	private String[]selectionArgs;





	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.queue_layout);

		queuestart_initiated = false;
		prev = (ImageButton)findViewById(R.id.imageButton1);
		stop = (ImageButton)findViewById(R.id.imageButton2);
		play = (ImageButton)findViewById(R.id.imageButton3);
		next = (ImageButton)findViewById(R.id.imageButton4);
		volume_up = (ImageButton) findViewById(R.id.volume_up);
		volume_down = (ImageButton) findViewById(R.id.volume_down);
		listView = (ListView) findViewById(R.id.queue_list);
		seekbar = (SeekBar) findViewById(R.id.seekBar1);
		seekbar.setEnabled(false);
		queueBaseAdapter = new QueueBaseAdapter();
		layoutInflater = (LayoutInflater) BlueconeContext.getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		cursor = BlueconeContext.getContext().getContentResolver().query(Track.CONTENT_URI, new String[] { BaseColumns._ID, Track.TITLE, 
				Track.ALBUM_TITLE, Track.ARTIST_NAME,Track.PATH }, null, null, null);
		startManagingCursor(cursor);
		listView.setAdapter(queueBaseAdapter);
		registerForContextMenu(listView);
		setMaster(false);

	}

	@Override
	public void onStart(){
		super.onStart();
		IntentFilter startQueueIntent = new IntentFilter(START_UPDATE_QUEUE);
		IntentFilter queueIntent = new IntentFilter(UPDATE_QUEUE);
		IntentFilter masterIntent = new IntentFilter(MASTER_MODE);
		IntentFilter removeIntent = new IntentFilter(REMOVE_FIRST_IN_QUEUE);
		IntentFilter progressIntent = new IntentFilter(MainTabActivity.SET_NOW_PLAYING);
		this.registerReceiver(receiver, startQueueIntent);
		this.registerReceiver(receiver, queueIntent);
		this.registerReceiver(receiver, masterIntent);
		this.registerReceiver(receiver, removeIntent);
		this.registerReceiver(receiver, progressIntent);

	}

	@Override
	public void onResume(){
		super.onResume();

	}
	@Override
	public void onPause(){
		super.onPause();

	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {



		@Override
		public void onReceive(Context context, Intent intent) {
			switch(actionMap.get(intent.getAction())){
			case START:
				queuestart_initiated = true;
				max = intent.getIntExtra(MAX, 1);
				if(Debug.D)Log.d(Debug.TAG_QUEUE, "Start "+max);
				DATA.clear();
				break;
			case UPDATE:
				if(Debug.D)Log.d(Debug.TAG_QUEUE, "queuestart_initiated = "+queuestart_initiated);
				selection = Track.PATH+"=? ";
				selectionArgs = new String[]{intent.getStringExtra(Track.PATH)};
				if(Debug.D)Log.d(Debug.TAG_QUEUE, "input: "+selectionArgs[0]);
				
				cursor = BlueconeContext.getContext().getContentResolver().query(Track.CONTENT_URI,new String[] {BaseColumns._ID,Track.TITLE,
						Track.ALBUM_TITLE, Track.ARTIST_NAME,Track.PATH,Track.TRACK_LENGHT}, selection, selectionArgs, null);
				cursor.moveToFirst();

				int pos = Integer.parseInt(intent.getStringExtra(POS));
				if(Debug.D)Log.d(Debug.TAG_QUEUE, "Pos: " + pos);
				try{
					DATA.add(pos, cursor.getString(1));
				}catch(IndexOutOfBoundsException e){
					Log.d(Debug.TAG_QUEUE, "UPDATE Cursor size = " + cursor.getCount());
				}
				cursor.close();
				update();



				break;
			case MASTER:
				Log.d(Debug.TAG_QUEUE, "IS_MASTER= "+intent.getBooleanExtra(IS_MASTER, false));
				setMaster(intent.getBooleanExtra(IS_MASTER, false));
				break;
			case REMOVE:
				if(Debug.D)Log.d(Debug.TAG_QUEUE, "REMOVE");
				currentProgress = 0;
				selection = Track.TITLE+"=?";
				selectionArgs = new String[]{DATA.get(0)};
				Cursor lenght_cursor = BlueconeContext.getContext().getContentResolver().query(Track.CONTENT_URI,
						new String[] {BaseColumns._ID,Track.TRACK_LENGHT},
						selection, selectionArgs, null);
				try{
					lenght_cursor.moveToFirst();
					nowPlaying = DATA.remove(0);
					Intent currentTrackIntent = new Intent(MainTabActivity.SET_NOW_PLAYING);
					currentTrackIntent.putExtra(NOW_PLAYING, nowPlaying);
					currentTrackIntent.putExtra(PROGRESS, lenght_cursor.getInt(1));
					sendBroadcast(currentTrackIntent);
					lenght_cursor.close();
				}catch(IndexOutOfBoundsException e){
					Log.d(Debug.TAG_QUEUE, "Arraylist is empty");
				}			
				break;
			case SET_PROGRESS:
				if(Debug.D)Log.d(Debug.TAG_QUEUE, "SET_PROGRESS");
				currentProgress = intent.getIntExtra(CURRENT_PROGRESS, 0);
				startSeekBar(intent.getIntExtra(PROGRESS, 100));


				break;
			}


		}




		private void startSeekBar(int max) {
			seekbar.setMax(max);
			if(Debug.D)Log.d(Debug.TAG_QUEUE, "Seekbar Max = "+max);
			ProgressThread seekbarProgressThread = new ProgressThread();
			seekbarProgressThread.start();

		}

	};


	private Handler seekBarHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			switch(msg.what){
			case SET_PROGRESS:
				seekbar.setProgress(msg.arg1);
				break;
			}
		}
	};

	private void setMaster(boolean master){
		if (!master) {
			prev.setVisibility(Button.GONE);
			stop.setVisibility(Button.GONE);
			play.setVisibility(Button.GONE);
			next.setVisibility(Button.GONE);
			volume_up.setVisibility(Button.GONE);
			volume_down.setVisibility(Button.GONE);
		} else {
			prev.setVisibility(Button.VISIBLE);
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

			holder.playing.setText((CharSequence) DATA.toArray()[position]);
			if(Debug.D)Log.d(Debug.TAG_QUEUE,"getView: "+DATA.toArray()[position]);


			return convertView;

		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public Object getItem(int position) {
			
			return DATA.get(position);
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return DATA.toArray().length;
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
		  selection = Track.TITLE+"=? ";
			selectionArgs = new String[]{(String) queueBaseAdapter.getItem(info.position)};
		  Cursor pathCursor = BlueconeContext.getContext().getContentResolver().query(Track.CONTENT_URI,
					new String[] {BaseColumns._ID,Track.PATH},
					selection, selectionArgs, null);
		  pathCursor.moveToFirst();
		  Toast.makeText(BlueconeContext.getContext(), pathCursor.getString(1), Toast.LENGTH_LONG).show();
		  pathCursor.close();
	    return true;
	  default:
	    return super.onContextItemSelected(item);
	  }
	}


	public void play(View view){
		Intent intent = new Intent(MainTabActivity.REQUEST_MASTER);
		intent.putExtra(MainTabActivity.MASTER_COMMAND, "PLAY");
		sendBroadcast(intent);
	}
	public void stop(View view){
		Intent intent = new Intent(MainTabActivity.REQUEST_MASTER);
		intent.putExtra(MainTabActivity.MASTER_COMMAND, "STOP");
		sendBroadcast(intent);

	}
	public void next(View view){
		Intent intent = new Intent(MainTabActivity.REQUEST_MASTER);
		intent.putExtra(MainTabActivity.MASTER_COMMAND, "NEXT");
		sendBroadcast(intent);

	}
	public void prev(View view){
		Intent intent = new Intent(MainTabActivity.REQUEST_MASTER);
		intent.putExtra(MainTabActivity.MASTER_COMMAND, "PREV");
		sendBroadcast(intent);

	}
	public void adjustVolumeUp(View view){
		Intent intent = new Intent(MainTabActivity.REQUEST_MASTER);
		intent.putExtra(MainTabActivity.MASTER_COMMAND, "VOLUP");
		sendBroadcast(intent);

	}
	public void adjustVolumeDown(View view){
		Intent intent = new Intent(MainTabActivity.REQUEST_MASTER);
		intent.putExtra(MainTabActivity.MASTER_COMMAND, "VOLDOWN");
		sendBroadcast(intent);

	}

	static{
		actionMap = new HashMap<String, Integer>();
		actionMap.put(START_UPDATE_QUEUE, START);
		actionMap.put(UPDATE_QUEUE, UPDATE);
		actionMap.put(MASTER_MODE, MASTER);
		actionMap.put(MASTER_MODE, MASTER);
		actionMap.put(REMOVE_FIRST_IN_QUEUE, REMOVE);
		actionMap.put(MainTabActivity.SET_NOW_PLAYING, SET_PROGRESS);

	}

	private  class ProgressThread extends Thread{


		@Override
		public void run() {
			while(currentProgress<seekbar.getMax()){
				seekBarHandler.sendMessage(seekBarHandler.obtainMessage(SET_PROGRESS, ++currentProgress, 0));

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					if(Debug.D)Log.d(Debug.TAG_QUEUE, "PROGRESSTHREAD "+ currentProgress);	
				}

			}
		}
	}

}
