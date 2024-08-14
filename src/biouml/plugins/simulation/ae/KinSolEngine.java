package biouml.plugins.simulation.ae;

import biouml.plugins.simulation.ae.AeModel;
import biouml.plugins.simulation.ode.jvode.VectorUtils;

public abstract class KinSolEngine extends KinSolSupport
{

    public KinSolEngine(AeModel f, double[] initialGuess, int strategy) throws Exception
    {
        super(f, initialGuess);
    }

    /**
     * Manages the computational
     * process of computing an approximate solution of the nonlinear
     * system F(u) = 0. Routine calls the following
     * subroutines:
     *
     *  <li>initSolver    checks if initial guess satisfies user-supplied
     *                constraints and initializes linear solver
     *
     *  <li>approxSolution  interfaces with linear solver to find a
     *                solution of the system J(u)*x = b (calculate
     *                Newton step)
     *
     *  <li>newton/lineSearch  implement the global strategy
     *
     *  <li>forcingTerm  computes the forcing term (eta)
     *
     *  <li>checkSolution  determines if an approximate solution has been found
     * @param initialGuess - initial guess for problem solution
     * @param strategy - strategy for solving the problem can be NONE or LINESEARCH
     */
    public int start(double[] initialGuess, int strategy)
    {
        setIntitalGuess(initialGuess);
        setStartegy(strategy);
        return start();
    }

    public int start()
    {
        /* initialize solver */
        int flag = initSolver();
        if( flag != SUCCESS )
        {
            processFlag(flag);
            return flag;
        }
        /* Note: The following logic allows the choice of whether or not
           to force a call to the linear solver setup upon a given call to
           KINSol */
        sthrsh = ( noInitSetup ) ? 1 : 2;

        /* if eps is to be bounded from below, set the bound */
        double epsmin = ( inexactLs && !noMinEps ) ? 0.01 * fTolerance : 0;


        /* if omega is zero at this point, make sure it will be evaluated
           at each iteration based on the provided min/max bounds and the
           current function norm. */
        evalOmega = ( omega == 0 );

        for( ;; )
        {
            iterationsNumber++;

            /* calculate the epsilon (stopping criteria for iterative linear solver)
               for this iteration based on eta from the routine KINForcingTerm */
            if( inexactLs )
            {
                eps = ( eta + UNIT_ROUNDOFF ) * fnorm;
                if( !noMinEps )
                    eps = Math.max(epsmin, eps);
            }

            retryIteration = false;
            do
            {
                flag = doIteration();
            }
            while( retryIteration );

            /* update uu after the iteration */
            VectorUtils.copy(uNew, u);
            f1norm = f1normp;

            /* print the current nni, fnorm, and nfe values if printfl > 0 */
            if( debagLevel > 0 )
                printInfo("nni = " + iterationsNumber + " nfe = " + modelFunctionCallNumber + "fnorm = " + fnorm);

            if( flag != CONTINUE_ITERATIONS )
                break;

        } /* end of loop; return */

        if( debagLevel > 0 )
            printInfo("flag = " + flag);

        processFlag(flag);
        return flag;
    }

    private int doIteration()
    {
        /* call approxSolution to calculate the (approximate) Newton step, pp */
        int flag = approxSolution();

        if( flag != SUCCESS )
            return flag;

        /* call the appropriate routine to calculate an acceptable step pp */
        int sflag = 0;

        if( strategy == NONE )
        {
            /* Full Newton Step*/
            sflag = newton();

            /* if sysfunc failed unrecoverably, stop */
            if( ( sflag == SYSFUNC_FAIL ) || ( sflag == REPTD_SYSFUNC_ERR ) )
                return sflag;
        }
        else if( strategy == LINESEARCH )
        {
            /* Line Search */
            sflag = lineSearch();

            /* if sysfunc failed unrecoverably, stop */
            if( sflag == SYSFUNC_FAIL )
                return sflag;

            /* if too many beta condition failures, then stop iteration */
            if( nbcf > mxnbcf )
                return LINESEARCH_BCFAIL;
        }

        /* evaluate eta by calling the forcing term routine */
        if( callForcingTerm )
            forcingTerms();

        fnorm = fnormp;

        /* call checkSolution to check if tolerances where met by this iteration */
        return checkSolution(sflag);
    }


