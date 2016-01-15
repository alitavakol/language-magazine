package me.ali.coolenglishmagazine;

import android.app.Activity;
import android.graphics.BitmapFactory;
import android.support.design.widget.CollapsingToolbarLayout;
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

import java.io.File;
import java.io.IOException;

import me.ali.coolenglishmagazine.model.MagazineContent;

/**
 * A fragment representing a single Item detail screen.
 * This fragment is either contained in a {@link ItemListActivity}
 * in two-pane mode (on tablets) or a {@link ItemDetailActivity}
 * on handsets.
 */
public class ItemDetailFragment extends Fragment {

    /**
     * The magazine item this fragment is presenting.
     */
    private MagazineContent.Item item;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ItemDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ItemDetailActivity.ARG_ROOT_DIRECTORY)) {
            // Load magazine content specified by the fragment
            // arguments. In a real-world scenario, use a Loader
            // to load content from a content provider.
            try {
                item = MagazineContent.getItem(new File(getArguments().getString(ItemDetailActivity.ARG_ROOT_DIRECTORY)));

            } catch (IOException e) {
                // TODO: handle error
            }
        }
    }

    WebView webView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_item_detail, container, false);

        webView = (WebView) rootView.findViewById(R.id.webView);
        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                final String command = "javascript:adjustLayout("
                        + 0 // HTML content top margin
                        + ", 0, " // poster height
                        + ContextCompat.getColor(getActivity(), R.color.colorAccent) // accent color
                        + ", 0xc5c5c5, " // text color
                        + ContextCompat.getColor(getActivity(), R.color.colorPrimary) // background color
                        + ", 0xf8f8f8" // new word color
                        + ");";
                webView.loadUrl(command);
            }
        });
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebViewJavaScriptInterface(), "app");
        webView.setVerticalScrollBarEnabled(false);

        if (item != null) {
            final File input = new File(item.rootDirectory, MagazineContent.Item.introFileName);
            webView.loadUrl("file://" + input.getAbsolutePath());
        }

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Activity activity = this.getActivity();
        CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
        if (appBarLayout != null) {
//            appBarLayout.setTitle(item.title);
            ((ImageView) appBarLayout.findViewById(R.id.poster)).setImageBitmap(BitmapFactory.decodeFile(new File(item.rootDirectory, item.posterFileName).getAbsolutePath()));
        }
    }

    public class WebViewJavaScriptInterface {
        @JavascriptInterface
        public void dummy() {
        }
    }
}