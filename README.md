# 📇 vCard Quick Viewer

Quick-view provider for **vCard / `.vcf` contact files** in [Nuclr Commander](https://nuclr.dev). 🚀

Selecting a `.vcf` file in a panel and pressing **Ctrl+Q** ⌨️ renders its
contacts as styled cards instead of raw text.

## ✨ What it does

- 🖼️ Circular **avatar** — the embedded photo when present, otherwise initials
  on a colour generated deterministically from the contact's name.
- 🧑‍💼 **Name**, job **title** and **organisation** header, plus nickname.
- 📋 Labelled, **selectable/copyable** fields: 📞 phone (mobile vs landline),
  ✉️ email, 💬 instant-messaging, 🔗 websites, 📍 postal address, 🎂 birthday,
  🏷️ categories and 📝 notes.
- 🎨 Each field has a hand-drawn Java2D vector icon (no image assets shipped).
- 🌗 Fully **theme-aware** — colours and fonts follow the active Commander theme.
- 👥 Handles multi-contact files (an address-book export shows one card each).

## 🧩 Supported formats

| Extension | Format |
|---|---|
| `.vcf` | vCard |
| `.vcard` | vCard |

Files with another extension are still accepted when their content starts with
`BEGIN:VCARD`.

## 📥 Installation

Copy the signed plugin archive and detached signature into the Nuclr Commander `plugins/` directory:

```text
quick-view-vcf-<version>.zip
quick-view-vcf-<version>.zip.sig
```

Nuclr Commander verifies the RSA-SHA256 signature against `nuclr-cert.pem` on load. The plugin becomes available immediately without a restart.

## 🔨 Build & deploy

```bash
mvn clean verify          # 🧪 compiles, runs tests, builds + signs the plugin ZIP
deploy.bat                # 📦 builds and copies the ZIP + .sig into Commander
```

Signing needs the keystore at `C:/nuclr/key/nuclr-signing.p12` (alias `nuclr`);
pass the password with `-Djarsigner.storepass=<password>`.

## 🗂️ Source layout

```text
src/main/java/dev/nuclr/plugin/core/quick/viewer/vcf/
├── VcfQuickViewProvider.java   QuickViewNuclrPlugin entry point
├── VcfQuickViewPanel.java      scrollable layout of rounded contact cards
├── VcfFileSupport.java         extension detection with BEGIN:VCARD fallback
├── Contact.java                off-EDT projection of an ez-vcard VCard
├── AvatarIcon.java             Java2D circular avatar (photo or initials)
└── ContactGlyph.java           Java2D field icons
```

## 📚 Dependencies

| Library | Version | Purpose |
|---|---|---|
| `dev.nuclr:platform-sdk` | `3.0.2` | Nuclr platform interfaces |
| [`ez-vcard`](https://github.com/mangstadt/ez-vcard) | `0.12.2` | vCard parsing |

## 📜 License

Apache License 2.0 — see [LICENSE](LICENSE).
