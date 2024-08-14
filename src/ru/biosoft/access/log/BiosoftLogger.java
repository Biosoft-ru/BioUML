package ru.biosoft.access.log;

public interface BiosoftLogger
{
    public void info(String msg);
    public void warn(String msg);
    public void error(String msg);
    
    default void log(EventType type, String msg) {
        switch(type) {
            case INFO:
                info(msg);
                break;
            case WARN:
                warn(msg);
                break;
            case ERROR:
                error(msg);
                break;
        }
    }
    
    public void addListener(BiosoftLoggerListener listener);
    public void removeListener(BiosoftLoggerListener listener);
}
