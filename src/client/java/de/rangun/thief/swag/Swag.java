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
import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.Lists;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.mojang.serialization.JsonOps;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Clipboard;
import net.minecraft.item.Item;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.registry.Registry;

public final class Swag {

	private final static String GIVE_CMD_PREFIX = "/give @p ";
	private final static Path CONFIGPATH = FabricLoader.getInstance().getConfigDir().resolve("thief");
	private final static Path SWAGFILE = CONFIGPATH.resolve("swag.json");
	private final JsonObject container;
	private final JsonArray swag;

	public Swag() throws JsonSyntaxException, IOException {

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

	public Swag(final boolean empty) {
		this.container = new JsonObject();
		this.swag = new JsonArray();
		this.container.add("swag", swag);
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

		if (consumer != null) {
			consumer.accept(lastItemKey);
		}

		new Clipboard().setClipboard(MinecraftClient.getInstance().getWindow().getHandle(),
				String.join(System.lineSeparator() + System.lineSeparator(), copyCmds));
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
}
