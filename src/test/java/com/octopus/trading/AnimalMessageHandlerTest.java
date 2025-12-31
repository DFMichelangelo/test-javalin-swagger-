package com.octopus.trading;

import com.google.protobuf.InvalidProtocolBufferException;
import com.octopus.proto.AnimalProto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnimalMessageHandlerTest {

    private AnimalMessageHandler handler;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() {
        handler = new AnimalMessageHandler();
        outputStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
    }

    @Test
    void shouldHandleDogMessage() throws InvalidProtocolBufferException {
        // Given
        AnimalMessage message = AnimalMessage.newBuilder()
                .setTimestamp(System.currentTimeMillis())
                .setDog(Dog.newBuilder()
                        .setName("Max")
                        .setBreed("Golden Retriever")
                        .setAge(3)
                        .setIsTrained(true)
                        .build())
                .build();

        // When
        handler.handleMessage(message.toByteArray());

        // Then
        String output = outputStream.toString();
        assertThat(output).contains("Dog");
        assertThat(output).contains("Max");
        assertThat(output).contains("Golden Retriever");
        assertThat(output).contains("Yes");
    }

    @Test
    void shouldHandleCatMessage() throws InvalidProtocolBufferException {
        // Given
        AnimalMessage message = AnimalMessage.newBuilder()
                .setTimestamp(System.currentTimeMillis())
                .setCat(Cat.newBuilder()
                        .setName("Luna")
                        .setColor("Gray")
                        .setAge(2)
                        .setIsIndoor(true)
                        .build())
                .build();

        // When
        handler.handleMessage(message.toByteArray());

        // Then
        String output = outputStream.toString();
        assertThat(output).contains("Cat");
        assertThat(output).contains("Luna");
        assertThat(output).contains("Gray");
        assertThat(output).contains("Yes");
    }

    @Test
    void shouldHandleBirdMessage() throws InvalidProtocolBufferException {
        // Given
        AnimalMessage message = AnimalMessage.newBuilder()
                .setTimestamp(System.currentTimeMillis())
                .setBird(Bird.newBuilder()
                        .setName("Tweety")
                        .setSpecies("Canary")
                        .setCanFly(true)
                        .setWingspanCm(15.5)
                        .build())
                .build();

        // When
        handler.handleMessage(message.toByteArray());

        // Then
        String output = outputStream.toString();
        assertThat(output).contains("Bird");
        assertThat(output).contains("Tweety");
        assertThat(output).contains("Canary");
        assertThat(output).contains("15.5");
    }

    @Test
    void shouldHandleFishMessage() throws InvalidProtocolBufferException {
        // Given
        AnimalMessage message = AnimalMessage.newBuilder()
                .setTimestamp(System.currentTimeMillis())
                .setFish(Fish.newBuilder()
                        .setName("Nemo")
                        .setType("Clownfish")
                        .setLengthCm(8.5)
                        .setWaterType("SALT")
                        .build())
                .build();

        // When
        handler.handleMessage(message.toByteArray());

        // Then
        String output = outputStream.toString();
        assertThat(output).contains("Fish");
        assertThat(output).contains("Nemo");
        assertThat(output).contains("Clownfish");
        assertThat(output).contains("SALT");
    }

    @Test
    void shouldThrowExceptionForEmptyMessage() {
        // Given
        AnimalMessage message = AnimalMessage.newBuilder()
                .setTimestamp(System.currentTimeMillis())
                .build();

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            handler.handleMessage(message.toByteArray());
        });
    }

    @Test
    void shouldCheckMessageType() throws InvalidProtocolBufferException {
        // Given - Dog message
        AnimalMessage dogMsg = AnimalMessage.newBuilder()
                .setDog(Dog.newBuilder()
                        .setName("Max")
                        .setBreed("Golden Retriever")
                        .setAge(3)
                        .build())
                .build();

        // When
        AnimalMessage parsed = AnimalMessage.parseFrom(dogMsg.toByteArray());

        // Then
        assertThat(parsed.getAnimalTypeCase()).isEqualTo(AnimalMessage.AnimalTypeCase.DOG);
        assertThat(parsed.hasDog()).isTrue();
        assertThat(parsed.hasCat()).isFalse();
        assertThat(parsed.hasBird()).isFalse();
        assertThat(parsed.hasFish()).isFalse();
    }
}
