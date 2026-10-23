/*
 * PlayerESP — full entity radar for packet-filtered servers (DonutSMP etc).
 * Uses only already-received player entities + optional sound side-channel.
 * No chunk requests, no extra C2S packets.
 */
package net.wimods.chestesp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.wimods.chestesp.util.RenderUtils;

public final class PlayerEsp
{
	private static final Minecraft MC = Minecraft.getInstance();
	
	public static final class Tracked
	{
		public final UUID id;
		public String name = "?";
		public Vec3 pos = Vec3.ZERO;
		public Vec3 lastPos = Vec3.ZERO;
		public long lastUpdate;
		public long lastSound;
		public boolean underground;
		public boolean fromSoundOnly;
		public float health = -1f;
		public float absorption = 0f;
		public String held = "";
		public int armorPieces;
		public double speed;
		
		public Tracked(UUID id)
		{
			this.id = id;
		}
		
		public Vec3 interpolated(float pt)
		{
			if(lastPos == null || pos == null)
				return pos == null ? Vec3.ZERO : pos;
			return lastPos.lerp(pos, Math.min(1f, pt * 2f));
		}
	}
	
	private final Map<UUID, Tracked> tracked = new ConcurrentHashMap<>();
	private final Map<Long, SoundHit> recentSounds = new ConcurrentHashMap<>();
	
	private static final class SoundHit
	{
		final Vec3 pos;
		final long time;
		
		SoundHit(Vec3 pos, long time)
		{
			this.pos = pos;
			this.time = time;
		}
	}
	
	/** Call from PacketIntel or sound packet handler when a positional sound arrives. */
	public void onPositionalSound(double x, double y, double z, String soundId)
	{
		if(soundId == null)
			return;
		String s = soundId.toLowerCase(Locale.ROOT);
		// footsteps, dig, place, splash, eat — anything that implies a player body
		if(!(s.contains("step") || s.contains("dig") || s.contains("hit")
			|| s.contains("place") || s.contains("splash") || s.contains("eat")
			|| s.contains("burp") || s.contains("hurt") || s.contains("fall")))
			return;
		
		long now = System.currentTimeMillis();
		long key = (Math.round(x) << 32) ^ (Math.round(y) << 16) ^ Math.round(z);
		recentSounds.put(key, new SoundHit(new Vec3(x, y, z), now));
		
		// try to attach to nearest tracked player within 4 blocks
		Tracked best = null;
		double bestD = 4.0;
		for(Tracked t : tracked.values())
		{
			if(t.pos == null)
				continue;
			double d = t.pos.distanceTo(new Vec3(x, y, z));
			if(d < bestD)
			{
				bestD = d;
				best = t;
			}
		}
		if(best != null)
			best.lastSound = now;
		else if(y < 80) // orphan deep sound — create temporary marker
		{
			UUID phantom = new UUID(0xSOUND0000000000L, key);
			Tracked t = tracked.computeIfAbsent(phantom, Tracked::new);
			t.name = "sound";
			t.lastPos = t.pos;
			t.pos = new Vec3(x, y, z);
			t.lastUpdate = now;
			t.lastSound = now;
			t.underground = y < 60;
			t.fromSoundOnly = true;
		}
	}
	
