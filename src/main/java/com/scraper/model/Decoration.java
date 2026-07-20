package com.scraper.model;

/** A decoration (gem) socketed into a weapon or armor slot. */
public record Decoration(
        String name,
        String slug,
        int level
) {}
