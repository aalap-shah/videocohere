package com.krystal.videocohere;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.MovieBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;
import com.krystal.videocohere.ThumbOnlySeekBar.SeekBarOnTouchCallback;
import com.krystal.videocohere.database.Video;
import com.krystal.videocohere.services.ThumbnailLoader;
import com.krystal.videocohere.services.TranscodingService;

public class MainActivity extends Activity {
	List<Video> mVideos = null;
	VideoListArrayAdapter mVideoListAdapter = null;
	private static int VIDEO_IMPORT_FROM_GALLERY = 0;
	private static String mPath = null;
	private SharedPreferences mPrefs = null;
	private MediaMetadataRetriever mMediaRetriever = null;
	private LinearLayout mLinearLayout;
	private float mDuration = 0;
	private ProgressDialog mThumbnailPB = null;
	private VideoView mVideoView = null;
	private Video mOutputVideo = null;
	private ImageView mMainVideoIV = null;
	private ImageView mMainVideoFullScreenIV = null;
	private ProgressBar mMainVideoPB;
	private int mCurrentVideo = -1;
	private TranscodingServiceBroadcastReceiver mDataBroadcastReceiver = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(false);

		mPrefs = this.getSharedPreferences("com.krystal.videocohere",
				Context.MODE_PRIVATE);
		mLinearLayout = (LinearLayout) findViewById(R.id.MainClipsLLContainer);
		mVideoView = (VideoView) findViewById(R.id.MainVideoVV);
		mMainVideoIV = (ImageView) findViewById(R.id.MainVideoIV);
		mMainVideoFullScreenIV = (ImageView) findViewById(R.id.MainVideoFullscreenIV);
		mMainVideoPB = (ProgressBar) findViewById(R.id.MainVideoPB);
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
		case R.id.action_merge_videos:
			updateMyLifeViews(R.drawable.play, null, View.INVISIBLE,
					View.VISIBLE, View.INVISIBLE);
			Log.d("qwe", "Calling service");
			intent = new Intent(this, TranscodingService.class);
			intent.setAction(TranscodingService.ActionMergeAllVideos);
			startService(intent);
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

		FrameLayout mMainVideoFL = (FrameLayout) findViewById(R.id.MainVideoFL);
		new MediaController(this, mMainVideoFL, mVideoView);

		mVideoView
				.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

