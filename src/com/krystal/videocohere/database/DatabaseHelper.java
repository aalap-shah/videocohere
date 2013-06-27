package com.krystal.videocohere.database;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
	private static String DATABASE_NAME = "videocohere.db";
	private static int DATABASE_VERSION = 1;
	private String VIDEOS_TABLE_NAME = "videos";

	private String V_TABLE_KEY = "key";
	private String V_TABLE_PATH = "path";
	private String V_TABLE_DURATION = "duration";
	private String V_TABLE_START_TIME = "start";
	private String V_TABLE_END_TIME = "end";
	private String V_TABLE_THUMBNAILS = "thumbnails";

	private int V_TABLE_KEY_INDEX = 0;
	private int V_TABLE_PATH_INDEX = 1;
	private int V_TABLE_DURATION_INDEX = 2;
	private int V_TABLE_START_TIME_INDEX = 3;
	private int V_TABLE_END_TIME_INDEX = 4;
	private int V_TABLE_THUMBNAILS_INDEX = 5;

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
		// TODO Auto-generated constructor stub
	}

	private String VIDEOS_TABLE_CREATE = "CREATE TABLE " + VIDEOS_TABLE_NAME
			+ " (" + V_TABLE_KEY + " integer primary key autoincrement, "
			+ V_TABLE_PATH + " text, " + V_TABLE_DURATION + " integer, "
			+ V_TABLE_START_TIME + " integer, " + V_TABLE_END_TIME
			+ " integer, " + V_TABLE_THUMBNAILS + " text " + ");";

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.d("Swati", "Video table = " + VIDEOS_TABLE_CREATE);
		db.execSQL(VIDEOS_TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + VIDEOS_TABLE_NAME);
		onCreate(db);
	}

	public List<Video> getVideos() {
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor c = db.query(VIDEOS_TABLE_NAME, null, null, null, null, null,
				null);

		List<Video> videoList = null;

		if (c.moveToFirst()) {
			videoList = new ArrayList<Video>();
			while (!c.isAfterLast()) {
				Video v = new Video(c.getInt(V_TABLE_KEY_INDEX),
						c.getString(V_TABLE_PATH_INDEX),
						c.getLong(V_TABLE_DURATION_INDEX),
						c.getLong(V_TABLE_START_TIME_INDEX),
						c.getLong(V_TABLE_END_TIME_INDEX),
						c.getString(V_TABLE_THUMBNAILS_INDEX));
				videoList.add(v);
				c.moveToNext();
			}
		}
		c.close();
		
        return videoList;
	}
	
	public long addVideo(Video v) {
		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		
		values.put(V_TABLE_PATH, v.path);
		values.put(V_TABLE_DURATION, v.duration);
		values.put(V_TABLE_START_TIME, v.startTime);
		values.put(V_TABLE_END_TIME, v.endTime);
		values.put(V_TABLE_THUMBNAILS, v.thumbnails);

		return db.insert(VIDEOS_TABLE_NAME, null, values);
	}
	
	public int deleteVideo(Video v) {

		SQLiteDatabase db = this.getWritableDatabase();

		String whereClause = V_TABLE_PATH + " = '" + v.path + "'";
		int returnValue = db.delete(VIDEOS_TABLE_NAME, whereClause, null);

		File file = new File(v.path);
		file.delete();

		/* TODO Need to check what this does */
		file = new File(v.path.substring(0, v.path.length() - 3) + "png");
		file.delete();

		return returnValue;
	}
	
	public void updateSelections(int id, long startTime, long endTime) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues values = new ContentValues();
		values.put(V_TABLE_START_TIME, startTime);
		values.put(V_TABLE_END_TIME, endTime);
		
		String whereClause = V_TABLE_KEY + " = " + id;
		db.update(VIDEOS_TABLE_NAME, values, whereClause, null);
	}
}
