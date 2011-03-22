package com.bluecone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.bluecone.connect.DeviceConnector;
import com.bluecone.intent.Bluecone_intent;
import com.bluecone.storage.AlbumContent;
import com.bluecone.storage.ArtistContent;
import com.bluecone.storage.ArtistList;
import com.bluecone.storage.ArtistList.Track;
import com.bluecone.storage.Contents;
import com.bluecone.storage.TrackContent;
import debug.Debug;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.os.Handler;
import android.os.Message;
import android.provider.BaseColumns;
import android.util.Log;
import android.widget.Toast;


public final class BlueconeHandler extends Handler {


	private static BlueconeHandler handler = new BlueconeHandler();
	private ContentResolver contentResolver;
	private static final HashMap<String, Integer> map;

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
	private static final int MASTER = 4;
	private static final int REMOVE = 5;
	private static final int PLAYING = 6;


	//*************************************
	public static final int OUTPUT=4;
	public static final int TOAST=5;

	private static int max;
	private static Contents[] contents;
	private final	int PATH =0;
	private final	int ARTIST = 1;
	private final	int ALBUM = 2;
	private final	int TRACK = 3;

	private boolean ready_to_release = false;
	private List<Intent> queueBuffer = new ArrayList<Intent>();
	private Intent remove_buffer;
	private String [] selectionArg_buffer;

	private final Intent progressIntent = new Intent(Bluecone_intent.START_TRANSMITT);




	public static BlueconeHandler getHandler(){
		return handler;
	}

	public BlueconeHandler(){

		contentResolver = BlueconeContext.getContext().getContentResolver();
		contents = new Contents[3];
	}

