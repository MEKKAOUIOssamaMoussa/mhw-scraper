package com.scraper.extractor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scraper.model.*;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a Mobalytics build guide page into a structured {@link BuildPage}.
 *
 * Data is pulled from two sources within the same HTML document:
 *   - JSON-LD script tag: page metadata (title, dates, favorites, URL)
 *   - window.__PRELOADED_STATE__: full build data (variants, equipment, skills)
 */
public class BuildExtractor {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PRELOADED_STATE_KEY = "window.__PRELOADED_STATE__=";

    public BuildPage extract(Document doc) throws IOException {
        JsonNode jsonLd = extractJsonLd(doc);
        String datePublished = "";
        String dateModified  = "";
        int favorites        = 0;
        String sourceUrl     = "";

        if (jsonLd != null && jsonLd.isArray()) {
            for (JsonNode node : jsonLd) {
                if ("CreativeWork".equals(node.path("@type").asText())) {
                    datePublished = node.path("datePublished").asText();
                    dateModified  = node.path("dateModified").asText();
                    favorites     = node.path("interactionStatistic")
                                       .path("userInteractionCount")
                                       .asInt(0);
                    sourceUrl     = node.path("url").asText();
                }
            }
        }

        JsonNode state = MAPPER.readTree(extractPreloadedStateJson(doc));

        // Path through the Apollo GraphQL cache to the build document
        JsonNode queries = state.at("/mhwState/apollo/graphqlV2/queries");
        if (queries.isMissingNode() || queries.size() < 2) {
            throw new IllegalStateException(
                "Expected at least 2 GraphQL queries in __PRELOADED_STATE__, found: " +
                (queries.isMissingNode() ? "none" : queries.size())
            );
        }

        JsonNode ugdNode = queries.at("/1/state/data/0/game/documents/userGeneratedDocumentBySlug/data");
        if (ugdNode.isMissingNode()) {
            throw new IllegalStateException(
                "userGeneratedDocumentBySlug not found — the page structure may have changed."
            );
        }

        String slug      = ugdNode.path("slugifiedName").asText();
        String title     = ugdNode.path("data").path("name").asText();
        String weaponType = "";
        for (JsonNode tag : ugdNode.path("tags").path("data")) {
            if ("weapon".equals(tag.path("groupSlug").asText())) {
                weaponType = tag.path("slug").asText();
                break;
            }
        }

        if (favorites == 0) {
            favorites = ugdNode.path("favorites").path("favoritesCount").asInt(0);
        }

        List<BuildVariant> variants = new ArrayList<>();
        for (JsonNode variantNode : ugdNode.path("data").path("buildVariants").path("values")) {
            variants.add(parseVariant(variantNode));
        }

        return new BuildPage(title, slug, weaponType, datePublished, dateModified,
                             favorites, sourceUrl, variants);
    }

    private JsonNode extractJsonLd(Document doc) throws IOException {
        Element scriptTag = doc.selectFirst("script[type=application/ld+json]");
        if (scriptTag == null) return null;
        return MAPPER.readTree(scriptTag.data()).path("@graph");
    }

    /**
     * Extracts the raw JSON string from window.__PRELOADED_STATE__=...;
     * Uses indexOf + substring rather than regex to avoid backtracking on a 200KB+ blob.
     */
    private String extractPreloadedStateJson(Document doc) {
        for (Element script : doc.select("script")) {
            String content = script.data();
            int keyIndex = content.indexOf(PRELOADED_STATE_KEY);
            if (keyIndex == -1) continue;

            String json = content.substring(keyIndex + PRELOADED_STATE_KEY.length()).trim();
            if (json.endsWith(";")) json = json.substring(0, json.length() - 1);
            return json;
        }
        throw new IllegalStateException(
            "window.__PRELOADED_STATE__ not found — the page may not have fully rendered."
        );
    }

