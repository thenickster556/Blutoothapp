package com.example.blutooth;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class StarterPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.starter_page);
        Thread welcomeThread = new Thread() {

            @Override
            public void run() {
                try {
                    super.run();
                    sleep(6000);  //Delay of 6 seconds
                } catch (Exception e) {

                } finally {
                    navigateUpTo(getIntent());
                }
            }
        };
        welcomeThread.start();
    }

}
