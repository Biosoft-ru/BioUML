package ru.biosoft.access;

/**
 * Bean provider which allows to retrieve beans associated with some component
 * @author lan
 */
public interface BeanProvider
{
    public Object getBean(String path);
    
    default void saveBean(String path, Object bean) throws Exception { }
}
