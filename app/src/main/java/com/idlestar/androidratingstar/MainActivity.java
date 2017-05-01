package com.idlestar.androidratingstar;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.idlestar.ratingstar.RatingStarView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RatingStarView rsv_rating = (RatingStarView) findViewById(R.id.rsv_rating);
        rsv_rating.setRating(1.5f);
    }
}
