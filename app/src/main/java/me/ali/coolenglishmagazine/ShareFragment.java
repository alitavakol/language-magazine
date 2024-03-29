package me.ali.coolenglishmagazine;


import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
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

import java.net.MalformedURLException;
import java.net.URL;

import io.fabric.sdk.android.Fabric;


/**
 * A simple {@link Fragment} subclass.
 */
public class ShareFragment extends DialogFragment {

    public ShareFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_share, container, false);

        final String share_description = getString(R.string.share_description, getString(R.string.app_website));
        final Uri imageUrl = Uri.parse(getString(R.string.app_icon_url));

        Button facebookButton = (Button) v.findViewById(R.id.facebook_button);
        facebookButton.setCompoundDrawables(null,
                new IconicsDrawable(getActivity()).icon(FontAwesome.Icon.faw_facebook_official).sizeDp(48).paddingDp(4).color(Color.parseColor("#3C5899")),
                null, null);
        facebookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FacebookSdk.sdkInitialize(getActivity());

                // https://developers.facebook.com/docs/sharing/android
                final ShareDialog shareDialog = new ShareDialog(getActivity());

                if (ShareDialog.canShow(ShareLinkContent.class)) {
                    ShareLinkContent linkContent = new ShareLinkContent.Builder()
                            .setContentTitle(getString(R.string.share_title))
                            .setContentDescription(share_description)
                            .setContentUrl(Uri.parse(getString(R.string.app_website)))
                            .setImageUrl(imageUrl)
                            .build();

                    shareDialog.show(linkContent);
                }

                dismiss();
            }
        });

        Button twitterButton = (Button) v.findViewById(R.id.twitter_button);
        twitterButton.setCompoundDrawables(null,
                new IconicsDrawable(getActivity()).icon(FontAwesome.Icon.faw_twitter).sizeDp(48).paddingDp(4).color(Color.parseColor("#5EA9DD")),
                null, null);
        twitterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // https://apps.twitter.com/app/12065331/keys
                TwitterAuthConfig authConfig = new TwitterAuthConfig("RkLSPKHaoinx8HbGPgvK4RIa8", "GLhRzP9yUW7dH1Ixyu5k6Zhn2XzmaxdCC7kxtgn14w3bEivbOu");
                Fabric.with(getActivity(), new TwitterCore(authConfig), new TweetComposer());

                try {
                    // https://docs.fabric.io/android/twitter/compose-tweets.html
                    TweetComposer.Builder builder = new TweetComposer.Builder(getActivity())
                            .text(getString(R.string.share_description_twitter))
                            .image(imageUrl)
                            .url(new URL(getString(R.string.app_website)));
                    builder.show();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }

                dismiss();
            }
        });

        Button googlePlusButton = (Button) v.findViewById(R.id.google_plus_button);
        googlePlusButton.setCompoundDrawables(null,
                new IconicsDrawable(getActivity()).icon(FontAwesome.Icon.faw_google_plus).sizeDp(48).paddingDp(4).color(Color.parseColor("#DC4A3D")),
                null, null);
        googlePlusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Launch the Google+ share dialog with attribution to your app.
                // https://developers.google.com/+/mobile/android/share/prefill
                Intent shareIntent = new PlusShare.Builder(getActivity())
                        .setType("text/plain")
                        .setText(share_description)
                        .setContentUrl(Uri.parse(getString(R.string.app_website)))
                        .getIntent();

                try {
                    startActivityForResult(shareIntent, 0);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(getContext(), R.string.activity_not_found, Toast.LENGTH_SHORT).show();
                }

                dismiss();
            }
        });

        Button emailButton = (Button) v.findViewById(R.id.email_button);
        emailButton.setCompoundDrawables(null,
                new IconicsDrawable(getActivity()).icon(FontAwesome.Icon.faw_envelope).sizeDp(48).paddingDp(4).color(Color.parseColor("#eeaa00")),
                null, null);
        emailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_title));
                emailIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.share_title));
                emailIntent.putExtra(Intent.EXTRA_TEXT, share_description);
//                emailIntent.putExtra(Intent.EXTRA_HTML_TEXT, getString(R.string.share_description_html)); // if you are using HTML in your body text

                startActivity(Intent.createChooser(emailIntent, "Share this app via"));

                dismiss();
            }
        });

        Button copyButton = (Button) v.findViewById(R.id.copy_button);
        copyButton.setCompoundDrawables(null,
                new IconicsDrawable(getActivity()).icon(FontAwesome.Icon.faw_clipboard).sizeDp(48).paddingDp(4).color(Color.argb(255, 50, 220, 20)),
                null, null);
        copyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText(getString(R.string.share_title), share_description);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(getActivity(), R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();

                dismiss();
            }
        });

        Button moreButton = (Button) v.findViewById(R.id.more_button);
        moreButton.setCompoundDrawables(null,
                new IconicsDrawable(getActivity()).icon(FontAwesome.Icon.faw_ellipsis_h).sizeDp(32).colorRes(R.color.accent),
                null, null);
        moreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.share_title));
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_title));
                sendIntent.putExtra(Intent.EXTRA_TEXT, share_description);
                sendIntent.setType("text/plain");
                startActivity(sendIntent);

                dismiss();
            }
        });

//        getDialog().setTitle(R.string.share_title);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return v;
    }
}
