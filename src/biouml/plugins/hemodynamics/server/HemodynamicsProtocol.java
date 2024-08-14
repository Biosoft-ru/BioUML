package biouml.plugins.hemodynamics.server;

import biouml.plugins.server.access.AccessProtocol;

public class HemodynamicsProtocol extends AccessProtocol
{
    public static final String HEMODYNAMICS_SERVICE = "hemodynamics.service";
    
    /////////////////////////////////////////////
    // Hemodynamics constants
    //
    
    public static final int HD_IS_VISIBLE = 601;
    
    public static final int HD_GET_PARAMETERS = 602;
    
    public static final int HD_GET_TABLE = 603;
    
    public static final int RUN = 604;
    
    public static final int STOP = 605;
    
    public static final int HD_GET_IMG = 606;
    
    
    ////////////////////////////////////////////
    // Cache object prefixes
    //
    public static final String HD_PARAMETERS_BEAN = "beans/hemodynamics/parameters/";
    public static final String HD_VESSELSTABLE = "hemodynamics/table/";
    public static final String HD_TABLE_RESOLVER = "hemodynamics/resolver";
    public static final String HD_PLOT_SERIES = "hemodynamics/plotseries/";
    public static final String HD_PLOT_DATA = "hemodynamics/plot/data/";
    public static final String HD_PLOT_TAU = "hemodynamics/plot/tau/";
    public static final String HD_SOLVER = "hemodynamics/solver/";
    
}
