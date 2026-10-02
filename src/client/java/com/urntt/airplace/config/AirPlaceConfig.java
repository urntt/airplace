package com.urntt.airplace.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.urntt.airplace.AirPlaceClient;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The mod's settings, persisted as JSON. Every change is written to disk immediately.
 */
public final class AirPlaceConfig {
	/** Largest value the minimum and maximum distance accept, in blocks. */
	public static final double DISTANCE_LIMIT = 10.0;
	/** Smallest scroll step: one hundredth of a block, the precision of every distance setting. */
	public static final double MIN_SCROLL_STEP = fromHundredths(1);
	/** Largest scroll step, in blocks. */
	public static final double MAX_SCROLL_STEP = 1.0;

	private static final Logger LOGGER = LoggerFactory.getLogger(AirPlaceClient.MOD_ID);
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private final Path path;
	private final Settings settings;

	private AirPlaceConfig(final Path path, final Settings settings) {
		this.path = path;
		this.settings = settings;
	}

	/**
	 * Converts a distance in blocks to the nearest whole number of hundredths, the precision of every distance
	 * setting.
	 */
	public static int toHundredths(final double blocks) {
		return (int) Math.round(blocks * 100.0);
	}

	public static double fromHundredths(final int hundredths) {
		return hundredths / 100.0;
	}

