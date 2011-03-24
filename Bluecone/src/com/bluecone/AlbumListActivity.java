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
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class AlbumListActivity extends ListActivity {

	/**Intent actions*/


	private Cursor cursor;
	private LayoutInflater layoutInflater;
	private AlbumBaseAdapter albumBaseAdapter;
	private static final int REFRESH = 0;
	private static final int SHOW_ALBUM = 1;
	private static final HashMap<String, Integer> actionMap;
	private static String selection;
	private static String[] selectionArgs;
	private static String sortOrder;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(Debug.D)Log.d(Debug.TAG_ALBUM, "onCreate...");
		setContentView(R.layout.album_layout);
		sortOrder = Album.TITLE+" ASC";
		albumBaseAdapter = new AlbumBaseAdapter();
		layoutInflater = (LayoutInflater) BlueconeContext.getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		cursor = BlueconeContext.getContext().getContentResolver().query(Album.CONTENT_URI, new String[] { BaseColumns._ID, Album.TITLE, Album.ARTIST_NAME}, null, null, sortOrder);
		startManagingCursor(cursor);
		setListAdapter(albumBaseAdapter);
		registerForContextMenu(getListView());

	}

	@Override
	public void onStart(){
		super.onStart();
		IntentFilter refresh_all = new IntentFilter(Bluecone_intent.REFRESH);
		IntentFilter refresh_artist = new IntentFilter(Bluecone_intent.REFRESH_ALBUM);
		this.registerReceiver(receiver, refresh_all);
		this.registerReceiver(receiver, refresh_artist);
	}



	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			switch(actionMap.get(intent.getAction())){
			case REFRESH:
				if(Debug.D)Log.d(Debug.TAG_ALBUM, "REFRESH_FILTER");
				selection = null;
				selectionArgs = null;				 	
				update();
				break;
			case SHOW_ALBUM:	
				selection = Album.ARTIST_NAME+"=? ";
				selectionArgs = new String[]{intent.getStringExtra(Artist.NAME)};
				update();
				MainTabActivity.tabHost.setCurrentTab(1);
				break;
			}
		}
	};	

	private void update(){
		cursor.close();
		cursor = BlueconeContext.getContext().getContentResolver().
		query(Album.CONTENT_URI,new String[]{BaseColumns._ID,Album.TITLE,
				Album.ARTIST_NAME}, selection, selectionArgs, sortOrder);
		albumBaseAdapter.notifyDataSetChanged();			

	}


	private class AlbumBaseAdapter extends BaseAdapter{


		public int getCount() {
			return cursor.getCount();
		}

		public Object getItem(int position) {
			return position;
			
		}
		public String getAlbum(int position){
			cursor.moveToPosition(position);
			return cursor.getString(1);
			
		}
		
		public String getArtist(int position){
			cursor.moveToPosition(position);
			return cursor.getString(2);

		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			cursor.moveToPosition(position);
			ViewHolder holder;
			if(convertView==null){
				convertView = layoutInflater.inflate(R.layout.album_entry, null);
				holder = new ViewHolder();
				holder.title = (TextView) convertView.findViewById(R.id.album_title);
				holder.artist = (TextView) convertView.findViewById(R.id.album_artist_name);
				convertView.setTag(holder);
			}
			else{
				holder = (ViewHolder) convertView.getTag();
			}


			holder.title.setText(cursor.getString(1));
			holder.artist.setText(cursor.getString(2));
			return convertView;
		}

	}

	private class ViewHolder{
		TextView title;
		TextView artist;

	} 
	
	@Override
	protected void onListItemClick (ListView l, View v, int position, long id){
		Intent intent = new Intent(Bluecone_intent.REFRESH_TRACK);
		intent.putExtra(Album.TITLE,((((ViewHolder) v.getTag()).title)).getText());
		intent.putExtra(Album.ARTIST_NAME,((((ViewHolder) v.getTag()).artist)).getText());
		sendBroadcast(intent);
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.album_menu, menu);

	}
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		switch (item.getItemId()) {
		case R.id.add_album:
			selection = Track.ALBUM_TITLE+"=? AND "+Track.ARTIST_NAME+"=?";
			selectionArgs = new String[]{albumBaseAdapter.getAlbum(info.position),albumBaseAdapter.getArtist(info.position)};
			Cursor albumCursor = BlueconeContext.getContext().getContentResolver().query(Track.CONTENT_URI, new String[] { BaseColumns._ID,
					Track.PATH}, selection, selectionArgs, sortOrder);
			final int nbr_of_tracks = albumCursor.getCount();
			int i=0;
			final String[] paths = new String[nbr_of_tracks];
			albumCursor.moveToFirst();
			paths[i] = albumCursor.getString(1);
			while(albumCursor.moveToNext()){
				paths[++i] = albumCursor.getString(1);
			}
			albumCursor.close();
			
				new Thread(new Runnable() {
				int i=0;
				@Override
				public void run() {
					while(i<nbr_of_tracks){
						Log.d(Debug.TAG_ALBUM, "path = "+paths[i]);
						Intent writeIntent = new Intent(Bluecone_intent.REQUEST_WRITE);
						writeIntent.putExtra(Bluecone_intent.EXTRA_COMMAND,"ADD#");
						writeIntent.putExtra(Bluecone_intent.EXTRA_BLUECONE_WRITE, paths[i++]);
						sendBroadcast(writeIntent);
						
					}
					
				}
			}).start();
			
			break;
		case R.id.cancel:
			return false;
		default:
			return super.onContextItemSelected(item);
		}
		return true;
	}


	static{
		actionMap = new HashMap<String, Integer>();
		actionMap.put(Bluecone_intent.REFRESH, REFRESH);
		actionMap.put(Bluecone_intent.REFRESH_ALBUM, SHOW_ALBUM);
	}

}
