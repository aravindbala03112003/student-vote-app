package com.example.voteapp.data;

import org.junit.Test;

import static org.junit.Assert.*;

public class DataEntitiesTest {

    @Test
    public void testUserEntityDefaultsAndConstructor() {
        UserEntity student = new UserEntity("john_doe", "pass123", "student", "John Doe", "CSE", "4");
        assertEquals("john_doe", student.username);
        assertEquals("student", student.role);
        assertEquals("John Doe", student.name);
        assertEquals("CSE", student.department);
        assertEquals("4", student.year);
        assertEquals("pending", student.status);
        assertFalse(student.hasBacklog);

        UserEntity candidate = new UserEntity("alice", "pass123", "candidate", "Alice Smith", "ECE", "3");
        candidate.status = "pending";
        candidate.hasBacklog = false;
        candidate.partyName = "Progressive Youth";
        candidate.gender = CandidateEntity.GENDER_FEMALE;

        assertEquals("candidate", candidate.role);
        assertEquals("pending", candidate.status);
        assertEquals("Progressive Youth", candidate.partyName);
        assertEquals("Female", candidate.gender);
    }

    @Test
    public void testCandidateEntity() {
        CandidateEntity c = new CandidateEntity("Bob Marley", "IT", "United Students", CandidateEntity.GENDER_MALE, "9876543210");
        c.firebaseId = "cand_123";
        c.numericalId = 1;

        assertEquals("cand_123", c.firebaseId);
        assertEquals(1, c.numericalId);
        assertEquals("Bob Marley", c.name);
        assertEquals("IT", c.department);
        assertEquals("United Students", c.partyName);
        assertEquals("Male", c.gender);
        assertEquals("9876543210", c.phoneNumber);
    }

    @Test
    public void testVoteEntity() {
        long now = System.currentTimeMillis();
        VoteEntity vote = new VoteEntity("STU1001", "Bob Marley", now);
        vote.latitude = 11.9345;
        vote.longitude = 79.8291;
        vote.feedback = "Strong leadership";

        assertEquals("STU1001", vote.studentId);
        assertEquals("Bob Marley", vote.candidateName);
        assertEquals(now, vote.timestamp);
        assertEquals(11.9345, vote.latitude, 0.0001);
        assertEquals(79.8291, vote.longitude, 0.0001);
        assertEquals("Strong leadership", vote.feedback);
    }

    @Test
    public void testGeofenceConfigEntity() {
        GeofenceConfigEntity config = new GeofenceConfigEntity(11.9345, 79.8291, 500f);
        config.startTime = 1000L;
        config.endTime = 2000L;
        config.isActive = true;

        assertEquals(11.9345, config.centerLat, 0.0001);
        assertEquals(79.8291, config.centerLng, 0.0001);
        assertEquals(500f, config.radiusMeters, 0.01f);
        assertEquals(1000L, config.startTime);
        assertEquals(2000L, config.endTime);
        assertTrue(config.isActive);
    }

    @Test
    public void testBroadcastMessageEntity() {
        long now = System.currentTimeMillis();
        BroadcastMessageEntity msg = new BroadcastMessageEntity("Election Started", "Voting is now open across all departments.", now);

        assertEquals("Election Started", msg.title);
        assertEquals("Voting is now open across all departments.", msg.message);
        assertEquals(now, msg.createdAt);
    }
}
