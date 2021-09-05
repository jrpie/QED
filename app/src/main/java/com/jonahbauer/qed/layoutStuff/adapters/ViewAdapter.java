package com.jonahbauer.qed.layoutStuff.adapters;

import android.view.View;

import androidx.databinding.BindingAdapter;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ViewAdapter {
    @BindingAdapter("android:visibility")
    public static void bindVisibility(View view, int visibility) {
        view.setVisibility(visibility);
    }
}
