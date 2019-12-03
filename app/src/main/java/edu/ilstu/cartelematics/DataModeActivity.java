package edu.ilstu.cartelematics;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.Toolbar;

import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class DataModeActivity extends AppCompatActivity {

    private FloatingActionButton fabMain;
    private FloatingActionButton fab1;
    private FloatingActionButton fab2;
    private FloatingActionButton fab3;
    private FloatingActionButton fab4;
    private FloatingActionButton fab5;
    private FloatingActionButton fab6;
    private FloatingActionButton nextPage;
    private boolean fabMenuOpen = false;
    private int fabPage = 1;
    private ArrayList<String> stringList = new ArrayList<String>();
    private ListView listView;
    private ArrayAdapter arrayAdapter;
    private String[] values = {"" , "", "", "", "", ""};
    private String[] valuesIoT = {"" , "", "", "", "", "", ""};
    private boolean[] active = {false, false, false, false, false, false};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_mode);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fabMain = findViewById(R.id.fab_main);
        fabMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(fabMenuOpen){
                    closeFabMenu();
                }else{
                    openFabMenu();
                }
            }
        });
        nextPage = findViewById(R.id.fabItemNextPage);
        nextPage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cycleFabPage();
            }
        });
        fab1 = findViewById(R.id.fabItem1);
        fab1.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                buttonPressed(0);
            }
        });
        fab2 = findViewById(R.id.fabItem2);
        fab2.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                buttonPressed(1);
            }
        });
        fab3 = findViewById(R.id.fabItem3);
        fab3.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                buttonPressed(2);
            }
        });
        fab4 = findViewById(R.id.fabItem4);
        fab4.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                buttonPressed(3);
            }
        });
        fab5 = findViewById(R.id.fabItem5);
        fab5.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                buttonPressed(4);
            }
        });
        fab6 = findViewById(R.id.fabItem6);
        fab6.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                buttonPressed(5);
            }
        });
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        arrayAdapter = new ArrayAdapter<String>(this, R.layout.activity_listview, stringList);
        listView = findViewById(R.id.listView);
        listView.setAdapter(arrayAdapter);

        IoT.Connect(this);

        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            valuesIoT = IoT.getData();
                            for(int i = 0; i < 6; i++){
                                if(active[i]){
                                    updateItem(values[i], valuesIoT[i+1]);
                                    values[i] = valuesIoT[i+1];
                                }
                            }
                        }
                    });
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void closeFabMenu(){
        fabMenuOpen = false;
        closeItem(R.id.item1, R.id.item1Text);
        closeItem(R.id.item2, R.id.item2Text);
        closeItem(R.id.item3, R.id.item3Text);
        closeItem(R.id.item4, R.id.item4Text);
        closeItem(R.id.item5, R.id.item5Text);
        closeItem(R.id.item6, R.id.item6Text);
        closeItem(R.id.itemNextPage, R.id.itemNextPageText);
        fabPage = 1;
    }

    private void openFabMenu() {
        fabMenuOpen = true;
        openItem(R.id.item1, R.id.item1Text, R.dimen.fab1);
        openItem(R.id.item2, R.id.item2Text, R.dimen.fab2);
        openItem(R.id.item3, R.id.item3Text, R.dimen.fab3);
        openItem(R.id.itemNextPage, R.id.itemNextPageText, R.dimen.fab4);
    }

    private void cycleFabPage(){
        if(fabPage == 1){
            closeItem(R.id.item1, R.id.item1Text);
            closeItem(R.id.item2, R.id.item2Text);
            closeItem(R.id.item3, R.id.item3Text);
            closeItem(R.id.itemNextPage, R.id.itemNextPageText);

            openItem(R.id.item4, R.id.item4Text, R.dimen.fab1);
            openItem(R.id.item5, R.id.item5Text, R.dimen.fab2);
            openItem(R.id.item6, R.id.item6Text, R.dimen.fab3);
            openItem(R.id.itemNextPage, R.id.itemNextPageText, R.dimen.fab4);
            fabPage = 2;
        }else{
            closeItem(R.id.item4, R.id.item4Text);
            closeItem(R.id.item5, R.id.item5Text);
            closeItem(R.id.item6, R.id.item6Text);
            closeItem(R.id.itemNextPage, R.id.itemNextPageText);

            openItem(R.id.item1, R.id.item1Text, R.dimen.fab1);
            openItem(R.id.item2, R.id.item2Text, R.dimen.fab2);
            openItem(R.id.item3, R.id.item3Text, R.dimen.fab3);
            openItem(R.id.itemNextPage, R.id.itemNextPageText, R.dimen.fab4);
            fabPage = 1;
        }
    }

    private void openItem(int layout, int text, int dimen){
        LinearLayout linearLayout = findViewById(layout);
        TextView textView = findViewById(text);
        linearLayout.animate().translationY(-getResources().getDimension(dimen));
        textView.setVisibility(View.VISIBLE);
    }

    private void closeItem(int layout, int text){
        LinearLayout linearLayout = findViewById(layout);
        TextView textView = findViewById(text);
        textView.setVisibility(View.INVISIBLE);
        linearLayout.animate().translationY(0);
    }

    private void updateItem(String item, String newValue){
        int index = stringList.indexOf(item);
        stringList.set(index, newValue);
        arrayAdapter.notifyDataSetChanged();
    }

    private void buttonPressed(int value){
        if(!active[value]){
            stringList.add(values[value]);
            arrayAdapter.notifyDataSetChanged();
            active[value] = true;
        }
        else{
            stringList.remove(values[value]);
            arrayAdapter.notifyDataSetChanged();
            active[value] = false;
        }
    }

}
