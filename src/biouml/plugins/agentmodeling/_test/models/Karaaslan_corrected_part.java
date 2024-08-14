package biouml.plugins.agentmodeling._test.models;
import biouml.plugins.simulation.java.JavaBaseModel;

public class Karaaslan_corrected_part extends JavaBaseModel
{
        protected double eta_dt_sodreab;
        protected double N_rsna;
        protected double N_als;
        protected double ksi_map;
        protected double R_tp;
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
        protected double vas_d;
        protected double gamma_at;
        protected double nu_rsna;
        protected double P_ma;
        protected double Fi_cd_sodreab;
        protected double n_eta_pt;
        protected double K_bar;
        protected double Fi_t_wreab;
        protected double Fi_pt_sodreab;
        protected double psi_al;
        protected double eta_cd_sodreab;
        protected double P_go;
        protected double P_mf;
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
        protected double R_bv;
        protected double alpha_chemo;
        protected double T_al;
        protected double gamma_rsna;
        protected double beta_rsna;
        protected double C_adh;
        protected double K_vd;
        protected double ksi_at;
        protected double R_ba;
        protected double delta_ra;
        protected double R_vr;
        protected double nu_md_sod;
        protected double gamma_filsod;
        protected double mu_adh;
        protected double P_ra;
        protected double Fi_sodin;
        protected double C_anp;
        protected double eta_pt_sodreab;
        protected double Fi_filsod;
        protected double T_r;
        protected double R_a;
        protected double V_b;
        protected double P_f;
        protected double vas_f;
        protected double T_adh;
        protected double lambda_anp;
        protected double Fi_win;
        protected double alpha_baro;
        protected double ksi_k_sod;
        protected double lambda_dt;
        protected double C_at;
        protected double Fi_u;
        protected double alpha_rap;
        protected double V_large;

