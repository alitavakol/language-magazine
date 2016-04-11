package me.ali.coolenglishmagazine.broadcast_receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;

import me.ali.coolenglishmagazine.AlarmsTabFragment;

public class BootReceiver extends BroadcastReceiver {
    public BootReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        AlarmsTabFragment.startAllAlarms(context);
    }
}
