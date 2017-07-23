package com.ivianuu.autorxpreferences.sample;

import android.preference.Preference;
import android.support.v4.util.Pair;

import com.ivianuu.autorxpreferences.annotations.Key;
import com.ivianuu.autorxpreferences.annotations.Preferences;

import java.util.HashMap;
import java.util.HashSet;
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
    @Key HashMap<String, HashMap<Boolean, Pair<Preference, HashMap<HashMap<String, Long>, HashSet<Throwable>>>>> testHash;
}
