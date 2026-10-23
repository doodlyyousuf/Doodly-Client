/*
 * Multi-band HUD: ChestESP / BaseCluster
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
	
	public void setLine(String line)
	{
		this.line = line == null ? "" : line;
	}
	
	public void setPlayerLine(String line) { /* removed — use standalone PlayerESP */ }
	public void setIntelLine(String line) {}
	public void setIntelLog(List<String> log) {}
	
	public void render(GuiGraphics graphics, DeltaTracker delta)
	{
		if(MC.options.hideGui)
			return;
		if(line.isEmpty())
			return;
		
		Font font = MC.font;
		int x = 4;
		int y = 4;
		int w = font.width(line);
		graphics.fill(x - 2, y - 2, x + w + 2, y + 10, 0x90000000);
		graphics.drawString(font, line, x, y, 0xFFFFFFFF, true);
	}
}
