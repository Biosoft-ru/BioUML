package biouml.plugins.fbc;

import java.io.IOException;

import ru.biosoft.exception.InternalException;

public interface FbcConstant
{
    public static final String FBC_PACKAGE_NAME               = "fbc";
    
    public static final String LIST_OF_FLUX_BOUNDS            = "fbc:listOfFluxBounds";
    public static final String FLUX_BOUND                     = "fbc:fluxBound";
    public static final String LIST_OF_OBJECTIVES             = "fbc:listOfObjectives";
    public static final String OBJECTIVE                      = "fbc:objective";
    public static final String LIST_OF_FLUX_OBJECTIVES        = "fbc:listOfFluxObjectives";
    public static final String FLUX_OBJECTIVE                 = "fbc:fluxObjective";
    public static final String FBC_ACTIVE_OBJECTIVE           = "fbc:activeObjective";
    public static final String FBC_REACTION                   = "fbc:reaction";
    public static final String FBC_COEFFICIENT                = "fbc:coefficient";
    public static final String FBC_OPERATION                  = "fbc:operation";
    public static final String FBC_VALUE                      = "fbc:value";


    public static final String FBC_ID                         = "fbc:id";
    public static final String FBC_TYPE                       = "fbc:type";
    public static final String FBC_OBJECTIVES                 = "objectives";
    public static final String FBC_BOUNDS                     = "bounds";

    public static final String FBC_LESS_EQUAL                 = "lessEqual";
    public static final String FBC_GREATER_EQUAL              = "greaterEqual";
    public static final String FBC_EQUAL                      = "equal";

    public static final String FBC_CHARGE                     = "fbc:charge";
    public static final String FBC_CHEMICAL_FORMULA           = "fbc:chemicalFormula";
    public static final String FBC_NAME                       = "fbc:name";

    public static final String SBML_TYPE                      = "SbmlType";
    public static final String FBC_LIST_OBJECTIVES            = "listObjectives";

    public static final String MAX                            = "maximize";
    public static final String MIN                            = "minimize";

    public static final String APACHE_SOLVER                  = "Apache solver";
    public static final String GUROBI_SOLVER                  = "Gurobi solver";
    public static final String GLPK_SOLVER                    = "GLPK solver";

    //fbc v.2
    public static final String FBC_VERSION2                   = "http://www.sbml.org/sbml/level3/version1/fbc/version2";
    public static final String FBC_LOWER_FLUX_BOUND           = "fbc:lowerFluxBound";
    public static final String FBC_UPPER_FLUX_BOUND           = "fbc:upperFluxBound";

    public static final String FBC_GENE_PRODUCT               = "fbc:geneProduct";
    public static final String FBC_GENE_PRODUCT_REF           = "fbc:geneProductRef";
    public static final String FBC_AND                        = "fbc:and";
    public static final String FBC_OR                         = "fbc:or";
    public static final String FBC_GENE_PRODUCT_ASSOCIATION   = "fbc:geneProductAssociation";
    
    public static final String GENE_PRODUCT_ASSOCIATION_ATTR  = "geneProductAssociation";
    public static final String FBC_ASSOCIATED_SPECIES         = "fbc:associatedSpecies";
    public static final String FBC_LABEL                      = "fbc:label";
    public static final String FBC_LIST_OF_GENE_PRODUCTS      = "fbc:listOfGeneProducts";
    
    public static final String FBC_STRICT                     = "fbc:strict";
    public static final String FBC_REQUIRED                   = "fbc:required";
    
    public static String[] getAvailableFunctionTypes()
    {
        return new String[] {MAX, MIN};
    }

    //TODO: move solvers to registry
    public static String[] getAvailableSolverNames()
    {
        try
        {
            FbcModelCreator test = new GLPKModelCreator();
            return new String[] {GLPK_SOLVER, APACHE_SOLVER};//, GUROBI_SOLVER};
        }
        catch( Throwable e )
        {
            return new String[] {APACHE_SOLVER};//, GUROBI_SOLVER};
        }

    }
    public static FbcModelCreator getSolverByType(String solverType)
    {
        if( solverType.equals( FbcConstant.GUROBI_SOLVER ) )
            return new GurobiModelCreator();
        else if( solverType.equals( FbcConstant.APACHE_SOLVER ) )
            return new ApacheModelCreator();
        else if( solverType.equals( FbcConstant.GLPK_SOLVER ) )
            try
            {
                return new GLPKModelCreator();
            }
            catch( IOException e )
            {
                throw new InternalException( e, "Can not load GLPK model creator" );
            }
        else
            throw new InternalException( "Wrong solver type" );
    }

}
