package com.drivesmart.app.android.view.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Handler;
import android.support.design.widget.AppBarLayout;
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

    private static final long FAB_REVEAL_ANIMATION_DURATION = 300L;
    private static final long FAB_HIDE_ANIMATION_DURATION   = 300L;
    private static final long CHILDREN_ANIMATION_DURATION   = 100L;
    private static final long CHILDREN_ANIMATION_STEP       = 25L;
    private static final float FAB_SCALE_FACTOR             = 20.0f;
    private static final int MINIMUM_X_DISTANCE             = 150;

    private static final PathPoint REVEAL_CURVE = PathPoint.curveTo(-200, 1000, -400, 100, -600, 1000);
    private static final PathPoint HIDE_CURVE   = PathPoint.curveTo(-200, 700, 0, 0, 0, 0);

    private enum AnimationState{
        ANIMATING_FAB_REVEALING,
        ANIMATING_CHILDREN_REVEALING,
        PASSIVE_REVEALED,
        ANIMATING_CHILDREN_HIDING,
        ANIMATING_FAB_HIDING,
        PASSIVE_HIDDEN
    }

    private ViewGroup rootView;
    private ViewGroup fabContainer;
    private View fab;
    private View animationFab;
    private ViewGroup addContentContainer;
    private View addReportButton;
    private View cancelReportButton;

    private final View[] addReportAnimatableChildren;

    private boolean revealFlag;

    public MainActivityAnimationHandler(ViewGroup rootView){
        this.rootView = rootView;
        fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
        fabContainer = (FrameLayout) rootView.findViewById(R.id.fab_container);
        animationFab = (ImageButton) rootView.findViewById(R.id.animation_fab);
        addContentContainer = (ViewGroup) rootView.findViewById(R.id.content_add);
        addReportButton = (Button) rootView.findViewById(R.id.button_add_report);
        cancelReportButton = (Button) rootView.findViewById(R.id.button_cancel_report);

        addReportAnimatableChildren = new View[]{
                rootView.findViewById(R.id.input_title),
                rootView.findViewById(R.id.input_location),
                rootView.findViewById(R.id.input_description),
                addReportButton,
                cancelReportButton
        };
    }

    public void revealReportView(){
        final float startX = fab.getX();

        setAnimationState(AnimationState.ANIMATING_FAB_REVEALING);

        AnimatorPath path = new AnimatorPath();
        path.moveTo(0, 0);
        path.curveTo(REVEAL_CURVE);

        final ObjectAnimator anim = ObjectAnimator.ofObject(this, "fabLoc", new PathEvaluator(), path.getPoints().toArray());

        anim.setInterpolator(new AccelerateInterpolator());
        anim.setDuration(FAB_REVEAL_ANIMATION_DURATION);
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
        for(int i = addReportAnimatableChildren.length-1; i >= 0; i--){
            View v = addReportAnimatableChildren[i];
            v.animate()
                    .scaleX(0)
                    .scaleY(0)
                    .setDuration(CHILDREN_ANIMATION_DURATION)
                    .setStartDelay(i * CHILDREN_ANIMATION_STEP)
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
        }, 200 + (addReportAnimatableChildren.length - 1) * 50);
    }
    private void onCancelAnimationEnded(){
        revealFlag = false;

       setAnimationState(AnimationState.ANIMATING_FAB_HIDING);

        AnimatorPath path = new AnimatorPath();
        path.moveTo(-animationFab.getX(), animationFab.getY());
        path.curveTo(HIDE_CURVE);

        final ObjectAnimator anim = ObjectAnimator.ofObject(this, "fabLoc", new PathEvaluator(), path.getPoints().toArray());

        anim.setInterpolator(new AccelerateInterpolator());
        anim.setDuration(FAB_HIDE_ANIMATION_DURATION);
        anim.setStartDelay(CHILDREN_ANIMATION_DURATION);
        anim.start();

        animationFab.animate()
                .scaleX(1)
                .scaleY(1)
                .setDuration(FAB_HIDE_ANIMATION_DURATION)
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
                ((AppBarLayout)rootView.findViewById(R.id.app_bar)).setExpanded(true, true);
            }
        }, (long) (FAB_HIDE_ANIMATION_DURATION * 1.5));
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                setAnimationState(AnimationState.PASSIVE_HIDDEN);
            }
        }, (long)(FAB_HIDE_ANIMATION_DURATION * 2.0));
    }

    public void setFabLoc(PathPoint newLoc){
        animationFab.setTranslationX(newLoc.mX);
        if(!revealFlag){
            animationFab.setTranslationY(newLoc.mY);
        }
    }
    private void onFabAnimationEnded(){
        setAnimationState(AnimationState.ANIMATING_CHILDREN_REVEALING);
        for(int i = 0; i < addReportAnimatableChildren.length; i++){
            View v = addReportAnimatableChildren[i];
            v.animate()
                    .scaleX(1)
                    .scaleY(1)
                    .setDuration(CHILDREN_ANIMATION_DURATION)
                    .setStartDelay(i * CHILDREN_ANIMATION_STEP)
                    .start();
        }
    }

    private void setAnimationState(AnimationState state){
        switch(state){
            case ANIMATING_FAB_REVEALING:
                rootView.removeView(fab);
                animationFab.setVisibility(View.VISIBLE);
                break;
            case ANIMATING_CHILDREN_REVEALING:
                animationFab.setVisibility(View.INVISIBLE);
                addContentContainer.setVisibility(View.VISIBLE);
                break;
            case PASSIVE_REVEALED:
                break;
            case ANIMATING_CHILDREN_HIDING:
                break;
            case ANIMATING_FAB_HIDING:
                animationFab.setVisibility(View.VISIBLE);
                addContentContainer.setVisibility(View.INVISIBLE);
                break;
            case PASSIVE_HIDDEN:
                rootView.addView(fab);
                animationFab.setVisibility(View.INVISIBLE);
                break;
        }
    }
}
