package me.ali.coolenglishmagazine.data;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.ali.coolenglishmagazine.util.LogHelper;

public class Magazines {

    private static final String TAG = LogHelper.makeLogTag(Magazines.class);

    /**
     * An array of magazine issues.
     */
    public final List<Issue> ISSUES = new ArrayList<>();

    public void loadIssues(String issuesRootDirectory) {
        File f = new File(issuesRootDirectory);
        File[] files = f.listFiles();
        for (File g : files) {
            if (g.isDirectory()) {
                try {
                    addIssue(getIssue(g.getAbsolutePath() + "/"));
                } catch (IOException e) {
                    LogHelper.e(TAG, e.getMessage());
                }
            }
        }
    }

    public static Issue getIssue(String issueRootDirectory) throws IOException {
        Issue issue = new Issue();

        File input = new File(issueRootDirectory, Issue.manifestFileName);
        final Document doc = Jsoup.parse(input, "UTF-8", "");

        Element e = doc.getElementsByTag("issue").first();
        if (e == null)
            throw new IOException("Invalid manifest file.");

        issue.rootDirectory = issueRootDirectory;
        issue.title = e.attr("title");
        issue.posterFileName = e.attr("poster");

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
         * an string representation of this issue, combination of year and week number.
         * e.g. "Cool English Magazine #1 (2016, Week #1)
         */
        public String title;

        public String posterFileName;

        public String rootDirectory;

        @Override
        public String toString() {
            return title;
        }
    }
}
