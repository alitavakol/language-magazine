package me.ali.coolenglishmagazine;


import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.iconics.IconicsDrawable;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import me.ali.coolenglishmagazine.util.NetworkHelper;


/**
 * A simple {@link Fragment} subclass.
 */
public class FeedbackFragment extends DialogFragment {

    public FeedbackFragment() {
        // Required empty public constructor
    }

    @Override
    public void onStop() {
        super.onStop();
        if (requestQueue != null) {
            requestQueue.cancelAll(this);
            requestQueue = null;
        }
    }

    private RequestQueue requestQueue;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final Context context = getActivity();

        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_feedback, container, false);

        final ImageButton sendButton = (ImageButton) view.findViewById(R.id.send_button);
        sendButton.setImageDrawable(new IconicsDrawable(context).icon(FontAwesome.Icon.faw_paper_plane).sizeDp(48).paddingDp(4).colorRes(R.color.accent));
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

                final EditText editText = (EditText) view.findViewById(R.id.edit_text);
                final String message = editText.getText().toString();
                if (message.length() == 0) {
                    Toast.makeText(context, R.string.error_empty_message, Toast.LENGTH_SHORT).show();
                    return;

                } else if (message.equals(getString(R.string.error_empty_message))) { // unlock pro version
                    preferences.edit().putBoolean("upgrade_required", true).apply();
                    return;
                }

                if (requestQueue == null)
                    requestQueue = Volley.newRequestQueue(context);

                Toast.makeText(context, R.string.not_available, Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.visitAppStore).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final FragmentActivity activity = getActivity();
                try {
                    // open marker to rate this app
                    if (BuildConfig.MARKET_APPLICATION_ID.equals("com.farsitel.bazaar")) {
                        Intent intent = new Intent(Intent.ACTION_EDIT);
                        intent.setData(Uri.parse("bazaar://details?id=" + activity.getPackageName()));
                        intent.setPackage("com.farsitel.bazaar");
                        startActivity(intent);

                    } else if (BuildConfig.MARKET_APPLICATION_ID.equals("ir.mservices.market")) {
                        String url = "myket://comment?id=" + activity.getPackageName();
                        Intent intent = new Intent();
                        intent.setAction(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        startActivity(intent);
                    }

                    PreferenceManager.getDefaultSharedPreferences(activity)
                            .edit()
                            .putBoolean("rate_app_shown", true)
                            .apply();
                    dismiss();

                } catch (ActivityNotFoundException e) {
                    Toast.makeText(activity, R.string.app_store_not_found, Toast.LENGTH_SHORT).show();
                }
            }
        });

//        getDialog().setTitle(R.string.feedback_title);
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return view;
    }

}
