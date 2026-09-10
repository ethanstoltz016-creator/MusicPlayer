const audio = document.querySelector('#audio');
const playlistElement = document.querySelector('#playlist');
const emptyMessage = document.querySelector('#emptyMessage');
const nowPlaying = document.querySelector('#nowPlaying');
const playButton = document.querySelector('#playButton');
const shuffleButton = document.querySelector('#shuffleButton');
const volume = document.querySelector('#volume');

// const bundledTracks = [
//   '02_A_Place_for_My_Head_SpotiDost.mp3',
//   '06_Breaking_the_Habit_SpotiDost.mp3',
//   '11_Heavy_Is_the_Crown_SpotiDost.mp3',
//   '12_The_Catalyst_SpotiDost.mp3'
// ];

// let tracks = bundledTracks.map((fileName) => ({
//   name: displayName(fileName),
//   source: `audio/${encodeURIComponent(fileName)}`,
//   durationStr: '--:--' // Initial placeholder before background metadata loader completes
// }));
let tracks = [];
let currentIndex = 0;
let shuffle = false;

function displayName(fileName) {
  return fileName.replace(/_SpotiDost\.mp3$/i, '').replaceAll('_', ' ');
}

// Helper utility to convert floating seconds into clean MM:SS format strings
function formatDuration(timeInSeconds) {
  if (isNaN(timeInSeconds) || !isFinite(timeInSeconds)) return '--:--';
  const minutes = Math.floor(timeInSeconds / 60);
  const seconds = Math.floor(timeInSeconds % 60);
  return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
}

// Background utility loop that scans through track indexes to parse durations concurrently
function loadAllTrackDurations() {
  tracks.forEach((track) => {
    // Only resolve files that don't have a computed time yet
    if (track.durationStr && track.durationStr !== '--:--') return;

    const tempAudio = new Audio();
    tempAudio.preload = 'metadata';
    
    tempAudio.addEventListener('loadedmetadata', () => {
      track.durationStr = formatDuration(tempAudio.duration);
      renderPlaylist(); // Cleanly re-renders updated properties to list cells
    });

    tempAudio.addEventListener('error', () => {
      track.durationStr = '--:--';
    });

    tempAudio.src = track.source;
  });
}

function renderPlaylist() {
  playlistElement.replaceChildren();
  emptyMessage.hidden = tracks.length > 0;
  
  tracks.forEach((track, index) => {
    const item = document.createElement('li');
    item.className = 'playlist-item-wrapper';

    const button = document.createElement('button');
    button.type = 'button';
    button.className = index === currentIndex ? 'selected' : '';
    button.ariaLabel = `Play ${track.name}`;
    button.addEventListener('click', () => playTrack(index));

    // Dynamic split content layouts
    const titleSpan = document.createElement('span');
    titleSpan.className = 'track-title';
    titleSpan.textContent = track.name;

    const durationSpan = document.createElement('span');
    durationSpan.className = 'track-duration';
    durationSpan.textContent = track.durationStr || '--:--';

    button.append(titleSpan, durationSpan);
    item.append(button);
    playlistElement.append(item);
  });
}

function playTrack(index) {
  if (!tracks.length) return;
  currentIndex = (index + tracks.length) % tracks.length;
  audio.src = tracks[currentIndex].source;
  nowPlaying.textContent = tracks[currentIndex].name;
  renderPlaylist();
  audio.play().catch(() => {
    playButton.textContent = 'Play';
  });
}

function nextIndex() {
  if (!tracks.length) return 0;
  if (!shuffle || tracks.length < 2) return (currentIndex + 1) % tracks.length;
  let index = currentIndex;
  while (index === currentIndex) {
    index = Math.floor(Math.random() * tracks.length);
  }
  return index;
}

// function loadFiles(fileList) {
//   const files = [...fileList].filter((file) => file.type.startsWith('audio/') || /\.mp3$/i.test(file.name));
//   if (!files.length) return;
  
//   tracks.forEach((track) => {
//     if (track.local) URL.revokeObjectURL(track.source);
//   });
  
//   tracks = files.sort((first, second) => first.name.localeCompare(second.name)).map((file) => ({
//     name: file.name.replace(/\.[^/.]+$/, '').replaceAll('_', ' '),
//     source: URL.createObjectURL(file),
//     local: true,
//     durationStr: '--:--'
//   }));
  
//   currentIndex = 0;
//   renderPlaylist();
//   loadAllTrackDurations(); // Pull durations immediately for new folders/files loaded
//   playTrack(0);
// }

function loadFiles(fileList) {
  const files = Array.from(fileList).filter(file =>
    file.type.startsWith('audio/') ||
    /\.(mp3|wav|ogg|m4a|aac|flac)$/i.test(file.name)
  );

  console.log('Files received:', files);
  console.log('Number of files:', files.length);

  if (files.length === 0) {
    console.log('No audio files found.');
    return;
  }

  // Revoke old local object URLs
  tracks.forEach(track => {
    if (track.local && track.source) {
      URL.revokeObjectURL(track.source);
    }
  });

  // Create tracks from selected files
  tracks = files
    .sort((a, b) => a.name.localeCompare(b.name))
    .map(file => ({
      name: file.name
        .replace(/\.[^/.]+$/, '')
        .replaceAll('_', ' '),
      source: URL.createObjectURL(file),
      local: true,
      durationStr: '--:--'
    }));

  console.log('Tracks created:', tracks);

  currentIndex = 0;

  renderPlaylist();
  loadAllTrackDurations();

  // Don't automatically play here.
  // Chrome may block autoplay.
  if (tracks.length > 0) {
    playTrack(0);
  }
}

document.querySelector('#folderInput').addEventListener('change', event => {
  console.log('Folder selected:', event.target.files);
  loadFiles(event.target.files);
});

document.querySelector('#fileInput').addEventListener('change', event => {
  console.log('Files selected:', event.target.files);
  loadFiles(event.target.files);
});


document.querySelector('#previousButton').addEventListener('click', () => {
  if (!tracks.length) return;
  playTrack(currentIndex - 1);
});
document.querySelector('#nextButton').addEventListener('click', () => {
  if (!tracks.length) return;
  playTrack(nextIndex());
});
playButton.addEventListener('click', () => {
  if (!tracks.length) return;
  if (audio.paused) {
    audio.play();
  } else {
    audio.pause();
  }
});
shuffleButton.addEventListener('click', () => {
  shuffle = !shuffle;
  shuffleButton.textContent = `Shuffle: ${shuffle ? 'On' : 'Off'}`;
  shuffleButton.setAttribute('aria-pressed', shuffle);
});
volume.addEventListener('input', () => {
  audio.volume = volume.value;
});
audio.addEventListener('ended', () => {
  if (tracks.length) {
    playTrack(nextIndex());
  }
});
audio.addEventListener('play', () => { playButton.textContent = 'Pause'; });
audio.addEventListener('pause', () => { playButton.textContent = 'Play'; });

audio.volume = volume.value;
renderPlaylist();
if (tracks.length) {
  loadAllTrackDurations();
  playTrack(0);
}
