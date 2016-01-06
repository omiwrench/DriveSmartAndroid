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
    private Button addReportButton;
    private Button cancelReportButton;

    private List<Report> reportsList = new ArrayList<>();
    private MaterialListView reportsListView;

    private ReportsFetchService reportsFetcher;
    private DriveSmartDbHelper dbHelper;

    private float[] fabStartingPosition = new float[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JodaTimeAndroid.init(this);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        reportsFetcher = new ReportsFetchService(this);
        dbHelper = new DriveSmartDbHelper(this);

        bindViews();
        bindClickListeners();

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
        fabContainer = (FrameLayout) findViewById(R.id.fab_container);
        animationFab = (ImageButton) findViewById(R.id.animation_fab);
        addContentContainer = (ViewGroup) findViewById(R.id.content_add);
        addReportButton = (Button) findViewById(R.id.button_add_report);
        cancelReportButton = (Button) findViewById(R.id.button_cancel_report);
        reportsListView = (MaterialListView) findViewById(R.id.reports_listview);
    }
    private void bindClickListeners(){
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onFabPressed(view);
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
        final float startX = fab.getX();
        final float startY = fab.getY();

        fabStartingPosition = new float[]{startX, startY};

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

    private void onCancelPressed(View view){
        Log.d(TAG, "Cancel pressed");
        View[] subViews = {
                findViewById(R.id.input_title),
                findViewById(R.id.input_location),
                findViewById(R.id.input_description),
                findViewById(R.id.button_add_report),
                findViewById(R.id.button_cancel_report)
        };
        for(int i = subViews.length-1; i >= 0; i--){
            View v = subViews[i];
            v.animate()
                    .scaleX(0)
                    .scaleY(0)
                    .setDuration(200)
                    .setStartDelay(i * 50)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                        }
                    })
                    .start();
        }
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                onCancelAnimationEnded();
            }
        }, 200 + (subViews.length-1)*50);
    }
    private void onCancelAnimationEnded(){
        Log.d(TAG, "OnCancelAnimationEnded");
        int dur = 400;
        revealFlag = false;
        animationFab.setVisibility(View.VISIBLE);
        addContentContainer.setVisibility(View.INVISIBLE);

        final float startX = fab.getX();

        AnimatorPath path = new AnimatorPath();
        path.moveTo(-animationFab.getX(), animationFab.getY());
        path.curveTo(-200, 700, 0, 0, 0, 0);

        final ObjectAnimator anim = ObjectAnimator.ofObject(this, "fabLoc", new PathEvaluator(), path.getPoints().toArray());

        anim.setInterpolator(new AccelerateInterpolator());
        anim.setDuration(dur);
        anim.setStartDelay(dur / 2);
        anim.start();

        animationFab.animate()
                .scaleX(1)
                .scaleY(1)
                .setDuration(dur)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        //If I remove this the animation gets all fucked up. Good job devs.
                    }
                });
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                fabContainer.setClipChildren(false);
                rootView.setClipChildren(false);
            }
        }, (long) (dur * 1.5));
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                animationFab.setVisibility(View.INVISIBLE);
                fab.setVisibility(View.VISIBLE);
            }
        }, (long)(dur*2.0));
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
