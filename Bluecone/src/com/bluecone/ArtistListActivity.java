package com.bluecone;


import com.bluecone.storage.ArtistList.Artist;
import android.app.ListActivity;
import android.app.ProgressDialog;
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



public class ArtistListActivity extends ListActivity{

	private static final String TAG = "Artistlist";
	private static final boolean D = true;
	protected static String REFRESH_ALBUM = "com.bluecone.REFRESH_ALBUM";

	private Cursor cursor;
	private LayoutInflater layoutInflater;
	private ArtistBaseAdapter artistBaseAdapter;
	private ProgressDialog dialog;
	private int max;
	protected static String MAX = "max";
	private int progress;
	private static String sortOrder;
	public static final  String PROGRESS_ARTIST = "com.bluecone.PROGRESS_ARTIST";
	
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.artist_layout);
		sortOrder = Artist.NAME+" ASC";
		artistBaseAdapter = new ArtistBaseAdapter();
		layoutInflater = (LayoutInflater) BlueconeContext.getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		cursor = BlueconeContext.getContext().getContentResolver().query(Artist.CONTENT_URI, new String[] { BaseColumns._ID, Artist.NAME}, null, null, sortOrder);		
		startManagingCursor(cursor);	// Denne er deprecated. CursorLoader kan/skal brukes, men denne virker sannsynligvis ikke på SDK 7. 
		 dialog = new ProgressDialog(this);
		 dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		 
		 setListAdapter(artistBaseAdapter);
		if(D)Log.d(TAG, "...onCreate");
	}

	@Override
	public void onStart(){
		super.onStart();
		IntentFilter filter = new IntentFilter(MainTabActivity.REFRESH_FILTER);
		IntentFilter progressFilter = new IntentFilter(PROGRESS_ARTIST );
		this.registerReceiver(receiver, filter);
		this.registerReceiver(receiver, progressFilter);
	}



	private BroadcastReceiver receiver = new BroadcastReceiver() {


		@Override
		public void onReceive(Context context, Intent intent) {
			if(D)Log.d(TAG, "onReceive...");
			if(intent.getAction().equalsIgnoreCase(PROGRESS_ARTIST)){
				if(!dialog.isShowing()){
					max = intent.getIntExtra(MAX , 100);
					dialog.setMax(max);
					progress = 0;
					dialog.show();
				}
				dialog.setProgress(++progress);
				if(progress>max){
					Intent updateAlbum = new Intent(MainTabActivity.REFRESH_FILTER);
					sendBroadcast(updateAlbum);
					dialog.dismiss();

				}
				
			}
			update();
			if(D)Log.d(TAG, "...onReceive");

		}
	};
	
	private void update(){
		cursor = BlueconeContext.getContext().getContentResolver().query(Artist.CONTENT_URI, new String[] { BaseColumns._ID, Artist.NAME}, null, null, null);		
		artistBaseAdapter.notifyDataSetChanged();
		
	}

	private class ArtistBaseAdapter extends BaseAdapter implements OnClickListener{
			

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
				convertView = layoutInflater.inflate(R.layout.artist_entry, null);
				holder = new ViewHolder();
				holder.name = (TextView) convertView.findViewById(R.id.artist_name);
				convertView.setTag(holder);
				convertView.setOnClickListener(ArtistBaseAdapter.this);
			}
			else{
				holder = (ViewHolder) convertView.getTag();
			}


			holder.name.setText(cursor.getString(1));

			return convertView;
		}

		public void onClick(View v) {
			Intent intent = new Intent(REFRESH_ALBUM);
			intent.putExtra(Artist.NAME, ((((ViewHolder) v.getTag()).name)).getText());
			Log.d(TAG, "Trykker på "+intent.getStringExtra(Artist.NAME));
			sendBroadcast(intent);
		
			
			
		}

	}

	private class ViewHolder{
		TextView name;
	}

}
