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
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            ArrayList<AlarmsTabFragment.Alarm> alarms = AlarmsTabFragment.importAlarms(context);
            for (AlarmsTabFragment.Alarm alarm : alarms) {
                if (alarm.enabled)
                    AlarmsTabFragment.turnOnAlarm(context, alarm);
            }
        }
    }
}
