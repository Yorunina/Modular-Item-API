package smartin.miapi.modules.properties.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.redpxnda.nucleus.codec.auto.AutoCodec;
import net.minecraft.world.item.ItemStack;
import smartin.miapi.modules.ModuleInstance;
import smartin.miapi.modules.cache.ModularItemCache;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class DoubleProperty extends CodecBasedProperty<DoubleOperationResolvable> {
    public DoubleProperty property;
    public double baseValue = 0;
    public boolean allowVisualOnly = false;
    private final String cacheKey;
    static Codec<List<DoubleOperationResolvable.Operation>> listCodec = Codec.list(AutoCodec.of(DoubleOperationResolvable.Operation.class).codec());
    public static Codec<List<DoubleOperationResolvable.Operation>> CODEC = Codec.withAlternative(
            new Codec<>() {
                @Override
                public <T> DataResult<T> encode(List<DoubleOperationResolvable.Operation> input, DynamicOps<T> ops, T prefix) {
                    return listCodec.encode(input, ops, prefix);
                }

                @Override
                public <T> DataResult<Pair<List<DoubleOperationResolvable.Operation>, T>> decode(DynamicOps<T> ops, T input) {
                    Pair<String, T> stringTPair = Codec.STRING.decode(ops, input).getOrThrow();
                    List<DoubleOperationResolvable.Operation> operations = List.of(new DoubleOperationResolvable.Operation(stringTPair.getFirst()));
                    return DataResult.success(new Pair<>(operations, stringTPair.getSecond()));
                }
            },
            listCodec);

    protected DoubleProperty(String cacheKey) {
        super(DoubleOperationResolvable.CODEC);
        this.cacheKey = cacheKey + "_internal_double";
        property = this;
        ModularItemCache.setSupplier(cacheKey, (itemStack) ->
                Optional.ofNullable(getData(itemStack).evaluate(baseValue)));
    }

    public DoubleOperationResolvable initialize(DoubleOperationResolvable property, ModuleInstance context) {
        return property.initialize(context);
    }

    public Optional<Double> getValue(ItemStack itemStack) {
        return ModularItemCache.get(itemStack, cacheKey, Optional.empty());
    }

    public double getForItems(Iterable<ItemStack> itemStacks) {
        List<DoubleOperationResolvable.Operation> operations = new ArrayList<>();
        itemStacks.forEach(itemStack -> operations.addAll(getData(itemStack).operations));
        return DoubleOperationResolvable.resolve(operations, baseValue, baseValue);
    }

    public DoubleOperationResolvable merge(DoubleOperationResolvable left, DoubleOperationResolvable right, MergeType mergeType) {
        return left.merge(right, mergeType);
    }

}
