# MusicPlayer
This is a music player I wrote with help from Copilot.
To work on the java file, you must have Maven and JavaFX installed.

If you edit something, and need to test the exe file, you will need to run:<br>
'mvn clean package'<br>
and then:<br>
'& "C:\Program Files\Java\jdk-26.0.2\bin\jpackage.exe" --type app-image `<br>
  --input target `<br>
  --main-jar musicplayer.jar `<br>
  --main-class MainLauncher `<br>
  --name "MusicPlayer" `<br>
  --icon "src/main/resources/myIcon.ico"'<br>
