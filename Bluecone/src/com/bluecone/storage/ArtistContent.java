package com.bluecone.storage;

import android.util.Log;

import com.bluecone.storage.ArtistList.Artist;

import debug.Debug;

public class ArtistContent extends Contents {

	public ArtistContent(int lenght,int id) {
		super(lenght,id);
	}

	@Override
	public void setTitle(String track) {
		if(Debug.D)Log.d(Debug.TAG_ARTIST_CONTENTS, "setTitle not implemented");
		
	}

	@SuppressWarnings("static-access")
	@Override
	public void setArtist(String artist) {
		super.value[super.index].put(Artist.NAME, artist);
		
	}

	@Override
	public void setAlbum(String album) {
		if(Debug.D)Log.d(Debug.TAG_ARTIST_CONTENTS, "setAlbum not implemented");
		
	}

	@Override
	public void setPath(String path) {
		if(Debug.D)Log.d(Debug.TAG_ARTIST_CONTENTS, "setPath not implemented");
		
	}


}
