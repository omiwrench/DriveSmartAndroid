package com.drivesmart.app.android;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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
import com.drivesmart.app.android.view.animation.MainActivityAnimationHandler;
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
    private AppBarLayout appBar;

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
        bindListeners();

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
        appBar = (AppBarLayout) findViewById(R.id.app_bar);
    }
    private void bindListeners(){
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
                hideKeyboard();
                onCancelPressed(view);
            }
        });
        titleInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                appBar.setExpanded(false, true);
            }
        });
        descriptionInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId == EditorInfo.IME_ACTION_DONE){
                    hideKeyboard();
                    return true;
                }
                return false;
            }
        });
    }
    private void onFabPressed(View view){
        animationHandler.revealReportView();
    }
    private void onAddPressed(View view){
        hideKeyboard();

        Report report = extractReportFromInputs();
        reportsPublisher.publishReport(report, new OnRequestFinished<Void>() {
            @Override
            public void onSuccess(List<Void> results) {
                animationHandler.dismissReportsView();
                fetchNewReports();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error: ");
            }
        });

        clearInputs();
    }
    private void onCancelPressed(View view){
        hideKeyboard();
        animationHandler.dismissReportsView();
        clearInputs();
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
    private Report extractReportFromInputs(){
        String title = titleInput.getText().toString();
        String location = locationInput.getText().toString();
        String description = descriptionInput.getText().toString();
        Report report = new Report(0, title, description, location, new DateTime());
        return report;
    }

    private void fetchNewReports(){
        reportsFetcher.getNewReports(new ReportsFetchService.OnFetchFinished() {
            @Override
            public void onSuccess(List<Report> reports) {
                if(reports.size() > 0){
                    handleNewReports(reports);
                }
            }
            @Override
            public void onError(String error) {}
        });
    }
    private void startAutoUpdatingReports(){
        reportsFetcher.startAutoUpdating(new ReportsFetchService.OnFetchFinished() {
            @Override
            public void onSuccess(List<Report> reports) {
                Log.d(TAG, "Fetched " + reports.size() + " reports");
                if (reports.size() > 0) {
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
                    public void onError(String error) {
                    }
                });
            }

            @Override
            public void onError(Exception e) {
            }
        });
    }

    private void hideKeyboard(){
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    private void clearInputs(){
        titleInput.setText("");
        locationInput.setText("");
        descriptionInput.setText("");
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
