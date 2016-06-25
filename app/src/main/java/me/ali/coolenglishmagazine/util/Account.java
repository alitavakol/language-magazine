package me.ali.coolenglishmagazine.util;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

import me.ali.coolenglishmagazine.R;

/**
 * Common functions to perform sign-in to Google account.
 * Created by Hamed on 6/24/16.
 */
public class Account implements GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = LogHelper.makeLogTag(Account.class);

    public GoogleApiClient mGoogleApiClient;

    public AppCompatActivity context;

    public Account(Callbacks context) {
        this.context = (AppCompatActivity) context;
        this.callbacks = context;

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestId()
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        mGoogleApiClient = new GoogleApiClient.Builder(this.context)
                .enableAutoManage(this.context /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    /**
     * textually displays sign-in {@link Status} through a toast.
     *
     * @param status sign-in result status
     */
    public void toastGoogleSignInResult(Status status) {
        int id;
        switch (status.getStatusCode()) {
            case GoogleSignInStatusCodes.SIGN_IN_CANCELLED:
                id = NetworkHelper.isOnline(context) ? R.string.sign_in_cancelled : R.string.sign_in_cancelled_maybe;
                break;

            case GoogleSignInStatusCodes.SIGN_IN_FAILED:
                id = R.string.sign_in_failed;
                break;

            case CommonStatusCodes.NETWORK_ERROR:
                id = NetworkHelper.isOnline(context) ? R.string.sign_in_network_error : R.string.check_connection;
                break;

            case CommonStatusCodes.INVALID_ACCOUNT:
                id = R.string.sign_in_invalid_account;
                break;

            case CommonStatusCodes.SIGN_IN_REQUIRED:
                id = 0;//R.string.sign_in_required;
                break;

            default:
                id = R.string.sign_in_error;
        }
        if (id != 0)
            Toast.makeText(context, id, Toast.LENGTH_SHORT).show();
    }

    public void silentSignIn() {
        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            LogHelper.d(TAG, "Got cached sign-in");
            GoogleSignInResult result = opr.get();
            handleSignInResult(result);

        } else {
            // If the user has not previously signed in on this device or the sign-in has expired,
            // this asynchronous branch will attempt to sign in the user silently.  Cross-device
            // single sign-on will occur in this branch.
            callbacks.showProgressDialog();
            callbacks.signingIn(true);

            opr.setResultCallback(new ResultCallback<GoogleSignInResult>() {
                @Override
                public void onResult(@NonNull GoogleSignInResult googleSignInResult) {
                    handleSignInResult(googleSignInResult);

                    callbacks.signingIn(false);
                    callbacks.hideProgressDialog();
                }
            });
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        LogHelper.d(TAG, "handleSignInResult:" + result.isSuccess());

        if (result.isSuccess()) { // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            if (acct != null) {
                final Uri personPhoto = acct.getPhotoUrl();
                final String displayName = acct.getDisplayName();
                final String email = acct.getEmail();
                final String user_id_token = acct.getIdToken();
                callbacks.updateProfileInfo(personPhoto != null ? personPhoto.toString() : null, displayName, email, user_id_token);
            }

        } else { // Signed out, show unauthenticated UI.
            toastGoogleSignInResult(result.getStatus());
            callbacks.updateProfileInfo(null, null, null, null);
        }
    }

    public interface Callbacks {
        /**
         * when sign in/out process starts, it is invoked to reflect it in GUI.
         */
        void showProgressDialog();

        /**
         * when sign in/out process finishes, it is invoked to reflect in in GUI.
         */
        void hideProgressDialog();

        /**
         * sends profile information to the listener. fields are null if sign out or error occurs.
         */
        void updateProfileInfo(String personPhoto, String displayName, String email, String userIdToken);

        void signingIn(boolean inProgress);
    }

    Callbacks callbacks;

    protected static final int RC_SIGN_IN = 1;

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);

            callbacks.signingIn(false);
            callbacks.hideProgressDialog();
        }
    }

    public void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        context.startActivityForResult(signInIntent, RC_SIGN_IN);

        callbacks.showProgressDialog();
        callbacks.signingIn(true);
    }

    public void signOut() {
        callbacks.showProgressDialog();
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            callbacks.updateProfileInfo(null, null, null, null);

                        } else {
                            int id = NetworkHelper.isOnline(context) ? R.string.sign_out_error : R.string.check_connection;
                            Toast.makeText(context, id, Toast.LENGTH_SHORT).show();
                        }

                        callbacks.signingIn(false);
                        callbacks.hideProgressDialog();
                    }
                });
    }

    private void revokeAccess() {
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        callbacks.signingIn(false);
                        callbacks.hideProgressDialog();
                    }
                });
    }

    public void onConnectionFailed(@NonNull ConnectionResult result) {
        // TODO: https://developers.google.com/android/reference/com/google/android/gms/common/api/GoogleApiClient.OnConnectionFailedListener#public-methods
        callbacks.signingIn(false);
        callbacks.hideProgressDialog();
        Toast.makeText(context, R.string.sign_in_error, Toast.LENGTH_SHORT).show();
    }
}
