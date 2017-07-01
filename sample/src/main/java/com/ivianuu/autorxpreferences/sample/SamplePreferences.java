package com.ivianuu.autorxpreferences.sample;

import com.ivianuu.autorxpreferences.annotations.Key;
import com.ivianuu.autorxpreferences.annotations.Preferences;

/**
 * @author Manuel Wrage (IVIanuu)
 */
@Preferences
class SamplePreferences {
    @Key String accessToken;
    @Key UserData userData;
    @Key Boolean loggedIn;
}
