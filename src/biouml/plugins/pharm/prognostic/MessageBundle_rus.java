package biouml.plugins.pharm.prognostic;

public class MessageBundle_rus extends MessageBundle
{
    @Override
    protected Object[][] getContents()
    {
        return contents;
    }
    private final static Object[][] contents =
    {
        //Properties
        { "PARAMETERS",                 "Параметры"},

        { "INPUT_FILES",                "Входные файлы"},
        { "OUTPUT_FILES",               "Сохранение результатов"},
        { "PATIENTS_NUMBER",            "Число пациентов для генерации"},
        { "PATIENT_PHYSIOLOGY",         "Физиология пациента"},
        { "OPTIMIZATION_SETTINGS",      "Системные настройки оптимизации"},
        { "SLOW_MODEL_NUM_OF_ITERATIONS",    "Число итераций для оптимизации медленной модели"},
        { "SLOW_MODEL_SURVIVAL_SIZE",        "Число выживания для оптимизации медленной модели"},
        { "FAST_MODEL_NUM_OF_ITERATIONS",    "Число итераций для оптимизации быстрой модели"},
        { "FAST_MODEL_SURVIVAL_SIZE",        "Число выживания для оптимизации быстрой модели"},
        { "POSSIBLE_DEVIATION",        	"Возможное отклонение от известных значений переменных в популяции"},
        { "CHECK_SODIUM_LOAD_EXPERIMENT","Проверка эксперимента с солевой нагрузкой"},
        { "DECREASE_IN_SBP",            "Снижение САД, мм рт. ст."},
        { "DECREASE_IN_DBP",            "Снижение ДАД, мм рт. ст."},
        { "SBP",                        "САД, мм рт. ст."},
        { "DBP",                        "ДАД, мм рт. ст."},

        { "SYSTOLIC_BLOOD_PRESSURE",    "Систолическое давление"},
        { "DIASTOLIC_BLOOD_PRESSURE",   "Диастолическое давление"},
        { "MMHG",                       "мм рт. ст."},

        { "REPORT_TITLE",                   "Отчет о моделировании лечения антигипертензивными препаратами"},
        { "GENERATION_ANALYSIS_SETTINGS",   "Данные пациента"},
        { "BASELINE_CHARACTERISTICS",       "Характеристики популяции"},
        { "CHARACTERISTICS",                "Характеристики"},
        { "VALUES",                         "Значения, среднее ± SD (мин - макс)"},

        { "GENERAL_DATA",           "Общая информация"},
        { "PRESSURE",               "Мониторинг АД"},
        { "ECG",                    "Электрокардиография"},
        { "HEART_ULTRASOUND",       "УЗИ сердца"},
        { "BLOOD_TEST",             "Общий анализ крови"},
        { "BIOCHEMISTRY",           "Биохимия"},
        { "DISEASES",               "Заболевания"},
        { "GENETICS",               "Генетика"},
        { "CALCULATED_PARAMETERS",  "Расчетные параметры"},

        { "AGE",                    "Возраст, лет"},
        { "HEIGHT",                 "Рост, см"},
        { "WEIGHT",                 "Вес, кг"},
        { "SEX",                    "Пол"},
        { "RACE",                   "Раса"},
        { "BMI",                    "Индекс массы тела, кг/м<sup>2</sup>"},
        { "SEX_DISTRIBUTION",       "Пол, мужчины/женщины"},
        { "RACE_DISTRIBUTION",      "Раса, европеоиды/негроиды"},

        { "PS",                     "Систолическое артериальное давление, мм рт. ст."},
        { "PD",                     "Диастолическое артериальное давление, мм рт. ст."},

        { "HR",                     "Частота сердечных сокращений, уд/мин"},

        { "SV",                     "Ударный Объем, мл"},
        { "EF",                     "Фракция Выброса, %"},

        { "HE",                     "Гемоглобин, г/л"},
        { "HCT",                    "Гематокрит, %"},

        { "TP",                     "Белок общий, г/л"},
        { "GLUCOSE",                "Глюкоза, ммоль/л"},
        { "UREA",                   "Мочевина, ммоль/л"},
        { "CREATININE",             "Креатинин, мкмоль/л"},
        { "POTASSIUM",              "Калий, ммоль/л"},
        { "SODIUM",                 "Натрий, ммоль/л"},

        { "GENE_ADRB1",             "Ген ADRB1"},
        { "POLYMORPHISM_ARG389GLY", "Полиморфизм Arg389Gly"},
        { "GLY_GLY",                "Gly/Gly"},
        { "ARG_GLY",                "Arg/Gly"},
        { "ARG_ARG",                "Arg/Arg"},

        { "HYPERTENSION",           "Артериальная гипертензия"},
        { "CHF",                    "Хроническая сердечная недостаточность"},
        { "CHF_CLASS",              " - Функциональный класс (NYHA)"},
        { "PH",                     "Легочная гипертензия"},
        { "LVDD",                   "Диастолическая дисфункция ЛЖ"},
        { "LVDD_TYPE",              " - Тип диастолической дисфункции ЛЖ"},
        { "LVH",                    "Гипертрофия ЛЖ"},
        { "CRF",                    "Хроническая почечная недостаточность"},
        { "ATHEROSCLEROSIS",        "Атеросклероз сонных и/или коронарных артерий"},
        { "MR",                     "Митральная регургитация"},
        { "MR_STAGE",               " - Стадия митральной регургитации"},
        { "TR",                     "Трикуспидальная регургитация"},
        { "TR_STAGE",               " - Стадия трикуспидальной регургитации"},
        { "AR",                     "Аортальная регургитация"},
        { "AR_STAGE",               " - Стадия аортальной регургитации"},
        { "PR",                     "Легочная регургитация"},
        { "PR_STAGE",               " - Стадия легочной регургитации"},

        { "BV",                     "Общий объем крови (формула Надлера), мл"},
        { "MAP",                    "Среднее артериальное давление, мм рт. ст."},
        { "CL",                     "Длина сердечного цикла, сек"},
        { "CO",                     "Сердечный выброс, л/мин"},
        { "LVEDV",                  "Конечный диастолический объем левого желудочка, мл"},
        { "LVESV",                  "Конечный систолический объем левого желудочка, мл"},
        { "VIS",                    "Вязкость крови, спз"},
        { "GFR",                    "Скорость клубочковой фильтрации, л/мин (CKD-EPI)"},

        { "MAN",                    "мужчина"},
        { "WOMAN",                  "женщина"},

        { "CAUCASOID",              "европеоидная"},
        { "NEGROID",                "негроидная"},

        { "UNKNOWN",                "неизвестно"},
        { "YES",                    "есть"},
        { "NO",                     "нет"},

        { "CLASS_I",                "класс I"},
        { "CLASS_II",               "класс II"},
        { "CLASS_III",              "класс III"},
        { "CLASS_IV",               "класс IV"},

        { "TYPE_1",                 "тип 1"},
        { "TYPE_2",                 "тип 2"},
        { "TYPE_3",                 "тип 3"},

        { "MILD",                   "легкая"},
        { "MODERATE",               "умеренная"},
        { "SEVERE",                 "тяжелая"},

        { "POPULATION",             "Популяция"},
        { "DRUGS",                  "Лекарства"},
        { "GENERATION_INFO",        "Данные пациента"},
        { "TREATMENT_TIME",         "Время лечения (сек)"},

        { "POPULATION_SIZE",        "Размер популяции"},
        { "TREATMENT_RESULTS",      "Результаты лечения"},
        { "REGIMENS",               "Схемы лечения"},

        //Messages
        { "ILLEGAL_OUTPUT",                     "Выберите папку для сохранения результатов."},
        { "ILLEGAL_HEIGHT",                     "Рост пациента - необходимый для моделирования параметр, пожалуйста, укажите его."},
        { "ILLEGAL_WEIGHT",                     "Вес пациента - необходимый для моделирования параметр, пожалуйста, укажите его."},
        { "ILLEGAL_PS",                         "Систолическое артериальное давление - необходимый для моделирования параметр, пожалуйста, укажите его."},
        { "ILLEGAL_PD",                         "Диастолическое артериальное давление - необходимый для моделирования параметр, пожалуйста, укажите его."},
        { "ILLEGAL_HR",                         "Частота сердечных сокращений - необходимый для моделирования параметр, пожалуйста, укажите его."},
        { "ILLEGAL_DRUGS",                      "Выберите лекарства для симуляции лечения."},
        { "ILLEGAL_POPULATION",                 "Выберите таблицу с виртуальной популяцией."},

        { "ERROR_SLOW_MODEL_EXPERIMENT_GENERATION", "Произошла внутренняя ошибка: невозможно создать экспериментальный файл для медленной модели."},
        { "ERROR_FAST_MODEL_EXPERIMENT_GENERATION",  "Произошла внутренняя ошибка: невозможно создать экспериментальный файл для быстрой модели."},

        { "INFO_INITIALIZATION",                "Инициализация..."},
        { "INFO_GENERATION",                    "Генерация {0} пациента..."},
        { "INFO_SLOW_MODEL_GENERATION",         "Генерация значений для медленной модели..."},
        { "INFO_FAST_MODEL_GENERATION",         "Генерация значений для быстрой модели..."},
        { "INFO_MERGING",                       "Объединение значений, полученных для быстрой и медленной моделей..."},
        { "INFO_OPTIMIZATION_RESULT",           "Фиксировано значение {0} = {1}: штрафная функция = {2}, целевая функция = {3}"},
        { "INFO_INCORRECT_VALUE",               "Получено значение за пределами отсечки: {0} = {1}"},
        { "INFO_INCORRECT_SOLUTION",            "Решение некорректно. Следующая попытка генерации."},
        { "INFO_SLOW_MODEL_GENERATION_IS_FAILED",   "Все попытки сгенерировать значения для медленной модели потерпели неудачу. "
                + "Создание виртуальной популяции с данными настройками невозможно."},
        { "INFO_MERGING_IS_FAILED",             "Устойчивое состояние композитной модели не найдено. "
                + "Объединение значений не удалось. Следующая попытка генерации."},
        { "INFO_MERGING_IS_SUCCESSFUL",         "Устойчивое состояние композитной модели найдено на шаге time = {0}."},
        { "INFO_INCORRECT_SOLUTION_AFTER_MERGING","Ограничение параметров {0} после объединения значений не выполнено. Отклонение составляет {1}. Следующая попытка генерации."},
        { "INFO_SODIUM_LOAD_EXPERIMENT",        "Тестирование эксперимента с натриевой нагрузкой..."},
        { "INFO_FAILED_SODIUM_LOAD_EXPERIMENT", "Эксперимент с натриевой нагрузкой не удался. Следующая попытка генерации."},

        { "SEVERE_DRUG_NOT_FOUND",              "Параметр не найден: {0}."},
        { "INFO_UNKNOWN_COLUMN",                "Переменная {0} в модели не найдена. Значения соответствующего столбца в таблице популяции не учитываются."},
        { "SEVERE_SIMULATION_TASK_CREATION",    "Ошибка при создании задачи моделирования: {0}"},
        { "INFO_CURRENT_PATIENT",               "{0}: {1}/{2} пациент готов"},
        { "INFO_TREATMENT_SIMULATION",          "Симуляция лечения..."},
        { "INFO_PATIENT_IS_FAILED",             "Симуляция лечения пациента {0} не удалась"},

        { "ALISKIREN_150",                      "Алискирен 150 мг"},
        { "ALISKIREN_300",                      "Алискирен 300 мг"},
        { "AZILSARTAN_40",                      "Азилсартан 40 мг"},
        { "AMLODIPINE_5",                       "Амлодипин 5 мг"},
        { "BISOPROLOL_5",                       "Бисопролол 5 мг"},
        { "HCTZ_12_5",                          "Гидрохлоротиазид 12.5 мг"},
        { "LOSARTAN_50",                        "Лозартан 50 мг"},
        { "LOSARTAN_100",                       "Лозартан 100 мг"},
        { "ENALAPRIL_20",                       "Эналаприл 20 мг"},

        { "ALISKIREN_150_AMLODIPINE_5",         "Алискирен 150 мг/Амлодипин 5 мг"},
        { "ALISKIREN_150_BISOPROLOL_5",         "Алискирен 150 мг/Бисопролол 5 мг"},
        { "ALISKIREN_150_HCTZ_12_5",            "Алискирен 150 мг/Гидрохлоротиазид 12.5 мг"},
        { "ALISKIREN_300_AMLODIPINE_5",         "Алискирен 300 мг/Амлодипин 5 мг"},
        { "ALISKIREN_300_BISOPROLOL_5",         "Алискирен 300 мг/Бисопролол 5 мг"},
        { "ALISKIREN_300_HCTZ_12_5",            "Алискирен 300 мг/Гидрохлоротиазид 12.5 мг"},
        { "AZILSARTAN_40_AMLODIPINE_5",         "Азилсартан 40 мг/Амлодипин 5 мг"},
        { "AZILSARTAN_40_BISOPROLOL_5",         "Азилсартан 40 мг/Бисопролол 5 мг"},
        { "AZILSARTAN_40_HCTZ_12_5",            "Азилсартан 40 мг/Гидрохлоротиазид 12.5 мг"},
        { "LOSARTAN_50_AMLODIPINE_5",           "Лозартан 50 мг/Амлодипин 5 мг"},
        { "LOSARTAN_50_BISOPROLOL_5",           "Лозартан 50 мг/Бисопролол 5 мг"},
        { "LOSARTAN_50_HCTZ_12_5",              "Лозартан 50 мг/Гидрохлоротиазид 12.5 мг"},
        { "LOSARTAN_100_AMLODIPINE_5",          "Лозартан 100 мг/Амлодипин 5 мг"},
        { "LOSARTAN_100_BISOPROLOL_5",          "Лозартан 100 мг/Бисопролол 5 мг"},
        { "LOSARTAN_100_HCTZ_12_5",             "Лозартан 100 мг/Гидрохлоротиазид 12.5 мг"},
        { "ENALAPRIL_20_AMLODIPINE_5",          "Эналаприл 20 мг/Амлодипин 5 мг"},
        { "ENALAPRIL_20_BISOPROLOL_5",          "Эналаприл 20 мг/Бисопролол 5 мг"},
        { "ENALAPRIL_20_HCTZ_12_5",             "Эналаприл 20 мг/Гидрохлоротиазид 12.5 мг"},

        { "ALISKIREN_150_AMLODIPINE_5_BISOPROLOL_5",    "Алискирен 150 мг/Амлодипин 5 мг/Бисопролол 5 мг"},
        { "ALISKIREN_150_AMLODIPINE_5_HCTZ_12_5",       "Алискирен 150 мг/Амлодипин 5 мг/Гидрохлоротиазид 12.5 мг"},
        { "ALISKIREN_150_BISOPROLOL_5_HCTZ_12_5",       "Алискирен 150 мг/Бисопролол 5 мг/Гидрохлоротиазид 12.5 мг"},
        { "ALISKIREN_300_AMLODIPINE_5_BISOPROLOL_5",    "Алискирен 300 мг/Амлодипин 5 мг/Бисопролол 5 мг"},
        { "ALISKIREN_300_AMLODIPINE_5_HCTZ_12_5",       "Алискирен 300 мг/Амлодипин 5 мг/Гидрохлоротиазид 12.5 мг"},
        { "ALISKIREN_300_BISOPROLOL_5_HCTZ_12_5",       "Алискирен 300 мг/Бисопролол 5 мг/Гидрохлоротиазид 12.5 мг"},
        { "AZILSARTAN_40_AMLODIPINE_5_BISOPROLOL_5",    "Азилсартан 40 мг/Амлодипин 5 мг/Бисопролол 5 мг"},
        { "AZILSARTAN_40_AMLODIPINE_5_HCTZ_12_5",       "Азилсартан 40 мг/Амлодипин 5 мг/Гидрохлоротиазид 12.5 мг"},
        { "AZILSARTAN_40_BISOPROLOL_5_HCTZ_12_5",       "Азилсартан 40 мг/Бисопролол 5 мг/Гидрохлоротиазид 12.5 мг"},
        { "LOSARTAN_50_AMLODIPINE_5_BISOPROLOL_5",      "Лозартан 50 мг/Амлодипин 5 мг/Бисопролол 5 мг"},
        { "LOSARTAN_50_AMLODIPINE_5_HCTZ_12_5",         "Лозартан 50 мг/Амлодипин 5 мг/Гидрохлоротиазид 12.5 мг"},
        { "LOSARTAN_50_BISOPROLOL_5_HCTZ_12_5",         "Лозартан 50 мг/Бисопролол 5 мг/Гидрохлоротиазид 12.5 мг"},
        { "LOSARTAN_100_AMLODIPINE_5_BISOPROLOL_5",     "Лозартан 100 мг/Амлодипин 5 мг/Бисопролол 5 мг"},
        { "LOSARTAN_100_AMLODIPINE_5_HCTZ_12_5",        "Лозартан 100 мг/Амлодипин 5 мг/Гидрохлоротиазид 12.5 мг"},
        { "LOSARTAN_100_BISOPROLOL_5_HCTZ_12_5",        "Лозартан 100 мг/Бисопролол 5 мг/Гидрохлоротиазид 12.5 мг"},
        { "ENALAPRIL_20_AMLODIPINE_5_BISOPROLOL_5",     "Эналаприл 20 мг/Амлодипин 5 мг/Бисопролол 5 мг"},
        { "ENALAPRIL_20_AMLODIPINE_5_HCTZ_12_5",        "Эналаприл 20 мг/Амлодипин 5 мг/Гидрохлоротиазид 12.5 мг"},
        { "ENALAPRIL_20_BISOPROLOL_5_HCTZ_12_5",        "Эналаприл 20 мг/Бисопролол 5 мг/Гидрохлоротиазид 12.5 мг"},
    };
}
