package com.vihaan.ferritymod;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;

public class FerrityMemory extends SavedData {

    public record Message(
            String role,
            String text
    ) {
        public static final Codec<Message> CODEC =
                RecordCodecBuilder.create(instance ->
                        instance.group(
                                Codec.STRING
                                        .fieldOf("role")
                                        .forGetter(Message::role),

                                Codec.STRING
                                        .fieldOf("text")
                                        .forGetter(Message::text)
                        ).apply(instance, Message::new)
                );
    }

    private final List<Message> messages;

    public FerrityMemory() {
        this(new ArrayList<>());
    }

    public FerrityMemory(List<Message> messages) {
        this.messages = new ArrayList<>(messages);
    }

    public static final Codec<FerrityMemory> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(
                            Message.CODEC
                                    .listOf()
                                    .optionalFieldOf(
                                            "messages",
                                            List.of()
                                    )
                                    .forGetter(
                                            memory -> memory.messages
                                    )
                    ).apply(
                            instance,
                            FerrityMemory::new
                    )
            );

    public static final SavedDataType<FerrityMemory> TYPE =
            new SavedDataType<>(
                    Identifier.fromNamespaceAndPath(
                            "ferritymod",
                            "ferrity_memory"
                    ),
                    FerrityMemory::new,
                    CODEC,
                    null
            );

    public static FerrityMemory get(
            MinecraftServer server
    ) {
        return server
                .getDataStorage()
                .computeIfAbsent(TYPE);
    }

    public void addPlayerMessage(String text) {
        messages.add(
                new Message(
                        "user",
                        text
                )
        );

        setDirty();
    }

    public void addFerrityMessage(String text) {
        messages.add(
                new Message(
                        "model",
                        text
                )
        );

        setDirty();
    }

    public List<Message> getMessages() {
        return List.copyOf(messages);
    }

    public int size() {
        return messages.size();
    }
}