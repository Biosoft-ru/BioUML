@echo off

REM 0: generate unique temp folder
setlocal EnableExtensions
:uniqLoop
set "uniqueFileName=%tmp%\JPACKAGE_%RANDOM%.tmp"
if exist "%uniqueFileName%" goto :uniqLoop
set temp_dir=%uniqueFileName%
set img_dir=%temp_dir%\image

set source_dir=..\
set result_dir=.

REM 1: jpackage run - create application image using only plugins folder
jpackage --type app-image --input %source_dir% --dest %temp_dir% --main-jar "plugins\org.eclipse.equinox.launcher_1.4.0.v20161219-1356.jar" --name "BioUML" --app-version "2.0" --win-console --arguments "-application biouml.workbench.launcher app/data app/data_resources app/analyses" --icon %source_dir%/BioUML.ico

REM 2: copy only nececssary files to separate image folder (easier than just delete not necessary files)
ROBOCOPY %temp_dir%\BioUML\runtime %img_dir%\BioUML\runtime /S /E *.* >>log.txt
ROBOCOPY %temp_dir%\BioUML %img_dir%\BioUML BioUML.exe /MOV  >>log.txt
ROBOCOPY %temp_dir%\BioUML\app %img_dir%\BioUML\app BioUML.cfg /MOV  >>log.txt
ROBOCOPY %temp_dir%\BioUML\app %img_dir%\BioUML\app .jpackage.xml /MOV  >>log.txt
ROBOCOPY %temp_dir%\BioUML\app %img_dir%\BioUML\app preferences.xml >>log.txt

ROBOCOPY %temp_dir%\BioUML\app\plugins %img_dir%\BioUML\app\plugins /S /E *.* >>log.txt
ROBOCOPY %temp_dir%\BioUML\app\analyses %img_dir%\BioUML\app\analyses /S >>log.txt
ROBOCOPY %temp_dir%\BioUML\app\configuration %img_dir%\BioUML\app\configuration /S >>log.txt
ROBOCOPY %temp_dir%\BioUML\app\data %img_dir%/BioUML\app\data default.config  >>log.txt
ROBOCOPY %temp_dir%\BioUML\app\data_resources %img_dir%\BioUML\app\data_resources default.config >>log.txt
ROBOCOPY %temp_dir%\BioUML\app\data_resources\Collaboration %img_dir%\BioUML\app\data_resources\Collaboration default.config >>log.txt

REM 3: Some files need to be copied to top level of created image (near EXE file). Preferences and lcf files are neccessary for BioUML run, ico is needed for application icon
ROBOCOPY %temp_dir%\BioUML\app %img_dir%\BioUML preferences.xml >>log.txt
ROBOCOPY %temp_dir%\BioUML\app %img_dir%\BioUML biouml.lcf >>log.txt
ROBOCOPY %temp_dir%\BioUML\app %img_dir%\BioUML BioUML.ico >>log.txt

REM 4: second jpackage run: generate installator on the base of modified image
jpackage --type msi --app-image %img_dir%\BioUML --dest %result_dir% --name "BioUML" --app-version "2.0" --vendor "Biosoft.RU" --win-shortcut --win-dir-chooser --win-menu --win-per-user-install --icon %img_dir%\BioUML\BioUML.ico --win-help-url "www.biouml.org" --win-shortcut-prompt 

REM 5: remove generated image
RMDIR /S /Q %temp_dir%
del log.txt