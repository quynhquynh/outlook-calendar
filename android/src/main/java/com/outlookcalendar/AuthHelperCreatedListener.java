package com.outlookcalendar;

import com.outlookcalendar.AuthHelper;
import com.microsoft.identity.client.exception.MsalException;

public interface AuthHelperCreatedListener {
    void onCreated(final AuthHelper authHelper);

    void onError(final MsalException exception);
}
