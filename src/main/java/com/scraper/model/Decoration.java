package com.scraper.model;

import java.util.List;

/** A decoration (gem) socketed into a weapon or armor slot. */
public record Decoration(
        String name,
        String slug,
        int level,
        List<SkillEntry> skills
) {}
