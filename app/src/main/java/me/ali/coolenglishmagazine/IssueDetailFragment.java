package me.ali.coolenglishmagazine;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;

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
                e.printStackTrace();
            }
        }
    }

    boolean isAttached = false;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        isAttached = true;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        isAttached = false;
    }

    protected WebView webView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_issue_detail, container, false);

        ((ImageView) rootView.findViewById(R.id.hourglass)).setImageDrawable(new IconicsDrawable(getActivity()).icon(GoogleMaterial.Icon.gmd_hourglass_full).sizeDp(72).colorRes(R.color.accent));

        if (issue == null)
            webView = null;

        else {
            webView = (WebView) rootView.findViewById(R.id.webView);
            webView.setWebViewClient(new WebViewClient() {
                public void onPageFinished(WebView view, String url) {
                    if (isAttached) { // when activity is finished/finishing, becomes null
                        final String command = "javascript:adjustLayout({"
                                + "horizontalMargin: " + getResources().getDimension(R.dimen.activity_horizontal_margin)
                                + ", verticalMargin: " + getResources().getDimension(R.dimen.activity_vertical_margin)
                                + ", spacing: " + getResources().getDimension(R.dimen.spacing_normal)
                                + ", accentColor: " + ContextCompat.getColor(getActivity(), R.color.colorAccent) // accent color
                                + ", primaryColor: " + ContextCompat.getColor(getActivity(), R.color.primary) // accent color
                                + ", textColor: " + ContextCompat.getColor(getActivity(), android.R.color.primary_text_light) // text color
                                + ", newWordColor: 0xf8f8f8" // new word color
                                + "});";
                        webView.loadUrl(command);
//                        webView.loadUrl("javascript:restoreInstanceState(" + new JSONArray(Arrays.asList(webViewState)) + ");");
//                        webView.loadUrl("javascript:app.onAdjustLayoutComplete();");
                        webView.loadUrl("javascript:setTimeout(function() { app.onAdjustLayoutComplete(); }, 600);");
                    }
                }
            });
            webView.getSettings().setJavaScriptEnabled(true);
            webView.setBackgroundColor(Color.TRANSPARENT);
            webView.addJavascriptInterface(new WebViewJavaScriptInterface(), "app");

            final File input = new File(issue.rootDirectory, Magazines.Issue.contentFileName);
            webView.loadUrl(input.toURI().toString());
        }

        return rootView;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (webView != null) {
            webView.removeJavascriptInterface("app");
            webView.destroy();
        }
    }

    /**
     * JavaScript Interface. Web code can access methods in here
     * (as long as they have the @JavascriptInterface annotation)
     */
    public class WebViewJavaScriptInterface {
        @SuppressWarnings("unused")
        @JavascriptInterface
        public void onAdjustLayoutComplete() {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    if (isAttached) {
                        ((IssueDetailActivity) getActivity()).setOnScrollViewLayoutChangedListener();
                        final View view = getView();
                        if (view != null) {
                            view.findViewById(R.id.hourglass).setVisibility(View.GONE);
                            view.findViewById(R.id.webView).setVisibility(View.VISIBLE);
                        }
                    }
                }
            });
        }
    }
}
