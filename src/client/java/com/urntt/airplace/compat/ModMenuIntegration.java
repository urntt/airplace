package com.urntt.airplace.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.urntt.airplace.config.AirPlaceConfigScreen;

/**
 * Mod Menu entrypoint. This is the only class that may reference Mod Menu, which is an optional dependency.
 */
public final class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return AirPlaceConfigScreen::new;
	}
}
