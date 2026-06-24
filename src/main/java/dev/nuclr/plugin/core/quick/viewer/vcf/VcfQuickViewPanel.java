package dev.nuclr.plugin.core.quick.viewer.vcf;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import dev.nuclr.platform.NuclrThemeScheme;
import dev.nuclr.platform.plugin.NuclrResource;
import dev.nuclr.plugin.core.quick.viewer.vcf.Contact.Field;
import ezvcard.Ezvcard;
import ezvcard.VCard;
import lombok.extern.slf4j.Slf4j;

/**
 * Quick-view surface for vCard files. Renders each contact as a rounded card
 * with a circular avatar, name, organisation and a set of labelled, selectable
 * fields (phone, email, address, links, …).
 *
 * <p>
 * Parsing and photo decoding run on the caller's background thread; only the
 * Swing assembly is dispatched to the EDT.
 */
@Slf4j
public class VcfQuickViewPanel extends JPanel {

	/** Hard caps so a hostile or huge file can never exhaust memory / the EDT. */
	private static final long MAX_FILE_SIZE = 16L * 1024 * 1024; // 16 MB
	private static final int MAX_CONTACTS = 2000;

	private final JPanel content;
	private final JScrollPane scroll;
	private final JLabel header;

	private Palette palette = Palette.fallback();
	private NuclrThemeScheme theme;

