/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.chestesp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigHolder;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.wimods.chestesp.util.ChunkUtils;
import net.wimods.chestesp.util.PlausibleAnalytics;
import net.wimods.chestesp.util.RenderUtils;

public final class ChestEspMod
{
	private static final Minecraft MC = Minecraft.getInstance();
	public static final Logger LOGGER = LoggerFactory.getLogger("ChestESP");
	
	private final ConfigHolder<ChestEspConfig> configHolder;
	private final PlausibleAnalytics plausible;
	private final ChestEspGroupManager groups;
	private final ChestEspHud hud = new ChestEspHud();
	private final KeyMapping toggleKey;
	private final KeyMapping toggleFillKey;
	private final KeyMapping toggleLabelsKey;
	private final KeyMapping cycleStyleKey;
	private final Set<Long> seenKeys = new HashSet<>();
	private final Set<Long> openedKeys = new HashSet<>();
	
	private boolean enabled;
	private int ticksUntilScan;
	
	public ChestEspMod()
	{
		LOGGER.info("Starting ChestESP...");
		
		configHolder = AutoConfig.register(ChestEspConfig.class,
			GsonConfigSerializer::new);
		
		groups = new ChestEspGroupManager(configHolder);
		
		KeyMapping.Category kbCategory = KeyMapping.Category
			.register(Identifier.fromNamespaceAndPath("chestesp", "chestesp"));
		toggleKey = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.chestesp.toggle", InputConstants.Type.KEYSYM,
				InputConstants.UNKNOWN.getValue(), kbCategory, 0));
		toggleFillKey = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.chestesp.toggle_fill", InputConstants.Type.KEYSYM,
				InputConstants.UNKNOWN.getValue(), kbCategory, 0));
		toggleLabelsKey = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.chestesp.toggle_labels",
				InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(),
				kbCategory, 0));
		cycleStyleKey = KeyBindingHelper.registerKeyBinding(
			new KeyMapping("key.chestesp.cycle_style", InputConstants.Type.KEYSYM,
				InputConstants.UNKNOWN.getValue(), kbCategory, 0));
		
		ClientTickEvents.END_CLIENT_TICK.register(client -> handleKeys());
		
		ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
			if(!isEnabled())
				return;
			
			chunk.getBlockEntities().values().forEach(
				be -> groups.blockGroups.forEach(group -> group.addIfMatches(be)));
		});
		
		ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
			seenKeys.clear();
			openedKeys.clear();
			hud.setLine("");
		});
		
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if(player == MC.player)
				openedKeys.add(hit.getBlockPos().asLong());
			return InteractionResult.PASS;
		});
		
		HudElementRegistry.addLast(
			Identifier.fromNamespaceAndPath("chestesp", "hud"), hud::render);
		
		plausible = new PlausibleAnalytics(configHolder, groups, toggleKey);
		plausible.pageview("/");
	}
	
	private void handleKeys()
	{
		ChestEspConfig config = configHolder.getConfig();
		boolean changed = false;
		
		while(toggleKey.consumeClick())
		{
			setEnabled(!config.enable);
			changed = true;
		}
		while(toggleFillKey.consumeClick())
		{
			config.fill_boxes = !config.fill_boxes;
			changed = true;
		}
		while(toggleLabelsKey.consumeClick())
		{
			config.show_labels = !config.show_labels;
			changed = true;
		}
		while(cycleStyleKey.consumeClick())
		{
			ChestEspStyle[] values = ChestEspStyle.values();
			config.style = values[(config.style.ordinal() + 1) % values.length];
			changed = true;
		}
		
		if(changed)
			configHolder.save();
	}
	
	public void setEnabled(boolean enabled)
	{
		if(this.enabled == enabled)
			return;
		
		LOGGER.info("{} ChestESP.", enabled ? "Enabling" : "Disabling");
		
		if(configHolder.getConfig().enable != enabled)
		{
			configHolder.getConfig().enable = enabled;
			configHolder.save();
		}
		
		this.enabled = enabled;
		
		if(!enabled)
		{
			groups.allGroups.forEach(ChestEspGroup::clear);
			seenKeys.clear();
			hud.setLine("");
			return;
		}
		
		if(MC.level != null)
			MC.levelRenderer.allChanged();
		
		ticksUntilScan = 0;
		onUpdate();
	}
	
	public void onUpdate()
	{
		setEnabled(configHolder.getConfig().enable);
		if(!isEnabled())
			return;
		
		if(ticksUntilScan-- > 0)
			return;
		
		ticksUntilScan = Math.max(1, configHolder.getConfig().scan_interval_ticks);
		scanLoadedContainers();
	}
	
	private void scanLoadedContainers()
	{
		groups.allGroups.forEach(ChestEspGroup::clear);
		
		ChunkUtils.getLoadedBlockEntities().forEach(
			be -> groups.blockGroups.forEach(group -> group.addIfMatches(be)));
		
		if(MC.level != null)
			MC.level.entitiesForRendering().forEach(
				e -> groups.entityGroups.forEach(group -> group.addIfMatches(e)));
		
		groups.entityGroups.forEach(group -> group.updateBoxes(0));
		notifyNewTargets();
	}
	
	private void notifyNewTargets()
	{
		Set<Long> current = new HashSet<>();
		for(ChestEspGroup group : groups.allGroups)
			for(ChestEspTarget target : group.getTargets())
				current.add(target.key);
		
		boolean ping = configHolder.getConfig().notify_new && !seenKeys.isEmpty();
		if(ping)
			for(long key : current)
				if(!seenKeys.contains(key))
				{
					MC.getSoundManager().play(SimpleSoundInstance.forUI(
						SoundEvents.NOTE_BLOCK_HAT.value(), 1.4F, 0.25F));
					break;
				}
		
		seenKeys.clear();
		seenKeys.addAll(current);
	}
	
	public boolean shouldCancelViewBobbing()
	{
		return enabled && configHolder.getConfig().style.hasLines();
	}
	
	public void onRender(PoseStack matrixStack, float partialTicks)
	{
		ChestEspConfig config = configHolder.getConfig();
		if(config.hide_when_gui_open && MC.screen != null)
		{
			hud.setLine("");
			return;
		}
		
		groups.entityGroups.stream().filter(ChestEspGroup::isEnabled)
			.forEach(g -> g.updateBoxes(partialTicks));
		
		ChestEspStyle style = config.style;
		boolean depthTest = !config.through_walls;
		
		ArrayList<AABB> fillBoxes = new ArrayList<>();
		ArrayList<Integer> fillColors = new ArrayList<>();
		ArrayList<AABB> outlineBoxes = new ArrayList<>();
		ArrayList<Integer> outlineColors = new ArrayList<>();
		ArrayList<Vec3> tracerEnds = new ArrayList<>();
		ArrayList<Integer> tracerColors = new ArrayList<>();
		ArrayList<Label> labels = new ArrayList<>();
		
		Vec3 cam = RenderUtils.getCameraPos();
		double maxDist = config.max_distance;
		boolean fill = config.fill_boxes;
		boolean outline = config.outline_boxes;
		if(!fill && !outline)
			outline = true;
		
		int fillBase = Math.round(config.fill_opacity * 255 / 100F);
		int lineBase = Math.round(config.outline_opacity * 255 / 100F);
		
		ChestEspTarget nearest = null;
		double nearestDist = Double.MAX_VALUE;
		int visible = 0;
		
		record Candidate(ChestEspGroup group, ChestEspTarget target, double dist,
			boolean opened)
		{}
		ArrayList<Candidate> candidates = new ArrayList<>();
		
		for(ChestEspGroup group : groups.allGroups)
		{
			if(!group.isEnabled())
				continue;
			
			for(ChestEspTarget target : group.getTargets())
			{
				Vec3 center = target.center();
				double dist = cam.distanceTo(center);
				if(dist > maxDist)
					continue;
				if(center.y < config.min_y || center.y > config.max_y)
					continue;
				
				boolean opened = openedKeys.contains(target.key);
				if(config.hide_opened && opened)
					continue;
				
				candidates.add(new Candidate(group, target, dist, opened));
				visible++;
				if(dist < nearestDist)
				{
					nearestDist = dist;
					nearest = target;
				}
			}
		}
		
		for(Candidate c : candidates)
		{
			ChestEspTarget target = c.target;
			int rgb = c.opened ? config.opened_color
				: ChestEspTarget.chromaRgb(c.group.getColorI(0) & 0xFFFFFF, config,
					c.dist);
			boolean isNearest =
				config.highlight_nearest && nearest != null && target == nearest;
			int fillA = ChestEspTarget.fadeAlpha(fillBase, c.dist, maxDist,
				config.fade_by_distance);
			int lineA = ChestEspTarget.fadeAlpha(lineBase, c.dist, maxDist,
				config.fade_by_distance);
			if(isNearest)
			{
				fillA = Math.min(255, fillA + 40);
				lineA = 255;
			}
			
			if(style.hasBoxes())
			{
				if(fill)
				{
					fillBoxes.add(target.box);
					fillColors.add(rgbWithAlpha(rgb, fillA));
				}
				if(outline)
				{
					outlineBoxes.add(target.box);
					outlineColors.add(rgbWithAlpha(rgb, lineA));
				}
			}
			
			boolean drawTracer = style.hasLines()
				&& (!config.tracers_nearest_only || isNearest);
			if(drawTracer)
			{
				tracerEnds.add(target.center());
				tracerColors.add(rgbWithAlpha(rgb, lineA));
			}
			
			if(config.show_labels && c.dist <= config.label_max_distance)
				labels.add(new Label(
					target.center().add(0, target.box.getYsize() * 0.5 + 0.2, 0),
					target.displayText(c.dist, config), rgbWithAlpha(rgb, lineA),
					c.dist));
		}
		
		labels.sort(Comparator.comparingDouble(l -> l.dist));
		if(labels.size() > config.max_labels)
			labels.subList(config.max_labels, labels.size()).clear();
		
		if(style.hasBoxes())
		{
			if(fill)
				RenderUtils.drawSolidBoxes(matrixStack, fillBoxes, fillColors,
					depthTest);
			if(outline)
				RenderUtils.drawOutlinedBoxes(matrixStack, outlineBoxes,
					outlineColors, depthTest);
		}
		
		if(style.hasLines())
			RenderUtils.drawTracers(matrixStack, partialTicks, tracerEnds,
				tracerColors, depthTest);
		
		if(config.show_labels)
		{
			ArrayList<Vec3> labelPos = new ArrayList<>();
			ArrayList<String> labelText = new ArrayList<>();
			ArrayList<Integer> labelColors = new ArrayList<>();
			for(Label label : labels)
			{
				labelPos.add(label.pos);
				labelText.add(label.text);
				labelColors.add(label.color);
			}
			RenderUtils.drawWorldLabels(matrixStack, labelPos, labelText,
				labelColors);
		}
		
		if(config.show_hud && enabled)
		{
			String nearestText = nearest == null ? "-"
				: nearest.label + " " + (int)nearestDist + "m";
			hud.setLine("ChestESP  " + visible + "  nearest " + nearestText);
		}else
			hud.setLine("");
	}
	
	private static int rgbWithAlpha(int rgb, int alpha)
	{
		return (alpha & 0xFF) << 24 | (rgb & 0xFFFFFF);
	}
	
	private record Label(Vec3 pos, String text, int color, double dist)
	{}
	
	public static ChestEspMod getInstance()
	{
		return ChestEspModInitializer.getInstance();
	}
	
	public boolean isEnabled()
	{
		return enabled;
	}
	
	public ConfigHolder<ChestEspConfig> getConfigHolder()
	{
		return configHolder;
	}
	
	public PlausibleAnalytics getPlausible()
	{
		return plausible;
	}
}
