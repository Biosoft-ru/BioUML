package biouml.plugins.agentmodeling._test.models;
/*
 * This code is generated by BioUML FrameWork
 * for KidneyModel diagram  at 2010.11.09 14:39:44
 */
import biouml.plugins.simulation.ae.NewtonSolver;
import biouml.plugins.simulation.java.JavaBaseModel;

public class KidneyModel extends JavaBaseModel
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
   protected double Fi_sodin;
   protected double Fi_dt_sodreab;
   protected double mu_al;
   protected double P_B;
   protected double Fi_gfilt;
   protected double Fi_rb;
   protected double vas_d;
   protected double gamma_at;
   protected double nu_rsna;
   protected double P_ma;
   protected double Fi_cd_sodreab;
   protected double K_bar;
   protected double Fi_t_wreab;
   protected double Fi_pt_sodreab;
   protected double R_bv;
   protected double psi_al;
   protected double eta_cd_sodreab;
   protected double Fi_co;
   protected double P_mf;
   protected double R_a;
   protected double sigma_tgf;
   protected double N_rs;
   protected double R_aass;
   protected double C_al;
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
   protected double Fi_md_sod;
   protected double gamma_rsna;
   protected double beta_rsna;
   protected double C_adh;
   protected double K_vd;
   protected double ksi_at;
   protected double R_tp;
   protected double delta_ra;
   protected double C_gcf;
   protected double eps_aum;
   protected double nu_md_sod;
   protected double gamma_filsod;
   protected double R_r;
   protected double mu_adh;
   protected double P_ra;
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
   protected double ksi_k_sod;
   protected double lambda_dt;
   protected double C_at;
   protected double Fi_u;
   protected double alpha_rap;
   protected double[] x;
   
   @Override
    public double[] solveAlgebraic(double[] z)
   {
      
P_ma = z[0];
Fi_co = z[1];
C_al = z[2];
Fi_md_sod = z[3];
eps_aum = z[4];
R_r = z[5];
      final double [] result = new double [6];
      result[0] = (P_ma - Fi_rb*31.67*beta_rsna*(0.3408 + 3.449/(3.88 + Math.exp((Fi_md_sod - 3.859)/-0.9617))) - P_B - P_go)*C_gcf*C_sod*(1 - n_eta_pt*((0.8 + 0.3/(1 + Math.exp(1 + (Fi_filsod - 14)/138)))/0.882)*gamma_at*gamma_rsna) - Fi_md_sod;
      result[1] = R_aa + R_ea - R_r;
      result[2] = alpha_chemo + x[5] - eps_aum;
      result[3] = x[1]*85 - C_al;
      result[4] = Fi_co*R_tp - P_ma;
      result[5] = (P_mf - P_ra)/R_vr - Fi_co;
      return result;
   }

 

 
   @Override
