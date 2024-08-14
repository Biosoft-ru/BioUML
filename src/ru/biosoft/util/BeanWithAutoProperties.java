package ru.biosoft.util;


/**
 * Bean which may have auto-properties (property which value can be automatically set if user hasn't changed it)
 * @author lan
 */
public interface BeanWithAutoProperties
{
    public static enum AutoPropertyStatus
    {
        NOT_AUTO_PROPERTY, AUTO_MODE_OFF, AUTO_MODE_ON
    }

    /**
     * Check the status of auto-property
     * @param name - property name
     * @return
     */
    public AutoPropertyStatus getAutoPropertyStatus(String name);
}
