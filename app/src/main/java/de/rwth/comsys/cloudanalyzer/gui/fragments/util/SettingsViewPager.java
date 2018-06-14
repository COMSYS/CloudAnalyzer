package de.rwth.comsys.cloudanalyzer.gui.fragments.util;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class SettingsViewPager extends ViewPager
{

    private float initVal;
    private Direction direction;
    public SettingsViewPager(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.direction = Direction.all;
    }

    @Override
    public boolean performClick()
    {
        return super.performClick();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (this.swipeAllowed(event))
        {
            return super.onTouchEvent(event);
        }
        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event)
    {
        if (this.swipeAllowed(event))
        {
            return super.onInterceptTouchEvent(event);
        }
        return false;
    }

    private boolean swipeAllowed(MotionEvent event)
    {
        if (this.direction == Direction.all)
            return true;

        if (this.direction == Direction.none)
            return false;

        if (event.getAction() == MotionEvent.ACTION_DOWN)
        {
            initVal = event.getX();
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_MOVE)
        {
            try
            {
                float diff = event.getX() - initVal;
                if (diff < 0 && direction == Direction.left)
                {
                    return false;
                }
            }
            catch (Exception exception)
            {
                exception.printStackTrace();
            }
        }
        return true;
    }

    public void setAllowedSwipeDirection(Direction direction)
    {
        this.direction = direction;
    }

    public enum Direction
    {
        all, left, none
    }
}
