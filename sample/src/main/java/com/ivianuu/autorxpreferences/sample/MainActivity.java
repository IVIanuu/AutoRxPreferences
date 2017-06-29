package com.ivianuu.autorxpreferences.sample;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

import com.f2prateek.rx.preferences2.RxSharedPreferences;
import com.ivianuu.autorxpreferences.annotations.Key;
import com.ivianuu.autorxpreferences.annotations.Preferences;

import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {

    private SamplePreferences_ samplePreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        samplePreferences = SamplePreferences_.create(this);
    }
}
