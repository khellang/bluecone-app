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

public class QueueActivity extends Activity {


	private static final int START = 0;
	private static final int UPDATE = 1;
	private static final int MASTER = 2;
	private static final int REMOVE = 3;
	private static final int SET_PROGRESS = 4;
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
	private int currentProgress;
	private String selection;
	private String[]selectionArgs;
	private boolean isMaster;





	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.queue_layout);
		isMaster = false;
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
		IntentFilter startQueueIntent = new IntentFilter(Bluecone_intent.START_UPDATE_QUEUE);
		IntentFilter queueIntent = new IntentFilter(Bluecone_intent.UPDATE_QUEUE);
		IntentFilter masterIntent = new IntentFilter(Bluecone_intent.MASTER_MODE);
		IntentFilter removeIntent = new IntentFilter(Bluecone_intent.REMOVE);
		IntentFilter progressIntent = new IntentFilter(Bluecone_intent.SET_NOW_PLAYING);
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
				DATA.clear();
				break;
			case UPDATE:
				selection = Track.PATH+"=? ";
				selectionArgs = new String[]{intent.getStringExtra(Track.PATH)};
				if(Debug.D)Log.d(Debug.TAG_QUEUE, "input: "+selectionArgs[0]);

				cursor = BlueconeContext.getContext().getContentResolver().query(Track.CONTENT_URI,new String[] {BaseColumns._ID,Track.TITLE,
						Track.ALBUM_TITLE, Track.ARTIST_NAME,Track.PATH,Track.TRACK_LENGHT}, selection, selectionArgs, null);
				cursor.moveToFirst();

				int pos = Integer.parseInt(intent.getStringExtra(Bluecone_intent.EXTRA_POS));
				if(Debug.D)Log.d(Debug.TAG_QUEUE, "Pos: " + pos+" track_name = "+cursor.getString(1));
				try{
					DATA.add(pos, cursor.getString(1));
				}catch(IndexOutOfBoundsException e){
					Log.d(Debug.TAG_QUEUE, "UPDATE Cursor size = " + cursor.getCount());
				}
				cursor.close();
				update();



				break;
			case MASTER:
				Log.d(Debug.TAG_QUEUE, "IS_MASTER= "+intent.getBooleanExtra(Bluecone_intent.EXTRA_IS_MASTER, false));
				setMaster(intent.getBooleanExtra(Bluecone_intent.EXTRA_IS_MASTER, false));
				break;
			case REMOVE:
				if(Debug.D)Log.d(Debug.TAG_QUEUE, "REMOVE");
				try{
					DATA.remove(intent.getIntExtra(Bluecone_intent.EXTRA_REMOVE_POS, 0));
				}catch(IndexOutOfBoundsException e){
					Log.d(Debug.TAG_QUEUE, "Remove index: "+intent.getIntExtra(Bluecone_intent.EXTRA_REMOVE_POS, 0));
				}
				update();			
				break;
			case SET_PROGRESS:
				if(Debug.D)Log.d(Debug.TAG_QUEUE, "SET_PROGRESS");
				currentProgress = intent.getIntExtra(Bluecone_intent.EXTRA_CURRENT_PROGRESS, 0);
				startSeekBar(intent.getIntExtra(Bluecone_intent.EXTRA_DURATION, 100));


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
		isMaster = master;
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
			return position;
		}

		@Override
		public Object getItem(int position) {

			return DATA.get(position);
		}

		@Override
		public int getCount() {
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
			if(!isMaster){
				Toast.makeText(BlueconeContext.getContext(), "Master mode required", Toast.LENGTH_SHORT).show();
				return true;
			}
			selection = Track.TITLE+"=? ";
			selectionArgs = new String[]{(String) queueBaseAdapter.getItem(info.position)};
			Cursor pathCursor = BlueconeContext.getContext().getContentResolver().query(Track.CONTENT_URI,
					new String[] {BaseColumns._ID,Track.PATH},
					selection, selectionArgs, null);
			pathCursor.moveToFirst();
			Intent removeIntent = new Intent(Bluecone_intent.REQUEST_WRITE);
			removeIntent.putExtra(Bluecone_intent.EXTRA_COMMAND, "QUEUEREMOVE#");
			removeIntent.putExtra(Bluecone_intent.EXTRA_BLUECONE_WRITE, pathCursor.getString(1) );
			sendBroadcast(removeIntent);
			pathCursor.close();
			return true;
		default:
			return super.onContextItemSelected(item);
		}
	}


	public void play(View view){
		Intent intent = new Intent(Bluecone_intent.REQUEST_MASTER);
		intent.putExtra(Bluecone_intent.EXTRA_MASTER_COMMAND, "PLAY");
		sendBroadcast(intent);
	}
	public void stop(View view){
		Intent intent = new Intent(Bluecone_intent.REQUEST_MASTER);
		intent.putExtra(Bluecone_intent.EXTRA_MASTER_COMMAND, "STOP");
		sendBroadcast(intent);

	}
	public void next(View view){
		Intent intent = new Intent(Bluecone_intent.REQUEST_MASTER);
		intent.putExtra(Bluecone_intent.EXTRA_MASTER_COMMAND, "NEXT");
		sendBroadcast(intent);

	}
	public void prev(View view){
		Intent intent = new Intent(Bluecone_intent.REQUEST_MASTER);
		intent.putExtra(Bluecone_intent.EXTRA_MASTER_COMMAND, "PREV");
		sendBroadcast(intent);

	}
	public void adjustVolumeUp(View view){
		Intent intent = new Intent(Bluecone_intent.REQUEST_MASTER);
		intent.putExtra(Bluecone_intent.EXTRA_MASTER_COMMAND, "VOLUP");
		sendBroadcast(intent);

	}
	public void adjustVolumeDown(View view){
		Intent intent = new Intent(Bluecone_intent.REQUEST_MASTER);
		intent.putExtra(Bluecone_intent.EXTRA_MASTER_COMMAND, "VOLDOWN");
		sendBroadcast(intent);

	}

	static{
		actionMap = new HashMap<String, Integer>();
		actionMap.put(Bluecone_intent.START_UPDATE_QUEUE, START);
		actionMap.put(Bluecone_intent.UPDATE_QUEUE, UPDATE);
		actionMap.put(Bluecone_intent.MASTER_MODE, MASTER);
		actionMap.put(Bluecone_intent.MASTER_MODE, MASTER);
		actionMap.put(Bluecone_intent.REMOVE, REMOVE);
		actionMap.put(Bluecone_intent.SET_NOW_PLAYING, SET_PROGRESS);

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
