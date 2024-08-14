package biouml.plugins.agentmodeling._test.models;

import biouml.plugins.simulation.java.JavaBaseModel;

public class CVS1_Karaaslan extends JavaBaseModel
{
    protected double eta_dt_sodreab;
    protected double N_rsna;
    protected double N_als;
    protected double ksi_map;
    protected double R_ea;
    protected double P_gh;
    protected double R_aa;
    protected double alpha_map;
    protected double C_K;
    protected double Fi_u_sod;
    protected double C_sod;
    protected double Fi_co;
    protected double Fi_dt_sodreab;
    protected double C_gcf;
    protected double mu_al;
    protected double P_B;
    protected double Fi_gfilt;
    protected double Fi_rb;
    protected double gamma_at;
    protected double nu_rsna;
    protected double P_ma;
    protected double Fi_cd_sodreab;
    protected double n_eta_pt;
    protected double Fi_t_wreab;
    protected double Fi_pt_sodreab;
    protected double psi_al;
    protected double eta_cd_sodreab;
    protected double P_go;
    protected double C_al;
    protected double sigma_tgf;
    protected double N_rs;
    protected double R_aass;
    protected double R_r;
    protected double alpha_auto;
    protected double rsna;
    protected double N_adhs;
    protected double n_eps_dt;
    protected double n_eta_cd;
    protected double Fi_dt_sod;
    protected double time;
    protected double alpha_chemo;
    protected double T_al;
    protected double gamma_rsna;
    protected double beta_rsna;
    protected double C_adh;
    protected double ksi_at;
    protected double delta_ra;
    protected double nu_md_sod;
    protected double gamma_filsod;
    protected double mu_adh;
    protected double P_ra;
    protected double Fi_sodin;
    protected double C_anp;
    protected double eta_pt_sodreab;
    protected double Fi_filsod;
    protected double T_r;
    protected double V_b;
    protected double P_f;
    protected double T_adh;
    protected double lambda_anp;
    protected double Fi_win;
    protected double alpha_baro;
    protected double ksi_k_sod;
    protected double lambda_dt;
    protected double C_at;
    protected double Fi_u;
    protected double V_large;
    protected double alpha_rap;
    protected double vas_f;
    protected double vas_d;
    protected double K_vd;

