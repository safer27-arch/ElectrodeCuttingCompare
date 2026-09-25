package com.example.electrodecutcompare;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Button;

public class LocalizedButton extends Button {
    public LocalizedButton(Context c){super(c);}
    public LocalizedButton(Context c, AttributeSet a){super(c,a);}
    public LocalizedButton(Context c, AttributeSet a,int s){super(c,a,s);}
    @Override public void setText(CharSequence text, BufferType type){super.setText(LanguageManager.t(getContext(),text),type);}
}
