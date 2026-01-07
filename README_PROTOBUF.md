# Protobuf Auto-Serialization with Custom Converters

Simple protobuf serialization/deserialization with automatic type detection using the **`oneof`** field in `GenericWrapper`.

**No external headers, no type IDs** - uses protobuf's native `oneof` mechanism to determine message type!

## Format

```
Just GenericWrapper bytes - the oneof field contains the type information
```

## How It Works

Uses `GenericWrapper` from your proto file:

```protobuf
message GenericWrapper {
  oneof payload {
    Dog dog = 1;
    Cat cat = 2;
    Bird bird = 3;
    Fish fish = 4;
    AnimalMessage animal_message = 5;
  }
}
```

The `oneof` field acts as the **discriminator** - protobuf knows which field is set, so we can determine the message type!

## Quick Start

### 1. Create Converters

Converters use protocol builders to wrap messages in GenericWrapper:

```java
public class DogConverter implements ProtobufConverter<Dog> {
    @Override
    public byte[] serialize(Dog message) {
        // Use protocol builder to wrap in GenericWrapper
        GenericWrapper wrapper = GenericWrapper.newBuilder()
                .setDog(message)
                .build();
        return wrapper.toByteArray();
    }

    @Override
    public Dog deserialize(byte[] data) throws InvalidProtocolBufferException {
        // Parse GenericWrapper and extract Dog
        GenericWrapper wrapper = GenericWrapper.parseFrom(data);
        return wrapper.getDog();
    }

    @Override
    public Class<Dog> getMessageClass() {
        return Dog.class;
    }
}
```

### 2. Register Converters

```java
ProtoSerializer.registerConverter(new DogConverter());
ProtoSerializer.registerConverter(new CatConverter());
ProtoSerializer.registerConverter(new BirdConverter());
ProtoSerializer.registerConverter(new FishConverter());
```

### 3. Serialize

```java
Dog dog = Dog.newBuilder()
    .setName("Max")
    .setBreed("Golden Retriever")
    .build();

// Wraps in GenericWrapper, serializes it
byte[] data = ProtoSerializer.serialize(dog);
```

### 4. Deserialize

```java
// Parses GenericWrapper, checks oneof field, extracts message!
Message message = ProtoSerializer.deserialize(data);

// Type-safe extraction
if (message instanceof Dog d) {
    System.out.println("Dog: " + d.getName());
}
```

## How Serialization Works

1. Takes your message (e.g., `Dog`)
2. Finds the registered converter
3. Converter uses protocol builder to wrap: `GenericWrapper.newBuilder().setDog(dog).build()`
4. Serializes the `GenericWrapper`
5. Returns the bytes

The `oneof` field automatically records which type was set!

## How Deserialization Works

1. Parses bytes as `GenericWrapper`
2. Checks `wrapper.getPayloadCase()` to see which `oneof` field is set
   - `DOG` → extracts `wrapper.getDog()`
   - `CAT` → extracts `wrapper.getCat()`
   - etc.
3. Returns the extracted message

No guessing, no headers - the `oneof` field IS the type discriminator!

## Benefits

✅ **Pure Protobuf** - Uses protobuf's native `oneof` mechanism
✅ **Protocol Builders** - Converters use generated protocol builders for type safety
✅ **No External Headers** - Type info is part of the message structure
✅ **Type Safe** - Protobuf validates the oneof field
✅ **Efficient** - Minimal overhead (just the oneof tag - 1 byte)
✅ **Auto-Detection** - No need to specify type when deserializing
✅ **Extensible** - Add new types to `GenericWrapper` proto

## API

### ProtobufConverter Interface

```java
interface ProtobufConverter<T extends Message> {
    byte[] serialize(T message);
    T deserialize(byte[] data) throws InvalidProtocolBufferException;
    Class<T> getMessageClass();
}
```

### ProtoSerializer Methods

- **`registerConverter(ProtobufConverter<T>)`** - Register a converter
- **`serialize(Message)`** - Wraps in GenericWrapper + serializes
- **`deserialize(byte[])`** - Parses GenericWrapper + extracts message
- **`isConverterRegistered(Class<?>)`** - Check if converter is registered
- **`getConverter(Class<T>)`** - Get the registered converter
- **`clearRegistry()`** - Clear all converters
- **`getRegistrySize()`** - Get number of registered converters

