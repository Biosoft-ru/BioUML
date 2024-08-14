package biouml.model.dynamics;

import biouml.model.Role;

/**
 * Utility interface that indicates that object contains
 * mathematical expression that should be parsed.
 *
 * Currently it is used
 */
public interface ExpressionOwner
{
    /**
     * Returns true if property contains string that should be parsed
     * as mathematical expression.
     */
    public boolean isExpression(String propertyName);

    /**
     * Returns all owned mathematical expressions.
     * Currently this methods is used to remove unused parameters.
     */
    public String[] getExpressions();
    
    /**
     * Sets all mathematical expressions.
     * Currently this methods is used only to rename parameters.
     */
    public void setExpressions(String[] exps);

    /** Return role object essential to variables name resolving. */
    public Role getRole();
}


