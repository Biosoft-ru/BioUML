package ru.biosoft.util.serialization;

/**
 * Created by IntelliJ IDEA.
 * User: puz
 * Date: 08.08.2006
 * Time: 17:43:38
 * <p/>
 * Used for construction of non-default constructible objects
 * <p/>
 * NOTE: do not use anonymous inheritants of it - serilization of anonymous classes
 * will fail
 */
public interface CustomTypeFactory
{
    public Object getOriginObject();

    /**
     * @param o Objects to be serialized
     * @return instanse of factory wrapping origin object
     */
    public Object getFactoryInstance( Object o );
}
