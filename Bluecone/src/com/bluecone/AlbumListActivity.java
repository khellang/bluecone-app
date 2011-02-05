package com.bluecone;

import com.bluecone.storage.ArtistList.Album;
import com.bluecone.storage.ArtistList.Artist;

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
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class AlbumListActivity extends ListActivity {

	private static final String TAG = "Albumlist";
	private static final boolean D = true;
	public static String REFRESH_TRACK = "com.bluecone.REFRESH_TRACK";

	private Cursor cursor;
	private LayoutInflater layoutInflater;
	private AlbumBaseAdapter albumBaseAdapter;

	private static String selection;
	private static String[] selectionArgs;
	private static String sortOrder;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		if(D)Log.d(TAG, "onCreate...");
		setContentView(R.layout.album_layout);

		sortOrder = Album.TITLE+" ASC";
		albumBaseAdapter = new AlbumBaseAdapter();
		layoutInflater = (LayoutInflater) BlueconeContext.getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		cursor = BlueconeContext.getContext().getContentResolver().query(Album.CONTENT_URI, new String[] { BaseColumns._ID, Album.TITLE, Album.ARTIST_NAME}, null, null, sortOrder);
		startManagingCursor(cursor);
		setListAdapter(albumBaseAdapter);

	}

	@Override
	public void onStart(){
		super.onStart();
		IntentFilter refresh_all = new IntentFilter(MainTabActivity.REFRESH_FILTER);
		IntentFilter refresh_artist = new IntentFilter(ArtistListActivity.REFRESH_ALBUM);
		this.registerReceiver(receiver, refresh_all);
		this.registerReceiver(receiver, refresh_artist);
	}



	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equalsIgnoreCase(MainTabActivity.REFRESH_FILTER)){
				if(D)Log.d(TAG, "REFRESH_FILTER");
				selection = null;
				selectionArgs = null;				 	
				update();
			}
			else if(intent.getAction().equalsIgnoreCase(ArtistListActivity.REFRESH_ALBUM)){
				if(D)Log.d(TAG,"REFRESH_ALBUM");
				selection = Album.ARTIST_NAME+"=? ";
				selectionArgs = new String[]{intent.getStringExtra(Artist.NAME)};
				update();
				MainTabActivity.tabHost.setCurrentTab(1);

			}
		}
	};	

	private void update(){
		cursor = BlueconeContext.getContext().getContentResolver().query(Album.CONTENT_URI,new String[]{BaseColumns._ID,Album.TITLE,Album.ARTIST_NAME}, selection, selectionArgs, sortOrder);
		albumBaseAdapter.notifyDataSetChanged();			
		
	}


	private class AlbumBaseAdapter extends BaseAdapter implements OnClickListener{


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
				convertView = layoutInflater.inflate(R.layout.album_entry, null);
				holder = new ViewHolder();
				holder.title = (TextView) convertView.findViewById(R.id.album_title);
				holder.artist = (TextView) convertView.findViewById(R.id.album_artist_name);
				convertView.setTag(holder);
				convertView.setOnClickListener(AlbumBaseAdapter.this);
			}
			else{
				holder = (ViewHolder) convertView.getTag();
			}


			holder.title.setText(cursor.getString(1));
			holder.artist.setText(cursor.getString(2));
			return convertView;
		}

		public void onClick(View v) {
			Intent intent = new Intent(REFRESH_TRACK);
			intent.putExtra(Album.TITLE,((((ViewHolder) v.getTag()).title)).getText());
			sendBroadcast(intent);
		}

	}

	private class ViewHolder{
		TextView title;
		TextView artist;
	} 
}
