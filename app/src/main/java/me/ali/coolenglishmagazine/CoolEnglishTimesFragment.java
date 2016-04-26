package me.ali.coolenglishmagazine;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import me.ali.coolenglishmagazine.util.FontManager;
import me.ali.coolenglishmagazine.util.LogHelper;

public class CoolEnglishTimesFragment extends Fragment {

    private static final String TAG = LogHelper.makeLogTag(CoolEnglishTimesFragment.class);

    public static final String FRAGMENT_TAG = CoolEnglishTimesFragment.class.getName();

    /**
     * fragment argument representing initial tab index to show.
     */
    private static final String ARG_TAB_INDEX = "tab_index";

    /**
     * current view pager tab
     */
    private int currentTabIndex;

    public OnFragmentInteractionListener mListener;

    public CoolEnglishTimesFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param initialTabIndex view pager tab index to show on start
     * @return A new instance of fragment GalleryOfIssuesFragment.
     */
    public static CoolEnglishTimesFragment newInstance(int initialTabIndex) {
        CoolEnglishTimesFragment fragment = new CoolEnglishTimesFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_TAB_INDEX, initialTabIndex);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            currentTabIndex = getArguments().getInt(ARG_TAB_INDEX);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        finishActionMode();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_cool_english_times, container, false);

        mListener.onToolbarCreated((Toolbar) view.findViewById(R.id.toolbar_actionbar), R.string.cool_english_times);

        final ViewPager viewPager = (ViewPager) view.findViewById(R.id.view_pager);
        setupViewPager(viewPager);

        TabLayout tabLayout = (TabLayout) view.findViewById(R.id.tab_layout);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(currentTabIndex).select();

        Typeface typeface = FontManager.getTypeface(getActivity(), FontManager.UBUNTU_LIGHT);
        FontManager.markAsIconContainer(tabLayout, typeface);

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

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getChildFragmentManager());

        AlarmsTabFragment alarmsTabFragment = AlarmsTabFragment.newInstance();
        adapter.addFragment(alarmsTabFragment, R.string.alarms);

        WaitingListFragment waitingListFragment = WaitingListFragment.newInstance();
        adapter.addFragment(waitingListFragment, R.string.waiting_list);

        viewPager.setAdapter(adapter);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                finishActionMode();
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    /**
     * when user long presses an item, action mode is turned on.
     */
    public ActionMode actionMode;

    protected void finishActionMode() {
        if (actionMode != null)
            actionMode.finish();
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(Fragment fragment, int titleId) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(getResources().getString(titleId));
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }
    }

}