	public void tick(ChestEspConfig config)
	{
		if(!config.player_esp_enable || MC.level == null || MC.player == null)
			return;
		
		long now = System.currentTimeMillis();
		Set<String> hide = parseNameList(config.player_esp_hide_names);
		Set<String> focus = parseNameList(config.player_esp_focus_names);
		
		for(Player p : MC.level.players())
		{
			if(p == MC.player)
				continue;
			if(p.isInvisible() && config.player_esp_ignore_invisible)
				continue;
			
			String name = p.getName().getString();
			if(hide.contains(name.toLowerCase(Locale.ROOT)))
				continue;
			if(!focus.isEmpty() && !focus.contains(name.toLowerCase(Locale.ROOT)))
				continue;
			
			UUID id = p.getUUID();
			Tracked t = tracked.computeIfAbsent(id, Tracked::new);
			t.fromSoundOnly = false;
			t.name = name;
			t.lastPos = t.pos;
			t.pos = p.getBoundingBox().getCenter();
			t.lastUpdate = now;
			t.underground = t.pos.y < config.player_esp_underground_y;
			
			if(config.player_esp_show_health)
			{
				t.health = p.getHealth();
				t.absorption = p.getAbsorptionAmount();
			}
			if(config.player_esp_show_held)
			{
				ItemStack main = p.getMainHandItem();
				t.held = main.isEmpty() ? ""
					: main.getItem().getName(main).getString();
			}
			if(config.player_esp_show_armor)
			{
				int pieces = 0;
				for(EquipmentSlot slot : new EquipmentSlot[]{
					EquipmentSlot.HEAD, EquipmentSlot.CHEST,
					EquipmentSlot.LEGS, EquipmentSlot.FEET})
					if(!p.getItemBySlot(slot).isEmpty())
						pieces++;
				t.armorPieces = pieces;
			}
			if(t.lastPos != null && t.pos != null)
			{
				double dx = t.pos.x - t.lastPos.x;
				double dz = t.pos.z - t.lastPos.z;
				t.speed = Math.sqrt(dx * dx + dz * dz) * 20.0; // rough b/s
			}
		}
		
		// prune
		long staleMs = config.player_esp_stale_seconds * 1000L;
		long soundStale = 4000L;
		tracked.entrySet().removeIf(e -> {
			Tracked t = e.getValue();
			if(t.fromSoundOnly)
				return now - t.lastUpdate > soundStale;
			return now - t.lastUpdate > staleMs;
		});
		recentSounds.entrySet().removeIf(e -> now - e.getValue().time > 5000);
	}
	
	public void render(PoseStack matrices, float partialTicks, ChestEspConfig config)
	{
		if(!config.player_esp_enable || MC.player == null)
			return;
		
		Vec3 cam = RenderUtils.getCameraPos();
		double maxDist = config.player_esp_max_distance;
		boolean depthTest = !config.player_esp_through_walls;
		
		ArrayList<AABB> boxes = new ArrayList<>();
		ArrayList<Integer> boxColors = new ArrayList<>();
		ArrayList<Vec3> tracers = new ArrayList<>();
		ArrayList<Integer> tracerColors = new ArrayList<>();
		ArrayList<Label> labels = new ArrayList<>();
		
		Tracked nearest = null;
		double nearestDist = Double.MAX_VALUE;
		
		for(Tracked t : tracked.values())
		{
			if(t.pos == null)
				continue;
			Vec3 drawPos = config.player_esp_smooth ? t.interpolated(partialTicks) : t.pos;
			double dist = cam.distanceTo(drawPos);
			if(dist > maxDist)
				continue;
			if(config.player_esp_underground_only && !t.underground)
				continue;
			
			if(dist < nearestDist)
			{
				nearestDist = dist;
				nearest = t;
			}
			
			boolean isNearest = nearest == t;
			int rgb = t.fromSoundOnly ? config.player_esp_sound_color
				: (t.underground ? config.player_esp_underground_color
					: config.player_esp_surface_color);
			if(isNearest && config.player_esp_pulse_nearest)
			{
				float pulse = (float)(0.6 + 0.4 * Math.sin(System.currentTimeMillis() / 180.0));
				rgb = scaleRgb(rgb, pulse);
			}
			
			int alpha = config.player_esp_through_walls ? 170 : 230;
			if(t.fromSoundOnly)
				alpha = 120;
			int color = (alpha << 24) | (rgb & 0xFFFFFF);
			
			double halfW = 0.4;
			double halfH = 0.9;
			AABB box = new AABB(drawPos.x - halfW, drawPos.y - halfH, drawPos.z - halfW,
				drawPos.x + halfW, drawPos.y + halfH, drawPos.z + halfW);
			
			if(config.player_esp_boxes)
			{
				boxes.add(box);
				boxColors.add(color);
			}
			if(config.player_esp_tracers
				&& (!config.player_esp_tracers_nearest_only || isNearest))
			{
				tracers.add(drawPos);
				tracerColors.add(color);
			}
			if(config.player_esp_labels && dist <= config.player_esp_label_distance)
			{
				StringBuilder sb = new StringBuilder(t.name);
				if(config.player_esp_show_distance)
					sb.append(' ').append((int)dist).append('m');
				if(t.underground)
					sb.append(" [U]");
				if(t.fromSoundOnly)
					sb.append(" ~");
				if(config.player_esp_show_health && t.health >= 0)
					sb.append(String.format(" %.0fhp", t.health + t.absorption));
				if(config.player_esp_show_armor && t.armorPieces > 0)
					sb.append(" A").append(t.armorPieces);
				if(config.player_esp_show_held && t.held != null && !t.held.isEmpty())
					sb.append(" {").append(truncate(t.held, 12)).append('}');
				if(config.player_esp_show_speed && t.speed > 0.5)
					sb.append(String.format(" %.1fbs", t.speed));
				labels.add(new Label(drawPos.add(0, 1.15, 0), sb.toString(), color, dist));
			}
		}
		
		if(config.player_esp_boxes && !boxes.isEmpty())
		{
			RenderUtils.drawOutlinedBoxes(matrices, boxes, boxColors, depthTest);
			if(config.player_esp_fill)
				RenderUtils.drawSolidBoxes(matrices, boxes, boxColors, depthTest);
		}
		if(config.player_esp_tracers && !tracers.isEmpty())
			RenderUtils.drawTracers(matrices, partialTicks, tracers, tracerColors, depthTest);
		
		if(config.player_esp_labels && !labels.isEmpty())
		{
			labels.sort(Comparator.comparingDouble(l -> l.dist));
			int max = Math.min(labels.size(), config.player_esp_max_labels);
			ArrayList<Vec3> pos = new ArrayList<>();
			ArrayList<String> text = new ArrayList<>();
			ArrayList<Integer> col = new ArrayList<>();
			for(int i = 0; i < max; i++)
			{
				Label l = labels.get(i);
				pos.add(l.pos);
				text.add(l.text);
				col.add(l.color);
			}
			RenderUtils.drawWorldLabels(matrices, pos, text, col);
		}
	}
	
