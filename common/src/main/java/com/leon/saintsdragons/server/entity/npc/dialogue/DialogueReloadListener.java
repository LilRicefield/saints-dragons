package com.leon.saintsdragons.server.entity.npc.dialogue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.leon.saintsdragons.common.SaintsDragonsCommon;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class DialogueReloadListener extends SimpleJsonResourceReloadListener {
    private static final Gson GSON = new GsonBuilder().create();
    private static final DialogueReloadListener INSTANCE = new DialogueReloadListener();

    private DialogueReloadListener() {
        super(GSON, "dialogues");
    }

    public static DialogueReloadListener getInstance() {
        return INSTANCE;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> jsonMap,
                         @NotNull ResourceManager resourceManager,
                         @NotNull ProfilerFiller profiler) {
        Map<ResourceLocation, DialogueDefinition> parsed = new LinkedHashMap<>();
        jsonMap.entrySet().stream()
                .sorted(Comparator.comparing(entry -> entry.getKey().toString()))
                .forEach(entry -> parseFile(entry.getKey(), entry.getValue(), parsed));
        DialogueRegistry.replaceDatapackDialogues(parsed);
    }

    private static void parseFile(ResourceLocation fileId,
                                  JsonElement element,
                                  Map<ResourceLocation, DialogueDefinition> parsed) {
        try {
            JsonObject root = GsonHelper.convertToJsonObject(element, fileId.toString());
            String start = GsonHelper.getAsString(root, root.has("start_at") ? "start_at" : "start", "start");
            JsonObject nodesJson = GsonHelper.getAsJsonObject(root, root.has("states") ? "states" : "nodes");
            Map<String, DialogueDefinition.Node> nodes = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : nodesJson.entrySet()) {
                JsonObject nodeJson = GsonHelper.convertToJsonObject(entry.getValue(), fileId + " node " + entry.getKey());
                Component speaker = nodeJson.has("speaker") ? parseComponent(nodeJson.get("speaker")) : Component.empty();
                List<Component> texts = parseNodeTexts(nodeJson);
                Component text = nodeJson.has("text") && !nodeJson.get("text").isJsonArray()
                        ? parseComponent(nodeJson.get("text"))
                        : texts.isEmpty() ? Component.empty() : texts.get(0);
                List<DialogueDefinition.Choice> choices = nodeJson.has("choices")
                        ? parseChoices(fileId, GsonHelper.getAsJsonArray(nodeJson, "choices"))
                        : List.of();
                DialogueDefinition.Type type = DialogueDefinition.Type.byName(GsonHelper.getAsString(nodeJson, "type", "default"));
                nodes.put(entry.getKey(), new DialogueDefinition.Node(speaker, text, texts, choices, type));
            }
            DialogueDefinition dialogue = new DialogueDefinition(fileId, start, nodes);
            DialogueValidationResult validation = DialogueValidator.validate(dialogue);
            if (!validation.valid()) {
                SaintsDragonsCommon.LOGGER.error("Failed to validate dialogue file {}: {}", fileId, validation.message());
                return;
            }
            parsed.put(fileId, dialogue);
        } catch (Exception exception) {
            SaintsDragonsCommon.LOGGER.error("Failed to parse dialogue file {}", fileId, exception);
        }
    }

    private static DialogueDefinition.Choice parseChoice(ResourceLocation fileId, JsonElement element) {
        JsonObject choice = GsonHelper.convertToJsonObject(element, fileId + " choice");
        return new DialogueDefinition.Choice(
                parseComponent(choice.get("text")),
                GsonHelper.getAsString(choice, "next", ""),
                GsonHelper.getAsString(choice, "impression", ""),
                GsonHelper.getAsString(choice, "set_flag", ""),
                GsonHelper.getAsString(choice, "requires_flag", ""),
                GsonHelper.getAsString(choice, "hidden_if_flag", "")
        );
    }

    private static List<Component> parseNodeTexts(JsonObject nodeJson) {
        if (nodeJson.has("texts")) {
            return parseComponents(GsonHelper.getAsJsonArray(nodeJson, "texts"));
        }
        if (nodeJson.has("text") && nodeJson.get("text").isJsonArray()) {
            return parseComponents(GsonHelper.getAsJsonArray(nodeJson, "text"));
        }
        return List.of();
    }

    private static Component parseComponent(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return Component.empty();
        }
        if (element.isJsonPrimitive()) {
            return Component.translatable(element.getAsString());
        }
        JsonObject object = GsonHelper.convertToJsonObject(element, "dialogue component");
        if (object.has("text") && object.get("text").isJsonPrimitive() && !object.has("translate")) {
            return parseStyledLiteral(object.get("text").getAsString());
        }
        Component component = Component.Serializer.fromJson(element);
        return component == null ? Component.empty() : component;
    }

    private static Component parseStyledLiteral(String text) {
        var root = Component.literal("");
        int index = 0;
        while (index < text.length()) {
            int markerStart = text.indexOf('*', index);
            if (markerStart < 0) {
                root.append(text.substring(index));
                break;
            }
            int markerLength = markerStart + 1 < text.length() && text.charAt(markerStart + 1) == '*' ? 2 : 1;
            String marker = "*".repeat(markerLength);
            int markerEnd = text.indexOf(marker, markerStart + markerLength);
            if (markerEnd < 0) {
                root.append(text.substring(index));
                break;
            }
            if (markerStart > index) {
                root.append(text.substring(index, markerStart));
            }
            String italicText = text.substring(markerStart + markerLength, markerEnd);
            root.append(Component.literal(italicText).withStyle(style -> style.withItalic(true)));
            index = markerEnd + markerLength;
        }
        return root;
    }

    private static List<DialogueDefinition.Choice> parseChoices(ResourceLocation fileId, JsonArray array) {
        List<DialogueDefinition.Choice> choices = new ArrayList<>();
        for (JsonElement element : array) {
            choices.add(parseChoice(fileId, element));
        }
        return choices;
    }

    private static List<Component> parseComponents(JsonArray array) {
        List<Component> components = new ArrayList<>();
        for (JsonElement element : array) {
            components.add(parseComponent(element));
        }
        return components;
    }
}
