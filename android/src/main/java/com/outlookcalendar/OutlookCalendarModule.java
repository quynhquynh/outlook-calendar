package com.outlookcalendar;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.microsoft.graph.models.extensions.Event;
import com.microsoft.graph.models.extensions.User;
import com.microsoft.graph.requests.extensions.IEventCollectionPage;
import com.microsoft.identity.client.AuthenticationCallback;
import com.microsoft.identity.client.IAuthenticationResult;
import com.microsoft.identity.client.ICurrentAccountResult;
import com.microsoft.identity.client.ISingleAccountPublicClientApplication;
import com.microsoft.identity.client.exception.MsalClientException;
import com.microsoft.identity.client.exception.MsalException;
import com.microsoft.identity.client.exception.MsalServiceException;
import com.microsoft.identity.client.exception.MsalUiRequiredException;
import com.microsoft.identity.client.exception.MsalUserCancelException;

import java.util.Objects;

public class OutlookCalendarModule extends ReactContextBaseJavaModule {

    private static final String TAG = "OutlookCalendar";
    private final ReactApplicationContext reactContext;
    private AuthHelper mAuthHelper = null;
    GraphHelper graphHelper = null;
    private String accessToken = "";

    public OutlookCalendarModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return TAG;
    }

    @ReactMethod
    public void sampleMethod(String stringArgument, int numberArgument, Callback callback) {
        // TODO: Implement some actually useful functionality
        callback.invoke("Received numberArgument: " + numberArgument + " stringArgument: " + stringArgument);
    }

    @ReactMethod
    public void authenticateUser(Callback cb) {
        AuthHelper.getInstance(reactContext,
                new AuthHelperCreatedListener() {
                    @Override
                    public void onCreated(AuthHelper authHelper) {
                        mAuthHelper = authHelper;
                        try {
                            mAuthHelper.acquireTokenSilently(getAuthCallback(cb));
                        } catch (Exception e) {
                            // TODO
                        }                          
                    }

                    @Override
                    public void onError(MsalException exception) {
                        // Error creating auth helper
                        onAuthenticationFail(exception, cb);
                    }
                });
    }

    @ReactMethod
    public void getCurrentUser(final Promise promise) {
        try {
            User user = graphHelper.getUser(accessToken);
            WritableMap userMapObject = Utility.getMapObject(user.getRawObject());
            promise.resolve(userMapObject);
        } catch (Exception e) {
            promise.reject("GetUserRequestError", e.getMessage());
        }
    }

    @ReactMethod
    public void saveEvent(ReadableMap data, final Promise promise) {
        try {
            System.out.println("saveEvent");
            Event event = graphHelper.saveEvent(accessToken, data);
            System.out.println("event id: " + event.getRawObject().get("id"));
            WritableMap eventMapObject = Utility.getMapObject(event.getRawObject());
            promise.resolve(eventMapObject);
        } catch (Exception e) {
            System.out.println("saveEvent exception: " + e.getMessage());
            promise.reject("PostEventRequestError", e.getMessage());
        }
    }

    @ReactMethod
    public void findEventById(String id, final Promise promise) {
        try {
            Event event = graphHelper.getEvent(accessToken, id);
            WritableMap eventMapObject = Utility.getMapObject(event.getRawObject());
            promise.resolve(eventMapObject);
        } catch (Exception e) {
            promise.reject("GetEventRequestError", e.getMessage());
        }
    }

    @ReactMethod
    public void getEvents(final Promise promise) {
        try {
            IEventCollectionPage iEventCollectionPage = graphHelper.getEvents(accessToken);
            WritableMap eventsMapObject = Utility.getMapObject(iEventCollectionPage.getRawObject());
            promise.resolve(eventsMapObject);
        } catch (Exception e) {
            promise.reject("GetEventsRequestError", e.getMessage());
        }
    }

    @ReactMethod
    public void removeEvent(String id, final Promise promise) {
        try {
            graphHelper.deleteEvent(accessToken, id);
            WritableMap eventsMapObject = new WritableNativeMap();
            promise.resolve(eventsMapObject);
        } catch (Exception e) {
            promise.reject("RemoveEventRequestError", e.getMessage());
        }
    }

    private AuthenticationCallback getAuthCallback(Callback cb) {
        return new AuthenticationCallback() {
            @Override
            public void onCancel() {
                onAuthenticationFail(new MsalUserCancelException(), cb);
            }

            @Override
            public void onSuccess(IAuthenticationResult authenticationResult) {
                accessToken = authenticationResult.getAccessToken();
                // Get Graph client and get user
                graphHelper = GraphHelper.getInstance();

                ISingleAccountPublicClientApplication mSingleAccountApp = (ISingleAccountPublicClientApplication) AuthHelper.getPublicClientApp();

                new Thread(() -> {
                    ICurrentAccountResult currentAccountResult = null;
                    try {
                        currentAccountResult = mSingleAccountApp.getCurrentAccount();
                    } catch (InterruptedException e) {
                        WritableMap error = Utility.generateErrorMapObject("InterruptedException", e.getMessage());
                        cb.invoke(error);
                    } catch (MsalException e) {
                        onAuthenticationFail(e, cb);
                    }
                    String username = currentAccountResult.getCurrentAccount().getUsername();
                    cb.invoke(username);
                }).start();
            }

            @Override
            public void onError(MsalException exception) {
                onAuthenticationFail(exception, cb);
            }
        };
    }

    private void onAuthenticationFail(MsalException exception, Callback cb) {
        WritableMap error = new WritableNativeMap();
        error.putString("errorCode", "AUTH");
        if (exception instanceof MsalUiRequiredException) {
            mAuthHelper.acquireTokenInteractively(getCurrentActivity(), getAuthCallback(cb));
        } else if (exception instanceof MsalClientException) {
            MsalClientException clientException = (MsalClientException) exception;
            if (Objects.equals(clientException.getErrorCode(), "no_current_account") ||
                    Objects.equals(clientException.getErrorCode(), "no_account_found")) {
                mAuthHelper.acquireTokenInteractively(getCurrentActivity(), getAuthCallback(cb));
            } else {
                error.putString("message", "Client error authenticating");
                cb.invoke(error);
            }
        } else if (exception instanceof MsalServiceException) {
            // Exception when communicating with the auth server, likely config issue
            error.putString("message", "Service error authenticating");
            cb.invoke(error);
        } else if (exception instanceof MsalUserCancelException) {
            error.putString("message", "Authentication cancelled");
            cb.invoke(error);
        } else {
            error.putString("message", "Failed to load account");
            cb.invoke(error);
        }
    }
}
