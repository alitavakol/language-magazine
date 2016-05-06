package me.ali.coolenglishmagazine;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
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
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.ali.coolenglishmagazine.model.Magazines;
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

        magazines = new Magazines();
        magazines.loadIssues(getActivity());

        firstMissingIssueNumber = findFirstMissingIssueNumber();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_gallery_of_issues, container, false);

        mListener.onToolbarCreated((Toolbar) view.findViewById(R.id.toolbar_actionbar), R.string.gallery_of_issues);

        viewPager = (ViewPager) view.findViewById(R.id.view_pager);
        setupViewPager();

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
    public void onStop() {
        super.onStop();
        cancelSync(getActivity(), null);
        finishActionMode();
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

    protected ViewPager viewPager;

    private void setupViewPager() {
        final ViewPagerAdapter adapter = new ViewPagerAdapter(getChildFragmentManager());

        for (int i = 0; i < 3; i++) {
            IssuesTabFragment fragment = IssuesTabFragment.newInstance(i);
            adapter.addFragment(fragment, getResources().obtainTypedArray(R.array.issue_list_tab_titles).getResourceId(i, 0));
        }

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
        public final List<IssuesTabFragment> mFragmentList = new ArrayList<>();
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

        public void addFragment(IssuesTabFragment fragment, int titleId) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(getResources().getString(titleId));
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
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
    RequestQueue requestQueue = null;

    private class UnzipOperation extends AsyncTask<Object, Void, Boolean> {
        IssuesTabFragment.IssuesRecyclerViewAdapter adapter;

        @Override
        protected Boolean doInBackground(Object... obj) {
            byte[] response = (byte[]) obj[0];
            adapter = (IssuesTabFragment.IssuesRecyclerViewAdapter) obj[1];

            try {
                final File cacheDir = getContext().getExternalCacheDir();
                final File zipFile = File.createTempFile("issues-preview", ".zip", cacheDir);

                FileOutputStream f = new FileOutputStream(zipFile);
                f.write(response, 0, response.length);
                f.close();

                ZipHelper.unzip(zipFile, getContext().getExternalFilesDir(null));

                FileHelper.delete(zipFile);
                return true;

            } catch (IOException e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                final int firstMissingIssueNumber = findFirstMissingIssueNumber();

                if (firstMissingIssueNumber > GalleryOfIssuesFragment.this.firstMissingIssueNumber) {
                    magazines.loadIssues(getContext());

                    GalleryOfIssuesFragment.this.firstMissingIssueNumber = firstMissingIssueNumber;

                    // rerun sync to see if there are more, because number of local issues has changed
                    if (firstMissingIssueNumber % CHUNK_SIZE == 0) { // if first missing issue number is not a multiple of CHUNK_SIZE, then we are already up to date.
                        // do it in a moment later
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (snackbar != null) // if user has not cancelled sync
                                    syncAvailableIssuesList(firstMissingIssueNumber, adapter);
                            }
                        }, 1000);

                    } else {
                        Toast.makeText(getActivity(), R.string.sync_complete, Toast.LENGTH_SHORT).show();
                        success = false; // force jump into the following if block
                    }

                } else {
                    Toast.makeText(getActivity(), R.string.sync_complete, Toast.LENGTH_SHORT).show();
                    success = false; // force jump into the following if block
                }

            } else {
                Toast.makeText(getActivity(), R.string.unzip_error, Toast.LENGTH_SHORT).show();
            }

            if (!success)
                cancelSync(getActivity(), adapter);
        }
    }

    protected static final int CHUNK_SIZE = 3;

    /**
     * gets list of available issues from server.
     */
    void syncAvailableIssuesList(int firstMissingIssueNumber, final IssuesTabFragment.IssuesRecyclerViewAdapter adapter) {
        if (firstMissingIssueNumber == -1) {
            if (syncing)
                return;

            firstMissingIssueNumber = this.firstMissingIssueNumber;
        }

        final Activity context = getActivity();
        syncing = true;

        // Instantiate the RequestQueue.
        if (requestQueue == null)
            requestQueue = Volley.newRequestQueue(context);

        if (NetworkHelper.isOnline(context)) {
            snackbar = Snackbar
                    .make(getView(), R.string.syncing, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.cancel_syncing, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            cancelSync(getActivity(), adapter);
                        }
                    }).setActionTextColor(getResources().getColor(R.color.primary_light));
            snackbar.show();

            final Uri uri = Uri.parse(PreferenceManager.getDefaultSharedPreferences(context).getString("server_address", getResources().getString(R.string.pref_default_server_address)));
            // http://docs.oracle.com/javase/tutorial/networking/urls/urlInfo.html
            final String url = uri.toString() + "/api/issues?min_issue_number=" + firstMissingIssueNumber;

            // Request a string response from the provided URL.
            InputStreamVolleyRequest request = new InputStreamVolleyRequest(Request.Method.GET, url, new Response.Listener<byte[]>() {
                @Override
                public void onResponse(byte[] response) {
                    if (response.length > 0) {
                        new UnzipOperation().execute(response, adapter);

                    } else {
                        // received success with status code 204 (no content)
                        cancelSync(context, adapter);
                        Toast.makeText(context, R.string.update_success, Toast.LENGTH_SHORT).show();
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    cancelSync(context, adapter);
                    Toast.makeText(context, R.string.network_error, Toast.LENGTH_SHORT).show();
                }
            }, null);

            request.setTag(this);

            // Add the request to the RequestQueue.
            requestQueue.add(request);

        } else {
            cancelSync(context, adapter);
            Toast.makeText(context, R.string.check_connection, Toast.LENGTH_SHORT).show();
        }
    }

    protected void cancelSync(Context context, IssuesTabFragment.IssuesRecyclerViewAdapter adapter) {
        if (requestQueue != null) {
            requestQueue.cancelAll(context);
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
    protected int findFirstMissingIssueNumber() {
        int i = Math.max(1, firstMissingIssueNumber); // count up from last found value
        while (new File(getContext().getExternalFilesDir(null), Integer.toString(i)).exists())
            i++;
        return i;
    }

}
