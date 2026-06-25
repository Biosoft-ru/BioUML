package biouml.plugins.servermonitor;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.Preferences;

/**
 * Configuration for the server monitoring plugin.
 * All parameters are configurable via BioUML preferences or the API.
 */
public class ServerMonitorConfig {

    private static final Logger log = Logger.getLogger(ServerMonitorConfig.class.getName());

    // --- Task monitoring ---
    public static final String SLOW_TASK_THRESHOLD = "slowTaskThreshold";
    public static final String CHECK_INTERVAL = "checkInterval";

    // --- Profiler ---
    public static final String PROFILER_PATH = "profilerPath";
    public static final String PROFILER_DIR = "profilerDir";
    public static final String MAX_PROFILES = "maxProfiles";
    public static final String PROFILE_DURATION = "profileDuration";

    // --- Periodic profiling ---
    public static final String PERIODIC_INTERVAL = "periodicInterval";
    public static final String PERIODIC_MODE = "periodicMode";

    // --- Cleanup ---
    public static final String MAX_PROFILE_AGE = "maxProfileAge";

    // --- Logging ---
    public static final String LOG_LEVEL = "logLevel";

    // Default values
    public static final int DEFAULT_SLOW_TASK_THRESHOLD = 3600;       // 1 hour
    public static final int DEFAULT_CHECK_INTERVAL = 60;              // 60 seconds
    public static final String DEFAULT_PROFILER_PATH = "./profiling/async-profiler-3.0-linux-x64/bin/profiler.sh";
    public static final String DEFAULT_PROFILER_DIR = "./profiling";
    public static final int DEFAULT_MAX_PROFILES = 50;
    public static final int DEFAULT_PROFILE_DURATION = 30;            // 30 seconds
    public static final int DEFAULT_PERIODIC_INTERVAL = 1800;         // 30 minutes
    public static final String DEFAULT_PERIODIC_MODE = "random";
    public static final int DEFAULT_MAX_PROFILE_AGE = 604800;         // 7 days
    public static final String DEFAULT_LOG_LEVEL = "INFO";

    // Config values
    private int slowTaskThreshold = DEFAULT_SLOW_TASK_THRESHOLD;
    private int checkInterval = DEFAULT_CHECK_INTERVAL;
    private String profilerPath = DEFAULT_PROFILER_PATH;
    private String profilerDir = DEFAULT_PROFILER_DIR;
    private int maxProfiles = DEFAULT_MAX_PROFILES;
    private int profileDuration = DEFAULT_PROFILE_DURATION;
    private int periodicInterval = DEFAULT_PERIODIC_INTERVAL;
    private String periodicMode = DEFAULT_PERIODIC_MODE;
    private int maxProfileAge = DEFAULT_MAX_PROFILE_AGE;
    private String logLevel = DEFAULT_LOG_LEVEL;

