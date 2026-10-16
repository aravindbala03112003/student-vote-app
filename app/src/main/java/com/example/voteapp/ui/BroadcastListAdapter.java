package com.example.voteapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voteapp.R;
import com.example.voteapp.data.BroadcastMessageEntity;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BroadcastListAdapter extends RecyclerView.Adapter<BroadcastListAdapter.ViewHolder> {

    private final List<BroadcastMessageEntity> items = new ArrayList<>();

    public void submitList(List<BroadcastMessageEntity> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_broadcast, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BroadcastMessageEntity item = items.get(position);
        holder.title.setText(item.title);
        holder.message.setText(item.message);
        String date = DateFormat.getDateTimeInstance().format(new Date(item.createdAt));
        holder.time.setText(date);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        TextView message;
        TextView time;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.textBroadcastTitle);
            message = itemView.findViewById(R.id.textBroadcastMessage);
            time = itemView.findViewById(R.id.textBroadcastTime);
        }
    }
}
