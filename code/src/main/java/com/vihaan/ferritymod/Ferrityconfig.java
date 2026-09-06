package com.vihaan.ferritymod;

import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Ferrityconfig {

    /*
     * CLIENT CONFIG
     *
     * Used on the Minecraft client and therefore
     * also by an integrated singleplayer server.
     *
     * This is the config exposed by the config menu.
     */
    public static final ModConfigSpec CLIENT_SPEC;

    public static final ModConfigSpec.ConfigValue<String>
            CLIENT_PROVIDER;

    public static final ModConfigSpec.ConfigValue<String>
            CLIENT_API_KEY;

    public static final ModConfigSpec.ConfigValue<String>
            CLIENT_MODEL;

    public static final ModConfigSpec.ConfigValue<String>
            CLIENT_BASE_URL;


    /*
     * COMMON CONFIG
     *
     * Used by a dedicated Minecraft server.
     *
     * The dedicated server keeps its own provider,
     * API key, model and base URL.
     */
    public static final ModConfigSpec COMMON_SPEC;

    public static final ModConfigSpec.ConfigValue<String>
            COMMON_PROVIDER;

    public static final ModConfigSpec.ConfigValue<String>
            COMMON_API_KEY;

    public static final ModConfigSpec.ConfigValue<String>
            COMMON_MODEL;

    public static final ModConfigSpec.ConfigValue<String>
            COMMON_BASE_URL;


    static {

        /*
         * =========================================
         * CLIENT CONFIG
         * =========================================
         */

        ModConfigSpec.Builder clientBuilder =
                new ModConfigSpec.Builder();

        clientBuilder.comment(
                "Ferrity AI configuration",
                "",
                "Used for singleplayer.",
                "",
                "Supported providers:",
                "gemini",
                "groq",
                "ollama",
                "openai-compatible"
        );


        CLIENT_PROVIDER =
                clientBuilder
                        .comment(
                                "Which AI provider Ferrity should use.",
                                "gemini, groq, ollama, or openai-compatible"
                        )
                        .define(
                                "provider",
                                "gemini"
                        );


        CLIENT_API_KEY =
                clientBuilder
                        .comment(
                                "API key for the selected provider.",
                                "Leave blank for local servers such as Ollama.",
                                "DO NOT share this value."
                        )
                        .define(
                                "apikey",
                                ""
                        );


        CLIENT_MODEL =
                clientBuilder
                        .comment(
                                "Model name.",
                                "This is free-form so Ferrity does not need an update",
                                "every time a provider adds a model."
                        )
                        .define(
                                "model",
                                ""
                        );


        CLIENT_BASE_URL =
                clientBuilder
                        .comment(
                                "Custom server/API base URL.",
                                "Mainly used for Ollama and OpenAI-compatible servers.",
                                "Leave blank to use Ferrity's default for Gemini/Groq."
                        )
                        .define(
                                "baseurl",
                                ""
                        );


        CLIENT_SPEC =
                clientBuilder.build();


        /*
         * =========================================
         * COMMON / DEDICATED SERVER CONFIG
         * =========================================
         */

        ModConfigSpec.Builder commonBuilder =
                new ModConfigSpec.Builder();

        commonBuilder.comment(
                "Ferrity dedicated server AI configuration",
                "",
                "Used when Ferrity is running on a dedicated server.",
                "",
                "Supported providers:",
                "gemini",
                "groq",
                "ollama",
                "openai-compatible"
        );


        COMMON_PROVIDER =
                commonBuilder
                        .comment(
                                "Which AI provider Ferrity should use.",
                                "gemini, groq, ollama, or openai-compatible"
                        )
                        .define(
                                "provider",
                                "gemini"
                        );


        COMMON_API_KEY =
                commonBuilder
                        .comment(
                                "API key for the selected provider.",
                                "Leave blank for local servers such as Ollama.",
                                "DO NOT share this value."
                        )
                        .define(
                                "apikey",
                                ""
                        );


        COMMON_MODEL =
                commonBuilder
                        .comment(
                                "Model name.",
                                "This is free-form so Ferrity does not need an update",
                                "every time a provider adds a model."
                        )
                        .define(
                                "model",
                                ""
                        );


        COMMON_BASE_URL =
                commonBuilder
                        .comment(
                                "Custom server/API base URL.",
                                "Mainly used for Ollama and OpenAI-compatible servers.",
                                "Leave blank to use Ferrity's default for Gemini/Groq."
                        )
                        .define(
                                "baseurl",
                                ""
                        );


        COMMON_SPEC =
                commonBuilder.build();
    }


    /*
     * A dedicated server runs on the SERVER physical side.
     *
     * Singleplayer's integrated server lives inside
     * the CLIENT process, so it uses the CLIENT config.
     */
    private static boolean useServerConfig() {
        return !FMLEnvironment
                .getDist()
                .isClient();
    }


    /*
     * Keep the ORIGINAL no-argument API.
     *
     * This means FerrityAI does not need to be rewritten
     * in ten different places.
     */

    public static String provider() {

        if (useServerConfig()) {
            return COMMON_PROVIDER
                    .get()
                    .trim()
                    .toLowerCase();
        }

        return CLIENT_PROVIDER
                .get()
                .trim()
                .toLowerCase();
    }


    public static String apiKey() {

        if (useServerConfig()) {
            return COMMON_API_KEY
                    .get()
                    .trim();
        }

        return CLIENT_API_KEY
                .get()
                .trim();
    }


    public static String model() {

        if (useServerConfig()) {
            return COMMON_MODEL
                    .get()
                    .trim();
        }

        return CLIENT_MODEL
                .get()
                .trim();
    }


    public static String baseUrl() {

        if (useServerConfig()) {
            return COMMON_BASE_URL
                    .get()
                    .trim();
        }

        return CLIENT_BASE_URL
                .get()
                .trim();
    }


    private Ferrityconfig() {
    }
}