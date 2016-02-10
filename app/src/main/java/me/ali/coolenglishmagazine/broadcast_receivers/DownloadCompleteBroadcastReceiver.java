package me.ali.coolenglishmagazine.broadcast_receivers;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import me.ali.coolenglishmagazine.IssueDetailActivity;
import me.ali.coolenglishmagazine.ItemListActivity;
import me.ali.coolenglishmagazine.R;
import me.ali.coolenglishmagazine.model.Magazines;
import me.ali.coolenglishmagazine.util.LogHelper;
import me.ali.coolenglishmagazine.util.ZipHelper;

public class DownloadCompleteBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = LogHelper.makeLogTag(DownloadCompleteBroadcastReceiver.class);

    public static final int ISSUE_DOWNLOADED_NOTIFICATION_ID = 200;

    /**
     * local broadcast intent action that is sent when an issue has been downloaded and extracted.
     */
    public static final String ACTION_DOWNLOAD_EXTRACTED = "me.ali.coolenglishmagazine.ACTION_DOWNLOAD_EXTRACTED";

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
                new UnzipOperation().execute(context, new File(savedFilePath));
                break;

            case DownloadManager.STATUS_FAILED:
                Toast.makeText(context, context.getResources().getString(R.string.download_failed_msg) + reason, Toast.LENGTH_LONG).show();
                break;
        }

        cursor.close();
    }

    /**
     * a background task that extracts downloaded zip archive, and emits a download extracted broadcast intent.
     */
    private class UnzipOperation extends AsyncTask<Object, Void, Void> {
        Context context;
        File f;

        @Override
        protected Void doInBackground(Object... params) {
            context = (Context) params[0];
            f = (File) params[1];

            try {
                ZipHelper.unzip(f, context.getExternalFilesDir(null));
            } catch (IOException e) {
                LogHelper.e(TAG, e.getMessage());
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void params) {
            try {
                f.delete();

                final String issueId = f.getName().split("\\.(?=[^\\.]+$)")[0];
                final Magazines.Issue issue = Magazines.getIssue(new File(context.getExternalFilesDir(null), issueId));

                // prepare intent which is triggered if the notification is selected
                Intent intent = new Intent(context, ItemListActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
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
                        .build();

                ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE)).notify(ISSUE_DOWNLOADED_NOTIFICATION_ID + issue.id, n);

                LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(DownloadCompleteBroadcastReceiver.ACTION_DOWNLOAD_EXTRACTED));

            } catch (IOException e) {
                LogHelper.e(TAG, e.getMessage());
            }
        }
    }

}
