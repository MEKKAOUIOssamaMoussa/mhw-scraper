package com.scraper.model;

/**
 * A skill entry. Used in three contexts:
 *   - Aggregate build total (equipment.skills): level is null, filled by summing per-piece values
 *   - Per-armor-piece intrinsic skill: name and maxLevel are empty
 *   - Set bonus / group skill: maxLevel is 0
 */
public record SkillEntry(
        String name,
        String slug,
        Integer level,
        int maxLevel
) {}
