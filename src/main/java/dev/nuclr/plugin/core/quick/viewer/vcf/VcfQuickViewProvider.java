package dev.nuclr.plugin.core.quick.viewer.vcf;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JComponent;

import dev.nuclr.platform.NuclrThemeScheme;
import dev.nuclr.platform.plugin.NuclrPluginContext;
import dev.nuclr.platform.plugin.NuclrResource;
import dev.nuclr.platform.plugin.QuickViewNuclrPlugin;
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