## Adding New Message Types

### 1. Update `animals.proto`:

```protobuf
message GenericWrapper {
  oneof payload {
    Dog dog = 1;
    Cat cat = 2;
    Bird bird = 3;
    Fish fish = 4;
    AnimalMessage animal_message = 5;
    Rabbit rabbit = 6;  // Add here
  }
}

message Rabbit {
  string name = 1;
  int32 hop_height = 2;
}
```

### 2. Rebuild proto classes:

```bash
./gradlew build
```

### 3. Update `ProtoSerializer.deserialize()` switch statement:

```java
case RABBIT -> wrapper.getRabbit();
```

### 4. Create a converter:

```java
public class RabbitConverter implements ProtobufConverter<Rabbit> {
    @Override
    public byte[] serialize(Rabbit message) {
        return GenericWrapper.newBuilder().setRabbit(message).build().toByteArray();
    }

    @Override
    public Rabbit deserialize(byte[] data) throws InvalidProtocolBufferException {
        GenericWrapper wrapper = GenericWrapper.parseFrom(data);
        return wrapper.getRabbit();
    }

    @Override
    public Class<Rabbit> getMessageClass() {
        return Rabbit.class;
    }
}
```

### 5. Register the converter:

```java
ProtoSerializer.registerConverter(new RabbitConverter());
```

## Example Converters

See these files:
- `src/main/java/com/octopus/util/converter/DogConverter.java`
- `src/main/java/com/octopus/util/converter/CatConverter.java`
- `src/main/java/com/octopus/util/converter/BirdConverter.java`
- `src/main/java/com/octopus/util/converter/FishConverter.java`

Each converter demonstrates using protocol builders to wrap messages in GenericWrapper.

## Example Application

```bash
./gradlew installDist
java -cp "build/install/test-correlation-id/lib/*" com.octopus.trading.AutoDeserializationExample
```

## Use Cases

### Custom Validation

```java
public class DogConverter implements ProtobufConverter<Dog> {
    @Override
    public byte[] serialize(Dog message) {
        if (message.getName().isEmpty()) {
            throw new IllegalArgumentException("Dog must have a name");
        }
        return GenericWrapper.newBuilder().setDog(message).build().toByteArray();
    }
}
```

### Logging/Metrics

```java
public class DogConverter implements ProtobufConverter<Dog> {
    @Override
    public byte[] serialize(Dog message) {
        logger.info("Serializing dog: {}", message.getName());
        metrics.increment("dog.serialized");
        return GenericWrapper.newBuilder().setDog(message).build().toByteArray();
    }
}
```

### Versioning/Migration

```java
public class DogConverter implements ProtobufConverter<Dog> {
    @Override
    public Dog deserialize(byte[] data) throws InvalidProtocolBufferException {
        GenericWrapper wrapper = GenericWrapper.parseFrom(data);
        Dog dog = wrapper.getDog();

        // Handle version migration if needed
        if (dog.getVersion() < 2) {
            return migrateToV2(dog);
        }
        return dog;
    }
}
```

## Files

- **Interface**: `src/main/java/com/octopus/util/ProtobufConverter.java`
- **Serializer**: `src/main/java/com/octopus/util/ProtoSerializer.java`
- **Converters**: `src/main/java/com/octopus/util/converter/`
- **Example**: `src/main/java/com/octopus/trading/AutoDeserializationExample.java`
- **Proto**: `src/main/proto/animals.proto`

## Overhead

- **Message size**: Only 1 byte overhead (oneof tag)
- **Performance**: Native protobuf parsing
- **Thread safety**: Thread-safe for reads, register converters at startup

## Architecture

```
Serialize:
  Message → Converter → GenericWrapper.setXxx(message) → serialize GenericWrapper

Deserialize:
  bytes → GenericWrapper.parseFrom() → check oneof → extract message

The oneof field IS the discriminator - built into protobuf!
```

## Why Protocol Builders?

**Type-Safe**: Protocol builders (GenericWrapper.newBuilder()) provide compile-time type safety.

**Generated Code**: Leverages protobuf's generated builders for correct wrapping.

**Maintainable**: Converters explicitly show how each type wraps into GenericWrapper.

**Extensible**: Easy to add custom logic (validation, logging, migration) in converters.

Pure protobuf solution - no external type IDs, no headers, just the native `oneof` mechanism with protocol builders!
