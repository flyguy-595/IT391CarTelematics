package edu.ilstu.cartelematics;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void openDataMode(View v){
        Intent intent = new Intent(this, DataModeActivity.class);
        startActivity(intent);
    }

    public void openCameraMode(View v){
        Intent intent = new Intent(this, CameraModeActivity.class);
        startActivity(intent);
    }

}
