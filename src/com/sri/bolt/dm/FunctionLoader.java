package com.sri.bolt.dm;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class FunctionLoader {

    public static Object createObject(String canonicalClassName, Object... constructorArgs) {
    	Object o2 = null;
        Class[] argTypes;
        argTypes = constructorArgs == null ? new Class[0] : new Class[constructorArgs.length];
        for (int i = 0; i < argTypes.length; i++)
            argTypes[i] = constructorArgs[i].getClass();

        try {
            Class<?> c2 = Class.forName(canonicalClassName); 
            Constructor<?> constructor2 = c2.getConstructor(argTypes);
            o2 = (Object) constructor2.newInstance(constructorArgs);
        } 
        catch (Exception e) {
            //e.printStackTrace();
        } 
        return o2;
    }

    public static Method getMethod(String canonicalClassName, String methodName, Class<?>... argTypes) {
        Class<?> c;
        Method m = null;
        try {
            c = Class.forName(canonicalClassName);
            m = getMethodTag(c, methodName, argTypes);
            m.setAccessible(true);
        } 
        catch (Exception e) {
//            e.printStackTrace();
        }
        return m;
    }

    public static Method getMethod(Class<?> c, String methodName, Object... args) {
        Class<?>[] argTypes = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] == null)
                argTypes[i] = null;
            else
                argTypes[i] = args[i].getClass();
        }

        return getMethod(c, methodName, argTypes);
    }

    private static Method getMethod(Class<?> c, String methodName, Class<?>... argTypes) {
        Method m = null;
        try {
            m = getMethodTag(c, methodName, argTypes);
            if (m != null)
                m.setAccessible(true);
        } catch (SecurityException e) {
//            e.printStackTrace();
        }
        return m;
    }

    private static Method getMethodTag(Class targetClass, String methodName, Class[] parameters) {
        for (Method method : targetClass.getMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            boolean matches = true;
            for (int i = 0; i < parameterTypes.length; i++) {
                if (parameterTypes[i] == Object[].class)
                    break;
                if (parameters[i] != null && !parameterTypes[i].isAssignableFrom(parameters[i])) {
                    matches = false;
                    break;
                }
            }
            if (matches)
                return method;
        }
        return null;
    }

    public static Object invokeMethod(Object o, String methodName, Object... args) {
        Method m = getMethod(o.getClass(), methodName, args);
        return invoke(o, m, args);
    }

    public static Class<?> getReturnType(Object o, String methodName, Object... args) {
        Method m = getMethod(o.getClass(), methodName, args);
        Class<?> c = m.getReturnType();
        return c;
    }

    public static Object invoke(Object o, Method m, Object... args) {
        try {
            Object[] arr = new Object[args.length];
            for (int i = 0; i < args.length; i++)
                arr[i] = args[i];

            if (m.getParameterTypes().length == 0)
                return m.invoke(o, new Object[0]);
            else if (m.getParameterTypes()[0] == Object[].class)
                return m.invoke(o, (Object) arr);
            else
                return m.invoke(o, args);
        } 
        catch (Exception e) {
        	// e.printStackTrace();
        }
        return null;
    }
}
