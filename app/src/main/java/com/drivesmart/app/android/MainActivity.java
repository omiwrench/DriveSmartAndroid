package com.drivesmart.app.android;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.media.Image;
import android.os.Bundle;
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
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.dexafree.materialList.card.Card;
import com.dexafree.materialList.listeners.OnDismissCallback;
import com.dexafree.materialList.view.MaterialListAdapter;
import com.dexafree.materialList.view.MaterialListView;
import com.drivesmart.app.android.dao.DriveSmartDbHelper;
import com.drivesmart.app.android.dao.OnQueryFinished;
import com.drivesmart.app.android.model.Report;
import com.drivesmart.app.android.service.ReportsFetchService;
import com.drivesmart.app.android.view.animation.AnimatorPath;
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

    private static final long FAB_ANIMATION_DURATION = 300L;
    private static final float FAB_SCALE_FACTOR = 20.0f;
    private static final int MINIMUM_X_DISTANCE = 200;

    private ViewGroup rootView;
    private FrameLayout fabContainer;
    private FloatingActionButton fab;
    private ImageButton animationFab;
    private boolean revealFlag;
    private float fabSize;

    private ViewGroup addContentContainer;

    private List<Report> reportsList = new ArrayList<>();
    private MaterialListView reportsListView;

    private ReportsFetchService reportsFetcher;
    private DriveSmartDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JodaTimeAndroid.init(this);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        reportsFetcher = new ReportsFetchService(this);
        dbHelper = new DriveSmartDbHelper(this);

        rootView = (ViewGroup) findViewById(R.id.root_view);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onFabPressed(view);
            }
        });
        fabContainer = (FrameLayout) findViewById(R.id.fab_container);
        animationFab = (ImageButton) findViewById(R.id.animation_fab);
        addContentContainer = (ViewGroup) findViewById(R.id.content_add);

        startAutoUpdatingReports();

        reportsListView = (MaterialListView) findViewById(R.id.reports_listview);
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
        updateReportsList();
    }
    private void onFabPressed(View view){
        final float startX = fab.getX();

        fab.setVisibility(View.INVISIBLE);
        animationFab.setVisibility(View.VISIBLE);

        AnimatorPath path = new AnimatorPath();
        path.moveTo(0, 0);
        path.curveTo(-200, 1000, -400, 100, -600, 1000);

        final ObjectAnimator anim = ObjectAnimator.ofObject(this, "fabLoc", new PathEvaluator(), path.getPoints().toArray());

        anim.setInterpolator(new AccelerateInterpolator());
        anim.setDuration(FAB_ANIMATION_DURATION);
        anim.start();

        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                if (Math.abs(startX - animationFab.getX()) > MINIMUM_X_DISTANCE && !revealFlag) {
                    //fabContainer.setY(fabContainer.getY() + (fabSize/2));
                    fabContainer.setClipChildren(true);
                    rootView.setClipChildren(true);

                    animationFab.animate()
                            .scaleXBy(FAB_SCALE_FACTOR)
                            .scaleYBy(FAB_SCALE_FACTOR)
                            .setDuration(FAB_ANIMATION_DURATION)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    onFabAnimationEnded();
                                }
                            });
                    revealFlag = true;
                }
            }
        });
    }
    public void setFabLoc(PathPoint newLoc){
        animationFab.setTranslationX(newLoc.mX);
        if(revealFlag){
            //fab.setTranslationY(newLoc.mY - (fabSize / 2));
        }
        else{
            animationFab.setTranslationY(newLoc.mY);
        }
    }
    private void onFabAnimationEnded(){
        animationFab.setVisibility(View.INVISIBLE);
        addContentContainer.setVisibility(View.VISIBLE);

        View[] subViews = {
                findViewById(R.id.input_title),
                findViewById(R.id.input_location),
                findViewById(R.id.input_description),
                findViewById(R.id.button_add_report),
                findViewById(R.id.button_cancel_report)
        };
        for(int i = 0; i < subViews.length; i++){
            View v = subViews[i];
            v.animate()
                    .scaleX(1)
                    .scaleY(1)
                    .setDuration(200)
                    .setStartDelay(i*50)
                    .start();
        }
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
