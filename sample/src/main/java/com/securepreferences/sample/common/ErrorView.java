package com.securepreferences.sample.common;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.widget.LinearLayout;

import com.securepreferences.sample.R;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class ErrorView extends LinearLayout {

    private ErrorListener errorListener;

    public ErrorView(Context context) {
        super(context);
        init();
    }

    public ErrorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ErrorView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ErrorView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setOrientation(VERTICAL);
        setGravity(Gravity.CENTER);
        LayoutInflater.from(getContext()).inflate(R.layout.view_error, this);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.button_reload)
    public void onReloadButtonClick() {
        if (errorListener != null) {
            errorListener.onReloadData();
        }
    }

    public void setErrorListener(ErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    public interface ErrorListener {
        void onReloadData();
    }
}
