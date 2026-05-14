package com.niocess.perflab.client.db;

import jakarta.persistence.*;

@Entity
@Table(name = "risk_profiles")
public class RiskProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clientId;
    private int score;
    private String category;

    protected RiskProfile() {
    }

    RiskProfile(Long id, String clientId, int score, String category) {
        this.id = id;
        this.clientId = clientId;
        this.score = score;
        this.category = category;
    }

    public Long getId() { return id; }
    public String getClientId() { return clientId; }
    public int getScore() { return score; }
    public String getCategory() { return category; }
}