	@Override
	public void handleMessage(Message msg){
		switch(msg.what){
		case STATE_CHANGED:
			switch(msg.arg1){
			case STATE_NONE:
				if(Debug.D)Log.d(Debug.TAG_HANDLER, "STATE_NONE");
				Intent disconnectedIntent = new Intent(Bluecone_intent.CONNECTION_LOST);
				BlueconeContext.getContext().sendBroadcast(disconnectedIntent);
				break;
			case STATE_CONNECTING:
				break;
			case STATE_CONNECTED:
				Intent intent = new Intent(Bluecone_intent.DEVICE_CONNECTED);
				BlueconeContext.getContext().sendBroadcast(intent);
				break;

			}
			break;
		case FINISHED_INSERT:
			Log.d(Debug.TAG_HANDLER, "FINISHED_INSERT");
			for(Intent i:queueBuffer){
				BlueconeContext.getContext().sendBroadcast(i);
			}
			if(remove_buffer!=null)
			BlueconeContext.getContext().sendBroadcast(remove_buffer);
			if(selectionArg_buffer!=null){
				String selection = Track.PATH+"=? ";
				Cursor cur = contentResolver.query(ArtistList.Track.CONTENT_URI, new String[] {BaseColumns._ID,Track.TITLE,Track.TRACK_LENGHT}, selection, selectionArg_buffer, null);
				cur.moveToFirst();
				try{
					String nowPlaying = cur.getString(1);
					Intent currentTrackIntent = new Intent(Bluecone_intent.SET_NOW_PLAYING);
					currentTrackIntent.putExtra(Bluecone_intent.EXTRA_NOW_PLAYING, nowPlaying);
					currentTrackIntent.putExtra(Bluecone_intent.EXTRA_DURATION, cur.getInt(2));
					currentTrackIntent.putExtra(Bluecone_intent.EXTRA_CURRENT_PROGRESS, 50);
					BlueconeContext.getContext().sendBroadcast(currentTrackIntent);
				}catch(CursorIndexOutOfBoundsException e){
					Log.d(Debug.TAG_HANDLER, "PLAYING: Cursor size =  "+cur.getCount());
				}
				
			}
			ready_to_release = true;

			break;
		case INPUT:
			try{
				String tmp = new String((String) msg.obj).trim();
				String [] in = tmp.split("#");

				switch(map.get(in[0])){
				case LISTSTART:
					max = Integer.parseInt(in[1]);
					contents[0] = new ArtistContent(max,0);
					contents[1] = new AlbumContent(max,1);
					contents[2] = new TrackContent(max,2);
					Intent progressIntent = new Intent(Bluecone_intent.REQUEST_TRANSMITT);
					progressIntent.putExtra(Bluecone_intent.EXTRA_PROGRESS_MAX, max);
					BlueconeContext.getContext().sendBroadcast(progressIntent);
					break;
					//				case QUEUESTART:
					//					if(Debug.D)Log.d(Debug.TAG_HANDLER, "Queuestart");
					//					final Intent startUpdateIntent = new Intent(Bluecone_intent.START_UPDATE_QUEUE);
					//					if(!connected){
					//						new Thread(new Runnable() {
					//							
					//							@Override
					//							public void run() {
					//								while(true){
					//								if(ready_to_release){
					//								BlueconeContext.getContext().sendBroadcast(startUpdateIntent);				
					//								}
					//								}
					//							}
					//						}).start();
					//					}
					//					BlueconeContext.getContext().sendBroadcast(startUpdateIntent);				
					//					break;
				case QUEUE:
					Intent addQueueIntent = new Intent(Bluecone_intent.UPDATE_QUEUE);
					String[] temp = in[1].split("\\|");
					Log.d(Debug.TAG_HANDLER, "QUEUE Pos: " + temp[0] + ", Path: " + temp[1]);
					addQueueIntent.putExtra(Track.PATH, temp[1]);
					addQueueIntent.putExtra(Bluecone_intent.EXTRA_POS, temp[0]);
					if(!ready_to_release){
						queueBuffer.add(addQueueIntent);
						Log.d(Debug.TAG_HANDLER, "Added to queuebuffer");

					}
					else 
						BlueconeContext.getContext().sendBroadcast(addQueueIntent);
					break;
				case LIST:
					if(Debug.D)Log.d(Debug.TAG_HANDLER, "List: in[0] =  "+in[0]+"\nin[1]= "+in[1]);
					String[] input = in[1].split("\\|");
					try{
						contents[0].setArtist(input[ARTIST]);
						contents[1].setAlbum(input[ALBUM]);
						contents[1].setArtist(input[ARTIST]);
						contents[2].setPath(input[PATH]);
						contents[2].setTitle(input[TRACK]);
						contents[2].setAlbum(input[ALBUM]);
						contents[2].setArtist(input[ARTIST]);
						contents[2].setLenght(500);
					}catch(NullPointerException e){
						Log.d(Debug.TAG_HANDLER, "NullPointer");
					}
					BlueconeContext.getContext().sendBroadcast(this.progressIntent);
					if(contents[0].tryCommit()){
						final ProgressDialog center = ProgressDialog.show(BlueconeContext.getContext(), null, "Insert to database",true,false);

						new Thread(new Runnable() {

							@Override
							public void run() {
								for(Contents c:contents)
									c.commitContent();
								center.dismiss();
								BlueconeHandler.getHandler().obtainMessage(BlueconeHandler.FINISHED_INSERT).sendToTarget();
							}
						}).start();
					}

					break;
				case MASTER:
					if(Debug.D)Log.d(Debug.TAG_HANDLER, "Master Mode");
					if (in[1].equals("OK")) {
						Intent masterIntent = new Intent(Bluecone_intent.MASTER_MODE);
						masterIntent.putExtra(Bluecone_intent.EXTRA_IS_MASTER, true);
						BlueconeContext.getContext().sendBroadcast(masterIntent);
						Toast.makeText(BlueconeContext.getContext(), "Master Mode Enabled", Toast.LENGTH_LONG).show();
					} else if (in[1].equals("ERR")) {
						Toast.makeText(BlueconeContext.getContext(), "Wrong Password", Toast.LENGTH_LONG).show();
					}
					break;
				case REMOVE:
					if(Debug.D)Log.d(Debug.TAG_HANDLER, "REMOVE");
					Intent removeIntent = new Intent(Bluecone_intent.REMOVE);
					if(!ready_to_release){
						remove_buffer = removeIntent;
						Log.d(Debug.TAG_HANDLER, "Added to removebuffer");
					}else {try{
						removeIntent.putExtra(Bluecone_intent.EXTRA_REMOVE_POS, Integer.parseInt(in[1]));
					}catch(ArrayIndexOutOfBoundsException e){}
					BlueconeContext.getContext().sendBroadcast(removeIntent);
					}
					break;
				case PLAYING:
					String selection = Track.PATH+"=? ";
					String[]selectionArgs = new String[]{in[1]};
					if(!ready_to_release){
						selectionArg_buffer = selectionArgs;
						Log.d(Debug.TAG_HANDLER, "Added to selectionArgbuffer");
					}
					else{
					Cursor cur = contentResolver.query(ArtistList.Track.CONTENT_URI, new String[] {BaseColumns._ID,Track.TITLE,Track.TRACK_LENGHT}, selection, selectionArgs, null);
					cur.moveToFirst();
					try{
						String nowPlaying = cur.getString(1);
						Intent currentTrackIntent = new Intent(Bluecone_intent.SET_NOW_PLAYING);
						currentTrackIntent.putExtra(Bluecone_intent.EXTRA_NOW_PLAYING, nowPlaying);
						currentTrackIntent.putExtra(Bluecone_intent.EXTRA_DURATION, cur.getInt(2));
						currentTrackIntent.putExtra(Bluecone_intent.EXTRA_CURRENT_PROGRESS, 50);
						BlueconeContext.getContext().sendBroadcast(currentTrackIntent);
					}catch(CursorIndexOutOfBoundsException e){
						Log.d(Debug.TAG_HANDLER, "PLAYING: Cursor size =  "+cur.getCount());
					}
					}
					break;
				default: Log.d(Debug.TAG_HANDLER, "Default. Input fra Bluecone: "+msg.obj);
				}
			}catch(NullPointerException e){Log.d(Debug.TAG_HANDLER, "Nullpointer. Input fra Bluecone: "+msg.obj);}
			break;
		case TOAST:
			if(Debug.D)Log.d(Debug.TAG_HANDLER, "Toast");
			String tmp_2 = msg.getData().getString(DeviceConnector.KEY_TOAST) ;
			if(tmp_2!=null)
				Toast.makeText(BlueconeContext.getContext(),tmp_2, Toast.LENGTH_LONG).show();
			break;
		}

	}


	static{
		map = new HashMap<String, Integer>();
		map.put("LISTSTART", LISTSTART);
		map.put("LIST", LIST);
		map.put("QUEUESTART", QUEUESTART);
		map.put("QUEUE", QUEUE);
		map.put("MASTER", MASTER);
		map.put("REMOVE", REMOVE);
		map.put("PLAYING", PLAYING);
	}

}

