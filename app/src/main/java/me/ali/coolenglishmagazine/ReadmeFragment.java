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
import android.widget.FrameLayout;

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

        // force determine if toolbar should be hidden or not
        isToolbarVisible = -1;
    }

    private ObservableScrollView mScrollView;
    private Toolbar toolbar;

    protected int currentCardIndex = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.fragment_readme, container, false);

        toolbar = (Toolbar) view.findViewById(R.id.toolbar_actionbar);
        mListener.onToolbarCreated(toolbar, R.string.readme);

        mScrollView = (ObservableScrollView) view.findViewById(R.id.scroll_view);
        mScrollView.addCallbacks(this);

        final View buttonPrevious = view.findViewById(R.id.button_previous);
        final View buttonNext = view.findViewById(R.id.button_next);
        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((FrameLayout) view.findViewById(R.id.card_container)).getChildAt(currentCardIndex).setVisibility(View.GONE);
                currentCardIndex++;
                ((FrameLayout) view.findViewById(R.id.card_container)).getChildAt(currentCardIndex).setVisibility(View.VISIBLE);
                buttonPrevious.setClickable(currentCardIndex > 0);
                buttonNext.setClickable(currentCardIndex < 1);
            }
        });
        buttonPrevious.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((FrameLayout) view.findViewById(R.id.card_container)).getChildAt(currentCardIndex).setVisibility(View.GONE);
                currentCardIndex--;
                ((FrameLayout) view.findViewById(R.id.card_container)).getChildAt(currentCardIndex).setVisibility(View.VISIBLE);
                buttonPrevious.setClickable(currentCardIndex > 0);
                buttonNext.setClickable(currentCardIndex < 1);
            }
        });

        ((FrameLayout) view.findViewById(R.id.card_container)).getChildAt(currentCardIndex).setVisibility(View.VISIBLE);
        buttonPrevious.setClickable(currentCardIndex > 0);
        buttonNext.setClickable(currentCardIndex < 1);

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

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentCardIndex", currentCardIndex);
    }
}
