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

package de.rangun.thief;

import static net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.DISPATCHER;
import static net.fabricmc.fabric.api.client.command.v1.ClientCommandManager.literal;

import de.rangun.thief.commands.StealCommand;
import de.rangun.thief.commands.SwagCommand;
import de.rangun.thief.swag.SwagScreen;
import de.rangun.thief.swag.SwagScreenHandler;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public final class ThiefMod implements ClientModInitializer {

	private final static ScreenHandlerType<SwagScreenHandler> SWAGTYPE = Registry.register(Registry.SCREEN_HANDLER,
			new Identifier("thief", "swag"), new ScreenHandlerType<>(SwagScreenHandler::new));

	@Override
	public void onInitializeClient() {

		HandledScreens.register(SWAGTYPE, SwagScreen::new);

		DISPATCHER.register(literal("steal").executes(new StealCommand()));
		DISPATCHER.register(literal("swag").executes(new SwagCommand()));
	}

	public static ScreenHandlerType<SwagScreenHandler> getSwagHandlerType() {
		return SWAGTYPE;
	}
}
