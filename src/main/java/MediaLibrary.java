import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class MediaLibrary extends Application {

    // Simple data model class to hold track metadata
    public static class Track {
        private final String source;
        private final String displayName;
        private String album = "Unknown Album";
        private String durationStr = "Loading...";

        public Track(String source) {
            this.source = source;
            String name = source.startsWith("file:") ? new File(source).getName() : source;
            this.displayName = name.replace("_SpotiDost.mp3", "").replace('_', ' ');
        }

        public String getSource() { return source; }
        public String getDisplayName() { return displayName; }
        public String getAlbum() { return album; }
        public void setAlbum(String album) { this.album = album == null || album.isBlank() ? "Unknown Album" : album; }
        public String getDurationStr() { return durationStr; }
        public void setDurationStr(String durationStr) { this.durationStr = durationStr; }
    }

    // Use an ObservableList so changes to track durations automatically refresh the UI
    private final ObservableList<Track> playlist = FXCollections.observableArrayList();
    private int currentTrackIndex;
    private boolean shuffleMode;
    private MediaPlayer mediaPlayer;
    private MediaView mediaView;
    private Label trackLabel;
    private Button playButton;
    private Slider progressSlider;
    private Label currentTimeLabel;
    private Label totalTimeLabel;
    private ListView<Track> listView;

    @Override
    public void start(Stage primaryStage) {
        
        // --- 1. Header Area (<header> Layout Grid) ---
        HBox topRow = new HBox();
        Label eyebrow = new Label("PERSONAL MUSIC LIBRARY");
        eyebrow.getStyleClass().add("eyebrow");
        
        Button darkModeBtn = new Button("Dark Mode");
        darkModeBtn.getStyleClass().add("color-btn");
        
        VBox titleArea = new VBox(5, eyebrow, new Label("Music Player"));
        titleArea.getChildren().get(1).getStyleClass().add("main-title");
        HBox.setHgrow(titleArea, Priority.ALWAYS);
        topRow.getChildren().addAll(titleArea, darkModeBtn);

        trackLabel = new Label("Choose a song to begin");
        trackLabel.getStyleClass().add("now-playing");

        VBox headerContainer = new VBox(10, topRow, trackLabel);
        headerContainer.getStyleClass().add("header-panel");

        // --- 2. Player Controls Panel (<section class="player-panel">) ---
        progressSlider = new Slider();
        currentTimeLabel = new Label("00:00");
        totalTimeLabel = new Label("00:00");
        currentTimeLabel.getStyleClass().add("time-label");
        totalTimeLabel.getStyleClass().add("time-label");
        HBox.setHgrow(progressSlider, Priority.ALWAYS);
        progressSlider.setOnMouseReleased(e -> {
            if (mediaPlayer != null) {
                double total = mediaPlayer.getTotalDuration().toMillis();
                if (total > 0) {
                    mediaPlayer.seek(mediaPlayer.getTotalDuration().multiply(progressSlider.getValue() / 100.0));
                }
            }
        });

        playButton = new Button("Play");
        playButton.setOnAction(e -> {
            if (mediaPlayer != null && mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                mediaPlayer.pause();
                playButton.setText("Play");
            } else if (mediaPlayer != null) {
                mediaPlayer.play();
                playButton.setText("Pause");
            }
        });

        Button nextButton = new Button("Next");
        nextButton.setOnAction(e -> playTrack(nextTrackIndex()));

        Button shuffleButton = new Button("Shuffle: Off");
        shuffleButton.setOnAction(e -> {
            shuffleMode = !shuffleMode;
            shuffleButton.setText("Shuffle: " + (shuffleMode ? "On" : "Off"));
        });

        Label timeSeparator = new Label("/");
        timeSeparator.getStyleClass().add("time-separator");

        HBox currentTimeBox = new HBox(0, currentTimeLabel, timeSeparator, totalTimeLabel);
        currentTimeBox.setAlignment(Pos.CENTER_LEFT);

        HBox controlsLayout = new HBox(4, playButton, nextButton, shuffleButton, progressSlider, currentTimeBox);
        controlsLayout.setAlignment(Pos.CENTER_LEFT);

        VBox playerPanelCard = new VBox(controlsLayout);
        playerPanelCard.getStyleClass().add("player-panel");

        // --- 3. Library/Queue Panel (<section class="library-panel">) ---
        Label queueEyebrow = new Label("YOUR QUEUE");
        queueEyebrow.getStyleClass().add("eyebrow");
        Label playlistTitle = new Label("Playlist");
        playlistTitle.getStyleClass().add("section-title");
        VBox playlistHeading = new VBox(2, queueEyebrow, playlistTitle);

        Label volumeLabel = new Label("Volume");
        volumeLabel.getStyleClass().add("help-text");
        Slider volumeSlider = new Slider(0, 100, 50);
        volumeSlider.setPrefWidth(120);
        volumeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(newValue.doubleValue() / 100.0);
            }
        });
        
        HBox volumeBox = new HBox(8, volumeLabel, volumeSlider);
        volumeBox.setAlignment(Pos.BOTTOM_RIGHT);
        HBox.setHgrow(volumeBox, Priority.ALWAYS);
        volumeBox.getStyleClass().add("volume-box");

        HBox sectionHeaderRow = new HBox(playlistHeading, volumeBox);
        sectionHeaderRow.setAlignment(Pos.BOTTOM_LEFT);

        // File/Folder Tools inputs
        TextField musicPathField = new TextField();
        musicPathField.setPromptText("Type or browse a music folder path...");
        HBox.setHgrow(musicPathField, Priority.ALWAYS);

        Button browseButton = new Button("Browse Folder");
        browseButton.setOnAction(e -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            directoryChooser.setTitle("Choose Music Folder");
            File selectedFolder = directoryChooser.showDialog(primaryStage);
            if (selectedFolder != null) {
                musicPathField.setText(selectedFolder.getAbsolutePath());
                loadMusicFolder(selectedFolder);
            }
        });

        HBox fileToolsRow = new HBox(10, musicPathField, browseButton);
        Label helpText = new Label("Select a directory from your device to load your tracks privately.");
        helpText.getStyleClass().add("help-text");

        // Web Translated Playlist Cell Architecture
        listView = new ListView<>(playlist);
        VBox.setVgrow(listView, Priority.ALWAYS);
        listView.setCellFactory(param -> new ListCell<>() {
            private final HBox cellLayout = new HBox(14);
            private final Label nameLabel = new Label();
            private final Label albumLabel = new Label();
            private final Label timeLabel = new Label();
            
            {
                HBox.setHgrow(nameLabel, Priority.ALWAYS);
                HBox.setHgrow(albumLabel, Priority.ALWAYS);
                nameLabel.setMaxWidth(Double.MAX_VALUE);
                albumLabel.setMaxWidth(Double.MAX_VALUE);
                
                nameLabel.getStyleClass().add("track-title-label");
                albumLabel.getStyleClass().add("track-album-label");
                timeLabel.getStyleClass().add("track-duration-label");
                
                cellLayout.getStyleClass().add("cell-layout");
                cellLayout.getChildren().addAll(nameLabel, albumLabel, timeLabel);
            }
            
            @Override
            protected void updateItem(Track track, boolean empty) {
                super.updateItem(track, empty);
                if (empty || track == null) {
                    setGraphic(null);
                } else {
                    nameLabel.setText(track.getDisplayName());
                    albumLabel.setText(track.getAlbum());
                    timeLabel.setText(track.getDurationStr());
                    setGraphic(cellLayout);
                }
            }
        });

        listView.setOnMouseClicked(e -> {
            int selectedIndex = listView.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                playTrack(selectedIndex);
            }
        });

        VBox libraryPanelCard = new VBox(20, sectionHeaderRow, fileToolsRow, helpText, listView);
        libraryPanelCard.getStyleClass().add("library-panel");

        // Assemble Main Web-Shell Shell Structure Layout
        mediaView = new MediaView(); 

        // Ensure it is added into your main root layout stack right here
        VBox rootShell = new VBox(25, headerContainer, mediaView, playerPanelCard, libraryPanelCard);
        rootShell.getStyleClass().add("player-shell");

        // Seamless Dark Mode Selector Mechanism
        darkModeBtn.setOnAction(e -> {
            ObservableList<String> styleClasses = rootShell.getStyleClass();
            if (styleClasses.contains("dark-mode")) {
                styleClasses.remove("dark-mode");
                darkModeBtn.setText("Dark Mode");
            } else {
                styleClasses.add("dark-mode");
                darkModeBtn.setText("Light Mode");
            }
        });

        // Initialize Scene and attach CSS properties engine files
        Scene scene = new Scene(rootShell, 900, 750);
        java.net.URL cssUrl = MediaLibrary.class.getClassLoader().getResource("desktop.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }


        bindTimestampFontToWindow(scene);

        primaryStage.setTitle("Personal Music Player");
        primaryStage.setScene(scene);
        
        // Safe container block prevents resource path variations from crashing the boot layer
        try {
            java.io.InputStream iconStream = MediaLibrary.class.getClassLoader().getResourceAsStream("myIcon.ico");
            if (iconStream != null) {
                primaryStage.getIcons().add(new javafx.scene.image.Image(iconStream));
            }
        } catch (Exception e) {
            System.err.println("Taskbar icon resource could not be loaded safely.");
        }

        primaryStage.show();

        if (!playlist.isEmpty()) {
            playTrack(currentTrackIndex);
        }
    }

    private void bindTimestampFontToWindow(Scene scene) {
        if (scene == null) return;

        currentTimeLabel.fontProperty().bind(Bindings.createObjectBinding(() -> {
            double width = scene.getWidth();
            double size = Math.max(9.0, Math.min(12.0, 9.0 + ((width - 700.0) / 220.0)));
            return Font.font("monospace", size);
        }, scene.widthProperty()));

        totalTimeLabel.fontProperty().bind(Bindings.createObjectBinding(() -> {
            double width = scene.getWidth();
            double size = Math.max(9.0, Math.min(12.0, 9.0 + ((width - 700.0) / 220.0)));
            return Font.font("monospace", size);
        }, scene.widthProperty()));

        Label timeSeparator = new Label("/");
        timeSeparator.getStyleClass().add("time-separator");
        timeSeparator.fontProperty().bind(Bindings.createObjectBinding(() -> {
            double width = scene.getWidth();
            double size = Math.max(9.0, Math.min(12.0, 9.0 + ((width - 700.0) / 220.0)));
            return Font.font("monospace", size);
        }, scene.widthProperty()));
    }

    private String formatTime(double totalSeconds) {
        long totalSecondsLong = Math.max(0, Math.round(totalSeconds));
        long minutes = totalSecondsLong / 60;
        long seconds = totalSecondsLong % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void loadAllTrackDurationsInBackground() {
        for (Track track : playlist) {
            String url = getFullMediaUrl(track.getSource());
            try {
                Media rawMedia = new Media(url);
                MediaPlayer tempPlayer = new MediaPlayer(rawMedia);
                
                tempPlayer.setOnReady(() -> {
                    double totalSeconds = tempPlayer.getTotalDuration().toSeconds();
                    int minutes = (int) totalSeconds / 60;
                    int seconds = (int) totalSeconds % 60;
                    
                    Platform.runLater(() -> {
                        track.setDurationStr(String.format("%02d:%02d", minutes, seconds));
                        listView.refresh();
                    });
                    tempPlayer.dispose();
                });

                tempPlayer.setOnError(() -> {
                    Platform.runLater(() -> track.setDurationStr("--:--"));
                    tempPlayer.dispose();
                });
            } catch (Exception ignored) {
                track.setDurationStr("--:--");
            }
        }
    }

    private void loadTrackAlbumMetadata(Track track) {
        try {
            Media rawMedia = new Media(getFullMediaUrl(track.getSource()));
            MediaPlayer tempPlayer = new MediaPlayer(rawMedia);

            tempPlayer.setOnReady(() -> {
                Object albumValue = rawMedia.getMetadata() != null ? rawMedia.getMetadata().get("album") : null;
                String album = albumValue == null ? "Unknown Album" : String.valueOf(albumValue).trim();

                Platform.runLater(() -> {
                    track.setAlbum(album);
                    if (listView != null) {
                        listView.refresh();
                    }
                });
                tempPlayer.dispose();
            });

            tempPlayer.setOnError(() -> tempPlayer.dispose());
        } catch (Exception ignored) {
            track.setAlbum("Unknown Album");
        }
    }

    private String getFullMediaUrl(String source) {
        return source.startsWith("file:") || source.startsWith("http:")
            ? source
            : getClass().getResource("/audio/" + source).toExternalForm();
    }

    private void loadMusicFolder(File folder) {
        if (!folder.isDirectory()) return;

        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));
        if (files == null || files.length == 0) return;

        Arrays.sort(files, (a, b) -> {
            String aName = a.getName();
            String bName = b.getName();
            Matcher aMatcher = Pattern.compile("^(\\d+)").matcher(aName);
            Matcher bMatcher = Pattern.compile("^(\\d+)").matcher(bName);

            if (aMatcher.find() && bMatcher.find()) {
                int aNum = Integer.parseInt(aMatcher.group(1));
                int bNum = Integer.parseInt(bMatcher.group(1));
                if (aNum != bNum) {
                    return Integer.compare(aNum, bNum);
                }
            }

            return aName.compareToIgnoreCase(bName);
        });
        
        playlist.clear();
        for (File file : files) {
            Track track = new Track(file.toURI().toString());
            loadTrackAlbumMetadata(track);
            playlist.add(track);
        }
        
        currentTrackIndex = 0;
        listView.getSelectionModel().select(currentTrackIndex);
        
        loadAllTrackDurationsInBackground();
        playTrack(currentTrackIndex);
    }

    private void playTrack(int trackIndex) {
        if (playlist.isEmpty()) return;

        currentTrackIndex = trackIndex;
        Track track = playlist.get(trackIndex);
        String mediaUrl = getFullMediaUrl(track.getSource());

         if (mediaPlayer != null) {
            mediaPlayer.dispose();
        }

        // 1. Create the new track player session instance
        mediaPlayer = new MediaPlayer(new Media(mediaUrl));

        // 2. Safely apply the fresh player directly into your existing layout view container
        mediaView.setMediaPlayer(mediaPlayer);

        trackLabel.setText("Now Playing: " + track.getDisplayName());
        currentTimeLabel.setText("00:00");
        totalTimeLabel.setText("00:00");
        
        if (listView != null) {
            listView.getSelectionModel().select(trackIndex);
        }
        progressSlider.setValue(0);
        mediaPlayer.setVolume(0.5);

        mediaPlayer.currentTimeProperty().addListener((observable, oldValue, newValue) -> {
            double total = mediaPlayer.getTotalDuration().toMillis();
            if (total > 0 && !progressSlider.isValueChanging()) {
                progressSlider.setValue((newValue.toMillis() / total) * 100);
            }
            currentTimeLabel.setText(formatTime(newValue.toMillis() / 1000.0));
            if (total > 0) {
                totalTimeLabel.setText(formatTime(total / 1000.0));
            }
        });

        mediaPlayer.setOnEndOfMedia(() -> playTrack(nextTrackIndex()));
        mediaPlayer.play();
        playButton.setText("Pause");
    }

    private int nextTrackIndex() {
        if (shuffleMode && playlist.size() > 1) {
            int nextIndex;
            do {
                nextIndex = ThreadLocalRandom.current().nextInt(playlist.size());
            } while (nextIndex == currentTrackIndex);
            return nextIndex;
        }
        return (currentTrackIndex + 1) % playlist.size();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