	public int count()
	{
		return tracked.size();
	}
	
	public int undergroundCount()
	{
		int n = 0;
		for(Tracked t : tracked.values())
			if(t.underground && !t.fromSoundOnly)
				n++;
		return n;
	}
	
	public String nearestSummary(ChestEspConfig config)
	{
		if(!config.player_esp_enable || MC.player == null)
			return "-";
		Vec3 cam = RenderUtils.getCameraPos();
		Tracked best = null;
		double bestD = Double.MAX_VALUE;
		for(Tracked t : tracked.values())
		{
			if(t.pos == null || t.fromSoundOnly)
				continue;
			double d = cam.distanceTo(t.pos);
			if(d < bestD)
			{
				bestD = d;
				best = t;
			}
		}
		if(best == null)
			return "-";
		return best.name + " " + (int)bestD + "m" + (best.underground ? " [U]" : "");
	}
	
	public void clear()
	{
		tracked.clear();
		recentSounds.clear();
	}
	
	private static Set<String> parseNameList(String raw)
	{
		java.util.HashSet<String> set = new java.util.HashSet<>();
		if(raw == null || raw.isBlank())
			return set;
		for(String part : raw.split(","))
		{
			String s = part.trim().toLowerCase(Locale.ROOT);
			if(!s.isEmpty())
				set.add(s);
		}
		return set;
	}
	
	private static String truncate(String s, int max)
	{
		if(s == null)
			return "";
		return s.length() <= max ? s : s.substring(0, max - 1) + "…";
	}
	
	private static int scaleRgb(int rgb, float factor)
	{
		int r = Math.min(255, (int)(((rgb >> 16) & 0xFF) * factor));
		int g = Math.min(255, (int)(((rgb >> 8) & 0xFF) * factor));
		int b = Math.min(255, (int)((rgb & 0xFF) * factor));
		return (r << 16) | (g << 8) | b;
	}
	
	private record Label(Vec3 pos, String text, int color, double dist)
	{}
}