public double[] dy_dt(double time, double[] x)
   {
      this.time = time;
      final double[] dydt = new double[8];
      final double [] algebraicResult = new double [6];
algebraicResult[0] =  P_ma;
algebraicResult[1] =  Fi_co;
algebraicResult[2] =  C_al;
algebraicResult[3] =  Fi_md_sod;
algebraicResult[4] =  eps_aum;
algebraicResult[5] =  R_r;
this.x = x;
try
{
   NewtonSolver.solve(algebraicResult, this);
}
   catch (Throwable t)
{
   t.printStackTrace();
}
P_ma = algebraicResult[0];
Fi_co = algebraicResult[1];
C_al = algebraicResult[2];
Fi_md_sod = algebraicResult[3];
eps_aum = algebraicResult[4];
R_r = algebraicResult[5];
      
psi_al = 0.17 + 0.94/(1 + Math.exp((0.48 - 1.2*Math.log(C_al)/Math.log(10))/0.88));
eta_dt_sodreab = n_eps_dt*psi_al;
Fi_dt_sodreab = Fi_md_sod*eta_dt_sodreab;
C_at = 20*x[4];
alpha_map = 0.5 + 1.1/(1 + Math.exp((P_ma - 100)/15));
P_ra = 0.2787*Math.exp(0.2281*Fi_co);
alpha_rap = 1 - 0.0080*P_ra;
rsna = N_rsna*alpha_map*alpha_rap;
beta_rsna = 1.5*(rsna - 1) + 1;
R_ea = 51.66*(0.9432 + 0.1363/(0.2069 + Math.exp(3.108 - 1.785*Math.log(C_at)/Math.log(10))));
sigma_tgf = 0.3408 + 3.449/(3.88 + Math.exp((Fi_md_sod - 3.859)/-0.9617));
R_aa = R_aass*beta_rsna*sigma_tgf;
double piecewise_3 = 0;
if (time < 3000) {
    piecewise_3 = 0.126;
}
else if (time >= 3000 && time < 8200) {
    piecewise_3 = 0.26;
}
else {
    piecewise_3 = 0.02;
}


Fi_sodin = piecewise_3;
Fi_dt_sod = Fi_md_sod - Fi_dt_sodreab;
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
C_sod = x[0]/x[3];
Fi_filsod = Fi_gfilt*C_sod;
gamma_filsod = (0.8 + 0.3/(1 + Math.exp(1 + (Fi_filsod - 14)/138)))/0.882;
gamma_rsna = (0.5 + 0.7/(1 + Math.exp((1 - rsna)/2.18)))/0.853;
eta_pt_sodreab = n_eta_pt*gamma_filsod*gamma_at*gamma_rsna;
Fi_pt_sodreab = Fi_filsod*eta_pt_sodreab;
C_adh = 4*x[7];
delta_ra = 0.2*P_ra - 7.0E-4*x[2];
N_adhs = (C_sod - 140 + eps_aum - delta_ra)/3;
mu_adh = 0.37 + 0.8/(1 + Math.exp(0.6 - 3.7*Math.log(C_adh)/Math.log(10)));
mu_al = 0.17 + 0.94/(1 + Math.exp((0.48 - 1.2*Math.log(C_al)/Math.log(10))/0.88));
Fi_t_wreab = 0.025 - 0.0011/(mu_al*mu_adh) + 0.8*Fi_gfilt;
Fi_u = Fi_gfilt - Fi_t_wreab;
Fi_win = 0.0080/(1 + 1.822*Math.pow(C_adh, -1.607)) - 0.0053;
R_ba = K_bar/x[6];
R_a = R_ba*eps_aum;
R_bv = 3.4;
R_vr = (8*R_bv + R_a)/31;
V_b = 4.5602271 + 2.4312171/(1 + Math.exp(-(x[3] - 18.11278)*0.47437));
P_mf = (7.436*V_b - 30.18)*eps_aum;
alpha_auto = 3.079*Math.exp(-P_ma*0.011);
alpha_chemo = 0.25*alpha_auto;
vas_f = 11.312*Math.exp(-Fi_co*0.4799)/100000;
vas_d = x[6]*K_vd;
ksi_map = 1;
ksi_at = 0.4 + 2.4/(1 + Math.exp(2.82 - 1.5*Math.log(C_at)/Math.log(10)/0.8));
ksi_k_sod = C_K/(0.003525*C_sod) - 9;
N_als = ksi_k_sod*ksi_map*ksi_at;
R_tp = R_a + R_bv;
nu_md_sod = 0.2262 + 28.04/(11.56 + Math.exp((Fi_md_sod - 1.667)/0.6056));
nu_rsna = 1.89 - 2.056/(1.358 + Math.exp(rsna - 0.8667));
N_rs = nu_md_sod*nu_rsna;
C_K = 5;
      
      dydt[0] = +Fi_sodin - Fi_u_sod;
      dydt[1] = +(N_als - x[1])/T_al;
      dydt[2] = +delta_ra;
      dydt[3] = +Fi_win - Fi_u;
      dydt[4] = +(N_rs - x[4])/T_r;
      dydt[5] = -5.0025E-5*(x[5] - 1);
      dydt[6] = +vas_f - vas_d;
      dydt[7] = +(N_adhs - x[7])/T_adh;
      return dydt;
   }

   @Override
