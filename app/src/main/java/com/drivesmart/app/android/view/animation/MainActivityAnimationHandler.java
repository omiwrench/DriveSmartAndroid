package com.drivesmart.app.android.view.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.dexafree.materialList.view.MaterialListView;
import com.drivesmart.app.android.R;

import java.security.acl.Group;

/**
 * Created by omiwrench on 2016-01-06.
 */
public class MainActivityAnimationHandler {
    private static final String TAG = MainActivityAnimationHandler.class.getName();

    private static final long FAB_ANIMATION_DURATION = 300L;
    private static final float FAB_SCALE_FACTOR = 20.0f;
    private static final int MINIMUM_X_DISTANCE = 150;

    private ViewGroup rootView;
    private ViewGroup fabContainer;
    private View fab;
    private View animationFab;
    private ViewGroup addContentContainer;
    private View addReportButton;
    private View cancelReportButton;

    private boolean revealFlag;

    public MainActivityAnimationHandler(ViewGroup rootView){
        this.rootView = rootView;
        fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fabContainer = (FrameLayout) rootView.findViewById(R.id.fab_container);
        animationFab = (ImageButton) rootView.findViewById(R.id.animation_fab);
        addContentContainer = (ViewGroup) rootView.findViewById(R.id.content_add);
        addReportButton = (Button) rootView.findViewById(R.id.button_add_report);
        cancelReportButton = (Button) rootView.findViewById(R.id.button_cancel_report);
    }

    public void revealReportView(){
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
                    fabContainer.setClipChildren(true);
                    rootView.setClipChildren(true);

                    animationFab.animate()
                            .scaleXBy(FAB_SCALE_FACTOR)
                            .scaleYBy(FAB_SCALE_FACTOR)
                            .setDuration(200)
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
    public void dismissReportsView(){
        View[] subViews = {
                rootView.findViewById(R.id.input_title),
                rootView.findViewById(R.id.input_location),
                rootView.findViewById(R.id.input_description),
                rootView.findViewById(R.id.button_add_report),
                rootView.findViewById(R.id.button_cancel_report)
        };
        for(int i = subViews.length-1; i >= 0; i--){
            View v = subViews[i];
            v.animate()
                    .scaleX(0)
                    .scaleY(0)
                    .setDuration(150)
                    .setStartDelay(i * 30)
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
        }, 200 + (subViews.length - 1) * 50);
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

    public void setFabLoc(PathPoint newLoc){
        animationFab.setTranslationX(newLoc.mX);
        if(!revealFlag){
            animationFab.setTranslationY(newLoc.mY);
        }
    }
    private void onFabAnimationEnded(){
        animationFab.setVisibility(View.INVISIBLE);
        addContentContainer.setVisibility(View.VISIBLE);

        View[] subViews = {
                rootView.findViewById(R.id.input_title),
                rootView.findViewById(R.id.input_location),
                rootView.findViewById(R.id.input_description),
                rootView.findViewById(R.id.button_add_report),
                rootView.findViewById(R.id.button_cancel_report)
        };
        for(int i = 0; i < subViews.length; i++){
            View v = subViews[i];
            v.animate()
                    .scaleX(1)
                    .scaleY(1)
                    .setDuration(150)
                    .setStartDelay(i*30)
                    .start();
        }
    }
}
