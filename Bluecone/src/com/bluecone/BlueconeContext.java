package com.bluecone;

import android.content.Context;

public final class BlueconeContext {
	private static Context blueconeContext;
	
	private static BlueconeContext b = new BlueconeContext();
	
	public static BlueconeContext getBlueconeContext(){
		return b;
	}

	public static void setBlueconeContext(Context blueconeContext) {
		BlueconeContext.blueconeContext = blueconeContext;
	}

	public static Context getContext() {
		return blueconeContext;
	}
}
