package com.course.server;

import com.course.server.config.ServerConfig;
import com.course.server.network.TcpServer;

import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main application entry point for the Server module.
 */
public class ServerApplication {
    private static final Logger LOGGER = Logger.getLogger(ServerApplication.class.getName());

    public static void main(String[] args) {
        // 1. Parse dynamic CLI arguments (Port, Node Name, Flags)
        ServerConfig.getInstance().applyCliArgs(args);

        LOGGER.info(String.format("Initializing [%s] on port %d...",
                ServerConfig.getInstance().getNodeName(), ServerConfig.getInstance().getServerPort()));

        boolean headless = GraphicsEnvironment.isHeadless();
        for (String arg : args) {
            if ("--cli".equalsIgnoreCase(arg) || "--headless".equalsIgnoreCase(arg)) {
                headless = true;
                break;
            }
        }

        if (headless) {
            startCliServer();
        } else {
            startGuiServer();
        }
    }

    public static void startCliServer() {
        LOGGER.info("Starting Server in CLI/Headless mode...");
        try {
            TcpServer server = TcpServer.createDefault();
            ServerContext.getInstance().initialize(server);
            server.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Shutdown hook triggered. Stopping server...");
                server.stop();
            }));

            // Keep main thread alive
            Thread.currentThread().join();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start TCP Server in CLI mode", e);
        }
    }

    public static void startGuiServer() {
        // Will be connected with AdminServerDashboard in Part 8
        try {
            Class<?> dashboardClass = Class.forName("com.course.server.ui.AdminDashboardFrame");
            java.lang.reflect.Method mainMethod = dashboardClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object) new String[]{});
        } catch (ClassNotFoundException e) {
            LOGGER.info("Admin UI not yet available, falling back to CLI mode.");
            startCliServer();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not launch Admin GUI, falling back to CLI mode.", e);
            startCliServer();
        }
    }
}
