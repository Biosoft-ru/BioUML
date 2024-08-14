package biouml.plugins.agentmodeling._test.models;

import biouml.plugins.simulation.java.JavaBaseModel;

public class NeuroHumoralControlSystem extends JavaBaseModel
{
   protected double Reactivity_HeartCenter;
   protected double Stress_HeartCenter;
   protected double Pressure_0;
   protected double BR_HeartCenter;
   protected double Pressure_Arterial;
   protected double BloodFlow_Capillary;
   protected double Volume_Arterial;
   protected double time;
 
   @Override
public double[] dy_dt(double time, double[] x)
   {
      this.time = time;
      final double[] dydt = new double[1];
      dydt[0] = +Reactivity_HeartCenter*(BR_HeartCenter*BloodFlow_Capillary/Volume_Arterial - Stress_HeartCenter*(Pressure_Arterial - Pressure_0) - x[0]);
      return dydt;
   }

   @Override
public void init()
   {
      Reactivity_HeartCenter = 2.0; // initial value of Reactivity_HeartCenter
      Stress_HeartCenter = 0.0015; // initial value of Stress_HeartCenter
      Pressure_0 = 70.0; // initial value of Pressure_0
      BR_HeartCenter = 6.0; // initial value of BR_HeartCenter
      Pressure_Arterial = 0.0; // initial value of Pressure_Arterial
      BloodFlow_Capillary = 0.0; // initial value of BloodFlow_Capillary
      Volume_Arterial = 0.0; // initial value of Volume_Arterial
      time = 0.0; // initial value of time
      initialValues = getInitialValues();
      this.isInit = true;
   }

   @Override
public double[] getInitialValues()
   {
      double [] x = new double[1];
      this.time = 0.0;
      if (!this.isInit)
      {
           x[0] = 1.1; //  initial value of Humoral
             
      
      return x;
      }
      else return initialValues;
   }

   @Override
public double[] extendResult(double time, double[] x)
   {
      this.time = time;
      double[] yv69 = new double[9];
      yv69[0] = Reactivity_HeartCenter;
      yv69[1] = x[0];
      yv69[2] = Stress_HeartCenter;
      yv69[3] = Pressure_0;
      yv69[4] = BR_HeartCenter;
      yv69[5] = Pressure_Arterial;
      yv69[6] = BloodFlow_Capillary;
      yv69[7] = Volume_Arterial;
      yv69[8] = time;
      return yv69;
   }

   }