package com.bluecone;

import android.content.Context;

public final class BlueconeContext {
	private static Context blueconeContext;
	

	public static void setBlueconeContext(Context blueconeContext) {
		BlueconeContext.blueconeContext = blueconeContext;
	}

	public static Context getContext() {
		return blueconeContext;
	}
}
