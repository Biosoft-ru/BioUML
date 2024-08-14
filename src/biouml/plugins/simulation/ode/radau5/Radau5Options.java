package biouml.plugins.simulation.ode.radau5;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.plugins.simulation.OdeSimulatorOptions;

public class Radau5Options extends OdeSimulatorOptions
{
    private double hinit;
    private int mljac;
    private int mujac;
    private int mlmas;
    private int mumas;
    private double hmax;
    private int nmax;
    private double safe;
    private double facl;
    private double facr;
    private int nit;
    private boolean startn;
    private boolean predictGustafsson;
    private boolean hessenberg;
    private double fnewt;
    private double quot1;
    private double quot2;
    private double thet;

    public Radau5Options()
    {

        // Initial step size guess;
        // For stiff equations with initial transient, h = 1.0/(norm of f'), usually 1.0e-3 or 1.0e-5, is good. This choice is not very
        // important, the step size is quickly adapted.
        hinit = 1.E-6;

        // mljac = n - m1:
        //If the non-trivial part of the Jacobian is full
        //0 <= mljac < n - m1:
        //If the (mm+1) submatrices (for k = 0, mm) partial f[i+m1] / partial y[j+k*m2],  i, j = 0, m2-1
        //are banded, mljac is the maximal lower bandwidth of these mm+1 submatrices
        mljac = -1;
        //Maximal upper bandwidth of these mm + 1 submatrices. Need not be defined if mljac = n - m1
        mujac = -1;

        //Switch for the banded structure of the mass-matrix:
        //mlmas = n:         The full matrix case. The linear algebra is done by full-matrix Gauss-elimination.
        //0 <= mlmas < n:    mlmas is the lower bandwith of the matrix ( >= number of non-zero diagonals below the main diagonal).
        //mlmas is supposed to be <= mljac.
        mlmas = 0;

        //Upper bandwith of mass-matrix ( >= number of non-zero diagonals above the main diagonal). Need not be defined if mlmas = n. mumas is supposed to be <= mujac.
        mumas = 0;

        //Maximal step size, default (when hmax is set = 0) xend - x.
        hmax = 0;//1.0e-1;

        //This is the maximal number of allowed steps. The default value is 100000.
        nmax = 100000;

        //The safety factor in step size prediction, default 0.9.
        safe = 0.9;

        //New step size is chosen subject to the restriction 1/facl <= hnew/hold <= 1/facr. Default values: facl = 5.0, facr = 1.0/8.0
        facl = 5.0;
        facr = 0.125;

        //The maximum number of Newton iterations for the solution of the implicit system in each step. The default value is 7.
        nit = 7;

        //If startn == 0 the extrapolated collocation solution is taken as starting value for Newton's method. If startn != false zero starting values are used.
        //The latter is recommended if Newton's method has difficulties with convergence. (This is the case when nstep is larger than naccpt + nrejct; see output parameters).
        //Default is startn = false.
        startn = false;

        //Switch for step size strategy predictGustafsson = true  mod. predictive controller (Gustafsson) else classical step size control
        //The choice predictGustafsson = true seems to produce safer results. For simple problems, the choice predictGustafsson = false produces often slightly faster runs.
        predictGustafsson = false;

        //If the differential system has the special structure that
        //y[i]' = y[i+m2]   for  i = 1, ... , m1,
        //with m1 a multiple of m2, a substantial gain in computertime can be achieved by setting the parameters m1 and m2. e.g., for second order systems
        //p' = v, v' = g(p,v), where p and v are vectors of dimension n/2, one has to put m1 = m2 = n/2.
        //!!! there are no set and get functions for these parameters now
        //m1 = 0;
        //m2 = 0;

        //If hess != 0, the code transforms the Jacobian matrix to Hessenberg form. This is particularly advantageous for large
        //systems with full Jacobian. It does not work for banded Jacobian (mljac < n) and not for implicit systems (imas = 1).
        hessenberg = false;

        //Stopping criterion for Newton's method, usually chosen < 1. Smaller values of fnewt make the code slower, but safer. Default min(0.03, sqrt(rtoler))
        fnewt = 0.0;

        //If quot1 < hnew/hold < quot2, then the step size is not changed. This saves, together with a large thet, lu-decompositions and
        //computing time for large systems. for small systems one may have quot1 = 1.0, quot2 = 1.2, for large full systems quot1 = 0.99,
        //quot2 = 2.0 might be good. Defaults quot1 = 1.0, quot2 = 1.2.
        quot1 = 1.0;
        quot2 = 1.2;

        // Decides whether the Jacobian should be recomputed. Increase thet, to 0.1 say, when Jacobian evaluations are costly. for small systems
        //thet should be smaller (0.001, say). Negative thet forces the code to compute the Jacobian after every accepted step. Default 0.001.
        thet = 0.01;
    }

