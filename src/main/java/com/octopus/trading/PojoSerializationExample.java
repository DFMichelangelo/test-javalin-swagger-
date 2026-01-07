package com.octopus.trading;

import com.google.protobuf.InvalidProtocolBufferException;
import com.octopus.model.CatPojo;
import com.octopus.model.DogPojo;
import com.octopus.model.BirdPojo;
import com.octopus.model.FishPojo;
import com.octopus.util.ProtoSerializer;
import com.octopus.util.converter.CatConverter;
import com.octopus.util.converter.DogConverter;
import com.octopus.util.converter.BirdConverter;
import com.octopus.util.converter.FishConverter;

import java.util.ArrayList;
import java.util.List;

/**
 * Demonstrates serialization/deserialization using POJOs (Plain Old Java Objects).
 * The protobuf layer is completely hidden - you work with plain Java objects!
 */
public class PojoSerializationExample {

    public static void main(String[] args) throws InvalidProtocolBufferException {

        System.out.println("=== POJO-based Serialization ===\n");

        // Step 1: Register converters
        System.out.println("Step 1: Registering converters...");
        ProtoSerializer.registerConverter(new CatConverter());
        ProtoSerializer.registerConverter(new DogConverter());
        ProtoSerializer.registerConverter(new BirdConverter());
        ProtoSerializer.registerConverter(new FishConverter());
        System.out.println("Registered " + ProtoSerializer.getRegistrySize() + " converters\n");

        // Step 2: Create POJOs (Plain Old Java Objects)
        System.out.println("Step 2: Creating POJOs...\n");

        CatPojo cat = new CatPojo("Luna", "Gray", 2, true);
        System.out.println("Created: " + cat);

        DogPojo dog = new DogPojo("Max", "Golden Retriever", 3, true);
        System.out.println("Created: " + dog);

        BirdPojo bird = new BirdPojo("Tweety", "Canary", true, 15.5);
        System.out.println("Created: " + bird);

        FishPojo fish = new FishPojo("Nemo", "Clownfish", 8.5, "SALT");
        System.out.println("Created: " + fish);
        System.out.println();

        // Step 3: Serialize POJOs to byte arrays
        System.out.println("Step 3: Serializing POJOs...");
        byte[] catBytes = ProtoSerializer.serialize(cat);
        byte[] dogBytes = ProtoSerializer.serialize(dog);
        byte[] birdBytes = ProtoSerializer.serialize(bird);
        byte[] fishBytes = ProtoSerializer.serialize(fish);
        System.out.println("Serialized 4 POJOs to byte arrays\n");

        // Step 4: Store all byte arrays together
        System.out.println("Step 4: Storing byte arrays (simulating network transmission)...");
        List<byte[]> transmittedData = new ArrayList<>();
        transmittedData.add(catBytes);
        transmittedData.add(dogBytes);
        transmittedData.add(birdBytes);
        transmittedData.add(fishBytes);
        System.out.println("Stored " + transmittedData.size() + " byte arrays\n");

        // Step 5: Deserialize back to POJOs WITHOUT knowing the type!
        System.out.println("Step 5: Deserializing back to POJOs...\n");
        System.out.println("GenericWrapper's oneof field tells us the type:\n");

        for (int i = 0; i < transmittedData.size(); i++) {
            byte[] data = transmittedData.get(i);

            System.out.println("Data " + (i + 1) + ":");

            // Deserialize - returns the actual POJO!
            Object pojo = ProtoSerializer.deserialize(data);
            System.out.println("  Deserialized as: " + pojo.getClass().getSimpleName());
            System.out.println("  Content: " + pojo);
            System.out.println();
        }

        // Step 6: Type-safe extraction
        System.out.println("=== Step 6: Type-safe extraction ===\n");

        Object unknownPojo = ProtoSerializer.deserialize(catBytes);
        if (unknownPojo instanceof CatPojo c) {
            System.out.println("Successfully extracted as CatPojo:");
            System.out.println("  Name: " + c.getName());
            System.out.println("  Color: " + c.getColor());
            System.out.println("  Age: " + c.getAge());
            System.out.println("  Indoor: " + c.isIndoor());
        }
        System.out.println();

        // Step 7: Demonstrate type checking
        System.out.println("=== Step 7: Type checking ===\n");

        Object dogPojo = ProtoSerializer.deserialize(dogBytes);
        if (dogPojo instanceof DogPojo d) {
            System.out.println("Correctly identified as DogPojo: " + d.getName());
        } else {
            System.out.println("Not a DogPojo");
        }

        if (dogPojo instanceof CatPojo) {
            System.out.println("Identified as CatPojo");
        } else {
            System.out.println("Not a CatPojo (as expected)");
        }
        System.out.println();

        // Step 8: Show the benefit
        System.out.println("=== Step 8: Benefits ===\n");
        System.out.println("✓ Work with plain Java POJOs - no protobuf knowledge needed!");
        System.out.println("✓ Protobuf is just the wire format - hidden from business logic");
        System.out.println("✓ Type-safe serialization/deserialization");
        System.out.println("✓ Automatic type detection via GenericWrapper's oneof field");
        System.out.println("✓ Only 1 byte overhead (oneof tag)");
    }
}
