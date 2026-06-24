# 📇 plugin-quick-viewer-vcf

Quick-view provider for **vCard / `.vcf` contact files** in Nuclr Commander. 🚀

Selecting a `.vcf` file in a panel and pressing **Ctrl+Q** ⌨️ renders its
contacts as styled cards instead of raw text:

- 🖼️ Circular **avatar** — the embedded photo when present, otherwise initials
  on a colour generated deterministically from the contact's name.
- 🧑‍💼 **Name**, job **title** and **organisation** header, plus nickname.
- 📋 Labelled, **selectable/copyable** fields: 📞 phone (mobile vs landline),
  ✉️ email, 💬 instant-messaging, 🔗 websites, 📍 postal address, 🎂 birthday,
  🏷️ categories and 📝 notes.
- 🎨 Each field has a hand-drawn Java2D vector icon (no image assets shipped).
- 🌗 Fully **theme-aware** — colours and fonts follow the active Commander theme.
- 👥 Handles multi-contact files (an address-book export shows one card each).

## 🔨 Build & deploy

```bash
mvn clean verify          # 🧪 compiles, runs tests, builds + signs the plugin ZIP
deploy.bat                # 📦 builds and copies the ZIP + .sig into Commander
```

The signed artifact is `target/quick-view-vcf-1.0.0.zip` (+ `.zip.sig`). ✅

## 🧩 Implementation

- 🔍 `VcfFileSupport` — detects `.vcf`/`.vcard` by extension with a `BEGIN:VCARD`
  content fallback.
- 🗂️ `Contact` — off-EDT projection of an ez-vcard `VCard` into display-ready
  fields and a decoded avatar image.
- 🎭 `AvatarIcon` / `ContactGlyph` — Java2D circular avatar and field icons.
- 🪟 `VcfQuickViewPanel` — scrollable layout of rounded contact cards.
- 🔌 `VcfQuickViewProvider` — the `QuickViewNuclrPlugin` entry point.

📚 Parsing uses [ez-vcard](https://github.com/mangstadt/ez-vcard).
