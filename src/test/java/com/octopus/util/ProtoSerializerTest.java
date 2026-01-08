package com.octopus.util;

import com.google.protobuf.InvalidProtocolBufferException;
import com.octopus.model.DogPojo;
import com.octopus.model.CatPojo;
import com.octopus.proto.AnimalProto.GenericWrapper;
import com.octopus.util.converter.DogConverter;
import com.octopus.util.converter.CatConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProtoSerializerTest {

    private ProtoSerializer<GenericWrapper> serializer;

    @BeforeEach
    void setUp() {
        // Create serializer with AnimalWrapperParser
        serializer = new ProtoSerializer<>(new AnimalWrapperParser());

        // Register converters
        serializer.registerConverter(new DogConverter());
        serializer.registerConverter(new CatConverter());
    }

    @Test
    void testSerializeAndDeserialize() throws InvalidProtocolBufferException {
        // Create a dog POJO
        DogPojo dog = new DogPojo("Max", "Golden Retriever", 3, true);

        // Serialize
        byte[] data = serializer.serialize(dog);
        assertNotNull(data);
        assertTrue(data.length > 0);

        // Deserialize - type automatically detected
        Object result = serializer.deserialize(data);
        assertNotNull(result);
        assertTrue(result instanceof DogPojo);

        DogPojo deserializedDog = (DogPojo) result;
        assertEquals("Max", deserializedDog.getName());
        assertEquals("Golden Retriever", deserializedDog.getBreed());
        assertEquals(3, deserializedDog.getAge());
        assertTrue(deserializedDog.isTrained());
    }

    @Test
    void testMultipleTypes() throws InvalidProtocolBufferException {
        // Create POJOs
        DogPojo dog = new DogPojo("Max", "Golden Retriever", 3, true);
        CatPojo cat = new CatPojo("Luna", "Gray", 2, true);

        // Serialize both
        byte[] dogBytes = serializer.serialize(dog);
        byte[] catBytes = serializer.serialize(cat);

        // Deserialize both - types automatically detected
        Object deserializedDogObj = serializer.deserialize(dogBytes);
        Object deserializedCatObj = serializer.deserialize(catBytes);

        // Verify types
        assertTrue(deserializedDogObj instanceof DogPojo);
        assertTrue(deserializedCatObj instanceof CatPojo);

        // Verify content
        assertEquals("Max", ((DogPojo) deserializedDogObj).getName());
        assertEquals("Luna", ((CatPojo) deserializedCatObj).getName());
    }

    @Test
    void testRegistrySize() {
        assertEquals(2, serializer.getRegistrySize());
    }

    @Test
    void testIsConverterRegistered() {
        assertTrue(serializer.isConverterRegistered(DogPojo.class));
        assertTrue(serializer.isConverterRegistered(CatPojo.class));
    }

    @Test
    void testSerializeUnregisteredType() {
        // Try to serialize a type that hasn't been registered
        Object unknownObject = new Object();

        assertThrows(IllegalArgumentException.class, () -> {
            serializer.serialize(unknownObject);
        });
    }

    @Test
    void testTypeDetection() throws InvalidProtocolBufferException {
        // Serialize a dog
        DogPojo dog = new DogPojo("Max", "Golden Retriever", 3, true);
        byte[] data = serializer.serialize(dog);

        // Deserialize and verify type is correctly detected
        Object result = serializer.deserialize(data);
        assertTrue(result instanceof DogPojo);
        assertFalse(result instanceof CatPojo);
    }

    @Test
    void testAutomaticDeserialization() throws InvalidProtocolBufferException {
        // Create and serialize a cat
        CatPojo cat = new CatPojo("Luna", "Gray", 2, true);
        byte[] data = serializer.serialize(cat);

        // Automatic type detection
        Object result = serializer.deserialize(data);

        // Verify type and content
        assertNotNull(result);
        assertTrue(result instanceof CatPojo);

        CatPojo deserializedCat = (CatPojo) result;
        assertEquals("Luna", deserializedCat.getName());
        assertEquals("Gray", deserializedCat.getColor());
        assertEquals(2, deserializedCat.getAge());
        assertTrue(deserializedCat.isIndoor());
    }
}
