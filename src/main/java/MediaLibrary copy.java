// import java.io.File;
// import java.util.Arrays;
// import java.util.concurrent.ThreadLocalRandom;

// import javafx.application.Application;
// import javafx.application.Platform;
// import javafx.collections.FXCollections;
// import javafx.collections.ObservableList;
// import javafx.geometry.Insets;
// import javafx.geometry.Pos;
// import javafx.scene.Scene;
// import javafx.scene.control.Button;
// import javafx.scene.control.Label;
// import javafx.scene.control.ListCell;
// import javafx.scene.control.ListView;
// import javafx.scene.control.Slider;
// import javafx.scene.control.TextField;
// import javafx.scene.layout.HBox;
// import javafx.scene.layout.Priority;
// import javafx.scene.layout.VBox;
// import javafx.scene.media.Media;
// import javafx.scene.media.MediaPlayer;
// import javafx.scene.media.MediaView;
// import javafx.stage.DirectoryChooser;
// import javafx.stage.Stage;

// public class MediaLibrary extends Application {

//     // Simple data model class to hold track metadata
//     public static class Track {
//         private final String source;
//         private final String displayName;
//         private String durationStr = "Loading...";

//         public Track(String source) {
//             this.source = source;
//             String name = source.startsWith("file:") ? new File(source).getName() : source;
//             this.displayName = name.replace("_SpotiDost.mp3", "").replace('_', ' ');
//         }

//         public String getSource() { return source; }
//         public String getDisplayName() { return displayName; }
//         public String getDurationStr() { return durationStr; }
//         public void setDurationStr(String durationStr) { this.durationStr = durationStr; }
//     }

//     // Use an ObservableList so changes to track durations automatically refresh the UI
//     private final ObservableList<Track> playlist = FXCollections.observableArrayList();
//     private int currentTrackIndex;
//     private boolean shuffleMode;
//     private MediaPlayer mediaPlayer;
//     private MediaView mediaView;
//     private Label trackLabel;
//     private Button playButton;
//     private Slider progressSlider;
//     private ListView<Track> listView;

//     @Override
//     public void start(Stage primaryStage) {
//         // 1. Track Display Label
//         trackLabel = new Label();
//         trackLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333333;");

//         // 2. Play / Pause Control
//         playButton = new Button("Pause");
//         playButton.setMinWidth(60);
//         playButton.setOnAction(e -> {
//             if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
//                 mediaPlayer.pause();
//                 playButton.setText("Play");
//             } else if (mediaPlayer != null) {
//                 mediaPlayer.play();
//                 playButton.setText("Pause");
//             }
//         });

//         Button nextButton = new Button("Next");
//         nextButton.setOnAction(e -> playTrack(nextTrackIndex()));

//         Button shuffleButton = new Button("Shuffle: Off");
//         shuffleButton.setOnAction(e -> {
//             shuffleMode = !shuffleMode;
//             shuffleButton.setText("Shuffle: " + (shuffleMode ? "On" : "Off"));
//         });

//         // 3. Progress Bar (Slider)
//         progressSlider = new Slider();
//         progressSlider.setMinWidth(350);
//         progressSlider.setOnMouseReleased(e -> {
//             if (mediaPlayer != null) {
//                 double total = mediaPlayer.getTotalDuration().toMillis();
//                 if (total > 0) {
//                     mediaPlayer.seek(mediaPlayer.getTotalDuration().multiply(progressSlider.getValue() / 100.0));
//                 }
//             }
//         });

//         // 4. Volume Control
//         Label volumeLabel = new Label("🔊 Volume:");
//         Slider volumeSlider = new Slider(0, 100, 50);
//         volumeSlider.setMaxWidth(100);
//         volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
//             if (mediaPlayer != null) {
//                 mediaPlayer.setVolume(newValue.doubleValue() / 100.0);
//             }
//         });

//         // 5. Playlist View with a fluid HBox design layout
//         listView = new ListView<>(playlist);
//         listView.setCellFactory(param -> new ListCell<>() {
//             private final HBox cellLayout = new HBox();
//             private final Label nameLabel = new Label();
//             private final Label timeLabel = new Label();

//             {
//                 // Push name and duration layout edges dynamically to opposite corners
//                 HBox.setHgrow(nameLabel, Priority.ALWAYS);
//                 nameLabel.setMaxWidth(Double.MAX_VALUE);
                
//                 // Style configurations
//                 timeLabel.setStyle("-fx-text-fill: #888888; -fx-font-family: 'Courier New';");
//                 cellLayout.getChildren().addAll(nameLabel, timeLabel);
//             }

//             @Override
//             protected void updateItem(Track track, boolean empty) {
//                 super.updateItem(track, empty);
//                 if (empty || track == null) {
//                     setGraphic(null);
//                 } else {
//                     nameLabel.setText(track.getDisplayName());
//                     timeLabel.setText("[" + track.getDurationStr() + "]");
//                     setGraphic(cellLayout);
//                 }
//             }
//         });

//         listView.getSelectionModel().select(currentTrackIndex);
//         listView.setOnMouseClicked(e -> {
//             int selectedIndex = listView.getSelectionModel().getSelectedIndex();
//             if (selectedIndex >= 0) {
//                 playTrack(selectedIndex);
//             }
//         });

//         TextField musicPathField = new TextField();
//         musicPathField.setPromptText("Type a music folder path");
//         musicPathField.setPrefWidth(500);

//         Button browseButton = new Button("Browse...");
//         browseButton.setOnAction(e -> {
//             DirectoryChooser directoryChooser = new DirectoryChooser();
//             directoryChooser.setTitle("Choose Music Folder");
//             File selectedFolder = directoryChooser.showDialog(primaryStage);
//             if (selectedFolder != null) {
//                 musicPathField.setText(selectedFolder.getAbsolutePath());
//                 loadMusicFolder(selectedFolder);
//             }
//         });

