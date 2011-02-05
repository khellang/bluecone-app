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
					break;	
				}
				break;
			case FINISHED_INSERT:
			/*	progress++;
				dialog.setProgress(progress);
				if(progress>=max)
					dialog.dismiss();*/
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
					//Hent ut alle sanger fra in[1]
					//Start en progressbar med MAX = antall sanger
					max = Integer.parseInt(in[1]);
					Log.d(TAG, "max= "+max);
					Intent progressIntent = new Intent(ArtistListActivity.PROGRESS_ARTIST);
					progressIntent.putExtra(ArtistListActivity.MAX, max);
					MainTabActivity.tabHost.setCurrentTab(3);
					MainTabActivity.tabHost.setCurrentTab(2);
					MainTabActivity.tabHost.setCurrentTab(1);
					MainTabActivity.tabHost.setCurrentTab(0);
					BlueconeContext.getContext().sendBroadcast(progressIntent);
					writerThread.start();
					break;
				case QUEUESTART:
					if(D)Log.d(TAG, "Queuestart");
					break;
			}
			}
				/*Intent refreshIntent = new Intent(BlueconeTabActivity.REFRESH_FILTER);
				BlueconeContext.getContext().sendBroadcast(refreshIntent);
				Log.d(TAG, "INPUT.. ");
				String tmp = new String((byte[]) msg.obj).trim();
				
				Log.d(TAG, "FLAG_INPUT in = "+tmp);
				String [] in = tmp.split("#");
					switch(map.get(in[0])){
					case LISTSTART:
						if(D)Log.d(TAG, "Liststart");
						//Hent ut alle sanger fra in[1]
						//Start en progressbar med MAX = antall sanger
						break;
					case LIST:
						if(D)Log.d(TAG, "List");
						String[] input = in[1].split("\\|");
						ContentValues artValues = new ContentValues();
						ContentValues albumValues = new ContentValues();
						ContentValues trackValues = new ContentValues();
						artValues.put(Artist.NAME, input[1]);
						albumValues.put(Album.TITLE, input[2]);
						albumValues.put(Album.ARTIST_NAME, input[1]);
						trackValues.put(Track.PATH, input[0]);
						trackValues.put(Track.TITLE, input[3]);
						trackValues.put(Track.ALBUM_TITLE, input[2]);
						trackValues.put(Track.ARTIST_NAME, input[1]);
						try{
							contentResolver.insert(Track.CONTENT_URI, trackValues);
							contentResolver.insert(Album.CONTENT_URI, albumValues);
							contentResolver.insert(Artist.CONTENT_URI, artValues);
							
						}catch(SQLException a){
							if(D)Log.d(TAG, "SQLException..."+a);
						}catch(IllegalArgumentException b){
							if(D)Log.d(TAG, "IllegalArgumentException..."+b);
							
						}
					
						break;
					case QUEUESTART:
						break;
					case QUEUE:
						break;
						default: Log.d(TAG, "Uventet feil");
						break;
					}*/
	
			
				break;
			case OUTPUT:
				//Brukes ikke forel�pig..... Write track fanges opp i BlueconeTabActivity.class
				break;
			case TOAST:
				Toast.makeText(BlueconeContext.getContext(),msg.getData().getString(DeviceConnector.KEY_TOAST) , Toast.LENGTH_LONG).show();
				break;
		
			}
		}
			
		private Thread writerThread = new Thread(){
		private int progress = 0;
			public void	run(){
			while(progress<max){
				while(!storage.isEmpty()){
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
					if(D)Log.d(TAG, "List");
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
						contentResolver.insert(Track.CONTENT_URI, trackValues);
						Intent progressIntent = new Intent(ArtistListActivity.PROGRESS_ARTIST);
						progressIntent.putExtra(Artist.NAME, input[artist]);
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
					break;
					default: Log.d(TAG, "Uventet feil");
					break;
				}
					setProgress(++progress);	
				}
				
					
			}
		}
		};
		
		public void setProgress(int progress){
			if(progress>=max)Log.d(TAG, "FINISHED");
				
		}

}
		