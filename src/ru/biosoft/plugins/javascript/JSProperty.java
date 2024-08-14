package ru.biosoft.plugins.javascript;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface JSProperty
{
    /**
     * Bean property corresponding to method field
     */
    String value();

}
