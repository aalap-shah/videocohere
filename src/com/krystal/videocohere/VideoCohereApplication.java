package com.krystal.videocohere;

import java.util.List;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;

import com.krystal.videocohere.database.DatabaseHelper;
import com.krystal.videocohere.services.ThumbnailLoader;

public class VideoCohereApplication extends Application{
	private DatabaseHelper mDBA;
	public static ThumbnailLoader mTL = null;
	private SharedPreferences mPrefs = null;
	private static String mExtFileDirectory = null;
	
	@Override
	public void onCreate() {
		mDBA = new DatabaseHelper(getApplicationContext());
		mTL = ThumbnailLoader.initialize(this, mDBA);
		mPrefs = this.getSharedPreferences("com.krystal.videocohere",
				Context.MODE_PRIVATE);
		setDefaultCameraResolution();
		
		mExtFileDirectory = getApplicationContext().getExternalFilesDir(
				Environment.DIRECTORY_MOVIES).getAbsolutePath();
	}
	
	public DatabaseHelper getDBA() {
		return mDBA;
	}
	
	public static String getFilePathFromContentUri(Uri selectedVideoUri,
			ContentResolver contentResolver) {
		String filePath;
		Cursor cursor = null;
		if (android.os.Build.VERSION.SDK_INT >= 16) {
			String[] filePathColumn = { MediaColumns.DATA, MediaColumns.HEIGHT,
					MediaColumns.WIDTH };
			cursor = contentResolver.query(selectedVideoUri, filePathColumn,
					null, null, null);
		} else {
			String[] filePathColumn = { MediaColumns.DATA };
			cursor = contentResolver.query(selectedVideoUri, filePathColumn,
					null, null, null);
		}

		cursor.moveToFirst();

		filePath = cursor.getString(0);

		if (android.os.Build.VERSION.SDK_INT == 16) {
			int height = cursor.getInt(1);
			int width = cursor.getInt(2);
			cursor.close();
			if (width < height)
				return null;
		}
		return filePath;
	}
	
	public void setDefaultCameraResolution() {

		Camera c = null;
		try {
			c = Camera.open();
		} catch (Exception e) {
			e.printStackTrace();
		}

		Parameters param = c.getParameters();
		List<Camera.Size> list = null;
		int maxHeight = 0, maxWidth = 0;

		list = param.getSupportedVideoSizes();
		if (list != null && list.size() > 0) {
			maxWidth = 0;
			for (Size s : list) {
				if (s.width > maxWidth) {
					maxWidth = s.width;
				}
			}
		}
		list = param.getSupportedPreviewSizes();
		if (list != null && list.size() > 0) {
			for (Size s : list) {
				if (s.width > maxWidth) {
					maxWidth = s.width;
				}
			}
		}

		if (maxWidth == 1920) {
			maxHeight = 1080;
		} else if (maxWidth == 1280) {
			maxHeight = 720;
		} else if (maxWidth == 960) {
			maxHeight = 720;
		} else if (maxWidth == 800) {
			maxHeight = 480;
		} else if (maxWidth == 768) {
			maxHeight = 576;
		} else if (maxWidth == 720) {
			maxHeight = 480;
		} else if (maxWidth == 640) {
			maxHeight = 480;
		} else if (maxWidth == 352) {
			maxHeight = 288;
		} else if (maxWidth == 320) {
			maxHeight = 240;
		} else if (maxWidth == 240) {
			maxHeight = 160;
		} else if (maxWidth == 176) {
			maxHeight = 144;
		} else if (maxWidth == 128) {
			maxHeight = 96;
		}

		mPrefs.edit().putInt("com.krystal.videocohere.standardheight", maxHeight)
				.putInt("com.krystal.videocohere.standardwidth", maxWidth).commit();
	}
	
	public static String getThumbnailPathFromVideoPath(String mPath, int frame) {
		String filename = mPath.substring(mPath.lastIndexOf("/"), mPath.length() - 4) + "_" + frame + ".png";
		Log.d ("Swati", "Filename = " + mExtFileDirectory + filename);
		return mExtFileDirectory + filename;
	}
}