public void init()
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
      Fi_sodin = 0.126; // initial value of Fi_sodin
      Fi_dt_sodreab = 1.8; // initial value of Fi_dt_sodreab
      mu_al = 1.0; // initial value of mu_al
      P_B = 18.0; // initial value of P_B
      Fi_gfilt = 0.125; // initial value of Fi_gfilt
      Fi_rb = 1.2; // initial value of Fi_rb
      vas_d = 1.0E-5; // initial value of vas_d
      gamma_at = 1.0; // initial value of gamma_at
      nu_rsna = 1.0; // initial value of nu_rsna
      P_ma = 100.0; // initial value of P_ma
      Fi_cd_sodreab = 1.674; // initial value of Fi_cd_sodreab
      K_bar = 16.6; // initial value of K_bar
      Fi_t_wreab = 0.124; // initial value of Fi_t_wreab
      Fi_pt_sodreab = 14.4; // initial value of Fi_pt_sodreab
      R_bv = 3.4; // initial value of R_bv
      psi_al = 1.0; // initial value of psi_al
      eta_cd_sodreab = 0.93; // initial value of eta_cd_sodreab
      Fi_co = 5.0; // initial value of Fi_co
      P_mf = 7.0; // initial value of P_mf
      R_a = 16.6; // initial value of R_a
      sigma_tgf = 1.0; // initial value of sigma_tgf
      N_rs = 1.0; // initial value of N_rs
      R_aass = 31.67; // initial value of R_aass
      C_al = 85.0; // initial value of C_al
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
      Fi_md_sod = 3.6; // initial value of Fi_md_sod
      gamma_rsna = 1.0; // initial value of gamma_rsna
      beta_rsna = 1.0; // initial value of beta_rsna
      C_adh = 4.0; // initial value of C_adh
      K_vd = 1.0E-5; // initial value of K_vd
      ksi_at = 1.0; // initial value of ksi_at
      R_tp = 20.0; // initial value of R_tp
      delta_ra = 0.0; // initial value of delta_ra
      C_gcf = 0.00781; // initial value of C_gcf
      eps_aum = 1.0; // initial value of eps_aum
      nu_md_sod = 1.0; // initial value of nu_md_sod
      gamma_filsod = 1.0; // initial value of gamma_filsod
      R_r = 83.33; // initial value of R_r
      mu_adh = 1.0; // initial value of mu_adh
      P_ra = 0.0; // initial value of P_ra
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
      ksi_k_sod = 1.0; // initial value of ksi_k_sod
      lambda_dt = 1.0; // initial value of lambda_dt
      C_at = 20.0; // initial value of C_at
      Fi_u = 0.0010; // initial value of Fi_u
      alpha_rap = 1.0; // initial value of alpha_rap
      initialValues = getInitialValues();
      this.isInit = true;
   }

   @Override
