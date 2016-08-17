package me.ali.coolenglishmagazine;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.AppBarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.IconicsDrawable;
import com.squareup.picasso.Picasso;

import me.ali.coolenglishmagazine.util.LogHelper;
import me.ali.coolenglishmagazine.widget.ObservableScrollView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ReadmeFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * * Use the {@link ReadmeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReadmeFragment extends Fragment {

    private static final String TAG = LogHelper.makeLogTag(ReadmeFragment.class);

    public static final String FRAGMENT_TAG = ReadmeFragment.class.getName();

    private OnFragmentInteractionListener mListener;

    public ReadmeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment ReadmeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ReadmeFragment newInstance() {
        ReadmeFragment fragment = new ReadmeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            currentCardIndex = savedInstanceState.getInt("currentCardIndex");
        }

        setHasOptionsMenu(true);

        final RootActivity activity = (RootActivity) getActivity();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        preferences.edit().putBoolean("readme_seen", true).apply();
        activity.updateIconBlinkers();
    }

    protected int currentCardIndex = 0;
    protected int cardCount;

    private TextView buttonPrevious, buttonNext;
    protected ObservableScrollView scrollView;
    protected AppBarLayout appBarLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_readme, container, false);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar_actionbar);
        mListener.onToolbarCreated(toolbar, R.string.readme);

        scrollView = (ObservableScrollView) view.findViewById(R.id.scroll_view);
        appBarLayout = (AppBarLayout) view.findViewById(R.id.app_bar);

        final FrameLayout cardContainer = (FrameLayout) view.findViewById(R.id.card_container);
        cardCount = cardContainer.getChildCount();

//        FontManager.markAsIconContainer(cardContainer, FontManager.getTypeface(getActivity(), FontManager.UBUNTU));

        buttonPrevious = (TextView) view.findViewById(R.id.button_previous);
        buttonNext = (TextView) view.findViewById(R.id.button_next);

        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardContainer.getChildAt(currentCardIndex).setVisibility(View.GONE);
                currentCardIndex++;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        cardContainer.getChildAt(currentCardIndex).setVisibility(View.VISIBLE);
                    }
                }, 200);
                updateButtons();
            }
        });

        buttonPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardContainer.getChildAt(currentCardIndex).setVisibility(View.GONE);
                currentCardIndex--;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        cardContainer.getChildAt(currentCardIndex).setVisibility(View.VISIBLE);
                    }
                }, 200);
                updateButtons();
            }
        });

        cardContainer.getChildAt(currentCardIndex).setVisibility(View.VISIBLE);
        updateButtons();

        TypedValue tv = new TypedValue();
        if (getActivity().getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            int actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
            int logoHeight = getResources().getDimensionPixelSize(R.dimen.welcome_image_height);

            int height = (int) (1.3 * actionBarHeight);
            view.findViewById(R.id.logo_container).setPadding(0, height, 0, height);

            scrollView.setPadding(0, logoHeight + (int) (1.4 * actionBarHeight), 0, actionBarHeight);
        }

        Picasso
                .with(getContext())
                .load(R.drawable.readme_background_icon)
                .resize(getResources().getDimensionPixelSize(R.dimen.welcome_image_width), getResources().getDimensionPixelSize(R.dimen.welcome_appbar_height))
                .centerInside()
                .into((ImageView) view.findViewById(R.id.logo));

        return view;
    }

    private void updateButtons() {
        if (currentCardIndex > 0) {
            buttonPrevious.setClickable(true);
            buttonPrevious.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_light));
        } else {
            buttonPrevious.setClickable(false);
            buttonPrevious.setTextColor(Color.GRAY);
        }

        scrollView.fullScroll(View.FOCUS_UP);
        appBarLayout.setExpanded(false);

        if (currentCardIndex < cardCount - 1) {
            buttonNext.setClickable(true);
            buttonNext.setTextColor(ContextCompat.getColor(getContext(), R.color.primary_light));
        } else {
            buttonNext.setClickable(false);
            buttonNext.setTextColor(Color.GRAY);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        /**
         * called when toolbar is created, and container activity can set up navigation drawer.
         *
         * @param toolbar app toolbar
         */
        void onToolbarCreated(Toolbar toolbar, int titleRes);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentCardIndex", currentCardIndex);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.readme_fragment_menu, menu);
        if (isAdded())
            menu.findItem(R.id.action_toggle_language).setIcon(new IconicsDrawable(getContext(), GoogleMaterial.Icon.gmd_language).sizeDp(24).paddingDp(4).colorRes(R.color.md_dark_primary_text));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_toggle_language:
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                String language = preferences.getString("locale", "fa");
                language = language.equals("fa") ? "en" : "fa";
                preferences.edit().putString("locale", language).apply();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
