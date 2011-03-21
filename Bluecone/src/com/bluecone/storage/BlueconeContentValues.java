package com.bluecone.storage;

public interface BlueconeContentValues {

	public void setTitle(String track);
	public void setArtist(String artist);
	public void setAlbum(String album);
	public void setPath(String path);
	public void setLenght(int lenght);
	public boolean tryCommit();
	public void commitContent();
	
}
