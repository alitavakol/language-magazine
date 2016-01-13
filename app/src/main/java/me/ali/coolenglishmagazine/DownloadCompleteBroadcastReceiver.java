package me.ali.coolenglishmagazine;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import me.ali.coolenglishmagazine.data.Magazines;
import me.ali.coolenglishmagazine.util.LogHelper;
import me.ali.coolenglishmagazine.util.ZipHelper;

public class DownloadCompleteBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = LogHelper.makeLogTag(DownloadCompleteBroadcastReceiver.class);

    protected static final int ISSUE_DOWNLOADED_NOTIFICATION_ID = 200;

    public DownloadCompleteBroadcastReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent_) {
        long reference = intent_.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);

        // do something with the download file
        DownloadManager.Query query = new DownloadManager.Query();
        query.setFilterById(reference);

        Cursor cursor = ((DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE)).query(query);
        cursor.moveToFirst();

        // get the status of the download
        int columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
        int status = cursor.getInt(columnIndex);

        int fileNameIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME);
        String savedFilePath = cursor.getString(fileNameIndex);

        // get the reason - more detail on the status
        int columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
        int reason = cursor.getInt(columnReason);

        switch (status) {
            case DownloadManager.STATUS_SUCCESSFUL:
                try {
                    File f = new File(savedFilePath);
                    ZipHelper.unzip(f, context.getExternalFilesDir(null));
                    f.delete();

                    final String issueId = new File(savedFilePath).getName().split("\\.(?=[^\\.]+$)")[0];
                    final Magazines.Issue issue = Magazines.getIssue(new File(context.getExternalFilesDir(null), issueId));

                    // prepare intent which is triggered if the notification is selected
                    Intent intent = new Intent(context, IssueDetailActivity.class);
                    intent.putExtra(IssueDetailActivity.ARG_ROOT_DIRECTORY, issue.rootDirectory.getAbsolutePath());

                    // use System.currentTimeMillis() to have a unique ID for the pending intent
                    PendingIntent pIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 0);

                    // build notification
                    // the addAction re-use the same intent to keep the example short
                    Notification n = new Notification.Builder(context)
                            .setContentTitle(issue.title)
                            .setContentText(context.getResources().getString(R.string.issue_downloaded_notification))
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentIntent(pIntent)
                            .setAutoCancel(true)
//                            .addAction(R.drawable.icon, "Call", pIntent)
//                            .addAction(R.drawable.icon, "More", pIntent)
//                            .addAction(R.drawable.icon, "And more", pIntent)
                            .build();

                    ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(ISSUE_DOWNLOADED_NOTIFICATION_ID + issue.id, n);

                } catch (IOException e) {
                    LogHelper.e(TAG, e.getMessage());
                }
                break;

            case DownloadManager.STATUS_FAILED:
                Toast.makeText(context, "FAILED: " + reason, Toast.LENGTH_LONG).show();
                break;

            case DownloadManager.STATUS_PAUSED:
                Toast.makeText(context, "PAUSED: " + reason, Toast.LENGTH_LONG).show();
                break;

            case DownloadManager.STATUS_PENDING:
                Toast.makeText(context, "PENDING!", Toast.LENGTH_LONG).show();
                break;

            case DownloadManager.STATUS_RUNNING:
                Toast.makeText(context, "RUNNING!", Toast.LENGTH_LONG).show();
                break;
        }

        cursor.close();
    }

}
