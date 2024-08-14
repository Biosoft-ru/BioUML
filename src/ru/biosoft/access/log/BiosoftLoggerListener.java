package ru.biosoft.access.log;

public interface BiosoftLoggerListener
{
    public void messageAdded(EventType type, String message);
}
