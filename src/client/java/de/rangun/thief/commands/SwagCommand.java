/*
 * Copyright 2022 by Heiko Schäfer <heiko@rangun.de>
 *
 * This file is part of Thief.
 *
 * Thief is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Thief is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Thief.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.rangun.thief.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import de.rangun.thief.ThiefMod;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.text.LiteralText;

public final class SwagCommand implements Command<FabricClientCommandSource> {

	@Override
	public int run(CommandContext<FabricClientCommandSource> context) throws CommandSyntaxException {

		context.getSource().getClient()
				.send(() -> HandledScreens.open(ThiefMod.getSwagHandlerType(), context.getSource().getClient(),
						context.getSource().getPlayer().playerScreenHandler.syncId, new LiteralText("Swag")));

		return Command.SINGLE_SUCCESS;
	}
}