    /**
    * Initializes the problem for the specific input
    * received in this call to solver.
    * @return
    *   <li>SUCCESS : indicates a normal initialization
    *   <li>INITIAL_GUESS_OK : indicates that the guess u
    *                          satisfied the system f(u) = 0
    *                          within the tolerances specified
    * @throws IllegalARgumentException if initial guess does not meet constraints
    */
    private int initSolver()
    {
        /* check for illegal input parameters */
        if( uscale == null )
        {
            uscale = new double[n];
            for( int i = 0; i < n; i++ )
                uscale[i] = 1;
        }
        if( fscale == null )
        {
            fscale = new double[n];
            for( int i = 0; i < n; i++ )
                fscale[i] = 1;
        }

        conseqStepsOfMaxSize = 0;

        constraintsSet = ( constraints != null );

        /* check the initial guess uu against the constraints */
        if( constraintsSet )
        {
            if( !VectorUtils.constrMask(constraints, u, new double[n]) )
            {
                throw new IllegalArgumentException("Initial guess does not meet constraints.");
            }
        }

        /* all error checking is complete at this point */
        if( debagLevel > 0 )
            printInfo("scsteptol = " + stepTolerance + "fnormtol = " + fTolerance);

        /* calculate the default value for mxnewtstep (maximum Newton step) */
        if( mxnewtstep == 0 )
            mxnewtstep = 1000 * VectorUtils.l2Norm(u, uscale);
        if( mxnewtstep < 1.0 )
            mxnewtstep = 1.0;


        /* additional set-up for inexact linear solvers */

        if( inexactLs )
        {
            /* set up the coefficients for the eta calculation */
            callForcingTerm = ( etaflag != ETACONSTANT );

            /* this value is always used for choice #1 */
            if( etaflag == ETACHOICE1 )
                etaAlpha = ( 1.0 + Math.sqrt(5) ) * 0.5;

            /* initial value for eta set to 0.5 for other than the ETACONSTANT option */
            if( etaflag != ETACONSTANT )
                eta = 0.5;

            /* disable residual monitoring if using an inexact linear solver */
            noResMon = true;
        }
        else
        {
            callForcingTerm = false;
        }

        /* initialize counters */
        modelFunctionCallNumber = nnilset = nnilset_sub = iterationsNumber = nbcf = backTrackNumber = 0;

        /* see if the system func(uu) = 0 is satisfied by the initial guess uu */
        int flag = func(u, fValue);
        modelFunctionCallNumber++;
        if( flag < 0 )
            return SYSFUNC_FAIL;
        else if( flag > 0 )
            return FIRST_SYSFUNC_ERR;

        double fmax = cFNorm(fValue, fscale);

        if( debagLevel > 1 )
            printInfo("fmax = " + fmax);

        if( fmax <= (/* 0.01 */ fTolerance ) )//TODO: check why this "0.1" is in the original KINSOL code
            return INITIAL_GUESS_OK;

        /* initialize the L2 (Euclidean) norms of f for the linear iteration steps */
        fnorm = VectorUtils.l2Norm(fValue, fscale);
        f1norm = 0.5 * fnorm * fnorm;

        fnormSub = fnorm;

        if( debagLevel > 0 )
            printInfo("nni = " + iterationsNumber + " nfe = " + modelFunctionCallNumber + "fnorm = " + fnorm);

        /* problem has now been successfully initialized */
        return SUCCESS;
    }


