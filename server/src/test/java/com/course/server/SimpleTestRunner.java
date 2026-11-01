package com.course.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class SimpleTestRunner {
    public static void main(String[] args) {
        String[] testClasses = args != null && args.length > 0 ? args : new String[]{
            "com.course.server.session.SingleSessionPolicyTest",
            "com.course.server.security.BCryptPasswordTest",
            "com.course.server.session.ClientDisconnectRetentionIntegrationTest"
        };
        int passed = 0;
        int failed = 0;
        for (String className : testClasses) {
            System.out.println("Running " + className + "...");
            try {
                Class<?> clazz = Class.forName(className);
                List<Method> beforeMethods = new ArrayList<>();
                List<Method> testMethods = new ArrayList<>();
                for (Method m : clazz.getDeclaredMethods()) {
                    if (m.isAnnotationPresent(BeforeEach.class)) beforeMethods.add(m);
                    if (m.isAnnotationPresent(Test.class)) testMethods.add(m);
                }
                java.lang.reflect.Constructor<?> constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                for (Method tm : testMethods) {
                    Object instance = constructor.newInstance();
                    try {
                        for (Method bm : beforeMethods) {
                            bm.setAccessible(true);
                            bm.invoke(instance);
                        }
                        tm.setAccessible(true);
                        tm.invoke(instance);
                        System.out.println("  [PASS] " + tm.getName());
                        passed++;
                    } catch (Throwable ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        System.err.println("  [FAIL] " + tm.getName() + ": " + cause.getMessage());
                        cause.printStackTrace(System.err);
                        failed++;
                    }
                }
            } catch (Throwable e) {
                System.err.println("Failed to run test class " + className + ": " + e.getMessage());
                e.printStackTrace(System.err);
                failed++;
            }
        }
        System.out.println("========================================");
        System.out.println("Test Results: " + passed + " PASSED, " + failed + " FAILED");
        System.out.println("========================================");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
