/*
 * Multi-band HUD: ChestESP / PlayerESP / PacketIntel
 */
package net.wimods.chestesp;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class ChestEspHud
{
	private static final Minecraft MC = Minecraft.getInstance();
	
	private String line = "";
	private String playerLine = "";
	private String intelLine = "";
	private List<String> intelLog = List.of();
	
	public void setLine(String line)
	{
		this.line = line == null ? "" : line;
	}
	
	public void setPlayerLine(String line)
	{
		this.playerLine = line == null ? "" : line;
	}
	
	public void setIntelLine(String line)
	{
		this.intelLine = line == null ? "" : line;
	}
	
	public void setIntelLog(List<String> log)
	{
		this.intelLog = log == null ? List.of() : new ArrayList<>(log);
	}
	
	public void render(GuiGraphics graphics, DeltaTracker delta)
	{
		if(MC.options.hideGui)
			return;
		
		Font font = MC.font;
		int x = 4;
		int y = 4;
		
		if(!line.isEmpty())
		{
			drawBar(graphics, font, x, y, line, 0xFFFFFFFF);
			y += 12;
		}
		if(!playerLine.isEmpty())
		{
			drawBar(graphics, font, x, y, playerLine, 0xFF55FF55);
			y += 12;
		}
		if(!intelLine.isEmpty())
		{
			drawBar(graphics, font, x, y, intelLine, 0xFF55AAFF);
			y += 12;
		}
		for(String entry : intelLog)
		{
			drawBar(graphics, font, x, y, entry, 0xFFCCCCCC);
			y += 11;
		}
	}
	
	private void drawBar(GuiGraphics graphics, Font font, int x, int y,
		String text, int color)
	{
		int w = font.width(text);
		graphics.fill(x - 2, y - 2, x + w + 2, y + 10, 0x90000000);
		graphics.drawString(font, text, x, y, color, true);
	}
}
