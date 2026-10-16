package com.example.voteapp.ui;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voteapp.R;
import com.example.voteapp.data.CandidateEntity;

import java.util.ArrayList;
import java.util.List;

public class CandidateListAdapter extends RecyclerView.Adapter<CandidateListAdapter.CandidateViewHolder> {

    public interface OnCandidateLongClickListener {
        void onCandidateLongClicked(CandidateEntity candidate);
    }

    private final List<CandidateEntity> items = new ArrayList<>();
    private final OnCandidateLongClickListener longClickListener;

    public CandidateListAdapter() {
        this(null);
    }

    public CandidateListAdapter(OnCandidateLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public void submitList(List<CandidateEntity> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CandidateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_candidate, parent, false);
        return new CandidateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CandidateViewHolder holder, int position) {
        CandidateEntity c = items.get(position);
        holder.textTitle.setText(c.name);
        String dep = c.department == null ? "" : c.department;
        String party = c.partyName == null ? "" : c.partyName;
        String details;
        if (!dep.isEmpty() && !party.isEmpty()) {
            details = dep + " | " + party;
        } else if (!dep.isEmpty()) {
            details = dep;
        } else if (!party.isEmpty()) {
            details = party;
        } else {
            details = "";
        }
        holder.textDetails.setText(details);
        
        // Show numerical ID instead of complex Firebase ID
        holder.textId.setText("ID: " + c.numericalId);
        
        if (CandidateEntity.GENDER_FEMALE.equalsIgnoreCase(c.gender)) {
            holder.imageGender.setImageResource(R.drawable.ic_candidate_female);
            holder.imageGender.setColorFilter(Color.RED);
        } else {
            holder.imageGender.setImageResource(R.drawable.ic_candidate_male);
            holder.imageGender.setColorFilter(Color.BLUE);
        }

        if (longClickListener != null) {
            holder.itemView.setOnLongClickListener(v -> {
                longClickListener.onCandidateLongClicked(c);
                return true;
            });
        } else {
            holder.itemView.setOnLongClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class CandidateViewHolder extends RecyclerView.ViewHolder {
        final TextView textTitle;
        final TextView textDetails;
        final TextView textId;
        final ImageView imageGender;

        CandidateViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textCandidateTitle);
            textDetails = itemView.findViewById(R.id.textCandidateDetails);
            textId = itemView.findViewById(R.id.textCandidateId);
            imageGender = itemView.findViewById(R.id.imageCandidateGender);
        }
    }
}
