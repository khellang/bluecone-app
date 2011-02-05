package com.bluecone;

import com.bluecone.storage.ArtistList.Track;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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
	private LayoutInflater layoutInflater;
		
	
	private static String[] playList; 
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.queue_layout);
	
		layoutInflater = (LayoutInflater) BlueconeContext.getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		
		if(D)Log.d(TAG, "...onCreate");
	}

	@Override
	public void onStart(){
		super.onStart();

	}



	private BroadcastReceiver receiver = new BroadcastReceiver() {


		@Override
		public void onReceive(Context context, Intent intent) {
		
	
			update();
		

		}
	};
	
	private void update(){
	}
	
	private class QueueAdapter extends BaseAdapter{

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Object getItem(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if(convertView==null){
	
	
			}
			else{
	
			}
			return null;
		}
		
	}
	private class ViewHolder{
	
	} 
}