public double[] getInitialValues()
   {
      double [] x = new double[8];
      this.time = 0.0;
      if (!this.isInit)
      {
                x[0] = 2160.0; //  initial value of M_sod
                  x[1] = 1.0; //  initial value of N_al
              x[2] = 0.0; //  initial value of temp
                   x[3] = 15.0; //  initial value of V_ecf
                    x[4] = 1.0; //  initial value of C_r
                 x[5] = 0.75; //  initial value of alpha_baro
                       x[6] = 1.0; //  initial value of vas
                     x[7] = 1.0; //  initial value of N_adh
                
psi_al = 0.17 + 0.94/(1 + Math.exp((0.48 - 1.2*Math.log(C_al)/Math.log(10))/0.88));
eta_dt_sodreab = n_eps_dt*psi_al;
Fi_dt_sodreab = Fi_md_sod*eta_dt_sodreab;
C_at = 20*x[4];
alpha_map = 0.5 + 1.1/(1 + Math.exp((P_ma - 100)/15));
P_ra = 0.2787*Math.exp(0.2281*Fi_co);
alpha_rap = 1 - 0.0080*P_ra;
rsna = N_rsna*alpha_map*alpha_rap;
beta_rsna = 1.5*(rsna - 1) + 1;
R_ea = 51.66*(0.9432 + 0.1363/(0.2069 + Math.exp(3.108 - 1.785*Math.log(C_at)/Math.log(10))));
sigma_tgf = 0.3408 + 3.449/(3.88 + Math.exp((Fi_md_sod - 3.859)/-0.9617));
R_aa = R_aass*beta_rsna*sigma_tgf;
double piecewise_3 = 0;
if (time < 3000) {
    piecewise_3 = 0.126;
}
else if (time >= 3000 && time < 8200) {
    piecewise_3 = 0.26;
}
else {
    piecewise_3 = 0.02;
}


Fi_sodin = piecewise_3;
Fi_dt_sod = Fi_md_sod - Fi_dt_sodreab;
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
C_sod = x[0]/x[3];
Fi_filsod = Fi_gfilt*C_sod;
gamma_filsod = (0.8 + 0.3/(1 + Math.exp(1 + (Fi_filsod - 14)/138)))/0.882;
gamma_rsna = (0.5 + 0.7/(1 + Math.exp((1 - rsna)/2.18)))/0.853;
eta_pt_sodreab = n_eta_pt*gamma_filsod*gamma_at*gamma_rsna;
Fi_pt_sodreab = Fi_filsod*eta_pt_sodreab;
C_adh = 4*x[7];
delta_ra = 0.2*P_ra - 7.0E-4*x[2];
N_adhs = (C_sod - 140 + eps_aum - delta_ra)/3;
mu_adh = 0.37 + 0.8/(1 + Math.exp(0.6 - 3.7*Math.log(C_adh)/Math.log(10)));
mu_al = 0.17 + 0.94/(1 + Math.exp((0.48 - 1.2*Math.log(C_al)/Math.log(10))/0.88));
Fi_t_wreab = 0.025 - 0.0011/(mu_al*mu_adh) + 0.8*Fi_gfilt;
Fi_u = Fi_gfilt - Fi_t_wreab;
Fi_win = 0.0080/(1 + 1.822*Math.pow(C_adh, -1.607)) - 0.0053;
R_ba = K_bar/x[6];
R_a = R_ba*eps_aum;
R_bv = 3.4;
R_vr = (8*R_bv + R_a)/31;
V_b = 4.5602271 + 2.4312171/(1 + Math.exp(-(x[3] - 18.11278)*0.47437));
P_mf = (7.436*V_b - 30.18)*eps_aum;
alpha_auto = 3.079*Math.exp(-P_ma*0.011);
alpha_chemo = 0.25*alpha_auto;
vas_f = 11.312*Math.exp(-Fi_co*0.4799)/100000;
vas_d = x[6]*K_vd;
ksi_map = 1;
ksi_at = 0.4 + 2.4/(1 + Math.exp(2.82 - 1.5*Math.log(C_at)/Math.log(10)/0.8));
ksi_k_sod = C_K/(0.003525*C_sod) - 9;
N_als = ksi_k_sod*ksi_map*ksi_at;
R_tp = R_a + R_bv;
nu_md_sod = 0.2262 + 28.04/(11.56 + Math.exp((Fi_md_sod - 1.667)/0.6056));
nu_rsna = 1.89 - 2.056/(1.358 + Math.exp(rsna - 0.8667));
N_rs = nu_md_sod*nu_rsna;
C_K = 5;
      final double [] algebraicResult = new double [6];
algebraicResult[0] =  P_ma;
algebraicResult[1] =  Fi_co;
algebraicResult[2] =  C_al;
algebraicResult[3] =  Fi_md_sod;
algebraicResult[4] =  eps_aum;
algebraicResult[5] =  R_r;
this.x = x;
try
{
   NewtonSolver.solve(algebraicResult, this);
}
   catch (Throwable t)
{
   t.printStackTrace();
}
P_ma = algebraicResult[0];
Fi_co = algebraicResult[1];
C_al = algebraicResult[2];
Fi_md_sod = algebraicResult[3];
eps_aum = algebraicResult[4];
R_r = algebraicResult[5];
      return x;
      }
      else return initialValues;
   }

   @Override
