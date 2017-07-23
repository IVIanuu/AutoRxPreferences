package com.ivianuu.autorxpreferences.sample;

import com.ivianuu.autorxpreferences.annotations.Key;
import com.ivianuu.autorxpreferences.annotations.Preferences;

import java.util.List;
import java.util.Set;

/**
 * @author Manuel Wrage (IVIanuu)
 */
@Preferences
class SamplePreferences {
    @Key String accessToken;
    @Key UserData userData;
    @Key Boolean loggedIn;
    @Key Set<String> myStringSet;
    @Key List<UserData> userDataList;
}
