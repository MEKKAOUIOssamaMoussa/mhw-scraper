package com.scraper.model;

import java.util.List;

/**
 * Equipped weapon stat block.
 *
 * sharpness: 7-element array indexed by color tier [Red, Orange, Yellow, Green, Blue, White, Purple].
 * element/ailment: null for raw weapons.
 */
public record WeaponSlot(
        String name,
        String slug,
        String type,
        int attack,
        int affinity,
        String element,
        String ailment,
        int[] sharpness,
        List<Integer> slots,
        List<Decoration> decorations,
        List<SkillEntry> skills,
        boolean isArtian
) {}
