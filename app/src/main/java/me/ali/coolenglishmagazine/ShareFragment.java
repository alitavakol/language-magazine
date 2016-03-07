package me.ali.coolenglishmagazine;


import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.facebook.share.model.ShareLinkContent;
import com.facebook.share.widget.ShareDialog;
import com.google.android.gms.plus.PlusShare;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.iconics.IconicsDrawable;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.tweetcomposer.TweetComposer;

import io.fabric.sdk.android.Fabric;


/**
 * A simple {@link Fragment} subclass.
 */
public class ShareFragment extends Fragment implements View.OnClickListener {

    public ShareFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FacebookSdk.sdkInitialize(getActivity());

        // https://apps.twitter.com/app/12065331/keys
        TwitterAuthConfig authConfig = new TwitterAuthConfig("RkLSPKHaoinx8HbGPgvK4RIa8", "GLhRzP9yUW7dH1Ixyu5k6Zhn2XzmaxdCC7kxtgn14w3bEivbOu");
        Fabric.with(getActivity(), new TwitterCore(authConfig), new TweetComposer());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_share, container, false);

        Button facebookButton = (Button) v.findViewById(R.id.facebook_button);
        facebookButton.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                new IconicsDrawable(getActivity()).icon(FontAwesome.Icon.faw_facebook_official).sizeDp(48).color(Color.parseColor("#3C5899")),
                null, null);
        facebookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // https://developers.facebook.com/docs/sharing/android
                final ShareDialog shareDialog = new ShareDialog(getActivity());

                if (ShareDialog.canShow(ShareLinkContent.class)) {
                    ShareLinkContent linkContent = new ShareLinkContent.Builder()
                            .setContentTitle("The Cool English Magazine Android App")
                            .setContentDescription(
                                    "This application helps you learn English language, in a cool and easy way. And of course with fun!")
                            .setContentUrl(Uri.parse("http://alitavakol.me"))
                            .build();

                    shareDialog.show(linkContent);
                }
            }
        });

        Button twitterButton = (Button) v.findViewById(R.id.twitter_button);
        twitterButton.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                new IconicsDrawable(getActivity()).icon(FontAwesome.Icon.faw_twitter).sizeDp(48).color(Color.parseColor("#5EA9DD")),
                null, null);
        twitterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // https://docs.fabric.io/android/twitter/compose-tweets.html
                TweetComposer.Builder builder = new TweetComposer.Builder(getActivity())
//                        .image(myImageUri)
                        .text("just setting up my Fabric.");
                builder.show();
            }
        });

        Button googlePlusButton = (Button) v.findViewById(R.id.google_plus_button);
        googlePlusButton.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                new IconicsDrawable(getActivity()).icon(FontAwesome.Icon.faw_google_plus).sizeDp(48).color(Color.parseColor("#DC4A3D")),
                null, null);
        googlePlusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch the Google+ share dialog with attribution to your app.
                // https://developers.google.com/+/mobile/android/share/prefill
                Intent shareIntent = new PlusShare.Builder(getActivity())
                        .setType("text/plain")
                        .setText("Just tried this new restaurant! #nomnomnom #myappname")
                        .setContentUrl(Uri.parse("https://developers.google.com/+/web/snippet/examples/restaurant"))
                        .getIntent();

                startActivityForResult(shareIntent, 0);
            }
        });

        Button emailButton = (Button) v.findViewById(R.id.email_button);
        emailButton.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                new IconicsDrawable(getActivity()).icon(FontAwesome.Icon.faw_envelope).sizeDp(48).color(Color.parseColor("#eeaa00")),
                null, null);
        emailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "subject");
                emailIntent.putExtra(Intent.EXTRA_TEXT, "body");
                emailIntent.putExtra(Intent.EXTRA_HTML_TEXT, "body"); // if you are using HTML in your body text

                startActivity(Intent.createChooser(emailIntent, "Share this app via"));
            }
        });

        Button copyButton = (Button) v.findViewById(R.id.copy_button);
        copyButton.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                new IconicsDrawable(getActivity()).icon(FontAwesome.Icon.faw_clipboard).sizeDp(48).color(Color.parseColor("#22ff77")),
                null, null);
        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("label", "text");
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getActivity(), "Copied to clipboard!", Toast.LENGTH_SHORT).show();
            }
        });

        Button moreButton = (Button) v.findViewById(R.id.more_button);
        moreButton.setCompoundDrawablesRelativeWithIntrinsicBounds(null,
                new IconicsDrawable(getActivity()).icon(FontAwesome.Icon.faw_ellipsis_h).sizeDp(36).color(Color.LTGRAY),
                null, null);
        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT,
                        "Hey check out my app at: https://play.google.com/store/apps/details?id=com.google.android.apps.plus");
                sendIntent.setType("text/plain");
                startActivity(sendIntent);
            }
        });

        return v;
    }

    @Override
    public void onClick(View v) {
        String application = null;

        switch (v.getId()) {
            case R.id.facebook_button:
                application = "com.facebook.katana";
                break;
            case R.id.twitter_button:
                application = "com.twitter.android";
                break;
        }

        if (application != null) {
            Context context = getActivity();

            Intent intent = context.getPackageManager().getLaunchIntentForPackage(application);
            if (intent != null) {
                // The application exists
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.setPackage(application);

                shareIntent.putExtra(android.content.Intent.EXTRA_TITLE, "title");
                shareIntent.putExtra(Intent.EXTRA_TEXT, "description");

                // Start the specific social application
                context.startActivity(shareIntent);

            } else {
                // The application does not exist
                Toast.makeText(context, "Application is not installed.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
