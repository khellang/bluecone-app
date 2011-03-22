package com.bluecone.storage;

import com.bluecone.BlueconeContext;
import com.bluecone.BlueconeHandler;
import com.bluecone.intent.Bluecone_intent;

import debug.Debug;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteConstraintException;
import android.util.Log;

public abstract class Contents implements BlueconeContentValues {

	protected ContentValues[] value;
	protected static int index;
	private static int lenght;
	private int _id;
	
	public Contents(int size,int id){
		lenght = size;
		_id = id;
		value = new ContentValues[lenght];
		for(int i = 0;i<lenght;i++){
			value[i] = new ContentValues();
		}
		index = 0;
	}
	
	@Override
	public boolean tryCommit(){
		if(Debug.D)Log.d(Debug.TAG_CONTENTS, "index = "+index);
		return ((++index)>=lenght)?true:false;
	}


	@Override
	public synchronized void commitContent() {
		if(Debug.D)Log.d(Debug.TAG_CONTENTS, "Id = "+_id);
		try{
		BlueconeContentProvider.insertThis(value,_id);
		}catch(SQLiteConstraintException e){}
		Intent update_intent = new Intent(Bluecone_intent.REFRESH);
		BlueconeContext.getContext().sendBroadcast(update_intent);
	

	}

}
