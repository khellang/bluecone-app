package com.bluecone;

import java.util.HashMap;

import com.bluecone.storage.ArtistList.Album;
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
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class TrackListActivity extends ListActivity {

	private static final String TAG = "Tracklist";
	private static final boolean D = true;


	private Cursor cursor;
	private LayoutInflater layoutInflater;
	private TrackBaseAdapter trackBaseAdapter;
	private static final HashMap<String, Integer> actionMap;
	private static final int REFRESH=0;
	private static final int REFRESH_TRACK=1;
	private static String selection;
	private static String[] selectionArgs;
	private static String sortOrder;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.track_layout);
		if(D)Log.d(TAG, "onCreate");

		sortOrder = Track.TITLE+" ASC";
		trackBaseAdapter = new TrackBaseAdapter();
		layoutInflater = (LayoutInflater) BlueconeContext.getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		cursor = BlueconeContext.getContext().getContentResolver().query(Track.CONTENT_URI, new String[] { BaseColumns._ID, Track.TITLE, Track.ALBUM_TITLE, Track.ARTIST_NAME,Track.PATH }, null, null, null);
		startManagingCursor(cursor);
		setListAdapter(trackBaseAdapter);

	}

	@Override
	public void onStart(){
		super.onStart();
		IntentFilter refresh_allIntent = new IntentFilter(MainTabActivity.REFRESH);
		IntentFilter refresh_albumIntent = new IntentFilter(AlbumListActivity.REFRESH_TRACK);
		this.registerReceiver(receiver, refresh_allIntent);
		this.registerReceiver(receiver, refresh_albumIntent);
	}


	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			switch(actionMap.get(intent.getAction())){
			case REFRESH:
				selection = null;
			selectionArgs = null;	
			update();
			break;
			case REFRESH_TRACK:
				selection = Track.ALBUM_TITLE+"=? ";
				selectionArgs = new String[]{intent.getStringExtra(Album.TITLE)};
				update();
				MainTabActivity.tabHost.setCurrentTab(2);
				break;
			}

		}
	};

	private void update(){
		cursor = BlueconeContext.getContext().getContentResolver().query(Track.CONTENT_URI,new String[] { BaseColumns._ID, Track.TITLE, Track.ALBUM_TITLE, Track.ARTIST_NAME,Track.PATH }, selection, selectionArgs, sortOrder);
		trackBaseAdapter.notifyDataSetChanged();

	}

	private class TrackBaseAdapter extends BaseAdapter implements OnClickListener, OnLongClickListener{


		public int getCount() {
			// TODO Auto-generated method stub
			return cursor.getCount();
		}

		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

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
				convertView.setOnClickListener(TrackBaseAdapter.this);
				convertView.setOnLongClickListener(TrackBaseAdapter.this);
				
			}
			else{
				holder = (ViewHolder) convertView.getTag();
			}

			
			holder.title.setText(cursor.getString(1));
			holder.title.setEnabled(false);
			holder.album.setText(cursor.getString(2));
			holder.artist.setText(cursor.getString(3));
			holder.path = (cursor.getString(4));
			return convertView;
		}

		public void onClick(View v) {
			Log.d(TAG, " KLIKK");
		
			((ViewHolder)v.getTag()).title.setEnabled(true);
			String path = ((ViewHolder)v.getTag()).path;
			Intent writeIntent = new Intent(MainTabActivity.REQUEST_WRITE);
			writeIntent.putExtra(MainTabActivity.TRACK_WRITE, path);
			sendBroadcast(writeIntent);


		}

		@Override
		public boolean onLongClick(View v) {
			Toast.makeText(BlueconeContext.getContext(), "Long click not implemented", Toast.LENGTH_LONG).show();
			return false;
		}

	}

	private class ViewHolder{
		TextView title;
		TextView album;
		TextView artist;
		String path;
	} 
	
	static{
		actionMap = new HashMap<String, Integer>();
		actionMap.put(MainTabActivity.REFRESH, REFRESH);
		actionMap.put(AlbumListActivity.REFRESH_TRACK, REFRESH_TRACK);
		
	}


}
