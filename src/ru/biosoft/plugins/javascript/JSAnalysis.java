package ru.biosoft.plugins.javascript;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation indicates that method is JavaScript-method corresponding to some AnalysisMethod
 * This used for automatic generation of JavaScript code
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JSAnalysis
{
    /**
     * Analysis class corresponding to the method
     */
    Class<? extends Object> value();
}
