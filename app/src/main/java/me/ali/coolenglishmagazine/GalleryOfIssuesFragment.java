package me.ali.coolenglishmagazine;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
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
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.mikepenz.iconics.view.IconicsTextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import me.ali.coolenglishmagazine.model.Magazines;
import me.ali.coolenglishmagazine.util.Blinker;
import me.ali.coolenglishmagazine.util.FileHelper;
import me.ali.coolenglishmagazine.util.FontManager;
import me.ali.coolenglishmagazine.util.InputStreamVolleyRequest;
import me.ali.coolenglishmagazine.util.LogHelper;
import me.ali.coolenglishmagazine.util.NetworkHelper;
import me.ali.coolenglishmagazine.util.ZipHelper;

public class GalleryOfIssuesFragment extends Fragment {

    private static final String TAG = LogHelper.makeLogTag(GalleryOfIssuesFragment.class);

    public static final String FRAGMENT_TAG = GalleryOfIssuesFragment.class.getName();

    /**
     * fragment argument representing initial tab index to show.
     */
    private static final String ARG_TAB_INDEX = "tab_index";

    public Magazines magazines;

    /**
     * current view pager tab
     */
    private int currentTabIndex;

    public OnFragmentInteractionListener mListener;

    public GalleryOfIssuesFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param initialTabIndex view pager tab index to show on start
     * @return A new instance of fragment GalleryOfIssuesFragment.
     */
    public static GalleryOfIssuesFragment newInstance(int initialTabIndex) {
        GalleryOfIssuesFragment fragment = new GalleryOfIssuesFragment();
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

        Context context = getActivity().getApplicationContext();

        magazines = new Magazines();
        magazines.loadIssues(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_gallery_of_issues, container, false);

        mListener.onToolbarCreated((Toolbar) view.findViewById(R.id.toolbar_actionbar), R.string.gallery_of_issues);

        viewPager = (ViewPager) view.findViewById(R.id.view_pager);
        tabLayout = (TabLayout) view.findViewById(R.id.tab_layout);

        setupViewPager();

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
    public void onStart() {
        super.onStart();
        final FragmentActivity activity = getActivity();
        PreferenceManager.getDefaultSharedPreferences(activity).registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        final FragmentActivity activity = getActivity();
        cancelSync(activity, null);
        finishActionMode();
        PreferenceManager.getDefaultSharedPreferences(activity).unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        for (Blinker blinker : adapter.blinkers)
            blinker.stop();
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

        /**
         * called when user clicks an issue item from the list of issues.
         *
         * @param issue selected issue
         */
        void onIssueSelected(Magazines.Issue issue);
    }

    public ViewPager viewPager;
    protected TabLayout tabLayout;
    public ViewPagerAdapter adapter;

    private void setupViewPager() {
        adapter = new ViewPagerAdapter(getChildFragmentManager());

        for (int i = 0; i < 3; i++)
            adapter.addFragment(i);

        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(adapter.getCount() - 1);

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

        for (int i = 0; i < 3; i++)
            tabLayout.getTabAt(i).setCustomView(adapter.getTabView(i));

        viewPager.setCurrentItem(2 - currentTabIndex);
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

        public final List<IssuesTabFragment> mFragmentList = new ArrayList<>();
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
            final String tag = "ISSUES_TAB_" + position;
            if (fragmentManager.findFragmentByTag(tag) == null) {
                FragmentTransaction trans = fragmentManager.beginTransaction();
                trans.add(container.getId(), fragment, tag);
                trans.commit();
            }
            return fragment;
        }

//        @Override
//        public void destroyItem(ViewGroup container, int position, Object object) {
//            FragmentTransaction trans = fragmentManager.beginTransaction();
//            trans.remove(mFragmentList.remove(position));
//            trans.commit();
//        }

        @Override
        public boolean isViewFromObject(View view, Object fragment) {
            return ((Fragment) fragment).getView() == view;
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        public void addFragment(int tabIndex) {
            IssuesTabFragment fragment = (IssuesTabFragment) fragmentManager.findFragmentByTag("ISSUES_TAB_" + tabIndex);
            if (fragment == null)
                fragment = IssuesTabFragment.newInstance(tabIndex);
            mFragmentList.add(fragment);
            mFragmentTitleList.add(getResources().getString(getResources().obtainTypedArray(R.array.issue_list_tab_titles).getResourceId(tabIndex, 0)));
            mFragmentIconList.add(getResources().getString(getResources().obtainTypedArray(R.array.issue_list_tab_icons).getResourceId(tabIndex, 0)));
            blinkers.add(new Blinker());
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }

        public View getTabView(int position) {
            View v = LayoutInflater.from(getActivity()).inflate(R.layout.custom_tab, null);
            ((TextView) v.findViewById(R.id.tab_title)).setText(mFragmentTitleList.get(position));
            final IconicsTextView iconicsTextView = (IconicsTextView) v.findViewById(R.id.tab_icon);
            iconicsTextView.setText(mFragmentIconList.get(position));
            blinkers.get(position).setBlinkingView(iconicsTextView);
            return v;
        }
    }

    /**
     * issue number of the first missing issue
     */
    int firstMissingIssueNumber;

    /**
     * do not perform sync when already in a sync job
     */
    boolean syncing = false;

    /**
     * an snack bar which enables user to cancel syncing
     */
    Snackbar snackbar;

    /**
     * volley network request queue
     */
    RequestQueue requestQueue;

    /**
     * saves received data into a temporary zip file, extracts it.
     * then if latest available issue number has changed, it
     * invokes {@link #syncAvailableIssuesList} sync process again.
     */
    private class UnzipOperation extends AsyncTask<Object, Void, Boolean> {
        IssuesTabFragment.IssuesRecyclerViewAdapter adapter;
        Context context;

        @Override
        protected Boolean doInBackground(Object... obj) {
            context = (Context) obj[0];
            byte[] response = (byte[]) obj[1];
            adapter = (IssuesTabFragment.IssuesRecyclerViewAdapter) obj[2];

            try {
                final File cacheDir = context.getExternalCacheDir();
                final File zipFile = File.createTempFile("issues-preview", ".zip", cacheDir);

                FileOutputStream f = new FileOutputStream(zipFile);
                f.write(response, 0, response.length);
                f.close();

                ZipHelper.unzip(zipFile, context.getExternalFilesDir(null));

                FileHelper.delete(zipFile);
                return true;

            } catch (IOException e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                final int firstMissingIssueNumber = findFirstMissingIssueNumber(context);

                if (firstMissingIssueNumber > GalleryOfIssuesFragment.this.firstMissingIssueNumber) {
                    magazines.loadIssues(context);

                    GalleryOfIssuesFragment.this.firstMissingIssueNumber = firstMissingIssueNumber;

                    // rerun sync to see if there are more, because number of local issues has changed
                    // do it in a moment later
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (snackbar != null) // if user has not cancelled sync
                                syncAvailableIssuesList(context, firstMissingIssueNumber, adapter);
                        }
                    }, 1000);

                } else {
                    final FragmentActivity activity = getActivity();
                    if (activity != null)
                        Toast.makeText(activity, R.string.sync_complete, Toast.LENGTH_SHORT).show();
                    success = false; // force jump into the following if block
//                    updateInAppBillingData();
                }

            } else {
                final FragmentActivity activity = getActivity();
                if (activity != null)
                    Toast.makeText(activity, R.string.unzip_error, Toast.LENGTH_SHORT).show();
            }

