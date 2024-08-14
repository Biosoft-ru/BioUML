package ru.biosoft.plugins.javascript;

import org.mozilla.javascript.Scriptable;

import ru.biosoft.access.script.ScriptEnvironment;

/**
 * Interface for JavaScript objects which provide JSEnvironment
 */
public interface JSEnvironmentProvider
{
    /**
     * Returns JavaScript environment
     */
    public ScriptEnvironment getEnvironment();

    /**
     * Set scope for JavaScript object
     */
    public void setScopeObject(Scriptable scope);
}
