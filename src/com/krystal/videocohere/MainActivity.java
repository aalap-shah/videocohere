package com.krystal.videocohere;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.krystal.videocohere.ThumbOnlySeekBar.SeekBarOnTouchCallback;
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
	private ProgressDialog mThumbnailPB;

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
		private SeekBar mStartSeekBar;
		private SeekBar mEndSeekBar;
		private SeekBar mSelectedSeekBar;
		protected boolean mActionModeShown = false;

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

		private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.main_activity_contextual_menu, menu);
				return true;
			}

			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				mActionModeShown = true;
				return false;
			}

			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {

				switch (item.getItemId()) {
				case R.id.left_button:
					
					if (mSelectedSeekBar != null) {
						Log.d("Swati", "Moving left " + mSelectedSeekBar.getProgress());
						mSelectedSeekBar.setProgress(mSelectedSeekBar
								.getProgress() - 1);
						Log.d("Swati", "Moving left " + mSelectedSeekBar.getProgress());
					}
					return true;
				case R.id.right_button:
					
					if (mSelectedSeekBar != null) {
						Log.d ("Swati", "Moving right " + mSelectedSeekBar.getProgress());
						mSelectedSeekBar.setProgress(mSelectedSeekBar
								.getProgress() + 1);
						Log.d ("Swati", "Moving right " + mSelectedSeekBar.getProgress());
					}
					return true;
				default:
					return false;
				}
			}

			public void onDestroyActionMode(ActionMode mode) {
				mSelectedSeekBar = null;
				mActionModeShown = false;
			}
		};

		private SeekBarOnTouchCallback mOnSeekBarTouchCallback = new SeekBarOnTouchCallback() {
			@Override
			public void onTouch(SeekBar mSeekBar) {
			mSelectedSeekBar = mSeekBar;
			if (!mActionModeShown)
			    startActionMode(mActionModeCallback);
			}
	    };
	    
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			Video currentVideo = mList.get(position);

			Log.d("Swati", "Entered getView");
			if (convertView == null) {
				convertView = mInflater.inflate(R.layout.main_clips_list_item,
						null);
			} else {
				TextView tv = (TextView) convertView
						.findViewById(R.id.MainClipsTV);
				String clipText = tv.getText().toString();
				String clipNo = null;

				if (clipText != null)
					clipNo = clipText
							.substring((clipText.lastIndexOf(" ") + 1));

				if ((clipNo != null)
						&& (position + 1) != Integer.parseInt(clipNo)) {
					Log.d("Swati", "Clearing");
					((LinearLayout) convertView
							.findViewById(R.id.MainClipsLLContainer))
							.removeAllViews();
				} else {
					return convertView;
				}
			}

			/** NOTE: Setting the background color of tiles */
			if (position % 2 == 0) {
				convertView.setBackgroundResource(R.drawable.alterselector1);
			} else {
				convertView.setBackgroundResource(R.drawable.alterselector2);
			}

			((TextView) convertView.findViewById(R.id.MainClipsTV))
					.setText("Clip " + (position + 1));

			List<String> thumbnailList = Arrays.asList(currentVideo.thumbnails
					.split("\\s*,\\s*"));
			mLinearLayout = (LinearLayout) convertView
					.findViewById(R.id.MainClipsLLContainer);
			int count = 0;
			for (String thumbnailPath : thumbnailList) {

				count++;
				Log.d("Swati", "Adding path = " + thumbnailPath);
				ImageView iv = new ImageView(getContext());
				mLinearLayout.addView(iv, this.params);

				VideoCohereApplication.mTL.loadImage(thumbnailPath, iv);
				// iv.setTag(v);

				iv.setOnClickListener(this);
				iv.setOnLongClickListener(this);
			}

			long duration = currentVideo.duration;
			mStartSeekBar = (SeekBar) convertView
					.findViewById(R.id.MainClipsStartSB);
			mEndSeekBar = (SeekBar) convertView
					.findViewById(R.id.MainClipsEndSB);

			int progress = (int) (duration * 60 / count);
			if (progress < 10)
				progress = (int) (duration * 60 * 10);
			else
				progress = (int) (duration * 60);

			Log.d("Swati", "Progress = " + progress);
			mStartSeekBar.setMax((int) (progress));
			mEndSeekBar.setMax((int) (progress));

			mStartSeekBar.setProgress(0);
			mEndSeekBar.setProgress((int) progress);

			((ThumbOnlySeekBar) mStartSeekBar).setOnTouchCallback(mOnSeekBarTouchCallback);
			((ThumbOnlySeekBar)mEndSeekBar).setOnTouchCallback(mOnSeekBarTouchCallback);

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
							|| thb.getHeight() != mPrefs
									.getInt("com.krystal.videocohere.standardheight",
											0)) {
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

		double noOfFrames;
		float secondInterval;

		Log.d("Swati", "Duration = " + mDuration);
		noOfFrames = (Math.log(mDuration) / Math.log(2)) + 1;
		if (noOfFrames > 4)
			noOfFrames = 4;

		secondInterval = (float) (mDuration / noOfFrames);
		Log.d("Swati", "noofframes = " + noOfFrames + " secondinterval = "
				+ secondInterval);
		Video mVideo = new Video(mPath, (int) mDuration);
		VideoCohereApplication.mTL.loadVideoThumbnails(mMediaRetriever, mVideo,
				noOfFrames, secondInterval,
				new ThumbnailLoader.ThumbnailLoaderCallback() {

					@Override
					public void onSuccess(int status) {
						refreshVideoList();
					}
				});

		mThumbnailPB = new ProgressDialog(this, AlertDialog.THEME_HOLO_DARK);
		mThumbnailPB.setCancelable(false);
		mThumbnailPB.setMessage("Loading video");
		mThumbnailPB.show();

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

	public void refreshVideoList() {
		mVideos = ((VideoCohereApplication) getApplication()).getDBA()
				.getVideos();

		mVideoListAdapter = new VideoListArrayAdapter(this, mVideos);
		ListView mVideoList = (ListView) findViewById(R.id.MainClipsLV);
		mVideoList.setAdapter(mVideoListAdapter);
		mThumbnailPB.dismiss();
		mThumbnailPB = null;
	}
}
