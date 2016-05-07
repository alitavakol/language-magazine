package me.ali.coolenglishmagazine.broadcast_receivers;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;

import java.io.IOException;

import me.ali.coolenglishmagazine.R;
import me.ali.coolenglishmagazine.ReadAndListenActivity;
import me.ali.coolenglishmagazine.model.MagazineContent;
import me.ali.coolenglishmagazine.model.Magazines;
import me.ali.coolenglishmagazine.model.WaitingItems;

public class AlarmBroadcastReceiver extends BroadcastReceiver {

    /**
     * it takes up {@code Magazines.MAX_ISSUES} * {@code MagazineContent.MAX_ITEMS} interval
     */
    public static final int COOL_ENGLISH_TIME_NOTIFICATION_ID = DownloadCompleteBroadcastReceiver.ISSUE_DOWNLOADED_NOTIFICATION_ID + Magazines.MAX_ISSUES;

    public AlarmBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent_) {
        // prepare intent which is triggered if the notification is selected
        Intent intent = new Intent(context, ReadAndListenActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        // find top item from the waiting list of lessons.
        WaitingItems.importWaitingItems(context);
        for (WaitingItems.WaitingItem waitingItem : WaitingItems.waitingItems) {
            try {
                final MagazineContent.Item item = MagazineContent.getItem(waitingItem.itemRootDirectory);

                intent.putExtra(ReadAndListenActivity.ARG_ROOT_DIRECTORY, waitingItem.itemRootDirectory.getAbsolutePath());

                // use System.currentTimeMillis() to have a unique ID for the pending intent
                PendingIntent pIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 0);

                // build notification
                NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                        .setContentTitle(context.getResources().getString(R.string.cool_english_times))
                        .setContentText(context.getResources().getString(R.string.cool_english_time_notification_text_short))
                        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.notification_large_icon))
                        .setSmallIcon(R.drawable.sunglasses)
                        .setColor(context.getResources().getColor(R.color.primary_dark))
                        .setContentIntent(pIntent)
                        .setAutoCancel(true);

                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("notification_sound_enabled", true))
                    builder.setSound(Uri.parse(PreferenceManager.getDefaultSharedPreferences(context).getString("notification_ringtone", Settings.System.DEFAULT_NOTIFICATION_URI.getPath())));

                if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("notification_vibrate_enabled", false))
                    builder.setVibrate(new long[]{100, 250, 250, 250});

                // construct expanded view for the notification
                NotificationCompat.BigTextStyle textStyle = new NotificationCompat.BigTextStyle();

                textStyle.setBigContentTitle(context.getResources().getString(R.string.cool_english_times));
                textStyle.bigText(context.getResources().getString(R.string.cool_english_time_notification_text, item.title));
                textStyle.setSummaryText(context.getResources().getQuantityString(
                        R.plurals.cool_english_time_notification_summary_text,
                        waitingItem.hitCount, waitingItem.hitCount));

                // Moves the expanded layout object into the notification object.
                builder.setStyle(textStyle);

                ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(COOL_ENGLISH_TIME_NOTIFICATION_ID + item.getUid(), builder.build());

                break;

            } catch (IOException e) {
                // build notification for next waiting item
            }
        }
    }

}
