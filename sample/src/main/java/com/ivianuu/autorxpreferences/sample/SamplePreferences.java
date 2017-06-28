package com.ivianuu.autorxpreferences.sample;

import com.ivianuu.autorxpreferences.annotations.Key;
import com.ivianuu.autorxpreferences.annotations.Preferences;

/**
 * @author Manuel Wrage (IVIanuu)
 */
@Preferences
class SamplePreferences {
    @Key protected String accessToken;
    @Key protected Long tokenExpiresAt;
    @Key protected UserData userData;
}
