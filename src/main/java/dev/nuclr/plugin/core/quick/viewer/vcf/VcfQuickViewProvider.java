package dev.nuclr.plugin.core.quick.viewer.vcf;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JComponent;

import dev.nuclr.platform.NuclrThemeScheme;
import dev.nuclr.platform.plugin.NuclrPluginContext;
import dev.nuclr.platform.plugin.NuclrResource;
import dev.nuclr.platform.plugin.QuickViewNuclrPlugin;
import ezvcard.Ezvcard;
import ezvcard.VCard;
import lombok.extern.slf4j.Slf4j;

/**
 * Quick-view provider for vCard / {@code .vcf} contact files. Renders each
 * contact as a styled card via {@link VcfQuickViewPanel}.
 */
@Slf4j
public class VcfQuickViewProvider implements QuickViewNuclrPlugin {

	private static final String ID = "dev.nuclr.plugin.core.quickviewer.vcf";

	private NuclrPluginContext context;
	private VcfQuickViewPanel panel;
	private NuclrThemeScheme theme;
	private NuclrResource currentResource;
	private AtomicBoolean currentCancelled;


	@Override
	public JComponent panel() {
		if (panel == null) {
			panel = new VcfQuickViewPanel();
			panel.applyTheme(theme);
		}
		return panel;
	}

	@Override
	public void preinit(NuclrPluginContext context) {
		this.context = context;
		applyTheme(context != null ? context.getTheme() : null);
	}

	@Override
	public void init() {
	}

	@Override
	public NuclrPluginContext getContext() {
		return this.context;
	}

	@Override
	public void unload() {
		closeResource();
		panel = null;
		context = null;
	}

	@Override
	public boolean supports(NuclrResource resource) {
		return VcfFileSupport.supports(resource);
	}


	@Override
	public boolean openResource(NuclrResource resource, AtomicBoolean cancelled) {
		if (currentCancelled != null) {
			currentCancelled.set(true);
		}
		currentResource = resource;
		currentCancelled = cancelled;
		panel();
		return panel.load(resource, cancelled);
	}

	@Override
	public boolean supportsThumbnails() {
		return true;
	}

	/**
	 * The first contact's avatar, as the card shows it: their photo in a circle,
	 * or their initials on a colour derived from the name.
	 */
	@Override
	public BufferedImage thumbnail(NuclrResource resource, int maxWidth, int maxHeight, AtomicBoolean cancelled) {
		if (maxWidth <= 0 || maxHeight <= 0 || !supports(resource)
				|| resource.getLength() > VcfQuickViewPanel.MAX_FILE_SIZE) {
			return null;
		}
		try (var in = resource.openInputStream()) {
			VCard card = Ezvcard.parse(in).first();
			if (card == null || (cancelled != null && cancelled.get())) {
				return null;
			}
			Contact contact = Contact.from(card);
			int size = Math.min(maxWidth, maxHeight);
			BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			try {
				new AvatarIcon(contact.photo(), contact.displayName(), size, null).paintIcon(null, g, 0, 0);
			} finally {
				g.dispose();
			}
			return image;
		} catch (Exception e) {
			log.debug("No thumbnail for {}: {}", resource.getName(), e.toString());
			return null;
		}
	}

	@Override
	public void closeResource() {
		if (currentCancelled != null) {
			currentCancelled.set(true);
			currentCancelled = null;
		}
		if (panel != null) {
			panel.clear();
		}
	}

	public void applyTheme(NuclrThemeScheme theme) {
		this.theme = theme;
		if (panel != null) {
			panel.applyTheme(theme);
		}
	}

	@Override
	public void updateTheme(NuclrThemeScheme themeScheme) {
		applyTheme(themeScheme);
	}

	@Override
	public boolean onFocusGained() {
		return false;
	}

	@Override
	public void onFocusLost() {
	}

	@Override
	public boolean isFocused() {
		return false;
	}

	@Override
	public NuclrResource getCurrentResource() {
		return currentResource;
	}

	@Override
	public String getWindowTitle() {
		return "Quick View: " + (currentResource != null ? currentResource.getName() : "");
	}

	// ── Plugin metadata ──────────────────────────────────────────────────────────

	@Override
	public String uuid() {
		return ID;
	}



}
