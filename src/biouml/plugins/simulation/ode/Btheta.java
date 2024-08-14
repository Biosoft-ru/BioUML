package biouml.plugins.simulation.ode;

/*
   this is the interpolant interface (or abstract superclass).  Any interpolant that is
   defined in this package or that the user wants to define must implement (be a subclass
   of) this class
 */
public interface Btheta
{
    // constructors

    // methods

    /*
       a Btheta (interpolant function) declaration that is defined by the implementor
     */
    public double[] f(double theta); // a dummy f function (method)
    // that will be overloaded to represent an interpolant function
}