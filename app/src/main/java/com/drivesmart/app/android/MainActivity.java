package com.drivesmart.app.android;

import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.dexafree.materialList.card.Card;
import com.dexafree.materialList.card.CardProvider;
import com.dexafree.materialList.view.MaterialListView;
import com.drivesmart.app.android.model.Report;
import com.drivesmart.app.android.service.ReportsFetchService;
import com.drivesmart.app.android.view.layoutmanager.WrappingLinearLayoutManager;
import com.drivesmart.app.android.view.provider.ReportCardProvider;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.recyclerview.animators.SlideInLeftAnimator;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();

    MaterialListView reportsList;
    ReportsFetchService reportsFetcher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JodaTimeAndroid.init(this);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        reportsFetcher = new ReportsFetchService(this);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                mockCards();
            }
        });

        reportsFetcher.startAutoUpdating(new ReportsFetchService.OnFetchFinished() {
            @Override
            public void onSuccess(List<Report> reports) {
                Log.d(TAG, "Fetched " + reports.size() + " reports");
                createReportCards(reports);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Reports failed to fetch. ");
                Log.e(TAG, error);
            }
        });

        reportsList = (MaterialListView) findViewById(R.id.reports_listview);
        reportsList.setItemAnimator(new SlideInLeftAnimator());
        reportsList.getItemAnimator().setAddDuration(300);
        reportsList.getItemAnimator().setRemoveDuration(300);
        //reportsList.setNestedScrollingEnabled(false);
        reportsList.setHasFixedSize(false);
        //reportsList.setLayoutManager(new WrappingLinearLayoutManager(this));
    }

    private void createReportCards(List<Report> reports){
        List<Card> cards = new ArrayList<>();
        for(Report report : reports){
            Card card = new Card.Builder(this)
                                .setTag("REPORT_" + report.getId())
                                .withProvider(new ReportCardProvider())
                                .setTitle(report.getTitle())
                                .setDescription(report.getDescription())
                                .setLocation(report.getLocation())
                                .endConfig()
                                .build();
            cards.add(card);
        }
        reportsList.getAdapter().addAll(cards);
        reportsList.getAdapter().notifyDataSetChanged();
    }
    private void mockCards(){
        Card card = new Card.Builder(this)
                            .setTag("MOCK")
                            .withProvider(new ReportCardProvider())
                            .setTitle("Krock på motorvägen")
                            .setDescription("Stillastående köer över hela jävla skiten")
                            .setLocation("E4an")
                            .endConfig()
                            .build();
        reportsList.getAdapter().add(card);
        reportsList.getAdapter().notifyDataSetChanged();
        Log.d(TAG, "items: " + reportsList.getAdapter().getItemCount());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
