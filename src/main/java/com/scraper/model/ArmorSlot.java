package com.scraper.model;

import java.util.List;

/** One equipped armor piece with its defense, resistances, skills, and decorations. */
public record ArmorSlot(
        String name,
        String slug,
        int[] defense,
        int fireResist,
        int waterResist,
        int thunderResist,
        int iceResist,
        int dragonResist,
        List<SkillEntry> skills,
        List<Decoration> decorations
) {}
