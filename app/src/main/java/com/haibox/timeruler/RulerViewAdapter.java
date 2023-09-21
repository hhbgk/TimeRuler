package com.haibox.timeruler;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RulerViewAdapter extends RecyclerView.Adapter<RulerViewAdapter.ViewHolder> {
    private final List<String> mDataList = new ArrayList<>();
    private OnItemClickListener<String> mOnItemClickListener;

    public void setOnItemClickListener(OnItemClickListener<String> listener) {
        mOnItemClickListener = listener;
    }

    public void addData(List<String> dataList) {
        if (dataList != null) {
            mDataList.addAll(dataList);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_ruler_view, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String classname = mDataList.get(position);
        int index = classname.lastIndexOf(".");
        if (index > 0) classname = classname.substring(index + 1);
        holder.tvTitle.setText(classname);

        if (mOnItemClickListener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getLayoutPosition();
                    mOnItemClickListener.onItemClick(v, mDataList.get(pos), pos);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mDataList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
        }
    }
}
