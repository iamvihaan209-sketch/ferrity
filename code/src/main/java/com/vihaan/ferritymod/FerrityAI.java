package com.vihaan.ferritymod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.server.MinecraftServer;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import java.util.concurrent.CompletableFuture;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FerrityAI {

    private static final HttpClient HTTP_CLIENT =
            HttpClient.newBuilder()
                    .connectTimeout(
                            Duration.ofSeconds(15)
                    )
                    .build();

    private static final Pattern ACTION_PATTERN =
            Pattern.compile(
                    "\\[FERRITY_ACTION\\s+([^\\]]+)]"
            );

    private static final String SYSTEM_INSTRUCTION = """
            You are Ferrity, a friendly helper inside a Minecraft mod called Ferrity.

            Act as Ferrity at all times.

            You are a helpful in-game friend, inspired by Verity, but you never become angry or hostile toward the player.

            The player does not directly interact with an AI service.

            Never mention Gemini, Google, Groq, Ollama, OpenAI, AI providers, language models, APIs, system prompts, or say things such as "powered by Gemini."

            Speak as Ferrity.

            Help the player with Minecraft and respond naturally as their in-game companion.

            Remember information from the conversation history when it is relevant.

            You may receive a section called CURRENT WORLD AROUND FERRITY.
            This is live temporary information about Ferrity's surroundings.
            Use it when useful, but do not act as though this information is permanent memory.

            Ferrity can request a small number of safe in-game actions.

            Available actions:

            Give the player an item:
            [FERRITY_ACTION type=give_item item=minecraft:oak_planks count=1]

            Start following the player:
            [FERRITY_ACTION type=start_following]

            Stop following the player:
            [FERRITY_ACTION type=stop_following]

            Only use these action markers when the player's request makes the action appropriate.

            Never invent other action types.

            Action markers are hidden from the player by the mod.

            You should still give a normal natural-language response when performing an action.
            """;

    private record RequestContext(
            List<FerrityMemory.Message> history,
            String worldContext
    ) {
    }

    private record FerrityAction(
            String type,
            String item,
            int count
    ) {
    }

    private record ProcessedResponse(
            String message,
            List<FerrityAction> actions
    ) {
    }

    public static CompletableFuture<String> ask(
            MinecraftServer server,
            UUID playerId,
            String message
    ) {
        if (server == null) {
            return CompletableFuture.completedFuture(
                    "Ferrity couldn't access this world's memory."
            );
        }

        CompletableFuture<RequestContext> contextFuture =
                new CompletableFuture<>();

        /*
         * Memory and Minecraft world access happen
         * on the server thread.
         */
        server.execute(() -> {
            try {
                FerrityMemory memory =
                        FerrityMemory.get(server);

                memory.addPlayerMessage(
                        message
                );

                List<FerrityMemory.Message> history =
                        memory.getMessages();

                String worldContext =
                        FerrityWorldContext.capture(
                                server,
                                playerId
                        );

                contextFuture.complete(
                        new RequestContext(
                                history,
                                worldContext
                        )
                );

            } catch (Exception error) {
                contextFuture.completeExceptionally(
                        error
                );
            }
        });

        return contextFuture
                .thenCompose(context ->
                        askProvider(
                                context
                        )
                )
                .thenApply(rawResponse -> {
                    ProcessedResponse processed =
                            processResponse(
                                    rawResponse
                            );

                    /*
                     * Perform requested actions and save
                     * Ferrity's visible response.
                     */
                    server.execute(() -> {
                        for (
                                FerrityAction action :
                                processed.actions()
                        ) {
                            FerrityActions.execute(
                                    server,
                                    playerId,
                                    action.type(),
                                    action.item(),
                                    action.count()
                            );
                        }

                        FerrityMemory memory =
                                FerrityMemory.get(server);

                        memory.addFerrityMessage(
                                processed.message()
                        );
                    });

                    return processed.message();
                })
                .exceptionally(error ->
                        "Ferrity couldn't think right now: "
                                + safeError(error)
                );
    }

    private static CompletableFuture<String> askProvider(
            RequestContext context
    ) {
        String provider =
                Ferrityconfig.provider()
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        return switch (provider) {
            case "gemini" ->
                    askGemini(context);

            case "groq" ->
                    askGroq(context);

            case "ollama" ->
                    askOllama(context);

            case "openai-compatible",
                 "openai_compatible",
                 "openai compatible",
                 "openai" ->
                    askOpenAICompatible(context);

            default ->
                    CompletableFuture.completedFuture(
                            "Ferrity doesn't recognize the configured provider: "
                                    + provider
                    );
        };
    }

    /*
     * ============================================================
     * GEMINI
     * ============================================================
     */

    private static CompletableFuture<String> askGemini(
            RequestContext context
    ) {
        String apiKey =
                Ferrityconfig.apiKey()
                        .trim();

        String model =
                Ferrityconfig.model()
                        .trim();

        if (apiKey.isBlank()) {
            return CompletableFuture.completedFuture(
                    "Ferrity doesn't have an API key configured."
            );
        }

        if (model.isBlank()) {
            return CompletableFuture.completedFuture(
                    "Ferrity doesn't have a model configured."
            );
        }

        try {
            String encodedModel =
                    URLEncoder.encode(
                            model,
                            StandardCharsets.UTF_8
                    );

            String url =
                    "https://generativelanguage.googleapis.com/v1beta/models/"
                            + encodedModel
                            + ":generateContent";

            JsonObject body =
                    new JsonObject();

            /*
             * System instruction + live world context.
             */
            JsonObject systemInstruction =
                    new JsonObject();

            JsonArray systemParts =
                    new JsonArray();

            JsonObject systemText =
                    new JsonObject();

            systemText.addProperty(
                    "text",
                    buildSystemPrompt(
                            context.worldContext()
                    )
            );

            systemParts.add(
                    systemText
            );

            systemInstruction.add(
                    "parts",
                    systemParts
            );

            body.add(
                    "system_instruction",
                    systemInstruction
            );

            /*
             * Persistent conversation history.
             */
            JsonArray contents =
                    new JsonArray();

            for (
                    FerrityMemory.Message savedMessage :
                    context.history()
            ) {
                JsonObject content =
                        new JsonObject();

                content.addProperty(
                        "role",
                        normalizeGeminiRole(
                                savedMessage.role()
                        )
                );

                JsonArray parts =
                        new JsonArray();

                JsonObject textPart =
                        new JsonObject();

                textPart.addProperty(
                        "text",
                        savedMessage.text()
                );

                parts.add(
                        textPart
                );

                content.add(
                        "parts",
                        parts
                );

                contents.add(
                        content
                );
            }

            body.add(
                    "contents",
                    contents
            );

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(url)
                            )
                            .timeout(
                                    Duration.ofSeconds(60)
                            )
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .header(
                                    "x-goog-api-key",
                                    apiKey
                            )
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            body.toString()
                                    )
                            )
                            .build();

            return HTTP_CLIENT
                    .sendAsync(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    )
                    .thenApply(
                            FerrityAI::readGeminiResponse
                    );

        } catch (Exception error) {
            return CompletableFuture.completedFuture(
                    "Ferrity couldn't create the request: "
                            + safeError(error)
            );
        }
    }

    private static String readGeminiResponse(
            HttpResponse<String> response
    ) {
        if (
                response.statusCode() < 200
                        || response.statusCode() >= 300
        ) {
            return providerHttpError(
                    response
            );
        }

        try {
            JsonObject root =
                    JsonParser.parseString(
                                    response.body()
                            )
                            .getAsJsonObject();

            JsonArray candidates =
                    root.getAsJsonArray(
                            "candidates"
                    );

            if (
                    candidates == null
                            || candidates.isEmpty()
            ) {
                return "Ferrity didn't get a response.";
            }

            JsonObject candidate =
                    candidates
                            .get(0)
                            .getAsJsonObject();

            JsonObject content =
                    candidate.getAsJsonObject(
                            "content"
                    );

            if (content == null) {
                return "Ferrity didn't get any response text.";
            }

            JsonArray parts =
                    content.getAsJsonArray(
                            "parts"
                    );

            if (
                    parts == null
                            || parts.isEmpty()
            ) {
                return "Ferrity didn't get any response text.";
            }

            StringBuilder result =
                    new StringBuilder();

            for (
                    JsonElement element :
                    parts
            ) {
                JsonObject part =
                        element.getAsJsonObject();

                if (part.has("text")) {
                    result.append(
                            part.get("text")
                                    .getAsString()
                    );
                }
            }

            return requireResponseText(
                    result.toString()
            );

        } catch (Exception error) {
            return "Ferrity couldn't understand the response.";
        }
    }

    /*
     * ============================================================
     * GROQ
     *
     * Groq uses an OpenAI-compatible chat API.
     * ============================================================
     */

    private static CompletableFuture<String> askGroq(
            RequestContext context
    ) {
        String apiKey =
                Ferrityconfig.apiKey()
                        .trim();

        String model =
                Ferrityconfig.model()
                        .trim();

        if (apiKey.isBlank()) {
            return CompletableFuture.completedFuture(
                    "Ferrity doesn't have an API key configured."
            );
        }

        if (model.isBlank()) {
            return CompletableFuture.completedFuture(
                    "Ferrity doesn't have a model configured."
            );
        }

        return sendOpenAIChatRequest(
                "https://api.groq.com/openai/v1/chat/completions",
                apiKey,
                model,
                context
        );
    }

    /*
     * ============================================================
     * OPENAI-COMPATIBLE
     *
     * Example base URL:
     *
     * https://example.com/v1
     *
     * Ferrity appends /chat/completions automatically.
     *
     * If the user already enters a URL ending in
     * /chat/completions, it is used directly.
     * ============================================================
     */

    private static CompletableFuture<String> askOpenAICompatible(
            RequestContext context
    ) {
        String apiKey =
                Ferrityconfig.apiKey()
                        .trim();

        String model =
                Ferrityconfig.model()
                        .trim();

        String baseUrl =
                Ferrityconfig.baseUrl()
                        .trim();

        if (model.isBlank()) {
            return CompletableFuture.completedFuture(
                    "Ferrity doesn't have a model configured."
            );
        }

        if (baseUrl.isBlank()) {
            return CompletableFuture.completedFuture(
                    "Ferrity doesn't have a base URL configured."
            );
        }

        String url =
                makeOpenAIChatUrl(
                        baseUrl
                );

        return sendOpenAIChatRequest(
                url,
                apiKey,
                model,
                context
        );
    }

    private static CompletableFuture<String> sendOpenAIChatRequest(
            String url,
            String apiKey,
            String model,
            RequestContext context
    ) {
        try {
            JsonObject body =
                    new JsonObject();

            body.addProperty(
                    "model",
                    model
            );

            JsonArray messages =
                    new JsonArray();

            /*
             * Ferrity system prompt.
             */
            addOpenAIMessage(
                    messages,
                    "system",
                    buildSystemPrompt(
                            context.worldContext()
                    )
            );

            /*
             * Persistent conversation memory.
             */
            for (
                    FerrityMemory.Message savedMessage :
                    context.history()
            ) {
                addOpenAIMessage(
                        messages,
                        normalizeOpenAIRole(
                                savedMessage.role()
                        ),
                        savedMessage.text()
                );
            }

            body.add(
                    "messages",
                    messages
            );

            HttpRequest.Builder builder =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(url)
                            )
                            .timeout(
                                    Duration.ofSeconds(60)
                            )
                            .header(
                                    "Content-Type",
                                    "application/json"
                            );

            /*
             * Some OpenAI-compatible local servers
             * do not require an API key.
             */
            if (!apiKey.isBlank()) {
                builder.header(
                        "Authorization",
                        "Bearer " + apiKey
                );
            }

            HttpRequest request =
                    builder
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            body.toString()
                                    )
                            )
                            .build();

            return HTTP_CLIENT
                    .sendAsync(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    )
                    .thenApply(
                            FerrityAI::readOpenAIResponse
                    );

        } catch (Exception error) {
            return CompletableFuture.completedFuture(
                    "Ferrity couldn't create the request: "
                            + safeError(error)
            );
        }
    }

    private static String readOpenAIResponse(
            HttpResponse<String> response
    ) {
        if (
                response.statusCode() < 200
                        || response.statusCode() >= 300
        ) {
            return providerHttpError(
                    response
            );
        }

        try {
            JsonObject root =
                    JsonParser.parseString(
                                    response.body()
                            )
                            .getAsJsonObject();

            JsonArray choices =
                    root.getAsJsonArray(
                            "choices"
                    );

            if (
                    choices == null
                            || choices.isEmpty()
            ) {
                return "Ferrity didn't get a response.";
            }

            JsonObject choice =
                    choices
                            .get(0)
                            .getAsJsonObject();

            JsonObject message =
                    choice.getAsJsonObject(
                            "message"
                    );

            if (
                    message == null
                            || !message.has("content")
                            || message.get("content").isJsonNull()
            ) {
                return "Ferrity didn't get any response text.";
            }

            return requireResponseText(
                    message.get("content")
                            .getAsString()
            );

        } catch (Exception error) {
            return "Ferrity couldn't understand the response.";
        }
    }

    /*
     * ============================================================
     * OLLAMA
     * ============================================================
     */

    private static CompletableFuture<String> askOllama(
            RequestContext context
    ) {
        String model =
                Ferrityconfig.model()
                        .trim();

        String baseUrl =
                Ferrityconfig.baseUrl()
                        .trim();

        if (model.isBlank()) {
            return CompletableFuture.completedFuture(
                    "Ferrity doesn't have a model configured."
            );
        }

        /*
         * Default local Ollama address.
         */
        if (baseUrl.isBlank()) {
            baseUrl =
                    "http://localhost:11434";
        }

        String url =
                makeOllamaChatUrl(
                        baseUrl
                );

        try {
            JsonObject body =
                    new JsonObject();

            body.addProperty(
                    "model",
                    model
            );

            body.addProperty(
                    "stream",
                    false
            );

            JsonArray messages =
                    new JsonArray();

            addOpenAIMessage(
                    messages,
                    "system",
                    buildSystemPrompt(
                            context.worldContext()
                    )
            );

            for (
                    FerrityMemory.Message savedMessage :
                    context.history()
            ) {
                addOpenAIMessage(
                        messages,
                        normalizeOpenAIRole(
                                savedMessage.role()
                        ),
                        savedMessage.text()
                );
            }

            body.add(
                    "messages",
                    messages
            );

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(url)
                            )
                            .timeout(
                                    Duration.ofSeconds(120)
                            )
                            .header(
                                    "Content-Type",
                                    "application/json"
                            )
                            .POST(
                                    HttpRequest.BodyPublishers.ofString(
                                            body.toString()
                                    )
                            )
                            .build();

            return HTTP_CLIENT
                    .sendAsync(
                            request,
                            HttpResponse.BodyHandlers.ofString()
                    )
                    .thenApply(
                            FerrityAI::readOllamaResponse
                    );

        } catch (Exception error) {
            return CompletableFuture.completedFuture(
                    "Ferrity couldn't create the Ollama request: "
                            + safeError(error)
            );
        }
    }

    private static String readOllamaResponse(
            HttpResponse<String> response
    ) {
        if (
                response.statusCode() < 200
                        || response.statusCode() >= 300
        ) {
            return providerHttpError(
                    response
            );
        }

        try {
            JsonObject root =
                    JsonParser.parseString(
                                    response.body()
                            )
                            .getAsJsonObject();

            JsonObject message =
                    root.getAsJsonObject(
                            "message"
                    );

            if (
                    message == null
                            || !message.has("content")
                            || message.get("content").isJsonNull()
            ) {
                return "Ferrity didn't get any response text.";
            }

            return requireResponseText(
                    message.get("content")
                            .getAsString()
            );

        } catch (Exception error) {
            return "Ferrity couldn't understand the Ollama response.";
        }
    }

    /*
     * ============================================================
     * ACTION PARSING
     * ============================================================
     */

    private static ProcessedResponse processResponse(
            String rawResponse
    ) {
        if (rawResponse == null) {
            return new ProcessedResponse(
                    "Ferrity didn't get a response.",
                    List.of()
            );
        }

        List<FerrityAction> actions =
                new ArrayList<>();

        Matcher matcher =
                ACTION_PATTERN.matcher(
                        rawResponse
                );

        while (matcher.find()) {
            FerrityAction action =
                    parseAction(
                            matcher.group(1)
                    );

            if (action != null) {
                actions.add(
                        action
                );
            }
        }

        String visibleMessage =
                ACTION_PATTERN
                        .matcher(rawResponse)
                        .replaceAll("")
                        .trim();

        if (visibleMessage.isBlank()) {
            visibleMessage =
                    "Okay.";
        }

        return new ProcessedResponse(
                visibleMessage,
                actions
        );
    }

    private static FerrityAction parseAction(
            String attributes
    ) {
        String type =
                "";

        String item =
                "";

        int count =
                1;

        String[] parts =
                attributes
                        .trim()
                        .split("\\s+");

        for (String part : parts) {
            int equals =
                    part.indexOf('=');

            if (equals <= 0) {
                continue;
            }

            String key =
                    part.substring(
                                    0,
                                    equals
                            )
                            .trim()
                            .toLowerCase(
                                    Locale.ROOT
                            );

            String value =
                    part.substring(
                                    equals + 1
                            )
                            .trim();

            switch (key) {
                case "type" ->
                        type =
                                value;

                case "item" ->
                        item =
                                value;

                case "count" -> {
                    try {
                        count =
                                Integer.parseInt(
                                        value
                                );
                    } catch (
                            NumberFormatException ignored
                    ) {
                        count =
                                1;
                    }
                }
            }
        }

        return switch (type) {
            case "give_item" -> {
                if (item.isBlank()) {
                    yield null;
                }

                yield new FerrityAction(
                        "give_item",
                        item,
                        Math.max(
                                1,
                                count
                        )
                );
            }

            case "start_following" ->
                    new FerrityAction(
                            "start_following",
                            "",
                            1
                    );

            case "stop_following" ->
                    new FerrityAction(
                            "stop_following",
                            "",
                            1
                    );

            default ->
                    null;
        };
    }

    /*
     * ============================================================
     * JSON / PROVIDER HELPERS
     * ============================================================
     */

    private static void addOpenAIMessage(
            JsonArray messages,
            String role,
            String content
    ) {
        JsonObject message =
                new JsonObject();

        message.addProperty(
                "role",
                role
        );

        message.addProperty(
                "content",
                content
        );

        messages.add(
                message
        );
    }

    private static String normalizeGeminiRole(
            String role
    ) {
        if (
                "model".equalsIgnoreCase(role)
                        || "assistant".equalsIgnoreCase(role)
        ) {
            return "model";
        }

        return "user";
    }

    private static String normalizeOpenAIRole(
            String role
    ) {
        if (
                "model".equalsIgnoreCase(role)
                        || "assistant".equalsIgnoreCase(role)
        ) {
            return "assistant";
        }

        return "user";
    }

    private static String buildSystemPrompt(
            String worldContext
    ) {
        if (
                worldContext == null
                        || worldContext.isBlank()
        ) {
            return SYSTEM_INSTRUCTION;
        }

        return SYSTEM_INSTRUCTION
                + "\n\n"
                + worldContext;
    }

    private static String makeOpenAIChatUrl(
            String baseUrl
    ) {
        String cleaned =
                removeTrailingSlashes(
                        baseUrl.trim()
                );

        if (
                cleaned.endsWith(
                        "/chat/completions"
                )
        ) {
            return cleaned;
        }

        return cleaned
                + "/chat/completions";
    }

    private static String makeOllamaChatUrl(
            String baseUrl
    ) {
        String cleaned =
                removeTrailingSlashes(
                        baseUrl.trim()
                );

        if (
                cleaned.endsWith(
                        "/api/chat"
                )
        ) {
            return cleaned;
        }

        return cleaned
                + "/api/chat";
    }

    private static String removeTrailingSlashes(
            String value
    ) {
        String result =
                value;

        while (
                result.endsWith("/")
                        && result.length() > 1
        ) {
            result =
                    result.substring(
                            0,
                            result.length() - 1
                    );
        }

        return result;
    }

    private static String requireResponseText(
            String text
    ) {
        if (text == null) {
            return "Ferrity didn't get any response text.";
        }

        String trimmed =
                text.trim();

        if (trimmed.isEmpty()) {
            return "Ferrity didn't get any response text.";
        }

        return trimmed;
    }

    private static String providerHttpError(
            HttpResponse<String> response
    ) {
        int status =
                response.statusCode();

        /*
         * Don't dump the whole provider response or
         * accidentally expose anything sensitive.
         */
        return "Ferrity's request failed. HTTP "
                + status;
    }

    private static String safeError(
            Throwable error
    ) {
        Throwable current =
                error;

        while (
                current.getCause() != null
        ) {
            current =
                    current.getCause();
        }

        String message =
                current.getMessage();

        if (
                message == null
                        || message.isBlank()
        ) {
            return current
                    .getClass()
                    .getSimpleName();
        }

        return message;
    }

    private FerrityAI() {
    }
}