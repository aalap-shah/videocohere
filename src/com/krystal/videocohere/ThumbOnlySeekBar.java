package com.krystal.videocohere;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class ThumbOnlySeekBar extends SeekBar {

	private Drawable mThumb;

	public ThumbOnlySeekBar(Context context) {
		super(context);
	}

	public ThumbOnlySeekBar(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ThumbOnlySeekBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public void setThumb(Drawable thumb) {
		Log.d("Swati", "Entered set thumb");
		super.setThumb(thumb);
		mThumb = thumb;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		Log.d("Swati", "Event x = " + (int) event.getX() + " Event y = "
				+ (int) event.getY());
		Log.d ("Swati", "Bounds = " + mThumb.getBounds());
		
		if (!mThumb.getBounds()
				.contains((int) event.getX(), (int) event.getY()))
			return false;
		return super.onTouchEvent(event);
	}
	
	

}
