package com.bluecone;

import com.bluecone.storage.ArtistList.Track;
import android.app.ListActivity;
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
import android.widget.TextView;

public class QueueListActivity extends ListActivity {

	private static final String TAG = "Queuelist";
	private static final boolean D = true;

	public static final String UPDATE_QUEUE = "com.bluecone.UPDATE_QUEUE";
	public static final String QUEUE_ELEMENTS="elements";
	public static final String PATH = "path";
	private LayoutInflater layoutInflater;
	private String[] playList=new String[]{"Terje Rocker"};
	
	private QueueBaseAdapter queueBaseAdapter;
	private Cursor cursor;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.queue_layout);
		queueBaseAdapter = new QueueBaseAdapter();
		layoutInflater = (LayoutInflater) BlueconeContext.getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		cursor = BlueconeContext.getContext().getContentResolver().query(Track.CONTENT_URI, new String[] { BaseColumns._ID, Track.TITLE, Track.ALBUM_TITLE, Track.ARTIST_NAME,Track.PATH }, null, null, null);
		startManagingCursor(cursor);
		setListAdapter(queueBaseAdapter);
	}

	@Override
	public void onStart(){
		super.onStart();
		IntentFilter queueIntent = new IntentFilter(UPDATE_QUEUE);
		this.registerReceiver(receiver, queueIntent);
	}



	private BroadcastReceiver receiver = new BroadcastReceiver() {



		@Override
		public void onReceive(Context context, Intent intent) {
			if(D)Log.d(TAG, "onReceive");
			String selection = Track.PATH+"=? ";
			String[]selectionArgs = new String[]{intent.getStringExtra(PATH)};
			if(D)Log.d(TAG, "input: "+selectionArgs[0]);
			synchronized (QueueListActivity.this) {
				
				cursor = BlueconeContext.getContext().getContentResolver().query(Track.CONTENT_URI,new String[] {BaseColumns._ID,Track.TITLE, Track.ALBUM_TITLE, Track.ARTIST_NAME,Track.PATH}, selection, selectionArgs, null);
				if(D)Log.d(TAG, "Antall kolonner: "+cursor.getColumnCount());
		
			}
		//	playList = intent.getStringArrayExtra(QUEUE_ELEMENTS);
			update();


		}
	};

	private void update(){
		queueBaseAdapter.notifyDataSetChanged();

	}

	private class QueueBaseAdapter extends BaseAdapter{

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			cursor.moveToPosition(position);
			ViewHolder holder;
			if(convertView==null){
				convertView = layoutInflater.inflate(R.layout.track_entry, null);
				holder = new ViewHolder();
				holder.title = (TextView) convertView.findViewById(R.id.track_title);
				holder.album = (TextView) convertView.findViewById(R.id.track_album_title);
				holder.artist = (TextView) convertView.findViewById(R.id.track_artist_name);
				convertView.setTag(holder);
			}
			else{
				holder = (ViewHolder) convertView.getTag();
			}


			holder.title.setText(cursor.getString(1));
			holder.album.setText(cursor.getString(2));
			holder.artist.setText(cursor.getString(3));
			holder.path = (cursor.getString(4));
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
			return cursor.getCount();
		}
	};
	private class ViewHolder{
		TextView title;
		TextView album;
		TextView artist;
		String path;
	} 
}
