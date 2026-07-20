package com.scraper.model;

import java.util.List;

/**
 * One complete equipment loadout from a build guide.
 * A single guide can have multiple variants (e.g. different rank tiers or playstyle options).
 */
public record BuildVariant(
        String id,
        WeaponSlot weapon,
        ArmorSlot head,
        ArmorSlot chest,
        ArmorSlot arms,
        ArmorSlot waist,
        ArmorSlot legs,
        ArmorSlot charm,
        ArmorSlot mantle,
        List<SkillEntry> skills,
        List<SkillEntry> setBonuses,
        List<SkillEntry> groupSkills
) {}
