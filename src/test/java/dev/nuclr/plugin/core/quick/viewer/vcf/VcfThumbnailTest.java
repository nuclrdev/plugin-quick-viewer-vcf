package dev.nuclr.plugin.core.quick.viewer.vcf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.OpenOption;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import dev.nuclr.platform.plugin.NuclrResource;

class VcfThumbnailTest {

	private static final String CARD = "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:Ada Lovelace\r\nEND:VCARD\r\n";

	private final VcfQuickViewProvider provider = new VcfQuickViewProvider();

	@Test
	void drawsARoundAvatarSizedToTheShorterSideBeforeInit() {
		assertTrue(provider.supportsThumbnails());

		BufferedImage image = provider.thumbnail(resource("ada.vcf", CARD), 120, 80, new AtomicBoolean());

		assertNotNull(image);
		assertEquals(80, image.getWidth());
		assertEquals(80, image.getHeight());
		assertEquals(0, image.getRGB(0, 0) >>> 24, "outside the circle is transparent");
		assertEquals(0xFF, image.getRGB(40, 20) >>> 24, "inside the circle is painted");
	}

	@Test
	void returnsNullWithoutAContactOrWhenCancelled() {
		assertNull(provider.thumbnail(resource("empty.vcf", "not a vcard"), 80, 80, new AtomicBoolean()));
		assertNull(provider.thumbnail(resource("ada.vcf", CARD), 80, 80, new AtomicBoolean(true)));
	}

	private static NuclrResource resource(String name, String content) {
		byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
		NuclrResource resource = new NuclrResource(null) {
			private static final long serialVersionUID = 1L;

			@Override
			public InputStream openInputStream(OpenOption... options) {
				return new ByteArrayInputStream(bytes);
			}
		};
		resource.setUuid(name);
		resource.setName(name);
		resource.setLength(bytes.length);
		return resource;
	}
}
