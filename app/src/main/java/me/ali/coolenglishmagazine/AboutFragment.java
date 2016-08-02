package me.ali.coolenglishmagazine;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.LinearLayout;

import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.mikepenz.aboutlibraries.LibsConfiguration;
import com.mikepenz.aboutlibraries.entity.Library;
import com.mikepenz.aboutlibraries.ui.item.HeaderItem;
import com.mikepenz.aboutlibraries.ui.item.LibraryItem;

import me.ali.coolenglishmagazine.util.LogHelper;

public class AboutFragment extends Fragment {

    private static final String TAG = LogHelper.makeLogTag(AboutFragment.class);

    public static final String FRAGMENT_TAG = AboutFragment.class.getName();

    public OnFragmentInteractionListener mListener;

    public AboutFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment GalleryOfIssuesFragment.
     */
    public static AboutFragment newInstance() {
        return new AboutFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_about, container, false);

        mListener.onToolbarCreated((Toolbar) view.findViewById(R.id.toolbar_actionbar), R.string.about);

        LibsConfiguration.LibsListener libsListener = new LibsConfiguration.LibsListener() {
            @Override
            public void onIconClicked(View v) {
            }

            @Override
            public boolean onLibraryAuthorClicked(View v, Library library) {
                return false;
            }

            @Override
            public boolean onLibraryContentClicked(View v, Library library) {
                return false;
            }

            @Override
            public boolean onLibraryBottomClicked(View v, Library library) {
                return false;
            }

            @Override
            public boolean onExtraClicked(View v, Libs.SpecialButton specialButton) {
                if (specialButton == Libs.SpecialButton.SPECIAL2) { // feedback
                    FragmentManager fm = getChildFragmentManager();
                    FeedbackFragment fragment = new FeedbackFragment();
                    fragment.show(fm, "feedbackFragment");

                } else if (specialButton == Libs.SpecialButton.SPECIAL3) { // share
                    FragmentManager fm = getChildFragmentManager();
                    ShareFragment fragment = new ShareFragment();
                    fragment.show(fm, "shareFragment");

                } else { // changelog
                    FragmentManager fm = getChildFragmentManager();
                    ChangelogFragment fragment = new ChangelogFragment();
                    fragment.show(fm, "changelogFragment");
                }

                return true;
            }

            @Override
            public boolean onIconLongClicked(View v) {
                return false;
            }

            @Override
            public boolean onLibraryAuthorLongClicked(View v, Library library) {
                return false;
            }

            @Override
            public boolean onLibraryContentLongClicked(View v, Library library) {
                return false;
            }

            @Override
            public boolean onLibraryBottomLongClicked(View v, Library library) {
                return false;
            }
        };

        LibsBuilder libsBuilder = new LibsBuilder()
                .withAutoDetect(false)
                .withSortEnabled(false)
                .withLibraries("effortless", "hem")
                .withExcludedLibraries("materialize", "fastadapter", "AboutLibraries", "AndroidIconics", "materialdrawer", "design", "appcompat_v7", "calligraphy")
                .withAboutIconShown(true)
                .withLibsRecyclerViewListener(new LibsConfiguration.LibsRecyclerViewListener() {
                    @Override
                    public void onBindViewHolder(HeaderItem.ViewHolder headerViewHolder) {
                        final int color = ContextCompat.getColor(getActivity(), R.color.primary_light);

                        final Button special1 = (Button) headerViewHolder.itemView.findViewById(com.mikepenz.aboutlibraries.R.id.aboutSpecial1);
                        special1.setTextColor(color);
                        special1.setMinimumHeight(0);
                        special1.setAllCaps(true);
                        LinearLayout.LayoutParams params1 = (LinearLayout.LayoutParams) special1.getLayoutParams();
                        params1.weight = 0.95f;
                        special1.setLayoutParams(params1);

                        final Button special2 = (Button) headerViewHolder.itemView.findViewById(com.mikepenz.aboutlibraries.R.id.aboutSpecial2);
                        special2.setTextColor(color);
                        special2.setMinimumHeight(0);
                        special2.setAllCaps(true);
                        special2.setMinimumWidth(0);
                        LinearLayout.LayoutParams params2 = (LinearLayout.LayoutParams) special2.getLayoutParams();
                        params2.weight = 1.1f;
                        special2.setLayoutParams(params2);

                        final Button special3 = (Button) headerViewHolder.itemView.findViewById(com.mikepenz.aboutlibraries.R.id.aboutSpecial3);
                        special3.setTextColor(color);
                        special3.setMinimumHeight(0);
                        special3.setAllCaps(true);
                        LinearLayout.LayoutParams params3 = (LinearLayout.LayoutParams) special3.getLayoutParams();
                        params3.weight = 0.95f;
                        special3.setLayoutParams(params3);
                    }

                    @Override
                    public void onBindViewHolder(LibraryItem.ViewHolder viewHolder) {
                    }
                })
                .withAboutAppName(getString(R.string.app_name))
                .withListener(libsListener)
                .withActivityTheme(R.style.AppTheme)
                .withAutoDetect(false)
                .withAboutVersionShownName(true);

        getChildFragmentManager().beginTransaction()
                .replace(R.id.about_container, libsBuilder.supportFragment())
                .commit();

        return view;
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

    protected void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getActivity().getWindow().getDecorView().getWindowToken(), 0);
    }

    @Override
    public void onStop() {
        super.onStop();
        hideKeyboard();
    }
}
