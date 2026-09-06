package com.vihaan.ferritymod;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Ferrityconfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.ConfigValue<String> PROVIDER;
    public static final ModConfigSpec.ConfigValue<String> API_KEY;
    public static final ModConfigSpec.ConfigValue<String> MODEL;
    public static final ModConfigSpec.ConfigValue<String> BASE_URL;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment(
                "Ferrity AI configuration",
                "",
                "Supported providers:",
                "gemini",
                "groq",
                "ollama",
                "openai-compatible"
        );

        PROVIDER = builder
                .comment(
                        "Which AI provider Ferrity should use.",
                        "gemini, groq, ollama, or openai-compatible"
                )
                .define(
                        "provider",
                        "gemini"
                );

        API_KEY = builder
                .comment(
                        "API key for the selected provider.",
                        "Leave blank for local servers such as Ollama.",
                        "DO NOT share this value."
                )
                .define(
                        "apikey",
                        ""
                );

        MODEL = builder
                .comment(
                        "Model name.",
                        "This is free-form so Ferrity does not need an update",
                        "every time a provider adds a model."
                )
                .define(
                        "model",
                        ""
                );

        BASE_URL = builder
                .comment(
                        "Custom server/API base URL.",
                        "Mainly used for Ollama and OpenAI-compatible servers.",
                        "Leave blank to use Ferrity's default for Gemini/Groq."
                )
                .define(
                        "baseurl",
                        ""
                );

        SPEC = builder.build();
    }

    public static String provider() {
        return PROVIDER.get().trim().toLowerCase();
    }

    public static String apiKey() {
        return API_KEY.get().trim();
    }

    public static String model() {
        return MODEL.get().trim();
    }

    public static String baseUrl() {
        return BASE_URL.get().trim();
    }

    private Ferrityconfig() {
    }
}