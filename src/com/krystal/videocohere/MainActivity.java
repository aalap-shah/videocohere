package com.krystal.videocohere;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.krystal.videocohere.database.Video;
import com.krystal.videocohere.services.ThumbnailLoader;

public class MainActivity extends Activity {
	List<Video> mVideos = null;
	VideoListArrayAdapter mVideoListAdapter = null;
	private static int VIDEO_IMPORT_FROM_GALLERY = 0;
	private static String mPath = null;
	private SharedPreferences mPrefs = null;
	private MediaMetadataRetriever mMediaRetriever = null;
	private int mVideoWidth = 0, mVideoHeight = 0;
	private LinearLayout mLinearLayout;
	private float mDuration = 0;
	private List<Track> tracks = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mPrefs = this.getSharedPreferences("com.krystal.videocohere",
				Context.MODE_PRIVATE);
		mLinearLayout = (LinearLayout) findViewById(R.id.MainClipsLLContainer);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
		switch (item.getItemId()) {
		case R.id.action_import_video:
			intent = new Intent(Intent.ACTION_GET_CONTENT);
			intent.setType("video/*");
			startActivityForResult(intent, VIDEO_IMPORT_FROM_GALLERY);
			return true;
		case R.id.action_settings:
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		mVideos = ((VideoCohereApplication) getApplication()).getDBA()
				.getVideos();

		mVideoListAdapter = new VideoListArrayAdapter(this, mVideos);
		ListView mVideoList = (ListView) findViewById(R.id.MainClipsLV);
		mVideoList.setAdapter(mVideoListAdapter);
	}

	public class VideoListArrayAdapter extends ArrayAdapter<Video> implements
			View.OnClickListener, View.OnLongClickListener {

		private List<Video> mList;
		private LayoutInflater mInflater;
		/* TODO why is this needed? */
		private ViewGroup.MarginLayoutParams params = null;

		public VideoListArrayAdapter(Context context, List<Video> List) {
			super(context, R.layout.main_clips_list_item);

			mList = List;
			mInflater = LayoutInflater.from(context);
			params = new ViewGroup.MarginLayoutParams(
					(int) TypedValue.applyDimension(
							TypedValue.COMPLEX_UNIT_DIP, 90, getResources()
									.getDisplayMetrics()),
					(int) TypedValue.applyDimension(
							TypedValue.COMPLEX_UNIT_DIP, 68, getResources()
									.getDisplayMetrics()));
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Video currentVideo = mList.get(position);

			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.main_clips_list_item,
						null);
			}

			/** NOTE: Setting the background color of tiles */
			if (position % 2 == 0) {
				convertView.setBackgroundResource(R.drawable.alterselector1);
			} else {
				convertView.setBackgroundResource(R.drawable.alterselector2);
			}

			List<String> thumbnailList = Arrays.asList(currentVideo.thumbnails
					.split("\\s*,\\s*"));
			mLinearLayout = (LinearLayout)convertView.findViewById(R.id.MainClipsLLContainer);
			for (String thumbnailPath : thumbnailList) {

				ImageView iv = new ImageView(getContext());
				mLinearLayout.addView(iv, this.params);

				VideoCohereApplication.mTL.loadImage(thumbnailPath, iv);
				// iv.setTag(v);

				iv.setOnClickListener(this);
				iv.setOnLongClickListener(this);
			}

			return convertView;
		}

		@Override
		public boolean onLongClick(View view) {
			return true;
		}

		@Override
		public void onClick(View view) {

		}

		@Override
		public int getCount() {
			if (mList == null)
				return 0;
			return mList.size();
		}

		@Override
		public Video getItem(int position) {
			return mList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == VIDEO_IMPORT_FROM_GALLERY && resultCode == RESULT_OK) {
			Uri selectedVideoLocation = data.getData();
			
			mPath = VideoCohereApplication.getFilePathFromContentUri(
					selectedVideoLocation, getContentResolver());

			try {
				FileInputStream f = new FileInputStream(mPath);
				FileChannel fc = f.getChannel();
				IsoFile isoFile = new IsoFile(fc);
				MovieBox moov = null;
				if (isoFile != null
						&& isoFile.getBoxes(MovieBox.class).size() > 0)
					moov = isoFile.getBoxes(MovieBox.class).get(0);

				if (moov != null && moov.getBoxes(TrackBox.class).size() > 0) {
					TrackBox track = moov.getBoxes(TrackBox.class).get(0);
					TrackHeaderBox thb = track.getTrackHeaderBox();

					if (thb.getWidth() != mPrefs.getInt(
							"com.krystal.videocohere.standardwidth", 0)
							|| thb.getHeight() != mPrefs.getInt(
									"com.krystal.videocohere.standardheight", 0)) {
						mPath = null;
					}
					// Log.i("asd", "movie box details are " + thb.getHeight() +
					// "   getWidth" + thb.getWidth());
				} else {
					mPath = null;
				}
				isoFile.close();
				f.close();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (mPath == null) {
				Toast.makeText(this, "Error importing. See help for details.",
						Toast.LENGTH_LONG).show();
				return;
			}

			mMediaRetriever = new MediaMetadataRetriever();
			try {
				mMediaRetriever.setDataSource(this, selectedVideoLocation);
			} catch (IllegalArgumentException e) {
				try {
					mMediaRetriever.setDataSource(mPath);
				} catch (IllegalArgumentException e1) {
					if (mPath == null) {
						Toast.makeText(this,
								"Error importing. See help for details.",
								Toast.LENGTH_LONG).show();
						return;
					}
				}
			}

			mDuration = Float
					.parseFloat(mMediaRetriever
							.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) / 1000;
			mVideoWidth = Integer
					.parseInt(mMediaRetriever
							.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
			mVideoHeight = Integer
					.parseInt(mMediaRetriever
							.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));

			onPrepared();
		} else {
			mPath = null;
		}
	}

	public void onPrepared() {

		double noOfFrames = 4;
		float secondInterval = 30;

		Video mVideo = new Video(mPath, (int) mDuration);
		VideoCohereApplication.mTL.loadVideoThumbnails(mMediaRetriever, mVideo,
				noOfFrames, secondInterval, new ThumbnailLoader.ThumbnailLoaderCallback() {
					
					@Override
					public void onSuccess(int status) {
						refreshVideoList();
					}
				});

		Movie movie = null;
		try {
			movie = MovieCreator.build(new FileInputStream(mPath).getChannel());
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		tracks = movie.getTracks();

	}
	
	public void refreshVideoList () {
		mVideos = ((VideoCohereApplication) getApplication()).getDBA()
				.getVideos();

		mVideoListAdapter = new VideoListArrayAdapter(this, mVideos);
		ListView mVideoList = (ListView) findViewById(R.id.MainClipsLV);
		mVideoList.setAdapter(mVideoListAdapter);
	}
}