	public VcfQuickViewPanel() {
		super(new BorderLayout());

		header = new JLabel();
		header.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
		header.setFont(header.getFont().deriveFont(Font.BOLD));

		content = new JPanel(new GridBagLayout());
		content.setBorder(BorderFactory.createEmptyBorder(8, 14, 14, 14));

		scroll = new JScrollPane(content,
				ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.getVerticalScrollBar().setUnitIncrement(18);

		add(header, BorderLayout.NORTH);
		add(scroll, BorderLayout.CENTER);

		applyPaletteColors();
	}

	// ── Public API used by the provider ─────────────────────────────────────────

	public void applyTheme(NuclrThemeScheme theme) {
		this.theme = theme;
		this.palette = Palette.from(theme);
		SwingUtilities.invokeLater(() -> {
			applyPaletteColors();
			// Re-render so existing cards pick up the new colours.
			if (lastContacts != null) {
				render(lastFileName, lastContacts);
			}
		});
	}

	/**
	 * Parses {@code item} and renders its contacts. Runs on a background thread;
	 * returns {@code false} only when the file should be handled by another
	 * provider (it is not a usable vCard).
	 */
	public boolean load(NuclrResource item, AtomicBoolean cancelled) {
		if (item.getLength() > MAX_FILE_SIZE) {
			showMessage("“" + item.getName() + "” is too large to preview.", cancelled);
			return true;
		}

		List<VCard> cards;
		try (var in = item.openInputStream()) {
			cards = Ezvcard.parse(in).all();
		} catch (Exception e) {
			log.warn("Failed to parse vCard: {}", item.getName(), e);
			showMessage("Could not read vCard: " + e.getMessage(), cancelled);
			return true;
		}

		if (cancelled.get()) {
			return false;
		}
		if (cards == null || cards.isEmpty()) {
			showMessage("No contacts found in this file.", cancelled);
			return true;
		}

		int limit = Math.min(cards.size(), MAX_CONTACTS);
		java.util.List<Contact> contacts = new java.util.ArrayList<>(limit);
		for (int i = 0; i < limit; i++) {
			if (cancelled.get()) {
				return false;
			}
			try {
				contacts.add(Contact.from(cards.get(i)));
			} catch (Exception e) {
				log.debug("Skipping malformed vCard entry #{}", i, e);
			}
		}

		final String summary = summary(contacts.size(), cards.size(), item.getName());
		SwingUtilities.invokeLater(() -> {
			if (!cancelled.get()) {
				render(summary, contacts);
			}
		});
		return true;
	}

	public void clear() {
		SwingUtilities.invokeLater(() -> {
			lastContacts = null;
			lastFileName = null;
			header.setText(" ");
			content.removeAll();
			content.revalidate();
			content.repaint();
		});
	}

	// ── Rendering ───────────────────────────────────────────────────────────────

	private String lastFileName;
	private List<Contact> lastContacts;

	private void render(String headerText, List<Contact> contacts) {
		lastFileName = headerText;
		lastContacts = contacts;

		header.setText(headerText);
		content.removeAll();

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = 0;
		gbc.weightx = 1.0;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.NORTH;
		gbc.insets = new Insets(6, 0, 6, 0);

		int row = 0;
		for (Contact c : contacts) {
			gbc.gridy = row++;
			content.add(buildCard(c), gbc);
		}

		// Push everything to the top.
		gbc.gridy = row;
		gbc.weighty = 1.0;
		gbc.fill = GridBagConstraints.BOTH;
		content.add(Box.createGlue(), gbc);

		content.revalidate();
		content.repaint();
		scroll.getVerticalScrollBar().setValue(0);
	}

	private void showMessage(String message, AtomicBoolean cancelled) {
		SwingUtilities.invokeLater(() -> {
			if (cancelled.get()) {
				return;
			}
			lastContacts = null;
			header.setText(" ");
			content.removeAll();

			JLabel label = new JLabel(message, SwingConstants.CENTER);
			label.setForeground(palette.secondary());
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.weightx = 1;
			gbc.weighty = 1;
			content.add(label, gbc);

			content.revalidate();
			content.repaint();
		});
	}

	private JComponent buildCard(Contact c) {
		RoundedCard card = new RoundedCard(palette);
		card.setLayout(new BorderLayout(0, 0));
		card.setBorder(BorderFactory.createEmptyBorder(14, 18, 16, 18));

		card.add(buildHeader(c), BorderLayout.NORTH);

		List<Field> fields = c.fields();
		if (!fields.isEmpty()) {
			JPanel body = new JPanel(new GridBagLayout());
			body.setOpaque(false);
			body.setBorder(BorderFactory.createEmptyBorder(12, 2, 0, 0));
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.weightx = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.anchor = GridBagConstraints.NORTHWEST;
			for (Field f : fields) {
				gbc.insets = new Insets(gbc.gridy == 0 ? 0 : 10, 0, 0, 0);
				body.add(buildField(f), gbc);
				gbc.gridy++;
			}
			card.add(body, BorderLayout.CENTER);
		}
		return card;
	}

	private JComponent buildHeader(Contact c) {
		JPanel head = new JPanel(new BorderLayout(14, 0));
		head.setOpaque(false);

		JLabel avatar = new JLabel(new AvatarIcon(c.photo(), c.displayName(), 56, palette.cardBorder()));
		avatar.setVerticalAlignment(SwingConstants.TOP);
		head.add(avatar, BorderLayout.WEST);

		JPanel titles = new JPanel();
		titles.setOpaque(false);
		titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
		titles.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

		titles.add(leftLabel(c.displayName(), palette.nameFont(), palette.text()));
		if (c.subtitle() != null) {
			titles.add(Box.createVerticalStrut(2));
			titles.add(leftLabel(c.subtitle(), palette.subtitleFont(), palette.secondary()));
		}
		if (c.nickname() != null) {
			titles.add(Box.createVerticalStrut(2));
			titles.add(leftLabel("“" + c.nickname() + "”", palette.subtitleFont().deriveFont(Font.ITALIC),
					palette.secondary()));
		}
		head.add(titles, BorderLayout.CENTER);
		return head;
	}

	private JComponent buildField(Field f) {
		JPanel row = new JPanel(new BorderLayout(12, 0));
		row.setOpaque(false);

		JLabel icon = new JLabel(new ContactGlyph(f.icon(), palette.accent(), 20));
		icon.setVerticalAlignment(SwingConstants.TOP);
		icon.setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));
		row.add(icon, BorderLayout.WEST);

		JPanel text = new JPanel();
		text.setOpaque(false);
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

		JTextArea value = new JTextArea(f.value());
		value.setEditable(false);
		value.setOpaque(false);
		value.setBorder(null);
		value.setLineWrap(true);
		value.setWrapStyleWord(true);
		value.setFont(palette.fieldFont());
		value.setForeground(palette.text());
		value.setSelectionColor(palette.selection());
		value.setSelectedTextColor(palette.text());
		value.setAlignmentX(Component.LEFT_ALIGNMENT);
		text.add(value);

		if (f.label() != null && !f.label().isBlank()) {
			JLabel label = leftLabel(f.label().toUpperCase(java.util.Locale.ROOT),
					palette.labelFont(), palette.secondary());
			label.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
			text.add(label);
		}