	/**
	 * Returns the location of the config file in the Fabric config directory.
	 */
	public static Path defaultPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(AirPlaceClient.MOD_ID + ".json");
	}

	/**
	 * Loads the configuration from {@code path}. A missing file is created with the defaults. A readable file is
	 * rewritten with any settings it lacks. An unreadable file falls back to the defaults and is left untouched
	 * until the next change is saved.
	 */
	public static AirPlaceConfig load(final Path path) {
		if (Files.notExists(path)) {
			AirPlaceConfig config = new AirPlaceConfig(path, new Settings());
			config.save();
			return config;
		}

		Settings settings;
		try {
			settings = GSON.fromJson(Files.readString(path), Settings.class);
		} catch (IOException | JsonParseException e) {
			LOGGER.warn("Failed to read config file {}, using defaults", path, e);
			return new AirPlaceConfig(path, new Settings());
		}

		if (settings == null) {
			settings = new Settings();
		}
		settings.normalize();
		AirPlaceConfig config = new AirPlaceConfig(path, settings);
		config.save();
		return config;
	}

	/** The current state of the feature, as toggled by the key binding. */
	public boolean isEnabled() {
		return this.settings.enabled;
	}

	public void setEnabled(final boolean enabled) {
		this.settings.enabled = enabled;
		this.save();
	}

	/** The state a reset restores in singleplayer worlds. */
	public boolean singleplayerDefault() {
		return this.settings.singleplayerDefault;
	}

	public void setSingleplayerDefault(final boolean singleplayerDefault) {
		this.settings.singleplayerDefault = singleplayerDefault;
		this.save();
	}

	/** The state a reset restores on multiplayer servers that the multiplayer mode allows. */
	public boolean multiplayerDefault() {
		return this.settings.multiplayerDefault;
	}

	public void setMultiplayerDefault(final boolean multiplayerDefault) {
		this.settings.multiplayerDefault = multiplayerDefault;
		this.save();
	}

	/** Whether every world starts from its default state. */
	public boolean resetOnWorldExit() {
		return this.settings.resetOnWorldExit;
	}

	public void setResetOnWorldExit(final boolean resetOnWorldExit) {
		this.settings.resetOnWorldExit = resetOnWorldExit;
		this.save();
	}

	/** Whether the first world after starting the game starts from its default state. */
	public boolean resetOnGameExit() {
		return this.settings.resetOnGameExit;
	}

	public void setResetOnGameExit(final boolean resetOnGameExit) {
		this.settings.resetOnGameExit = resetOnGameExit;
		this.save();
	}

	public MultiplayerMode multiplayerMode() {
		return this.settings.multiplayerMode;
	}

	public void setMultiplayerMode(final MultiplayerMode multiplayerMode) {
		this.settings.multiplayerMode = multiplayerMode;
		this.save();
	}

	/** The server list used by the whitelist and blacklist modes. */
	public List<String> servers() {
		return List.copyOf(this.settings.servers);
	}

	public void setServers(final List<String> servers) {
		this.settings.servers = new ArrayList<>(servers);
		this.settings.normalize();
		this.save();
	}

	/** The placement distance in blocks, from the player's eyes along the view direction. */
	public double distance() {
		return this.settings.distance;
	}

	/**
	 * Sets the placement distance, rounded to two decimals and kept within the minimum and maximum distance.
	 *
	 * @return the distance that was stored
	 */
	public double setDistance(final double distance) {
		this.settings.distance = distance;
		this.settings.normalize();
		this.save();
		return this.settings.distance;
	}

	/** The shortest placement distance the mouse wheel can select. */
	public double minDistance() {
		return this.settings.minDistance;
	}

	/**
	 * Sets the minimum distance, raising the maximum distance if it would be smaller.
	 */
	public void setMinDistance(final double minDistance) {
		this.settings.minDistance = minDistance;
		this.settings.maxDistance = Math.max(this.settings.maxDistance, minDistance);
		this.settings.normalize();
		this.save();
	}

	/** The longest placement distance the mouse wheel can select. */
	public double maxDistance() {
		return this.settings.maxDistance;
	}

	/**
	 * Sets the maximum distance, lowering the minimum distance if it would be larger.
	 */
	public void setMaxDistance(final double maxDistance) {
		this.settings.maxDistance = maxDistance;
		this.settings.minDistance = Math.min(this.settings.minDistance, maxDistance);
		this.settings.normalize();
		this.save();
	}

	/** How much one mouse wheel notch changes the placement distance, in blocks. */
	public double scrollStep() {
		return this.settings.scrollStep;
	}

	public void setScrollStep(final double scrollStep) {
		this.settings.scrollStep = scrollStep;
		this.settings.normalize();
		this.save();
	}

	private void save() {
		try {
			Files.createDirectories(this.path.getParent());
			Files.writeString(this.path, GSON.toJson(this.settings));
		} catch (IOException e) {
			LOGGER.error("Failed to write config file {}", this.path, e);
		}
	}

	/**
	 * The serialized form. Field initializers are the defaults, which also apply to keys missing from the file.
	 */
	private static final class Settings {
		private boolean enabled = true;
		private boolean singleplayerDefault = true;
		private boolean multiplayerDefault = true;
		private boolean resetOnWorldExit = false;
		private boolean resetOnGameExit = false;
		private MultiplayerMode multiplayerMode = MultiplayerMode.DISABLED;
		private List<String> servers = new ArrayList<>();
		private double distance = 3.0;
		private double minDistance = 0.0;
		private double maxDistance = 4.5;
		private double scrollStep = 0.1;

		/**
		 * Replaces values Gson could not map (unknown mode names, a missing list, numbers that are not finite), tidies
		 * the server list, and brings the distance settings into range with two decimals.
		 */
		private void normalize() {
			Settings defaults = new Settings();
			if (this.multiplayerMode == null) {
				this.multiplayerMode = defaults.multiplayerMode;
			}
			List<String> entries = new ArrayList<>();
			if (this.servers != null) {
				for (String entry : this.servers) {
					if (entry != null && !entry.isBlank()) {
						entries.add(entry.trim());
					}
				}
			}
			this.servers = entries;

			this.maxDistance = normalizeNumber(this.maxDistance, defaults.maxDistance, 0.0, DISTANCE_LIMIT);
			this.minDistance = normalizeNumber(this.minDistance, defaults.minDistance, 0.0, this.maxDistance);
			this.distance = normalizeNumber(this.distance, defaults.distance, this.minDistance, this.maxDistance);
			this.scrollStep = normalizeNumber(this.scrollStep, defaults.scrollStep, MIN_SCROLL_STEP, MAX_SCROLL_STEP);
		}

		private static double normalizeNumber(final double value, final double fallback, final double min, final double max) {
			double finite = Double.isFinite(value) ? value : fallback;
			return fromHundredths(toHundredths(Math.clamp(finite, min, max)));
		}
	}
}
