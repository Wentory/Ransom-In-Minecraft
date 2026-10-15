package com.wentory.ransom_in_minecraft.client;

import net.neoforged.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Method;

/** API failure selects the safe renderer; it does not mean a shader pack is active. */
final class CrucifixIrisCompatibility {
    private static final Logger LOGGER = LoggerFactory.getLogger(CrucifixIrisCompatibility.class);
    private static final boolean IRIS_INSTALLED = ModList.get().isLoaded("iris");
    private static Object api;
    private static Method inUse;
    private static boolean queryFailed;

    static {
        if (IRIS_INSTALLED) {
            try {
                Class<?> type = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                api = type.getMethod("getInstance").invoke(null);
                inUse = type.getMethod("isShaderPackInUse");
            } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
                markUnavailable(failure);
            }
        }
    }

    private CrucifixIrisCompatibility() {}

    static boolean useCompatibleRenderer() {
        if (!IRIS_INSTALLED) return false;
        if (queryFailed) return true;
        try {
            return Boolean.TRUE.equals(inUse.invoke(api));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError failure) {
            markUnavailable(failure);
            return true;
        }
    }

    private static void markUnavailable(Throwable failure) {
        if (!queryFailed) LOGGER.warn("Cannot query Iris API; using compatible Crucifix renderer", failure);
        queryFailed = true;
    }
}
