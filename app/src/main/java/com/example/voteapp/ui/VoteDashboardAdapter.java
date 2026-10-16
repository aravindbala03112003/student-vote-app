package com.example.voteapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voteapp.R;
import com.example.voteapp.data.CandidateEntity;
import com.example.voteapp.data.VoteEntity;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class VoteDashboardAdapter extends RecyclerView.Adapter<VoteDashboardAdapter.VoteViewHolder> {

    public static class VoteRow {
        public final VoteEntity vote;
        public final String gender;

        public VoteRow(VoteEntity vote, String gender) {
            this.vote = vote;
            this.gender = gender;
        }
    }

    private final List<VoteRow> items = new ArrayList<>();

    public void submitList(List<VoteRow> list) {
        items.clear();
        if (list != null) {
            items.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_vote_dashboard, parent, false);
        return new VoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VoteViewHolder holder, int position) {
        VoteRow row = items.get(position);
        VoteEntity v = row.vote;
        holder.textStudent.setText("Student: " + v.studentId);
        holder.textCandidate.setText("Candidate: " + v.candidateName);
        String time = DateFormat.getDateTimeInstance().format(new Date(v.timestamp));
        holder.textTime.setText("Time: " + time);

        if (v.feedback != null && !v.feedback.isEmpty()) {
            holder.textFeedback.setVisibility(View.VISIBLE);
            holder.textFeedback.setText("Feedback: " + v.feedback);
        } else {
            holder.textFeedback.setVisibility(View.GONE);
        }

        int iconRes = CandidateEntity.GENDER_FEMALE.equals(row.gender)
                ? R.drawable.ic_candidate_female
                : R.drawable.ic_candidate_male;
        holder.imageGender.setImageResource(iconRes);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VoteViewHolder extends RecyclerView.ViewHolder {
        final TextView textStudent;
        final TextView textCandidate;
        final TextView textTime;
        final TextView textFeedback;
        final ImageView imageGender;

        VoteViewHolder(@NonNull View itemView) {
            super(itemView);
            textStudent = itemView.findViewById(R.id.textVoteStudent);
            textCandidate = itemView.findViewById(R.id.textVoteCandidate);
            textTime = itemView.findViewById(R.id.textVoteTime);
            textFeedback = itemView.findViewById(R.id.textVoteFeedback);
            imageGender = itemView.findViewById(R.id.imageVoteDashboardGender);
        }
    }
}
