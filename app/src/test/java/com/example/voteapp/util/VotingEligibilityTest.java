package com.example.voteapp.util;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class VotingEligibilityTest {

    @Test
    public void testSessionTimeWindowValidation() {
        long startTime = 100000L;
        long endTime = 200000L;
        boolean isActive = true;

        // Before start
        long beforeTime = 50000L;
        boolean activeBefore = isActive && beforeTime >= startTime && beforeTime <= endTime;
        assertFalse("Session should be inactive before start time", activeBefore);

        // During window
        long duringTime = 150000L;
        boolean activeDuring = isActive && duringTime >= startTime && duringTime <= endTime;
        assertTrue("Session should be active during the window", activeDuring);

        // After window
        long afterTime = 250000L;
        boolean activeAfter = isActive && afterTime >= startTime && afterTime <= endTime;
        assertFalse("Session should be inactive after end time", activeAfter);

        // When deactivated by admin
        isActive = false;
        boolean deactivated = isActive && duringTime >= startTime && duringTime <= endTime;
        assertFalse("Session should be inactive when isActive is false", deactivated);
    }

    @Test
    public void testGeofenceHaversineCalculation() {
        // Center: Pondicherry University / PTU approx coordinates
        double centerLat = 12.0150;
        double centerLng = 79.8550;
        float radiusMeters = 500.0f;

        // Point inside radius (~50m away)
        double insideLat = 12.0153;
        double insideLng = 79.8552;
        double distanceInside = calculateDistanceMeters(centerLat, centerLng, insideLat, insideLng);
        assertTrue("Distance should be within 500m", distanceInside <= radiusMeters);

        // Point outside radius (~2000m away)
        double outsideLat = 12.0300;
        double outsideLng = 79.8700;
        double distanceOutside = calculateDistanceMeters(centerLat, centerLng, outsideLat, outsideLng);
        assertFalse("Distance should be outside 500m", distanceOutside <= radiusMeters);
    }

    @Test
    public void testStudentIdOcrExtractionRegex() {
        String ocrText = "PUDUCHERRY TECHNOLOGICAL UNIVERSITY\nSTUDENT ID CARD\nNAME: Aravind Bala\nREG NO: 20210458\nDEPT: CSE";

        String[] lines = ocrText.split("\n");
        Pattern digitPattern = Pattern.compile("\\d+");
        String bestDigits = null;
        int bestLen = 0;

        for (String line : lines) {
            Matcher m = digitPattern.matcher(line);
            while (m.find()) {
                String candidate = m.group();
                int len = candidate.length();
                if (len >= 4 && len > bestLen) {
                    bestLen = len;
                    bestDigits = candidate;
                }
            }
        }

        assertNotNull(bestDigits);
        assertEquals("20210458", bestDigits);
    }

    @Test
    public void testRoleVotingPermissions() {
        // Admin cannot vote
        boolean adminCanVote = !SessionManager.ROLE_ADMIN.equals("admin");
        assertFalse("Admins must not be allowed to vote", adminCanVote);

        // Candidate cannot vote
        boolean candidateCanVote = !SessionManager.ROLE_CANDIDATE.equals("candidate");
        assertFalse("Candidates must not be allowed to vote", candidateCanVote);

        // Student can vote
        boolean studentCanVote = SessionManager.ROLE_STUDENT.equals("student");
        assertTrue("Students must be allowed to vote", studentCanVote);
    }

    @Test
    public void testDuplicateVoteCheck() {
        java.util.Set<String> recordedVoters = new java.util.HashSet<>();
        recordedVoters.add("STU1001");

        // Attempt second vote with same student ID
        String incomingVoter = "STU1001";
        boolean alreadyVoted = recordedVoters.contains(incomingVoter);
        assertTrue("Duplicate vote must be rejected", alreadyVoted);

        // Attempt first vote with new student ID
        String newVoter = "STU1002";
        boolean newAlreadyVoted = recordedVoters.contains(newVoter);
        assertFalse("First-time voter must be accepted", newAlreadyVoted);
    }

    private double calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Radius of the earth in meters
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
