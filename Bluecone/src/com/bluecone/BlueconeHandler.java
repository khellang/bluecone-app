package com.bluecone;
import java.util.ArrayList;
import java.util.HashMap;
import com.bluecone.connect.DeviceConnector;
import com.bluecone.storage.ArtistList.Album;
import com.bluecone.storage.ArtistList.Artist;
import com.bluecone.storage.ArtistList.Track;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.SQLException;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;


public final class BlueconeHandler extends Handler {

	private static final String TAG = "BlueconeHandler";
	private static final boolean D = true;
	private static BlueconeHandler handler = new BlueconeHandler();
	private ContentResolver contentResolver;
	HashMap<String, Integer> map;
	
	public static final int STATE_CHANGED=0;
	//******************************
	public static final int STATE_NONE=0;
	public static final int STATE_CONNECTING=1;
	public static final int STATE_CONNECTED=2;
	//**********************************
	public static final int FINISHED_INSERT=1;
	public static final int INPUT_PREP=2;
	public static final int INPUT=3;	
	//***********************************
	private static final int LISTSTART = 0;
	private static final int LIST = 1;
	private static final int QUEUESTART = 2;
	private static final int QUEUE = 3;
	//*************************************
	public static final int OUTPUT=4;
	public static final int TOAST=5;
	
	private static ArrayList<byte[]> storage;
	private boolean waiting;
	private static int max;
	
	

	
	public static BlueconeHandler getHandler(){
		return handler;
	}
	
	public BlueconeHandler(){
		map = new HashMap<String, Integer>();
		map.put("LISTSTART", LISTSTART);
		map.put("LIST", LIST);
		map.put("QUEUESTART", QUEUESTART);
		map.put("QUEUE", QUEUE);
		contentResolver = BlueconeContext.getContext().getContentResolver();
		storage=new ArrayList<byte[]>();
		waiting = true;
	}
		
		@Override
		public void handleMessage(Message msg){
			switch(msg.what){
			case STATE_CHANGED:
				switch(msg.arg1){
				case STATE_NONE:
					break;
				case STATE_CONNECTING:
					break;
				case STATE_CONNECTED:
					Intent intent = new Intent(MainTabActivity.DEVICE_CONNECTED);
					BlueconeContext.getContext().sendBroadcast(intent);
					break;	
				}
				break;
			case FINISHED_INSERT:
				Log.d(TAG, "Waiting : "+waiting+" -->");
				waiting = true;
				Log.d(TAG, "--> "+waiting);
				break;
			case INPUT_PREP:
	
				break;
				
			case INPUT:
			if(!waiting)	
			storage.add((byte[]) msg.obj);
			else{

				String tmp = new String((byte[]) msg.obj).trim();
				String [] in = tmp.split("#");
				waiting = false;
				switch(map.get(in[0])){
				case LISTSTART:
					if(D)Log.d(TAG, "Liststart");
					max = Integer.parseInt(in[1]);
					Intent progressIntent = new Intent(MainTabActivity.REQUEST_TRANSMITT);
					progressIntent.putExtra(MainTabActivity.PROGRESS, max);
					MainTabActivity.tabHost.setCurrentTab(3);
					MainTabActivity.tabHost.setCurrentTab(2);
					MainTabActivity.tabHost.setCurrentTab(1);
					MainTabActivity.tabHost.setCurrentTab(0);
				BlueconeContext.getContext().sendBroadcast(progressIntent);
					WriterThread musicWriterThread = new WriterThread();
					musicWriterThread.start();
					break;
				case QUEUESTART:
					if(D)Log.d(TAG, "Queuestart");
					max = Integer.parseInt(in[1]);
					Intent startUpdateIntent = new Intent(QueueListActivity.START_UPDATE_QUEUE);
					startUpdateIntent.putExtra(QueueListActivity.MAX, max);
					BlueconeContext.getContext().sendBroadcast(startUpdateIntent);
					 WriterThread queueWriterThread = new WriterThread();
					 queueWriterThread.start();					
					break;
			}
			}
				break;
			case OUTPUT:
				//Brukes ikke foreløpig..... Write track fanges opp i BlueconeTabActivity.class
				break;
			case TOAST:
				Toast.makeText(BlueconeContext.getContext(),msg.getData().getString(DeviceConnector.KEY_TOAST) , Toast.LENGTH_LONG).show();
				break;
		
			}
		}
			
		private class WriterThread extends Thread{
			private int progress;
			private final Intent progressIntent = new Intent(MainTabActivity.START_TRANSMITT); 
			public WriterThread(){
				progress = 0;
			}
			public void	run(){
				Log.d("THREAD","running..."+progress);
			while(progress<max){
				while(!storage.isEmpty()){
					Log.d("THREAD","Storage!empty");
					String tmp = new String((byte[]) storage.get(0)).trim();
					
					Log.d(TAG, "FLAG_INPUT in = "+tmp);
					String [] in = tmp.split("#");
					int lenght = in.length;
					int path =0;
					int artist = 1;
					int album = 2;
					int track = 3;
						switch(map.get(in[0])){
				case LIST:
					storage.remove(0);
					for(int i = 1;i<lenght;i++){
					String[] input = in[i].split("\\|");
					ContentValues artValues = new ContentValues();
					ContentValues albumValues = new ContentValues();
					ContentValues trackValues = new ContentValues();
					artValues.put(Artist.NAME, input[artist]);
					albumValues.put(Album.TITLE, input[album]);
					albumValues.put(Album.ARTIST_NAME, input[artist]);
					trackValues.put(Track.PATH, input[path]);
					trackValues.put(Track.TITLE, input[track]);
					trackValues.put(Track.ALBUM_TITLE, input[album]);
					trackValues.put(Track.ARTIST_NAME, input[artist]);
					try{
						progress = setProgress(++progress)?0:progress;			//Keeps track of progress. When progress >= max; waiting : false-->true
						contentResolver.insert(Track.CONTENT_URI, trackValues);
						BlueconeContext.getContext().sendBroadcast(progressIntent);
						contentResolver.insert(Album.CONTENT_URI, albumValues);
						contentResolver.insert(Artist.CONTENT_URI, artValues);
						
					}catch(SQLException a){
						if(D)Log.d(TAG, "SQLException..."+a);
					}catch(IllegalArgumentException b){
						if(D)Log.d(TAG, "IllegalArgumentException..."+b);
						
					}
					}
					break;
				case QUEUE:
					storage.remove(0);
					for(int i = 1;i<lenght;i++){
						String[] input = in[i].split("\\|");
					progress = setProgress(++progress)?0:progress;			//Keeps track of progress. When progress >= max; waiting : false-->true
					Intent addQueueIntent = new Intent(QueueListActivity.UPDATE_QUEUE);
					addQueueIntent.putExtra(QueueListActivity.PATH, input[0]);
					BlueconeContext.getContext().sendBroadcast(addQueueIntent);
					}
					break;
					default: Log.d(TAG, "Uventet feil");
					break;
				}
			
				}
				
					
			}
		}
		};
		
		public boolean setProgress(int progress){
			if(progress>=max){
				max = 0;
				waiting = true;
				Log.d(TAG, "FINISHED");
				return true;
			}
			return false;
		}

}
		
