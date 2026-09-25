package com.example.electrodecutcompare;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class LocalizedTextView extends TextView {
    public LocalizedTextView(Context c){super(c);}
    public LocalizedTextView(Context c, AttributeSet a){super(c,a);}
    public LocalizedTextView(Context c, AttributeSet a,int s){super(c,a,s);}
    @Override public void setText(CharSequence text, BufferType type){super.setText(LanguageManager.t(getContext(),text),type);}
}