		row.add(text, BorderLayout.CENTER);
		return row;
	}

	private static JLabel leftLabel(String txt, Font font, Color color) {
		JLabel l = new JLabel(txt);
		l.setFont(font);
		l.setForeground(color);
		l.setAlignmentX(Component.LEFT_ALIGNMENT);
		return l;
	}

	private void applyPaletteColors() {
		setBackground(palette.background());
		content.setBackground(palette.background());
		scroll.getViewport().setBackground(palette.background());
		scroll.setBackground(palette.background());
		header.setBackground(palette.background());
		header.setForeground(palette.secondary());
		header.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 1, 0, palette.divider()),
				BorderFactory.createEmptyBorder(10, 16, 10, 16)));
	}

	private static String summary(int shown, int total, String fileName) {
		String count = shown == 1 ? "1 contact" : shown + " contacts";
		if (total > shown) {
			count += " (showing first " + shown + " of " + total + ")";
		}
		return count + "  ·  " + fileName;
	}

	// ── Rounded card component ───────────────────────────────────────────────────

	/** A card panel with a rounded background, hairline border and accent stripe. */
	private static final class RoundedCard extends JPanel {
		private static final int ARC = 18;
		private final Palette palette;

		RoundedCard(Palette palette) {
			this.palette = palette;
			setOpaque(false);
		}

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g.create();
			try {
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				int w = getWidth();
				int h = getHeight();
				g2.setColor(palette.cardBackground());
				g2.fillRoundRect(0, 0, w - 1, h - 1, ARC, ARC);
				// Accent stripe down the left edge.
				g2.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, w - 1, h - 1, ARC, ARC));
				g2.setColor(palette.accent());
				g2.fillRect(0, 0, 4, h);
				g2.setClip(null);
				g2.setColor(palette.cardBorder());
				g2.drawRoundRect(0, 0, w - 1, h - 1, ARC, ARC);
			} finally {
				g2.dispose();
			}
			super.paintComponent(g);
		}

		@Override
		public Dimension getMaximumSize() {
			return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
		}
	}

	// ── Theme palette ────────────────────────────────────────────────────────────

	/** Immutable set of theme-derived colours and fonts used while rendering. */
	private record Palette(
			Color background, Color cardBackground, Color cardBorder, Color divider,
			Color text, Color secondary, Color accent, Color selection,
			Font nameFont, Font subtitleFont, Font fieldFont, Font labelFont) {

		static Palette from(NuclrThemeScheme theme) {
			if (theme == null) {
				return fallback();
			}
			Color bg = theme.color("Panel.background", new Color(0x2b2b2b));
			Color fg = theme.color("Panel.foreground", new Color(0xdddddd));
			Color accent = theme.color("Table.selectionBackground", new Color(0x4b6eaf));
			Font base = theme.defaultFont() != null ? theme.defaultFont() : UIManager.getFont("defaultFont");
			return build(bg, fg, accent, base);
		}

		static Palette fallback() {
			Font base = UIManager.getFont("defaultFont");
			return build(new Color(0x2b2b2b), new Color(0xdddddd), new Color(0x4b6eaf), base);
		}

		private static Palette build(Color bg, Color fg, Color accent, Font base) {
			if (base == null) {
				base = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
			}
			Color cardBg = blend(bg, fg, 0.07f);
			Color cardBorder = blend(bg, fg, 0.20f);
			Color divider = blend(bg, fg, 0.12f);
			Color secondary = blend(fg, bg, 0.42f);
			Color selection = blend(bg, accent, 0.45f);
			return new Palette(
					bg, cardBg, cardBorder, divider,
					fg, secondary, accent, selection,
					base.deriveFont(Font.BOLD, base.getSize2D() + 3f),
					base.deriveFont(base.getSize2D() - 0.5f),
					base,
					base.deriveFont(base.getSize2D() - 2f));
		}

		private static Color blend(Color base, Color overlay, float overlayWeight) {
			float w = Math.max(0f, Math.min(1f, overlayWeight));
			float b = 1f - w;
			return new Color(
					Math.round(base.getRed() * b + overlay.getRed() * w),
					Math.round(base.getGreen() * b + overlay.getGreen() * w),
					Math.round(base.getBlue() * b + overlay.getBlue() * w));
		}
	}
}
