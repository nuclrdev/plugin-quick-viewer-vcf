package dev.nuclr.plugin.core.quick.viewer.vcf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import dev.nuclr.plugin.core.quick.viewer.vcf.Contact.Field;
import dev.nuclr.plugin.core.quick.viewer.vcf.ContactGlyph.Kind;
import ezvcard.Ezvcard;
import ezvcard.VCard;

class ContactTest {

	private static final String SAMPLE = """
			BEGIN:VCARD
			VERSION:3.0
			FN:Ada Lovelace
			N:Lovelace;Ada;;;
			NICKNAME:Countess
			ORG:Analytical Engines;Research
			TITLE:Mathematician
			TEL;TYPE=CELL:+1-202-555-0142
			TEL;TYPE=WORK,VOICE:+1-202-555-0177
			EMAIL;TYPE=HOME:ada@example.com
			URL:https://example.com/ada
			ADR;TYPE=HOME:;;12 Babbage St;London;;EC1;UK
			BDAY:1815-12-10
			CATEGORIES:Pioneers,Friends
			NOTE:First programmer.
			END:VCARD
			""";

	@Test
	void extractsCoreFields() {
		VCard card = Ezvcard.parse(SAMPLE).first();
		assertNotNull(card);

		Contact c = Contact.from(card);
		assertEquals("Ada Lovelace", c.displayName());
		assertEquals("Countess", c.nickname());
		assertNotNull(c.subtitle());
		assertTrue(c.subtitle().contains("Mathematician"));
		assertTrue(c.subtitle().contains("Analytical Engines"));

		List<Field> fields = c.fields();
		assertTrue(contains(fields, Kind.MOBILE, "+1-202-555-0142"));
		assertTrue(contains(fields, Kind.EMAIL, "ada@example.com"));
		assertTrue(contains(fields, Kind.LINK, "https://example.com/ada"));
		assertTrue(hasKind(fields, Kind.LOCATION));
		assertTrue(hasKind(fields, Kind.BIRTHDAY));
		assertTrue(hasKind(fields, Kind.TAG));
		assertTrue(contains(fields, Kind.NOTE, "First programmer."));
	}

	@Test
	void multipleCardsParsed() {
		String two = SAMPLE + "\n" + SAMPLE.replace("Ada Lovelace", "Grace Hopper");
		List<VCard> cards = Ezvcard.parse(two).all();
		assertEquals(2, cards.size());
		assertEquals("Grace Hopper", Contact.from(cards.get(1)).displayName());
	}

	@Test
	void fallsBackToStructuredNameWhenNoFormattedName() {
		VCard card = Ezvcard.parse("""
				BEGIN:VCARD
				VERSION:3.0
				N:Turing;Alan;;;
				END:VCARD
				""").first();
		assertEquals("Alan Turing", Contact.from(card).displayName());
	}

	private static boolean contains(List<Field> fields, Kind kind, String value) {
		return fields.stream().anyMatch(f -> f.icon() == kind && f.value().contains(value));
	}

	private static boolean hasKind(List<Field> fields, Kind kind) {
		return fields.stream().anyMatch(f -> f.icon() == kind);
	}
}
