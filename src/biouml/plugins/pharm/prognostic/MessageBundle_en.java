package biouml.plugins.pharm.prognostic;

public class MessageBundle_en extends MessageBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }
    private final static Object[][] contents =
    {
        //Properties
        { "PARAMETERS",                 "Parameters"},

        { "INPUT_FILES",                "Input files"},
        { "OUTPUT_FILES",               "Saving results"},
        { "PATIENTS_NUMBER",            "Number of patients to generate"},
        { "PATIENT_PHYSIOLOGY",         "Patient physiology"},
        { "OPTIMIZATION_SETTINGS",      "Optimization settings"},
        { "SLOW_MODEL_NUM_OF_ITERATIONS",    "Number of iterations in the slow model optimization"},
        { "SLOW_MODEL_SURVIVAL_SIZE",        "Survival size in the slow model optimization"},
        { "FAST_MODEL_NUM_OF_ITERATIONS",    "Number of iterations in the fast model optimization"},
        { "FAST_MODEL_SURVIVAL_SIZE",        "Survival size in the fast model optimization"},
        { "POSSIBLE_DEVIATION",        	"Possible deviation from known values of variables in the population"},
        { "CHECK_SODIUM_LOAD_EXPERIMENT","Checking the sodium load experiment"},
        { "DECREASE_IN_SBP",            "Decrease in SBP, mmHg"},
        { "DECREASE_IN_DBP",            "Decrease in DBP, mmHg"},
        { "SBP",                        "SBP, mmHg"},
        { "DBP",                        "DBP, mmHg"},

        { "SYSTOLIC_BLOOD_PRESSURE",    "Systolic blood pressure"},
        { "DIASTOLIC_BLOOD_PRESSURE",   "Diastolic blood pressure"},
        { "MMHG",                       "mmHg"},

        { "REPORT_TITLE",                   "Report on simulated treatment with antihypertensive drugs"},
        { "GENERATION_ANALYSIS_SETTINGS",   "Patient data"},
        { "BASELINE_CHARACTERISTICS",       "Population characteristics"},
        { "CHARACTERISTICS",                "Characteristics"},
        { "VALUES",                         "Values, mean ± SD (min - max)"},

        { "GENERAL_DATA",               "General info"},
        { "PRESSURE",                   "BP monitoring"},
        { "ECG",                        "Electrocardiography"},
        { "HEART_ULTRASOUND",           "Heart ultrasound"},
        { "BLOOD_TEST",                 "Blood test"},
        { "BIOCHEMISTRY",               "Biochemistry"},
        { "DISEASES",                   "Diseases"},
        { "GENETICS",                   "Genetics"},
        { "CALCULATED_PARAMETERS",      "Calculated parameters"},

        { "AGE",                        "Age, years"},
        { "HEIGHT",                     "Height, cm"},
        { "WEIGHT",                     "Weight, kg"},
        { "SEX",                        "Sex"},
        { "RACE",                       "Race"},
        { "BMI",                        "Body mass index, kg/m<sup>2</sup>"},
        { "SEX_DISTRIBUTION",           "Sex, men/women"},
        { "RACE_DISTRIBUTION",          "Race, caucasians/negroids"},

        { "PS",                         "Systolic blood pressure, mmHg"},
        { "PD",                         "Diastolic blood pressure, mmHg"},

        { "HR",                         "Heart rate, beats/min"},

        { "SV",                         "Stroke volume, ml"},
        { "EF",                         "Ejection Fraction, %"},

        { "HE",                         "Hemoglobin, g/l"},
        { "HCT",                        "Hematocrit, %"},

        { "TP",                         "Total protein, g/l"},
        { "GLUCOSE",                    "Glucose, mmol/l"},
        { "UREA",                       "Urea, mmol/l"},
        { "CREATININE",                 "Creatinine, μmol/l"},
        { "POTASSIUM",                  "Potassium, mmol/l"},
        { "SODIUM",                     "Sodium, mmol/l"},

        { "GENE_ADRB1",                 "Gene ADRB1"},
        { "POLYMORPHISM_ARG389GLY",     "Polymorphism Arg389Gly"},
        { "GLY_GLY",                    "Gly/Gly"},
        { "ARG_GLY",                    "Arg/Gly"},
        { "ARG_ARG",                    "Arg/Arg"},

        { "HYPERTENSION",               "Arterial hypertension"},
        { "CHF",                        "Chronic heart failure"},
        { "CHF_CLASS",                  " - Functional class (NYHA)"},
        { "PH",                         "Pulmonary hypertension"},
        { "LVDD",                       "LV diastolic dysfunction"},
        { "LVDD_TYPE",                  " - Type of LV diastolic dysfunction"},
        { "LVH",                        "LV hypertrophy"},
        { "CRF",                        "Chronic renal failure"},
        { "ATHEROSCLEROSIS",            "Carotid and/or coronary atherosclerosis"},
        { "COPD",   			        "Chronic obstructive pulmonary disease"},
        { "MR",                         "Mitral regurgitation"},
        { "MR_STAGE",                   " - Mitral regurgitation stage"},
        { "TR",                         "Tricuspid regurgitation"},
        { "TR_STAGE",                   " - Tricuspid regurgitation stage"},
        { "AR",                         "Aortic regurgitation"},
        { "AR_STAGE",                   " - Aortic regurgitation stage"},
        { "PR",                         "Pulmonary regurgitation"},
        { "PR_STAGE",                   " - Pulmonary regurgitation stage"},

        { "BV",                         "Total blood volume (Nadler equation), ml"},
        { "MAP",                        "Mean arterial pressure, mmHg"},
        { "CL",                         "Cardiac cycle length, sec"},
        { "CO",                         "Cardiac output, l/min"},
        { "LVEDV",                      "Left ventricular end-diastolic volume, ml"},
        { "LVESV",                      "Left ventricular end-systolic volume, ml"},
        { "VIS",                        "Blood viscosity, cP"},
        { "GFR",                        "Glomerular filtration rate, l/min (CKD-EPI)"},

        { "MAN",                        "man"},
        { "WOMAN",                      "woman"},

        { "CAUCASOID",                  "caucasoid"},
        { "NEGROID",                    "negroid"},

        { "UNKNOWN",                    "unknown"},
        { "YES",                        "yes"},
        { "NO",                         "no"},

        { "CLASS_I",                    "class I"},
        { "CLASS_II",                   "class II"},
        { "CLASS_III",                  "class III"},
        { "CLASS_IV",                   "class IV"},

        { "TYPE_1",                     "type 1"},
        { "TYPE_2",                     "type 2"},
        { "TYPE_3",                     "type 3"},

        { "MILD",                       "mild"},
        { "MODERATE",                   "moderate"},
        { "SEVERE",                     "severe"},

        { "POPULATION",                 "Population"},
        { "DRUGS",                      "Drugs"},
        { "GENERATION_INFO",            "Patient data"},
        { "TREATMENT_TIME",             "Treatment time (s)"},

        { "POPULATION_SIZE",            "Population size"},
        { "TREATMENT_RESULTS",          "Treatment results"},
        { "REGIMENS",                   "Regimens"},

        //Messages
        { "ILLEGAL_OUTPUT",                     "Please specify a folder to save results of the analysis."},
        { "ILLEGAL_HEIGHT",                     "Patient height is a required parameter for modeling, please specify it."},
        { "ILLEGAL_WEIGHT",                     "Patient weight is a required parameter for modeling, please specify it."},
        { "ILLEGAL_PS",                         "Systolic blood pressure is a required parameter for modeling, please specify it."},
        { "ILLEGAL_PD",                         "Diastolic blood pressure is a required parameter for modeling, please specify it."},
        { "ILLEGAL_HR",                         "Heart rate is a required parameter for modeling, please specify it."},
        { "ILLEGAL_DRUGS",                      "Please specify drugs to simulate treatment."},
        { "ILLEGAL_POPULATION",                 "Please specify table data collection with virtual population."},

        { "ERROR_SLOW_MODEL_EXPERIMENT_GENERATION", "Internal error occurred: cannot generate experimental file for the slow model."},
        { "ERROR_FAST_MODEL_EXPERIMENT_GENERATION",  "Internal error occurred: cannot generate experimental file for the fast model."},

        { "INFO_INITIALIZATION",                "Initialization..."},
        { "INFO_GENERATION",                    "Generating {0} patient..."},
        { "INFO_SLOW_MODEL_GENERATION",         "Generating values for the slow model..."},
        { "INFO_FAST_MODEL_GENERATION",         "Generating values for the fast model..."},
        { "INFO_MERGING",                       "Merging values obtained for slow and fast models..."},
        { "INFO_OPTIMIZATION_RESULT",           "Fixed value {0} = {1}: penalty function = {2}, objective function = {3}"},
        { "INFO_INCORRECT_VALUE",               "The following value is outside cutoff: {0} = {1}"},
        { "INFO_INCORRECT_SOLUTION",            "Incorrect solution. The next attempt of generation."},
        { "INFO_SLOW_MODEL_GENERATION_IS_FAILED",   "All attempts to generate values for the slow model are failed. "
                + "Generation of a virtual population with the given settings is not possible."},
        { "INFO_MERGING_IS_FAILED",             "Steady state of the composite model is not found. "
                + "The merge of the values ​​is failed. The next attempt of generation."},
        { "INFO_MERGING_IS_SUCCESSFUL",         "Steady state of the composite model is found at time = {0}."},
        { "INFO_INCORRECT_SOLUTION_AFTER_MERGING","The parameter constraint {0} is not satisfied after the merging. The deviation is {1}. The next attempt of generation."},
        { "INFO_SODIUM_LOAD_EXPERIMENT",        "Testing the sodium load experiment..."},
        { "INFO_FAILED_SODIUM_LOAD_EXPERIMENT", "Sodium load experiment is failed. The next attempt of generation."},


        { "SEVERE_DRUG_NOT_FOUND",              "Drug parameter not found: {0}."},
        { "INFO_UNKNOWN_COLUMN",                "The variable {0} is not found in the model. Values of the corresponding column in the population table are not be taken into account."},
        { "SEVERE_SIMULATION_TASK_CREATION",    "Error during the simulation task creation: {0}"},
        { "INFO_CURRENT_PATIENT",               "{0}: {1}/{2} patient done"},
        { "INFO_TREATMENT_SIMULATION",          "Treatment simulation..."},
        { "INFO_PATIENT_IS_FAILED",             "Treatment simulation of the patient {0} is failed"},
        
        { "ALISKIREN_150",                      "Aliskiren 150 mg"},
        { "ALISKIREN_300",                      "Aliskiren 300 mg"},
        { "AZILSARTAN_40",                      "Azilsartan 40 mg"},
        { "AMLODIPINE_5",                       "Amlodipine 5 mg"},
        { "BISOPROLOL_5",                       "Bisoprolol 5 mg"},
        { "HCTZ_12_5",                          "HCTZ 12.5 mg"},
        { "LOSARTAN_50",                        "Losartan 50 mg"},
        { "LOSARTAN_100",                       "Losartan 100 mg"},
        { "ENALAPRIL_20",                       "Enalapril 20 mg"},

        { "ALISKIREN_150_AMLODIPINE_5",         "Aliskiren 150 mg/Amlodipine 5 mg"},
        { "ALISKIREN_150_BISOPROLOL_5",         "Aliskiren 150 mg/Bisoprolol 5 mg"},
        { "ALISKIREN_150_HCTZ_12_5",            "Aliskiren 150 mg/HCTZ 12.5 mg"},
        { "ALISKIREN_300_AMLODIPINE_5",         "Aliskiren 300 mg/Amlodipine 5 mg"},
        { "ALISKIREN_300_BISOPROLOL_5",         "Aliskiren 300 mg/Bisoprolol 5 mg"},
        { "ALISKIREN_300_HCTZ_12_5",            "Aliskiren 300 mg/HCTZ 12.5 mg"},
        { "AZILSARTAN_40_AMLODIPINE_5",         "Azilsartan 40 mg/Amlodipine 5 mg"},
        { "AZILSARTAN_40_BISOPROLOL_5",         "Azilsartan 40 mg/Bisoprolol 5 mg"},
        { "AZILSARTAN_40_HCTZ_12_5",            "Azilsartan 40 mg/HCTZ 12.5 mg"},
        { "LOSARTAN_50_AMLODIPINE_5",           "Losartan 50 mg/Amlodipine 5 mg"},
        { "LOSARTAN_50_BISOPROLOL_5",           "Losartan 50 mg/Bisoprolol 5 mg"},
        { "LOSARTAN_50_HCTZ_12_5",              "Losartan 50 mg/HCTZ 12.5 mg"},
        { "LOSARTAN_100_AMLODIPINE_5",          "Losartan 100 mg/Amlodipine 5 mg"},
        { "LOSARTAN_100_BISOPROLOL_5",          "Losartan 100 mg/Bisoprolol 5 mg"},
        { "LOSARTAN_100_HCTZ_12_5",             "Losartan 100 mg/HCTZ 12.5 mg"},
        { "ENALAPRIL_20_AMLODIPINE_5",          "Enalapril 20 mg/Amlodipine 5 mg"},
        { "ENALAPRIL_20_BISOPROLOL_5",          "Enalapril 20 mg/Bisoprolol 5 mg"},
        { "ENALAPRIL_20_HCTZ_12_5",             "Enalapril 20 mg/HCTZ 12.5 mg"},

        { "ALISKIREN_150_AMLODIPINE_5_BISOPROLOL_5",    "Aliskiren 150 mg/Amlodipine 5 mg/Bisoprolol 5 mg"},
        { "ALISKIREN_150_AMLODIPINE_5_HCTZ_12_5",       "Aliskiren 150 mg/Amlodipine 5 mg/HCTZ 12.5 mg"},
        { "ALISKIREN_150_BISOPROLOL_5_HCTZ_12_5",       "Aliskiren 150 mg/Bisoprolol 5 mg/HCTZ 12.5 mg"},
        { "ALISKIREN_300_AMLODIPINE_5_BISOPROLOL_5",    "Aliskiren 300 mg/Amlodipine 5 mg/Bisoprolol 5 mg"},
        { "ALISKIREN_300_AMLODIPINE_5_HCTZ_12_5",       "Aliskiren 300 mg/Amlodipine 5 mg/HCTZ 12.5 mg"},
        { "ALISKIREN_300_BISOPROLOL_5_HCTZ_12_5",       "Aliskiren 300 mg/Bisoprolol 5 mg/HCTZ 12.5 mg"},
        { "AZILSARTAN_40_AMLODIPINE_5_BISOPROLOL_5",    "Azilsartan 40 mg/Amlodipine 5 mg/Bisoprolol 5 mg"},
        { "AZILSARTAN_40_AMLODIPINE_5_HCTZ_12_5",       "Azilsartan 40 mg/Amlodipine 5 mg/HCTZ 12.5 mg"},
        { "AZILSARTAN_40_BISOPROLOL_5_HCTZ_12_5",       "Azilsartan 40 mg/Bisoprolol 5 mg/HCTZ 12.5 mg"},
        { "LOSARTAN_50_AMLODIPINE_5_BISOPROLOL_5",      "Losartan 50 mg/Amlodipine 5 mg/Bisoprolol 5 mg"},
        { "LOSARTAN_50_AMLODIPINE_5_HCTZ_12_5",         "Losartan 50 mg/Amlodipine 5 mg/HCTZ 12.5 mg"},
        { "LOSARTAN_50_BISOPROLOL_5_HCTZ_12_5",         "Losartan 50 mg/Bisoprolol 5 mg/HCTZ 12.5 mg"},
        { "LOSARTAN_100_AMLODIPINE_5_BISOPROLOL_5",     "Losartan 100 mg/Amlodipine 5 mg/Bisoprolol 5 mg"},
        { "LOSARTAN_100_AMLODIPINE_5_HCTZ_12_5",        "Losartan 100 mg/Amlodipine 5 mg/HCTZ 12.5 mg"},
        { "LOSARTAN_100_BISOPROLOL_5_HCTZ_12_5",        "Losartan 100 mg/Bisoprolol 5 mg/HCTZ 12.5 mg"},
        { "ENALAPRIL_20_AMLODIPINE_5_BISOPROLOL_5",     "Enalapril 20 mg/Amlodipine 5 mg/Bisoprolol 5 mg"},
        { "ENALAPRIL_20_AMLODIPINE_5_HCTZ_12_5",        "Enalapril 20 mg/Amlodipine 5 mg/HCTZ 12.5 mg"},
        { "ENALAPRIL_20_BISOPROLOL_5_HCTZ_12_5",        "Enalapril 20 mg/Bisoprolol 5 mg/HCTZ 12.5 mg"},
    };
}