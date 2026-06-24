package biouml.plugins.servermonitor;

import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Plugin lifecycle class for the server monitoring feature.
 * Handles initialization and shutdown of the monitoring service.
 */
public class ServerMonitorPlugin {

    private static final Logger log = Logger.getLogger(ServerMonitorPlugin.class.getName());
    private static ServerMonitorPlugin instance;

    public static ServerMonitorPlugin getInstance() {
        return instance;
    }

    public ServerMonitorPlugin() {
        instance = this;
    }

    private MonitoringService monitoringService;

    /**
     * Initialize the plugin. Called at server startup.
     * @param properties initialization properties from the plugin registry
     */
    public void init(Properties properties) {
        log.info("ServerMonitorPlugin: initializing");

        try {
            // Load configuration
            ServerMonitorConfig config = ServerMonitorConfig.load(
                com.developmentontheedge.application.Application.getPreferences());

            log.info("ServerMonitorPlugin: config loaded: " + config);

            // Create and start the monitoring service
            monitoringService = new MonitoringService(config);
            monitoringService.start();

            log.info("ServerMonitorPlugin: monitoring service started");
        } catch (Exception e) {
            log.log(Level.SEVERE, "ServerMonitorPlugin: failed to start monitoring service", e);
        }
    }

    /**
     * Stop the plugin. Called at server shutdown.
     */
    public void stop() {
        log.info("ServerMonitorPlugin: stopping");

        if (monitoringService != null) {
            monitoringService.stop();
            monitoringService = null;
            log.info("ServerMonitorPlugin: monitoring service stopped");
        }
    }

    /**
     * Get the active monitoring service instance.
     * @return the MonitoringService, or null if not initialized
     */
    public MonitoringService getMonitoringService() {
        return monitoringService;
    }
}