//         Button loadButton = new Button("Load");
//         loadButton.setOnAction(e -> loadMusicFolder(new File(musicPathField.getText().trim())));
//         musicPathField.setOnAction(e -> loadMusicFolder(new File(musicPathField.getText().trim())));

//         // 6. Layout Alignment
//         mediaView = new MediaView();
//         HBox controlsLayout = new HBox(15);
//         controlsLayout.setAlignment(Pos.CENTER);
//         controlsLayout.getChildren().addAll(playButton, nextButton, shuffleButton, progressSlider, volumeLabel, volumeSlider);

//         HBox musicPathLayout = new HBox(10, musicPathField, browseButton, loadButton);
//         musicPathLayout.setAlignment(Pos.CENTER);

//         VBox mainLayout = new VBox(25);
//         mainLayout.setAlignment(Pos.CENTER);
//         mainLayout.setPadding(new Insets(30));
//         mainLayout.setStyle("-fx-background-color: #f5f5f5;");
//         mainLayout.getChildren().addAll(trackLabel, mediaView, controlsLayout, musicPathLayout, listView);

//         Scene scene = new Scene(mainLayout, 850, 450); 
//         primaryStage.setTitle("Java Media Player");
//         primaryStage.setScene(scene);
        
//         // Bakes your custom resource icon straight into the Windows runtime stage taskbar array
//         // primaryStage.getIcons().add(new javafx.scene.image.Image(Thread.currentThread().getContextClassLoader().getResourceAsStream("myIcon.ico")));
//         // Safe container block prevents resource path variations from crashing the boot layer
//         try {
//             // Loading via the class loader directly from the assembly root archive
//             java.io.InputStream iconStream = MediaLibrary.class.getClassLoader().getResourceAsStream("myIcon.ico");
//             if (iconStream != null) {
//                 primaryStage.getIcons().add(new javafx.scene.image.Image(iconStream));
//             }
//         } catch (Exception e) {
//             System.err.println("Taskbar icon resource could not be loaded safely.");
//         }
//         primaryStage.show();

//         // Trigger asynchronous background header scan for list info
//         loadAllTrackDurationsInBackground();

//         if (!playlist.isEmpty()) {
//             playTrack(currentTrackIndex);
//         }
//     }

//     private void loadAllTrackDurationsInBackground() {
//         for (Track track : playlist) {
//             String url = getFullMediaUrl(track.getSource());
//             try {
//                 Media rawMedia = new Media(url);
//                 MediaPlayer tempPlayer = new MediaPlayer(rawMedia);
                
//                 tempPlayer.setOnReady(() -> {
//                     double totalSeconds = tempPlayer.getTotalDuration().toSeconds();
//                     int minutes = (int) totalSeconds / 60;
//                     int seconds = (int) totalSeconds % 60;
                    
//                     // Safely modify data array on UI thread loop
//                     Platform.runLater(() -> {
//                         track.setDurationStr(String.format("%02d:%02d", minutes, seconds));
//                         listView.refresh(); // Tells list view to cleanly rebuild layout strings
//                     });
//                     tempPlayer.dispose();
//                 });
//             } catch (Exception ignored) {
//                 track.setDurationStr("--:--");
//             }
//         }
//     }

//     private String getFullMediaUrl(String source) {
//         return source.startsWith("file:") || source.startsWith("http:")
//             ? source
//             : getClass().getResource("/audio/" + source).toExternalForm();
//     }

//     private void loadMusicFolder(File folder) {
//         if (!folder.isDirectory()) return;

//         File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));
//         if (files == null || files.length == 0) return;

//         Arrays.sort(files, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        
//         playlist.clear();
//         for (File file : files) {
//             playlist.add(new Track(file.toURI().toString()));
//         }
        
//         currentTrackIndex = 0;
//         listView.getSelectionModel().select(currentTrackIndex);
        
//         // Scan new folder items asynchronously
//         loadAllTrackDurationsInBackground();
//         playTrack(currentTrackIndex);
//     }

//     private void playTrack(int trackIndex) {
//         if (playlist.isEmpty()) return;

//         currentTrackIndex = trackIndex;
//         Track track = playlist.get(trackIndex);
//         String mediaUrl = getFullMediaUrl(track.getSource());

//         if (mediaPlayer != null) {
//             mediaPlayer.dispose();
//         }

//         mediaPlayer = new MediaPlayer(new Media(mediaUrl));
//         mediaView = mediaView == null ? new MediaView(mediaPlayer) : mediaView;
//         mediaView.setMediaPlayer(mediaPlayer);

//         trackLabel.setText("Now Playing: " + track.getDisplayName());
        
//         if (listView != null) {
//             listView.getSelectionModel().select(trackIndex);
//         }
//         progressSlider.setValue(0);
//         mediaPlayer.setVolume(0.5);

//         mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
//             double total = mediaPlayer.getTotalDuration().toMillis();
//             if (total > 0 && !progressSlider.isValueChanging()) {
//                 progressSlider.setValue((newValue.toMillis() / total) * 100);
//             }
//         });

//         mediaPlayer.setOnEndOfMedia(() -> playTrack(nextTrackIndex()));
//         mediaPlayer.play();
//         playButton.setText("Pause");
//         }

//     private int nextTrackIndex() {
//         if (shuffleMode && playlist.size() > 1) {
//             int nextIndex;
//             do {
//                 nextIndex = ThreadLocalRandom.current().nextInt(playlist.size());
//             } while (nextIndex == currentTrackIndex);
//             return nextIndex;
//         }
//         return (currentTrackIndex + 1) % playlist.size();
//     }
//     public static void main(String[] args) {launch(args);}}