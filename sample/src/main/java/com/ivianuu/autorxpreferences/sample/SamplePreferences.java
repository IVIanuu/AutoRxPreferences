package com.ivianuu.autorxpreferences.sample;

import com.ivianuu.autorxpreferences.annotations.Key;
import com.ivianuu.autorxpreferences.annotations.Preferences;

/**
 * @author Manuel Wrage (IVIanuu)
 */
@Preferences(preferenceName = "haha")
class SamplePreferences {
    @Key String accessToken;
    @Key Long tokenExpiresAt;
    @Key UserData userData;
    @Key UserData otherUserData;
    @Key UserData anotherOtherUserData;
    @Key Song currentSong;

}
