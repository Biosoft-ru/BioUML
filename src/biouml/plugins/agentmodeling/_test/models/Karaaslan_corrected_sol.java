package biouml.plugins.agentmodeling._test.models;
import biouml.plugins.simulation.java.JavaBaseModel;

public class Karaaslan_corrected_sol extends JavaBaseModel
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
        
        C_al = x_values[1]*85;
        psi_al = 0.17 + 0.94/(1 + Math.exp((0.48 - 1.2*Math.log(C_al)/Math.log(10))/0.88));
        eta_dt_sodreab = n_eps_dt*psi_al;
        Fi_dt_sodreab = x_values[2]*eta_dt_sodreab;
        C_at = 20*x_values[6];
        R_ea = 51.66*(0.9432 + 0.1363/(0.2069 + Math.exp(3.108 - 1.785*Math.log(C_at)/Math.log(10))));
        sigma_tgf = 0.3408 + 3.449/(3.88 + Math.exp((x_values[2] - 3.859)/-0.9617));
        alpha_map = 0.5 + 1.1/(1 + Math.exp((P_ma - 100)/15));
        P_ra = 0.2787*Math.exp(0.2281*Fi_co);
        alpha_rap = 1 - 0.0080*P_ra;
        rsna = N_rsna*alpha_map*alpha_rap;
        beta_rsna = 1.5*(rsna - 1) + 1;
        R_aa = R_aass*beta_rsna*sigma_tgf;
        R_r = R_aa + R_ea;
        Fi_dt_sod = x_values[2] - Fi_dt_sodreab;
        C_anp = 7.427 - 6.554/(1 + Math.exp(P_ra - 3.762));
        lambda_anp = -0.1*C_anp + 1.1199;
        lambda_dt = 0.82 + 0.39/(1 + Math.exp((Fi_dt_sod - 1.6)/2));
        eta_cd_sodreab = n_eta_cd*lambda_dt*lambda_anp;
        Fi_cd_sodreab = Fi_dt_sod*eta_cd_sodreab;
        Fi_u_sod = Fi_dt_sod - Fi_cd_sodreab;
        gamma_at = 0.95 + 0.12/(1 + Math.exp(2.6 - 1.8*Math.log(C_at)/Math.log(10)));
        Fi_rb = P_ma/R_r;
        P_gh = P_ma - Fi_rb*R_aa;
        P_f = P_gh - P_B - P_go;
        Fi_gfilt = P_f*C_gcf;
        C_sod = x_values[0]/x_values[5];
        Fi_filsod = Fi_gfilt*C_sod;
        gamma_filsod = 0.8 + 0.3/(1 + Math.exp(Fi_filsod - 14)/138);
        gamma_rsna = 0.5 + 0.7/(1 + Math.exp(1 - rsna)/2.18);
        eta_pt_sodreab = n_eta_pt*gamma_filsod*gamma_at*gamma_rsna;
        Fi_pt_sodreab = Fi_filsod*eta_pt_sodreab;
        C_adh = 4*x_values[8];
        delta_ra = 0.2*P_ra - 7.0E-4*x_values[3];
        N_adhs = Math.max(0, C_sod - 141 + Math.max(0, x_values[7] - 1) - delta_ra)/3;
        mu_adh = 0.37 + 0.8/(1 + Math.exp(0.6 - 3.7*Math.log(C_adh)/Math.log(10)));
        mu_al = 0.17 + 0.94/(1 + Math.exp((0.48 - 1.2*Math.log(C_al)/Math.log(10))/0.88));
        Fi_t_wreab = 0.025 - 0.0010/(mu_al*mu_adh) + 0.8*Fi_gfilt;
        Fi_u = Fi_gfilt - Fi_t_wreab;
        Fi_win = 0.0080/(1 + 1.822*Math.pow(C_adh, -1.607)) - 0.0053;
        alpha_auto = 3.079*Math.exp(-P_ma*0.011);
        alpha_chemo = 0.25*alpha_auto;
        alpha_baro = 0.75*alpha_auto - x_values[4];
        V_b = 4.560227 + 2.431217/(1 + Math.exp(-(x_values[5] - 18.11278)*0.47437));
        
        double piecewise_0 = 0;
