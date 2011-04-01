package com.bluecone.storage;

import java.util.HashMap;
import com.bluecone.storage.ArtistList.Album;
import com.bluecone.storage.ArtistList.Artist;
import com.bluecone.storage.ArtistList.Track;
import debug.Debug;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

public class BlueconeContentProvider extends ContentProvider {


	private static final String DATABASE_NAME = "bluecone.db";
	private static final int DATABASE_VERSION = 1;
	private static final String ARTIST_TABLE_NAME = "artist";
	private static final String ALBUM_TABLE_NAME = "album";
	private static final String TRACK_TABLE_NAME = "track";
	private static final UriMatcher sUriMatcher;
	private static final int ARTIST = 1;
	private static final int ALBUM = 2;
	private static final int TRACK = 3;
	private static HashMap<String, String> artistProjectionMap;
	private static HashMap<String, String> albumProjectionMap;
	private static HashMap<String, String> trackProjectionMap;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE IF NOT EXISTS " + ARTIST_TABLE_NAME + " (" +
					BaseColumns._ID + " INTEGER, " + Artist.NAME + " VARCHAR(30) UNIQUE, " +
					"PRIMARY KEY (" + BaseColumns._ID + ", " + Artist.NAME + "))");
			db.execSQL("CREATE TABLE IF NOT EXISTS " + ALBUM_TABLE_NAME + " (" +
					BaseColumns._ID + " INTEGER, " + Album.TITLE + " VARCHAR(30) , " +
					Album.ARTIST_NAME + " VARCHAR(30), PRIMARY KEY (" +
					BaseColumns._ID + ", " + Album.TITLE + "), " +
					"UNIQUE (" + Album.TITLE+", "+Album.ARTIST_NAME +"), " +
					"FOREIGN KEY (" + Album.ARTIST_NAME + ") " +
					"REFERENCES " + ARTIST_TABLE_NAME + "(" + Artist.NAME + "))");
			db.execSQL("CREATE TABLE IF NOT EXISTS " + TRACK_TABLE_NAME + " (" +
					BaseColumns._ID + " INTEGER, " + Track.PATH + " VARCHAR(255) UNIQUE, " +
					Track.TITLE + " VARCHAR(30), " + Track.ALBUM_TITLE + " VARCHAR(30), " +
					Track.ARTIST_NAME + " VARCHAR(30), PRIMARY KEY (" + BaseColumns._ID + ", " + Track.PATH + "), " +
					"FOREIGN KEY (" + Track.ALBUM_TITLE + ") REFERENCES " + ALBUM_TABLE_NAME + "(" + Album.TITLE + "), " +
					"FOREIGN KEY (" + Track.ARTIST_NAME + ") REFERENCES " + ARTIST_TABLE_NAME + "(" + Artist.NAME + "))");
		}

		@Override
		public void onOpen(SQLiteDatabase db) {
			db.execSQL("DROP TABLE IF EXISTS " + ARTIST_TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + ALBUM_TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + TRACK_TABLE_NAME);
			onCreate(db);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(Debug.TAG_PROVIDER, "Upgrading database from version " + oldVersion + " to " + newVersion
					+ ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + ARTIST_TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + ALBUM_TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + TRACK_TABLE_NAME);
			onCreate(db);			
		}

	}

	private static DatabaseHelper dbHelper;

	public static void startTransaction(){
		dbHelper.getWritableDatabase().beginTransaction();
	}
	public static void setTransactionSucsssfull(){
		dbHelper.getWritableDatabase().setTransactionSuccessful();
	}
	public static void endTransaction(){
		dbHelper.getWritableDatabase().endTransaction();
	}

	
	public synchronized static void dropBlueconeDatabase(){
		dbHelper.onOpen(dbHelper.getReadableDatabase());
	}

	public synchronized static void insertThis(ContentValues[] value,int id) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		try{
			db.beginTransaction();
			for(ContentValues c:value){
				switch(id){
				case 0:
					db.insert(ARTIST_TABLE_NAME,Artist.NAME, c);
					break;
				case 1:
					db.insert(ALBUM_TABLE_NAME,Album.TITLE, c);
					break;
				case 2:
					db.insert(TRACK_TABLE_NAME,Track.TITLE, c);
					break;
				}
			}
			BlueconeContentProvider.setTransactionSucsssfull();
		}catch(SQLException a){
		}catch(IllegalArgumentException b){
		}finally{
			BlueconeContentProvider.endTransaction();
		}

	}


	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case ARTIST:
			count = db.delete(ARTIST_TABLE_NAME, selection, selectionArgs);
			break;
		case ALBUM:
			count = db.delete(ALBUM_TABLE_NAME, selection, selectionArgs);
			break;
		case TRACK:
			count = db.delete(TRACK_TABLE_NAME, selection, selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case ARTIST:
			return Artist.CONTENT_TYPE;
		case ALBUM:
			return Album.CONTENT_TYPE;
		case TRACK:
			return Track.CONTENT_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues)throws SQLException {
		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		String table;
		String nullColumnHack;
		Uri contentUri;

		switch (sUriMatcher.match(uri)) {
		case ARTIST:
			table = ARTIST_TABLE_NAME;
			nullColumnHack = Artist.NAME;
			contentUri = Artist.CONTENT_URI;
			break;
		case ALBUM:
			table = ALBUM_TABLE_NAME;
			nullColumnHack = Album.TITLE;
			contentUri = Album.CONTENT_URI;
			break;
		case TRACK:
			table = TRACK_TABLE_NAME;
			nullColumnHack = Track.TITLE;
			contentUri = Track.CONTENT_URI;
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri); 
		}

		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = db.insert(table, nullColumnHack, values);
		if (rowId > 0) {
			Uri testDataUri = ContentUris.withAppendedId(contentUri, rowId);
			getContext().getContentResolver().notifyChange(testDataUri, null);
			return testDataUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public boolean onCreate() {
		dbHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

		switch (sUriMatcher.match(uri)) {
		case ARTIST:
			qb.setTables(ARTIST_TABLE_NAME);
			qb.setProjectionMap(artistProjectionMap);
			break;
		case ALBUM:
			qb.setTables(ALBUM_TABLE_NAME);
			qb.setProjectionMap(albumProjectionMap);
			break;
		case TRACK:
			qb.setTables(TRACK_TABLE_NAME);
			qb.setProjectionMap(trackProjectionMap);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);

		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		int count;
		switch (sUriMatcher.match(uri)) {
		case ARTIST:
			count = db.update(ARTIST_TABLE_NAME, values, selection, selectionArgs);
			break;
		case ALBUM:
			count = db.update(ALBUM_TABLE_NAME, values, selection, selectionArgs);
			break;
		case TRACK:
			count = db.update(TRACK_TABLE_NAME, values, selection, selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(ArtistList.AUTHORITY, ARTIST_TABLE_NAME, ARTIST);
		sUriMatcher.addURI(ArtistList.AUTHORITY, ALBUM_TABLE_NAME, ALBUM);
		sUriMatcher.addURI(ArtistList.AUTHORITY, TRACK_TABLE_NAME, TRACK);

		artistProjectionMap = new HashMap<String, String>();
		artistProjectionMap.put(BaseColumns._ID, BaseColumns._ID);
		artistProjectionMap.put(Artist.NAME, Artist.NAME);

		albumProjectionMap = new HashMap<String, String>();
		albumProjectionMap.put(BaseColumns._ID, BaseColumns._ID);
		albumProjectionMap.put(Album.TITLE, Album.TITLE);
		albumProjectionMap.put(Album.ARTIST_NAME, Album.ARTIST_NAME);

		trackProjectionMap = new HashMap<String, String>();
		trackProjectionMap.put(BaseColumns._ID, BaseColumns._ID);
		trackProjectionMap.put(Track.PATH, Track.PATH);
		trackProjectionMap.put(Track.TITLE, Track.TITLE);
		trackProjectionMap.put(Track.ALBUM_TITLE, Track.ALBUM_TITLE);
		trackProjectionMap.put(Track.ARTIST_NAME, Track.ARTIST_NAME);
	}

}
