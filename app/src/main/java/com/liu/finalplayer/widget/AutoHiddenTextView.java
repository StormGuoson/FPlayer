package com.liu.finalplayer.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.animation.AlphaAnimation;
import android.widget.TextView;

/**
 * Created by StormGuoson on 2017/1/18.
 */

public class AutoHiddenTextView extends TextView {

    public AutoHiddenTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void start() {
        new Thread(runnable).start();
    }

    protected Runnable runnable = new Runnable() {
        @Override
        public void run() {
            try {
                handler.sendEmptyMessage(1);
                Thread.sleep(500);
                handler.sendEmptyMessage(0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };
    protected Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                AlphaAnimation alphaAnimation = new AlphaAnimation(1, 0);
                alphaAnimation.setDuration(500);
                alphaAnimation.setFillAfter(true);
                startAnimation(alphaAnimation);
            } else if (msg.what == 1) {
                setVisibility(VISIBLE);
            }
        }
    };
}
