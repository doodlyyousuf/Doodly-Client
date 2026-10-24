package net.doodly.playeresp;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Entity + sound side-channel tracker.
 * Never sends packets. Never requests chunks.
 * Designed for servers that strip underground sections (Donut-style).
 */
public final class Tracker
{
	private static final Minecraft MC = Minecraft.getInstance();
	
	public static final class Tracked
	{
		public final UUID id;
		public String name = "?";
		public Vec3 pos = Vec3.ZERO;
		public Vec3 lastPos = Vec3.ZERO;
		public Vec3 velocity = Vec3.ZERO;
		public long lastUpdate;
		public long lastSound;
		public boolean underground;
		public boolean fromSoundOnly;
		public float health = -1f;
		public float absorption;
		public String held = "";
		public int armorPieces;
		public double speed;
		public int confidence;
		public boolean friend;
		
		public Tracked(UUID id) { this.id = id; }
		
		public Vec3 interpolated(float pt)
		{
			if(lastPos == null || pos == null)
				return pos == null ? Vec3.ZERO : pos;
			return lastPos.lerp(pos, Math.min(1f, pt * 2f));
		}
		
		public Vec3 predicted(float pt, float seconds)
		{
			Vec3 base = interpolated(pt);
			if(velocity == null || velocity.lengthSqr() < 1e-6)
				return base;
			return base.add(velocity.scale(seconds));
		}
	}
	
	private final Map<UUID, Tracked> tracked = new ConcurrentHashMap<>();
	
	/**
	 * Sound side-channel — STRICT.
	 * Cows/pigs use the same block.*.step as players. Those must never become "~player".
	 * Only minecraft:entity.player.* may create sound phantoms.
	 * Block dig/place only refreshes an already-known real player track.
	 */
	public void onPositionalSound(double x, double y, double z, String soundId)
	{
		if(soundId == null) return;
		// ignore local player — own steps/digs must never become markers
		if(MC.player != null)
		{
			double dx = x - MC.player.getX();
			double dy = y - MC.player.getY();
			double dz = z - MC.player.getZ();
			if(dx * dx + dy * dy + dz * dz < 12.0) // ~3.5 blocks
				return;
		}
		String s = soundId.toLowerCase(Locale.ROOT);
		
		// anything that is a non-player entity sound → out
		if(s.contains("entity.") && !s.contains("entity.player"))
			return;
		if(s.contains("ambient.") || s.contains("weather.") || s.contains("ui.")
			|| s.contains("music.") || s.contains("particle."))
			return;
		
		boolean playerSound = s.contains("entity.player");
		// block step/hit is shared with mobs — NEVER create a phantom from it
		boolean blockStep = s.contains(".step") || s.contains("block.grass")
			|| s.contains("block.stone.step") || s.contains("block.sand.step")
			|| s.contains("block.wood.step") || s.contains("block.snow.step");
		if(blockStep && !playerSound)
			return;
		
		boolean containerOpen = s.contains("chest") || s.contains("barrel")
			|| s.contains("shulker") || s.contains("ender_chest");
		boolean digPlace = playerSound && (s.contains("break") || s.contains("place")
			|| s.contains("attack") || s.contains("hurt") || s.contains("fall")
			|| s.contains("swim") || s.contains("splash") || s.contains("step")
			|| s.contains("burp") || s.contains("eat") || s.contains("teleport"));
		
		if(!playerSound && !containerOpen)
			return;
		
		long now = System.currentTimeMillis();
		Vec3 hit = new Vec3(x, y, z);
		
		// reinforce nearest REAL player only
		Tracked best = null;
		double bestD = 6.0;
		for(Tracked tr : tracked.values())
		{
			if(tr.pos == null || tr.fromSoundOnly) continue;
			double d = tr.pos.distanceTo(hit);
			if(d < bestD) { bestD = d; best = tr; }
		}
		if(best != null)
		{
			best.lastSound = now;
			best.confidence = Math.min(100, best.confidence + 5);
			return;
		}
		
		// phantom: entity.player.* only, and prefer underground (filter noise)
		if(!playerSound)
			return; // container opens without a known player → ignore for player ESP
		if(y >= 72)
			return;
		
		long key = (Math.round(x * 2) << 32) ^ (Math.round(y * 2) << 16) ^ Math.round(z * 2);
		UUID phantom = new UUID(0x534F554E44000000L, key);
		Tracked tr = tracked.computeIfAbsent(phantom, Tracked::new);
		tr.name = "~player";
		tr.lastPos = tr.pos == null ? hit : tr.pos;
		tr.pos = hit;
		tr.lastUpdate = now;
		tr.lastSound = now;
		tr.underground = y < 60;
		tr.fromSoundOnly = true;
		tr.confidence = digPlace ? 45 : 30;
	}
	