    /**
     * This routine handles the process of solving for the approximate
     * solution of the Newton equations in the Newton iteration.
     * Subsequent routines handle the nonlinear aspects of its
     * application.
     */
    private int approxSolution()
    {

        int retval;

        if( ( iterationsNumber - nnilset ) >= msbset )
        {
            sthrsh = 2;
            updateFnormSub = true;
        }

        for( ;; )
        {
            jacCurrent = false;

            if( ( sthrsh > 1.5 ) && setupNonNull )
            {
                retval = setup();
                jacCurrent = true;
                nnilset = iterationsNumber;
                nnilset_sub = iterationsNumber;
                if( retval != 0 )
                    return LSETUP_FAIL;
            }

            /* load b with the current value of -fval */
            VectorUtils.scale( -1.0, fValue, uNew);

            /* call the generic 'lsolve' routine to solve the system Jx = b */
            retval = solve(uDelta, uNew);

            if( retval == 0 )
                return SUCCESS;
            else if( retval < 0 )
                return LSOLVE_FAIL;
            else if( ( !setupNonNull ) || ( jacCurrent ) )
                return LINSOLV_NO_RECOVERY;

            /* loop back only if the linear solver setup is in use and Jacobian information
               is not current */
            sthrsh = 2;
        }
    }
    /**
     * This routine is the main driver for the Full Newton
     * algorithm. Its purpose is to compute unew = uu + pp in the
     * direction pp from uu, taking the full Newton step. The
     * step may be constrained if the constraint conditions are
     * violated, or if the norm of pp is greater than mxnewtstep.
     */
    private int newton()
    {
        double pnorm, ratio;
        boolean fOK;
        int ircvr, retval;

        maxStepTaken = false;
        pnorm = VectorUtils.l2Norm(uDelta, uscale);
        ratio = 1.0;
        if( pnorm > mxnewtstep )
        {
            ratio = mxnewtstep / pnorm;
            VectorUtils.scale(ratio, uDelta);
            pnorm = mxnewtstep;
        }

        if( debagLevel > 0 )
            printInfo("pnorm = " + pnorm);

        /* If constraints are active, then constrain the step accordingly */

        stepl = pnorm;
        stepmul = 1.0;
        if( constraintsSet )
        {
            retval = checkConstraint();
            if( retval == CONSTR_VIOLATED )
            {
                /* Apply stepmul set in KINConstraint */
                ratio *= stepmul;
                VectorUtils.scale(stepmul, uDelta);
                pnorm *= stepmul;
                stepl = pnorm;
                if( debagLevel > 0 )
                    printInfo("pnorm = " + pnorm);
                if( pnorm <= stepTolerance )
                    return STEP_TOO_SMALL;
            }
        }

        /* Attempt (at most MAX_RECVR times) to evaluate function at the new iterate */
        fOK = false;

        for( ircvr = 1; ircvr <= MAX_RECVR; ircvr++ )
        {
            /* compute the iterate unew = uu + pp */
            VectorUtils.linearSum(u, uDelta, uNew);

            /* evaluate func(unew) and its norm, and return */
            retval = func(uNew, fValue);
            modelFunctionCallNumber++;

            /* if func was successful, accept pp */
            if( retval == 0 )
            {
                fOK = true;
                break;
            }

            /* if func failed unrecoverably, give up */
            else if( retval < 0 )
                return SYSFUNC_FAIL;

            /* func failed recoverably; cut step in half and try again */
            ratio *= 0.5;
            VectorUtils.scale(0.5, uDelta);
            pnorm *= 0.5;
            stepl = pnorm;
        }

        /* If func() failed recoverably MAX_RECVR times, give up */
        if( !fOK )
            return REPTD_SYSFUNC_ERR;

        /* Evaluate function norms */
        fnormp = VectorUtils.l2Norm(fValue, fscale);
        f1normp = 0.5 * fnormp * fnormp;

        /* scale sfdotJp and sJpnorm by ratio for later use in KINForcingTerm */
        sfdotJp *= ratio;
        sJpnorm *= ratio;

        if( debagLevel > 1 )
            printInfo("fnormp = " + fnormp);

        if( pnorm > ( 0.99 * mxnewtstep ) )
            maxStepTaken = true;

        return SUCCESS;
    }

