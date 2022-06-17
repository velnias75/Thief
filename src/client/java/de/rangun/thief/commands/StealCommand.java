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

import java.util.ArrayList;
import java.util.List;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SkullBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Clipboard;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.BannerItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.SkullItem;
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

	private final static String GIVE_CMD_PREFIX = "/give @p ";

	@Override
	public int run(final CommandContext<FabricClientCommandSource> context) throws CommandSyntaxException {

		final MinecraftClient client = context.getSource().getClient();
		final HitResult hit = client.crosshairTarget;

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

					copyGiveCmdToClipboard(block.asItem(), nbt, context);
					found = true;

				} else if (be instanceof BannerBlockEntity) {

					final BannerBlockEntity ban = (BannerBlockEntity) be;
					final ItemStack item = ban.getPickStack();

					copyGiveCmdToClipboard(block.asItem(), item.getOrCreateNbt(), context);
					found = true;
				}
			}

			break;
		case ENTITY:

			final EntityHitResult entityHit = (EntityHitResult) hit;
			final Entity entity = entityHit.getEntity();

			if (entity instanceof ArmorStandEntity) {

				final ArmorStandEntity as = (ArmorStandEntity) entity;
				final List<String> heads = new ArrayList<>();

				for (final ItemStack item : as.getArmorItems()) {

					if (item.getItem() instanceof SkullItem || item.getItem() instanceof BannerItem
							|| item.getItem() instanceof ShieldItem) {
						heads.add(GIVE_CMD_PREFIX + Registry.ITEM.getId(item.getItem())
								+ item.getOrCreateNbt().asString());

					}
				}

				for (final ItemStack item : as.getItemsHand()) {

					if (item.getItem() instanceof SkullItem || item.getItem() instanceof BannerItem
							|| item.getItem() instanceof ShieldItem) {
						heads.add(GIVE_CMD_PREFIX + Registry.ITEM.getId(item.getItem())
								+ item.getOrCreateNbt().asString());

					}
				}

				if (!heads.isEmpty()) {

					context.getSource().sendFeedback(getFeedbackText(
							Util.createTranslationKey("entity", Registry.ENTITY_TYPE.getId(entity.getType()))));

					new Clipboard().setClipboard(MinecraftClient.getInstance().getWindow().getHandle(),
							String.join("\n", heads).trim());

					found = true;
				}

			} else if (entity instanceof ItemFrameEntity) {

				final ItemFrameEntity ifr = (ItemFrameEntity) entity;

				if (ifr.getHeldItemStack().getItem() instanceof SkullItem
						|| ifr.getHeldItemStack().getItem() instanceof BannerItem
						|| ifr.getHeldItemStack().getItem() instanceof ShieldItem) {

					copyGiveCmdToClipboard(ifr.getHeldItemStack().getItem(), ifr.getHeldItemStack().getOrCreateNbt(),
							context);
					found = true;
				}
			}

			break;
		}

		if (!found) {
			context.getSource().sendError(new LiteralText("No suitable object found at crosshair position."));
		}

		return Command.SINGLE_SUCCESS;
	}

	private void copyGiveCmdToClipboard(final Item item, final NbtCompound nbt,
			final CommandContext<FabricClientCommandSource> context) {

		context.getSource().sendFeedback(getFeedbackText(item));

		new Clipboard().setClipboard(MinecraftClient.getInstance().getWindow().getHandle(),
				(GIVE_CMD_PREFIX + Registry.ITEM.getId(item) + nbt.asString()).trim());

	}

	private Text getFeedbackText(final Item item) {
		return getFeedbackText(item.getTranslationKey());
	}

	private Text getFeedbackText(final String key) {
		return (new TranslatableText(key).formatted(Formatting.DARK_AQUA)).append(" copied as ")
				.append(new LiteralText("/give command").formatted(Formatting.AQUA, Formatting.ITALIC))
				.append(new LiteralText(" to clipboard.")).formatted(Formatting.DARK_AQUA);
	}
}
