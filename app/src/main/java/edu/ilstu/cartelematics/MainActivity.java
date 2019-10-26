package edu.ilstu.cartelematics;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AWSConnection connection = new AWSConnection(this);
        connection.AWSConnect();
    }

    public void setData(String data) {
        TextView dataView = findViewById(R.id.textView);
        dataView.setText(data);
    }


}
