# Limelight Creatives website

Exported to plain HTML for hosting but can also run as a normal Spring Boot app

## Commands

| Command                                  | What it does                                                           |
|------------------------------------------|------------------------------------------------------------------------|
| `./gradlew bootRun`                      | Runs the site at http://localhost:8080                                 |
| `./gradlew exportSite`                   | Renders the whole site to `build/site/`                                |
| `./gradlew test`                         | Checks every page renders                                              |
| `./gradlew :build-logic:test`            | Tests the Tailwind plugin (unit tests plus real builds via TestKit)    |
| `./gradlew compileTailwind --continuous` | Rebuilds the CSS whenever a template changes (use alongside `bootRun`) |

## Tailwind

Stylesheets live with the source set, like Java does: `src/main/css/` belongs to `main`, `src/test/css/` to
`test`. Each source set has a `tailwind { }` block and its own compile task (`compileTailwind`,
`compileTestTailwind`, ...), whose output becomes part of that source set's resources. A source set with no
stylesheets is skipped (`NO-SOURCE`).

```kotlin
sourceSets.main {
    tailwind {
        // css.srcDir("src/main/css"), entry = "app.css" and outputPath = "static/css/app.css" are the defaults
        content.from("src/main/resources/templates")   // what Tailwind scans for class names
    }
}

tailwind {
    // shared settings, all optional: version (default: tailwind.version), minify, localBinary
}
```

```
build-logic/src/main/kotlin/org/limelightcreatives/gradle/tailwind/
├── TailwindPlugin.kt           ← wires it all together
├── TailwindExtension.kt        ← the project's tailwind { } block: version, minify, localBinary
├── TailwindSourceSet.kt        ← each source set's tailwind { } block: css folders, entry, content, outputPath
├── platform/TailwindPlatform   ← which CLI binary this OS/CPU needs
├── repository/TailwindReleases ← GitHub releases as an Ivy repository (RepositoryHandler.tailwindReleases())
├── verification/Checksums      ← sha256sums.txt parsing and hashing
└── tasks/                      ← tailwindInstall, compileTailwind (TailwindCompile)
```

- Tailwind's GitHub releases are declared as an **Ivy repository**, so the CLI for your OS is a normal
  dependency (`com.tailwindcss:tailwindcss-<os>-<cpu>:<version>`) in Gradle's shared cache.
- `tailwindInstall` checks it against the release's own `sha256sums.txt` (fetched the same way) and makes it
  executable.
- Each css folder is added to its source set's `allSource`, the way the groovy, scala and antlr plugins
  register theirs, so IDE imports see it as part of the source set.
- `compileTailwind` builds the CSS, only when a stylesheet (including ones the entry imports), a scanned file or
  an option changes. The result is cached and relocatable, so a fresh checkout can reuse it from the build cache.

To upgrade, change `tailwind.version` in `gradle.properties`; nothing else. Offline,
`-Ptailwind.bin=/path/to/tailwindcss` uses a binary you already have.

## Where things are

```
src/main/resources/
├── content/
│   ├── site.yml          ← dates, venues, schedule, sponsors, FAQs, team  (edit this for a new event)
│   └── legal/*.md        ← privacy, code of conduct, safeguarding (Markdown)
├── templates/
│   ├── layout.html       ← <head>, navbar, footer, popups — shared by every page
│   ├── index.html        ← home page: hero + the stacking cards
│   ├── home/             ← one file per home page section (hero, about, timeline, ...)
│   ├── legal.html        ← template for the legal pages
│   ├── error/404.html
│   └── fragments/        ← navbar, footer, button, popups, SVG art
└── static/               ← images, fonts, PDFs, js/site.js (all the interactivity)

src/main/css/app.css      ← Tailwind input: fonts, colours, base styles
src/main/java/org/limelightcreatives/site/
├── content/SiteContent   ← site.yml as Java records
├── legal/LegalPages      ← reads and renders the Markdown legal pages
├── web/SitePages         ← every page: URL + template + data (used by both modes below)
├── web/SiteController    ← serves pages when running as an app
└── export/SiteExporter   ← writes pages to files for exportSite
```

## Common edits

- **New event, sponsor, FAQ or team member:** edit `content/site.yml`. Team photos go in `static/team/headshots/`, sponsor logos in `static/assets/sponsors/`.
- **Legal page wording:** edit the Markdown in `content/legal/`. Each `## Heading` becomes a section, linkable as `#heading-in-lowercase`. A new `.md` file there becomes a new page automatically.
- **New page:** add a template, add it to `SitePages` (and a route in `SiteController`). The export picks it up from `SitePages.all()`.
- **New Tailwind classes:** just use them in templates. Tailwind scans `templates/`, `static/js/` and `src/main/java/` (see the `@source` lines in `app.css`).

## How the two modes stay in sync

`SitePages` is the single list of pages and the data each needs. `SiteController` renders
them on request; `SiteExporter` (active only with the `export` profile, no web server)
renders the same list through the same Thymeleaf engine into files. So `bootRun` and the
exported site produce identical HTML.

## SEO

- Every page gets its own `<title>`, description, canonical link and Open Graph / Twitter tags from `PageMeta`
  (built in `SitePages`; legal pages use their front matter). The 404 is `noindex` and not in the sitemap.
- `robots.txt` and `sitemap.xml` are generated from the page list (`CrawlerFiles`), so they can't go stale.
- Set `site.url` in `content/site.yml` to the live address; canonical links, the sitemap and `og:image` use it.
- The social preview image is `static/assets/og.jpg` (1200x630).
- Photos are WebP at display size (a test fails if an image goes over 400 KB). Convert new ones before adding them.
- The Register form starts loading when someone hovers, focuses or touches a Register button.
