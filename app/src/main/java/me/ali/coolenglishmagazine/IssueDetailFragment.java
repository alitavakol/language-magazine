package me.ali.coolenglishmagazine;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.io.IOException;

import me.ali.coolenglishmagazine.model.Magazines;
import me.ali.coolenglishmagazine.util.LogHelper;

/**
 * A fragment representing a single Issue detail screen.
 * This fragment is either contained in a {@link RootActivity}
 * in two-pane mode (on tablets) or a {@link IssueDetailActivity}
 * on handsets.
 */
public class IssueDetailFragment extends Fragment {

    private static final String TAG = LogHelper.makeLogTag(ReadAndListenActivity.class);

    /**
     * The magazine issue this fragment is presenting.
     */
    private Magazines.Issue issue;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public IssueDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(IssueDetailActivity.ARG_ROOT_DIRECTORY)) {
            // Load the dummy content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            try {
                issue = Magazines.getIssue(getActivity(), new File(getArguments().getString(IssueDetailActivity.ARG_ROOT_DIRECTORY)));

            } catch (IOException e) {
                // TODO: handle error
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.issue_detail, container, false);

        if (issue != null) {
            final WebView webView = (WebView) rootView.findViewById(R.id.webView);
            webView.setWebViewClient(new WebViewClient() {
                public void onPageFinished(WebView view, String url) {
                    final String command = "javascript:adjustLayout("
                            + 0 // HTML content top margin
                            + ", 0, 0, " // poster height
                            + ContextCompat.getColor(getActivity(), R.color.colorAccent) // accent color
                            + ", 0xc5c5c5, " // text color
                            + ContextCompat.getColor(getActivity(), R.color.colorPrimary) // background color
                            + ", 0xf8f8f8" // new word color
                            + ");";
                    webView.loadUrl(command);
//                    webView.loadUrl("javascript:setInstanceState(" + new JSONArray(Arrays.asList(webViewState)) + ");");
                }
            });
            webView.getSettings().setJavaScriptEnabled(true);
            webView.addJavascriptInterface(webViewJavaScriptInterface, "app");

            final File input = new File(issue.rootDirectory, Magazines.Issue.contentFileName);
            webView.loadUrl(input.toURI().toString());
        }

        return rootView;
    }

    WebViewJavaScriptInterface webViewJavaScriptInterface = new WebViewJavaScriptInterface();

    /**
     * JavaScript Interface. Web code can access methods in here
     * (as long as they have the @JavascriptInterface annotation)
     */
    public class WebViewJavaScriptInterface {
        @JavascriptInterface
        public void onAdjustLayoutComplete() {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    getView().setVisibility(View.VISIBLE);
                }
            });
        }
    }
}
