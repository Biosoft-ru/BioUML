package ru.biosoft.access.security;

/**
 * Interface for protected code blocks
 */
public interface PrivilegedAction
{
    /**
     * Execute protected code
     */
    public Object run() throws Exception;
}
