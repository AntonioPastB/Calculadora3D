@echo off
rem Crea en el escritorio un acceso directo "Calculadora 3D" con el icono del programa.
rem El acceso directo abre directamente el Java incluido (firmado), sin pasar por este .bat.
if not exist "%~dp0runtime\bin\javaw.exe" goto faltan

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$d = '%~dp0';" ^
  "$s = (New-Object -ComObject WScript.Shell).CreateShortcut([Environment]::GetFolderPath('Desktop') + '\Calculadora 3D.lnk');" ^
  "$s.TargetPath = $d + 'runtime\bin\javaw.exe';" ^
  "$s.Arguments = '-Dfile.encoding=UTF-8 -jar \"' + $d + 'app\Calculadora3D.jar\"';" ^
  "$s.WorkingDirectory = $d;" ^
  "$s.IconLocation = $d + 'app\icono.ico';" ^
  "$s.Description = 'Calculadora de impresion 3D';" ^
  "$s.Save()"
if errorlevel 1 goto error

echo.
echo  Listo: tienes el acceso directo "Calculadora 3D" en el escritorio.
echo  Si mueves esta carpeta a otro sitio, vuelve a ejecutar este archivo.
echo.
pause
exit /b 0

:faltan
echo.
echo  Faltan archivos del programa. Descomprime primero la carpeta completa
echo  (clic derecho sobre el .zip ^> Extraer todo) y ejecuta esto desde ahi.
echo.
pause
exit /b 1

:error
echo.
echo  No se ha podido crear el acceso directo.
echo.
pause
exit /b 1
