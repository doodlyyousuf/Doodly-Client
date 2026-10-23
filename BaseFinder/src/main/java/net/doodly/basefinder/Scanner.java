package net.doodly.basefinder;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

/**
 * Loaded-chunk only. No C2S. No portal markers.
 * Light: block-light > sky (artificial) underground → base cue.
 * Storage: block-entities already in client memory.
 * Sound: dig/step markers from inbound packets.
 */
public final class Scanner
{
	private static final Minecraft MC = Minecraft.getInstance();
	
	public static final class Hit
	{
		public final Vec3 pos;
		public final String label;
		public final int color;
		public final long time;
		
		public Hit(Vec3 pos, String label, int color)
		{
			this.pos = pos;
			this.label = label;
			this.color = color;
			this.time = System.currentTimeMillis();
		}
	}
	
	public static final class Cluster
	{
		public double x, y, z;
		public int count;
		public int shulkers;
		public int chests;
	}
	
	private final ArrayList<Hit> lights = new ArrayList<>();
	private final ArrayList<Cluster> clusters = new ArrayList<>();
	private final Map<Long, Hit> sounds = new ConcurrentHashMap<>();
	
	public synchronized void scan(BaseFinderConfig cfg)
	{
		if(MC.level == null || MC.player == null) return;
		
		lights.clear();
		clusters.clear();
		
		Vec3 origin = MC.player.position();
		int range = cfg.lightScanRange;
		int step = Math.max(2, cfg.lightStep);
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
		
		ArrayList<Vec3> storagePts = new ArrayList<>();
		ArrayList<String> storageKinds = new ArrayList<>();
		
		int minCx = ((int)origin.x - range) >> 4;
		int maxCx = ((int)origin.x + range) >> 4;
		int minCz = ((int)origin.z - range) >> 4;
		int maxCz = ((int)origin.z + range) >> 4;
		
		for(int cx = minCx; cx <= maxCx; cx++)
		{
			for(int cz = minCz; cz <= maxCz; cz++)
			{
				if(!MC.level.hasChunk(cx, cz)) continue;
				LevelChunk chunk = MC.level.getChunk(cx, cz);
				
				// block entities = storage already known to client
				if(cfg.storageClusters)
				{
					for(BlockEntity be : chunk.getBlockEntities().values())
					{
						BlockPos bp = be.getBlockPos();
						double d = origin.distanceTo(Vec3.atCenterOf(bp));
						if(d > cfg.maxDistance) continue;
						String kind = null;
						if(be instanceof ShulkerBoxBlockEntity) kind = "shulker";
						else if(be instanceof ChestBlockEntity) kind = "chest";
						else if(be instanceof BarrelBlockEntity) kind = "barrel";
						if(kind == null) continue;
						storagePts.add(Vec3.atCenterOf(bp));
						storageKinds.add(kind);
					}
				}
				
				// light step-scan
				if(cfg.lightScan)
				{
					int x0 = cx << 4;
					int z0 = cz << 4;
					for(int x = x0; x < x0 + 16; x += step)
					{
						for(int z = z0; z < z0 + 16; z += step)
						{
							int yMax = cfg.lightUndergroundOnly ? cfg.lightMaxY : 128;
							int yMin = MC.level.getMinY();
							for(int y = yMin; y <= yMax; y += step)
							{
								m.set(x, y, z);
								if(!MC.level.isLoaded(m)) continue;
								double d = origin.distanceTo(Vec3.atCenterOf(m));
								if(d > cfg.maxDistance) continue;
								
								int blockL = MC.level.getBrightness(LightLayer.BLOCK, m);
								if(blockL < cfg.minBlockLight) continue;
								if(cfg.lightOnlyIfBrighterThanSky)
								{
									int sky = MC.level.getBrightness(LightLayer.SKY, m);
									if(blockL <= sky) continue;
								}
								
								BlockState st = MC.level.getBlockState(m);
								if(st.isAir()) continue;
								// skip pure sky-open surface noise
								if(cfg.lightUndergroundOnly && y > cfg.lightMaxY)
									continue;
								
								lights.add(new Hit(Vec3.atCenterOf(m),
									"L" + blockL, cfg.lightColor));
							}
						}
					}
				}
			}
		}
		
		// cluster storage points
		if(cfg.storageClusters && !storagePts.isEmpty())
		{
			double r = cfg.clusterRadius;
			double r2 = r * r;
			boolean[] used = new boolean[storagePts.size()];
			for(int i = 0; i < storagePts.size(); i++)
			{
				if(used[i]) continue;
				Cluster cl = new Cluster();
				Vec3 p0 = storagePts.get(i);
				cl.x = p0.x; cl.y = p0.y; cl.z = p0.z;
				cl.count = 0;
				boolean grew = true;
				while(grew)
				{
					grew = false;
					for(int j = 0; j < storagePts.size(); j++)
					{
						if(used[j]) continue;
						Vec3 p = storagePts.get(j);
						double dx = p.x - cl.x, dy = p.y - cl.y, dz = p.z - cl.z;
						if(dx * dx + dy * dy + dz * dz > r2) continue;
						used[j] = true;
						cl.count++;
						if("shulker".equals(storageKinds.get(j))) cl.shulkers++;
						if("chest".equals(storageKinds.get(j))) cl.chests++;
						// re-center
						cl.x = (cl.x * (cl.count - 1) + p.x) / cl.count;
						cl.y = (cl.y * (cl.count - 1) + p.y) / cl.count;
						cl.z = (cl.z * (cl.count - 1) + p.z) / cl.count;
						grew = true;
					}
				}
				if(cl.count >= cfg.minContainers)
					clusters.add(cl);
			}
		}
		
		// prune old sounds
		long now = System.currentTimeMillis();
		sounds.entrySet().removeIf(e -> now - e.getValue().time > 12000);
	}
	
	public void onSound(double x, double y, double z, String id, BaseFinderConfig cfg)
	{
		if(!cfg.soundMarkers || id == null) return;
		String s = id.toLowerCase(Locale.ROOT);
		if(!(s.contains("dig") || s.contains("step") || s.contains("chest")
			|| s.contains("shulker") || s.contains("barrel") || s.contains("piston")
			|| s.contains("fire") || s.contains("furnace") || s.contains("anvil")))
			return;
		if(y > 80 && !s.contains("chest")) return;
		long key = (Math.round(x) << 32) ^ (Math.round(y) << 16) ^ Math.round(z);
		sounds.put(key, new Hit(new Vec3(x, y, z), "snd", cfg.soundColor));
	}
	
	public synchronized ArrayList<Hit> lights() { return new ArrayList<>(lights); }
	public synchronized ArrayList<Cluster> clusters() { return new ArrayList<>(clusters); }
	public ArrayList<Hit> soundHits() { return new ArrayList<>(sounds.values()); }
	
	public synchronized void clear()
	{
		lights.clear();
		clusters.clear();
		sounds.clear();
	}
	
	public String hudLine()
	{
		return "Base  L:" + lights.size()
			+ "  C:" + clusters.size()
			+ "  S:" + sounds.size();
	}
}