    /**
     * This routine implements the LineSearch algorithm.
     * Its purpose is to find unew = uu + rl * pp in the direction pp
     * from uu so that:
     *                                    t
     *  func(unew) <= func(uu) + alpha * g  (unew - uu) (alpha = 1.e-4)
     *
     *    and
     *                                   t
     *  func(unew) >= func(uu) + beta * g  (unew - uu) (beta = 0.9)
     *
     * where 0 < rlmin <= rl <= rlmax.
     *
     * Note:
     *             mxnewtstep
     *  rlmax = ----------------   if uu+pp is feasible
     *          ||uscale*pp||_L2
     *
     *  rlmax = 1   otherwise
     *
     *    and
     *
     *                 scsteptol
     *  rlmin = --------------------------
     *          ||           pp         ||
     *          || -------------------- ||_L-infinity
     *          || (1/uscale + ABS(uu)) ||
     *
     *
     * If the system function fails unrecoverably at any time, KINLineSearch
     * returns SYSFUNC_FAIL which will halt the solver.
     *
     * We attempt to corect recoverable system function failures only before
     * the alpha-condition loop; i.e. when the solution is updated with the
     * full Newton step (possibly reduced due to constraint violations).
     * Once we find a feasible pp, we assume that any update up to pp is
     * feasible.
     * 
     * If the step size is limited due to constraint violations and/or
     * recoverable system function failures, we set rlmax=1 to ensure
     * that the update remains feasible during the attempts to enforce
     * the beta-condition (this is not an isse while enforcing the alpha
     * condition, as rl can only decrease from 1 at that stage)
     */
    private int lineSearch()
    {
        double pnorm, ratio, slpi, rlmin, rlength, rl, rlmax, rldiff;
        double rltmp, rlprev, pt1trl, f1nprv, rllo, rlinc, alpha, beta;
        double alpha_cond, beta_cond, rl_a, tmp1, rl_b, tmp2, disc;
        int ircvr, nbktrk_l, retval;
        boolean firstBacktrack, fOK;

        /* Initializations */
        nbktrk_l = 0; /* local backtracking counter */
        ratio = 1.0; /* step change ratio          */
        alpha = 0.0001;
        beta = 0.9;

        firstBacktrack = true;
        maxStepTaken = false;

        rlprev = f1nprv = 0;

        /* Compute length of Newton step */
        pnorm = VectorUtils.l2Norm(uDelta, uscale);
        rlmax = mxnewtstep / pnorm;
        stepl = pnorm;

        /* If the full Newton step is too large, set it to the maximum allowable value */
        if( pnorm > mxnewtstep )
        {
            ratio = mxnewtstep / pnorm;
            VectorUtils.scale(ratio, uDelta);
            pnorm = mxnewtstep;
            rlmax = 1.0;
            stepl = pnorm;
        }

        /* If constraint checking is activated, check and correct violations */
        stepmul = 1.0;

        if( constraintsSet )
        {
            retval = checkConstraint();
            if( retval == CONSTR_VIOLATED )
            {
                /* Apply stepmul set in KINConstraint */
                VectorUtils.scale(stepmul, uDelta);
                ratio *= stepmul;
                pnorm *= stepmul;
                rlmax = 1.0;
                stepl = pnorm;
                if( debagLevel > 0 )
                    printInfo("pnorm = " + pnorm);
                if( pnorm <= stepTolerance )
                    return ( STEP_TOO_SMALL );
            }
        }

        /* Attempt (at most MAX_RECVR times) to evaluate function at the new iterate */
        fOK = false;

        for( ircvr = 1; ircvr <= MAX_RECVR; ircvr++ )
        {
            /* compute the iterate unew = uu + pp */
            VectorUtils.linearSum(u, uDelta, uNew);

            /* evaluate func(unew) and its norm, and return */
            retval = func(uNew, fValue);
            modelFunctionCallNumber++;

            /* if func was successful, accept pp */
            if( retval == 0 )
            {
                fOK = true;
                break;
            }

            /* if func failed unrecoverably, give up */
            else if( retval < 0 )
                return SYSFUNC_FAIL;

            /* func failed recoverably; cut step in half and try again */
            VectorUtils.scale(0.5, uDelta);
            ratio *= 0.5;
            pnorm *= 0.5;
            rlmax = 1.0;
            stepl = pnorm;

        }

        /* If func() failed recoverably MAX_RECVR times, give up */
        if( !fOK )
            return REPTD_SYSFUNC_ERR;

        /* Evaluate function norms */
        fnormp = VectorUtils.l2Norm(fValue, fscale);
        f1normp = 0.5 * fnormp * fnormp;

        /* Estimate the line search value rl (lambda) to satisfy both ALPHA and BETA conditions */

        slpi = sfdotJp * ratio;
        rlength = cSNorm(uDelta, u);
        rlmin = stepTolerance / rlength;
        rl = 1.0;

        if( debagLevel > 2 )
            printInfo("rlmin = " + rlmin + " f1norm = " + f1norm + " pnorm = " + pnorm);

        /* Loop until the ALPHA condition is satisfied. Terminate if rl becomes too small */
        for( ;; )
        {
            /* Evaluate test quantity */
            alpha_cond = f1norm + ( alpha * slpi * rl );

            if( debagLevel > 2 )
                printInfo("fnormp = " + fnormp + " f1normp = " + f1normp + " alpha_cond = " + alpha_cond + " r1 = " + rl);

            /* If ALPHA condition is satisfied, break out from loop */
            if( f1normp <= alpha_cond )
                break;

            /* Backtracking. Use quadratic fit the first time and cubic fit afterwards. */
            if( firstBacktrack )
            {
                rltmp = -slpi / ( 2 * ( f1normp - f1norm - slpi ) );
                firstBacktrack = false;
            }
            else
            {
                tmp1 = f1normp - f1norm - ( rl * slpi );
                tmp2 = f1nprv - f1norm - ( rlprev * slpi );
                rl_a = ( ( 1.0 / ( rl * rl ) ) * tmp1 ) - ( ( 1.0 / ( rlprev * rlprev ) ) * tmp2 );
                rl_b = ( ( -rlprev / ( rl * rl ) ) * tmp1 ) + ( ( rl / ( rlprev * rlprev ) ) * tmp2 );
                tmp1 = 1.0 / ( rl - rlprev );
                rl_a *= tmp1;
                rl_b *= tmp1;
                disc = ( rl_b * rl_b ) - ( 3 * rl_a * slpi );

                if( Math.abs(rl_a) < UNIT_ROUNDOFF ) // cubic is actually just a quadratic (rl_a ~ 0)
                    rltmp = -slpi / ( 2 * rl_b );
                else
                    // real cubic
                    rltmp = ( -rl_b + Math.sqrt(disc) ) / ( 3 * rl_a );

                rltmp = Math.min(rltmp, 0.5 * rl);
            }

            /* Set new rl (do not allow a reduction by a factor larger than 10) */
            rlprev = rl;
            f1nprv = f1normp;
            pt1trl = 0.1 * rl;
            rl = Math.max(pt1trl, rltmp);
            nbktrk_l++;

            /* Update unew and re-evaluate function */
            VectorUtils.linearSum(u, rl, uDelta, uNew);

            retval = func(uNew, fValue);
            modelFunctionCallNumber++;
            if( retval != 0 )
                return SYSFUNC_FAIL;

            fnormp = VectorUtils.l2Norm(fValue, fscale);
            f1normp = 0.5 * fnormp * fnormp;

            /* Check if rl (lambda) is too small */
            if( rl < rlmin )
            {
                /* unew sufficiently distinct from uu cannot be found.
                   copy uu into unew (step remains unchanged) and
                   return STEP_TOO_SMALL */
                VectorUtils.copy(u, uNew);
                return STEP_TOO_SMALL;
            }

        } /* end ALPHA condition loop */


        /* ALPHA condition is satisfied. Now check the BETA condition */
        beta_cond = f1norm + ( beta * slpi * rl );

        if( f1normp < beta_cond )
        {
            /* BETA condition not satisfied */
            if( ( rl == 1.0 ) && ( pnorm < mxnewtstep ) )
            {
                do
                {
                    rlprev = rl;
                    f1nprv = f1normp;
                    rl = Math.min( ( 2 * rl ), rlmax);
                    nbktrk_l++;

                    VectorUtils.linearSum(u, rl, uDelta, uNew);
                    retval = func(uNew, fValue);
                    modelFunctionCallNumber++;
                    if( retval != 0 )
                        return ( SYSFUNC_FAIL );
                    fnormp = VectorUtils.l2Norm(fValue, fscale);
                    f1normp = 0.5 * fnormp * fnormp;

                    alpha_cond = f1norm + ( alpha * slpi * rl );
                    beta_cond = f1norm + ( beta * slpi * rl );

                    if( debagLevel > 2 )
                        printInfo("f1normp = " + f1normp + " beta_cond = " + beta_cond + " r1 = " + rl);

                }
                while( ( f1normp <= alpha_cond ) && ( f1normp < beta_cond ) && ( rl < rlmax ) );

            } /* enf if (rl == 1.0) block */
            if( ( rl < 1.0 ) || ( ( rl > 1.0 ) && ( f1normp > alpha_cond ) ) )
            {
                rllo = Math.min(rl, rlprev);
                rldiff = Math.abs(rlprev - rl);

                do
                {
                    rlinc = 0.5 * rldiff;
                    rl = rllo + rlinc;
                    nbktrk_l++;

                    VectorUtils.linearSum(u, rl, uDelta, uNew);
                    retval = func(uNew, fValue);
                    modelFunctionCallNumber++;
                    if( retval != 0 )
                        return ( SYSFUNC_FAIL );
                    fnormp = VectorUtils.l2Norm(fValue, fscale);
                    f1normp = 0.5 * fnormp * fnormp;

                    alpha_cond = f1norm + ( alpha * slpi * rl );
                    beta_cond = f1norm + ( beta * slpi * rl );

                    if( debagLevel > 2 )
                        printInfo("f1normp = " + f1normp + " alpha_cond = " + alpha_cond + " beta_cond = " + beta_cond + " r1 = " + rl);

                    if( f1normp > alpha_cond )
                        rldiff = rlinc;
                    else if( f1normp < beta_cond )
                    {
                        rllo = rl;
                        rldiff = rldiff - rlinc;
                    }
                }
                while( ( f1normp > alpha_cond ) || ( ( f1normp < beta_cond ) && ( rldiff >= rlmin ) ) );

                if( f1normp < beta_cond )
                {
                    /* beta condition could not be satisfied so set unew to last u value
                       that satisfied the alpha condition and continue */
                    VectorUtils.linearSum(u, rllo, uDelta, uNew);
                    retval = func(uNew, fValue);
                    modelFunctionCallNumber++;
                    if( retval != 0 )
                        return SYSFUNC_FAIL;
                    fnormp = VectorUtils.l2Norm(fValue, fscale);
                    f1normp = 0.5 * fnormp * fnormp;

                    /* increment beta-condition failures counter */

                    nbcf++;
                }

            } /* end of if (rl < 1.0) block */

        } /* end of if (f1normp < beta_cond) block */

        /* Update number of backtracking operations */
        backTrackNumber += nbktrk_l;

        if( debagLevel > 1 )
            printInfo("nbktrk_l = " + nbktrk_l);

        /* scale sfdotJp and sJpnorm by rl * ratio for later use in KINForcingTerm */
        double f = rl * ratio;
        sfdotJp *= f;
        sJpnorm *= f;

        if( ( rl * pnorm ) > ( 0.99 * mxnewtstep ) )
            maxStepTaken = true;

        return SUCCESS;
    }
    /**
     * This routine checks if the proposed solution vector uu + pp
     * violates any constraints. If a constraint is violated, then the
     * scalar stepmul is determined such that uu + stepmul * pp does
     * not violate any constraints.
     *
     * Note: This routine is called by the functions
     *       KINLineSearch and KINFullNewton.
     */
    private int checkConstraint()
    {
        double[] vtemp1 = new double[n];
        double[] vtemp2 = new double[n];
        VectorUtils.linearSum(u, uDelta, vtemp1);

        /* if vtemp1[i] violates constraint[i] then vtemp2[i] = 1
           else vtemp2[i] = 0 (vtemp2 is the mask vector) */
        if( VectorUtils.constrMask(constraints, vtemp1, vtemp2) )
            return SUCCESS;

        /* vtemp1[i] = ABS(pp[i]) */
        VectorUtils.abs(uDelta, vtemp1);

        /* consider vtemp1[i] only if vtemp2[i] = 1 (constraint violated) */
        VectorUtils.prod(vtemp2, vtemp1, vtemp1);

        VectorUtils.abs(u, vtemp2);
        stepmul = 0.9 * VectorUtils.minQuotient(vtemp2, vtemp1);

        return CONSTR_VIOLATED;
    }

