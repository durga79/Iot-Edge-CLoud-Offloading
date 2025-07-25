@echo off
echo ===================================================
echo IoT Edge-Cloud Offloading Simulation Runner
echo ===================================================
echo.

:: Check if Java is installed
echo Checking Java installation...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH.
    echo Please install Java 8 or higher and try again.
    echo You can download it from: https://www.oracle.com/java/technologies/downloads/
    pause
    exit /b 1
)
echo Java is installed. Proceeding...
echo.

:: Check if Maven is installed
echo Checking Maven installation...
call mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo WARNING: Maven is not installed or not in PATH.
    echo Will attempt to run using the pre-compiled JAR file.
    goto RunJar
)
echo Maven is installed. Proceeding...
echo.

:: Clean and compile the project
echo Cleaning and compiling the project...
call mvn clean compile
if %errorlevel% neq 0 (
    echo WARNING: Maven compile failed. Will attempt to run using the pre-compiled JAR file.
    goto RunJar
)
echo Compilation successful.
echo.

:: Try running with Maven exec plugin
echo Running simulation using Maven exec plugin...
call mvn exec:java -Dexec.mainClass="org.hyboff.simulation.TestSimulation"
if %errorlevel% neq 0 (
    echo WARNING: Maven exec failed. Will attempt to run using the JAR file.
    goto BuildJar
)
goto End

:BuildJar
:: Build the JAR file
echo Building JAR file...
call mvn package
if %errorlevel% neq 0 (
    echo ERROR: Failed to build JAR file.
    echo Please check the Maven logs for details.
    pause
    exit /b 1
)
echo JAR file built successfully.
echo.

:RunJar
:: Run the JAR file
echo Running simulation from JAR file...
if exist "target\IoT-Edge-Cloud-Offloading-1.0-SNAPSHOT-jar-with-dependencies.jar" (
    java -jar "target\IoT-Edge-Cloud-Offloading-1.0-SNAPSHOT-jar-with-dependencies.jar"
) else if exist "IoT-Edge-Cloud-Offloading-1.0-SNAPSHOT-jar-with-dependencies.jar" (
    java -jar "IoT-Edge-Cloud-Offloading-1.0-SNAPSHOT-jar-with-dependencies.jar"
) else if exist "D:\projects\Iot-Edge-CLoud-Offloading\target\IoT-Edge-Cloud-Offloading-1.0-SNAPSHOT-jar-with-dependencies.jar" (
    java -jar "D:\projects\Iot-Edge-CLoud-Offloading\target\IoT-Edge-Cloud-Offloading-1.0-SNAPSHOT-jar-with-dependencies.jar"
) else if exist "D:\projects\Iot-Edge-CLoud-Offloading\IoT-Edge-Cloud-Offloading-1.0-SNAPSHOT-jar-with-dependencies.jar" (
    java -jar "D:\projects\Iot-Edge-CLoud-Offloading\IoT-Edge-Cloud-Offloading-1.0-SNAPSHOT-jar-with-dependencies.jar"
) else (
    echo ERROR: Could not find the JAR file.
    echo Please make sure you have the JAR file in the current directory or in the target directory.
    echo.
    echo Looking for: IoT-Edge-Cloud-Offloading-1.0-SNAPSHOT-jar-with-dependencies.jar
    echo Current directory: %CD%
    pause
    exit /b 1
)

:End
echo.
echo ===================================================
echo Simulation completed!
echo ===================================================
pause