        private void calculateParameters() throws Exception
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
            C_adh = 4*x_values[9];
            delta_ra = 0.2*P_ra - 7.0E-4*x_values[3];
            N_adhs = Math.max(0, C_sod - 141 + Math.max(0, x_values[7] - 1) - delta_ra)/3;
            mu_adh = 0.37 + 0.8/(1 + Math.exp(0.6 - 3.7*Math.log(C_adh)/Math.log(10)));
            mu_al = 0.17 + 0.94/(1 + Math.exp((0.48 - 1.2*Math.log(C_al)/Math.log(10))/0.88));
            Fi_t_wreab = 0.025 - 0.0010/(mu_al*mu_adh) + 0.8*Fi_gfilt;
            Fi_u = Fi_gfilt - Fi_t_wreab;
            Fi_win = 0.0080/(1 + 1.822*Math.pow(C_adh, -1.607)) - 0.0053;
            V_b = 4.560227 + 2.431217/(1 + Math.exp(-(x_values[5] - 18.11278)*0.47437));
            V_large = (V_b - 1.5)*1000;
            P_mf = (7.436*V_b - 30.18)*x_values[7];
            alpha_auto = 3.079*Math.exp(-P_ma*0.011);
            alpha_chemo = 0.25*alpha_auto;
            alpha_baro = 0.75*alpha_auto - x_values[4];
            vas_f = 11.312*Math.exp(-Fi_co*0.4799)/100000;
            vas_d = x_values[8]*K_vd;
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
        }
     

        @Override
        public double[] dy_dt(double time, double[] x_values) throws Exception
        {
            this.time = time;
            this.x_values = x_values;
            final double[] dydt = new double[10];
            calculateParameters();
            dydt[0] = +Fi_sodin - Fi_u_sod;
            dydt[1] = +(N_als - x_values[1])/T_al;
            dydt[2] = +Fi_filsod - Fi_pt_sodreab - x_values[2];
            dydt[3] = +delta_ra;
            dydt[4] = +5.0E-4*(alpha_baro - 0.75);
            dydt[5] = +Fi_win - Fi_u;
            dydt[6] = +(N_rs - x_values[6])/T_r;
            dydt[7] = +alpha_chemo + alpha_baro - x_values[7];
            dydt[8] = +vas_f - vas_d;
            dydt[9] = +(N_adhs - x_values[9])/T_adh;
            return dydt;
        }


        @Override
        public void init() throws Exception
        {
            super.init();
            V_large = 3500;
            eta_dt_sodreab = 0.5535084950484807; // initial value of $varName
            N_rsna = 1.0; // initial value of $varName
            N_als = 0.0; // initial value of $varName
            ksi_map = 0.0; // initial value of $varName
            R_tp = 20.10021897558536; // initial value of $varName
            R_ea = 70.76475388107161; // initial value of $varName
            P_gh = 69.45205677988511; // initial value of $varName
            R_aa = 32.96745230862174; // initial value of $varName
            alpha_map = 1.016894177017265; // initial value of $varName
            C_K = 5.0; // initial value of $varName
            Fi_u_sod = 0.10144419166693153; // initial value of $varName
            C_sod = 143.63884925276423; // initial value of $varName
            Fi_co = 5.065017345633908; // initial value of $varName
            Fi_dt_sodreab = 2.0600999342732274; // initial value of $varName
            C_gcf = 0.00781; // initial value of $varName
            mu_al = 1.1070169900969613; // initial value of $varName
            P_B = 18.0; // initial value of $varName
            Fi_gfilt = 0.18316056345090273; // initial value of $varName
            Fi_rb = 0.9814498457326277; // initial value of $varName
            vas_d = 9.951739678811103E-6; // initial value of $varName
            gamma_at = 1.0626973674320415; // initial value of $varName
            nu_rsna = 1.071438900846613; // initial value of $varName
            P_ma = 101.80795776237967; // initial value of $varName
            Fi_cd_sodreab = 1.5603498516631795; // initial value of $varName
            n_eta_pt = 0.8; // initial value of $varName
            K_bar = 16.6; // initial value of $varName
            Fi_t_wreab = 0.1707472855641951; // initial value of $varName
            Fi_pt_sodreab = 17.563534287313264; // initial value of $varName
            psi_al = 1.1070169900969613; // initial value of $varName
            eta_cd_sodreab = 0.9389550154701211; // initial value of $varName
            P_go = 28.0; // initial value of $varName
            P_mf = 8.057646586683026; // initial value of $varName
            C_al = 101.13911702475055; // initial value of $varName
            sigma_tgf = 1.0260459170939749; // initial value of $varName
            N_rs = 0.9694502918561185; // initial value of $varName
            R_aass = 31.67; // initial value of $varName
            R_r = 103.73220618969336; // initial value of $varName
            alpha_auto = 1.0047284776907697; // initial value of $varName
            rsna = 1.0096954391725261; // initial value of $varName
            N_adhs = 0.8800104573956414; // initial value of $varName
            n_eps_dt = 0.5; // initial value of $varName
            n_eta_cd = 0.93; // initial value of $varName
            Fi_dt_sod = 1.661794043330111; // initial value of $varName
            R_bv = 3.4; // initial value of $varName
            time = 0.0; // initial value of $varName
            alpha_chemo = 0.2511821194226924; // initial value of $varName
            T_al = 30.0; // initial value of $varName
            gamma_rsna = 0.9813346543080714; // initial value of $varName
            beta_rsna = 1.0145431587587892; // initial value of $varName
            C_adh = 3.520041829582587; // initial value of $varName
            K_vd = 1.0E-5; // initial value of $varName
            ksi_at = 2.654269758757526; // initial value of $varName
            R_ba = 16.680500631808272; // initial value of $varName
            delta_ra = -1.0269562977782698E-15; // initial value of $varName
            R_vr = 1.4161360959866245; // initial value of $varName
            nu_md_sod = 0.9048115493007518; // initial value of $varName
            gamma_filsod = 0.8001866427832497; // initial value of $varName
            mu_adh = 1.156386007211563; // initial value of $varName
            P_ra = 0.8848926967324804; // initial value of $varName
            Fi_sodin = 0.126; // initial value of $varName
            C_anp = 1.2223079012063316; // initial value of $varName
            eta_pt_sodreab = 0.6675872364661375; // initial value of $varName
            Fi_filsod = 26.308972562575576; // initial value of $varName
            T_r = 15.0; // initial value of $varName
            R_a = 16.70021897558536; // initial value of $varName
            V_b = 5.140953837098351; // initial value of $varName
            P_f = 23.452056779885112; // initial value of $varName
            vas_f = 9.9517396788069E-6; // initial value of $varName
            T_adh = 6.0; // initial value of $varName
            lambda_anp = 0.9976692098793667; // initial value of $varName
            Fi_win = 0.0011457540364351714; // initial value of $varName
            alpha_baro = 0.7499999999999999; // initial value of $varName
            ksi_k_sod = 0.8750423279707515; // initial value of $varName
            lambda_dt = 1.011987780012458; // initial value of $varName
            C_at = 19.389005837122372; // initial value of $varName
            Fi_u = 0.012413277886707624; // initial value of $varName
            alpha_rap = 0.9929208584261402; // initial value of $varName
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
                x_values[0] = 2250.7772249937684; //  initial value of M_sod
                x_values[1] = 1.1898719649970653; //  initial value of N_al
                x_values[2] = 3.7218939776033384; //  initial value of Fi_md_sod
                x_values[3] = 252.8264847807102; //  initial value of temp
                x_values[4] = 0.0035463582680773658; //  initial value of delta_baro
                x_values[5] = 15.669696859190438; //  initial value of V_ecf
                x_values[6] = 0.9694502918561186; //  initial value of C_r
                x_values[7] = 1.0011821194226922; //  initial value of eps_aum
                x_values[8] = 0.9951739678811101; //  initial value of vas
                x_values[9] = 0.8800104573956468; //  initial value of N_adh
            calculateParameters();
                return x_values;
            }
            else return initialValues;
        }


        @Override
        public double[] extendResult(double time, double[] x_values) throws Exception
        {
            this.time = time;
            this.x_values = x_values;
            calculateParameters();
            double[] y = new double[87];
            y[0] = eta_dt_sodreab;
            y[1] = N_rsna;
            y[2] = N_als;
            y[3] = ksi_map;
            y[4] = R_tp;
            y[5] = R_ea;
            y[6] = P_gh;
            y[7] = x_values[0];
            y[8] = R_aa;
            y[9] = alpha_map;
            y[10] = C_K;
            y[11] = Fi_u_sod;
            y[12] = C_sod;
            y[13] = Fi_co;
            y[14] = x_values[1];
            y[15] = Fi_dt_sodreab;
            y[16] = C_gcf;
            y[17] = mu_al;
            y[18] = x_values[2];
            y[19] = P_B;
            y[20] = Fi_gfilt;
            y[21] = x_values[3];
            y[22] = Fi_rb;
            y[23] = vas_d;
            y[24] = gamma_at;
            y[25] = nu_rsna;
            y[26] = P_ma;
            y[27] = Fi_cd_sodreab;
            y[28] = n_eta_pt;
            y[29] = K_bar;
            y[30] = Fi_t_wreab;
            y[31] = x_values[4];
            y[32] = Fi_pt_sodreab;
            y[33] = x_values[5];
            y[34] = psi_al;
            y[35] = eta_cd_sodreab;
            y[36] = P_go;
            y[37] = P_mf;
            y[38] = C_al;
            y[39] = sigma_tgf;
            y[40] = N_rs;
            y[41] = R_aass;
            y[42] = R_r;
            y[43] = x_values[6];
            y[44] = alpha_auto;
            y[45] = rsna;
            y[46] = N_adhs;
            y[47] = n_eps_dt;
            y[48] = n_eta_cd;
            y[49] = Fi_dt_sod;
            y[50] = x_values[7];
            y[51] = R_bv;
            y[52] = time;
            y[53] = alpha_chemo;
            y[54] = T_al;
            y[55] = gamma_rsna;
            y[56] = beta_rsna;
            y[57] = C_adh;
            y[58] = K_vd;
            y[59] = ksi_at;
            y[60] = R_ba;
            y[61] = delta_ra;
            y[62] = x_values[8];
            y[63] = R_vr;
            y[64] = nu_md_sod;
            y[65] = gamma_filsod;
            y[66] = mu_adh;
            y[67] = P_ra;
            y[68] = Fi_sodin;
            y[69] = C_anp;
            y[70] = eta_pt_sodreab;
            y[71] = Fi_filsod;
            y[72] = T_r;
            y[73] = R_a;
            y[74] = V_b;
            y[75] = x_values[9];
            y[76] = P_f;
            y[77] = vas_f;
            y[78] = T_adh;
            y[79] = lambda_anp;
            y[80] = Fi_win;
            y[81] = alpha_baro;
            y[82] = ksi_k_sod;
            y[83] = lambda_dt;
            y[84] = C_at;
            y[85] = Fi_u;
            y[86] = alpha_rap;
            return y;
        }
     }