    /**
     * This routine checks the current iterate unew to see if the
     * system f(uNew) = 0 is satisfied by a variety of tests.
     *
     * <li>strategy is one of NONE or LINESEARCH
     * <li>sflag    is one of SUCCESS, STEP_TOO_SMALL
     */
    private int checkSolution(int sflag)
    {
        double fmax, rlength;
        double[] delta;

        retryIteration = false;

        /* Check for too small a step */
        if( sflag == STEP_TOO_SMALL )
        {
            if( setupNonNull && !jacCurrent )
            {
                /* If the Jacobian is out of date, update it and retry */
                sthrsh = 2;
                return CONTINUE_ITERATIONS;
            }
            else
            {
                /* Give up */
                if( strategy == NONE )
                    return STEP_LT_STPTOL;
                else
                    return LINESEARCH_NONCONV;
            }
        }

        /* Check tolerance on scaled function norm at the current iterate */
        fmax = cFNorm(fValue, fscale);

        if( debagLevel > 1 )
            printInfo("fmax = " + fmax);

        if( fmax <= fTolerance )
            return SUCCESS;

        /* Check if the scaled distance between the last two steps is too small */
        /* NOTE: pp used as work space to store this distance */
        delta = uDelta;
        VectorUtils.linearDiff(uNew, u, delta);
        rlength = cSNorm(delta, uNew);

        if( rlength <= stepTolerance )
        {
            if( setupNonNull && !jacCurrent )
            {
                /* If the Jacobian is out of date, update it and retry */
                sthrsh = 2;
                return CONTINUE_ITERATIONS;
            }
            else
            {
                /* give up */
                return STEP_LT_STPTOL;
            }
        }

        /* Check if the maximum number of iterations is reached */
        if( iterationsNumber >= mxiter )
            return MAXITER_REACHED;

        /* Check for consecutive number of steps taken of size mxnewtstep
           and if not maxStepTaken, then set ncscmx to 0 */

        conseqStepsOfMaxSize = ( maxStepTaken ) ? conseqStepsOfMaxSize++ : 0;

        if( conseqStepsOfMaxSize == 5 )
            return MXNEWT_5X_EXCEEDED;

        /* Proceed according to the type of linear solver used */
        if( inexactLs )
        {
            /* We're doing inexact Newton.
               Load threshold for reevaluating the Jacobian. */
            sthrsh = rlength;
        }
        else if( !noResMon )
        {
            /* We're doing modified Newton and the user did not disable residual monitoring.
               Check if it is time to monitor residual. */
            if( ( iterationsNumber - nnilset_sub ) >= msbset_sub )
            {
                /* Residual monitoring needed */
                nnilset_sub = iterationsNumber;

                /* If indicated, estimate new OMEGA value */
                if( evalOmega )
                    omega = Math.min(omega_min * Math.exp(Math.max(0, ( fnorm / fTolerance ) - 1.0)), omega_max);

                /* Check if making satisfactory progress */
                if( fnorm > omega * fnormSub )
                {
                    /* Insuficient progress */
                    if( setupNonNull && !jacCurrent )
                    {
                        /* If the Jacobian is out of date, update it and retry */
                        sthrsh = 2;
                        retryIteration = true;
                        return RETRY_ITERATION;
                    }
                    else
                    {
                        /* Otherwise, we cannot do anything, so just return. */
                    }
                }
                else
                {
                    /* Sufficient progress */
                    fnormSub = fnorm;
                    sthrsh = 1.0;
                }
            }
            else
            {
                /* Reset sthrsh */
                if( retryIteration || updateFnormSub )
                    fnormSub = fnorm;
                if( updateFnormSub )
                    updateFnormSub = false;
                sthrsh = 1.0;
            }
        }

        /* if made it to here, then the iteration process is not finished
           so return CONTINUE_ITERATIONS flag */
        return CONTINUE_ITERATIONS;
    }
    /**
     * This routine computes eta, the scaling factor in the linear
     * convergence stopping tolerance eps when choice #1 or choice #2
     * forcing terms are used. Eta is computed here for all but the
     * first iterative step, which is set to the default in routine
     * KINSolInit.
     *
     * This routine was written by Homer Walker of Utah State
     * University with subsequent modifications by Allan Taylor @ LLNL.
     *
     * It is based on the concepts of the paper 'Choosing the forcing
     * terms in an inexact Newton method', SIAM J Sci Comput, 17
     * (1996), pp 16 - 32, or Utah State University Research Report
     * 6/94/75 of the same title.
     */
    private void forcingTerms()
    {
        double eta_max = 0.9;
        double eta_min = 0.0001;
        double eta_safe = 0.5;

        /* choice #1 forcing term */
        if( etaflag == ETACHOICE1 )
        {
            /* compute the norm of f + Jp , scaled L2 norm */
            double linmodel_norm = Math.sqrt( ( fnorm * fnorm ) + ( 2 * sfdotJp ) + ( sJpnorm * sJpnorm ));

            /* form the safeguarded for choice #1 */
            eta_safe = Math.pow(eta, etaAlpha);
            eta = Math.abs(fnormp - linmodel_norm) / fnorm;
        }

        /* choice #2 forcing term */
        if( etaflag == ETACHOICE2 )
        {
            eta_safe = etaGamma * Math.pow(eta, etaAlpha);
            eta = etaGamma * Math.pow( ( fnormp / fnorm ), etaAlpha);
        }

        /* apply safeguards */
        if( eta_safe < 0.1 )
            eta_safe = 0;
        eta = Math.max(eta, eta_safe);
        eta = Math.max(eta, eta_min);
        eta = Math.min(eta, eta_max);

        return;
    }

