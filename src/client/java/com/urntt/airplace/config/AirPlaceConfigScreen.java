package com.urntt.airplace.config;

import com.urntt.airplace.AirPlaceClient;
import com.urntt.airplace.AirPlacement;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Configuration screen built from vanilla widgets. Every change is saved immediately.
 */
public final class AirPlaceConfigScreen extends OptionsSubScreen {
	private static final Component TITLE = Component.translatable("options.airplace.title");
	/** Width of an options list row, which notes wrap to. */
	private static final int ROW_WIDTH = 310;
	/** Lines of text that fit into an options list row. */
	private static final int NOTE_MAX_ROWS = 2;

	private @Nullable OptionInstance<Integer> minDistanceOption;
	private @Nullable OptionInstance<Integer> maxDistanceOption;

	public AirPlaceConfigScreen(final @Nullable Screen parent) {
		super(parent, Minecraft.getInstance().options, TITLE);
	}

	@Override
	protected void addOptions() {
		if (this.list == null) {
			return;
		}
		AirPlaceConfig config = AirPlaceClient.config();

		this.list.addHeader(Component.translatable("options.airplace.section.current"));
		this.list.addBig(toggle(AirPlaceClient.FEATURE_NAME_KEY, config.isEnabled(), config::setEnabled));

		this.list.addHeader(Component.translatable("options.airplace.section.distance"));
		this.list.addBig(this.note("options.airplace.distance_note"));
		this.minDistanceOption = distance("options.airplace.min_distance", 0, AirPlaceConfig.DISTANCE_LIMIT,
				config::minDistance, value -> {
					config.setMinDistance(value);
					this.syncDistanceOptions();
				});
		this.maxDistanceOption = distance("options.airplace.max_distance", 0, AirPlaceConfig.DISTANCE_LIMIT,
				config::maxDistance, value -> {
					config.setMaxDistance(value);
					this.syncDistanceOptions();
				});
		this.list.addBig(this.minDistanceOption);
		this.list.addBig(this.maxDistanceOption);
		this.list.addBig(distance("options.airplace.scroll_step", AirPlaceConfig.MIN_SCROLL_STEP,
				AirPlaceConfig.MAX_SCROLL_STEP, config::scrollStep, config::setScrollStep));

		this.list.addHeader(Component.translatable("options.airplace.section.defaults"));
		this.list.addBig(toggle("options.airplace.singleplayer_default", config.singleplayerDefault(),
				config::setSingleplayerDefault));
		this.list.addBig(toggle("options.airplace.multiplayer_default", config.multiplayerDefault(),
				config::setMultiplayerDefault));

		this.list.addHeader(Component.translatable("options.airplace.section.reset"));
		this.list.addBig(toggle("options.airplace.reset_on_world_exit", config.resetOnWorldExit(),
				config::setResetOnWorldExit));
		this.list.addBig(toggle("options.airplace.reset_on_game_exit", config.resetOnGameExit(),
				config::setResetOnGameExit));

		this.list.addHeader(Component.translatable("options.airplace.section.multiplayer"));
		this.list.addBig(this.note("options.airplace.multiplayer_warning"));
		this.list.addBig(this.note("options.airplace.multiplayer_default_off"));
		this.list.addBig(new OptionInstance<>(
				"options.airplace.multiplayer_mode",
				mode -> Tooltip.create(mode.description()),
				// The button itself prepends the caption, so this only names the value.
				(caption, mode) -> mode.label(),
				new OptionInstance.Enum<>(List.of(MultiplayerMode.values()), MultiplayerMode.CODEC),
				config.multiplayerMode(),
				config::setMultiplayerMode));
		this.list.addBig(Button.builder(Component.translatable("options.airplace.edit_servers"),
						button -> this.minecraft.gui.setScreen(new ServerListScreen(this, config)))
				.tooltip(Tooltip.create(Component.translatable("options.airplace.edit_servers.tooltip")))
				.build());
	}

	/**
	 * Shows the values the configuration holds on the minimum and maximum distance sliders, since changing one of
	 * them can move the other.
	 */
	private void syncDistanceOptions() {
		AirPlaceConfig config = AirPlaceClient.config();
		sync(this.minDistanceOption, config.minDistance());
		sync(this.maxDistanceOption, config.maxDistance());
	}

	private void sync(final @Nullable OptionInstance<Integer> option, final double value) {
		int hundredths = AirPlaceConfig.toHundredths(value);
		if (option != null && option.get() != hundredths) {
			option.set(hundredths);
			this.resetOption(option);
		}
	}

	/**
	 * Creates a gray note that wraps to the width of a row.
	 */
	private MultiLineTextWidget note(final String key) {
		return new MultiLineTextWidget(Component.translatable(key).withStyle(ChatFormatting.GRAY), this.font)
				.setMaxWidth(ROW_WIDTH)
				.setMaxRows(NOTE_MAX_ROWS);
	}

	/**
	 * Creates an on/off option whose tooltip is the translation of {@code captionKey + ".tooltip"}.
	 */
	private static OptionInstance<Boolean> toggle(final String captionKey, final boolean value,
			final Consumer<Boolean> onChange) {
		return OptionInstance.createBoolean(
				captionKey,
				OptionInstance.cachedConstantTooltip(Component.translatable(captionKey + ".tooltip")),
				value,
				onChange::accept);
	}

	/**
	 * Creates a slider for a distance in blocks with two decimals, which it stores in hundredths so the arrow keys
	 * move it by 0.01. Its tooltip is the translation of {@code captionKey + ".tooltip"}.
	 */
	private static OptionInstance<Integer> distance(final String captionKey, final double min, final double max,
			final DoubleSupplier value, final DoubleConsumer onChange) {
		return new OptionInstance<>(
				captionKey,
				OptionInstance.cachedConstantTooltip(Component.translatable(captionKey + ".tooltip")),
				(caption, hundredths) -> Options.genericValueLabel(caption,
						Component.literal(AirPlacement.format(AirPlaceConfig.fromHundredths(hundredths)))),
				new OptionInstance.IntRange(AirPlaceConfig.toHundredths(min), AirPlaceConfig.toHundredths(max)),
				AirPlaceConfig.toHundredths(value.getAsDouble()),
				hundredths -> onChange.accept(AirPlaceConfig.fromHundredths(hundredths)));
	}
}
