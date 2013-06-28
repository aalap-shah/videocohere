package com.krystal.videocohere.database;


public class Video {

	public int id = -1;
	public String path = null;
	public long duration = -1;
	public long startTime = -1;
	public long endTime = -1;
	public String thumbnails = null;

	/*public class Thumbnail {
		public int id = -1;
		public int fk = -1;
		public String path = null;
		
		public Thumbnail (int i, int f, String p) {
			id = i;
			fk = f;
			path = p;
		}
	}*/
	
	public Video (int i, String p, long d, long s, long e, String t) {
		id = i;
		path = p;
		duration = d;
		startTime = s;
		endTime = e;
		thumbnails = t;
	}
	 
	public Video (String p, long d){
		path = p;
		duration = d;
	}
	
	public Video (String p) {
		path = p;
	}
}

