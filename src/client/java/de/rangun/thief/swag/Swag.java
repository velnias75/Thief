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

import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.JsonOps;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Clipboard;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SkullItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public final class Swag implements Inventory {

	private final static String GIVE_CMD_PREFIX = "/give @p ";
	private final static Path CONFIGPATH = FabricLoader.getInstance().getConfigDir().resolve("thief");
	private final static Path SWAGFILE = CONFIGPATH.resolve("swag.json");

	private final static NbtString LORE_LEFT_CLICK = NbtString
			.of(fixItalic(Text.Serializer.toJson(new LiteralText("Left click: ")
					.append(new LiteralText("try to give item to player").formatted(Formatting.AQUA))
					.formatted(Formatting.DARK_AQUA))));

	private final static NbtString LORE_RIGHT_CLICK = NbtString
			.of(fixItalic(
					Text.Serializer.toJson(new LiteralText("Right click: ")
							.append(new LiteralText("copy item's ")
									.append(new LiteralText("/give command").formatted(Formatting.ITALIC))
									.append(" to clipboard").formatted(Formatting.GREEN))
							.formatted(Formatting.DARK_GREEN))));

	private final static NbtString LORE_REMOVE_CLICK = NbtString
			.of(fixItalic(Text.Serializer.toJson(new LiteralText("SHIFT+right click: ")
					.append(new LiteralText("removes item from swag").formatted(Formatting.RED))
					.formatted(Formatting.DARK_RED))));

	private final JsonObject container;
	private final JsonArray swag;

	private static Swag instance = null;

	private final static class CopyDO {

		private final List<String> copyCmds;
		private final String lastItemKey;

		public CopyDO(final List<String> copyCmds, final String lastItemKey) {
			this.copyCmds = copyCmds;
			this.lastItemKey = lastItemKey;
		}
	}

	private Swag() throws JsonSyntaxException, IOException {

		if (Files.exists(SWAGFILE)) {

			this.container = JsonParser.parseString(Files.readString(SWAGFILE)).getAsJsonObject();
			this.swag = this.container.get("swag").getAsJsonArray();

		} else {
			this.container = new JsonObject();
			this.swag = new JsonArray();
			this.container.add("swag", swag);
		}

		if (!Files.exists(CONFIGPATH)) {
			try {
				Files.createDirectory(CONFIGPATH);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	private static String fixItalic(final String json) {

		final JsonElement el = JsonParser.parseString(json);

		if (el.isJsonObject()) {

			final JsonObject jo = el.getAsJsonObject();
			boolean hasItalic = false;

			for (final Entry<String, JsonElement> b : jo.entrySet()) {

				if ("italic".equalsIgnoreCase(b.getKey())) {
					hasItalic = true;
					break;
				}
			}

			if (!hasItalic) {
				jo.addProperty("italic", false);
			}
		}

		return el.toString();
	}

	public static Swag getInstance() {

		try {
			return instance != null ? instance : new Swag();
		} catch (JsonSyntaxException | IOException e) {
			return null;
		}
	}

	public JsonObject add(final Item item, final NbtCompound nbt) throws IOException {

		final JsonObject jobj = new JsonObject();

		jobj.addProperty("item", Registry.ITEM.getId(item).toString());
		jobj.addProperty("translation_key", item.getTranslationKey());
		jobj.add("nbt", NbtCompound.CODEC.encodeStart(JsonOps.COMPRESSED, nbt).getOrThrow(false, (msg) -> {
		}));

		swag.add(jobj);

		serialize();

		return jobj;
	}

	public void send(final JsonObject jobjs, Consumer<String> consumer) {
		send(Lists.asList(jobjs, new JsonObject[0]), consumer);
	}

	public void send(final List<JsonObject> jobjs, Consumer<String> consumer) {

		final CopyDO copyCmds = createCopyCommands(jobjs);

		if (consumer != null) {
			consumer.accept(copyCmds.lastItemKey);
		}

		final String cmd = String.join(System.lineSeparator() + System.lineSeparator(), copyCmds.copyCmds);

		send(cmd);
	}

	public void send(final String cmd) {
		new Clipboard().setClipboard(MinecraftClient.getInstance().getWindow().getHandle(),
				String.join(System.lineSeparator() + System.lineSeparator(), cmd));
	}

	private CopyDO createCopyCommands(final List<JsonObject> jobjs) {

		final List<String> copyCmds = new ArrayList<>(jobjs.size());

		String lastItemKey = null;

		for (final JsonObject jobj : jobjs) {

			final String itemKey = jobj.getAsJsonPrimitive("item").getAsString();
			final NbtCompound nbt = NbtCompound.CODEC.decode(JsonOps.INSTANCE, jobj.getAsJsonObject("nbt"))
					.getOrThrow(false, (msg) -> {
					}).getFirst();

			lastItemKey = jobj.getAsJsonPrimitive("translation_key").getAsString();
			copyCmds.add((GIVE_CMD_PREFIX + itemKey + nbt.asString()).trim());
		}

		return new CopyDO(copyCmds, lastItemKey);
	}

	private void serialize() throws IOException {

		Path path = SWAGFILE;

		if (!Files.exists(path)) {
			path = Files.createFile(path);
		}

		try (Writer w = new FileWriter(path.toFile())) {
			w.write(container.toString());
		}
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return 54;
	}

	@Override
	public boolean isEmpty() {
		return !(container.has("swag") && swag.size() > 0);
	}

	@Override
	public ItemStack getStack(int slot) {

		if (slot != ScreenHandler.EMPTY_SPACE_SLOT_INDEX && slot < Math.min(swag.size(), 54)) {

			final JsonObject itemDesc = swag.get(slot).getAsJsonObject();
			final ItemStack item = new ItemStack(Registry.ITEM.get(new Identifier(itemDesc.get("item").getAsString())),
					1);

			try {

				final String nbtString = itemDesc.get("nbt").getAsJsonObject().toString();

				item.setNbt(StringNbtReader.parse(nbtString));

				if (item.getItem() instanceof SkullItem) {
					fixSkull(item);
				}

			} catch (CommandSyntaxException e) {
				e.printStackTrace();
			}

			final NbtCompound nbt = item.getOrCreateNbt();

			if (!nbt.contains(ItemStack.DISPLAY_KEY, NbtElement.COMPOUND_TYPE)) {
				nbt.put(ItemStack.DISPLAY_KEY, new NbtCompound());
			}

			final NbtCompound display = nbt.getCompound(ItemStack.DISPLAY_KEY);

			if (!display.contains(ItemStack.LORE_KEY, NbtElement.COMPOUND_TYPE)) {
				display.put(ItemStack.LORE_KEY, new NbtList());
			}

			final NbtList lore = display.getList(ItemStack.LORE_KEY, NbtElement.STRING_TYPE);

			if (createCopyCommands(Arrays.asList(new JsonObject[] { itemDesc })).copyCmds.get(0).length() <= 256) {
				lore.add(LORE_LEFT_CLICK);
			}

			lore.add(LORE_RIGHT_CLICK);
			lore.add(LORE_REMOVE_CLICK);

			return item;
		}

		return ItemStack.EMPTY;
	}

	private void fixSkull(final ItemStack stack) {

		final NbtCompound nbtCompound = stack.getNbt();

		if (nbtCompound.contains("SkullOwner", NbtElement.COMPOUND_TYPE)) {

			final NbtCompound nbtCompound2 = nbtCompound.getCompound("SkullOwner");

			if (!nbtCompound2.contains("Name", NbtElement.STRING_TYPE)) {
				nbtCompound2.putString("Name", "Unknown Victim");
			}
		}
	}

	@Override
	public ItemStack removeStack(int slot, int amount) {
		return removeStack(slot);
	}

	@Override
	public ItemStack removeStack(int slot) {

		swag.remove(slot);

		try {
			serialize();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return ItemStack.EMPTY;
	}

	@Override
	public void setStack(int slot, ItemStack stack) {
		// intentionally does nothing
	}

	@Override
	public void markDirty() {
		// intentionally does nothing
	}

	@Override
	public boolean canPlayerUse(PlayerEntity player) {
		return false;
	}

	@Override
	public int getMaxCountPerStack() {
		return 1;
	}

	public String getGiveCmd(int slotIndex) {

		final ItemStack item = getStack(slotIndex);

		if (!ItemStack.EMPTY.equals(item)) {

			final List<JsonObject> list = new ArrayList<>(1);
			list.add(swag.get(slotIndex).getAsJsonObject());

			return createCopyCommands(list).copyCmds.get(0);
		}

		return null;
	}
}
