package com.wentory.ransom_in_minecraft.client;
final class CrucifixIrisCompatibility {
    private static Object api;
    private static java.lang.reflect.Method inUse;
    private static boolean unavailable;
    private static final boolean INSTALLED=ShaderLoaderPresence.installed();
    static {
        if(INSTALLED)try {
            Class<?> type=Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            api=type.getMethod("getInstance").invoke(null);inUse=type.getMethod("isShaderPackInUse");
        }catch(ReflectiveOperationException|RuntimeException|LinkageError error){unavailable=true;}
    }
    static boolean useCompatibleRenderer(){
        if(!INSTALLED)return false;
        if(unavailable)return true;
        try{return Boolean.TRUE.equals(inUse.invoke(api));}
        catch(ReflectiveOperationException|RuntimeException|LinkageError error){unavailable=true;return true;}
    }
    private CrucifixIrisCompatibility(){}
}
