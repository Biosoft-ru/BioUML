package ru.biosoft.plugins.javascript;

import org.mozilla.javascript.Scriptable;

/**
 * JavaScript preprocessor interface
 */
public interface Preprocessor
{
    /**
     * Returns preprocessed JavaScript code
     */
    public String preprocess(String code);
    /**
     * Converts to JavaScript code
     */
    public String wrap(String code, String object);
    
    /**
     * Get prefix for command string
     */
    public String getPrefix();
    
    /**
     * Look for new object in scope to add to preprocessor list
     */
    public String lookForNewPreprocessor(Scriptable scope);
}
