package com.ivianuu.autorxpreferences.sample;

import android.database.sqlite.SQLiteOpenHelper;
import android.support.v4.app.Fragment;
import android.view.View;

import com.ivianuu.autorxpreferences.annotations.Key;
import com.ivianuu.autorxpreferences.annotations.Preferences;

/**
 * @author Manuel Wrage (IVIanuu)
 */
@Preferences
class SamplePreferences {
    @Key String accessToken;
    @Key Long tokenExpiresAt;
    @Key UserData userData;
    @Key UserData otherUserData;
    @Key Boolean loggedIn;
}
