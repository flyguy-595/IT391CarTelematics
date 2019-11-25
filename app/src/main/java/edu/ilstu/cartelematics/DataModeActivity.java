package edu.ilstu.cartelematics;

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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

public class DataModeActivity extends AppCompatActivity {

    private FloatingActionButton fabMain;
    private FloatingActionButton fab1;
    private FloatingActionButton fab2;
    private FloatingActionButton fab3;
    private FloatingActionButton fab4;
    private FloatingActionButton fab5;
    private FloatingActionButton fab6;
    private FloatingActionButton fab7;
    private FloatingActionButton fab8;
    private FloatingActionButton fab9;
    private FloatingActionButton nextPage;
    boolean fabMenuOpen = false;
    int fabPage = 1;

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
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void closeFabMenu(){
        fabMenuOpen = false;
        closeItem(R.id.item1, R.id.item1Text);
        closeItem(R.id.item2, R.id.item2Text);
        closeItem(R.id.item3, R.id.item3Text);
        closeItem(R.id.item4, R.id.item4Text);
        closeItem(R.id.item5, R.id.item5Text);
        closeItem(R.id.item6, R.id.item6Text);
        closeItem(R.id.item7, R.id.item7Text);
        closeItem(R.id.item8, R.id.item8Text);
        closeItem(R.id.item9, R.id.item9Text);
        closeItem(R.id.itemNextPage, R.id.itemNextPageText);
        fabPage = 1;
    }

    private void openFabMenu() {
        fabMenuOpen = true;
        openItem(R.id.item1, R.id.item1Text, R.dimen.fab1);
        openItem(R.id.item2, R.id.item2Text, R.dimen.fab2);
        openItem(R.id.item3, R.id.item3Text, R.dimen.fab3);
        openItem(R.id.item4, R.id.item4Text, R.dimen.fab4);
        openItem(R.id.item5, R.id.item5Text, R.dimen.fab5);
        openItem(R.id.itemNextPage, R.id.itemNextPageText, R.dimen.fab6);
    }

    private void cycleFabPage(){
        if(fabPage == 1){
            closeItem(R.id.item1, R.id.item1Text);
            closeItem(R.id.item2, R.id.item2Text);
            closeItem(R.id.item3, R.id.item3Text);
            closeItem(R.id.item4, R.id.item4Text);
            closeItem(R.id.item5, R.id.item5Text);
            closeItem(R.id.itemNextPage, R.id.itemNextPageText);

            openItem(R.id.item6, R.id.item6Text, R.dimen.fab1);
            openItem(R.id.item7, R.id.item7Text, R.dimen.fab2);
            openItem(R.id.item8, R.id.item8Text, R.dimen.fab3);
            openItem(R.id.item9, R.id.item9Text, R.dimen.fab4);
            openItem(R.id.itemNextPage, R.id.itemNextPageText, R.dimen.fab5);
            fabPage = 2;
        }else{
            closeItem(R.id.item6, R.id.item6Text);
            closeItem(R.id.item7, R.id.item7Text);
            closeItem(R.id.item8, R.id.item8Text);
            closeItem(R.id.item9, R.id.item9Text);
            closeItem(R.id.itemNextPage, R.id.itemNextPageText);

            openItem(R.id.item1, R.id.item1Text, R.dimen.fab1);
            openItem(R.id.item2, R.id.item2Text, R.dimen.fab2);
            openItem(R.id.item3, R.id.item3Text, R.dimen.fab3);
            openItem(R.id.item4, R.id.item4Text, R.dimen.fab4);
            openItem(R.id.item5, R.id.item5Text, R.dimen.fab5);
            openItem(R.id.itemNextPage, R.id.itemNextPageText, R.dimen.fab6);
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

}
