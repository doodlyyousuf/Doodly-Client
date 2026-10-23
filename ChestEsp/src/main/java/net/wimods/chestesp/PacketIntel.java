/*
 * PacketIntel — inbound packet observer + plain-language HUD.
 * Client-side only. Never sends packets.
 */
package net.wimods.chestesp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundEntityPositionPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.world.entity.EntityType;

public final class PacketIntel
{
	private static final Minecraft MC = Minecraft.getInstance();
	private static final int MAX_LOG = 64;
	
	private final List<String> log =
		Collections.synchronizedList(new ArrayList<>());
	private final Map<Long, ChunkNote> chunks = new ConcurrentHashMap<>();
	private final AtomicInteger entitySpawns = new AtomicInteger();
	private final AtomicInteger playerSpawns = new AtomicInteger();
	private final AtomicInteger deepSounds = new AtomicInteger();
	private final AtomicInteger blockUpdates = new AtomicInteger();
	private final AtomicInteger removes = new AtomicInteger();
	private long windowStart = System.currentTimeMillis();
	private PlayerEsp playerEspHook; // optional cross-link for sound side-channel
	
	public static final class ChunkNote
	{
		public final int x, z;
		public long lastSeen;
		public int hits;
		
		public ChunkNote(int x, int z)
		{
			this.x = x;
			this.z = z;
			this.lastSeen = System.currentTimeMillis();
			this.hits = 1;
		}
	}
	
	public void setPlayerEspHook(PlayerEsp esp)
	{
		this.playerEspHook = esp;
	}
	
	public void onPacket(Packet<?> packet)
	{
		if(MC.level == null)
			return;
		
		try
		{
			if(packet instanceof ClientboundLevelChunkWithLightPacket chunk)
			{
				long key = chunkKey(chunk.getX(), chunk.getZ());
				ChunkNote note = chunks.get(key);
				if(note == null)
				{
					note = new ChunkNote(chunk.getX(), chunk.getZ());
					chunks.put(key, note);
					add("CHUNK " + chunk.getX() + " " + chunk.getZ() + " first");
				}
				else
				{
					note.hits++;
					note.lastSeen = System.currentTimeMillis();
				}
			}
			else if(packet instanceof ClientboundAddEntityPacket spawn)
			{
				entitySpawns.incrementAndGet();
				EntityType<?> type = spawn.getType();
				String path = EntityType.getKey(type).getPath();
				boolean isPlayer = type == EntityType.PLAYER || path.contains("player");
				if(isPlayer)
				{
					playerSpawns.incrementAndGet();
					add(String.format("PLAYER SPAWN @ %.0f %.0f %.0f",
						spawn.getX(), spawn.getY(), spawn.getZ()));
				}
				else if(isInteresting(path))
				{
					add(String.format("ENT %s @ %.0f %.0f %.0f", path,
						spawn.getX(), spawn.getY(), spawn.getZ()));
				}
			}
			else if(packet instanceof ClientboundRemoveEntitiesPacket)
			{
				removes.incrementAndGet();
			}
			else if(packet instanceof ClientboundEntityPositionPacket
				|| packet instanceof ClientboundMoveEntityPacket)
			{
				// high frequency — counted only in summary window
			}
			else if(packet instanceof ClientboundSoundPacket sound)
			{
				double y = sound.getY();
				String sid = sound.getSound().value().location().toString();
				if(y < 64)
				{
					deepSounds.incrementAndGet();
					add(String.format("SOUND %s @ %.0f %.0f %.0f",
						shortSound(sid), sound.getX(), y, sound.getZ()));
				}
				if(playerEspHook != null)
					playerEspHook.onPositionalSound(sound.getX(), sound.getY(),
						sound.getZ(), sid);
			}
			else if(packet instanceof ClientboundBlockUpdatePacket)
			{
				blockUpdates.incrementAndGet();
			}
			else if(packet instanceof ClientboundPlayerInfoUpdatePacket info)
			{
				// tab list changes — useful for join/leave correlation
				add("TAB update entries=" + info.entries().size());
			}
			else if(packet instanceof ClientboundSystemChatPacket chat)
			{
				// ignore spammy system chat unless it looks like staff/anticheat
				String content = chat.content().getString().toLowerCase(Locale.ROOT);
				if(content.contains("flag") || content.contains("vl")
					|| content.contains("suspect") || content.contains("ban")
					|| content.contains("kick"))
					add("CHAT " + truncate(chat.content().getString(), 48));
			}
		}catch(Throwable ignored)
		{
			// never break the client
		}
		
		if(System.currentTimeMillis() - windowStart > 30_000)
			resetWindow();
	}
	
	private boolean isInteresting(String path)
	{
		return path.contains("chest") || path.contains("minecart")
			|| path.contains("boat") || path.contains("shulker")
			|| path.contains("item") || path.contains("experience");
	}
	
	private long chunkKey(int x, int z)
	{
		return ((long)x & 0xFFFFFFFFL) << 32 | ((long)z & 0xFFFFFFFFL);
	}
	
	private String shortSound(String id)
	{
		int slash = id.lastIndexOf('/');
		int colon = id.lastIndexOf(':');
		int cut = Math.max(slash, colon);
		return cut >= 0 ? id.substring(cut + 1) : id;
	}
	
	private String truncate(String s, int max)
	{
		if(s == null)
			return "";
		s = s.replace('\n', ' ');
		return s.length() <= max ? s : s.substring(0, max - 1) + "…";
	}
	
	private void add(String line)
	{
		String timed = String.format("[%tT] %s", System.currentTimeMillis(), line);
		synchronized(log)
		{
			log.add(0, timed);
			while(log.size() > MAX_LOG)
				log.remove(log.size() - 1);
		}
	}
	
	private void resetWindow()
	{
		entitySpawns.set(0);
		playerSpawns.set(0);
		deepSounds.set(0);
		blockUpdates.set(0);
		removes.set(0);
		windowStart = System.currentTimeMillis();
	}
	
	public List<String> snapshot(int max)
	{
		synchronized(log)
		{
			int n = Math.min(max, log.size());
			return new ArrayList<>(log.subList(0, n));
		}
	}
	
	public String summaryLine()
	{
		return "Intel  ck:" + chunks.size()
			+ "  pSpawn:" + playerSpawns.get()
			+ "  eSpawn:" + entitySpawns.get()
			+ "  deepS:" + deepSounds.get()
			+ "  blk:" + blockUpdates.get()
			+ "  rm:" + removes.get();
	}
	
	/** Dump current log to .minecraft/packetintel.log */
	public void dumpToFile()
	{
		try
		{
			Path dir = MC.gameDirectory.toPath();
			Path out = dir.resolve("packetintel.log");
			List<String> lines;
			synchronized(log)
			{
				lines = new ArrayList<>(log);
			}
			Collections.reverse(lines);
			Files.write(out, lines, StandardCharsets.UTF_8,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			add("DUMP wrote " + lines.size() + " lines → packetintel.log");
		}catch(IOException e)
		{
			add("DUMP failed: " + e.getMessage());
		}
	}
	
	public void clear()
	{
		log.clear();
		chunks.clear();
		resetWindow();
	}
}
