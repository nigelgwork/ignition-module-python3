package com.inductiveautomation.ignition.examples.python3.designer;

/**
 * Represents Gateway performance impact assessment from Python 3 module.
 * Contains impact level classification and overall health score.
 */
public class GatewayImpact {
    private String impactLevel;      // LOW, MODERATE, HIGH, CRITICAL
    private int healthScore;         // 0-100
    private String recommendation;

    public GatewayImpact() {
    }

    public GatewayImpact(String impactLevel, int healthScore, String recommendation) {
        this.impactLevel = impactLevel;
        this.healthScore = healthScore;
        this.recommendation = recommendation;
    }

    public String getImpactLevel() {
        return impactLevel;
    }

    public void setImpactLevel(String impactLevel) {
        this.impactLevel = impactLevel;
    }

    public int getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(int healthScore) {
        this.healthScore = healthScore;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    @Override
    public String toString() {
        return "GatewayImpact{" +
                "impactLevel='" + impactLevel + '\'' +
                ", healthScore=" + healthScore +
                ", recommendation='" + recommendation + '\'' +
                '}';
    }
}
