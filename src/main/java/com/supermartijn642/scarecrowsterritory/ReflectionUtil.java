package com.supermartijn642.scarecrowsterritory;

import net.minecraft.world.spawner.AbstractSpawner;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created 1/14/2021 by SuperMartijn642
 */
public class ReflectionUtil {

    public static Field findField(String fieldName){
        try{
            Field field = ObfuscationReflectionHelper.findField(AbstractSpawner.class, fieldName);
            field.setAccessible(true);
            return field;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static Method findMethod(String methodName){
        try{
            Method method = ObfuscationReflectionHelper.findMethod(AbstractSpawner.class, methodName);
            method.setAccessible(true);
            return method;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
