/*
 * Copyright (c) 2023-2026 Wurst-Imperium and contributors.
 *
 * This source code is subject to the terms of the GNU General Public
 * License, version 3. If a copy of the GPL was not distributed with this
 * file, You can obtain one at: https://www.gnu.org/licenses/gpl-3.0.txt
 */
package net.wimods.chestesp;

import java.util.ArrayList;

import me.shedaniel.autoconfig.ConfigHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.wimods.chestesp.util.ContainerInspector;
import net.wimods.chestesp.util.ContainerInspector.Snapshot;
import net.wimods.chestesp.util.EntityUtils;

public abstract class ChestEspEntityGroup extends ChestEspGroup
{
	private final ArrayList<Entity> entities = new ArrayList<>();
	
	public ChestEspEntityGroup(ConfigHolder<ChestEspConfig> configHolder,
		String name)
	{
		super(configHolder, name);
	}
	
	public abstract boolean matches(Entity e);
	
	public final void addIfMatches(Entity e)
	{
		if(!matches(e))
			return;
		
		entities.add(e);
	}
	
	@Override
	public void clear()
	{
		entities.clear();
		super.clear();
	}
	
	public void updateBoxes(float partialTicks)
	{
		boxes.clear();
		targets.clear();
		
		for(Entity e : entities)
		{
			AABB box = EntityUtils.getLerpedBox(e, partialTicks);
			Snapshot snap = ContainerInspector.inspect(e, config());
			ChestEspTarget target = new ChestEspTarget(box, getDisplayName(),
				0xE000000000000000L ^ e.getId(), snap.filledSlots,
				snap.totalSlots, snap.known, snap.matchesFilter);
			if(!target.passes(config()))
				continue;
			
			targets.add(target);
			boxes.add(box);
		}
	}
}
