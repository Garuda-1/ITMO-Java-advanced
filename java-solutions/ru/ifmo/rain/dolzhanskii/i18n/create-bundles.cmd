@echo off
mkdir bundle
for %%a in (bundle-data\*.bundle) do call :bundle %%a
xcopy /y bundle\LocaleDemo_ru.properties bundle\LocaleDemo.*
exit /b

:bundle
echo %1
paste -d= bundle-data/names %1 > bundle\LocaleDemo_%~n1.properties