    private void calculateScalar()
    {
        C_al = x_values[1] * 85;
        psi_al = 0.17 + 0.94 / ( 1 + Math.exp( ( 0.48 - 1.2 * Math.log(C_al) / Math.log(10) ) / 0.88) );
        eta_dt_sodreab = n_eps_dt * psi_al;
        Fi_dt_sodreab = x_values[2] * eta_dt_sodreab;
        C_at = 20 * x_values[6];
        R_ea = 51.66 * ( 0.9432 + 0.1363 / ( 0.2069 + Math.exp(3.108 - 1.785 * Math.log(C_at) / Math.log(10)) ) );
        sigma_tgf = 0.3408 + 3.449 / ( 3.88 + Math.exp( ( x_values[2] - 3.859 ) / -0.9617) );
        alpha_map = 0.5 + 1.1 / ( 1 + Math.exp( ( P_ma - 100 ) / 15) );
        P_ra = 0.2787 * Math.exp(0.2281 * Fi_co);
        alpha_rap = 1 - 0.0080 * P_ra;
        rsna = N_rsna * alpha_map * alpha_rap;
        beta_rsna = 1.5 * ( rsna - 1 ) + 1;
        R_aa = R_aass * beta_rsna * sigma_tgf;
        R_r = R_aa + R_ea;
        Fi_dt_sod = x_values[2] - Fi_dt_sodreab;
        C_anp = 7.427 - 6.554 / ( 1 + Math.exp(P_ra - 3.762) );
        lambda_anp = -0.1 * C_anp + 1.1199;
        lambda_dt = 0.82 + 0.39 / ( 1 + Math.exp( ( Fi_dt_sod - 1.6 ) / 2) );
        eta_cd_sodreab = n_eta_cd * lambda_dt * lambda_anp;
        Fi_cd_sodreab = Fi_dt_sod * eta_cd_sodreab;
        Fi_u_sod = Fi_dt_sod - Fi_cd_sodreab;
        gamma_at = 0.95 + 0.12 / ( 1 + Math.exp(2.6 - 1.8 * Math.log(C_at) / Math.log(10)) );
        Fi_rb = P_ma / R_r;
        P_gh = P_ma - Fi_rb * R_aa;
        P_f = P_gh - P_B - P_go;
        Fi_gfilt = P_f * C_gcf;
        C_sod = x_values[0] / x_values[5];
        Fi_filsod = Fi_gfilt * C_sod;
        gamma_filsod = 0.8 + 0.3 / ( 1 + Math.exp(Fi_filsod - 14) / 138 );
        gamma_rsna = 0.5 + 0.7 / ( 1 + Math.exp(1 - rsna) / 2.18 );
        eta_pt_sodreab = n_eta_pt * gamma_filsod * gamma_at * gamma_rsna;
        Fi_pt_sodreab = Fi_filsod * eta_pt_sodreab;
        C_adh = 4 * x_values[8];
        delta_ra = 0.2 * P_ra - 7.0E-4 * x_values[3];
        N_adhs = Math.max(0, C_sod - 141 + Math.max(0, x_values[7] - 1) - delta_ra) / 3;
        mu_adh = 0.37 + 0.8 / ( 1 + Math.exp(0.6 - 3.7 * Math.log(C_adh) / Math.log(10)) );
        mu_al = 0.17 + 0.94 / ( 1 + Math.exp( ( 0.48 - 1.2 * Math.log(C_al) / Math.log(10) ) / 0.88) );
        Fi_t_wreab = 0.025 - 0.0010 / ( mu_al * mu_adh ) + 0.8 * Fi_gfilt;
        Fi_u = Fi_gfilt - Fi_t_wreab;
        Fi_win = 0.0080 / ( 1 + 1.822 * Math.pow(C_adh, -1.607) ) - 0.0053;
        alpha_auto = 3.079 * Math.exp( -P_ma * 0.011);
        alpha_chemo = 0.25 * alpha_auto;
        alpha_baro = 0.75 * alpha_auto - x_values[4];
        V_b = 4.560227 + 2.431217 / ( 1 + Math.exp( - ( x_values[5] - 18.11278 ) * 0.47437) );

        double piecewise_0 = 0;
        if( P_ma < 100 )
        {
            piecewise_0 = 69.03 * Math.exp( -0.0425 * P_ma);
        }
        else
        {
            piecewise_0 = 1;
        }
        ksi_map = piecewise_0;
        ksi_at = 0.4 + 2.4 / ( 1 + Math.exp(2.82 - 1.5 * Math.log(C_at) / Math.log(10) / 0.8) );
        C_K = 5;
        ksi_k_sod = C_K / ( 0.003525 * C_sod ) - 9;
        N_als = ksi_k_sod * ksi_map * ksi_at;
        nu_md_sod = 0.2262 + 28.04 / ( 11.56 + Math.exp( ( x_values[2] - 1.667 ) / 0.6056) );
        nu_rsna = 1.89 - 2.056 / ( 1.358 + Math.exp(rsna - 0.8667) );
        N_rs = nu_md_sod * nu_rsna;
        V_large = 1000 * ( V_b - 1.5 );
        vas_f = 11.312 * Math.exp( -Fi_co * 0.4799) / 100000;
        vas_d = x_values[9] * K_vd;
    }
    
