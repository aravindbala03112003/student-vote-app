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
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class VoteCandidateAdapter extends RecyclerView.Adapter<VoteCandidateAdapter.ViewHolder> {

    public interface OnVoteClickListener {
        void onVoteClicked(CandidateEntity candidate);
    }

    public static class Row {
        public final CandidateEntity candidate;
        public final int voteCount;

        public Row(CandidateEntity candidate, int voteCount) {
            this.candidate = candidate;
            this.voteCount = voteCount;
        }
    }

    private final boolean isAdmin;
    private final OnVoteClickListener listener;
    private final List<Row> items = new ArrayList<>();

    public VoteCandidateAdapter(boolean isAdmin, OnVoteClickListener listener) {
        this.isAdmin = isAdmin;
        this.listener = listener;
    }

    public void submitList(List<Row> rows) {
        items.clear();
        if (rows != null) {
            items.addAll(rows);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vote_candidate, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Row row = items.get(position);
        CandidateEntity c = row.candidate;

        holder.textName.setText(c.name);

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

        if (CandidateEntity.GENDER_FEMALE.equalsIgnoreCase(c.gender)) {
            holder.imageGender.setImageResource(R.drawable.ic_candidate_female);
            holder.imageGender.setColorFilter(Color.RED);
        } else {
            holder.imageGender.setImageResource(R.drawable.ic_candidate_male);
            holder.imageGender.setColorFilter(Color.BLUE);
        }

        if (isAdmin) {
            holder.textVotes.setVisibility(View.VISIBLE);
            holder.textVotes.setText("Votes: " + row.voteCount);
            holder.buttonVote.setEnabled(false);
            holder.buttonVote.setText("Admin cannot vote");
            holder.buttonVote.setOnClickListener(null);
        } else {
            holder.textVotes.setVisibility(View.GONE);
            holder.buttonVote.setEnabled(true);
            holder.buttonVote.setText("Vote for " + c.name);
            holder.buttonVote.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onVoteClicked(c);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textName;
        final TextView textDetails;
        final TextView textVotes;
        final MaterialButton buttonVote;
        final ImageView imageGender;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textVoteCandidateName);
            textDetails = itemView.findViewById(R.id.textVoteCandidateDetails);
            textVotes = itemView.findViewById(R.id.textVoteCandidateVotes);
            buttonVote = itemView.findViewById(R.id.buttonVoteCandidate);
            imageGender = itemView.findViewById(R.id.imageVoteCandidateGender);
        }
    }
}