	public void tick(PlayerEspConfig config)
	{
		if(MC.level == null || MC.player == null) return;
		
		long now = System.currentTimeMillis();
		long staleMs = config.staleSeconds * 1000L;
		Set<String> hide = parse(config.hideNames);
		Set<String> focus = parse(config.focusNames);
		Set<String> friends = parse(config.friendNames);
		
		for(Player p : MC.level.players())
		{
			if(p == MC.player) continue;
			if(config.ignoreInvisible && p.isInvisible()) continue;
			
			String name = p.getName().getString();
			if(name == null || name.isBlank())
				name = p.getScoreboardName();
			String lower = name.toLowerCase(Locale.ROOT);
			if(hide.contains(lower)) continue;
			if(!focus.isEmpty() && !focus.contains(lower)) continue;
			
			UUID id = p.getUUID();
			Tracked t = tracked.computeIfAbsent(id, Tracked::new);
			t.name = name;
			t.lastPos = t.pos;
			Vec3 newPos = p.position();
			if(t.pos != null && t.lastUpdate > 0)
			{
				double dt = (now - t.lastUpdate) / 1000.0;
				if(dt > 0.01 && dt < 2.0)
					t.velocity = newPos.subtract(t.pos).scale(1.0 / dt);
			}
			t.pos = newPos;
			t.lastUpdate = now;
			t.fromSoundOnly = false;
			t.confidence = 100;
			t.underground = t.pos.y < config.undergroundY;
			t.friend = friends.contains(lower);
			t.health = p.getHealth();
			t.absorption = p.getAbsorptionAmount();
			t.speed = Math.sqrt(
				p.getDeltaMovement().x * p.getDeltaMovement().x
					+ p.getDeltaMovement().z * p.getDeltaMovement().z) * 20.0;
			
			ItemStack main = p.getMainHandItem();
			if(main.isEmpty())
				t.held = "";
			else
			{
				String hover = main.getHoverName().getString();
				String path = main.getItem().toString(); // e.g. minecraft:netherite_sword
				int colon = path.indexOf(':');
				String plain = colon >= 0 ? path.substring(colon + 1).replace('_', ' ') : path;
				// prefer custom name if different from plain translation-ish
				t.held = hover.isBlank() ? plain : hover;
			}
			int armor = 0;
			for(EquipmentSlot slot : new EquipmentSlot[] {
				EquipmentSlot.HEAD, EquipmentSlot.CHEST,
				EquipmentSlot.LEGS, EquipmentSlot.FEET })
				if(!p.getItemBySlot(slot).isEmpty()) armor++;
			t.armorPieces = armor;
		}
		
		Vec3 cam = MC.player.position();
		double maxD = config.maxDistance;
		tracked.entrySet().removeIf(e -> {
			Tracked t = e.getValue();
			if(now - t.lastUpdate > staleMs) return true;
			if(t.pos != null && cam.distanceTo(t.pos) > maxD * 1.5) return true;
			if(t.fromSoundOnly && now - t.lastSound > 8000) return true;
			return false;
		});
	}
	
	public Map<UUID, Tracked> all() { return tracked; }
	
	public int count() { return tracked.size(); }
	
	public int undergroundCount()
	{
		int n = 0;
		for(Tracked t : tracked.values())
			if(t.underground && !t.fromSoundOnly) n++;
		return n;
	}
	
	public String nearestSummary(PlayerEspConfig config)
	{
		if(MC.player == null) return "-";
		Vec3 cam = MC.player.position();
		Tracked best = null;
		double bestD = Double.MAX_VALUE;
		for(Tracked t : tracked.values())
		{
			if(t.pos == null || t.fromSoundOnly) continue;
			double d = cam.distanceTo(t.pos);
			if(d < bestD) { bestD = d; best = t; }
		}
		if(best == null) return "-";
		return best.name + " " + (int)bestD + "m"
			+ (best.underground ? " [U]" : "")
			+ (best.friend ? " [F]" : "");
	}
	
	public void clear() { tracked.clear(); }
	
	private static Set<String> parse(String raw)
	{
		HashSet<String> set = new HashSet<>();
		if(raw == null || raw.isBlank()) return set;
		for(String part : raw.split(","))
		{
			String s = part.trim().toLowerCase(Locale.ROOT);
			if(!s.isEmpty()) set.add(s);
		}
		return set;
	}
}
