package com.example.voteapp.ui;

import android.Manifest;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voteapp.R;
import com.example.voteapp.data.BroadcastMessageEntity;
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

public class BroadcastFragment extends Fragment {

    private static final int PERMISSION_SMS = 101;
    private static final String SMS_SENT_ACTION = "SMS_SENT_ACTION";
    private static final String TAG = "BroadcastFragment";

    private DatabaseReference mDatabase;
    private BroadcastListAdapter adapter;
    private BroadcastReceiver sentReceiver;

    public BroadcastFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_broadcast, container, false);

        mDatabase = FirebaseDatabase.getInstance().getReference().child("broadcasts");

        TextInputEditText editTitle = root.findViewById(R.id.editBroadcastTitle);
        TextInputEditText editMessage = root.findViewById(R.id.editBroadcastMessage);
        TextInputEditText editPhone = root.findViewById(R.id.editBroadcastPhone);
        MaterialButton buttonSend = root.findViewById(R.id.buttonSendBroadcast);
        
        RecyclerView recyclerView = root.findViewById(R.id.recyclerBroadcasts);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new BroadcastListAdapter();
        recyclerView.setAdapter(adapter);

        boolean isAdmin = SessionManager.isAdmin(requireContext());
        if (!isAdmin) {
            if (buttonSend != null) buttonSend.setVisibility(View.GONE);
        } else {
            buttonSend.setOnClickListener(v -> {
                String title = editTitle.getText() != null ? editTitle.getText().toString().trim() : "";
                String message = editMessage.getText() != null ? editMessage.getText().toString().trim() : "";
                String phones = editPhone.getText() != null ? editPhone.getText().toString().trim() : "";

                if (TextUtils.isEmpty(title) || TextUtils.isEmpty(message)) {
                    Toast.makeText(requireContext(), "Enter title and message", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                sendBroadcast(title, message, phones);
                editTitle.setText("");
                editMessage.setText("");
                editPhone.setText("");
            });
        }

        loadBroadcastsFromFirebase();

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        registerSmsReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterSmsReceiver();
    }

    private void registerSmsReceiver() {
        sentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (SMS_SENT_ACTION.equals(intent.getAction())) {
                    switch (getResultCode()) {
                        case android.app.Activity.RESULT_OK:
                            Toast.makeText(context, "SMS Sent Successfully", Toast.LENGTH_SHORT).show();
                            break;
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            Toast.makeText(context, "Generic failure", Toast.LENGTH_SHORT).show();
                            break;
                        case SmsManager.RESULT_ERROR_NO_SERVICE:
                            Toast.makeText(context, "No service", Toast.LENGTH_SHORT).show();
                            break;
                        case SmsManager.RESULT_ERROR_NULL_PDU:
                            Toast.makeText(context, "Null PDU", Toast.LENGTH_SHORT).show();
                            break;
                        case SmsManager.RESULT_ERROR_RADIO_OFF:
                            Toast.makeText(context, "Radio off", Toast.LENGTH_SHORT).show();
                            break;
                        default:
                            Toast.makeText(context, "SMS Failed to send", Toast.LENGTH_SHORT).show();
                            break;
                    }
                }
            }
        };

        IntentFilter filter = new IntentFilter(SMS_SENT_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(sentReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            requireContext().registerReceiver(sentReceiver, filter);
        }
    }

    private void unregisterSmsReceiver() {
        if (sentReceiver != null) {
            try {
                requireContext().unregisterReceiver(sentReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Unregister receiver failed", e);
            }
            sentReceiver = null;
        }
    }

    private void sendBroadcast(String title, String message, String phones) {
        DatabaseReference newMsgRef = mDatabase.push();
        BroadcastMessageEntity msg = new BroadcastMessageEntity(title, message, System.currentTimeMillis());
        newMsgRef.setValue(msg);

        if (!TextUtils.isEmpty(phones)) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.SEND_SMS}, PERMISSION_SMS);
            } else {
                sendSms(phones, title + ": " + message);
            }
        }
    }

    private void sendSms(String phones, String text) {
        String[] numbers = phones.split(",");

        SmsManager smsManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            smsManager = requireContext().getSystemService(SmsManager.class);
        } else {
            smsManager = SmsManager.getDefault();
        }

        if (smsManager == null) {
            Toast.makeText(requireContext(), "SMS Manager not available", Toast.LENGTH_SHORT).show();
            return;
        }

        PendingIntent sentPI = PendingIntent.getBroadcast(requireContext(), 0, new Intent(SMS_SENT_ACTION),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        for (String num : numbers) {
            String cleanNum = num.trim();
            if (!cleanNum.isEmpty()) {
                try {
                    ArrayList<String> parts = smsManager.divideMessage(text);
                    ArrayList<PendingIntent> sentIntents = new ArrayList<>();
                    for (int i = 0; i < parts.size(); i++) {
                        sentIntents.add(sentPI);
                    }
                    smsManager.sendMultipartTextMessage(cleanNum, null, parts, sentIntents, null);
                    Log.d(TAG, "Queued SMS to: " + cleanNum);
                } catch (Exception e) {
                    Log.e(TAG, "Error sending SMS to: " + cleanNum, e);
                    Toast.makeText(requireContext(), "Error queueing SMS to " + cleanNum, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void loadBroadcastsFromFirebase() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<BroadcastMessageEntity> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    BroadcastMessageEntity msg = child.getValue(BroadcastMessageEntity.class);
                    if (msg != null) list.add(msg);
                }
                adapter.submitList(list);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_SMS && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(requireContext(), "Permission granted. Try sending again.", Toast.LENGTH_SHORT).show();
        }
    }
}
