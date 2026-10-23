/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.chestesp;

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
	
	public void render(GuiGraphics graphics, DeltaTracker delta)
	{
		if(line.isEmpty() || MC.options.hideGui)
			return;
		
		Font font = MC.font;
		int x = 4;
		int y = 4;
		graphics.fill(x - 2, y - 2, x + font.width(line) + 2, y + 10, 0x60000000);
		graphics.drawString(font, line, x, y, 0xFFFFFFFF, true);
	}
}
