package com.bluecone;

import android.app.ListActivity;
import android.os.Bundle;

public class QueueListActivity extends ListActivity {

	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.queue_layout);
	}
}
