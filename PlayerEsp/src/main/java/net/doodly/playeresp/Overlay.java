package net.doodly.playeresp;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;

import com.mojang.blaze3d.vertex.PoseStack;

import net.doodly.playeresp.Tracker.Tracked;
import net.doodly.playeresp.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Overlay
{
	private static final Minecraft MC = Minecraft.getInstance();
	
	public void drawWorld(PoseStack matrices, float partialTicks,
		Tracker tracker, PlayerEspConfig config)
	{
		if(MC.level == null || MC.player == null) return;
		
		boolean depthTest = !config.throughWalls;
		ArrayList<AABB> boxes = new ArrayList<>();
		ArrayList<Integer> boxColors = new ArrayList<>();
		ArrayList<Vec3> tracers = new ArrayList<>();
		ArrayList<Integer> tracerColors = new ArrayList<>();
		ArrayList<Label> labels = new ArrayList<>();
		
		Vec3 cam = RenderUtils.getCameraPos();
		Tracked nearest = null;
		double nearestD = Double.MAX_VALUE;
		
		for(Tracked t : tracker.all().values())
		{
			if(t.pos == null || t.fromSoundOnly) continue;
			if(config.undergroundOnly && !t.underground) continue;
			Vec3 pos = config.smooth ? t.interpolated(partialTicks) : t.pos;
			double dist = cam.distanceTo(pos);
			if(dist > config.maxDistance) continue;
			if(dist < nearestD) { nearestD = dist; nearest = t; }
		}
		
		for(Tracked t : tracker.all().values())
		{
			if(t.pos == null) continue;
			if(config.undergroundOnly && !t.underground) continue;
			
			Vec3 pos = config.smooth ? t.interpolated(partialTicks) : t.pos;
			double dist = cam.distanceTo(pos);
			if(dist > config.maxDistance) continue;
			
			int color = pickColor(t, config);
			if(config.pulseNearest && t == nearest)
				color = scaleRgb(color, 1.35f);
			
			if(config.boxes)
			{
				double h = t.fromSoundOnly ? 0.6 : 1.8;
				double w = t.fromSoundOnly ? 0.35 : 0.3;
				boxes.add(new AABB(pos.x - w, pos.y, pos.z - w,
					pos.x + w, pos.y + h, pos.z + w));
				boxColors.add(color | 0xFF000000);
			}
			
			boolean doTracer = config.tracers;
			if(doTracer && config.tracersNearestOnly && t != nearest)
				doTracer = false;
			if(doTracer)
			{
				Vec3 tip = config.predict
					? t.predicted(partialTicks, 0.15f)
					: pos.add(0, t.fromSoundOnly ? 0.3 : 0.9, 0);
				tracers.add(tip);
				tracerColors.add(color | 0xFF000000);
			}
			
			if(config.labels && dist <= config.labelDistance)
			{
				// multi-line stack above head: name / health / held
				String clearName = stripCodes(t.name);
				if(t.friend) clearName = "[F] " + clearName;
				if(t.fromSoundOnly) clearName = clearName + " ~" + t.confidence;
				
				double baseY = t.fromSoundOnly ? 1.0 : 2.25;
				int line = 0;
				
				// line 0: clear name (+ distance)
				String nameLine = clearName;
				if(config.showDistance)
					nameLine = nameLine + " " + (int)dist + "m";
				labels.add(new Label(pos.add(0, baseY + line * 0.28, 0),
					nameLine, 0xFFFFFF, dist));
				line++;
				
				// line 1: health
				if(config.showHealth && t.health >= 0 && !t.fromSoundOnly)
				{
					float total = t.health + t.absorption;
					String hp = String.format(Locale.ROOT, "%.0f/%.0f HP",
						total, 20.0 + t.absorption);
					int hpColor = total > 14 ? 0x55FF55
						: total > 8 ? 0xFFAA00 : 0xFF5555;
					labels.add(new Label(pos.add(0, baseY + line * 0.28, 0),
						hp, hpColor, dist));
					line++;
				}
				
				// line 2: held item
				if(config.showHeld && !t.held.isEmpty() && !t.fromSoundOnly)
				{
					String item = stripCodes(t.held);
					if(item.length() > 24) item = item.substring(0, 23) + "…";
					labels.add(new Label(pos.add(0, baseY + line * 0.28, 0),
						item, 0xFFD700, dist));
					line++;
				}
				
				// optional armor / speed under item
				if(config.showArmor && t.armorPieces > 0 && !t.fromSoundOnly)
				{
					labels.add(new Label(pos.add(0, baseY + line * 0.28, 0),
						t.armorPieces + " armor", 0xAAAAAA, dist));
					line++;
				}
				if(config.showSpeed && t.speed > 0.1)
				{
					labels.add(new Label(pos.add(0, baseY + line * 0.28, 0),
						String.format(Locale.ROOT, "%.1f b/s", t.speed),
						0xAAAAAA, dist));
				}
			}
		}
		
		if(config.boxes && !boxes.isEmpty())
		{
			RenderUtils.drawOutlinedBoxes(matrices, boxes, boxColors, depthTest);
			if(config.fill)
				RenderUtils.drawSolidBoxes(matrices, boxes, boxColors, depthTest);
		}
		if(config.tracers && !tracers.isEmpty())
			RenderUtils.drawTracers(matrices, partialTicks, tracers, tracerColors, depthTest);
		
		if(config.labels && !labels.isEmpty())
		{
			labels.sort(Comparator.comparingDouble(l -> l.dist));
			int max = Math.min(labels.size(), config.maxLabels);
			ArrayList<Vec3> pos = new ArrayList<>();
			ArrayList<String> text = new ArrayList<>();
			ArrayList<Integer> col = new ArrayList<>();
			for(int i = 0; i < max; i++)
			{
				Label l = labels.get(i);
				pos.add(l.pos); text.add(l.text); col.add(l.color);
			}
			RenderUtils.drawWorldLabels(matrices, pos, text, col);
		}
	}
	
	public void drawHud(GuiGraphics g, Tracker tracker, PlayerEspConfig config)
	{
		Font font = MC.font;
		String line = "Players  " + tracker.count()
			+ "  under:" + tracker.undergroundCount()
			+ "  nearest " + tracker.nearestSummary(config);
		int x = 4, y = 4;
		int w = font.width(line);
		g.fill(x - 2, y - 2, x + w + 2, y + 10, 0x90000000);
		g.drawString(font, line, x, y, 0xFF55FF55, true);
	}
	
	public void drawRadar(GuiGraphics g, Tracker tracker, PlayerEspConfig config,
		int screenW, int screenH)
	{
		if(MC.player == null) return;
		int size = config.radarSize;
		int margin = 8;
		int cx = screenW - size / 2 - margin;
		int cy = size / 2 + margin;
		int r = size / 2 - 2;
		
		g.fill(cx - r, cy - r, cx + r, cy + r, 0x66000000);
		g.fill(cx - r, cy - r, cx + r, cy - r + 1, 0xAAFFFFFF);
		g.fill(cx - r, cy + r - 1, cx + r, cy + r, 0xAAFFFFFF);
		g.fill(cx - r, cy - r, cx - r + 1, cy + r, 0xAAFFFFFF);
		g.fill(cx + r - 1, cy - r, cx + r, cy + r, 0xAAFFFFFF);
		g.fill(cx - 3, cy, cx + 4, cy + 1, 0x88FFFFFF);
		g.fill(cx, cy - 3, cx + 1, cy + 4, 0x88FFFFFF);
		
		float yaw = MC.player.getYRot();
		double scale = (r - 4.0) / Math.max(16, config.maxDistance);
		Vec3 origin = MC.player.position();
		
		for(Tracked t : tracker.all().values())
		{
			if(t.pos == null) continue;
			if(config.undergroundOnly && !t.underground) continue;
			double dx = t.pos.x - origin.x;
			double dz = t.pos.z - origin.z;
			double dist = Math.sqrt(dx * dx + dz * dz);
			if(dist > config.maxDistance) continue;
			
			double rad = Math.toRadians(-yaw);
			double rx = dx * Math.cos(rad) - dz * Math.sin(rad);
			double rz = dx * Math.sin(rad) + dz * Math.cos(rad);
			int px = cx + (int)(rx * scale);
			int pz = cy + (int)(rz * scale);
			int color = pickColor(t, config) | 0xFF000000;
			int dot = t.fromSoundOnly ? 2 : (t.friend ? 4 : 3);
			g.fill(px - dot / 2, pz - dot / 2, px + (dot + 1) / 2,
				pz + (dot + 1) / 2, color);
		}
	}
	
	private static int pickColor(Tracked t, PlayerEspConfig config)
	{
		if(t.friend) return config.friendColor;
		if(t.fromSoundOnly) return config.soundColor;
		if(t.underground) return config.undergroundColor;
		return config.surfaceColor;
	}
	
	private static int scaleRgb(int rgb, float factor)
	{
		int r = Math.min(255, (int)(((rgb >> 16) & 0xFF) * factor));
		int g = Math.min(255, (int)(((rgb >> 8) & 0xFF) * factor));
		int b = Math.min(255, (int)((rgb & 0xFF) * factor));
		return (r << 16) | (g << 8) | b;
	}
	
	private static String trunc(String s, int max)
	{
		if(s == null) return "";
		return s.length() <= max ? s : s.substring(0, max - 1) + "…";
	}
	
	private static String stripCodes(String s)
	{
		if(s == null) return "";
		StringBuilder out = new StringBuilder(s.length());
		for(int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if(c == '§' || c == '&')
			{
				if(i + 1 < s.length()) i++;
				continue;
			}
			if(c < 32) continue;
			out.append(c);
		}
		return out.toString().trim();
	}
	
	private record Label(Vec3 pos, String text, int color, double dist) {}
}

