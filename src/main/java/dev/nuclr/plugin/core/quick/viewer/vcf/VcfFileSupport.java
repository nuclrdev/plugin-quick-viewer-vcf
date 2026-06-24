package dev.nuclr.plugin.core.quick.viewer.vcf;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import dev.nuclr.platform.plugin.NuclrResource;

/**
 * Decides whether a resource is a vCard (.vcf) file that this plugin can render.
 *
 * <p>
 * Detection is by extension first (fast, allocation-free) and falls back to a
 * small content sniff for the {@code BEGIN:VCARD} marker so that vCards saved
 * with an unusual extension are still recognised.
 */
final class VcfFileSupport {

	/** Marker every vCard begins with, per RFC 6350. */
	private static final byte[] MARKER = "BEGIN:VCARD".getBytes(StandardCharsets.US_ASCII);

	/** Bytes scanned for the marker; the marker may follow a BOM / leading whitespace. */
	private static final int SNIFF_SIZE = 64;

	private static final Set<String> VCF_EXTENSIONS = Set.of("vcf", "vcard");

	private VcfFileSupport() {
	}

	static boolean supports(NuclrResource resource) {
		if (resource == null) {
			return false;
		}
		String ext = extension(resource.getName());
		if (VCF_EXTENSIONS.contains(ext)) {
			return true;
		}
		// Unknown / missing extension: only sniff content, never for known
		// non-vcard extensions, to keep selection fast and predictable.
		return ext.isEmpty() && hasVCardMarker(resource);
	}

	private static boolean hasVCardMarker(NuclrResource resource) {
		try (InputStream in = resource.openInputStream()) {
			byte[] head = in.readNBytes(SNIFF_SIZE);
			return indexOfIgnoreCase(head) >= 0;
		} catch (Exception e) {
			return false;
		}
	}

	/** Case-insensitive search for {@link #MARKER} within {@code head}. */
	private static int indexOfIgnoreCase(byte[] head) {
		outer:
		for (int i = 0; i + MARKER.length <= head.length; i++) {
			for (int j = 0; j < MARKER.length; j++) {
				if (lower(head[i + j]) != lower(MARKER[j])) {
					continue outer;
				}
			}
			return i;
		}
		return -1;
	}

	private static int lower(byte b) {
		int c = b & 0xFF;
		return (c >= 'A' && c <= 'Z') ? c + 32 : c;
	}

	private static String extension(String filename) {
		if (filename == null || filename.isBlank()) {
			return "";
		}
		int dot = filename.lastIndexOf('.');
		return dot > 0 ? filename.substring(dot + 1).toLowerCase() : "";
	}
}
