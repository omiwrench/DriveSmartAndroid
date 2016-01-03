package com.drivesmart.app.android.view.provider;

import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.dexafree.materialList.card.Card;
import com.dexafree.materialList.card.CardProvider;
import com.drivesmart.app.android.R;

/**
 * Created by omiwrench on 2016-01-03.
 */
public class ReportCardProvider extends CardProvider<ReportCardProvider>{

    private String title;
    private String location;
    private String description;

    @Override
    public int getLayout(){
        return R.layout.report_card_layout;
    }

    public ReportCardProvider setTitle(String title){
        this.title = title;
        notifyDataSetChanged();
        return this;
    }
    public ReportCardProvider setLocation(String location){
        this.location = location;
        notifyDataSetChanged();
        return this;
    }
    public ReportCardProvider setDescription(String description){
        this.description = description;
        notifyDataSetChanged();
        return this;
    }

    @Override
    public void render(@NonNull View view, @NonNull Card card){
        super.render(view, card);

        TextView titleView = (TextView)view.findViewById(R.id.report_title);
        TextView locationView = (TextView)view.findViewById(R.id.report_location);
        TextView descriptionView = (TextView)view.findViewById(R.id.report_description);

        titleView.setText(title);
        locationView.setText(location);
        descriptionView.setText(description);
    }
}
