package me.ali.coolenglishmagazine;


import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;

import me.ali.coolenglishmagazine.util.Identification;


/**
 * A simple {@link Fragment} subclass.
 */
public class GemFragment extends DialogFragment {

    public GemFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, R.style.DialogTheme);
    }

    Button buttonThrow, buttonShare;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_gem, container, false);

        buttonThrow = (Button) view.findViewById(R.id.buttonThrow);
        buttonThrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Context context = getContext();
                ShareFragment.openShareSelector(context, getString(R.string.throw_share_title), Identification.getGemUrl(context));

                disableButtons((Button) v);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            dismiss();
                        } catch (Exception e) {
                        }
                    }
                }, 2000);
            }
        });

        buttonShare = (Button) view.findViewById(R.id.buttonShare);
        buttonShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShareFragment.openShareSelector(getContext(), getString(R.string.share_title), getString(R.string.share_description_short, getString(R.string.app_website)));

                disableButtons((Button) v);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            dismiss();
                        } catch (Exception e) {
                        }
                    }
                }, 2000);
            }
        });

        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        return view;
    }

    public void disableButtons(Button clicked) {
        clicked.setText(R.string.wait);
        clicked.setTextColor(getResources().getColor(R.color.linkColorDisabled));

        buttonShare.setClickable(false);
        buttonThrow.setClickable(false);
        buttonShare.setEnabled(false);
        buttonThrow.setEnabled(false);
    }

}
