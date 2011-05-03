package com.bluecone;

import java.util.HashMap;

import com.bluecone.intent.Bluecone_intent;
import com.bluecone.storage.ArtistList.Album;
import com.bluecone.storage.ArtistList.Artist;
import com.bluecone.storage.ArtistList.Track;
import debug.Debug;
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
import android.widget.ListView;
import android.widget.TextView;

public class TrackListActivity extends ListActivity {


	private Cursor cursor;
	private LayoutInflater layoutInflater;
	private TrackBaseAdapter trackBaseAdapter;
	private static final HashMap<String, Integer> actionMap;
	private static final int REFRESH=0;
	private static final int REFRESH_TRACK=1;
	private static final int REFRESH_ARTIST_TRACKS=2;
	private static String selection;
	private static String[] selectionArgs;
	private static String sortOrder;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.track_layout);
		if(Debug.D)Log.d(Debug.TAG_TRACK, "onCreate");

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
		IntentFilter refresh_allIntent = new IntentFilter(Bluecone_intent.REFRESH);
		IntentFilter refresh_albumIntent = new IntentFilter(Bluecone_intent.REFRESH_TRACK);
		IntentFilter refresh_Tracks_artist = new IntentFilter(Bluecone_intent.REFRESH_ALBUM);
		this.registerReceiver(receiver, refresh_allIntent);
		this.registerReceiver(receiver, refresh_albumIntent);
		this.registerReceiver(receiver, refresh_Tracks_artist);
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
				selection = Track.ALBUM_TITLE+"=? AND "+ Track.ARTIST_NAME+"=?";
				selectionArgs = new String[]{intent.getStringExtra(Album.TITLE),intent.getStringExtra(Album.ARTIST_NAME)};
				update();
				MainTabActivity.tabHost.setCurrentTab(2);
				break;
			case REFRESH_ARTIST_TRACKS:
				selection = Track.ARTIST_NAME+" =?";
				selectionArgs = new String[]{intent.getStringExtra(Artist.NAME)};
				update();
			}

		}
	};

	private void update(){
		cursor.close();
		cursor = BlueconeContext.getContext().getContentResolver().query(Track.CONTENT_URI,new String[] { BaseColumns._ID, Track.TITLE, Track.ALBUM_TITLE, Track.ARTIST_NAME,Track.PATH }, selection, selectionArgs, sortOrder);
		trackBaseAdapter.notifyDataSetChanged();

	}

	private class TrackBaseAdapter extends BaseAdapter{


		public int getCount() {
			return cursor.getCount();
		}

		public Object getItem(int position) {
				cursor.moveToPosition(position);
				
			return position ;
		}

		public long getItemId(int position) {
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
	

	}
	
	@Override
	protected void onListItemClick (ListView l, View v, int position, long id){
			Log.d(Debug.TAG_TRACK, "OnListItemClick");
			((ViewHolder)v.getTag()).title.setEnabled(true);
			String path = ((ViewHolder)v.getTag()).path;
//			Intent writeIntent = new Intent(Bluecone_intent.REQUEST_WRITE);
//			writeIntent.putExtra(Bluecone_intent.EXTRA_COMMAND,"ADD#");
//			writeIntent.putExtra(Bluecone_intent.EXTRA_BLUECONE_WRITE, path);
//			sendBroadcast(writeIntent);
			BlueconeHandler.getHandler().sendMessage(BlueconeHandler.getHandler().obtainMessage(BlueconeHandler.WRITE, "ADD#"+path));
	}
	
	

	private class ViewHolder{
		TextView title;
		TextView album;
		TextView artist;
		String path;
	} 
	

	
	static{
		actionMap = new HashMap<String, Integer>();
		actionMap.put(Bluecone_intent.REFRESH, REFRESH);
		actionMap.put(Bluecone_intent.REFRESH_TRACK, REFRESH_TRACK);
		actionMap.put(Bluecone_intent.REFRESH_ALBUM, REFRESH_ARTIST_TRACKS);
		
	}


}
