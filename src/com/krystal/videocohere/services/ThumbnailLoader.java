package com.krystal.videocohere.services;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import com.krystal.videocohere.VideoCohereApplication;
import com.krystal.videocohere.database.DatabaseHelper;
import com.krystal.videocohere.database.Video;

public class ThumbnailLoader {
	private class imageObject {
		String imageUrl;
		Bitmap imageBitmap;
		ConcurrentLinkedQueue<ImageView> imageViews;
	}

	static ThumbnailLoader TLRef;
	static LinkedHashMap<String, imageObject> imageCache;
	static final Object mTaskLock = new Object();
	static int mTaskCounter = 0;
	static ConcurrentLinkedQueue<Object> fastQueue;
	private MediaMetadataRetriever mMediaRetriever = null;
	private static String mThumbnailsFinalPath = null;
	private int mCount = 0;
	private Video mVideo;
	private ThumbnailLoaderCallback mCallback;
	
	static DatabaseHelper mDBA = null;

	private ThumbnailLoader() {
	}

	public static ThumbnailLoader initialize(Context context, DatabaseHelper DBA) {
		if (TLRef == null) {
			TLRef = new ThumbnailLoader();
			mDBA = DBA;
			imageCache = new LinkedHashMap<String, imageObject>(200, 0.75f,
					true);
			fastQueue = new ConcurrentLinkedQueue<Object>();
			mTaskCounter = 0;
		}
		return TLRef;
	}

	public void loadImage(String imageUrl, ImageView imageView) {

		if ((imageUrl == null) || (imageUrl.length() == 0)) {
			return;
		}

		if (imageCache.containsKey(imageUrl) == true) {
			imageObject iO = imageCache.get(imageUrl);
			if (iO != null && iO.imageBitmap != null) {

				if (imageView != null) {
					imageView.setImageBitmap(iO.imageBitmap);
				}
				ImageView iv;
				while ((iv = (ImageView) iO.imageViews.poll()) != null) {
					iv.setImageBitmap(iO.imageBitmap);
				}
				return;
			} else if (iO != null) {
				boolean existsFlag = false;
				for (Iterator<ImageView> it = iO.imageViews.iterator(); it
						.hasNext();) {
					ImageView iv = (ImageView) it.next();
					if (iv == imageView) {
						existsFlag = true;
						break;
					}
				}
				if (existsFlag == false && imageView != null) {
					iO.imageViews.add(imageView);
				}
				return;
			}
		}

		File f = new File(imageUrl);

		if (f.exists()) {

			imageObject iO1 = new imageObject();
			iO1 = new imageObject();
			iO1.imageUrl = imageUrl;

			iO1.imageViews = new ConcurrentLinkedQueue<ImageView>();
			iO1.imageBitmap = BitmapFactory.decodeFile(imageUrl);

			if (imageView != null) {
				imageView.setImageBitmap(iO1.imageBitmap);
			}

			synchronized (getClass()) {
				imageCache.put(imageUrl, iO1);
			}
			return;
		}
	}

	
	public static interface ThumbnailLoaderCallback {
		public void onSuccess (int status);
	}
	
	public void loadVideoThumbnails(MediaMetadataRetriever mediaRetriever, Video video, double noOfFrames, float secondInterval, ThumbnailLoaderCallback callback) {
		int i;
		mMediaRetriever = mediaRetriever;
		mCount = (int) noOfFrames;
		mVideo = video;
		mCallback = callback;
		for (i = 0; i < noOfFrames; i++) {
			String thumbnailPath = VideoCohereApplication.getThumbnailPathFromVideoPath(video.path, i);
			new getFrameTask().executeOnExecutor(
					AsyncTask.THREAD_POOL_EXECUTOR,
					new FrameIVStruct (thumbnailPath, Math.round(i * secondInterval * 1000000)));
		}
	}

	public class FrameIVStruct {
		String thumbnailPath = null;
		int frameAt;
		Bitmap b = null;

		FrameIVStruct(String path, int i) {
			this.thumbnailPath = path;
			this.frameAt = i;
		}
	}
	
	public class getFrameTask extends AsyncTask<FrameIVStruct, Void, String> {

		@Override
		protected String doInBackground(FrameIVStruct... arg0) {
			FrameIVStruct frameIv = arg0[0];
			Bitmap b = Bitmap.createScaledBitmap(
					mMediaRetriever.getFrameAtTime(frameIv.frameAt), 160, 120, false);
			
			if (b != null) {
				try {
					FileOutputStream out = new FileOutputStream(frameIv.thumbnailPath);
					b.compress(Bitmap.CompressFormat.PNG, 90, out);
					imageObject iO1 = new imageObject();
					iO1.imageUrl = frameIv.thumbnailPath;

					iO1.imageViews = new ConcurrentLinkedQueue<ImageView>();
					iO1.imageBitmap = b;

					synchronized (getClass()) {
						imageCache.put(frameIv.thumbnailPath, iO1);
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
				return frameIv.thumbnailPath;
			}
			return null;
		}

		@Override
		protected void onPostExecute(String result) {
			mCount--;
	
			if (result != null) {
				if (mThumbnailsFinalPath == null) 
					mThumbnailsFinalPath = result;
				else 
					mThumbnailsFinalPath = mThumbnailsFinalPath.concat("," + result);
			}
			
			if ((mCount == 0) && (mVideo != null)) {
				mVideo.thumbnails = mThumbnailsFinalPath;
				Log.d ("Swati", "Final thumbnails = " + mThumbnailsFinalPath);
				mDBA.addVideo(mVideo);
				mCallback.onSuccess(0);
				mVideo = null;
				mThumbnailsFinalPath = null;
			}
				
			super.onPostExecute(result);
		}
	}
}
