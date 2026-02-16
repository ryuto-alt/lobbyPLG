@echo off
setlocal

REM JBR + HotSwapAgent でサーバーを起動
REM FastServerの代わりにこのバッチファイルを使用

set JBR_HOME=C:\Users\Unoryuto\Documents\MyPlg\myplg\jbr\jbr-21.0.9-windows-x64-b1163.91
set AGENT_JAR=C:\Users\Unoryuto\Documents\MyPlg\myplg\agent\hotswap-agent.jar
set PROJECT_DIR=C:\Users\Unoryuto\Documents\MyPlg\myplg

echo ========================================
echo   HotSwap Server (JBR + HotSwapAgent)
echo ========================================
echo.

"%JBR_HOME%\bin\java.exe" ^
    -XX:+AllowEnhancedClassRedefinition ^
    -XX:HotswapAgent=fatjar ^
    -javaagent:"%AGENT_JAR%" ^
    -Dhotswap.extraClasspath="%PROJECT_DIR%\build\classes\java\main" ^
    -Dhotswap.autoHotswap=true ^
    -Dhotswap.watchResources="%PROJECT_DIR%\src\main\resources" ^
    -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 ^
    -Xms2G ^
    -Xmx4G ^
    -jar Server.jar ^
    --nogui

pause
endlocal
