@echo off
setlocal
pushd "%~dp0"
echo SehatHarian - aplikasi desktop
echo Memerlukan JDK 27 dan internet untuk unduhan dependency pertama.
echo.
where java >nul 2>nul
if errorlevel 1 (
    echo Java tidak ditemukan. Pasang JDK 27 dan atur JAVA_HOME serta PATH.
    popd
    pause
    exit /b 1
)
call gradlew.bat :Frontend2:run --console=plain
set "APP_EXIT=%ERRORLEVEL%"
echo.
if not "%APP_EXIT%"=="0" echo Aplikasi gagal dijalankan. Lihat pesan di atas dan BACA_DULU.md.
popd
pause
exit /b %APP_EXIT%
