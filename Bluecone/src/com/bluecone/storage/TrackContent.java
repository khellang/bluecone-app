package com.bluecone.storage;

import com.bluecone.storage.ArtistList.Track;

public class TrackContent extends Contents {

	public TrackContent(int lenght,int id) {
		super(lenght,id);
	}

	@SuppressWarnings("static-access")
	@Override
	public void setTitle(String track) {
		super.value[super.index].put(Track.TITLE, track);
		
	}

	@SuppressWarnings("static-access")
	@Override
	public void setArtist(String artist) {
		super.value[super.index].put(Track.ARTIST_NAME, artist);
		
	}

	@SuppressWarnings("static-access")
	@Override
	public void setAlbum(String album) {
		super.value[super.index].put(Track.ALBUM_TITLE, album);
		
	}

	@SuppressWarnings("static-access")
	@Override
	public void setPath(String path) {
		super.value[super.index].put(Track.PATH, path);
		
	}

	@SuppressWarnings("static-access")
	@Override
	public void setLenght(int lenght) {
		super.value[super.index].put(Track.TRACK_LENGHT, lenght);
		
	}



}
