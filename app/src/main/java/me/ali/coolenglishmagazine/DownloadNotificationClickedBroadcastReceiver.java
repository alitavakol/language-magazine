package me.ali.coolenglishmagazine;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import java.io.File;
import java.io.IOException;

import me.ali.coolenglishmagazine.data.Magazines;
import me.ali.coolenglishmagazine.util.LogHelper;

public class DownloadNotificationClickedBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = LogHelper.makeLogTag(DownloadNotificationClickedBroadcastReceiver.class);

    public DownloadNotificationClickedBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        long[] references = intent.getLongArrayExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS);

        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(references);

        Cursor cursor = ((DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE)).query(query);
        final int fileNameIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);

        while (cursor.moveToNext()) {
            final String savedFilePath = cursor.getString(fileNameIndex);

            try {
                final String issueId = new File(savedFilePath).getName().split("\\.(?=[^\\.]+$)")[0];
                final Magazines.Issue issue = Magazines.getIssue(new File(context.getExternalFilesDir(null), issueId));

                Intent issueDetailsIntent = new Intent(context, IssueDetailActivity.class);
                issueDetailsIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                issueDetailsIntent.putExtra(IssueDetailActivity.ARG_ROOT_DIRECTORY, issue.rootDirectory.getAbsolutePath());

                context.startActivity(issueDetailsIntent);

            } catch (IOException e) {
            }
        }

        cursor.close();
    }

}
