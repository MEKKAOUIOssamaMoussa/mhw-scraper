package com.scraper;

import com.scraper.extractor.BuildExtractor;
import com.scraper.model.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.File;
import java.util.List;

/**
 * Entry point for the MHW build scraper.
 *
 * Usage:
 *   mvn -q compile exec:java -Dexec.args="src/test/resources/long-sword.html"
 *   mvn -q compile exec:java   (live URL — currently blocked by Cloudflare)
 */
public class Main {

    public static void main(String[] args) {
        try {
            Document doc = loadDocument(args);
            BuildExtractor extractor = new BuildExtractor();
            BuildPage page = extractor.extract(doc);
            printBuildReport(page);
        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Document loadDocument(String[] args) throws Exception {
        if (args.length > 0) {
            File localFile = new File(args[0]);
            System.out.println("[MODE] Local file: " + localFile.getAbsolutePath());
            if (!localFile.exists()) {
                throw new IllegalArgumentException("File not found: " + localFile.getAbsolutePath());
            }
            return Jsoup.parse(localFile, "UTF-8", "https://mobalytics.gg");
        }

        String targetUrl = "https://mobalytics.gg/mhw/builds/endgame-long-sword-meta";
        System.out.println("[MODE] Live URL: " + targetUrl);
        return Jsoup.connect(targetUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .referrer("https://mobalytics.gg/mhw/builds/")
                .timeout(15000)
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .get();
    }

    private static void printBuildReport(BuildPage page) {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.printf( "║  %-60s║%n", page.title());
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.printf( "║  Weapon Type : %-46s║%n", page.weaponType());
        System.out.printf( "║  Slug        : %-46s║%n", page.slug());
        System.out.printf( "║  Published   : %-46s║%n", page.datePublished());
        System.out.printf( "║  Updated     : %-46s║%n", page.dateModified());
        System.out.printf( "║  Favorites   : %-46d║%n", page.favorites());
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.printf( "║  Found %d build variant(s)%-37s║%n", page.variants().size(), "");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        for (int i = 0; i < page.variants().size(); i++) {
            printVariant(i + 1, page.variants().get(i));
        }
    }

    private static void printVariant(int index, BuildVariant v) {
        System.out.println("\n┌─────────────────────────────────────────────────────────────┐");
        System.out.printf( "│  VARIANT #%d  (id: %-42s)│%n", index, v.id());
        System.out.println("└─────────────────────────────────────────────────────────────┘");

        if (v.weapon() != null) {
            WeaponSlot w = v.weapon();
            System.out.printf("  %-8s %s%n", "[WEAPON]", w.name());
            System.out.printf("           ATK:%-6d AFF:%-5d%s Slots:%s%n",
                w.attack(), w.affinity(),
                w.element() != null ? " Element:" + w.element() : "        ",
                w.slots()
            );
            printDecorations("           ", w.decorations());
        }

        printArmorSlot("HEAD",   v.head());
        printArmorSlot("CHEST",  v.chest());
        printArmorSlot("ARMS",   v.arms());
        printArmorSlot("WAIST",  v.waist());
        printArmorSlot("LEGS",   v.legs());
        printArmorSlot("CHARM",  v.charm());
        printArmorSlot("MANTLE", v.mantle());

        System.out.println("\n  ── Skills ──────────────────────────────────────────────────");
        for (SkillEntry s : v.skills()) {
            // Aggregate level is null in the source JSON; actual value is the sum of per-piece contributions
            String lvDisplay = s.level() != null && s.level() > 0 ? "Lv" + s.level() : "(sum of pieces)";
            System.out.printf("  %-38s %s / max %d%n", s.name(), lvDisplay, s.maxLevel());
        }

        if (!v.setBonuses().isEmpty()) {
            System.out.println("\n  ── Set Bonuses ──────────────────────────────────────────────");
            for (SkillEntry s : v.setBonuses()) {
                System.out.printf("  %-38s Lv%s%n", s.name(), s.level() != null ? s.level() : "?");
            }
        }

        if (!v.groupSkills().isEmpty()) {
            System.out.println("\n  ── Group Skills ─────────────────────────────────────────────");
            for (SkillEntry s : v.groupSkills()) {
                System.out.printf("  %-38s Lv%s%n", s.name(), s.level() != null ? s.level() : "?");
            }
        }
    }

    private static void printArmorSlot(String label, ArmorSlot slot) {
        if (slot == null) {
            System.out.printf("  [%-6s] (empty)%n", label);
            return;
        }
        System.out.printf("  [%-6s] %s%n", label, slot.name());
        printDecorations("           ", slot.decorations());
    }

    private static void printDecorations(String indent, List<Decoration> decs) {
        for (Decoration d : decs) {
            System.out.printf("%s└─ [%d] %s%n", indent, d.level(), d.name());
        }
    }
}