    @Override
    public double[] dy_dt(double time, double[] x_values) throws Exception
    {
        this.time = time;
        this.x_values = x_values;
        final double[] dydt = new double[10];
        calculateScalar();

        dydt[0] = +Fi_sodin - Fi_u_sod;
        dydt[1] = + ( N_als - x_values[1] ) / T_al;
        dydt[2] = +Fi_filsod - Fi_pt_sodreab - x_values[2];
        dydt[3] = +delta_ra;
        dydt[4] = +5.0E-4 * ( alpha_baro - 0.75 );
        dydt[5] = +Fi_win - Fi_u;
        dydt[6] = + ( N_rs - x_values[6] ) / T_r;
        dydt[7] = +alpha_chemo + alpha_baro - x_values[7];
        dydt[8] = + ( N_adhs - x_values[8] ) / T_adh;
        dydt[9] = +vas_f - vas_d;
        return dydt;
    }
    @Override
    public void init() throws Exception
    {
        alpha_auto = 1.0201358713435358;
        alpha_baro = 0.7500202044462966;
        alpha_chemo = 0.25503396783588395;
        alpha_map = 1.0422187803533614;
        alpha_rap = 0.9900409442660595;
        beta_rsna = 1.047758898149294;
        C_adh = 3.259066450268863;
        C_al = 112.10432304688872;
        C_anp = 1.36236634608621;
        C_at = 25.147678735931343;
        C_gcf = 0.00781;
        C_K = 5.0;
        C_sod = 143.4392369420559;
        delta_ra = -1.26542179021083E-4;
        eta_cd_sodreab = 0.925006485628931;
        eta_dt_sodreab = 0.5101648699125243;
        eta_pt_sodreab = 0.8088492548738926;
        Fi_cd_sodreab = 1.5530829004980748;
        Fi_co = 6.5614207105974955;
        Fi_dt_sod = 1.6789967688087093;
        Fi_dt_sodreab = 1.748680557047586;
        Fi_filsod = 17.931792210394423;
        Fi_gfilt = 0.12501315952787867;
        Fi_pt_sodreab = 14.504116767931;
        Fi_rb = 1.1885348713749648;
        Fi_sodin = 0.126;
        Fi_t_wreab = 0.12402893919618398;
        Fi_u = 9.842203316946935E-4;
        Fi_u_sod = 0.12591386831063445;
        Fi_win = 9.848488071196593E-4;
        gamma_at = 1.0076281234796054;
        gamma_filsod = 1.0190495945624287;
        gamma_rsna = 0.984650264322122;
        K_vd = 1.0E-5;
        ksi_at = 1.4839242147575504;
        ksi_k_sod = 0.888784593054222;
        ksi_map = 1.0;
        lambda_anp = 0.9836633653913789;
        lambda_dt = 1.0111494081237693;
        mu_adh = 0.9984585164029149;
        mu_al = 1.0203297398250486;
        N_adhs = 0.8148024491218141;
        N_als = 1.3188889793365954;
        n_eps_dt = 0.5;
        n_eta_cd = 0.93;
        n_eta_pt = 0.8;
        N_rs = 1.2579383781380222;
        N_rsna = 1.0;
        nu_md_sod = 1.1650036857774624;
        nu_rsna = 1.0797720157413409;
        P_B = 18.0;
        P_f = 16.006806597679727;
        P_gh = 62.00680659767973;
        P_go = 28.0;
        P_ma = 100.42445848344819;
        P_ra = 1.244881966742571;
        psi_al = 1.0203297398250486;
        R_aa = 32.323537837240515;
        R_aass = 31.67;
        R_ea = 52.17079287370569;
        R_r = 84.4943307109462;
        rsna = 1.0318392654328628;
        sigma_tgf = 0.9741132874862677;
        T_adh = 6.0;
        T_al = 30.0;
        T_r = 15.0;
        time = 0.0;
        V_b = 5.059407414056776;
        V_large = 3559.407414056776;
        vas_d = 4.973797606697772E-6;
        vas_f = 4.853127794321516E-6;
        initialValues = getInitialValues();
        this.isInit = true;
    }

