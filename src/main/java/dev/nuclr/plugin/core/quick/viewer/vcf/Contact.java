package dev.nuclr.plugin.core.quick.viewer.vcf;

import java.io.ByteArrayInputStream;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import dev.nuclr.plugin.core.quick.viewer.vcf.ContactGlyph.Kind;
import ezvcard.VCard;
import ezvcard.parameter.VCardParameter;
import ezvcard.property.Address;
import ezvcard.property.Email;
import ezvcard.property.Impp;
import ezvcard.property.Note;
import ezvcard.property.Organization;
import ezvcard.property.Photo;
import ezvcard.property.StructuredName;
import ezvcard.property.Telephone;
import ezvcard.property.Title;
import ezvcard.property.Url;
import lombok.extern.slf4j.Slf4j;

/**
 * Display-ready projection of a single {@link VCard}. All vCard parsing,
 * formatting and photo decoding happens here so it can run off the EDT; the
 * Swing layer only consumes the resulting immutable fields.
 */
@Slf4j
final class Contact {

	/** A single labelled line within a contact card. */
	record Field(Kind icon, String label, String value, boolean selectable) {
	}

	private final String displayName;
	private final String subtitle;
	private final String nickname;
	private final java.awt.image.BufferedImage photo;
	private final List<Field> fields;

	private Contact(String displayName, String subtitle, String nickname,
			java.awt.image.BufferedImage photo, List<Field> fields) {
		this.displayName = displayName;
		this.subtitle = subtitle;
		this.nickname = nickname;
		this.photo = photo;
		this.fields = fields;
	}

	String displayName() {
		return displayName;
	}

	String subtitle() {
		return subtitle;
	}

	String nickname() {
		return nickname;
	}

	java.awt.image.BufferedImage photo() {
		return photo;
	}

	List<Field> fields() {
		return fields;
	}

	// ── Construction ────────────────────────────────────────────────────────────

	static Contact from(VCard card) {
		String name = displayName(card);
		String subtitle = subtitle(card);
		String nickname = card.getNickname() != null ? joinValues(card.getNickname().getValues()) : null;

		List<Field> fields = new ArrayList<>();
		for (Telephone t : card.getTelephoneNumbers()) {
			String v = phoneValue(t);
			if (v != null) {
				boolean mobile = hasType(t.getTypes(), "cell", "mobile");
				fields.add(new Field(mobile ? Kind.MOBILE : Kind.PHONE, typeLabel(t.getTypes()), v, true));
			}
		}
		for (Email e : card.getEmails()) {
			if (notBlank(e.getValue())) {
				fields.add(new Field(Kind.EMAIL, typeLabel(e.getTypes()), e.getValue().trim(), true));
			}
		}
		for (Impp i : card.getImpps()) {
			String v = imppValue(i);
			if (v != null) {
				String label = i.getProtocol() != null ? i.getProtocol() : typeLabel(i.getTypes());
				fields.add(new Field(Kind.CHAT, label, v, true));
			}
		}
		for (Url u : card.getUrls()) {
			if (notBlank(u.getValue())) {
				fields.add(new Field(Kind.LINK, label(u.getType(), "website"), u.getValue().trim(), true));
			}
		}
		for (Address a : card.getAddresses()) {
			String v = addressValue(a);
			if (v != null) {
				fields.add(new Field(Kind.LOCATION, typeLabel(a.getTypes()), v, false));
			}
		}
		String birthday = birthday(card);
		if (birthday != null) {
			fields.add(new Field(Kind.BIRTHDAY, "birthday", birthday, false));
		}
		if (card.getCategories() != null) {
			String cats = joinValues(card.getCategories().getValues());
			if (notBlank(cats)) {
				fields.add(new Field(Kind.TAG, "tags", cats, false));
			}
		}
		for (Note n : card.getNotes()) {
			if (notBlank(n.getValue())) {
				fields.add(new Field(Kind.NOTE, "note", n.getValue().trim(), true));
			}
		}

		return new Contact(name, subtitle, nickname, decodePhoto(card), fields);
	}

	private static String displayName(VCard card) {
		if (card.getFormattedName() != null && notBlank(card.getFormattedName().getValue())) {
			return card.getFormattedName().getValue().trim();
		}
		StructuredName n = card.getStructuredName();
		if (n != null) {
			List<String> parts = new ArrayList<>();
			addAll(parts, n.getPrefixes());
			add(parts, n.getGiven());
			addAll(parts, n.getAdditionalNames());
			add(parts, n.getFamily());
			addAll(parts, n.getSuffixes());
			String joined = String.join(" ", parts).trim();
			if (notBlank(joined)) {
				return joined;
			}
		}
		if (!card.getEmails().isEmpty() && notBlank(card.getEmails().get(0).getValue())) {
			return card.getEmails().get(0).getValue().trim();
		}
		return "Unnamed contact";
	}

