package net.doodly.basefinder;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

/**
 * Loaded-chunk only. No C2S. Patterns adapted from open Donut-style finders:
 * scan block entities already on the client (chests, shulkers, spawners).
 * Light: artificial block-light underground.
 * Sound: entity.player.* only, never self, never mobs.
 */
public final class Scanner
{
	private static final Minecraft MC = Minecraft.getInstance();
	private static final int MAX_LIGHT = 250;
	
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
		public int spawners;
	}
	
	public static final class Target
	{
		public final BlockPos pos;
		public final String kind; // chest, shulker, spawner, ...
		public final int color;
		
		public Target(BlockPos pos, String kind, int color)
		{
			this.pos = pos;
			this.kind = kind;
			this.color = color;
		}
	}
	
	private final ArrayList<Hit> lights = new ArrayList<>();
	private final ArrayList<Cluster> clusters = new ArrayList<>();
	private final ArrayList<Cluster> stashes = new ArrayList<>();
	private final ArrayList<Target> targets = new ArrayList<>();
	private final Map<Long, Hit> sounds = new ConcurrentHashMap<>();
	
	public synchronized void scan(BaseFinderConfig cfg)
	{
		if(MC.level == null || MC.player == null) return;
		
		lights.clear();
		clusters.clear();
		targets.clear();
		
		Vec3 origin = MC.player.position();
		int range = cfg.lightScanRange;
		int step = Math.max(2, cfg.lightStep);
		int chunkR = Math.max(1, (int)Math.ceil(Math.max(range, cfg.maxDistance) / 16.0));
		int centerX = MC.player.chunkPosition().x;
		int centerZ = MC.player.chunkPosition().z;
		
		ArrayList<Vec3> storagePts = new ArrayList<>();
		ArrayList<String> storageKinds = new ArrayList<>();
		
		for(int cx = centerX - chunkR; cx <= centerX + chunkR; cx++)
		{
			for(int cz = centerZ - chunkR; cz <= centerZ + chunkR; cz++)
			{
				var chunk = MC.level.getChunkSource().getChunk(cx, cz, ChunkStatus.FULL, false);
				if(chunk == null) continue;
				
				// --- block entities (same idea as Raindots finder) ---
				if(cfg.storageClusters || cfg.spawners)
				{
					for(BlockEntity be : chunk.getBlockEntities().values())
					{
						if(be.isRemoved()) continue;
						BlockPos bp = be.getBlockPos();
						double dsq = bp.distToCenterSqr(origin);
						if(dsq > (double)cfg.maxDistance * cfg.maxDistance) continue;
						
						BlockState st = be.getBlockState();
						String kind = classify(st, cfg);
						if(kind == null) continue;
						
						int color = colorFor(kind, cfg);
						targets.add(new Target(bp.immutable(), kind, color));
						
						if(kind.equals("spawner"))
						{
							if(!cfg.spawners) continue;
							// each spawner is its own "cluster" seed
							storagePts.add(Vec3.atCenterOf(bp));
							storageKinds.add("spawner");
						}
						else if(cfg.storageClusters)
						{
							storagePts.add(Vec3.atCenterOf(bp));
							storageKinds.add(kind);
						}
					}
				}
				
				// --- artificial light ---
				if(cfg.lightScan && chunk instanceof LevelChunk)
				{
					int x0 = cx << 4;
					int z0 = cz << 4;
					BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
					int lightDrawn = 0;
					for(int x = x0; x < x0 + 16 && lightDrawn < MAX_LIGHT; x += step)
					{
						for(int z = z0; z < z0 + 16 && lightDrawn < MAX_LIGHT; z += step)
						{
							int yMax = cfg.lightUndergroundOnly ? cfg.lightMaxY : 128;
							int yMin = MC.level.getMinY();
							for(int y = yMin; y <= yMax && lightDrawn < MAX_LIGHT; y += step)
							{
								m.set(x, y, z);
								if(!MC.level.isLoaded(m)) continue;
								if(origin.distanceToSqr(x + 0.5, y + 0.5, z + 0.5)
									> (double)cfg.maxDistance * cfg.maxDistance)
									continue;
								
								int blockL = MC.level.getBrightness(LightLayer.BLOCK, m);
								if(blockL < cfg.minBlockLight) continue;
								if(cfg.lightOnlyIfBrighterThanSky)
								{
									int sky = MC.level.getBrightness(LightLayer.SKY, m);
									if(blockL <= sky) continue;
								}
								BlockState st = MC.level.getBlockState(m);
								if(st.isAir()) continue;
								
								lights.add(new Hit(Vec3.atCenterOf(m), "L" + blockL, cfg.lightColor));
								lightDrawn++;
							}
						}
					}
				}
			}
		}
		
		// cluster nearby storage / spawners
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
						String k = storageKinds.get(j);
						if("shulker".equals(k)) cl.shulkers++;
						if("chest".equals(k) || "trapped_chest".equals(k)
							|| "ender_chest".equals(k)) cl.chests++;
						if("spawner".equals(k))
							cl.spawners++;
						cl.x = (cl.x * (cl.count - 1) + p.x) / cl.count;
						cl.y = (cl.y * (cl.count - 1) + p.y) / cl.count;
						cl.z = (cl.z * (cl.count - 1) + p.z) / cl.count;
						grew = true;
					}
				}
				// spawners alone count as a base even with minContainers
				boolean keep = cl.count >= cfg.minContainers || cl.spawners > 0;
				if(keep)
				{
					clusters.add(cl);
					if(cfg.stashLog)
						rememberStash(cl, cfg);
				}
			}
		}
		
		long now = System.currentTimeMillis();
		sounds.entrySet().removeIf(e -> now - e.getValue().time > 12000);
	}
	
	private static String classify(BlockState st, BaseFinderConfig cfg)
	{
		if(st.is(Blocks.SPAWNER)) return cfg.spawners ? "spawner" : null;
		// trial spawners excluded — only classic mob spawners
		if(!cfg.storageClusters) return null;
		if(st.is(Blocks.CHEST)) return "chest";
		if(st.is(Blocks.TRAPPED_CHEST)) return "trapped_chest";
		if(st.is(Blocks.ENDER_CHEST)) return "ender_chest";
		if(st.getBlock() instanceof ShulkerBoxBlock) return "shulker";
		if(st.is(Blocks.BARREL)) return "barrel";
		if(st.is(Blocks.DROPPER)) return "dropper";
		if(st.is(Blocks.DISPENSER)) return "dispenser";
		if(st.is(Blocks.HOPPER)) return "hopper";
		return null;
	}
	
	private static int colorFor(String kind, BaseFinderConfig cfg)
	{
		return switch(kind)
		{
			case "spawner" -> cfg.spawnerColor;
			case "shulker" -> 0xFF00FF;
			case "ender_chest" -> 0x00FFFF;
			case "trapped_chest" -> 0xFF8000;
			default -> cfg.clusterColor;
		};
	}
	
	public void onSound(double x, double y, double z, String id, BaseFinderConfig cfg)
	{
		if(!cfg.soundMarkers || id == null) return;
		if(MC.player != null)
		{
			double dx = x - MC.player.getX();
			double dy = y - MC.player.getY();
			double dz = z - MC.player.getZ();
			if(dx * dx + dy * dy + dz * dz < 12.0)
				return;
		}
		String s = id.toLowerCase(Locale.ROOT);
		if(s.contains("entity.") && !s.contains("entity.player"))
			return;
		if(s.contains(".step") && !s.contains("entity.player"))
			return;
		boolean ok = s.contains("entity.player")
			|| s.contains("chest") || s.contains("shulker") || s.contains("barrel")
			|| s.contains("piston") || s.contains("furnace") || s.contains("anvil")
			|| (s.contains("block.") && (s.contains("break") || s.contains("place")));
		if(!ok) return;
		if(y > 80 && !(s.contains("chest") || s.contains("shulker"))) return;
		long key = (Math.round(x * 2) << 32) ^ (Math.round(y * 2) << 16) ^ Math.round(z * 2);
		String label = s.contains("entity.player") ? "~p" : "~blk";
		sounds.put(key, new Hit(new Vec3(x, y, z), label, cfg.soundColor));
	}
	
	public synchronized void rememberStash(Cluster cl, BaseFinderConfig cfg)
	{
		for(Cluster s : stashes)
		{
			double dx = s.x - cl.x, dy = s.y - cl.y, dz = s.z - cl.z;
			if(dx * dx + dy * dy + dz * dz < 64)
			{
				if(cl.count > s.count || cl.spawners > s.spawners)
				{
					s.x = cl.x; s.y = cl.y; s.z = cl.z;
					s.count = cl.count; s.shulkers = cl.shulkers;
					s.chests = cl.chests; s.spawners = cl.spawners;
				}
				return;
			}
		}
		Cluster copy = new Cluster();
		copy.x = cl.x; copy.y = cl.y; copy.z = cl.z;
		copy.count = cl.count; copy.shulkers = cl.shulkers;
		copy.chests = cl.chests; copy.spawners = cl.spawners;
		stashes.add(copy);
		while(stashes.size() > cfg.stashMax)
			stashes.remove(0);
	}
	
	public synchronized ArrayList<Hit> lights() { return new ArrayList<>(lights); }
	public synchronized ArrayList<Cluster> clusters() { return new ArrayList<>(clusters); }
	public synchronized ArrayList<Cluster> stashes() { return new ArrayList<>(stashes); }
	public synchronized ArrayList<Target> targets() { return new ArrayList<>(targets); }
	public ArrayList<Hit> soundHits() { return new ArrayList<>(sounds.values()); }
	
	public synchronized void clear()
	{
		lights.clear();
		clusters.clear();
		targets.clear();
		sounds.clear();
	}
	
	public String hudLine()
	{
		return "Base  T:" + targets.size()
			+ "  C:" + clusters.size()
			+ "  stash:" + stashes.size()
			+ "  L:" + lights.size()
			+ "  S:" + sounds.size();
	}
}
