@echo off

del dpi.js 2>nul
for /f %%i in ('type dpi.buildpaths.txt') do type ..\..\..\..\..\..\..\..\..\..\%%i >>dpi.js
del dpi.min.js 2>nul
ren dpi.js dpi.min.js