            if (!success) {
                final FragmentActivity activity = getActivity();
                if (activity != null)
                    cancelSync(activity, adapter);
            }
        }
    }

    /**
     * gets list of available issues from server.
     */
    void syncAvailableIssuesList(final Context context, int firstMissingIssueNumber, final IssuesTabFragment.IssuesRecyclerViewAdapter adapter) {
        final Context app = context.getApplicationContext();

        if (firstMissingIssueNumber == -1) {
            if (syncing)
                return;

            this.firstMissingIssueNumber = 1;
            this.firstMissingIssueNumber = findFirstMissingIssueNumber(context);
            firstMissingIssueNumber = this.firstMissingIssueNumber;
        }

        syncing = true;

        // Instantiate the RequestQueue.
        if (requestQueue == null)
            requestQueue = Volley.newRequestQueue(context);

        if (NetworkHelper.isOnline(context)) {
            if (snackbar == null) {
                snackbar = Snackbar
                        .make(getView(), R.string.syncing, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.cancel_syncing, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                cancelSync(getActivity(), adapter);
                            }
                        }).setActionTextColor(getResources().getColor(R.color.primary_light));
                snackbar.show();
            }

            final Uri uri = Uri.parse(PreferenceManager.getDefaultSharedPreferences(context).getString("server_address", getResources().getString(R.string.pref_default_server_address)));
            // http://docs.oracle.com/javase/tutorial/networking/urls/urlInfo.html
            final String url = uri.toString() + "/api/issues?min_issue_number=" + firstMissingIssueNumber + "&app_version=" + BuildConfig.VERSION_CODE;

            // Request a string response from the provided URL.
            InputStreamVolleyRequest request = new InputStreamVolleyRequest(Request.Method.GET, url, new Response.Listener<byte[]>() {
                @Override
                public void onResponse(byte[] response) {
                    if (response.length > 0) {
                        new UnzipOperation().execute(app, response, adapter);

                    } else {
                        // received success with status code 204 (no content)
                        Context context = getActivity();
                        if (context != null) {
                            cancelSync(context, adapter);
                            Toast.makeText(context, R.string.update_success, Toast.LENGTH_SHORT).show();
                        }
//                        updateInAppBillingData();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    if (error.networkResponse != null && error.networkResponse.statusCode == 426) { // 426 Upgrade Required
                        NetworkHelper.showUpgradeDialog(getActivity());

                    } else {
                        Context context = getActivity();
                        if (context != null)
                            Toast.makeText(context, R.string.network_error, Toast.LENGTH_SHORT).show();
                    }

                    Context context = getActivity();
                    if (context != null)
                        cancelSync(context, adapter);
                }
            }, null);

            request.setTag(GalleryOfIssuesFragment.this)
                    .setRetryPolicy(new DefaultRetryPolicy(30000,
                            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            // Add the request to the RequestQueue.
            requestQueue.add(request);

        } else {
            Context activity = getActivity();
            if (activity != null) {
                cancelSync(activity, adapter);
                Toast.makeText(activity, R.string.check_connection, Toast.LENGTH_SHORT).show();
            }
        }
    }

    protected void cancelSync(Context context, IssuesTabFragment.IssuesRecyclerViewAdapter adapter) {
        if (requestQueue != null) {
            requestQueue.cancelAll(GalleryOfIssuesFragment.this);
            requestQueue = null;
        }

        if (snackbar != null) {
            snackbar.dismiss();
            snackbar = null;
        }

        if (adapter != null)
            adapter.preNotifyDataSetChanged(false, null);

        syncing = false;
    }

    /**
     * in order to ask server for new issues, we need to know minimum issue number to request for.
     * in response to each request, server sends compressed file issues-preview-{10k}-{10k+9}.zip,
     * which also contains issue with requested minimum issue number.
     *
     * @return issue number of the first missing magazine
     */
    protected int findFirstMissingIssueNumber(Context context) {
        int i = Math.max(1, firstMissingIssueNumber); // count up from last found value
        while (new File(context.getExternalFilesDir(null), Integer.toString(i)).exists())
            i++;
        return i;
    }

    int latestAvailableIssueNumberOnServer;

    /**
     * get latest available issue's ID from server
     *
     * @param context activity context
     */
    void fetchLatestIssueNumber(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

//        if (!NetworkHelper.isOnline(context)) {
//            latestAvailableIssueNumberOnServer = preferences.getInt("latestAvailableIssueNumberOnServer", 0);
//            updateBlinker(IssuesTabFragment.AVAILABLE_ISSUES);
//            return;
//        }

        final long currentTime = System.currentTimeMillis();
        long lastUpdateCheck = preferences.getLong("last_update_check", currentTime - 365L * 24L * 3600L * 1000L);
        if (currentTime - lastUpdateCheck < 3L * 24L * 3600L * 1000L) { // don't check if last update occurred less than 3 days ago
            latestAvailableIssueNumberOnServer = PreferenceManager.getDefaultSharedPreferences(context).getInt("latestAvailableIssueNumberOnServer", 0);
            updateBlinker(IssuesTabFragment.AVAILABLE_ISSUES);
            return;
        }

        if (requestQueue == null)
            requestQueue = Volley.newRequestQueue(context);

        final Uri uri = Uri.parse(preferences.getString("server_address", getResources().getString(R.string.pref_default_server_address)));
        final String url = uri.toString() + "/api/issues/latest?app_version=" + BuildConfig.VERSION_CODE;

        InputStreamVolleyRequest request = new InputStreamVolleyRequest(Request.Method.GET, url, new Response.Listener<byte[]>() {
            @Override
            public void onResponse(byte[] response) {
                latestAvailableIssueNumberOnServer = Integer.parseInt(new String(response));
                Activity activity = getActivity();
                if (activity != null)
                    PreferenceManager.getDefaultSharedPreferences(activity).edit()
                            .putInt("latestAvailableIssueNumberOnServer", latestAvailableIssueNumberOnServer)
                            .putLong("last_update_check", currentTime)
                            .apply();
                updateBlinker(IssuesTabFragment.AVAILABLE_ISSUES);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Activity activity = getActivity();
                if (activity != null) {
                    latestAvailableIssueNumberOnServer = PreferenceManager.getDefaultSharedPreferences(activity).getInt("latestAvailableIssueNumberOnServer", 0);
                    updateBlinker(IssuesTabFragment.AVAILABLE_ISSUES);
                }
            }
        }, null);

        request.setTag(GalleryOfIssuesFragment.this);

        // Add the request to the RequestQueue.
        requestQueue.add(request);
    }

    protected SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
            if (key.equals("new_saved_issues"))
                updateBlinker(IssuesTabFragment.SAVED_ISSUES);
        }
    };

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
            case IssuesTabFragment.AVAILABLE_ISSUES:
                if (Magazines.file2issue.size() == 0) // blink if no issue previews are fetched
                    start = true;
                else {
                    int latestSaved = findFirstMissingIssueNumber(context) - 1;
                    if (latestAvailableIssueNumberOnServer > latestSaved) // blink if latest issue number on server is greater than what app knows
                        start = true;
                }
                break;

            case IssuesTabFragment.SAVED_ISSUES: // blinks saved issues tab, if unseen issues are there
                int newSavedIssuesCount = PreferenceManager.getDefaultSharedPreferences(context).getStringSet("new_saved_issues", new HashSet<String>(0)).size();
                start = newSavedIssuesCount > 0;
                break;
        }

        Blinker blinker = adapter.blinkers.get(filter);
        if (start) {
            blinker.start();

            if (filter == IssuesTabFragment.AVAILABLE_ISSUES) {
                RootActivity activity = (RootActivity) getActivity();
                if (!activity.newIssuesAvailableWarningShown) {
                    Snackbar
                            .make(getView(), R.string.new_issues_available, Snackbar.LENGTH_LONG)
                            .setAction(R.string.refresh, new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    adapter.mFragmentList.get(IssuesTabFragment.AVAILABLE_ISSUES).onRefresh();
                                }
                            }).setActionTextColor(getResources().getColor(R.color.primary_light))
                            .show();

                    activity.newIssuesAvailableWarningShown = true;
                }
            }

        } else {
            blinker.stop();
        }
    }

}
