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

package de.rangun.thief.swag;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;

public final class SwagScreenHandler extends GenericContainerScreenHandler {

	public SwagScreenHandler(final int syncId, final PlayerInventory playerInventory) {
		super(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, Swag.getInstance(), 6);
	}

	@Override
	public boolean canUse(PlayerEntity player) {
		return getInventory().canPlayerUse(player);
	}

	@Override
	public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {

		super.onSlotClick(slotIndex, button, actionType, player);
		
		final Swag swag = (Swag) getInventory();
		final String cmd = swag.getGiveCmd(slotIndex);

		if (cmd != null) {

			if (SlotActionType.PICKUP.equals(actionType))
				switch (button) {
				case GLFW.GLFW_MOUSE_BUTTON_1:
					if (player instanceof ClientPlayerEntity && cmd.length() <= 256) {
						((ClientPlayerEntity) player).sendChatMessage(cmd);
					}
					break;
				case GLFW.GLFW_MOUSE_BUTTON_2:
					swag.send(cmd);
					break;
				default:
					System.out.println("Button: " + button);
				}
		}
	}
}
