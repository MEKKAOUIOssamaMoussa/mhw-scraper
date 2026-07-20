package com.scraper.model;

import java.util.List;

/**
 * Top-level result of scraping one Mobalytics build guide page.
 * Metadata is merged from JSON-LD (dates, favorites) and __PRELOADED_STATE__ (variants, weapon type).
 */
public record BuildPage(
        String title,
        String slug,
        String weaponType,
        String datePublished,
        String dateModified,
        int favorites,
        String sourceUrl,
        List<BuildVariant> variants
) {}
