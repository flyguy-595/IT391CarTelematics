package edu.ilstu.cartelematics;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Context context = getApplicationContext();

        AWSConnection connection = new AWSConnection();

        String data = connection.AWSConnect(context);

        TextView dataView = findViewById(R.id.textView);

        dataView.setText(data);
    }

}