public double[] extendResult(double time, double[] x)
   {
      this.time = time;
      final double [] algebraicResult = new double [6];
algebraicResult[0] =  P_ma;
algebraicResult[1] =  Fi_co;
algebraicResult[2] =  C_al;
algebraicResult[3] =  Fi_md_sod;
algebraicResult[4] =  eps_aum;
algebraicResult[5] =  R_r;
this.x = x;
try
{
   NewtonSolver.solve(algebraicResult, this);
}
   catch (Throwable t)
{
   t.printStackTrace();
}
P_ma = algebraicResult[0];
Fi_co = algebraicResult[1];
C_al = algebraicResult[2];
Fi_md_sod = algebraicResult[3];
eps_aum = algebraicResult[4];
R_r = algebraicResult[5];
      
psi_al = 0.17 + 0.94/(1 + Math.exp((0.48 - 1.2*Math.log(C_al)/Math.log(10))/0.88));
eta_dt_sodreab = n_eps_dt*psi_al;
Fi_dt_sodreab = Fi_md_sod*eta_dt_sodreab;
C_at = 20*x[4];
alpha_map = 0.5 + 1.1/(1 + Math.exp((P_ma - 100)/15));
P_ra = 0.2787*Math.exp(0.2281*Fi_co);
alpha_rap = 1 - 0.0080*P_ra;
rsna = N_rsna*alpha_map*alpha_rap;
beta_rsna = 1.5*(rsna - 1) + 1;
R_ea = 51.66*(0.9432 + 0.1363/(0.2069 + Math.exp(3.108 - 1.785*Math.log(C_at)/Math.log(10))));
sigma_tgf = 0.3408 + 3.449/(3.88 + Math.exp((Fi_md_sod - 3.859)/-0.9617));
R_aa = R_aass*beta_rsna*sigma_tgf;
double piecewise_3 = 0;
if (time < 3000) {
    piecewise_3 = 0.126;
}
else if (time >= 3000 && time < 8200) {
    piecewise_3 = 0.26;
}
else {
    piecewise_3 = 0.02;
}


Fi_sodin = piecewise_3;
Fi_dt_sod = Fi_md_sod - Fi_dt_sodreab;
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
C_sod = x[0]/x[3];
Fi_filsod = Fi_gfilt*C_sod;
gamma_filsod = (0.8 + 0.3/(1 + Math.exp(1 + (Fi_filsod - 14)/138)))/0.882;
gamma_rsna = (0.5 + 0.7/(1 + Math.exp((1 - rsna)/2.18)))/0.853;
eta_pt_sodreab = n_eta_pt*gamma_filsod*gamma_at*gamma_rsna;
Fi_pt_sodreab = Fi_filsod*eta_pt_sodreab;
C_adh = 4*x[7];
delta_ra = 0.2*P_ra - 7.0E-4*x[2];
N_adhs = (C_sod - 140 + eps_aum - delta_ra)/3;
mu_adh = 0.37 + 0.8/(1 + Math.exp(0.6 - 3.7*Math.log(C_adh)/Math.log(10)));
mu_al = 0.17 + 0.94/(1 + Math.exp((0.48 - 1.2*Math.log(C_al)/Math.log(10))/0.88));
Fi_t_wreab = 0.025 - 0.0011/(mu_al*mu_adh) + 0.8*Fi_gfilt;
Fi_u = Fi_gfilt - Fi_t_wreab;
Fi_win = 0.0080/(1 + 1.822*Math.pow(C_adh, -1.607)) - 0.0053;
R_ba = K_bar/x[6];
R_a = R_ba*eps_aum;
R_bv = 3.4;
R_vr = (8*R_bv + R_a)/31;
V_b = 4.5602271 + 2.4312171/(1 + Math.exp(-(x[3] - 18.11278)*0.47437));
P_mf = (7.436*V_b - 30.18)*eps_aum;
alpha_auto = 3.079*Math.exp(-P_ma*0.011);
alpha_chemo = 0.25*alpha_auto;
vas_f = 11.312*Math.exp(-Fi_co*0.4799)/100000;
vas_d = x[6]*K_vd;
ksi_map = 1;
ksi_at = 0.4 + 2.4/(1 + Math.exp(2.82 - 1.5*Math.log(C_at)/Math.log(10)/0.8));
ksi_k_sod = C_K/(0.003525*C_sod) - 9;
N_als = ksi_k_sod*ksi_map*ksi_at;
R_tp = R_a + R_bv;
nu_md_sod = 0.2262 + 28.04/(11.56 + Math.exp((Fi_md_sod - 1.667)/0.6056));
nu_rsna = 1.89 - 2.056/(1.358 + Math.exp(rsna - 0.8667));
N_rs = nu_md_sod*nu_rsna;
C_K = 5;
      double[] yv3 = new double[86];
      yv3[0] = eta_dt_sodreab;
      yv3[1] = N_rsna;
      yv3[2] = N_als;
      yv3[3] = ksi_map;
      yv3[4] = R_ea;
      yv3[5] = P_gh;
      yv3[6] = x[0];
      yv3[7] = R_ba;
      yv3[8] = R_vr;
      yv3[9] = R_aa;
      yv3[10] = alpha_map;
      yv3[11] = Fi_u_sod;
      yv3[12] = C_sod;
      yv3[13] = P_go;
      yv3[14] = Fi_sodin;
      yv3[15] = x[1];
      yv3[16] = Fi_dt_sodreab;
      yv3[17] = mu_al;
      yv3[18] = P_B;
      yv3[19] = Fi_gfilt;
      yv3[20] = x[2];
      yv3[21] = Fi_rb;
      yv3[22] = vas_d;
      yv3[23] = gamma_at;
      yv3[24] = nu_rsna;
      yv3[25] = P_ma;
      yv3[26] = Fi_cd_sodreab;
      yv3[27] = K_bar;
      yv3[28] = Fi_t_wreab;
      yv3[29] = Fi_pt_sodreab;
      yv3[30] = x[3];
      yv3[31] = R_bv;
      yv3[32] = psi_al;
      yv3[33] = eta_cd_sodreab;
      yv3[34] = Fi_co;
      yv3[35] = P_mf;
      yv3[36] = R_a;
      yv3[37] = sigma_tgf;
      yv3[38] = N_rs;
      yv3[39] = R_aass;
      yv3[40] = C_al;
      yv3[41] = x[4];
      yv3[42] = alpha_auto;
      yv3[43] = rsna;
      yv3[44] = N_adhs;
      yv3[45] = n_eps_dt;
      yv3[46] = n_eta_cd;
      yv3[47] = C_K;
      yv3[48] = Fi_dt_sod;
      yv3[49] = x[5];
      yv3[50] = time;
      yv3[51] = alpha_chemo;
      yv3[52] = T_al;
      yv3[53] = Fi_md_sod;
      yv3[54] = gamma_rsna;
      yv3[55] = beta_rsna;
      yv3[56] = C_adh;
      yv3[57] = K_vd;
      yv3[58] = ksi_at;
      yv3[59] = R_tp;
      yv3[60] = delta_ra;
      yv3[61] = C_gcf;
      yv3[62] = eps_aum;
      yv3[63] = x[6];
      yv3[64] = nu_md_sod;
      yv3[65] = gamma_filsod;
      yv3[66] = R_r;
      yv3[67] = mu_adh;
      yv3[68] = P_ra;
      yv3[69] = C_anp;
      yv3[70] = n_eta_pt;
      yv3[71] = eta_pt_sodreab;
      yv3[72] = Fi_filsod;
      yv3[73] = T_r;
      yv3[74] = V_b;
      yv3[75] = x[7];
      yv3[76] = P_f;
      yv3[77] = vas_f;
      yv3[78] = T_adh;
      yv3[79] = lambda_anp;
      yv3[80] = Fi_win;
      yv3[81] = ksi_k_sod;
      yv3[82] = lambda_dt;
      yv3[83] = C_at;
      yv3[84] = Fi_u;
      yv3[85] = alpha_rap;
      return yv3;
   }

   }
