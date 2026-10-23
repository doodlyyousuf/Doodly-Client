package net.doodly.basefinder;

import java.util.ArrayList;

import com.mojang.blaze3d.vertex.PoseStack;

import net.doodly.basefinder.Scanner.Cluster;
import net.doodly.basefinder.Scanner.Hit;
import net.doodly.basefinder.util.RenderUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class Renderer
{
	private static final Minecraft MC = Minecraft.getInstance();
	
	public void drawWorld(PoseStack matrices, float pt, Scanner scanner,
		BaseFinderConfig cfg)
	{
		if(MC.player == null) return;
		boolean depth = !cfg.throughWalls;
		Vec3 cam = RenderUtils.getCameraPos();
		
		ArrayList<AABB> boxes = new ArrayList<>();
		ArrayList<Integer> cols = new ArrayList<>();
		ArrayList<Vec3> tracers = new ArrayList<>();
		ArrayList<Integer> tcols = new ArrayList<>();
		ArrayList<Vec3> labs = new ArrayList<>();
		ArrayList<String> texts = new ArrayList<>();
		ArrayList<Integer> lcols = new ArrayList<>();
		
		for(Cluster cl : scanner.clusters())
		{
			Vec3 c = new Vec3(cl.x, cl.y, cl.z);
			if(cam.distanceTo(c) > cfg.maxDistance) continue;
			double s = 0.6 + Math.min(2.0, cl.count * 0.15);
			boxes.add(new AABB(c.x - s, c.y - 0.5, c.z - s, c.x + s, c.y + 1.5, c.z + s));
			cols.add(cfg.clusterColor | 0xFF000000);
			if(cfg.tracers)
			{
				tracers.add(c);
				tcols.add(cfg.clusterColor | 0xFF000000);
			}
			labs.add(c.add(0, 2.0, 0));
			texts.add("BASE x" + cl.count + (cl.shulkers > 0 ? " S" + cl.shulkers : ""));
			lcols.add(cfg.clusterColor);
		}
		
		// light hits (cap draw count)
		int drawn = 0;
		for(Hit h : scanner.lights())
		{
			if(drawn++ > 200) break;
			if(cam.distanceTo(h.pos) > cfg.maxDistance) continue;
			boxes.add(new AABB(h.pos.x - 0.2, h.pos.y - 0.2, h.pos.z - 0.2,
				h.pos.x + 0.2, h.pos.y + 0.2, h.pos.z + 0.2));
			cols.add(h.color | 0xFF000000);
		}
		
		for(Hit h : scanner.soundHits())
		{
			if(cam.distanceTo(h.pos) > cfg.maxDistance) continue;
			boxes.add(new AABB(h.pos.x - 0.35, h.pos.y, h.pos.z - 0.35,
				h.pos.x + 0.35, h.pos.y + 0.7, h.pos.z + 0.35));
			cols.add(h.color | 0xFF000000);
			labs.add(h.pos.add(0, 1.0, 0));
			texts.add("~");
			lcols.add(h.color);
		}
		
		if(!boxes.isEmpty())
			RenderUtils.drawOutlinedBoxes(matrices, boxes, cols, depth);
		if(cfg.tracers && !tracers.isEmpty())
			RenderUtils.drawTracers(matrices, pt, tracers, tcols, depth);
		if(!labs.isEmpty())
			RenderUtils.drawWorldLabels(matrices, labs, texts, lcols);
	}
	
	public void drawHud(GuiGraphics g, Scanner scanner, BaseFinderConfig cfg)
	{
		Font font = MC.font;
		String line = scanner.hudLine();
		int x = 4, y = 16;
		int w = font.width(line);
		g.fill(x - 2, y - 2, x + w + 2, y + 10, 0x90000000);
		g.drawString(font, line, x, y, 0xFF55AAFF, true);
	}
}
