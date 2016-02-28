package me.ali.coolenglishmagazine.broadcast_receivers;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Settings;

import me.ali.coolenglishmagazine.R;
import me.ali.coolenglishmagazine.ReadAndListenActivity;

public class AlarmBroadcastReceiver extends BroadcastReceiver {

    public static final int COOL_ENGLISH_TIME_NOTIFICATION_ID = 101;

    public AlarmBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent_) {
        // prepare intent which is triggered if the notification is selected
        Intent intent = new Intent(context, ReadAndListenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // TODO: find top item from the waiting list of lessons.
//        intent.putExtra(ReadAndListenActivity.ARG_ROOT_DIRECTORY, itemRootDirectory);

        // use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 0);

        // build notification
        Notification.Builder builder = new Notification.Builder(context)
                .setContentTitle(context.getResources().getString(R.string.cool_english_times))
                .setContentText(context.getResources().getString(R.string.cool_english_time_notification_text))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pIntent)
                .setAutoCancel(true);

        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("notification_sound_enabled", true))
            builder.setSound(Uri.parse(PreferenceManager.getDefaultSharedPreferences(context).getString("notification_ringtone", Settings.System.DEFAULT_NOTIFICATION_URI.getPath())));

        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("notification_vibrate_enabled", false))
            builder.setVibrate(new long[]{100, 250, 250, 250});

        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(COOL_ENGLISH_TIME_NOTIFICATION_ID, builder.build());
    }

}