	/** Job title and organisation, e.g. {@code "Engineer · Acme Corp"}. */
	private static String subtitle(VCard card) {
		List<String> parts = new ArrayList<>();
		if (!card.getTitles().isEmpty()) {
			Title t = card.getTitles().get(0);
			if (notBlank(t.getValue())) {
				parts.add(t.getValue().trim());
			}
		}
		Organization org = card.getOrganization();
		if (org != null) {
			String o = org.getValues().stream().filter(Contact::notBlank).collect(Collectors.joining(" · "));
			if (notBlank(o)) {
				parts.add(o);
			}
		}
		return parts.isEmpty() ? null : String.join("  ·  ", parts);
	}

	private static java.awt.image.BufferedImage decodePhoto(VCard card) {
		for (Photo photo : card.getPhotos()) {
			byte[] data = photo.getData();
			if (data == null || data.length == 0) {
				continue;
			}
			try {
				java.awt.image.BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
				if (img != null) {
					return img;
				}
			} catch (Exception e) {
				log.debug("Could not decode embedded vCard photo", e);
			}
		}
		return null;
	}

	// ── Value formatting ────────────────────────────────────────────────────────

	private static String phoneValue(Telephone t) {
		if (notBlank(t.getText())) {
			return t.getText().trim();
		}
		if (t.getUri() != null && notBlank(t.getUri().getNumber())) {
			return t.getUri().getNumber();
		}
		return null;
	}

	private static String imppValue(Impp i) {
		if (notBlank(i.getHandle())) {
			return i.getHandle();
		}
		return i.getUri() != null ? i.getUri().toString() : null;
	}

	private static String addressValue(Address a) {
		List<String> lines = new ArrayList<>();
		add(lines, a.getStreetAddress());
		add(lines, a.getExtendedAddress());
		String cityLine = joinNonBlank(" ",
				joinNonBlank(", ", a.getLocality(), a.getRegion()), a.getPostalCode());
		add(lines, cityLine);
		add(lines, a.getCountry());
		if (lines.isEmpty()) {
			add(lines, a.getLabel());
		}
		return lines.isEmpty() ? null : String.join("\n", lines);
	}

	private static String birthday(VCard card) {
		if (card.getBirthday() == null) {
			return null;
		}
		var bday = card.getBirthday();
		Temporal date = bday.getDate();
		if (date != null) {
			try {
				return DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH).format((TemporalAccessor) date);
			} catch (Exception e) {
				return date.toString();
			}
		}
		if (bday.getPartialDate() != null) {
			return bday.getPartialDate().toString();
		}
		return notBlank(bday.getText()) ? bday.getText().trim() : null;
	}

	// ── Type / label helpers ────────────────────────────────────────────────────

	private static boolean hasType(List<? extends VCardParameter> types, String... wanted) {
		for (VCardParameter p : types) {
			for (String w : wanted) {
				if (p.getValue() != null && p.getValue().equalsIgnoreCase(w)) {
					return true;
				}
			}
		}
		return false;
	}

	private static String typeLabel(List<? extends VCardParameter> types) {
		String joined = types.stream()
				.map(VCardParameter::getValue)
				.filter(Contact::notBlank)
				.map(Contact::prettyType)
				.distinct()
				.collect(Collectors.joining(", "));
		return joined.isBlank() ? "" : joined;
	}

	private static String prettyType(String raw) {
		return switch (raw.toLowerCase(Locale.ROOT)) {
			case "cell" -> "mobile";
			case "voice" -> "phone";
			case "pref" -> "preferred";
			default -> raw.toLowerCase(Locale.ROOT);
		};
	}

	private static String label(String raw, String fallback) {
		return notBlank(raw) ? raw.toLowerCase(Locale.ROOT) : fallback;
	}

	// ── Small utilities ─────────────────────────────────────────────────────────

	private static String joinValues(List<String> values) {
		return values == null ? null : values.stream().filter(Contact::notBlank).collect(Collectors.joining(", "));
	}

	private static String joinNonBlank(String sep, String... parts) {
		List<String> kept = new ArrayList<>();
		for (String p : parts) {
			if (notBlank(p)) {
				kept.add(p.trim());
			}
		}
		return String.join(sep, kept);
	}

	private static void add(List<String> list, String value) {
		if (notBlank(value)) {
			list.add(value.trim());
		}
	}

	private static void addAll(List<String> list, List<String> values) {
		if (values != null) {
			for (String v : values) {
				add(list, v);
			}
		}
	}

	private static boolean notBlank(String s) {
		return s != null && !s.isBlank();
	}
}
