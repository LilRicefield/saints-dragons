package com.leon.saintsdragons.common.config.dragon.profile;

final class StatProfileConfigSupport {
    private StatProfileConfigSupport() {
    }

    static double forgeDouble(Class<?> configClass, String fieldName) throws ReflectiveOperationException {
        Object holder = configClass.getField(fieldName).get(null);
        Object value = holder.getClass().getMethod("get").invoke(holder);
        return ((Number) value).doubleValue();
    }

    static boolean forgeBoolean(Class<?> configClass, String fieldName) throws ReflectiveOperationException {
        Object holder = configClass.getField(fieldName).get(null);
        return (boolean) holder.getClass().getMethod("get").invoke(holder);
    }
}
