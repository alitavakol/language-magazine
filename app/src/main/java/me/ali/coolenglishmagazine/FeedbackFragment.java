package me.ali.coolenglishmagazine;


import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
public class FeedbackFragment extends Fragment {

    public FeedbackFragment() {
        // Required empty public constructor
    }

    @Override
    public void onStop() {
        super.onStop();
        if (requestQueue != null)
            requestQueue.cancelAll(this);
    }

    private RequestQueue requestQueue;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final Context context = getActivity();

        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_feedback, container, false);

        final ImageButton sendButton = (ImageButton) view.findViewById(R.id.send_button);
        sendButton.setImageDrawable(new IconicsDrawable(context).icon(FontAwesome.Icon.faw_paper_plane).sizeDp(48).colorRes(R.color.accent));
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText editText = (EditText) view.findViewById(R.id.edit_text);
                final String message = editText.getText().toString();
                if (message.length() == 0) {
                    Toast.makeText(context, R.string.error_empty_message, Toast.LENGTH_SHORT).show();
                    return;
                }

                if (requestQueue == null)
                    requestQueue = Volley.newRequestQueue(context);

                if (NetworkHelper.isOnline(context)) {
                    sendButton.setClickable(false);

                    final RadioGroup radioGroup = (RadioGroup) view.findViewById(R.id.radio_group);
                    final String category = ((RadioButton) view.findViewById(radioGroup.getCheckedRadioButtonId())).getText().toString();

                    final Uri uri = Uri.parse(PreferenceManager.getDefaultSharedPreferences(context).getString("server_address", getResources().getString(R.string.pref_default_server_address)));
                    final String url = uri.toString() + "/api/feedbacks.json";

                    Map<String, String> params = new HashMap<>();
                    params.put("message", message);
                    params.put("category", category);

                    JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, new JSONObject(params), new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject o) {
                            editText.setText("");
                            sendButton.setClickable(true);
                            Toast.makeText(context, R.string.thanks, Toast.LENGTH_SHORT).show();
                        }
                    }, new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            sendButton.setClickable(true);
                            Toast.makeText(context, R.string.network_error, Toast.LENGTH_SHORT).show();
                        }
                    });

                    request.setTag(this);
                    requestQueue.add(request);

                } else {
                    requestQueue.cancelAll(this);
                    Toast.makeText(context, R.string.check_connection, Toast.LENGTH_SHORT).show();
                }
            }
        });

        return view;
    }

}