    /**
     * Load configuration from BioUML preferences.
     * @param prefs BioUML preferences, may be null
     * @return a new ServerMonitorConfig with values from preferences or defaults
     */
    public static ServerMonitorConfig load(Preferences prefs) {
        ServerMonitorConfig config = new ServerMonitorConfig();

        if (prefs == null) {
            log.fine("No preferences provided, using all defaults");
            return config;
        }

        try {
            Preferences monitorPrefs = prefs.getPreferencesValue("serverMonitor");
            if (monitorPrefs == null) {
                log.fine("No serverMonitor preferences found, using all defaults");
                return config;
            }

            for (DynamicProperty dp : monitorPrefs) {
                String key = dp.getName();
                Object value = dp.getValue();
                if (value == null) continue;

                if (SLOW_TASK_THRESHOLD.equals(key)) {
                    config.slowTaskThreshold = toInt(value, DEFAULT_SLOW_TASK_THRESHOLD);
                } else if (CHECK_INTERVAL.equals(key)) {
                    config.checkInterval = toInt(value, DEFAULT_CHECK_INTERVAL);
                } else if (PROFILER_PATH.equals(key)) {
                    config.profilerPath = toString(value, DEFAULT_PROFILER_PATH);
                } else if (PROFILER_DIR.equals(key)) {
                    config.profilerDir = toString(value, DEFAULT_PROFILER_DIR);
                } else if (MAX_PROFILES.equals(key)) {
                    config.maxProfiles = toInt(value, DEFAULT_MAX_PROFILES);
                } else if (PROFILE_DURATION.equals(key)) {
                    config.profileDuration = toInt(value, DEFAULT_PROFILE_DURATION);
                } else if (PERIODIC_INTERVAL.equals(key)) {
                    config.periodicInterval = toInt(value, DEFAULT_PERIODIC_INTERVAL);
                } else if (PERIODIC_MODE.equals(key)) {
                    config.periodicMode = toString(value, DEFAULT_PERIODIC_MODE);
                } else if (MAX_PROFILE_AGE.equals(key)) {
                    config.maxProfileAge = toInt(value, DEFAULT_MAX_PROFILE_AGE);
                } else if (LOG_LEVEL.equals(key)) {
                    config.logLevel = toString(value, DEFAULT_LOG_LEVEL);
                }
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Error loading serverMonitor config from preferences, using defaults", e);
        }

        return config;
    }

    /**
     * Load configuration from a properties file.
     * @param path path to the properties file
     * @return a new ServerMonitorConfig with values from the file or defaults
     */
    public static ServerMonitorConfig loadFromFile(String path) {
        ServerMonitorConfig config = new ServerMonitorConfig();
        File file = new File(path);

        if (!file.exists()) {
            log.fine("Config file not found: " + path + ", using all defaults");
            return config;
        }

        try (FileReader reader = new FileReader(file)) {
            Properties props = new Properties();
            props.load(reader);

            config.slowTaskThreshold = getIntProp(props, SLOW_TASK_THRESHOLD, DEFAULT_SLOW_TASK_THRESHOLD);
            config.checkInterval = getIntProp(props, CHECK_INTERVAL, DEFAULT_CHECK_INTERVAL);
            config.profilerPath = getStringProp(props, PROFILER_PATH, DEFAULT_PROFILER_PATH);
            config.profilerDir = getStringProp(props, PROFILER_DIR, DEFAULT_PROFILER_DIR);
            config.maxProfiles = getIntProp(props, MAX_PROFILES, DEFAULT_MAX_PROFILES);
            config.profileDuration = getIntProp(props, PROFILE_DURATION, DEFAULT_PROFILE_DURATION);
            config.periodicInterval = getIntProp(props, PERIODIC_INTERVAL, DEFAULT_PERIODIC_INTERVAL);
            config.periodicMode = getStringProp(props, PERIODIC_MODE, DEFAULT_PERIODIC_MODE);
            config.maxProfileAge = getIntProp(props, MAX_PROFILE_AGE, DEFAULT_MAX_PROFILE_AGE);
            config.logLevel = getStringProp(props, LOG_LEVEL, DEFAULT_LOG_LEVEL);
        } catch (Exception e) {
            log.log(Level.WARNING, "Error loading config from file: " + path, e);
        }

        return config;
    }

    /**
     * Get a configuration value by key name.
     * @param key the configuration key
     * @return the value as a String
     */
    public String get(String key) {
        switch (key) {
            case SLOW_TASK_THRESHOLD: return String.valueOf(slowTaskThreshold);
            case CHECK_INTERVAL: return String.valueOf(checkInterval);
            case PROFILER_PATH: return profilerPath;
            case PROFILER_DIR: return profilerDir;
            case MAX_PROFILES: return String.valueOf(maxProfiles);
            case PROFILE_DURATION: return String.valueOf(profileDuration);
            case PERIODIC_INTERVAL: return String.valueOf(periodicInterval);
            case PERIODIC_MODE: return periodicMode;
            case MAX_PROFILE_AGE: return String.valueOf(maxProfileAge);
            case LOG_LEVEL: return logLevel;
            default: return null;
        }
    }

    /**
     * Set a configuration value by key name.
     * @param key the configuration key
     * @param value the value to set
     */
    public void set(String key, Object value) {
        if (value == null) return;

        switch (key) {
            case SLOW_TASK_THRESHOLD:
                slowTaskThreshold = toInt(value, DEFAULT_SLOW_TASK_THRESHOLD);
                break;
            case CHECK_INTERVAL:
                checkInterval = toInt(value, DEFAULT_CHECK_INTERVAL);
                break;
            case PROFILER_PATH:
                profilerPath = toString(value, DEFAULT_PROFILER_PATH);
                break;
            case PROFILER_DIR:
                profilerDir = toString(value, DEFAULT_PROFILER_DIR);
                break;
            case MAX_PROFILES:
                maxProfiles = toInt(value, DEFAULT_MAX_PROFILES);
                break;
            case PROFILE_DURATION:
                profileDuration = toInt(value, DEFAULT_PROFILE_DURATION);
                break;
            case PERIODIC_INTERVAL:
                periodicInterval = toInt(value, DEFAULT_PERIODIC_INTERVAL);
                break;
            case PERIODIC_MODE:
                periodicMode = toString(value, DEFAULT_PERIODIC_MODE);
                break;
            case MAX_PROFILE_AGE:
                maxProfileAge = toInt(value, DEFAULT_MAX_PROFILE_AGE);
                break;
            case LOG_LEVEL:
                logLevel = toString(value, DEFAULT_LOG_LEVEL);
                break;
        }
    }

    // --- Getters ---

    public int getSlowTaskThreshold() {
        return slowTaskThreshold;
    }

    public int getCheckInterval() {
        return checkInterval;
    }

    public String getProfilerPath() {
        return profilerPath;
    }

    public String getProfilerDir() {
        return profilerDir;
    }

    public int getMaxProfiles() {
        return maxProfiles;
    }

    public int getProfileDuration() {
        return profileDuration;
    }

    public int getPeriodicInterval() {
        return periodicInterval;
    }

    public String getPeriodicMode() {
        return periodicMode;
    }

    public int getMaxProfileAge() {
        return maxProfileAge;
    }

    public String getLogLevel() {
        return logLevel;
    }

    // --- Utility methods ---

    private static int toInt(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private static String toString(Object value, String defaultValue) {
        return value == null ? defaultValue : value.toString();
    }

    private static int getIntProp(Properties props, String key, int defaultValue) {
        String val = props.getProperty(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String getStringProp(Properties props, String key, String defaultValue) {
        String val = props.getProperty(key);
        return val == null ? defaultValue : val;
    }

    @Override
    public String toString() {
        return "ServerMonitorConfig{" +
                "slowTaskThreshold=" + slowTaskThreshold +
                ", checkInterval=" + checkInterval +
                ", profilerPath='" + profilerPath + '\'' +
                ", profilerDir='" + profilerDir + '\'' +
                ", maxProfiles=" + maxProfiles +
                ", profileDuration=" + profileDuration +
                ", periodicInterval=" + periodicInterval +
                ", periodicMode='" + periodicMode + '\'' +
                ", maxProfileAge=" + maxProfileAge +
                ", logLevel='" + logLevel + '\'' +
                '}';
    }
}
