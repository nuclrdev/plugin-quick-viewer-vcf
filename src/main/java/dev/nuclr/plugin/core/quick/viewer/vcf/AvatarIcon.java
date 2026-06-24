package dev.nuclr.plugin.core.quick.viewer.vcf;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

import javax.swing.Icon;

/**
 * Circular avatar. When the vCard embeds a photo it is centre-cropped into the
 * circle; otherwise a deterministic two-tone gradient is generated from the
 * contact's name and the initials are drawn on top.
 */
final class AvatarIcon implements Icon {

	private final BufferedImage photo;
	private final String initials;
	private final int size;
	private final Color ring;
	private final Color top;
	private final Color bottom;
	private final Color textColor;

	AvatarIcon(BufferedImage photo, String displayName, int size, Color ring) {
		this.photo = photo;
		this.initials = initials(displayName);
		this.size = size;
		this.ring = ring;
		// Stable hue from the name so the same contact always looks the same.
		float hue = (Math.abs((displayName == null ? "" : displayName).hashCode()) % 360) / 360f;
		this.top = Color.getHSBColor(hue, 0.52f, 0.78f);
		this.bottom = Color.getHSBColor((hue + 0.06f) % 1f, 0.62f, 0.58f);
		this.textColor = Color.WHITE;
	}

	@Override
	public int getIconWidth() {
		return size;
	}

	@Override
	public int getIconHeight() {
		return size;
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2 = (Graphics2D) g.create();
		try {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2.translate(x, y);
			Shape circle = new Ellipse2D.Float(0.5f, 0.5f, size - 1f, size - 1f);

			if (photo != null) {
				g2.setClip(circle);
				int dim = Math.min(photo.getWidth(), photo.getHeight());
				int sx = (photo.getWidth() - dim) / 2;
				int sy = (photo.getHeight() - dim) / 2;
				g2.drawImage(photo, 0, 0, size, size, sx, sy, sx + dim, sy + dim, null);
				g2.setClip(null);
			} else {
				g2.setPaint(new GradientPaint(0, 0, top, 0, size, bottom));
				g2.fill(circle);
				g2.setColor(textColor);
				g2.setFont(initialsFont());
				var fm = g2.getFontMetrics();
				int tw = fm.stringWidth(initials);
				int tx = (size - tw) / 2;
				int ty = (size - fm.getHeight()) / 2 + fm.getAscent();
				g2.drawString(initials, tx, ty);
			}

			// Subtle ring to separate the avatar from the card background.
			if (ring != null) {
				g2.setColor(ring);
				g2.draw(circle);
			}
		} finally {
			g2.dispose();
		}
	}

	private Font initialsFont() {
		return new Font(Font.SANS_SERIF, Font.BOLD, Math.round(size * 0.42f));
	}

	private static String initials(String name) {
		if (name == null || name.isBlank()) {
			return "?";
		}
		String[] parts = name.trim().split("\\s+");
		StringBuilder sb = new StringBuilder();
		sb.append(Character.toUpperCase(parts[0].charAt(0)));
		if (parts.length > 1) {
			String last = parts[parts.length - 1];
			if (!last.isEmpty()) {
				sb.append(Character.toUpperCase(last.charAt(0)));
			}
		}
		return sb.toString();
	}
}
