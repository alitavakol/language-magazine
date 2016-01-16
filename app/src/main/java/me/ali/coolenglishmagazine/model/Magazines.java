package me.ali.coolenglishmagazine.model;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.preference.PreferenceManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.ali.coolenglishmagazine.R;
import me.ali.coolenglishmagazine.util.LogHelper;

public class Magazines {

    private static final String TAG = LogHelper.makeLogTag(Magazines.class);

    /**
     * An array of magazine issues.
     */
    public final List<Issue> ISSUES = new ArrayList<>();

    /**
     * populates list of {@code ISSUES} from the specified root directory of device's local storage
     */
    public void loadIssues(String issuesRootDirectory) {
        ISSUES.clear();

        File f = new File(issuesRootDirectory);
        File[] files = f.listFiles();
        for (File g : files) {
            if (g.isDirectory()) {
                try {
                    addIssue(getIssue(g));
                } catch (IOException e) {
                    LogHelper.e(TAG, e.getMessage());
                }
            }
        }
    }

    public static Issue getIssue(File issueRootDirectory) throws IOException {
        Issue issue = new Issue();

        File input = new File(issueRootDirectory, Issue.manifestFileName);
        final Document doc = Jsoup.parse(input, "UTF-8", "");

        Element e = doc.getElementsByTag("issue").first();
        if (e == null)
            throw new IOException("Invalid manifest file.");

        issue.rootDirectory = issueRootDirectory;
        issue.title = e.attr("title");
        issue.posterFileName = e.attr("poster");
        issue.id = Integer.parseInt(issueRootDirectory.getName());

        return issue;
    }

    private void addIssue(Issue issue) {
        ISSUES.add(issue);
    }

    /**
     * Properties of a magazine issue
     */
    public static class Issue {
        /**
         * file containing issue properties
         */
        protected static final String manifestFileName = "manifest.xml";

        /**
         * if this file is present, issue is downloaded and available to read offline.
         */
        public static final String downloadedFileName = "downloaded";

        /**
         * an string representation of this issue, combination of year and week number.
         * e.g. "Cool English Magazine #1 (2016, Week #1)
         */
        public String title;

        public String posterFileName;

        public File rootDirectory;

        /**
         * unique identifier which is also magazine root folder's name
         */
        public int id;

        @Override
        public String toString() {
            return title;
        }
    }

    /**
     * @return download reference number
     */
    public static long download(Context context, Issue issue) throws IOException {
        int id = Integer.parseInt(issue.rootDirectory.getName());

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(getIssueDownloadUrl(context, issue)))
                .setDescription(issue.title)
                .setTitle(context.getResources().getString(R.string.app_name))
                .setDestinationInExternalFilesDir(context, null, "" + id + ".zip")
                .setVisibleInDownloadsUi(false)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        return downloadManager.enqueue(request);
    }

    /**
     * @return URL from which the zip archive of the given issue can be downloaded.
     */
    public static String getIssueDownloadUrl(Context context, Issue issue) {
        final Uri uri = Uri.parse(PreferenceManager.getDefaultSharedPreferences(context).getString("server_address", context.getResources().getString(R.string.pref_default_server_address)));
        // http://docs.oracle.com/javase/tutorial/networking/urls/urlInfo.html
        return uri.toString() + "/api/issues/" + Integer.parseInt(issue.rootDirectory.getName());
    }

    /**
     * @return download status (if there is any) or -1
     */
    public static int getDownloadStatus(Context context, Issue issue) {
        final String issueDownloadUrl = getIssueDownloadUrl(context, issue);

        DownloadManager.Query query = new DownloadManager.Query();

        Cursor cursor = ((DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE)).query(query);
        final int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_URI);

        while (cursor.moveToNext()) {
            final String cursorUrl = cursor.getString(uriIndex);
            if (cursorUrl.equals(issueDownloadUrl)) {
                return cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
            }
        }

        cursor.close();
        return -1;
    }

}
