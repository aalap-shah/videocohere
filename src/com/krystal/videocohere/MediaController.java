package com.krystal.videocohere;

import java.lang.ref.WeakReference;
import java.util.Formatter;
import java.util.Locale;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.VideoView;

public class MediaController {

	private VideoView mVideoView = null;
	private RelativeLayout mContainer = null;
	private ImageView mPlayButton = null;
	private SeekBar mSeekBar = null;
	private TextView mStartText = null, mEndText = null;
	private Handler mHandler = new MessageHandler(this);
	private static final int sDefaultTimeout = 3000;
	private boolean mShowing;
	private boolean mDragging;
	private static final int FADE_OUT = 1;
	private static final int SHOW_PROGRESS = 2;
	StringBuilder mFormatBuilder;
	Formatter mFormatter;

	public MediaController(Context c, FrameLayout frameLayout,
			VideoView vv) {
		LayoutInflater inflater = (LayoutInflater) c
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		mVideoView = vv;

		mContainer = (RelativeLayout) inflater.inflate(
				R.layout.media_controller, null);
		LayoutParams params = new FrameLayout.LayoutParams(
				LayoutParams.MATCH_PARENT, 100, Gravity.BOTTOM);
		frameLayout.addView(mContainer, params);

		updatePausePlay();

		mPlayButton = (ImageView) mContainer
				.findViewById(R.id.MediaControllerIV);
		if (mPlayButton != null) {
			mPlayButton.requestFocus();
			mPlayButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					doPauseResume();
					show(sDefaultTimeout);
				}
			});
		}

		mSeekBar = (SeekBar) mContainer
				.findViewById(R.id.MediaControllerSB);
		if (mSeekBar != null) {
			mSeekBar.setOnSeekBarChangeListener(mSeekListener);
		}

		mStartText = (TextView) mContainer
				.findViewById(R.id.MediaControllerTV1);
		mEndText = (TextView) mContainer
				.findViewById(R.id.MediaControllerTV2);
		mFormatBuilder = new StringBuilder();
		mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

		mVideoView.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				show(sDefaultTimeout);
				return false;
			}
		});
	}

	public void show() {
		show(sDefaultTimeout);
	}

	public void show(int timeout) {
		mContainer.setVisibility(View.VISIBLE);

		if (!mShowing) {
			setProgress();
			if (mPlayButton != null) {
				mPlayButton.requestFocus();
			}

			mShowing = true;
		}
		updatePausePlay();

		mHandler.sendEmptyMessage(SHOW_PROGRESS);

		Message msg = mHandler.obtainMessage(FADE_OUT);
		if (timeout != 0) {
			mHandler.removeMessages(FADE_OUT);
			mHandler.sendMessageDelayed(msg, timeout);
		}
	}

	public void hide() {
		try {
			mContainer.setVisibility(View.INVISIBLE);
			mHandler.removeMessages(SHOW_PROGRESS);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		mShowing = false;
	}

	private String stringForTime(int timeMs) {
		int totalSeconds = timeMs / 1000;

		int seconds = totalSeconds % 60;
		int minutes = (totalSeconds / 60) % 60;
		int hours = totalSeconds / 3600;

		mFormatBuilder.setLength(0);
		if (hours > 0) {
			return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds)
					.toString();
		} else {
			return mFormatter.format("%02d:%02d", minutes, seconds).toString();
		}
	}

	private int setProgress() {
		if (mVideoView == null || mDragging) {
			return 0;
		}

		int position = mVideoView.getCurrentPosition();
		int duration = mVideoView.getDuration();
		if (mSeekBar != null) {
			if (duration > 0) {
				long pos = 1000L * position / duration;
				mSeekBar.setProgress((int) pos);
			}
			int percent = mVideoView.getBufferPercentage();
			mSeekBar.setSecondaryProgress(percent * 10);
		}

		if (mStartText != null)
			mStartText.setText(stringForTime(duration));
		if (mEndText != null)
			mEndText.setText(stringForTime(position));

		return position;
	}

	public void updatePausePlay() {
		if (mPlayButton == null || mVideoView == null) {
			return;
		}

		if (mVideoView.isPlaying()) {
			mPlayButton.setImageResource(android.R.drawable.ic_media_pause);
		} else {
			mPlayButton.setImageResource(android.R.drawable.ic_media_play);
		}
	}

	private void doPauseResume() {
		if (mVideoView == null) {
			return;
		}

		if (mVideoView.isPlaying()) {
			mVideoView.pause();
		} else {
			mVideoView.start();
		}
		updatePausePlay();
	}

	private static class MessageHandler extends Handler {
		private final WeakReference<MediaController> mMediaControllerRef;

		MessageHandler(MediaController view) {
			mMediaControllerRef = new WeakReference<MediaController>(view);
		}

		@Override
		public void handleMessage(Message msg) {
			MediaController mMediaController = mMediaControllerRef.get();
			if (mMediaController == null || mMediaController.mVideoView == null) {
				return;
			}

			int pos;
			switch (msg.what) {
			case FADE_OUT:
				mMediaController.hide();
				break;
			case SHOW_PROGRESS:
				pos = mMediaController.setProgress();
				if (!mMediaController.mDragging && mMediaController.mShowing
						&& mMediaController.mVideoView.isPlaying()) {
					msg = obtainMessage(SHOW_PROGRESS);
					sendMessageDelayed(msg, 1000 - (pos % 1000));
				}
				break;
			}
		}
	}

	private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
		public void onStartTrackingTouch(SeekBar bar) {
			show(3600000);

			mDragging = true;

			// By removing these pending progress messages we make sure
			// that a) we won't update the progress while the user adjusts
			// the seekbar and b) once the user is done dragging the thumb
			// we will post one of these messages to the queue again and
			// this ensures that there will be exactly one message queued up.
			mHandler.removeMessages(SHOW_PROGRESS);
		}

		public void onProgressChanged(SeekBar bar, int progress,
				boolean fromuser) {
			if (mVideoView == null) {
				return;
			}

			if (!fromuser) {
				// We're not interested in programmatically generated changes to
				// the progress bar's position.
				return;
			}

			long duration = mVideoView.getDuration();
			long newposition = (duration * progress) / 1000L;
			mVideoView.seekTo((int) newposition);
			if (mEndText != null)
				mEndText.setText(stringForTime((int) newposition));
		}

		public void onStopTrackingTouch(SeekBar bar) {
			mDragging = false;
			setProgress();
			updatePausePlay();
			show(sDefaultTimeout);

			// Ensure that progress is properly updated in the future,
			// the call to show() does not guarantee this because it is a
			// no-op if we are already showing.
			mHandler.sendEmptyMessage(SHOW_PROGRESS);
		}
	};
}
