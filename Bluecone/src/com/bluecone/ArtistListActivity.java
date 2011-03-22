package com.bluecone;


import com.bluecone.intent.Bluecone_intent;
import com.bluecone.storage.ArtistList.Artist;

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



public class ArtistListActivity extends ListActivity{

	private Cursor cursor;
	private LayoutInflater layoutInflater;
	private ArtistBaseAdapter artistBaseAdapter;
	private static String sortOrder;

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.artist_layout);
		sortOrder = Artist.NAME+" ASC";
		artistBaseAdapter = new ArtistBaseAdapter();
		layoutInflater = (LayoutInflater) BlueconeContext.getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		cursor = BlueconeContext.getContext().getContentResolver().query(Artist.CONTENT_URI, new String[] { BaseColumns._ID, Artist.NAME}, null, null, sortOrder);		
		startManagingCursor(cursor);	// Denne er deprecated. CursorLoader kan/skal brukes, men denne virker sannsynligvis ikke på SDK 7. 		 
		setListAdapter(artistBaseAdapter);
		if(Debug.D)Log.d(Debug.TAG_ARTIST, "...onCreate");
	}

	@Override
	public void onStart(){
		super.onStart();
		IntentFilter filter = new IntentFilter(Bluecone_intent.REFRESH);
		this.registerReceiver(receiver, filter);
	}
	@Override 
	public void onResume(){
		super.onResume();
	}
	@Override
	public void onPause(){
		super.onPause();
	}
	@Override
	public void onStop(){
		super.onStop();
	}
	@Override 
	public void onRestart(){
		super.onRestart();
	}
	@Override
	public void onDestroy(){
		super.onDestroy();
	}




	private BroadcastReceiver receiver = new BroadcastReceiver() {


		@Override
		public void onReceive(Context context, Intent intent) {
			update();

		}
	};

	private void update(){
		cursor.close();
		cursor = BlueconeContext.getContext().getContentResolver().query(Artist.CONTENT_URI, new String[] { BaseColumns._ID, Artist.NAME}, null, null,sortOrder);		
		artistBaseAdapter.notifyDataSetChanged();

	}

	private class ArtistBaseAdapter extends BaseAdapter{


		public int getCount() {
			return cursor.getCount();
		}

		public Object getItem(int position) {
			return position;
		}

		public long getItemId(int position) {
			return position;
		}

		public View getView(int position, View convertView, ViewGroup parent) {
			cursor.moveToPosition(position);
			ViewHolder holder;
			if(convertView==null){
				convertView = layoutInflater.inflate(R.layout.artist_entry, null);
				holder = new ViewHolder();
				holder.name = (TextView) convertView.findViewById(R.id.artist_name);
				convertView.setTag(holder);
			}
			else{
				holder = (ViewHolder) convertView.getTag();
			}


			holder.name.setText(cursor.getString(1));

			return convertView;
		}

	}

	private class ViewHolder{
		TextView name;
	}
	
	@Override
	protected void onListItemClick (ListView l, View v, int position, long id){
		Intent intent = new Intent(Bluecone_intent.REFRESH_ALBUM);
		intent.putExtra(Artist.NAME, ((((ViewHolder) v.getTag()).name)).getText());
		if(Debug.D)Log.d(Debug.TAG_ARTIST, "Trykker på "+intent.getStringExtra(Artist.NAME));
		sendBroadcast(intent);
	}


}
