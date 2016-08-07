package me.ali.coolenglishmagazine;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mikepenz.iconics.view.IconicsTextView;

import java.util.ArrayList;
import java.util.List;

import me.ali.coolenglishmagazine.model.WaitingItems;
import me.ali.coolenglishmagazine.util.Blinker;
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
    public void onPause() {
        super.onPause();
        for (Blinker blinker : adapter.blinkers)
            blinker.stop();
    }

    @Override
    public void onStop() {
        super.onStop();
        finishActionMode();
    }

    ViewPager viewPager;
    TabLayout tabLayout;
    public ViewPagerAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_cool_english_times, container, false);

        mListener.onToolbarCreated((Toolbar) view.findViewById(R.id.toolbar_actionbar), R.string.cool_english_times);

        viewPager = (ViewPager) view.findViewById(R.id.view_pager);
        tabLayout = (TabLayout) view.findViewById(R.id.tab_layout);

        setupViewPager(viewPager);

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
        adapter = new ViewPagerAdapter(getChildFragmentManager());

        adapter.addFragment(0);
        adapter.addFragment(1);

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

        tabLayout.setupWithViewPager(viewPager);

        for (int i = 0; i < 2; i++)
            tabLayout.getTabAt(i).setCustomView(adapter.getTabView(i));

        viewPager.setCurrentItem(1 - currentTabIndex);
        viewPager.setCurrentItem(currentTabIndex);

        Typeface typeface = FontManager.getTypeface(getActivity(), FontManager.UBUNTU_LIGHT);
        FontManager.markAsIconContainer(tabLayout, typeface);
    }

    /**
     * when user long presses an item, action mode is turned on.
     */
    public ActionMode actionMode;

    protected void finishActionMode() {
        if (actionMode != null)
            actionMode.finish();
    }

    class ViewPagerAdapter extends PagerAdapter {
        FragmentManager fragmentManager;

        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();
        private final List<String> mFragmentIconList = new ArrayList<>();
        public final List<Blinker> blinkers = new ArrayList<>();

        public ViewPagerAdapter(FragmentManager manager) {
            fragmentManager = manager;
        }

        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public Fragment instantiateItem(ViewGroup container, int position) {
            Fragment fragment = getItem(position);
            String tag = null;
            switch (position) {
                case 0:
                    tag = "ALARMS_TAB_FRAGMENT";
                    break;
                case 1:
                    tag = "WAITING_LIST_TAB_FRAGMENT";
                    break;
            }
            if (fragmentManager.findFragmentByTag(tag) == null) {
                FragmentTransaction trans = fragmentManager.beginTransaction();
                trans.add(container.getId(), fragment, tag);
                trans.commit();
            }
            return fragment;
        }

        @Override
        public boolean isViewFromObject(View view, Object fragment) {
            return ((Fragment) fragment).getView() == view;
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(int tabIndex) {
            Fragment fragment = null;
            int titleId = 0, iconId = 0;
            String tag;

            switch (tabIndex) {
                case 0:
                    tag = "ALARMS_TAB_FRAGMENT";
                    fragment = fragmentManager.findFragmentByTag(tag);
                    if (fragment == null)
                        fragment = AlarmsTabFragment.newInstance();
                    titleId = R.string.alarms;
                    iconId = R.string.alarms_icon;
                    break;

                case 1:
                    tag = "WAITING_LIST_TAB_FRAGMENT";
                    fragment = fragmentManager.findFragmentByTag(tag);
                    if (fragment == null)
                        fragment = WaitingListFragment.newInstance();
                    titleId = R.string.waiting_list;
                    iconId = R.string.waiting_list_icon;
                    break;
            }

            mFragmentList.add(fragment);
            mFragmentTitleList.add(getResources().getString(titleId));
            mFragmentIconList.add(getResources().getString(iconId));
            blinkers.add(new Blinker());
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }

        public View getTabView(int position) {
            View v = LayoutInflater.from(getActivity()).inflate(R.layout.custom_tab, null);
            ((TextView) v.findViewById(R.id.tab_title)).setText(mFragmentTitleList.get(position));
            ((IconicsTextView) v.findViewById(R.id.tab_icon)).setText(mFragmentIconList.get(position));
            blinkers.get(position).setTabView(v);
            return v;
        }
    }

    /**
     * blinks tab titles if some tabs require user notice.
     *
     * @param filter tab index
     */
    public void updateBlinker(int filter) {
        Context context = getActivity();
        if (context == null)
            return;

        boolean start = false;

        switch (filter) {
            case 0:
                start = ((AlarmsTabFragment) adapter.mFragmentList.get(0)).alarms.size() == 0 && WaitingItems.waitingItems != null && WaitingItems.waitingItems.size() > 0;
                break;
        }

        Blinker blinker = adapter.blinkers.get(filter);
        if (start)
            blinker.start();
        else
            blinker.stop();
    }

}
