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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import de.rangun.thief.ThiefMod;
import de.rangun.thief.swag.Swag;
import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.BannerItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.SkullItem;
import net.minecraft.item.WritableBookItem;
import net.minecraft.item.WrittenBookItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;

public final class StealCommand implements Command<FabricClientCommandSource> {

	private final ThiefMod mod;

	public StealCommand(final ThiefMod thiefMod) {
		this.mod = thiefMod;
	}

	@Override
	public int run(final CommandContext<FabricClientCommandSource> context) throws CommandSyntaxException {

		final MinecraftClient client = context.getSource().getClient();
		final HitResult hit = client.crosshairTarget;
		final Swag swag = mod.getSwag();

		boolean found = false;

		switch (hit.getType()) {
		case MISS:
			break;
		case BLOCK:

			final BlockHitResult blockHit = (BlockHitResult) hit;
			final BlockPos blockPos = blockHit.getBlockPos();
			final BlockState blockState = context.getSource().getWorld().getBlockState(blockPos);
			final Block block = blockState.getBlock();
			final BlockEntity be = context.getSource().getWorld().getBlockEntity(blockPos);

			if (be != null) {

				if (be instanceof SkullBlockEntity) {

					final SkullBlockEntity sbe = (SkullBlockEntity) be;
					final NbtCompound nbt = sbe.createNbt();

					sbe.readNbt(nbt);

					try {
						found = storeCopySend(block.asItem(), nbt, swag, context);
					} catch (IOException e) {
						// TODO log failure
					}

				} else if (be instanceof BannerBlockEntity) {

					try {
						found = storeCopySend(block.asItem(), ((BannerBlockEntity) be).getPickStack().getOrCreateNbt(),
								swag, context);

					} catch (IOException e) {
						// TODO log failure
					}
				}
			}

			break;
		case ENTITY:

			final EntityHitResult entityHit = (EntityHitResult) hit;
			final Entity entity = entityHit.getEntity();

			if (entity instanceof ArmorStandEntity) {

				try {

					final ArmorStandEntity as = (ArmorStandEntity) entity;
					final List<JsonObject> jobjs = new ArrayList<>(getStealable(as.getArmorItems(), swag));

					jobjs.addAll(getStealable(as.getItemsHand(), swag));

					if (!jobjs.isEmpty()) {

						swag.send(jobjs, (item) -> context.getSource().sendFeedback(getFeedbackText(
								Util.createTranslationKey("entity", Registry.ENTITY_TYPE.getId(entity.getType())))));

						found = true;
					}

				} catch (IOException e) {
					// TODO log failure
				}

			} else if (entity instanceof ItemFrameEntity) {

				final ItemStack heldItemStack = ((ItemFrameEntity) entity).getHeldItemStack();

				if (isStealable(heldItemStack.getItem())) {

					try {

						swag.send(swag.add(heldItemStack.getItem(), heldItemStack.getOrCreateNbt()),
								(item3) -> context.getSource().sendFeedback(getFeedbackText(item3)));

						found = storeCopySend(heldItemStack.getItem(), heldItemStack.getOrCreateNbt(), swag, context);

					} catch (IOException e) {
						// TODO log failure
					}
				}
			}

			break;
		}

		if (!found) {
			context.getSource().sendError(new LiteralText("No suitable object found at crosshair position."));
		}

		return Command.SINGLE_SUCCESS;
	}

	private static boolean storeCopySend(final Item item, final NbtCompound nbt, final Swag swag,
			final CommandContext<FabricClientCommandSource> context) throws IOException {

		swag.send(swag.add(item, nbt), (it) -> context.getSource().sendFeedback(getFeedbackText(it)));

		return true;
	}

	private static final List<JsonObject> getStealable(final Iterable<ItemStack> items, final Swag swag)
			throws IOException {

		final List<JsonObject> heads = new ArrayList<>(6);

		for (final ItemStack item : items) {

			if (isStealable(item.getItem())) {
				heads.add(swag.add(item.getItem(), item.getOrCreateNbt()));
			}
		}

		return heads;
	}

	private static Text getFeedbackText(final String key) {
		return (new TranslatableText(key).formatted(Formatting.DARK_AQUA)).append(" copied as ")
				.append(new LiteralText("/give command").formatted(Formatting.AQUA, Formatting.ITALIC))
				.append(new LiteralText(" to clipboard.")).formatted(Formatting.DARK_AQUA);
	}

	private static boolean isStealable(final Item item) {
		return item instanceof SkullItem || item instanceof BannerItem || item instanceof ShieldItem
				|| item instanceof WritableBookItem || item instanceof WrittenBookItem;
	}
}
