package com.ivianuu.autorxpreferences.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import io.reactivex.functions.Consumer;

public class MainActivity extends AppCompatActivity {

    private SamplePreferences_ samplePreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        samplePreferences = SamplePreferences_.create(getApplicationContext());
    }
}
