package me.ali.coolenglishmagazine.util;

import android.os.Handler;
import android.view.View;

import java.util.Timer;
import java.util.TimerTask;

public class Blinker {
    View view;
    boolean visible;

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (stopped)
                return;

            if (view != null) {
                visible = !visible;
                view.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            }
        }
    };

    Handler handler = new Handler();
    Timer timer;

    boolean stopped;

    public void setBlinkingView(View view) {
        this.view = view;
    }

    public void start() {
        if (timer != null)
            return;

        stopped = false;
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(runnable);
            }
        }, 0, 500);
    }

    public void stop() {
        stopped = true;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        if (view != null)
            view.setVisibility(View.VISIBLE);
    }
}
