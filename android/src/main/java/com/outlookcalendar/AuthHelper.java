package com.outlookcalendar;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IPublicClientApplication;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.PublicClientApplication;
import com.microsoft.identity.client.exception.MsalException;

public class AuthHelper {
    private static AuthHelper INSTANCE = null;
    private Context context;
    private static ISingleAccountPublicClientApplication mSingleAccountApp = null;
    private String[] mScopes = {"User.Read", "Calendars.ReadWrite"};

    private AuthHelper(Context ctx, final AuthHelperCreatedListener listener) {
        context = ctx;
        PublicClientApplication.createSingleAccountPublicClientApplication(ctx, R.raw.msal_config,
                new IPublicClientApplication.ISingleAccountApplicationCreatedListener() {
                    @Override
                    public void onCreated(ISingleAccountPublicClientApplication application) {
                        mSingleAccountApp = application;
                        listener.onCreated(INSTANCE);
                    }

                    @Override
                    public void onError(MsalException exception) {
                        listener.onError(exception);
                    }
                });
    }

    public static synchronized void getInstance(Context ctx, AuthHelperCreatedListener listener) {
        if (INSTANCE == null) {
            INSTANCE = new AuthHelper(ctx, listener);
        } else {
            listener.onCreated(INSTANCE);
        }
    }

    public static synchronized IPublicClientApplication getPublicClientApp() {
        return mSingleAccountApp;
    }

    public static synchronized AuthHelper getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException(
                    "RNAuthenticationHelper has not been initialized from MainActivity");
        }

        return INSTANCE;
    }

    public void acquireTokenInteractively(Activity activity, AuthenticationCallback callback) {
        mSingleAccountApp.signIn(activity, null, mScopes, callback);
    }

    public void acquireTokenSilently(AuthenticationCallback callback) {
        // Get the authority from MSAL config
        String authority = mSingleAccountApp.getConfiguration().getDefaultAuthority().getAuthorityURL().toString();
        mSingleAccountApp.acquireTokenSilentAsync(mScopes, authority, callback);
    }

    public void signOut() {
        mSingleAccountApp.signOut(new ISingleAccountPublicClientApplication.SignOutCallback() {
            @Override
            public void onSignOut() {
                Toast.makeText(context, "signed out from Outlook account", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(@NonNull MsalException exception) {
                Toast.makeText(context, "MSAL error signing out: " + exception, Toast.LENGTH_SHORT).show();
            }
        });
    }
}


