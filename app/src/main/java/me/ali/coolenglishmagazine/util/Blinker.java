package me.ali.coolenglishmagazine.util;

import android.os.Handler;
import android.view.View;

import java.util.Timer;
import java.util.TimerTask;

import me.ali.coolenglishmagazine.R;

public class Blinker {
    View view;
    View bullet;
    boolean visible;

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            if (stopped)
                return;

            if (bullet == null && view != null)
                bullet = view.findViewById(R.id.tab_icon);

            if (bullet != null) {
                visible = !visible;
                bullet.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
            }
        }
    };

    Handler handler = new Handler();
    Timer timer;

    boolean stopped;

    public void setTabView(View view) {
        this.view = view;
        bullet = null;
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
        if (bullet != null)
            bullet.setVisibility(View.VISIBLE);
    }
}
