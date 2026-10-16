package com.example.voteapp.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.voteapp.R;
import com.example.voteapp.data.VoteEntity;
import com.google.android.material.button.MaterialButton;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResultsFragment extends Fragment {

    private DatabaseReference mDatabase;
    private VoteDashboardAdapter dashboardAdapter;
    private List<CandidateReportRow> reportData = new ArrayList<>();

    private static class CandidateReportRow {
        String name, gender, dept, phone;
        int votes;
        CandidateReportRow(String n, String g, String d, String p, int v) {
            name = n; gender = g; dept = d; phone = p; votes = v;
        }
    }

    public ResultsFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_results, container, false);

        mDatabase = FirebaseDatabase.getInstance().getReference();

        TextView textSummary = root.findViewById(R.id.textResultsSummary);
        PieChart pieChart = root.findViewById(R.id.pieChartResults);
        BarChart barChart = root.findViewById(R.id.barChartResults);
        RecyclerView recyclerVotes = root.findViewById(R.id.recyclerVotes);
        MaterialButton buttonExport = root.findViewById(R.id.buttonExportResults);

        recyclerVotes.setLayoutManager(new LinearLayoutManager(requireContext()));
        dashboardAdapter = new VoteDashboardAdapter();
        recyclerVotes.setAdapter(dashboardAdapter);

        loadData(textSummary, pieChart, barChart);

        if (buttonExport != null) {
            buttonExport.setOnClickListener(v -> exportResultsAsPdf());
        }

        return root;
    }

    private void loadData(TextView textSummary, PieChart pieChart, BarChart barChart) {
        mDatabase.child("candidates").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot candidateSnapshot) {
                Map<String, CandidateReportRow> candidateMap = new HashMap<>();
                List<String> candidateNames = new ArrayList<>();
                
                for (DataSnapshot cSnap : candidateSnapshot.getChildren()) {
                    String name = cSnap.child("name").getValue(String.class);
                    String gender = cSnap.child("gender").getValue(String.class);
                    String dept = cSnap.child("department").getValue(String.class);
                    String phone = cSnap.child("phoneNumber").getValue(String.class);
                    if (name != null) {
                        candidateNames.add(name);
                        candidateMap.put(name, new CandidateReportRow(name, gender, dept, phone, 0));
                    }
                }

                mDatabase.child("votes").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot voteSnapshot) {
                        List<VoteDashboardAdapter.VoteRow> dashboardRows = new ArrayList<>();
                        int totalVotes = 0;

                        for (CandidateReportRow r : candidateMap.values()) r.votes = 0;

                        for (DataSnapshot vSnap : voteSnapshot.getChildren()) {
                            VoteEntity v = vSnap.getValue(VoteEntity.class);
                            if (v != null && v.candidateName != null) {
                                totalVotes++;
                                if (candidateMap.containsKey(v.candidateName)) {
                                    candidateMap.get(v.candidateName).votes++;
                                }
                                dashboardRows.add(new VoteDashboardAdapter.VoteRow(v, candidateMap.get(v.candidateName) != null ? candidateMap.get(v.candidateName).gender : "Male"));
                            }
                        }

                        reportData = new ArrayList<>(candidateMap.values());
                        updateCharts(candidateNames, candidateMap, totalVotes, textSummary, pieChart, barChart);
                        dashboardAdapter.submitList(dashboardRows);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateCharts(List<String> names, Map<String, CandidateReportRow> map, int total, 
                              TextView summaryView, PieChart pie, BarChart bar) {
        
        List<PieEntry> pieEntries = new ArrayList<>();
        List<BarEntry> barEntries = new ArrayList<>();
        StringBuilder summary = new StringBuilder("Total Votes: " + total + "\n\n");

        for (int i = 0; i < names.size(); i++) {
            String name = names.get(i);
            int count = map.containsKey(name) ? map.get(name).votes : 0;
            pieEntries.add(new PieEntry(count, name));
            barEntries.add(new BarEntry(i, count));
            summary.append(name).append(": ").append(count).append(" votes\n");
        }

        summaryView.setText(summary.toString());

        PieDataSet pieDataSet = new PieDataSet(pieEntries, "");
        pieDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        pie.setData(new PieData(pieDataSet));
        pie.getDescription().setEnabled(false);
        pie.animateY(800);

        BarDataSet barDataSet = new BarDataSet(barEntries, "Votes");
        barDataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        bar.setData(new BarData(barDataSet));
        bar.getDescription().setEnabled(false);
        bar.getXAxis().setGranularity(1f);
        bar.animateY(800);
    }

    private void exportResultsAsPdf() {
        if (reportData.isEmpty()) {
            Toast.makeText(requireContext(), "No data to export", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();
        
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(1200, 1600, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        titlePaint.setTextSize(40f);
        titlePaint.setFakeBoldText(true);
        canvas.drawText("Election Results Report", 400, 80, titlePaint);

        paint.setTextSize(25f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);

        int startX = 50, startY = 150, rowHeight = 60;
        int[] colWidths = {300, 150, 300, 250, 100};
        String[] headers = {"Name", "Gender", "Department", "Phone", "Votes"};

        for (int i = 0; i < headers.length; i++) {
            canvas.drawRect(startX, startY, startX + colWidths[i], startY + rowHeight, paint);
            canvas.drawText(headers[i], startX + 10, startY + 40, paint);
            startX += colWidths[i];
        }

        paint.setStyle(Paint.Style.FILL);
        int currentY = startY + rowHeight;
        for (CandidateReportRow row : reportData) {
            int currentX = 50;
            String[] data = {
                row.name != null ? row.name : "-",
                row.gender != null ? row.gender : "-",
                row.dept != null ? row.dept : "-",
                row.phone != null ? row.phone : "-",
                String.valueOf(row.votes)
            };
            for (int i = 0; i < data.length; i++) {
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawRect(currentX, currentY, currentX + colWidths[i], currentY + rowHeight, paint);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawText(data[i], currentX + 10, currentY + 40, paint);
                currentX += colWidths[i];
            }
            currentY += rowHeight;
        }

        pdfDocument.finishPage(page);

        File file = new File(requireContext().getExternalFilesDir(null), "Election_Results.pdf");
        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(requireContext(), "PDF Saved: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            
            Uri uri = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/pdf");
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(intent);
            } catch (android.content.ActivityNotFoundException e) {
                Toast.makeText(requireContext(), "PDF saved to device storage (no PDF viewer app found)", Toast.LENGTH_LONG).show();
            }
            
        } catch (IOException e) {
            Toast.makeText(requireContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        pdfDocument.close();
    }
}
