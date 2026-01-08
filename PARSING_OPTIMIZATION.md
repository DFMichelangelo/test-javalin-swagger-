# Parsing Optimization - Eliminating Duplicate Parsing

## Problem

The original implementation parsed protobuf data **twice** during deserialization:

1. **In `ProtoSerializer.deserialize()`**: Parsed wrapper to determine message type
   ```java
   GenericWrapper wrapper = GenericWrapper.parseFrom(data);
   ```

2. **In each converter's `deserialize()`**: Parsed wrapper again to extract message
   ```java
   GenericWrapper wrapper = GenericWrapper.parseFrom(data);  // DUPLICATE!
   ```

This was inefficient and violated the DRY (Don't Repeat Yourself) principle.

## Solution

Refactored the architecture to parse protobuf data **only once**:

### 1. Changed `ProtobufConverter` Interface

**Before:**
```java
byte[] serialize(P pojo);           // POJO -> bytes (handled wrapping)
P deserialize(byte[] data);         // bytes -> POJO (handled unwrapping)
```

**After:**
```java
M toMessage(P pojo);                // POJO -> Message (no wrapper handling)
P fromMessage(M message);           // Message -> POJO (no wrapper handling)
```

**Benefit**: Converters now only handle POJO ↔ Message conversion, not wrapper logic.

### 2. Added Wrapper Handling to `WrapperParser`

Extended `WrapperParser` interface with:

```java
Message getPayloadMessage(W wrapper);     // Extract message from wrapper
<M extends Message> W wrapMessage(M message);  // Wrap message in wrapper
```

### 3. Refactored `ProtoSerializer`

**Serialize path:**
```java
1. converter.toMessage(pojo)          // POJO → Message
2. wrapperParser.wrapMessage(message) // Message → Wrapper
3. wrapper.toByteArray()              // Wrapper → bytes
```

**Deserialize path:**
```java
1. wrapperParser.parseWrapper(data)       // bytes → Wrapper (ONCE!)
2. wrapperParser.getPayloadMessage(wrapper) // Wrapper → Message (no parsing!)
3. converter.fromMessage(message)         // Message → POJO
```

## Performance Impact

- **Before**: 2 protobuf parse operations per deserialization
- **After**: 1 protobuf parse operation per deserialization
- **Improvement**: **50% reduction** in parsing overhead

## Separation of Concerns

The refactoring also improved code organization:

| Component | Responsibility |
|-----------|---------------|
| **ProtoSerializer** | Orchestrates serialization/deserialization, manages converters |
| **WrapperParser** | Handles wrapper-specific logic (parsing, wrapping, extraction) |
| **ProtobufConverter** | Handles POJO ↔ Message conversion only |

Each component now has a single, well-defined responsibility.

## Example

### Before Refactoring

```java
// DogConverter.deserialize() - INEFFICIENT
GenericWrapper wrapper = GenericWrapper.parseFrom(data);  // Parse #1 in ProtoSerializer
                                                          // Parse #2 in converter!
Dog dog = wrapper.getDog();
return convertToP OJO(dog);
```

### After Refactoring

```java
// ProtoSerializer.deserialize() - EFFICIENT
W wrapper = wrapperParser.parseWrapper(data);            // Parse ONCE
Message message = wrapperParser.getPayloadMessage(wrapper); // Extract (no parsing)
return converter.fromMessage(message);                   // Convert to POJO

// DogConverter.fromMessage() - CLEAN
return convertToPojo(message);                           // No parsing needed!
```

## Benefits Summary

1. ✅ **Performance**: 50% reduction in protobuf parsing
2. ✅ **Code Quality**: Better separation of concerns
3. ✅ **Maintainability**: Wrapper logic centralized in `WrapperParser`
4. ✅ **Testability**: Each component can be tested independently
5. ✅ **Simplicity**: Converters are simpler (no wrapper handling)
