package me.ali.coolenglishmagazine;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import me.ali.coolenglishmagazine.util.LogHelper;

public class DownloadNotificationClickedBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = LogHelper.makeLogTag(DownloadNotificationClickedBroadcastReceiver.class);

    public DownloadNotificationClickedBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String extraId = DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS;
        long[] references = intent.getLongArrayExtra(extraId);
        for (long reference : references) {
//            Intent downloadingIssueDetailsIntent = new Intent(context, IssueDetailActivity.class);
//            downloadingIssueDetailsIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//            downloadingIssueDetailsIntent.putExtra(IssueDetailActivity.ARG_ROOT_DIRECTORY, issue.rootDirectory.getAbsolutePath());
            LogHelper.i(TAG, "Download notification " + reference + " clicked.");
        }
    }

}
