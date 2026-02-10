version 1.0

task FileCreationVsProcessing {
  input {
    String name
  }
  
  # ============================================
  # ЭТАП 1: СОЗДАНИЕ - внутри command
  # ============================================
  command <<<
    # Здесь ТОЛЬКО создание файла
    echo "10" > numbers.txt
    echo "20" >> numbers.txt
    echo "30" >> numbers.txt
    echo "40" >> numbers.txt
    echo "50" >> numbers.txt
    
    echo "Name: ~{name}" > metadata.txt
  >>>
  
  # ============================================
  # ЭТАП 2: ОБРАБОТКА - внутри output (снаружи command, но внутри таска!)
  # ============================================
  output {
    # Сначала получаем файлы
    File numbers_file = "numbers.txt"
    File metadata_file = "metadata.txt"
    
    # --- ТЕПЕРЬ ОБРАБАТЫВАЕМ ЭТИ ФАЙЛЫ ---
    # Всё ниже - это обработка СНАРУЖИ command, но ВНУТРИ таска!
    
    # Читаем файл в массив
    Array[Int] numbers = read_lines("numbers.txt")
    
    # Обрабатываем массив - берём отдельные элементы
    Int first = numbers[0]           # 10
    Int second = numbers[1]          # 20
    Int last = numbers[4]            # 50
    
    # Выполняем вычисления
    Int sum = first + second         # 30
    Int product = first * second     # 200
    Int total = numbers[0] + numbers[1] + numbers[2] + numbers[3] + numbers[4]  # 150
    
    # Анализируем данные
    Int length = length(numbers)     # 5
    Boolean is_long = length > 3     # true
    
    # Читаем и обрабатываем текстовый файл
    String metadata_content = read_string("metadata.txt")
    Array[String] metadata_lines = read_lines("metadata.txt")
    String first_line = metadata_lines[0]
    
    # Условная логика на основе обработанных данных
    String result_status = if total > 100 then "HIGH" else "LOW"
    
    # Создаём производные данные
    String report = "Total: ~{total}, Status: ~{result_status}"
  }
  
  runtime {
    docker: "ubuntu:latest"
  }
}

workflow Example {
  call FileCreationVsProcessing {
    input: name = "Test"
  }
  
  output {
    # Видим результаты обработки, которая была СНАРУЖИ command
    Int sum = FileCreationVsProcessing.sum
    Int total = FileCreationVsProcessing.total
    String status = FileCreationVsProcessing.result_status
    String report = FileCreationVsProcessing.report
  }
}