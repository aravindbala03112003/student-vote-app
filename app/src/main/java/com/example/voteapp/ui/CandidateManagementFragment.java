package com.example.voteapp.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voteapp.R;
import com.example.voteapp.data.CandidateEntity;
import com.example.voteapp.data.UserEntity;
import com.example.voteapp.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CandidateManagementFragment extends Fragment {

    private DatabaseReference mDatabase;
    private CandidateListAdapter activeAdapter;
    private PendingAdapter pendingAdapter;
    private TextView textNoPending;

    public CandidateManagementFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_candidates, container, false);

        if (!SessionManager.isAdmin(requireContext())) {
            return root;
        }

        mDatabase = FirebaseDatabase.getInstance().getReference();

        textNoPending = root.findViewById(R.id.textNoPending);
        RecyclerView recyclerPending = root.findViewById(R.id.recyclerPendingCandidates);
        RecyclerView recyclerActive = root.findViewById(R.id.recyclerCandidates);
        
        TextInputEditText editName = root.findViewById(R.id.editCandidateName);
        TextInputEditText editEmail = root.findViewById(R.id.editCandidateEmail);
        TextInputEditText editPhone = root.findViewById(R.id.editCandidatePhone);
        TextInputEditText editDept = root.findViewById(R.id.editCandidateDepartment);
        TextInputEditText editYear = root.findViewById(R.id.editCandidateYear);
        TextInputEditText editParty = root.findViewById(R.id.editCandidateParty);
        RadioGroup radioGenderGroup = root.findViewById(R.id.radioGenderGroup);
        MaterialButton buttonCreate = root.findViewById(R.id.buttonCandidateCreate);

        recyclerPending.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerActive.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        pendingAdapter = new PendingAdapter();
        recyclerPending.setAdapter(pendingAdapter);
        
        activeAdapter = new CandidateListAdapter(this::confirmDeleteCandidate);
        recyclerActive.setAdapter(activeAdapter);

        loadPendingRequests();
        loadActiveCandidates();

        buttonCreate.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String dept = editDept.getText().toString().trim();
            String party = editParty.getText().toString().trim();
            String phone = editPhone.getText().toString().trim();

            String gender = "Male";
            if (radioGenderGroup != null) {
                int checkedId = radioGenderGroup.getCheckedRadioButtonId();
                if (checkedId != -1) {
                    RadioButton rb = root.findViewById(checkedId);
                    if (rb != null && rb.getTag() != null) {
                        gender = rb.getTag().toString();
                    }
                }
            }

            if (!TextUtils.isEmpty(name)) {
                addCandidateToActiveList(name, dept, party.isEmpty() ? "Independent" : party, gender, phone);

                editName.setText("");
                editDept.setText("");
                if (editEmail != null) editEmail.setText("");
                if (editPhone != null) editPhone.setText("");
                if (editYear != null) editYear.setText("");
                if (editParty != null) editParty.setText("");
                if (radioGenderGroup != null) radioGenderGroup.check(R.id.radioMale);
            } else {
                Toast.makeText(requireContext(), "Enter name to add", Toast.LENGTH_SHORT).show();
            }
        });

        return root;
    }

    private void loadPendingRequests() {
        mDatabase.child("users").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<UserEntity> pending = new ArrayList<>();
                List<String> uids = new ArrayList<>();
                
                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        String role = child.child("role").getValue(String.class);
                        String status = child.child("status").getValue(String.class);
                        
                        if ("candidate".equalsIgnoreCase(role) && "pending".equalsIgnoreCase(status)) {
                            UserEntity user = child.getValue(UserEntity.class);
                            if (user != null) {
                                pending.add(user);
                                uids.add(child.getKey());
                            }
                        }
                    } catch (Exception e) {
                        Log.e("VoteApp", "Error parsing user: " + child.getKey(), e);
                    }
                }
                
                if (getActivity() == null) return;

                if (pending.isEmpty()) {
                    textNoPending.setText("No new candidate requests at the moment.");
                    textNoPending.setVisibility(View.VISIBLE);
                } else {
                    textNoPending.setVisibility(View.GONE);
                }
                
                pendingAdapter.setData(pending, uids);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) Toast.makeText(getContext(), "Database Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadActiveCandidates() {
        mDatabase.child("candidates").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<CandidateEntity> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    CandidateEntity c = child.getValue(CandidateEntity.class);
                    if (c != null) list.add(c);
                }
                activeAdapter.submitList(list);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void confirmDeleteCandidate(CandidateEntity candidate) {
        if (!isAdded() || getContext() == null || candidate == null) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Candidate")
                .setMessage("Are you sure you want to delete candidate " + candidate.name + " from the active list?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (candidate.firebaseId != null && !candidate.firebaseId.isEmpty()) {
                        mDatabase.child("candidates").child(candidate.firebaseId).removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    if (isAdded() && getContext() != null) {
                                        Toast.makeText(requireContext(), "Candidate removed", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (isAdded() && getContext() != null) {
                                        Toast.makeText(requireContext(), "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void approveCandidate(String uid, UserEntity user) {
        mDatabase.child("users").child(uid).child("status").setValue("approved")
                .addOnSuccessListener(aVoid -> {
                    addCandidateToActiveList(
                            user.name != null ? user.name : user.username, 
                            user.department != null ? user.department : "PTU", 
                            user.partyName != null ? user.partyName : "Registered", 
                            user.gender != null ? user.gender : "Male",
                            user.phoneNumber != null ? user.phoneNumber : "");
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(requireContext(), "Candidate approved and listed!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(requireContext(), "Approval failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void rejectCandidate(String uid) {
        mDatabase.child("users").child(uid).child("status").setValue("rejected")
                .addOnSuccessListener(aVoid -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(requireContext(), "Request rejected", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(requireContext(), "Rejection failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addCandidateToActiveList(String name, String dept, String party, String gender, String phone) {
        mDatabase.child("candidates").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int nextId = (int) snapshot.getChildrenCount() + 1;
                DatabaseReference newRef = mDatabase.child("candidates").push();
                CandidateEntity c = new CandidateEntity(name, dept, party, gender, phone);
                c.firebaseId = newRef.getKey();
                c.numericalId = nextId;
                newRef.setValue(c);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showDocumentsDialog(UserEntity user) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_view_docs, null);
        ImageView imageIdCard = dialogView.findViewById(R.id.imageDialogIdCard);
        ImageView imageMarksheet = dialogView.findViewById(R.id.imageDialogMarksheet);
        MaterialButton buttonClose = dialogView.findViewById(R.id.buttonCloseDialog);

        if (!TextUtils.isEmpty(user.idCardUrl)) {
            try {
                byte[] decoded = Base64.decode(user.idCardUrl, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                imageIdCard.setImageBitmap(bitmap);
            } catch (Exception e) {
                imageIdCard.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        }
        if (!TextUtils.isEmpty(user.marksheetUrl)) {
            try {
                byte[] decoded = Base64.decode(user.marksheetUrl, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                imageMarksheet.setImageBitmap(bitmap);
            } catch (Exception e) {
                imageMarksheet.setImageResource(android.R.drawable.ic_menu_report_image);
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Documents: " + (TextUtils.isEmpty(user.name) ? user.username : user.name))
                .setView(dialogView)
                .create();

        buttonClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private class PendingAdapter extends RecyclerView.Adapter<PendingAdapter.ViewHolder> {
        private List<UserEntity> users = new ArrayList<>();
        private List<String> uids = new ArrayList<>();

        void setData(List<UserEntity> users, List<String> uids) {
            this.users = users;
            this.uids = uids;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_candidate, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            UserEntity user = users.get(position);
            String uid = uids.get(position);
            
            holder.name.setText(TextUtils.isEmpty(user.name) ? user.username : user.name);
            
            String info = String.format("%s | Year %s\n%s", 
                    user.department != null ? user.department : "No Dept", 
                    user.year != null ? user.year : "-",
                    user.phoneNumber != null ? user.phoneNumber : "No Phone");
            holder.info.setText(info);
            
            holder.backlog.setText("Active Backlogs: " + (user.hasBacklog ? "YES" : "NO"));
            holder.backlog.setTextColor(user.hasBacklog ? 
                    getResources().getColor(android.R.color.holo_red_dark) : 
                    getResources().getColor(android.R.color.darker_gray));
            
            holder.btnViewDocs.setOnClickListener(v -> showDocumentsDialog(user));

            holder.btnApprove.setOnClickListener(v -> approveCandidate(uid, user));
            holder.btnReject.setOnClickListener(v -> rejectCandidate(uid));
        }

        @Override
        public int getItemCount() { return users.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, info, backlog;
            MaterialButton btnApprove, btnReject, btnViewDocs;
            ViewHolder(View v) {
                super(v);
                name = v.findViewById(R.id.textPendingName);
                info = v.findViewById(R.id.textPendingInfo);
                backlog = v.findViewById(R.id.textPendingBacklog);
                btnApprove = v.findViewById(R.id.buttonApprove);
                btnReject = v.findViewById(R.id.buttonReject);
                btnViewDocs = v.findViewById(R.id.buttonViewDocs);
            }
        }
    }
}