    @PropertyName("Initial step size")
    @PropertyDescription("Initial step size guess.")
    public double getHinit()
    {
        return hinit;
    }
    public void setHinit(double hinit)
    {
        double oldValue = this.hinit;
        this.hinit = hinit;
        firePropertyChange( "hinit", oldValue, hinit );
    }

    @PropertyName("Max time step")
    @PropertyDescription("Maximal step size, if set to zero, step size is not bounded.")
    public double getHmax()
    {
        return hmax;
    }
    public void setHmax(double hmax)
    {
        double oldValue = this.hmax;
        this.hmax = hmax;
        firePropertyChange( "hmax", oldValue, hmax );
    }

    @PropertyName("Safety factor")
    @PropertyDescription("The safety factor in step size prediction, default is 0.9.")
    public double getSafe()
    {
        return safe;
    }
    public void setSafe(double safe)
    {
        double oldValue = this.safe;
        this.safe = safe;
        firePropertyChange( "safe", oldValue, safe );
    }

    @PropertyName("Min Step size factor")
    @PropertyDescription("New step size is chosen subject to the restriction 1/facl <= hnew/hold. Default value: 5.0.")
    public double getFacl()
    {
        return facl;
    }
    public void setFacl(double facl)
    {
        double oldValue = this.facl;
        this.facl = facl;
        firePropertyChange( "facl", oldValue, facl );
    }

    @PropertyName("Max Step size factor")
    @PropertyDescription("New step size is chosen subject to the restriction hnew/hold <= 1/facr. Default value: 1.0/8.0.")
    public double getFacr()
    {
        return facr;
    }
    public void setFacr(double facr)
    {
        double oldValue = this.facr;
        this.facr = facr;
        firePropertyChange( "facr", oldValue, facr );
    }
    
    @PropertyName("Newton stop criteria")
    @PropertyDescription("Stopping criterion for Newton's method, usually chosen < 1. Smaller values  make simulation slower, but safer. Default min(0.03, sqrt(rtol))")
    public double getFnewt()
    {
        return fnewt;
    }
    public void setFnewt(double fnewt)
    {
        double oldValue = this.fnewt;
        this.fnewt = fnewt;
        firePropertyChange( "fnewt", oldValue, fnewt );
    }

    @PropertyName("quot1")
    @PropertyDescription("If quot1 < hnew/hold < quot2, then the step size is not changed. This saves, together with a large thet, lu-decompositions and computing time for large systems. for small systems one may have quot1 = 1.0, quot2 = 1.2, for large full systems quot1 = 0.99, quot2 = 2.0 might be good. Defaults quot1 = 1.0, quot2 = 1.2.")
    public double getQuot1()
    {
        return quot1;
    }
    public void setQuot1(double quot1)
    {
        double oldValue = this.quot1;
        this.quot1 = quot1;
        firePropertyChange( "quot1", oldValue, quot1 );
    }

    @PropertyName("quot2")
    @PropertyDescription("If quot1 < hnew/hold < quot2, then the step size is not changed. This saves, together with a large thet, lu-decompositions and computing time for large systems. for small systems one may have quot1 = 1.0, quot2 = 1.2, for large full systems quot1 = 0.99, quot2 = 2.0 might be good. Defaults quot1 = 1.0, quot2 = 1.2.")
    public double getQuot2()
    {
        return quot2;
    }

    public void setQuot2(double quot2)
    {
        double oldValue = this.quot2;
        this.quot2 = quot2;
        firePropertyChange( "quot2", oldValue, quot2 );
    }

    @PropertyName("thet")
    @PropertyDescription("Decides whether the Jacobian should be recomputed. Increase thet, to 0.1 say, when Jacobian evaluations are costly. for small systems thet should be smaller (0.001, say). Negative thet forces the code to compute the Jacobian after every accepted step. Default 0.001.")
    public double getThet()
    {
        return thet;
    }

    public void setThet(double thet)
    {
        double oldValue = this.thet;
        this.thet = thet;
        firePropertyChange( "thet", oldValue, thet );
    }

