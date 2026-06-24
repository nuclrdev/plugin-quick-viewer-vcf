package dev.nuclr.plugin.core.quick.viewer.vcf;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Icon;

/**
 * Lightweight, theme-coloured vector icons drawn with Java2D so the plugin
 * carries no image assets. Each glyph is stroked inside a square box and scales
 * cleanly to any size.
 */
final class ContactGlyph implements Icon {

	enum Kind {
		PHONE, MOBILE, EMAIL, LOCATION, LINK, BIRTHDAY, CHAT, TAG, NOTE, ORG, PERSON
	}

	private final Kind kind;
	private final Color color;
	private final int size;

	ContactGlyph(Kind kind, Color color, int size) {
		this.kind = kind;
		this.color = color;
		this.size = size;
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
			g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g2.translate(x, y);
			g2.setColor(color);
			float stroke = Math.max(1.3f, size / 11f);
			g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			// Work in a padded box so round caps never clip the icon bounds.
			float pad = stroke;
			float s = size - 2 * pad;
			g2.translate(pad, pad);
			switch (kind) {
				case PHONE -> phone(g2, s);
				case MOBILE -> mobile(g2, s);
				case EMAIL -> email(g2, s);
				case LOCATION -> location(g2, s);
				case LINK -> link(g2, s);
				case BIRTHDAY -> birthday(g2, s);
				case CHAT -> chat(g2, s);
				case TAG -> tag(g2, s);
				case NOTE -> note(g2, s);
				case ORG -> org(g2, s);
				case PERSON -> person(g2, s);
			}
		} finally {
			g2.dispose();
		}
	}

	// ── Glyphs ─────────────────────────────────────────────────────────────────

	private void phone(Graphics2D g, float s) {
		// Classic handset: a curved bar with two earpieces.
		GeneralPath p = new GeneralPath();
		p.moveTo(s * 0.16, s * 0.20);
		p.curveTo(s * 0.10, s * 0.42, s * 0.58, s * 0.90, s * 0.80, s * 0.84);
		p.lineTo(s * 0.86, s * 0.64);
		p.lineTo(s * 0.62, s * 0.56);
		p.lineTo(s * 0.52, s * 0.66);
		p.curveTo(s * 0.42, s * 0.60, s * 0.36, s * 0.54, s * 0.32, s * 0.44);
		p.lineTo(s * 0.42, s * 0.34);
		p.lineTo(s * 0.34, s * 0.10);
		p.closePath();
		g.draw(p);
	}

	private void mobile(Graphics2D g, float s) {
		g.draw(new RoundRectangle2D.Float(s * 0.30f, s * 0.06f, s * 0.40f, s * 0.88f, s * 0.16f, s * 0.16f));
		g.draw(line(s * 0.44f, s * 0.80f, s * 0.56f, s * 0.80f));
	}

	private void email(Graphics2D g, float s) {
		g.draw(new RoundRectangle2D.Float(s * 0.06f, s * 0.20f, s * 0.88f, s * 0.60f, s * 0.10f, s * 0.10f));
		GeneralPath flap = new GeneralPath();
		flap.moveTo(s * 0.09, s * 0.24);
		flap.lineTo(s * 0.50, s * 0.54);
		flap.lineTo(s * 0.91, s * 0.24);
		g.draw(flap);
	}

	private void location(Graphics2D g, float s) {
		// Map pin: teardrop + inner dot.
		GeneralPath p = new GeneralPath();
		float cx = s * 0.50f;
		float r = s * 0.28f;
		float topY = s * 0.10f;
		p.moveTo(cx, s * 0.94);
		p.curveTo(cx - r * 1.4, s * 0.52, cx - r, topY + r * 0.4f, cx, topY);
		p.curveTo(cx + r, topY + r * 0.4f, cx + r * 1.4, s * 0.52, cx, s * 0.94);
		p.closePath();
		g.draw(p);
		float dot = s * 0.16f;
		g.draw(new Ellipse2D.Float(cx - dot / 2, topY + r * 0.55f, dot, dot));
	}

	private void link(Graphics2D g, float s) {
		// Two interlocking rounded links along the diagonal.
		g.draw(new RoundRectangle2D.Float(s * 0.06f, s * 0.34f, s * 0.46f, s * 0.32f, s * 0.30f, s * 0.30f));
		g.draw(new RoundRectangle2D.Float(s * 0.48f, s * 0.34f, s * 0.46f, s * 0.32f, s * 0.30f, s * 0.30f));
		g.draw(line(s * 0.38f, s * 0.50f, s * 0.62f, s * 0.50f));
	}

	private void birthday(Graphics2D g, float s) {
		// Cake with a single candle + flame.
		g.draw(new RoundRectangle2D.Float(s * 0.12f, s * 0.50f, s * 0.76f, s * 0.40f, s * 0.10f, s * 0.10f));
		g.draw(line(s * 0.50f, s * 0.28f, s * 0.50f, s * 0.50f));
		g.fill(new Ellipse2D.Float(s * 0.45f, s * 0.16f, s * 0.10f, s * 0.14f));
		g.draw(line(s * 0.12f, s * 0.66f, s * 0.88f, s * 0.66f));
	}

	private void chat(Graphics2D g, float s) {
		g.draw(new RoundRectangle2D.Float(s * 0.08f, s * 0.12f, s * 0.84f, s * 0.58f, s * 0.18f, s * 0.18f));
		GeneralPath tail = new GeneralPath();
		tail.moveTo(s * 0.30, s * 0.70);
		tail.lineTo(s * 0.26, s * 0.92);
		tail.lineTo(s * 0.50, s * 0.70);
		g.draw(tail);
	}

	private void tag(Graphics2D g, float s) {
		GeneralPath p = new GeneralPath();
		p.moveTo(s * 0.10, s * 0.10);
		p.lineTo(s * 0.52, s * 0.10);
		p.lineTo(s * 0.90, s * 0.48);
		p.lineTo(s * 0.50, s * 0.90);
		p.lineTo(s * 0.10, s * 0.50);
		p.closePath();
		g.draw(p);
		g.fill(new Ellipse2D.Float(s * 0.24f, s * 0.24f, s * 0.12f, s * 0.12f));
	}

	private void note(Graphics2D g, float s) {
		GeneralPath p = new GeneralPath();
		p.moveTo(s * 0.20, s * 0.08);
		p.lineTo(s * 0.64, s * 0.08);
		p.lineTo(s * 0.82, s * 0.26);
		p.lineTo(s * 0.82, s * 0.92);
		p.lineTo(s * 0.20, s * 0.92);
		p.closePath();
		g.draw(p);
		GeneralPath fold = new GeneralPath();
		fold.moveTo(s * 0.64, s * 0.08);
		fold.lineTo(s * 0.64, s * 0.26);
		fold.lineTo(s * 0.82, s * 0.26);
		g.draw(fold);
		g.draw(line(s * 0.32f, s * 0.46f, s * 0.70f, s * 0.46f));
		g.draw(line(s * 0.32f, s * 0.62f, s * 0.70f, s * 0.62f));
		g.draw(line(s * 0.32f, s * 0.78f, s * 0.56f, s * 0.78f));
	}

	private void org(Graphics2D g, float s) {
		g.draw(new RoundRectangle2D.Float(s * 0.18f, s * 0.10f, s * 0.50f, s * 0.84f, s * 0.06f, s * 0.06f));
		g.draw(new RoundRectangle2D.Float(s * 0.68f, s * 0.40f, s * 0.20f, s * 0.54f, s * 0.06f, s * 0.06f));
		for (float wy = 0.22f; wy <= 0.74f; wy += 0.18f) {
			g.draw(line(s * 0.28f, s * wy, s * 0.34f, s * wy));
			g.draw(line(s * 0.48f, s * wy, s * 0.54f, s * wy));
		}
	}

	private void person(Graphics2D g, float s) {
		g.draw(new Ellipse2D.Float(s * 0.32f, s * 0.10f, s * 0.36f, s * 0.36f));
		GeneralPath body = new GeneralPath(Path2D.WIND_NON_ZERO);
		body.moveTo(s * 0.14, s * 0.92);
		body.curveTo(s * 0.14, s * 0.58, s * 0.86, s * 0.58, s * 0.86, s * 0.92);
		g.draw(body);
	}

	private static java.awt.geom.Line2D.Float line(float x1, float y1, float x2, float y2) {
		return new java.awt.geom.Line2D.Float(x1, y1, x2, y2);
	}
}
