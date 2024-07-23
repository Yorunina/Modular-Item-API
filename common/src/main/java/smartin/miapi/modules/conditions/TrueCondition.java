package smartin.miapi.modules.conditions;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public class TrueCondition implements ModuleCondition {
    public static Codec<TrueCondition> CODEC = new Codec<TrueCondition>() {
        @Override
        public <T> DataResult<Pair<TrueCondition, T>> decode(DynamicOps<T> ops, T input) {
            return DataResult.success(new Pair(new TrueCondition(), input));
        }

        @Override
        public <T> DataResult<T> encode(TrueCondition input, DynamicOps<T> ops, T prefix) {
            return DataResult.error(() -> "encoding condition is not fully supported");
        }
    };

    @Override
    public boolean isAllowed(ConditionManager.ConditionContext conditionContext) {
        return true;
    }
}
