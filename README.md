# Mod Menu Web Addon

Mod Menu addon that fills in missing mod descriptions, websites, issue trackers, source links, and
icons by looking them up on Modrinth and CurseForge.

Ever open Mod Menu and see a mod with no icon, no description, and no links? This addon fixes
that. For any installed mod whose `fabric.mod.json` is missing a description, website, issue
tracker, source link, or icon, Mod Menu Web Addon looks the mod up on
[Modrinth](https://modrinth.com) and, optionally, [CurseForge](https://curseforge.com), and fills
in whatever it finds — directly in Mod Menu's mod list and info screens. No other mod needs to
change anything; it works automatically in the background.

## Features

- **Automatic Modrinth lookups** — works out of the box, no setup, no API key required.
- **Optional CurseForge lookups** — bring your own free [Core API
  key](https://console.curseforge.com/) (CurseForge's terms don't allow bundling a shared key, so
  this is opt-in) to also check CurseForge for anything Modrinth couldn't find.
- **Smart skipping** — doesn't waste time or bandwidth on mods that don't need it: Minecraft/Java
  themselves, Fabric API's many sub-modules (already grouped under one entry in Mod Menu), and
  bundled library mods with no real listing of their own.
- **Cached results** — lookups are saved to disk for a week by default, so it won't hit the
  network on every launch.
- **Never overwrites what's already there** — only fills in gaps; a mod's own declared
  description, links, and icon always take priority.

## Configuration

`config/modmenuwebaddon.json` (created on first run):

```json
{
  "enabled": true,
  "fillMissingDescription": true,
  "fillMissingIcon": true,
  "fillMissingLinks": true,
  "useCurseForge": false,
  "curseForgeApiKey": "",
  "cacheTtlHours": 168
}
```

## Requirements

- Minecraft 26.2
- Fabric Loader
- Fabric API
- Mod Menu

## Building

Requires JDK 25.

```sh
JAVA_HOME="<path to JDK 25>" ./gradlew build
```

## License

MIT — see [LICENSE](LICENSE).
