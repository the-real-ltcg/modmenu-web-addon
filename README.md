# Mod Menu Web Addon

A [Mod Menu](https://modrinth.com/mod/modmenu) addon for Fabric (Minecraft 26.2). For any
installed mod whose `fabric.mod.json` is missing a description, website, issue tracker, source
link, or icon, this addon looks the mod up on [Modrinth](https://modrinth.com) and, optionally,
[CurseForge](https://curseforge.com), and fills in whatever it finds.

## Features

- Fills in missing description, website, issue tracker, source link, and icon in Mod Menu's mod
  list and mod info screens.
- Modrinth lookups work out of the box (public, unauthenticated API).
- CurseForge lookups are opt-in: paste your own free [Core API
  key](https://console.curseforge.com/) into `config/modmenuwebaddon.json` to enable them.
  CurseForge's terms don't allow redistributing a shared key, so there's no key baked into the
  mod itself.
- Results are cached to disk (`config/modmenuwebaddon/`) for 7 days by default, so it won't
  re-query the network every launch.
- Skips mods that don't need it: built-in pseudo-mods (`minecraft`, `java`), Fabric API's dozens
  of sub-modules (already collapsed under one entry in Mod Menu), and jar-in-jar nested/library
  mods that have no real listing of their own.

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

## Building

Requires JDK 25.

```sh
JAVA_HOME="<path to JDK 25>" ./gradlew build
```

## License

MIT — see [LICENSE](LICENSE).
