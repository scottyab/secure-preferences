package com.securepreferences.sample.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.securepreferences.sample.R;
import com.securepreferences.sample.utils.ViewUtil;


/**
 * Simple view group that container a progress spinner and text
 * Uses default theme colours unless manually changed.
 */
public class LoadingView extends RelativeLayout {

    private ProgressBar progressBar;
    private TextView loadingTextView;

    private int loadingTextColour;
    private int progressBarColour;
    private String loadingMessage;
    private boolean loading;

    public LoadingView(Context context) {
        super(context);
        init(context, null);
    }

    public LoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public LoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        inflateView();

        configureDefaults(context, attrs);

        //we want the loading view to intercept the clicks
        setClickable(true);

        setLoadingMessage(loadingMessage);
        setProgressBarColor(progressBarColour);
        setLoadingMessageColor(loadingTextColour);
    }


    private void inflateView() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.view_loading, this, true);
        loadingTextView = findViewById(R.id.loadingText);
        progressBar = findViewById(R.id.loadingProgressBar);
    }

    private void configureDefaults(Context context, AttributeSet attrs) {
        String defaultLoadingMsg = context.getString(R.string.msg_loading);
        int defaultForeGroundColor = ContextCompat.getColor(context, android.R.color.white);
        if (attrs != null) {
            TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.LoadingView);
            loadingMessage = attributes.getString(R.styleable.LoadingView_loadingMessage);
            if (TextUtils.isEmpty(loadingMessage)) {
                loadingMessage = defaultLoadingMsg;
            }
            loadingTextColour = attributes.getColor(R.styleable.LoadingView_loadingMessageColor, defaultForeGroundColor);
            progressBarColour = attributes.getColor(R.styleable.LoadingView_progressBarColor, defaultForeGroundColor);

            attributes.recycle();
        } else {
            loadingMessage = defaultLoadingMsg;
            progressBarColour = defaultForeGroundColor;
            loadingTextColour = defaultForeGroundColor;
        }
    }


    public void setLoadingMessage(@NonNull CharSequence msg) {
        loadingTextView.setText(msg);
    }

    public void setProgressBarColor(int color) {
        Drawable drawable = progressBar.getIndeterminateDrawable();
        if (drawable != null) {
            DrawableCompat.setTint(drawable, color);
        }
    }

    public void setLoadingMessageColor(int color) {
        loadingTextView.setTextColor(color);
    }

    public void show() {
        loading = true;
        ViewUtil.hideSoftKeyboard(getContext(), this);
        setVisibility(VISIBLE);
    }

    public void show(boolean show) {
        if (show) {
            show();
        } else {
            hide();
        }
    }

    public void hide() {
        loading = false;
        setVisibility(GONE);
    }

    public boolean isLoading() {
        return loading;
    }
}
