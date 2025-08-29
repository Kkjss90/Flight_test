## Компиляция

Сборка с помощью Maven:

```bash
mvn clean compile
```
Создание исполняемого JAR-файла со всеми зависимостями:

```bash
mvn clean package
```
После успешной сборки в папке target/ будут созданы:

Flight_test-1.0-SNAPSHOT.jar - основной JAR-файл c необходимыми зависимостями.

## Запуск

Запуск после сборки с указанием файла tickets.json:

```bash
java -jar target/Flight_test-1.0-SNAPSHOT.jar <path-to-file>
```

Также можно скачать файл Flight_test-1.0-SNAPSHOT.jar из папки target и запустить:

```bash
java -jar Flight_test-1.0-SNAPSHOT.jar <path-to-file>
```
