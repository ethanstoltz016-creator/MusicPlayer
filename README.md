# MusicPlayer
This is a music player I wrote with help from Copilot.
To work on the java file, you must have Maven and JavaFX installed.

If you edit something, and need to test the exe file, you will need to run:<br>
```mvn clean package```<br>
```rm -r MusicPlayer\```<br>
and:
```
& "C:\Program Files\Java\jdk-26.0.2\bin\jpackage.exe" --type app-image `
  --input target `
  --main-jar musicplayer.jar `
  --main-class MainLauncher `
  --name "MusicPlayer" `
  --icon "src/main/resources/myIcon.ico"'
```
You may need to change the path in the first line of the above command if you have a different version of the Java JDK or it is in a different location.
