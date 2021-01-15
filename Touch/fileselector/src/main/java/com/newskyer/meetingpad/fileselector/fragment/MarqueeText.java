package com.newskyer.meetingpad.fileselector.fragment;

import android.content.Context;
import android.util.AttributeSet;

public class MarqueeText extends androidx.appcompat.widget.AppCompatTextView {
    public MarqueeText(Context context) {
        super(context);
    }

    public MarqueeText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

    }

    public MarqueeText(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

    @Override
    public boolean isFocused() {
        return true;
    }
}