package com.bluecone;


import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;

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
	public static final String ADD_QUEUE = "com.bluecone.ADD_QUEUE";	
	public static final String QUEUE_ELEMENTS="elements";
	private LayoutInflater layoutInflater;
	private String[] playList=new String[]{"Terje Rocker"};

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.queue_layout);
		layoutInflater = (LayoutInflater) BlueconeContext.getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
		setListAdapter(queueAdapter);
		if(D)Log.d(TAG, "...onCreate");
	}

	@Override
	public void onStart(){
		super.onStart();
		IntentFilter queueIntent = new IntentFilter(UPDATE_QUEUE);
		IntentFilter addIntent = new IntentFilter(ADD_QUEUE);
		this.registerReceiver(receiver, queueIntent);
		this.registerReceiver(receiver, addIntent);
	}



	private BroadcastReceiver receiver = new BroadcastReceiver() {



		@Override
		public void onReceive(Context context, Intent intent) {
			playList = intent.getStringArrayExtra(QUEUE_ELEMENTS);
			update();


		}
	};

	private void update(){
		queueAdapter.notifyDataSetChanged();

	}

	private BaseAdapter queueAdapter = new BaseAdapter() {

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			if(convertView==null){
				convertView =	layoutInflater.inflate(R.layout.queue_entry, null);

			}
			((TextView)convertView.findViewById(R.id.queue_artist_name)).setText(playList[position]);


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
			return playList[position];
		}

		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return playList.length;
		}
	};
}