if (P_ma < 100) {
    piecewise_0 = 69.03*Math.exp(-0.0425*P_ma);
}
else {
    piecewise_0 = 1;
}


        ksi_map = piecewise_0;
        ksi_at = 0.4 + 2.4/(1 + Math.exp(2.82 - 1.5*Math.log(C_at)/Math.log(10)/0.8));
        C_K = 5;
        ksi_k_sod = C_K/(0.003525*C_sod) - 9;
        N_als = ksi_k_sod*ksi_map*ksi_at;
        nu_md_sod = 0.2262 + 28.04/(11.56 + Math.exp((x_values[2] - 1.667)/0.6056));
        nu_rsna = 1.89 - 2.056/(1.358 + Math.exp(rsna - 0.8667));
        N_rs = nu_md_sod*nu_rsna;
        V_large = 1000*(V_b - 1.5);
        
       
        vas_f = 11.312*Math.exp(-Fi_co*0.4799)/100000;
        vas_d = x_values[9]*K_vd;
    }
      @Override
    public double[] dy_dt(double time, double[] x_values) throws Exception
    {
        this.time = time;
        this.x_values = x_values;
        final double[] dydt = new double[10];
        calculateScalar();
        
        dydt[0] = +Fi_sodin - Fi_u_sod;
        dydt[1] = +(N_als - x_values[1])/T_al;
        dydt[2] = +Fi_filsod - Fi_pt_sodreab - x_values[2];
        dydt[3] = +delta_ra;
        dydt[4] = +5.0E-4*(alpha_baro - 0.75);
        dydt[5] = +Fi_win - Fi_u;
        dydt[6] = +(N_rs - x_values[6])/T_r;
        dydt[7] = +alpha_chemo + alpha_baro - x_values[7];
        dydt[8] = +(N_adhs - x_values[8])/T_adh;
        dydt[9] = +vas_f - vas_d;
        return dydt;
    }
    @Override
    public void init() throws Exception
    {
        eta_dt_sodreab = 0.50991484;//0.5076307730817058; // initial value of eta_dt_sodreab
        N_rsna = 1.0; // initial value of N_rsna
        N_als = 1.304778121;//1.1898719649970673; // initial value of N_als
        ksi_map = 1.0; // initial value of ksi_map
        R_ea = 52.1025582545899;//51.59481885621225; // initial value of R_ea
        P_gh = 62.0206382351962;//62.11710100160244; // initial value of P_gh
        R_aa = 32.4045849917263;//32.96745230862174; // initial value of R_aa
        alpha_map = 1.03911767861402;//1.016894177017265; // initial value of alpha_map
        C_K = 5.0; // initial value of C_K
        Fi_u_sod = 0.126651865182032;//0.12600000000000144; // initial value of Fi_u_sod
        C_sod = 143.456877385525;//143.63884925276423; // initial value of C_sod
        Fi_co = 6.46107030425499;//5.065017345633908; // initial value of Fi_co
        Fi_dt_sodreab = 1.76529408987442;//1.8893479171789276; // initial value of Fi_dt_sodreab
        C_gcf = 0.00781; // initial value of C_gcf
        mu_al = 1.01982960855728;//1.0152615461634116; // initial value of mu_al
        P_B = 18.0; // initial value of P_B
        Fi_gfilt = 0.125121184616883;//0.12587455882251508; // initial value of Fi_gfilt
        Fi_rb = 1.19035687138707;//1.2039406742509238; // initial value of Fi_rb
        gamma_at = 1.00695688970721;//1.0015838571573545; // initial value of gamma_at
        nu_rsna = 1.07870004322233;//1.071438900846613; // initial value of nu_rsna
        P_ma = 100.507082620077;//101.80795776237967; // initial value of P_ma
        Fi_cd_sodreab = 1.56999332394078;//1.7065460604244094; // initial value of Fi_cd_sodreab
        n_eta_pt = 0.8; // initial value of n_eta_pt
        Fi_t_wreab = 0.124116337724284;//0.12472880478607994; // initial value of Fi_t_wreab
        Fi_pt_sodreab = 14.4875552919645;//14.358582801862134; // initial value of Fi_pt_sodreab
        psi_al = 1.01982960855728;//1.0152615461634116; // initial value of psi_al
        eta_cd_sodreab = 0.925351590306566;//0.9312432016192704; // initial value of eta_cd_sodreab
        P_go = 28.0; // initial value of P_go
        C_al = 110.94612802958;//101.13911702475055; // initial value of C_al
        sigma_tgf = 0.980551677769544;//1.0260459170939749; // initial value of sigma_tgf
        N_rs = 1.22180338693015;//0.9694502918561185; // initial value of N_rs
        R_aass = 31.67; // initial value of R_aass
        R_r = 84.5071432463162;//84.56227116483399; // initial value of R_r
        alpha_auto = 1.01823895846844;//1.0047284776907697; // initial value of alpha_auto
        rsna = 1.02899272753752;//1.0096954391725261; // initial value of rsna
        N_adhs = 0.820408829318436;//0.8800104573956414; // initial value of N_adhs
        n_eps_dt = 0.5; // initial value of n_eps_dt
        n_eta_cd = 0.93; // initial value of n_eta_cd
        Fi_dt_sod = 1.69664518912282;//1.8325460604244108; // initial value of Fi_dt_sod
        time = 0.0; // initial value of time
        alpha_chemo = 0.254559739617111;//0.2511821194226924; // initial value of alpha_chemo
        T_al = 30.0; // initial value of T_al
        gamma_rsna = 0.984225616112119;//0.9813346543080714; // initial value of gamma_rsna
        beta_rsna = 1.04348909130628;//1.0145431587587892; // initial value of beta_rsna
        C_adh = 3.28158598635717;//3.520041829582587; // initial value of C_adh
        ksi_at = 1.47005890086295;//1.3597878947826614; // initial value of ksi_at
        delta_ra = -0.000144284494134755;//-1.0269562977782698E-15; // initial value of delta_ra
        nu_md_sod = 1.13266277739299;//0.9048115493007518; // initial value of nu_md_sod
        gamma_filsod = 1.01799901262833;//1.0099671764350113; // initial value of gamma_filsod
        mu_adh = 0.999944921974501;//1.0145498356407712; // initial value of mu_adh
        P_ra = 1.21797454764698;//0.8848926967324804; // initial value of P_ra
        Fi_sodin = 0.126; // initial value of Fi_sodin
        C_anp = 1.35032052900373;//1.2223079012063316; // initial value of C_anp
        eta_pt_sodreab = 0.807128877109057;//0.7941484606296234; // initial value of eta_pt_sodreab
        Fi_filsod = 17.9494944399159;//18.080476779465446; // initial value of Fi_filsod
        T_r = 15.0; // initial value of T_r
        V_b = 5.06632970309817;//5.140953837098351; // initial value of V_b
        P_f = 16.0206382351962;//16.117101001602443; // initial value of P_f
        T_adh = 6.0; // initial value of T_adh
        lambda_anp = 0.984867947099626;//0.9976692098793667; // initial value of lambda_anp
        Fi_win = 0.000999712214456830;//.0011457540364351714; // initial value of Fi_win
        alpha_baro = 0.74964525577926;//0.7499999999999999; // initial value of alpha_baro
        ksi_k_sod = 0.887568600145572;//0.8750423279707515; // initial value of ksi_k_sod
        lambda_dt = 1.01028946361517;//1.0036761343529645; // initial value of lambda_dt
        C_at = 24.4366773388834;//19.389005837122372; // initial value of C_at
        Fi_u = 0.0010048468925988;//0.0011457540364351454; // initial value of Fi_u
        V_large = 3566.32970309817; // initial value of V_large
        alpha_rap = 0.990256203618824;//0.9929208584261402; // initial value of alpha_rap
        
        vas_f = 0.5*10-4;//9.9517396788069E-6; // initial value of vas_f
        vas_d = 0.5*10-4;//9.951739678811103E-6; // initial value of vas_d
        K_vd = 1.0E-5; // initial value of K_vd
        initialValues = getInitialValues();
        this.isInit = true;
    }
    
    @Override
    public double[] getInitialValues() throws Exception
    {
       if (!this.isInit)
       {
            this.x_values = new double[10];
            this.time = 0.0;
            x_values[0] = 2194.37534571281;//2250.7772249937684; //  initial value of M_sod
            x_values[1] = 1.30524856505388;//1.1898719649970653; //  initial value of N_al
            x_values[2] = 3.46193927899724;//3.7218939776033384; //  initial value of Fi_md_sod
            x_values[3] = 348.198848605044;//252.8264847807102; //  initial value of temp
            x_values[4] = 0.0140339630720724;//0.0035463582680773658; //  initial value of delta_baro
            x_values[5] = 15.2964109194685;//15.669696859190438; //  initial value of V_ecf
            x_values[6] = 1.22183386694417;//0.9694502918561186; //  initial value of C_r
            x_values[7] = 1.00420481793553;//1.0011821194226922; //  initial value of eps_aum
            x_values[8] = 0.820396496589294;//0.8800104573956468; //  initial value of N_adh
            x_values[9] = 0.5;//0.9951739678811101; //  initial value of vas
            calculateScalar();
            
            return x_values;
        }
        else return initialValues;
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