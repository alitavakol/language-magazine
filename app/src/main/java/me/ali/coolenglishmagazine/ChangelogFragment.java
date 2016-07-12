package me.ali.coolenglishmagazine;


import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChangelogFragment extends DialogFragment {

    public ChangelogFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_changelog, container, false);

        TextView changeLogTextView = (TextView) view.findViewById(R.id.changeLog);
        Spanned stringSpanned = Html.fromHtml(getString(R.string.aboutLibraries_description_special1_text), null, null);
        changeLogTextView.setText(stringSpanned, TextView.BufferType.SPANNABLE);

        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return view;
    }

}
