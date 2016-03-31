package me.ali.coolenglishmagazine;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import me.ali.coolenglishmagazine.util.FontManager;


/**
 * A simple {@link Fragment} subclass.
 */
public class AppInfoFragment extends Fragment {

    public AppInfoFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final FragmentActivity context = getActivity();

        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_app_info, container, false);

        TextView titleTextView = (TextView) view.findViewById(R.id.title);
        titleTextView.setTypeface(FontManager.getTypeface(context, FontManager.FOFER));
        titleTextView.setTextColor(ContextCompat.getColor(context, R.color.accent));

        final ImageView logoImageView = (ImageView) view.findViewById(R.id.logo);
        logoImageView.setImageDrawable(getResources().getDrawableForDensity(R.mipmap.ic_launcher, DisplayMetrics.DENSITY_XXXHIGH));
//        logoImageView.setImageDrawable(new IconicsDrawable(context).icon(FontAwesome.Icon.faw_sun_o).sizeDp(72).color(Color.LTGRAY));
//        logoImageView.setColorFilter(Color.YELLOW);

        ((TextView) view.findViewById(R.id.versionTextView)).setText(BuildConfig.VERSION_NAME);

        return view;
    }

}