package com.bluecone.storage;

import android.util.Log;

import com.bluecone.storage.ArtistList.Album;

import debug.Debug;

public class AlbumContent extends Contents {

	public AlbumContent(int lenght,int id) {
		super(lenght,id);
		
	}

	@Override
	public void setTitle(String track) {
		if(Debug.D)Log.d(Debug.TAG_ALBUM_CONTENTS, "setTitle not implemented");
		
	}

	@SuppressWarnings("static-access")
	@Override
	public void setArtist(String artist) {
		super.value[super.index].put(Album.ARTIST_NAME, artist);
		
	}

	@SuppressWarnings("static-access")
	@Override
	public void setAlbum(String album) {
		super.value[super.index].put(Album.TITLE, album);
		
	}

	@Override
	public void setPath(String path) {
		if(Debug.D)Log.d(Debug.TAG_ALBUM_CONTENTS, "setPath not implemented");

		
	}



}
