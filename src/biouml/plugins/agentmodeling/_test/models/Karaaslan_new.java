

package biouml.plugins.agentmodeling._test.models;

import biouml.plugins.simulation.java.JavaBaseModel;

public class Karaaslan_new extends JavaBaseModel
{
    protected double eta_dt_sodreab;
    protected double N_rsna;
    protected double N_als;
    protected double ksi_map;
    protected double R_ea;
    protected double P_gh;
    protected double R_ba;
    protected double R_vr;
    protected double R_aa;
    protected double alpha_map;
    protected double Fi_u_sod;
    protected double C_sod;
    protected double P_go;
    protected double Fi_dt_sodreab;
    protected double mu_al;
    protected double P_B;
    protected double P_ma;
    protected double eps_aum;
    protected double Fi_gfilt;
    protected double Fi_rb;
    protected double vas_d;
    protected double gamma_at;
    protected double nu_rsna;
    protected double Fi_cd_sodreab;
    protected double K_bar;
    protected double Fi_t_wreab;
    protected double Fi_pt_sodreab;
    protected double R_bv;
    protected double psi_al;
    protected double eta_cd_sodreab;
    protected double P_mf;
    protected double R_a;
    protected double C_al;
    protected double sigma_tgf;
    protected double Fi_md_sod;
    protected double N_rs;
    protected double R_r;
    protected double alpha_auto;
    protected double rsna;
    protected double N_adhs;
    protected double n_eps_dt;
    protected double n_eta_cd;
    protected double C_K;
    protected double Fi_dt_sod;
    protected double time;
    protected double alpha_chemo;
    protected double T_al;
    protected double gamma_rsna;
    protected double beta_rsna;
    protected double C_adh;
    protected double K_vd;
    protected double ksi_at;
    protected double R_tp;
    protected double delta_ra;
    protected double C_gcf;
    protected double nu_md_sod;
    protected double gamma_filsod;
    protected double Fi_co;
    protected double mu_adh;
    protected double P_ra;
    protected double Fi_sodin;
    protected double C_anp;
    protected double n_eta_pt;
    protected double eta_pt_sodreab;
    protected double Fi_filsod;
    protected double T_r;
    protected double V_b;
    protected double P_f;
    protected double vas_f;
    protected double T_adh;
    protected double lambda_anp;
    protected double Fi_win;
    protected double alpha_baro;
    protected double ksi_k_sod;
    protected double R_aass;
    protected double lambda_dt;
    protected double C_at;
    protected double Fi_u;
    protected double alpha_rap;
    protected double[] x_values;
    private void calculateScalar()
    {

        C_al = x_values[1] * 85;
        psi_al = 0.17 + 0.94 / ( 1 + Math.exp( ( 0.48 - 1.2 * Math.log(C_al) / Math.log(10) ) / 0.88) );
        eta_dt_sodreab = n_eps_dt * psi_al;
        sigma_tgf = 0.3408 + 3.449 / ( 3.88 + Math.exp( ( Fi_md_sod - 3.859 ) / -0.9617) );
        R_bv = 3.4;
        eps_aum = 3.079 * Math.exp( -P_ma * 0.011) - x_values[3];
        R_ba = K_bar / x_values[6];
        R_a = R_ba * eps_aum;
        R_tp = R_a + R_bv;
        V_b = 4.5602271 + 2.4312171 / ( 1 + Math.exp( - ( x_values[4] - 18.11278 ) * 0.47437) );
        P_mf = ( 7.436 * V_b - 30.18 ) * eps_aum;
        P_ra = 0.2787 * Math.exp(0.2281 * Fi_co);
        R_vr = ( 8 * R_bv + R_a ) / 31;
        Fi_co = ( P_mf - P_ra ) / R_vr;
        P_ma = Fi_co * R_tp;
        alpha_map = 0.5 + 1.1 / ( 1 + Math.exp( ( P_ma - 100 ) / 15) );
        alpha_rap = 1 - 0.0080 * P_ra;
        rsna = N_rsna * alpha_map * alpha_rap;
        beta_rsna = 1.5 * ( rsna - 1 ) + 1;
        R_aa = R_aass * beta_rsna * sigma_tgf;
        C_at = 20 * x_values[5];
        R_ea = 51.66 * ( 0.9432 + 0.1363 / ( 0.2069 + Math.exp(3.108 - 1.785 * Math.log(C_at) / Math.log(10)) ) );
        R_r = R_aa + R_ea;
        Fi_rb = P_ma / R_r;
        P_gh = P_ma - Fi_rb * R_aa;
        P_f = P_gh - P_B - P_go;
        Fi_gfilt = P_f * C_gcf;
        C_sod = x_values[0] / x_values[4];
        Fi_filsod = Fi_gfilt * C_sod;
        gamma_at = 0.95 + 0.12 / ( 1 + Math.exp(2.6 - 1.8 * Math.log(C_at) / Math.log(10)) );
        gamma_filsod = 0.8 + 0.3 / ( 1 + Math.exp(Fi_filsod - 14) / 138 );
        gamma_rsna = 0.5 + 0.7 / ( 1 + Math.exp(1 - rsna) / 2.18 );
        eta_pt_sodreab = n_eta_pt * gamma_filsod * gamma_at * gamma_rsna;
        Fi_pt_sodreab = Fi_filsod * eta_pt_sodreab;
        Fi_md_sod = Fi_filsod - Fi_pt_sodreab;
        Fi_dt_sodreab = Fi_md_sod * eta_dt_sodreab;
        Fi_dt_sod = Fi_md_sod - Fi_dt_sodreab;
        C_anp = 7.427 - 6.554 / ( 1 + Math.exp(P_ra - 3.762) );
        lambda_anp = -0.1 * C_anp + 1.1199;
        lambda_dt = 0.82 + 0.39 / ( 1 + Math.exp( ( Fi_dt_sod - 1.6 ) / 2) );
        eta_cd_sodreab = n_eta_cd * lambda_dt * lambda_anp;
        Fi_cd_sodreab = Fi_dt_sod * eta_cd_sodreab;
        Fi_u_sod = Fi_dt_sod - Fi_cd_sodreab;
        C_adh = 4 * x_values[7];
        delta_ra = 0.2 * P_ra - 7.0E-4 * x_values[2];
        N_adhs = Math.max(0, C_sod - 139 + Math.max(0, eps_aum - 1) - delta_ra) / 3;
        mu_adh = 0.37 + 0.8 / ( 1 + Math.exp(0.6 - 3.7 * Math.log(C_adh) / Math.log(10)) );
        mu_al = 0.17 + 0.94 / ( 1 + Math.exp( ( 0.48 - 1.2 * Math.log(C_al) / Math.log(10) ) / 0.88) );
        Fi_t_wreab = 0.025 - 0.0010 / ( mu_al * mu_adh ) + 0.8 * Fi_gfilt;
        Fi_u = Fi_gfilt - Fi_t_wreab;
        Fi_win = 0.0080 / ( 1 + 1.822 * Math.pow(C_adh, -1.607) ) - 0.0053;
        alpha_auto = 3.079 * Math.exp( -P_ma * 0.011);
        alpha_chemo = 0.25 * alpha_auto;
        alpha_baro = 0.75 * alpha_auto - x_values[3];
        vas_f = 11.312 * Math.exp( -Fi_co * 0.4799) / 100000;
        vas_d = x_values[6] * K_vd;
        ksi_map = 1;
        ksi_at = 0.4 + 2.4 / ( 1 + Math.exp(2.82 - 1.5 * Math.log(C_at) / Math.log(10) / 0.8) );
        ksi_k_sod = C_K / ( 0.003525 * C_sod ) - 9;
        N_als = ksi_k_sod * ksi_map * ksi_at;
        nu_md_sod = 0.2262 + 28.04 / ( 11.56 + Math.exp( ( Fi_md_sod - 1.667 ) / 0.6056) );
        nu_rsna = 1.89 - 2.056 / ( 1.358 + Math.exp(rsna - 0.8667) );
        N_rs = nu_md_sod * nu_rsna;
        C_K = 5;
    }
    @Override
    public double[] dy_dt(double time, double[] x_values) throws Exception
    {
        this.time = time;
        this.x_values = x_values;
        final double[] dydt = new double[8];

        calculateScalar();

        dydt[0] = +Fi_sodin - Fi_u_sod;
        dydt[1] = + ( N_als - x_values[1] ) / T_al;
        dydt[2] = +delta_ra;
        dydt[3] = +5.0E-4 * ( alpha_baro - 0.75 );
        dydt[4] = +Fi_win - Fi_u;
        dydt[5] = + ( N_rs - x_values[5] ) / T_r;
        dydt[6] = +vas_f - vas_d;
        dydt[7] = + ( N_adhs - x_values[7] ) / T_adh;
        return dydt;
    }
    @Override
    public void init() throws Exception
    {
        eta_dt_sodreab = 0.5; // initial value of eta_dt_sodreab
        N_rsna = 1.0; // initial value of N_rsna
        N_als = 1.0; // initial value of N_als
        ksi_map = 1.0; // initial value of ksi_map
        R_ea = 51.66; // initial value of R_ea
        P_gh = 52.0; // initial value of P_gh
        R_ba = 16.6; // initial value of R_ba
        R_vr = 1.4; // initial value of R_vr
        R_aa = 31.67; // initial value of R_aa
        alpha_map = 1.0; // initial value of alpha_map
        Fi_u_sod = 0.126; // initial value of Fi_u_sod
        C_sod = 144.0; // initial value of C_sod
        P_go = 28.0; // initial value of P_go
        Fi_dt_sodreab = 1.8; // initial value of Fi_dt_sodreab
        mu_al = 1.0; // initial value of mu_al
        P_B = 18.0; // initial value of P_B
        P_ma = 100.0; // initial value of P_ma
        eps_aum = 1.0; // initial value of eps_aum
        Fi_gfilt = 0.125; // initial value of Fi_gfilt
        Fi_rb = 1.2; // initial value of Fi_rb
        vas_d = 1.0E-5; // initial value of vas_d
        gamma_at = 1.0; // initial value of gamma_at
        nu_rsna = 1.0; // initial value of nu_rsna
        Fi_cd_sodreab = 1.674; // initial value of Fi_cd_sodreab
        K_bar = 16.6; // initial value of K_bar
        Fi_t_wreab = 0.124; // initial value of Fi_t_wreab
        Fi_pt_sodreab = 14.4; // initial value of Fi_pt_sodreab
        R_bv = 3.4; // initial value of R_bv
        psi_al = 1.0; // initial value of psi_al
        eta_cd_sodreab = 0.93; // initial value of eta_cd_sodreab
        P_mf = 7.0; // initial value of P_mf
        R_a = 16.6; // initial value of R_a
        C_al = 85.0; // initial value of C_al
        sigma_tgf = 1.0; // initial value of sigma_tgf
        Fi_md_sod = 3.6; // initial value of Fi_md_sod
        N_rs = 1.0; // initial value of N_rs
        R_r = 83.33; // initial value of R_r
        alpha_auto = 1.0; // initial value of alpha_auto
        rsna = 1.0; // initial value of rsna
        N_adhs = 1.0; // initial value of N_adhs
        n_eps_dt = 0.5; // initial value of n_eps_dt
        n_eta_cd = 0.93; // initial value of n_eta_cd
        C_K = 5.1; // initial value of C_K
        Fi_dt_sod = 1.8; // initial value of Fi_dt_sod
        time = 0.0; // initial value of time
        alpha_chemo = 0.25; // initial value of alpha_chemo
        T_al = 30.0; // initial value of T_al
        gamma_rsna = 1.0; // initial value of gamma_rsna
        beta_rsna = 1.0; // initial value of beta_rsna
        C_adh = 4.0; // initial value of C_adh
        K_vd = 1.0E-5; // initial value of K_vd
        ksi_at = 1.0; // initial value of ksi_at
        R_tp = 20.0; // initial value of R_tp
        delta_ra = 0.0; // initial value of delta_ra
        C_gcf = 0.00781; // initial value of C_gcf
        nu_md_sod = 1.0; // initial value of nu_md_sod
        gamma_filsod = 1.0; // initial value of gamma_filsod
        Fi_co = 5.0; // initial value of Fi_co
        mu_adh = 1.0; // initial value of mu_adh
        P_ra = 0.0; // initial value of P_ra
        Fi_sodin = 0.126; // initial value of Fi_sodin
        C_anp = 1.0; // initial value of C_anp
        n_eta_pt = 0.8; // initial value of n_eta_pt
        eta_pt_sodreab = 0.8; // initial value of eta_pt_sodreab
        Fi_filsod = 18.0; // initial value of Fi_filsod
        T_r = 15.0; // initial value of T_r
        V_b = 5.0; // initial value of V_b
        P_f = 16.0; // initial value of P_f
        vas_f = 1.0E-5; // initial value of vas_f
        T_adh = 6.0; // initial value of T_adh
        lambda_anp = 1.0; // initial value of lambda_anp
        Fi_win = 0.0010; // initial value of Fi_win
        alpha_baro = 0.75; // initial value of alpha_baro
        ksi_k_sod = 1.0; // initial value of ksi_k_sod
        R_aass = 31.67; // initial value of R_aass
        lambda_dt = 1.0; // initial value of lambda_dt
        C_at = 20.0; // initial value of C_at
        Fi_u = 0.0010; // initial value of Fi_u
        alpha_rap = 1.0; // initial value of alpha_rap
        initialValues = getInitialValues();
        this.isInit = true;
    }
    @Override
    public double[] getInitialValues() throws Exception
    {
        if( !this.isInit )
        {
            this.x_values = new double[8];
            this.time = 0.0;
            x_values[0] = 2160.0; //  initial value of M_sod
            x_values[1] = 1.0; //  initial value of N_al
            x_values[2] = 0.0; //  initial value of temp
            x_values[3] = 0.0; //  initial value of delta_baro
            x_values[4] = 15.0; //  initial value of V_ecf
            x_values[5] = 1.0; //  initial value of C_r
            x_values[6] = 1.0; //  initial value of vas
            x_values[7] = 1.0; //  initial value of N_adh

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
        double[] yv5 = new double[87];
        yv5[0] = eta_dt_sodreab;
        yv5[1] = N_rsna;
        yv5[2] = N_als;
        yv5[3] = ksi_map;
        yv5[4] = R_ea;
        yv5[5] = P_gh;
        yv5[6] = x_values[0];
        yv5[7] = R_ba;
        yv5[8] = R_vr;
        yv5[9] = R_aa;
        yv5[10] = alpha_map;
        yv5[11] = Fi_u_sod;
        yv5[12] = C_sod;
        yv5[13] = P_go;
        yv5[14] = x_values[1];
        yv5[15] = Fi_dt_sodreab;
        yv5[16] = mu_al;
        yv5[17] = P_B;
        yv5[18] = P_ma;
        yv5[19] = eps_aum;
        yv5[20] = Fi_gfilt;
        yv5[21] = x_values[2];
        yv5[22] = Fi_rb;
        yv5[23] = vas_d;
        yv5[24] = gamma_at;
        yv5[25] = nu_rsna;
        yv5[26] = Fi_cd_sodreab;
        yv5[27] = K_bar;
        yv5[28] = Fi_t_wreab;
        yv5[29] = x_values[3];
        yv5[30] = Fi_pt_sodreab;
        yv5[31] = x_values[4];
        yv5[32] = R_bv;
        yv5[33] = psi_al;
        yv5[34] = eta_cd_sodreab;
        yv5[35] = P_mf;
        yv5[36] = R_a;
        yv5[37] = C_al;
        yv5[38] = sigma_tgf;
        yv5[39] = Fi_md_sod;
        yv5[40] = N_rs;
        yv5[41] = R_r;
        yv5[42] = x_values[5];
        yv5[43] = alpha_auto;
        yv5[44] = rsna;
        yv5[45] = N_adhs;
        yv5[46] = n_eps_dt;
        yv5[47] = n_eta_cd;
        yv5[48] = C_K;
        yv5[49] = Fi_dt_sod;
        yv5[50] = time;
        yv5[51] = alpha_chemo;
        yv5[52] = T_al;
        yv5[53] = gamma_rsna;
        yv5[54] = beta_rsna;
        yv5[55] = C_adh;
        yv5[56] = K_vd;
        yv5[57] = ksi_at;
        yv5[58] = R_tp;
        yv5[59] = delta_ra;
        yv5[60] = C_gcf;
        yv5[61] = x_values[6];
        yv5[62] = nu_md_sod;
        yv5[63] = gamma_filsod;
        yv5[64] = Fi_co;
        yv5[65] = mu_adh;
        yv5[66] = P_ra;
        yv5[67] = Fi_sodin;
        yv5[68] = C_anp;
        yv5[69] = n_eta_pt;
        yv5[70] = eta_pt_sodreab;
        yv5[71] = Fi_filsod;
        yv5[72] = T_r;
        yv5[73] = V_b;
        yv5[74] = x_values[7];
        yv5[75] = P_f;
        yv5[76] = vas_f;
        yv5[77] = T_adh;
        yv5[78] = lambda_anp;
        yv5[79] = Fi_win;
        yv5[80] = alpha_baro;
        yv5[81] = ksi_k_sod;
        yv5[82] = R_aass;
        yv5[83] = lambda_dt;
        yv5[84] = C_at;
        yv5[85] = Fi_u;
        yv5[86] = alpha_rap;
        return yv5;
    }
}