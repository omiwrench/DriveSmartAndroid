package com.drivesmart.app.android;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.dexafree.materialList.card.Card;
import com.dexafree.materialList.listeners.OnDismissCallback;
import com.dexafree.materialList.view.MaterialListAdapter;
import com.dexafree.materialList.view.MaterialListView;
import com.drivesmart.app.android.dao.DriveSmartDbHelper;
import com.drivesmart.app.android.dao.OnQueryFinished;
import com.drivesmart.app.android.helper.OnRequestFinished;
import com.drivesmart.app.android.model.Report;
import com.drivesmart.app.android.service.ReportsFetchService;
import com.drivesmart.app.android.service.ReportsPublishService;
import com.drivesmart.app.android.view.animation.AnimatorPath;
import com.drivesmart.app.android.view.animation.MainActivityAnimationHandler;
import com.drivesmart.app.android.view.animation.PathEvaluator;
import com.drivesmart.app.android.view.animation.PathPoint;
import com.drivesmart.app.android.view.provider.ReportCardProvider;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.recyclerview.animators.OvershootInRightAnimator;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getName();

    private ViewGroup rootView;
    private FloatingActionButton fab;
    private EditText titleInput;
    private EditText locationInput;
    private EditText descriptionInput;
    private Button addReportButton;
    private Button cancelReportButton;

    private MainActivityAnimationHandler animationHandler;

    private List<Report> reportsList = new ArrayList<>();
    private MaterialListView reportsListView;

    private ReportsFetchService reportsFetcher;
    private ReportsPublishService reportsPublisher;
    private DriveSmartDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JodaTimeAndroid.init(this);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        reportsFetcher = new ReportsFetchService(this);
        reportsPublisher = new ReportsPublishService(this);
        dbHelper = new DriveSmartDbHelper(this);

        bindViews();
        bindClickListeners();

        animationHandler = new MainActivityAnimationHandler(rootView);

        reportsListView.setItemAnimator(new OvershootInRightAnimator());
        reportsListView.getItemAnimator().setAddDuration(300);
        reportsListView.getItemAnimator().setRemoveDuration(300);
        reportsListView.setHasFixedSize(false);

        reportsListView.setOnDismissCallback(new OnDismissCallback() {
            @Override
            public void onDismiss(Card card, int position) {
                Report report = (Report) card.getTag();
                dbHelper.deleteReportById(report.getId());
            }
        });
        startAutoUpdatingReports();
        updateReportsList();
    }
    private void bindViews(){
        rootView = (ViewGroup) findViewById(R.id.root_view);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        addReportButton = (Button) findViewById(R.id.button_add_report);
        cancelReportButton = (Button) findViewById(R.id.button_cancel_report);
        reportsListView = (MaterialListView) findViewById(R.id.reports_listview);
        titleInput = (EditText) findViewById(R.id.input_title);
        locationInput = (EditText) findViewById(R.id.input_location);
        descriptionInput = (EditText) findViewById(R.id.input_description);
    }
    private void bindClickListeners(){
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onFabPressed(view);
            }
        });
        addReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAddPressed(v);
            }
        });
        cancelReportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCancelPressed(view);
            }
        });
    }
    private void onFabPressed(View view){
        animationHandler.revealReportView();
    }
    private void onAddPressed(View view){
        String title = titleInput.getText().toString();
        String location = locationInput.getText().toString();
        String description = descriptionInput.getText().toString();
        Report report = new Report(0, title, description, location, new DateTime());
        reportsPublisher.publishReport(report, new OnRequestFinished<Void>() {
            @Override
            public void onSuccess(List<Void> results) {
                animationHandler.dismissReportsView();
            }
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error: ");
            }
        });
    }
    private void onCancelPressed(View view){
        animationHandler.dismissReportsView();
    }

    private void createReportCards(List<Report> reports){
        MaterialListAdapter adapter = reportsListView.getAdapter();
        for(Report report : reports){
            Card card = new Card.Builder(this)
                                .setTag(report)
                                .withProvider(new ReportCardProvider())
                                .setTitle(report.getTitle())
                                .setDescription(report.getDescription())
                                .setLocation(report.getLocation())
                                .endConfig()
                                .setDismissible()
                                .build();
            adapter.addAtStart(card);
        }
    }

    private void startAutoUpdatingReports(){
        reportsFetcher.startAutoUpdating(new ReportsFetchService.OnFetchFinished() {
            @Override
            public void onSuccess(List<Report> reports) {
                Log.d(TAG, "Fetched " + reports.size() + " reports");
                if(reports.size() > 0) {
                    handleNewReports(reports);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Reports failed to fetch. ");
                Log.e(TAG, error);
            }
        });
    }
    private void stopAutoUpdatingReports(){
        reportsFetcher.cancelAutoUpdating();
    }
    private void handleNewReports(List<Report> reports){
        dbHelper.insertReports(reports, new OnQueryFinished<Void>() {
            @Override
            public void onSuccess(List<Void> results) {
                updateReportsList();
            }

            @Override
            public void onError(Exception e) {
            }
        });
    }
    private void updateReportsList(){
        dbHelper.getAllReports(new OnQueryFinished<Report>() {
            @Override
            public void onSuccess(List<Report> results) {
                results.removeAll(reportsList);
                createReportCards(results);
                reportsList.addAll(results);
            }

            @Override
            public void onError(Exception e) {
            }
        });
    }

    private void reFetchReports(){
        stopAutoUpdatingReports();
        dbHelper.deleteAllReports(new OnQueryFinished<Void>() {
            @Override
            public void onSuccess(List<Void> results) {
                reportsFetcher.getAllReportsSince(new DateTime().minusWeeks(1), new ReportsFetchService.OnFetchFinished() {
                    @Override
                    public void onSuccess(List<Report> reports) {
                        reportsList.clear();
                        handleNewReports(reports);
                        startAutoUpdatingReports();
                    }
                    @Override
                    public void onError(String error) {}
                });
            }
            @Override
            public void onError(Exception e) {}
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            reFetchReports();
        }
        return super.onOptionsItemSelected(item);
    }
}
