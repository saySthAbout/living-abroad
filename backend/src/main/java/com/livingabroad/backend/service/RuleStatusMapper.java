package com.livingabroad.backend.service;

/**
 * Bridges the AI server's RULE_STATUS codes (MET/NEEDS_IMPROVEMENT/NEEDS_CONFIRMATION,
 * per the API spec) to the DB's analysis_country_results.eligibility_status check
 * constraint (ELIGIBLE/CONDITIONAL/NOT_ELIGIBLE/UNKNOWN, per V1__init schema) and back.
 * The two enums were defined in separate docs and never reconciled.
 */
final class RuleStatusMapper {

    private RuleStatusMapper() {
    }

    static String toEligibilityStatus(String ruleStatus) {
        return switch (ruleStatus) {
            case "MET" -> "ELIGIBLE";
            case "NEEDS_IMPROVEMENT" -> "CONDITIONAL";
            case "NEEDS_CONFIRMATION" -> "UNKNOWN";
            default -> "UNKNOWN";
        };
    }

    static String toRuleStatus(String eligibilityStatus) {
        return switch (eligibilityStatus) {
            case "ELIGIBLE" -> "MET";
            case "CONDITIONAL" -> "NEEDS_IMPROVEMENT";
            case "NOT_ELIGIBLE" -> "NEEDS_IMPROVEMENT";
            default -> "NEEDS_CONFIRMATION";
        };
    }
}
