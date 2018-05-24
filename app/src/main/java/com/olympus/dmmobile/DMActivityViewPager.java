package com.olympus.dmmobile;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Custom ViewPager
 * @version 1.0.1
 *
 */
public class DMActivityViewPager extends ViewPager{
	private int childId;   
	
	public DMActivityViewPager(Context context) {
		super(context);
	}
	public DMActivityViewPager(Context context, AttributeSet attrs){
		super(context, attrs);
	}
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (childId > 0){  
			ViewPager pager = (ViewPager)findViewById(childId);
			if (pager != null) {
				pager.requestDisallowInterceptTouchEvent(true);
			}
		}
		return super.onInterceptTouchEvent(event);
	}
	public void setchildId(int id) {
		this.childId = id;
	}
}