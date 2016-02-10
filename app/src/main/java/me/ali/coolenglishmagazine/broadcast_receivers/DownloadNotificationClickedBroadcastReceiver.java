package me.ali.coolenglishmagazine.broadcast_receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import me.ali.coolenglishmagazine.IssueListActivity;
import me.ali.coolenglishmagazine.util.LogHelper;

public class DownloadNotificationClickedBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = LogHelper.makeLogTag(DownloadNotificationClickedBroadcastReceiver.class);

    public DownloadNotificationClickedBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent downloadListIntent = new Intent(context, IssueListActivity.class);
        downloadListIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        downloadListIntent.setAction(IssueListActivity.ACTION_SHOW_DOWNLOADS);
        context.startActivity(downloadListIntent);
    }

}
