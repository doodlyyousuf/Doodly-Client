/*
 * Groups already-loaded storage targets into bases.
 * Client data only. No chunk requests, no extra packets.
 */
package net.wimods.chestesp;

import java.util.ArrayList;
import java.util.Comparator;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.wimods.chestesp.util.RenderUtils;

public final class BaseCluster
{
	private static final Minecraft MC = Minecraft.getInstance();
	
	public static final class Cluster
	{
		public double x, y, z;
		public double minX, minY, minZ, maxX, maxY, maxZ;
		public int count;
		public int shulkers;
		public int chests;
		public int barrels;
		
		public Vec3 center()
		{
			return new Vec3(x, y, z);
		}
		
		public AABB box()
		{
			return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
		}
	}
	
	private final ArrayList<Cluster> clusters = new ArrayList<>();
	
	public void rebuild(ChestEspGroupManager groups, ChestEspConfig config)
	{
		clusters.clear();
		if(!config.base_finder_enable)
			return;
		
		ArrayList<Point> points = new ArrayList<>();
		for(ChestEspGroup group : groups.allGroups)
		{
			if(!group.isEnabled())
				continue;
			String name = group.getName();
			boolean storage = name.contains("chest") || name.contains("barrel")
				|| name.contains("shulker") || name.equals("vault");
			if(!storage)
				continue;
			for(ChestEspTarget t : group.getTargets())
			{
				if(!t.passes(config))
					continue;
				Vec3 c = t.center();
				points.add(new Point(c.x, c.y, c.z, name));
			}
		}
		
		double r = config.base_finder_radius;
		double r2 = r * r;
		boolean[] used = new boolean[points.size()];
		for(int i = 0; i < points.size(); i++)
		{
			if(used[i])
				continue;
			Cluster cl = new Cluster();
			cl.minX = cl.maxX = points.get(i).x;
			cl.minY = cl.maxY = points.get(i).y;
			cl.minZ = cl.maxZ = points.get(i).z;
			boolean grew = true;
			while(grew)
			{
				grew = false;
				for(int j = 0; j < points.size(); j++)
				{
					if(used[j])
						continue;
					Point p = points.get(j);
					double cx = (cl.minX + cl.maxX) * 0.5;
					double cy = (cl.minY + cl.maxY) * 0.5;
					double cz = (cl.minZ + cl.maxZ) * 0.5;
					double dx = p.x - cx;
					double dy = p.y - cy;
					double dz = p.z - cz;
					if(dx * dx + dy * dy + dz * dz > r2)
						continue;
					used[j] = true;
					cl.count++;
					if(p.kind.contains("shulker"))
						cl.shulkers++;
					else if(p.kind.contains("barrel"))
						cl.barrels++;
					else
						cl.chests++;
					cl.minX = Math.min(cl.minX, p.x - 0.5);
					cl.minY = Math.min(cl.minY, p.y - 0.5);
					cl.minZ = Math.min(cl.minZ, p.z - 0.5);
					cl.maxX = Math.max(cl.maxX, p.x + 0.5);
					cl.maxY = Math.max(cl.maxY, p.y + 0.5);
					cl.maxZ = Math.max(cl.maxZ, p.z + 0.5);
					grew = true;
				}
			}
			if(cl.count >= config.base_finder_min_containers)
			{
				cl.x = (cl.minX + cl.maxX) * 0.5;
				cl.y = (cl.minY + cl.maxY) * 0.5;
				cl.z = (cl.minZ + cl.maxZ) * 0.5;
				clusters.add(cl);
			}
		}
		clusters.sort(Comparator.comparingInt((Cluster c) -> c.count).reversed());
		if(clusters.size() > config.base_finder_max_bases)
			clusters.subList(config.base_finder_max_bases, clusters.size()).clear();
	}
	
	public void render(PoseStack matrices, float partialTicks, ChestEspConfig config)
	{
		if(!config.base_finder_enable || clusters.isEmpty() || MC.player == null)
			return;
		
		Vec3 cam = RenderUtils.getCameraPos();
		boolean depthTest = !config.base_finder_through_walls;
		ArrayList<AABB> boxes = new ArrayList<>();
		ArrayList<Integer> colors = new ArrayList<>();
		ArrayList<Vec3> ends = new ArrayList<>();
		ArrayList<Integer> tracerColors = new ArrayList<>();
		ArrayList<Vec3> labelPos = new ArrayList<>();
		ArrayList<String> labelText = new ArrayList<>();
		ArrayList<Integer> labelColors = new ArrayList<>();
		
		int shown = 0;
		for(Cluster cl : clusters)
		{
			double dist = cam.distanceTo(cl.center());
			if(dist > config.base_finder_max_distance)
				continue;
			int rgb = cl.shulkers > 0 ? config.base_finder_shulker_color
				: config.base_finder_color;
			int color = (180 << 24) | (rgb & 0xFFFFFF);
			boxes.add(cl.box().inflate(0.3));
			colors.add(color);
			if(config.base_finder_tracers && shown < 6)
			{
				ends.add(cl.center());
				tracerColors.add(color);
			}
			if(shown < config.base_finder_max_labels)
			{
				labelPos.add(cl.center().add(0, (cl.maxY - cl.minY) * 0.5 + 0.6, 0));
				labelText.add("BASE " + cl.count
					+ (cl.shulkers > 0 ? " S" + cl.shulkers : "")
					+ " " + (int)dist + "m");
				labelColors.add(color);
			}
			shown++;
		}
		
		if(!boxes.isEmpty())
			RenderUtils.drawOutlinedBoxes(matrices, boxes, colors, depthTest);
		if(!ends.isEmpty())
			RenderUtils.drawTracers(matrices, partialTicks, ends, tracerColors, depthTest);
		if(!labelPos.isEmpty())
			RenderUtils.drawWorldLabels(matrices, labelPos, labelText, labelColors);
	}
	
	public String hudLine()
	{
		if(clusters.isEmpty())
			return "Bases  0";
		Cluster top = clusters.get(0);
		Vec3 cam = RenderUtils.getCameraPos();
		int dist = (int)cam.distanceTo(top.center());
		return "Bases  " + clusters.size() + "  biggest " + top.count
			+ " @ " + (int)top.x + " " + (int)top.y + " " + (int)top.z
			+ " (" + dist + "m)";
	}
	
	public int count()
	{
		return clusters.size();
	}
	
	public void clear()
	{
		clusters.clear();
	}
	
	private record Point(double x, double y, double z, String kind)
	{}
}
