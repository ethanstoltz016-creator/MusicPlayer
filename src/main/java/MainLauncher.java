public class MainLauncher {
    public static void main(String[] args) {
        // Explicitly routes execution from the dummy file to the JavaFX setup wrapper
        javafx.application.Application.launch(MediaLibrary.class, args);
    }
}