					@Override
					public void onCompletion(MediaPlayer vmp) {
					/*	mOutputVideo = VideoCohereApplication.getOutputFile();
						if (mOutputVideo != null) {
							updateMyLifeViews(R.drawable.play,
									mOutputVideo.thumbnails, View.VISIBLE,
									View.INVISIBLE, View.VISIBLE);
						} else {
							updateMyLifeViews(R.drawable.no_video, null,
									View.VISIBLE, View.INVISIBLE,
									View.INVISIBLE);
						}*/
					}
				});

		mVideoView.setOnErrorListener(new OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
				Log.e("asd", "Cant Play This Video :(");
				return true;
			}
		});

		mVideoView.setOnPreparedListener(new OnPreparedListener() {

			@Override
			public void onPrepared(MediaPlayer arg0) {
				Log.d("asd", "asdsadsadsadsa");
				if ((mCurrentVideo == -1) && (mOutputVideo != null)) {
					updateMyLifeViews(R.drawable.play, mOutputVideo.thumbnails,
							View.VISIBLE, View.INVISIBLE, View.VISIBLE);
					// TODO check
					mVideoView.seekTo(0);
				}
			}
		});

		mMainVideoIV.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				if (view.getTag() != null) {
					updateMyLifeViews(R.drawable.play, null, View.INVISIBLE,
							View.INVISIBLE, View.INVISIBLE);
					mVideoView.start();
				}
			}
		});

		mMainVideoFullScreenIV.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				if (mOutputVideo != null) {
					Intent playIntent = new Intent();
					playIntent.setAction(Intent.ACTION_VIEW);
					playIntent.setDataAndType(
							Uri.fromFile(new File(mOutputVideo.path)),
							"video/*");

					startActivity(playIntent);
				}
			}
		});

		if ((mVideos != null) && (mVideos.size() > 0)) {
			Video mLastVideo = mVideos.get(0);
			Log.d("Swati", "Video path = " + mLastVideo.path);
			mVideoView.setVideoPath(mLastVideo.path);
			mVideoView.requestFocus();

			int end = mLastVideo.thumbnails.indexOf(",");
			String image = mLastVideo.thumbnails.substring(0, end);
			updateMyLifeViews(R.drawable.play, image, View.VISIBLE,
					View.INVISIBLE, View.VISIBLE);
		}
	}

	public class TranscodingServiceBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d ("qwe", "Enterd onRecieve");
			if (intent.hasExtra("OutputFileName") && this != null) {
				String outputFile = intent.getStringExtra("OutputFileName");

				if (!outputFile.isEmpty()) {
					Log.d("qwe", "Outputfile is not empty");
					mOutputVideo = VideoCohereApplication.getOutputFile();

					if (mOutputVideo != null) {
						Log.d("qwe", "Setting output video path");
						mVideoView.setVideoPath(mOutputVideo.path);
						mVideoView.requestFocus();
						mCurrentVideo = -1;
					}
				} else {
					Log.d("qwe", "Outputfile is empty");
					if (intent.hasExtra("Error")) {
						Toast.makeText(getApplicationContext(),
								intent.getStringExtra("Error"),
								Toast.LENGTH_LONG).show();
					}
					updateMyLifeViews(R.drawable.no_video, null, View.VISIBLE,
							View.INVISIBLE, View.INVISIBLE);
				}
			}
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		if (mDataBroadcastReceiver == null)
			mDataBroadcastReceiver = new TranscodingServiceBroadcastReceiver();

		IntentFilter intentFilter = new IntentFilter(
				TranscodingService.ActionMergeAllVideos);
		LocalBroadcastManager.getInstance(this).registerReceiver(
				mDataBroadcastReceiver, intentFilter);
	}

	@SuppressLint("NewApi")
	public void updateMyLifeViews(int IVRes, String IVPath, int IVVis,
			int PBVis, int TVVis) {

		if (android.os.Build.VERSION.SDK_INT == 14
				|| android.os.Build.VERSION.SDK_INT == 15) {
			mMainVideoIV.setAlpha(200);
		} else if (android.os.Build.VERSION.SDK_INT >= 16) {
			mMainVideoIV.setImageAlpha(200);
		}
		if (IVPath != null) {
			// VideoCohereApplication.mTL.loadImage(IVPath, mMainVideoIV);
			mMainVideoIV.setTag(IVPath);
			mMainVideoFullScreenIV.setVisibility(View.VISIBLE);
		} else {
			mMainVideoIV.setImageResource(IVRes);
			mMainVideoIV.setBackgroundColor(getResources().getColor(
			android.R.color.black));
			mMainVideoIV.setTag(null);
			mMainVideoFullScreenIV.setVisibility(View.INVISIBLE);
		}
		mMainVideoIV.requestLayout();
		mMainVideoIV.setVisibility(IVVis);
		mMainVideoPB.setVisibility(PBVis);

	}

	@Override
	public void onPause() {
		super.onPause();

		if (mDataBroadcastReceiver != null)
			LocalBroadcastManager.getInstance(this).unregisterReceiver(
					mDataBroadcastReceiver);

		if (mVideos == null) {
			return;
		}

		for (Video currentVideo : mVideos) {
			Log.d("Swati", "onPause Video = " + currentVideo.id
					+ " thumbnails = " + currentVideo.thumbnails);
			VideoCohereApplication.mDBA.updateSelections(currentVideo.id,
					currentVideo.startTime, currentVideo.endTime);
		}
	}

	public class VideoListArrayAdapter extends ArrayAdapter<Video> implements
			View.OnClickListener, View.OnLongClickListener {

		private List<Video> mList;
		private LayoutInflater mInflater;
		/* TODO why is this needed? */
		private ViewGroup.MarginLayoutParams params = null;
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
						mSelectedSeekBar.setProgress(mSelectedSeekBar
								.getProgress() - 1);
					}
					return true;
				case R.id.right_button:

					if (mSelectedSeekBar != null) {
						mSelectedSeekBar.setProgress(mSelectedSeekBar
								.getProgress() + 1);
					}
					return true;
				case R.id.toggle_button:
					mSelectedSeekBar = (SeekBar) mSelectedSeekBar
							.getTag(R.string.tag_toggle);
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
		private ActionMode mActionMode;

		private SeekBarOnTouchCallback mOnSeekBarTouchCallback = new SeekBarOnTouchCallback() {
			@Override
			public void onTouch(SeekBar mSeekBar) {
				mSelectedSeekBar = mSeekBar;
				Integer index = (Integer) mSelectedSeekBar
						.getTag(R.string.tag_video);

				if (mCurrentVideo != index.intValue()) {
					mCurrentVideo = index.intValue();
					Video v = mVideos.get(mCurrentVideo);
					mVideoView.setVideoPath(v.path);
				}
				if (!mActionModeShown)
					mActionMode = startActionMode(mActionModeCallback);
			}
		};

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			SeekBar mStartSeekBar = null;
			SeekBar mEndSeekBar = null;

			Video currentVideo = mList.get(position);

			Log.d("Swati", "Entered getView Video = " + currentVideo.id
					+ " position = " + position);
			if (convertView == null) {
				Log.d("Swati", "convertview = null");
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

				Log.d("Swati", "ClipText = " + clipText);
				if ((clipNo != null)
						&& (position + 1) != Integer.parseInt(clipNo)) {
					Log.d("Swati", "Clearing");
					((LinearLayout) convertView
							.findViewById(R.id.MainClipsLLContainer))
							.removeAllViews();
					convertView = mInflater.inflate(
							R.layout.main_clips_list_item, null);
				} else {
					Log.d("Swati", "Returning convertview");
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

			for (String thumbnailPath : thumbnailList) {

				Log.d("Swati", "Adding path = " + thumbnailPath);
				ImageView iv = new ImageView(getContext());
				mLinearLayout.addView(iv, this.params);

				VideoCohereApplication.mTL.loadImage(thumbnailPath, iv);
				// iv.setTag(v);

				// iv.setOnClickListener(this);
				// iv.setOnLongClickListener(this);
			}

			mStartSeekBar = (SeekBar) convertView
					.findViewById(R.id.MainClipsStartSB);
			mEndSeekBar = (SeekBar) convertView
					.findViewById(R.id.MainClipsEndSB);

			long duration = currentVideo.duration;
			/*
			 * int progress = (int) (duration / count); if (progress < 10)
			 * progress = (int) (duration * 10); else
			 */
			int progress = (int) (duration);

			Log.d("Swati", "Progress = " + progress);
			mStartSeekBar.setMax((int) (progress));
			mEndSeekBar.setMax((int) (progress));

			if (currentVideo.startTime == -1)
				currentVideo.startTime = 0;

			if (currentVideo.endTime == -1)
				currentVideo.endTime = progress;

			mStartSeekBar.setTag(R.string.tag_video, position);
			mStartSeekBar.setTag(R.string.tag_is_start_time, true);
			mStartSeekBar.setTag(R.string.tag_toggle, mEndSeekBar);

			mEndSeekBar.setTag(R.string.tag_video, position);
			mEndSeekBar.setTag(R.string.tag_is_start_time, false);
			mEndSeekBar.setTag(R.string.tag_toggle, mStartSeekBar);

			mStartSeekBar.setProgress((int) currentVideo.startTime);
			mEndSeekBar.setProgress((int) currentVideo.endTime);

			((ThumbOnlySeekBar) mStartSeekBar)
					.setOnTouchCallback(mOnSeekBarTouchCallback);
			((ThumbOnlySeekBar) mEndSeekBar)
					.setOnTouchCallback(mOnSeekBarTouchCallback);

			mStartSeekBar
					.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

						@Override
						public void onStopTrackingTouch(SeekBar arg0) {
							// TODO Auto-generated method stub

						}

						@Override
						public void onStartTrackingTouch(SeekBar arg0) {
							// TODO Auto-generated method stub

						}

						@Override
						public void onProgressChanged(SeekBar seekbar,
								int position, boolean arg2) {
							if (mVideoView.isPlaying())
								mVideoView.pause();
							Integer index = (Integer) seekbar
									.getTag(R.string.tag_video);
							Video video = mVideos.get(index.intValue());
							Boolean isStart = (Boolean) seekbar
									.getTag(R.string.tag_is_start_time);

							if ((video != null) && (isStart != null)) {
								Log.d("Swati",
										"position = " + position
												+ " isStart = "
												+ isStart.booleanValue()
												+ " video start = "
												+ video.startTime + " end = "
												+ video.endTime);
								if (isStart.booleanValue() == true) {
									/*
									 * If new position exceeds end seekbar set
									 * to itself
									 */

									if (position > video.endTime)
										seekbar.setProgress((int) video.startTime);
									else {
										/* Save new position */
										video.startTime = position;
										mVideoView.seekTo(position * 1000);
									}
								} else if (isStart.booleanValue() == false) {

									if (position < video.startTime)
										seekbar.setProgress((int) video.endTime);
									else {
										video.endTime = position;
										mVideoView.seekTo(position * 1000);
									}
								}
							}

						}
					});

			mEndSeekBar
					.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

						@Override
						public void onStopTrackingTouch(SeekBar arg0) {
							// TODO Auto-generated method stub

						}

						@Override
						public void onStartTrackingTouch(SeekBar arg0) {
							// TODO Auto-generated method stub

						}

						@Override
						public void onProgressChanged(SeekBar seekbar,
								int position, boolean arg2) {
							if (mVideoView.isPlaying())
								mVideoView.pause();
							Integer index = (Integer) seekbar
									.getTag(R.string.tag_video);
							Video video = mVideos.get(index.intValue());
							Boolean isStart = (Boolean) seekbar
									.getTag(R.string.tag_is_start_time);

							if ((video != null) && (isStart != null)) {
								Log.d("Swati",
										"position = " + position
												+ " isStart = "
												+ isStart.booleanValue()
												+ " video start = "
												+ video.startTime + " end = "
												+ video.endTime);
								if (isStart.booleanValue() == true) {
									/*
									 * If new position exceeds end seekbar set
									 * to itself
									 */

									if (position > video.endTime)
										seekbar.setProgress((int) video.startTime);
									else {
										/* Save new position */
										video.startTime = position;
										mVideoView.seekTo(position * 1000);
									}
								} else if (isStart.booleanValue() == false) {

									if (position < video.startTime)
										seekbar.setProgress((int) video.endTime);
									else {
										video.endTime = position;
										mVideoView.seekTo(position * 1000);
									}
								}
							}
						}
					});

			LinearLayout l = (LinearLayout) convertView
					.findViewById(R.id.MainClipsOuterLL);
			l.setTag(position);
			l.setOnClickListener(this);

			mLinearLayout.setTag(position);
			mLinearLayout.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View view) {
					if (mActionModeShown)
						mActionMode.finish();
					LinearLayout l = (LinearLayout) view;
					Integer p = (Integer) l.getTag();
					int index = p.intValue();

					updateMyLifeViews(R.drawable.play, null, View.INVISIBLE,
							View.INVISIBLE, View.INVISIBLE);
					final Video v = mVideos.get(index);
					if (mCurrentVideo != index) {
						mCurrentVideo = index;
						mVideoView.setVideoPath(v.path);
					}
					long time = (long) ((v.endTime - v.startTime) * 1000);
					Log.d("Swati", "Play time = " + time + "start = "
							+ v.startTime);
					mVideoView.seekTo((int) v.startTime * 1000);
					mVideoView.start();
					mVideoView.postDelayed(new Runnable() {

						@Override
						public void run() {
							mVideoView.pause();
							// mVideoView.seekTo((int) v.startTime * 1000);
							int end = v.thumbnails.indexOf(",");
							String image = v.thumbnails.substring(0, end);
							updateMyLifeViews(R.drawable.play, image,
									View.VISIBLE, View.INVISIBLE, View.VISIBLE);
						}
					}, time);
				}
			});
			return convertView;
		}

		@Override
		public boolean onLongClick(View view) {
			return true;
		}

		@Override
		public void onClick(View view) {
			/*
			 * LinearLayout l = (LinearLayout) view; Integer position =
			 * (Integer) l.getTag(); Log.d("Swati",
			 * "Entered onclick position = " + position);
			 * 
			 * if ((mVideos != null) && (position != null)) { Log.d("Swati",
			 * "onClick position = " + position.intValue()); Video mLastVideo =
			 * mVideos.get(position.intValue()); Log.d("Swati", "Video path = "
			 * + mLastVideo.path); mVideoView.setVideoPath(mLastVideo.path); int
			 * end = mLastVideo.thumbnails.indexOf(","); String image =
			 * mLastVideo.thumbnails.substring(0, end);
			 * updateMyLifeViews(R.drawable.play, image, View.VISIBLE,
			 * View.INVISIBLE, View.VISIBLE); }
			 */
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
			/*
			 * mVideoWidth = Integer .parseInt(mMediaRetriever
			 * .extractMetadata(MediaMetadataRetriever
			 * .METADATA_KEY_VIDEO_WIDTH)); mVideoHeight = Integer
			 * .parseInt(mMediaRetriever
			 * .extractMetadata(MediaMetadataRetriever.
			 * METADATA_KEY_VIDEO_HEIGHT));
			 */
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

		/*
		 * Movie movie = null; try { movie = MovieCreator.build(new
		 * FileInputStream(mPath).getChannel()); } catch (FileNotFoundException
		 * e1) { e1.printStackTrace(); } catch (IOException e1) {
		 * e1.printStackTrace(); }
		 * 
		 * tracks = movie.getTracks();
		 */
	}

	public void refreshVideoList() {
		mVideos = ((VideoCohereApplication) getApplication()).getDBA()
				.getVideos();

		mVideoListAdapter = new VideoListArrayAdapter(this, mVideos);
		ListView mVideoList = (ListView) findViewById(R.id.MainClipsLV);
		mVideoList.setAdapter(mVideoListAdapter);
		mThumbnailPB.dismiss();
		mThumbnailPB = null;

		Video mLastVideo = mVideos.get(mVideos.size() - 1);
		Log.d("Swati", "Video path = " + mLastVideo.path);
		mVideoView.setVideoPath(mLastVideo.path);
		int end = mLastVideo.thumbnails.indexOf(",");
		String image = mLastVideo.thumbnails.substring(0, end);
		updateMyLifeViews(R.drawable.play, image, View.VISIBLE, View.INVISIBLE,
				View.VISIBLE);

		mVideoView.requestFocus();
	}
}
