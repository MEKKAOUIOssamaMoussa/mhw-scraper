# mhw-scraper

A Java scraping pipeline targeting Mobalytics build guides for Monster Hunter Wilds.
Extracts weapon builds, armor sets, skills, and decorations from browser-rendered HTML.

## Stack

- Java 17 · Maven
- [Jsoup](https://jsoup.org/) — HTML parsing
- [Jackson Databind](https://github.com/FasterXML/jackson-databind) — JSON parsing

## How it works

Mobalytics pages are React SPAs protected by Cloudflare. The server-side rendered HTML contains a
`window.__PRELOADED_STATE__` JSON blob that holds the complete build data before JavaScript hydration.
The scraper targets that blob directly rather than parsing rendered DOM elements.

## Running

Save a page from your browser (File → Save As → Webpage, Complete), then:

```bash
mvn -q compile exec:java -Dexec.args="path/to/saved-page.html"
```

## Project structure

```
src/main/java/com/scraper/
├── Main.java                  # Entry point
├── extractor/
│   └── BuildExtractor.java    # Parses HTML → BuildPage
└── model/
    ├── BuildPage.java
    ├── BuildVariant.java
    ├── WeaponSlot.java
    ├── ArmorSlot.java
    ├── SkillEntry.java
    └── Decoration.java
```

## Roadmap

- [ ] Skill level aggregation (sum per-piece contributions)
- [ ] JSON file output via Jackson
- [ ] Multi-weapon batch processing
- [ ] PostgreSQL schema + Spring Boot API
- [ ] Multilingual frontend (EN / FR / AR)