    /**
     * This routine computes the max norm for scaled vectors. The
     * scaling vector is scale, and the vector of which the norm is to
     * be determined is vv. The returned value, fnormval, is the
     * resulting scaled vector norm. This routine uses double[]
     * functions from the vector module.
     */
    private double cFNorm(double[] v, double[] scale)
    {
        double[] vtemp1 = new double[n];
        VectorUtils.prod(scale, v, vtemp1);
        return VectorUtils.maxNorm(vtemp1);
    }

    /**
     * This routine computes the max norm of the scaled steplength, ss.
     * Here ucur is the current step and usc is the u scale factor.
     */
    private double cSNorm(double[] v, double[] u)
    {
        double[] vtemp1 = new double[n];
        double[] vtemp2 = new double[n];
        VectorUtils.inv(uscale, vtemp1);
        VectorUtils.abs(u, vtemp2);
        VectorUtils.linearSum(vtemp1, vtemp2, vtemp1);
        VectorUtils.divide(v, vtemp1, vtemp1);
        return VectorUtils.maxNorm(vtemp1);
    }

    /**
     * A high level error handling function
     * Based on the value info_code, it composes the info message and
     * passes it to the info handler function.
     */
    private void printInfo(String msg)
    {
        System.out.println("Algebraic KinSolver info: " + msg);
    }

    private void processFlag(int flag)
    {
        printInfo(flagToMessage.get(flag));
    }
}