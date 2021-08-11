package com.supermartijn642.scarecrowsterritory;

import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created 1/14/2021 by SuperMartijn642
 */
public class ReflectionUtil {

    public static <T> Field findField(Class<? super T> classs, String fieldName){
        try{
            Field field = ObfuscationReflectionHelper.findField(classs, fieldName);
            field.setAccessible(true);
            return field;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static <T> Method findMethod(Class<? super T> classs, String methodName, Class<?>... parameters){
        try{
            Method method = ObfuscationReflectionHelper.findMethod(classs, methodName, parameters);
            method.setAccessible(true);
            return method;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
