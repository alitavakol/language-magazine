package me.ali.coolenglishmagazine;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

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
public class ReadmeFragment extends Fragment implements ObservableScrollView.Callbacks {

    private static final String TAG = LogHelper.makeLogTag(ReadmeFragment.class);

    public static final String FRAGMENT_TAG = ReadmeFragment.class.getName();

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public ReadmeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ReadmeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ReadmeFragment newInstance(String param1, String param2) {
        ReadmeFragment fragment = new ReadmeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

        // force determine if toolbar should be hidden or not
        isToolbarVisible = -1;
    }

    private ObservableScrollView mScrollView;
    private Toolbar toolbar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_readme, container, false);

        toolbar = (Toolbar) view.findViewById(R.id.toolbar_actionbar);
        mListener.onToolbarCreated(toolbar, R.string.readme);

        mScrollView = (ObservableScrollView) view.findViewById(R.id.scroll_view);
        mScrollView.addCallbacks(this);

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

    /**
     * helper to remember visibility of toolbar, and avoid unnecessary computations on scroll change.
     * if -1, toolbar visibility must be calculated again.
     */
    int isToolbarVisible;

    int toolbarHeight, scrollViewPaddingTop, revealThreshold, hideThreshold;

    /**
     * accumulates scroll amount in a contiguous scroll direction
     */
    int scrollIntegral;

    int previousScrollDirection = -1;

    @Override
    public void onScrollChanged(int deltaX, int deltaY) {
        int scrollY = mScrollView.getScrollY();

        if (scrollViewPaddingTop == 0) {
            scrollViewPaddingTop = mScrollView.getPaddingTop();
            toolbarHeight = toolbar.getMeasuredHeight();
            revealThreshold = scrollViewPaddingTop - 2 * toolbarHeight;
            hideThreshold = scrollViewPaddingTop - toolbarHeight;
        }

        if (previousScrollDirection * deltaY < 0) {
            scrollIntegral = 0;
            previousScrollDirection *= -1;

        } else {
            scrollIntegral += deltaY * previousScrollDirection;
        }

        if (isToolbarVisible != 0 && ((deltaY > 20) || (deltaY > 0 && scrollIntegral > hideThreshold))) {
            toolbar.animate().translationY(-toolbarHeight).setInterpolator(new AccelerateInterpolator()).start();
            isToolbarVisible = 0;
            scrollIntegral = 0;

        } else if (isToolbarVisible != 1 && ((scrollY < revealThreshold) || (deltaY < -20) || (deltaY < 0 && scrollIntegral > hideThreshold))) {
            toolbar.animate().translationY(0).setInterpolator(new DecelerateInterpolator()).start();
            isToolbarVisible = 1;
            scrollIntegral = 0;
        }
    }

}
