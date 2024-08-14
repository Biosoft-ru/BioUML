
package ru.biosoft.bsa;

/**
 * The <code>Basis</code> enumeration values are used to specify whether
 * a <code>Site</code> originated from an experimental result
 * computational anlyses or both.
 */
public interface Basis
{
    public static final int BASIS_USER           = 0;
    public static final int BASIS_ANNOTATED      = 1;
    public static final int BASIS_PREDICTED      = 2;
    public static final int BASIS_USER_ANNOTATED = 3;
}