    @Override
    public double[] getInitialValues() throws Exception
    {
        if( !this.isInit )
        {
            this.x_values = new double[10];
            this.time = 0.0;
            x_values[0] = 2188.8558074128537; //  initial value of M_sod
            x_values[1] = 1.318874388786926; //  initial value of N_al
            x_values[2] = 3.4276773258562954; //  initial value of Fi_md_sod
            x_values[3] = 355.86133646790756; //  initial value of temp
            x_values[4] = 0.015081699061355223; //  initial value of delta_baro
            x_values[5] = 15.25981212725685;//15.669696859190438; //  initial value of V_ecf
            x_values[6] = 1.2573839367965671;//0.9694502918561186; //  initial value of C_r
            x_values[7] = 1.0050438631305076;//1.0011821194226922; //  initial value of eps_aum
            x_values[8] = 0.8147666125672157;//0.8800104573956468; //  initial value of N_adh
            x_values[9] = 0.4973797606697772;//0.9951739678811101; //  initial value of vas
            calculateScalar();

            return x_values;
        }
        else
            return initialValues;
    }
    @Override
    public double[] extendResult(double time, double[] x_values) throws Exception
    {
        this.time = time;
        this.x_values = x_values;
        calculateScalar();

        double[] yv0 = new double[81];
        yv0[0] = eta_dt_sodreab;
        yv0[1] = N_rsna;
        yv0[2] = N_als;
        yv0[3] = ksi_map;
        yv0[4] = R_ea;
        yv0[5] = P_gh;
        yv0[6] = x_values[0];
        yv0[7] = R_aa;
        yv0[8] = alpha_map;
        yv0[9] = C_K;
        yv0[10] = Fi_u_sod;
        yv0[11] = C_sod;
        yv0[12] = Fi_co;
        yv0[13] = x_values[1];
        yv0[14] = Fi_dt_sodreab;
        yv0[15] = C_gcf;
        yv0[16] = mu_al;
        yv0[17] = x_values[2];
        yv0[18] = P_B;
        yv0[19] = Fi_gfilt;
        yv0[20] = x_values[3];
        yv0[21] = Fi_rb;
        yv0[22] = gamma_at;
        yv0[23] = nu_rsna;
        yv0[24] = P_ma;
        yv0[25] = Fi_cd_sodreab;
        yv0[26] = n_eta_pt;
        yv0[27] = Fi_t_wreab;
        yv0[28] = x_values[4];
        yv0[29] = Fi_pt_sodreab;
        yv0[30] = x_values[5];
        yv0[31] = psi_al;
        yv0[32] = eta_cd_sodreab;
        yv0[33] = P_go;
        yv0[34] = C_al;
        yv0[35] = sigma_tgf;
        yv0[36] = N_rs;
        yv0[37] = R_aass;
        yv0[38] = R_r;
        yv0[39] = x_values[6];
        yv0[40] = alpha_auto;
        yv0[41] = rsna;
        yv0[42] = N_adhs;
        yv0[43] = n_eps_dt;
        yv0[44] = n_eta_cd;
        yv0[45] = Fi_dt_sod;
        yv0[46] = x_values[7];
        yv0[47] = time;
        yv0[48] = alpha_chemo;
        yv0[49] = T_al;
        yv0[50] = gamma_rsna;
        yv0[51] = beta_rsna;
        yv0[52] = C_adh;
        yv0[53] = ksi_at;
        yv0[54] = delta_ra;
        yv0[55] = nu_md_sod;
        yv0[56] = gamma_filsod;
        yv0[57] = mu_adh;
        yv0[58] = P_ra;
        yv0[59] = Fi_sodin;
        yv0[60] = C_anp;
        yv0[61] = eta_pt_sodreab;
        yv0[62] = Fi_filsod;
        yv0[63] = T_r;
        yv0[64] = V_b;
        yv0[65] = x_values[8];
        yv0[66] = P_f;
        yv0[67] = T_adh;
        yv0[68] = lambda_anp;
        yv0[69] = Fi_win;
        yv0[70] = alpha_baro;
        yv0[71] = ksi_k_sod;
        yv0[72] = lambda_dt;
        yv0[73] = C_at;
        yv0[74] = Fi_u;
        yv0[75] = V_large;
        yv0[76] = alpha_rap;
        yv0[77] = K_vd;
        yv0[78] = vas_f;
        yv0[79] = vas_d;
        yv0[80] = x_values[9];
        return yv0;
    }
}