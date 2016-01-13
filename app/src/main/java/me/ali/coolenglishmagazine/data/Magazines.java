package me.ali.coolenglishmagazine.data;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;

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
     * @param context
     * @param issueRootDirectory
     * @return download reference number
     */
    public static long download(Context context, File issueRootDirectory) throws IOException {
        final Issue issue = Magazines.getIssue(issueRootDirectory);

        int id = Integer.parseInt(issueRootDirectory.getName());
        final String url = "http://10.0.2.2:3000/api/issues/" + id;

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                .setDescription(issue.title)
                .setTitle(context.getResources().getString(R.string.app_name))
                .setDestinationInExternalFilesDir(context, null, "" + id + ".zip")
                .setVisibleInDownloadsUi(false)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);

        DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        return downloadManager.enqueue(request);
    }
}
