package com.haibox.timeruler;

import android.view.View;

public interface OnItemClickListener<T> {
    void onItemClick(View view, T t, int position);

//    void onItemLongClick(View view, T t, int position);
}
