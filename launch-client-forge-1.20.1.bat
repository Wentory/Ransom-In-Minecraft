@echo off
setlocal
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "JAVA_TOOL_OPTIONS=-Dnet.minecraftforge.gradle.check.certs=false -Djavax.net.ssl.trustStoreType=Windows-ROOT"
call "%~dp0gradlew.bat" runClient --offline
popd
endlocal
