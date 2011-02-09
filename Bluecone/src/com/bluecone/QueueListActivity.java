package com.bluecone;

import java.util.HashMap;

import com.bluecone.storage.ArtistList.Track;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class QueueListActivity extends Activity {

	
	private static final String TAG = "Queuelist";
	private static final boolean D = true;	
	public static final  String MASTER_MODE = "com.bluecone.MASTER_MODE";
	public static final String START_UPDATE_QUEUE = "com.bluecone.START_UPDATE_QUEUE";
	public static final String UPDATE_QUEUE = "com.bluecone.UPDATE_QUEUE";
	public static final String QUEUE_ELEMENTS="elements";
	public static final String PATH = "path";
	public static final String MAX = "max";
	public static final String IS_MASTER = "is_master";
	private static final int START = 0;
	private static final int UPDATE = 1;
	private static final int MASTER = 2;
	private int max;
	private int start;
	private static final HashMap<String, Integer> actionMap;
	private LayoutInflater layoutInflater;
	private  String[] DATA =new String[]{"Playlist empty"} ;
	private QueueBaseAdapter queueBaseAdapter;
	private Cursor cursor;
	private ListView listView;
	private ImageButton prev;
	private ImageButton stop;
	private ImageButton play;
	private ImageButton next;
	
	

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.queue_layout);

		prev = (ImageButton)findViewById(R.id.imageButton1);
		stop = (ImageButton)findViewById(R.id.imageButton2);
		play = (ImageButton)findViewById(R.id.imageButton3);
		next = (ImageButton)findViewById(R.id.imageButton4);
		listView = (ListView) findViewById(R.id.queue_list);
		queueBaseAdapter = new QueueBaseAdapter();
		layoutInflater = (LayoutInflater) BlueconeContext.getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		cursor = BlueconeContext.getContext().getContentResolver().query(Track.CONTENT_URI, new String[] { BaseColumns._ID, Track.TITLE, Track.ALBUM_TITLE, Track.ARTIST_NAME,Track.PATH }, null, null, null);
		startManagingCursor(cursor);
		listView.setAdapter(queueBaseAdapter);
		setMaster(false);
	
	}

	@Override
	public void onStart(){
		super.onStart();
		IntentFilter startQueueIntent = new IntentFilter(START_UPDATE_QUEUE);
		IntentFilter queueIntent = new IntentFilter(UPDATE_QUEUE);
		IntentFilter masterIntent = new IntentFilter(MASTER_MODE);
		this.registerReceiver(receiver, startQueueIntent);
		this.registerReceiver(receiver, queueIntent);
		this.registerReceiver(receiver, masterIntent);
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
					max = intent.getIntExtra(MAX, 1);
					if(D)Log.d(TAG, "Start "+max);
					DATA = new String[max];
					start = 0;
					break;
				case UPDATE:
					String selection = Track.PATH+"=? ";
					String[]selectionArgs = new String[]{intent.getStringExtra(PATH)};
					if(D)Log.d(TAG, "input: "+selectionArgs[0]);
				
						cursor = BlueconeContext.getContext().getContentResolver().query(Track.CONTENT_URI,new String[] {BaseColumns._ID,Track.TITLE, Track.ALBUM_TITLE, Track.ARTIST_NAME,Track.PATH}, selection, selectionArgs, null);
						update();
						if(start<max){
							cursor.moveToFirst();
							DATA[start++] = cursor.getString(1);
						}		
					break;
				case MASTER:
					Log.d(TAG, "IS_MASTER= "+intent.getBooleanExtra(IS_MASTER, false));
					setMaster(intent.getBooleanExtra(IS_MASTER, false));
					break;
			}


		}

	};
	
	private void setMaster(boolean master){
		prev.setEnabled(master);
		stop.setEnabled(master);
		play.setEnabled(master);
		next.setEnabled(master);
	
		
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
			
			holder.playing.setText(DATA[position]);
			Log.d(TAG,"getView: "+DATA[position]);

		
			return convertView;

		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return DATA.length;
		}
	};
	private class ViewHolder{
		TextView playing;
	
	}
	
	public void play(View view){
		Toast.makeText(this, "Play", Toast.LENGTH_SHORT).show();
	}
	public void stop(View view){
		Toast.makeText(this, "Stop", Toast.LENGTH_SHORT).show();
		
	}
	public void next(View view){
		Toast.makeText(this, "Next", Toast.LENGTH_SHORT).show();
		
	}
	public void prev(View view){
		Toast.makeText(this, "Prev", Toast.LENGTH_SHORT).show();
		
	}
	
	static{
		actionMap = new HashMap<String, Integer>();
		actionMap.put(START_UPDATE_QUEUE, START);
		actionMap.put(UPDATE_QUEUE, UPDATE);
		actionMap.put(MASTER_MODE, MASTER);
	
	}
}
