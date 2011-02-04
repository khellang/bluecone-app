package com.bluecone.storage;

import android.net.Uri;
import android.provider.BaseColumns;

public class ArtistList {

    public static final String AUTHORITY = "com.bluecone.storage.artistList";
	
	public static final class Artist implements BaseColumns {

		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/artist");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.bluecone.artists";
		public static final String NAME = "name";
		
	}
	
	public static final class Album implements BaseColumns {

		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/album");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.bluecone.albums";
		public static final String TITLE = "title";
		public static final String ARTIST_NAME = "artist_name";
		
	}
	
	public static final class Track implements BaseColumns {

		public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/track");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.bluecone.tracks";
		public static final String PATH = "path";
		public static final String TITLE = "title";
		public static final String ALBUM_TITLE = "album_title";
		public static final String ARTIST_NAME = "artist_name";
		
	}

}