    private BuildVariant parseVariant(JsonNode variant) {
        JsonNode eq = variant.path("equipment");
        return new BuildVariant(
            variant.path("id").asText(),
            parseWeapon(eq.path("weapon")),
            parseArmor(eq.path("head")),
            parseArmor(eq.path("chest")),
            parseArmor(eq.path("arms")),
            parseArmor(eq.path("waist")),
            parseArmor(eq.path("legs")),
            parseArmor(eq.path("charm")),
            parseArmor(eq.path("mantle")),
            parseSkillList(eq.path("skills")),
            parseBonusList(eq.path("setBonuses")),
            parseBonusList(eq.path("groupSkills"))
        );
    }

    private WeaponSlot parseWeapon(JsonNode w) {
        if (w.isMissingNode() || w.isNull()) return null;

        List<Integer> slots = new ArrayList<>();
        for (JsonNode s : w.path("slots")) slots.add(s.asInt());

        return new WeaponSlot(
            w.path("name").asText(),
            w.path("slug").asText(),
            w.path("type").asText(),
            w.path("attack").asInt(),
            w.path("affinity").asInt(),
            nullableText(w.path("element")),
            nullableText(w.path("ailment")),
            parseIntArray(w.path("sharpness")),
            slots,
            parseDecorations(w.path("decorations")),
            w.path("isArtian").asBoolean(false)
        );
    }

    private ArmorSlot parseArmor(JsonNode a) {
        if (a.isMissingNode() || a.isNull()) return null;

        return new ArmorSlot(
            a.path("name").asText(),
            a.path("slug").asText(),
            parseIntArray(a.path("defense")),
            a.path("fireResist").asInt(),
            a.path("waterResist").asInt(),
            a.path("thunderResist").asInt(),
            a.path("iceResist").asInt(),
            a.path("dragonResist").asInt(),
            parseArmorSkillList(a.path("skills")),
            parseDecorations(a.path("decorations"))
        );
    }

    // Aggregate skills list — level is null in the source JSON (sum of per-piece values)
    private List<SkillEntry> parseSkillList(JsonNode skillsNode) {
        List<SkillEntry> result = new ArrayList<>();
        for (JsonNode s : skillsNode) {
            result.add(new SkillEntry(
                s.path("name").asText(),
                s.path("slug").asText(),
                s.path("level").isNull() ? null : s.path("level").asInt(),
                s.path("maxLevel").asInt()
            ));
        }
        return result;
    }

    // Per-armor-piece skills — only slug + level are present at this level
    private List<SkillEntry> parseArmorSkillList(JsonNode skillsNode) {
        List<SkillEntry> result = new ArrayList<>();
        for (JsonNode s : skillsNode) {
            result.add(new SkillEntry("", s.path("slug").asText(), s.path("level").asInt(), 0));
        }
        return result;
    }

    private List<SkillEntry> parseBonusList(JsonNode bonusNode) {
        List<SkillEntry> result = new ArrayList<>();
        for (JsonNode s : bonusNode) {
            result.add(new SkillEntry(
                s.path("name").asText(),
                s.path("slug").asText(),
                s.path("level").isNull() ? null : s.path("level").asInt(),
                0
            ));
        }
        return result;
    }

    private List<Decoration> parseDecorations(JsonNode decsNode) {
        List<Decoration> result = new ArrayList<>();
        for (JsonNode d : decsNode) {
            if (d.isNull()) continue;
            result.add(new Decoration(d.path("name").asText(), d.path("slug").asText(), d.path("level").asInt()));
        }
        return result;
    }

    private int[] parseIntArray(JsonNode arrayNode) {
        if (arrayNode.isMissingNode() || arrayNode.isNull()) return new int[0];
        int[] arr = new int[arrayNode.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = arrayNode.get(i).asInt();
        return arr;
    }

    private String nullableText(JsonNode node) {
        return (node.isNull() || node.isMissingNode()) ? null : node.asText();
    }
}
