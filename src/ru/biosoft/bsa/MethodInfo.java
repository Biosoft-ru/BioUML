package ru.biosoft.bsa;


/**
 *
 * @todo comments
 * @pending name and context
 */
public interface MethodInfo
{
    public String getMethodName();

    /** changed by ela
    * I removed this method from MethodInfo,
    * since it is only sensible for Match.
    * Therefore this routine is now only included
    * in MatchMethodInfo and is no longer inherited to
    * the other MethodInfo classes
    *
    *   public double getWeight();
    */
}


