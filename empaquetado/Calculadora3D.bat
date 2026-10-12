@echo off
rem Abre la Calculadora de impresion 3D con el Java incluido en la carpeta "runtime".
if not exist "%~dp0runtime\bin\javaw.exe" goto faltan
if not exist "%~dp0app\Calculadora3D.jar" goto faltan
start "" "%~dp0runtime\bin\javaw.exe" -Dfile.encoding=UTF-8 -jar "%~dp0app\Calculadora3D.jar"
exit /b 0

:faltan
echo.
echo  Faltan archivos del programa.
echo.
echo  Si lo has abierto directamente desde el .zip, descomprime primero la
echo  carpeta completa (clic derecho ^> Extraer todo) y abrelo desde ahi.
echo.
pause
exit /b 1
