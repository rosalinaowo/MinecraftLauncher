:: Define variables
set buildDir=tmpBuild
:: Put your Java bin path here
set javaBin=C:\Program Files (x86)\OpenLogic\jdk-8.0.402.06-hotspot\bin
set classPath=import\gson-2.10.1.jar;import\commons-lang3-3.14.0.jar;import\commons-text-1.11.0.jar

:: Clean build directory
:: if exist %buildDir% (
::	del /s /q %buildDir%\*
::	rmdir /s /q %buildDir%
::)
mkdir %buildDir%

:: Compile java files
"%javaBin%\javac.exe" -d %buildDir% -sourcepath . -cp %classPath% *.java

:: Copy resources
mkdir %buildDir%\resources
mkdir %buildDir%\import
xcopy /s /e resources\* %buildDir%\resources
xcopy /s /e import\* %buildDir%\import

:: Extract dependencies
cd %buildDir%
for %%I in (import\*.jar) do (
	"%javaBin%\jar.exe" xf "%%I"
)
:: Delete useless directories
del /s /q META-INF
rmdir /s /q META-INF
del /s /q import
rmdir /s /q import

:: Copy manifests
mkdir manifests
xcopy /s /e ..\manifests\* manifests

:: Create JAR files
mkdir ..\build
for /d %%f in (manifests\*) do (
	"%javaBin%\jar.exe" cmf "%%f\META-INF\MANIFEST.MF" "..\build\%%~nf.jar" .
)

:: Clean build directory
cd ..
del /s /q %buildDir%\*
rmdir /s /q %buildDir%

PAUSE