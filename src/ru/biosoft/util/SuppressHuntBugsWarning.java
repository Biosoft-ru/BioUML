package ru.biosoft.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface SuppressHuntBugsWarning
{
    String[] value();
}
