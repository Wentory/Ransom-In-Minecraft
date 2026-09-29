@echo off
setlocal
title Ransom - Minecraft 1.20.1 Forge
set "PROJECT_ROOT=%~dp0.."
set "FORGE_WORKTREE=%PROJECT_ROOT%\.worktrees\1.20.1"
if not exist "%FORGE_WORKTREE%\gradlew.bat" (
    echo Forge 1.20.1 worktree not found: "%FORGE_WORKTREE%"
    pause
    exit /b 1
)
cd /d "%FORGE_WORKTREE%"
set "JAVA_HOME=C:\Program Files\Java\jdk-17"
set "JAVA_TOOL_OPTIONS=-Dnet.minecraftforge.gradle.check.certs=false -Djavax.net.ssl.trustStoreType=Windows-ROOT"
call gradlew.bat runClient
if errorlevel 1 pause
endlocal
