package com.ivianuu.autorxpreferences.sample;

import com.ivianuu.autorxpreferences.annotations.Key;
import com.ivianuu.autorxpreferences.annotations.Preferences;

/**
 * @author Manuel Wrage (IVIanuu)
 */
@Preferences(preferenceName = "hahaha", expose = false)
class SamplePreferences {
    @Key String accessToken;
    @Key Long tokenExpiresAt;
    @Key UserData userData;
    @Key UserData otherUserData;
    @Key UserData anotherOtherUserData;
    @Key Song currentSong;
    @Key Album lastPlayedAlbum;
    @Key Song nextSong;
    @Key Song lastSong;
}