    @PropertyName("Jacobian Ml")
    @PropertyDescription("Jacobian Ml = n - m1: If the non-trivial part of the Jacobian is full 0 <= Jacobian Ml < n - m1: If the (mm+1) submatrices (for k = 0, mm) partial f[i+m1] / partial y[j+k*m2],  i, j = 0, m2-1 are banded, Jacobian Ml is the maximal lower bandwidth of these mm+1 submatrices")
    public int getMljac()
    {
        return mljac;
    }
    public void setMljac(int mljac)
    {
        int oldValue = this.mljac;
        this.mljac = mljac;
        firePropertyChange( "mljac", oldValue, mljac );
    }

    @PropertyName("Jacobian Mu")
    @PropertyDescription("Maximal upper bandwidth of these mm + 1 submatrices. Need not be defined if Jacobian Ml = n - m1")
    public int getMujac()
    {
        return mujac;
    }
    public void setMujac(int mujac)
    {
        int oldValue = this.mujac;
        this.mujac = mujac;
        firePropertyChange( "mujac", oldValue, mujac );
    }
    
    @PropertyName("Mass Ml")
    @PropertyDescription("Switch for the banded structure of the mass-matrix: mlmas = n: The full matrix case. The linear algebra is done by full-matrix Gauss-elimination. 0 <= mlmas < n:    mlmas is the lower bandwith of the matrix ( >= number of non-zero diagonals below the main diagonal). mlmas is supposed to be <= mljac.")
    public int getMlmas()
    {
        return mlmas;
    }
    public void setMlmas(int mlmas)
    {
        int oldValue = this.mlmas;
        this.mlmas = mlmas;
        firePropertyChange( "mlmas", oldValue, mlmas );
    }

    @PropertyName("Mass Mu")
    @PropertyDescription("Upper bandwith of mass-matrix ( >= number of non-zero diagonals above the main diagonal). Need not be defined if mlmas = n. mumas is supposed to be <= mujac.")
    public int getMumas()
    {
        return mumas;
    }
    public void setMumas(int mumas)
    {
        int oldValue = this.mumas;
        this.mumas = mumas;
        firePropertyChange( "mumas", oldValue, mumas );
    }

    @PropertyName("Max steps number")
    @PropertyDescription("Maximal number of allowed steps. The default value is 100000.")
    public int getNmax()
    {
        return nmax;
    }
    public void setNmax(int nmax)
    {
        int oldValue = this.nmax;
        this.nmax = nmax;
        firePropertyChange( "nmax", oldValue, nmax );
    }

    @PropertyName("Newton iterations number")
    @PropertyDescription("The maximum number of Newton iterations for the solution of the implicit system in each step. The default value is 7.")
    public int getNit()
    {
        return nit;
    }
    public void setNit(int nit)
    {
        int oldValue = this.nit;
        this.nit = nit;
        firePropertyChange( "nit", oldValue, nit );
    }

    @PropertyName("Start from zero")
    @PropertyDescription("If false then the extrapolated collocation solution is taken as starting value for Newton's method. If true then zero starting values are used. The latter is recommended if Newton's method has difficulties with convergence. Default is false.")  
    public boolean isStartn()
    {
        return startn;
    }
    public void setStartn(boolean startn)
    {
        boolean oldValue = this.startn;
        this.startn = startn;
        firePropertyChange( "startn", oldValue, startn );
    }

    @PropertyName("Gustafsson predictive")
    @PropertyDescription("Switch for step size strategy: predictive controller (Gustafsson) or classical step size control.")
    public boolean isPredictGustafsson()
    {
        return predictGustafsson;
    }
    public void setPredictGustafsson(boolean predictGustafsson)
    {
        boolean oldValue = this.predictGustafsson;
        this.predictGustafsson = predictGustafsson;
        firePropertyChange( "predictGustafsson", oldValue, predictGustafsson );
    }

    @PropertyName("Hessenberg jacobian")
    @PropertyDescription("If true, Jacobian matrix is transformed to Hessenberg form. This is particularly advantageous for large systems with full Jacobian. It does not work for banded Jacobian and implicit systems.")
    public boolean isHessenberg()
    {
        return hessenberg;
    }
    public void setHessenberg(boolean hessenberg)
    {
        boolean oldValue = this.hessenberg;
        this.hessenberg = hessenberg;
        firePropertyChange( "hessenberg", oldValue, hessenberg );
    }
}