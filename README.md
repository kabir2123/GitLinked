# GitLinked 🔗

**A location-aware developer networking Android application** that integrates GitHub portfolios with Bluetooth-based proximity detection for real-time developer discovery.

## Features

- ✅ **GitHub OAuth Login** — Sign in with GitHub and showcase your repositories
- ✅ **Real GitHub Profile** — Fetches your actual repos, languages, and bio from GitHub
- ✅ **BLE Proximity Discovery** — Discover nearby developers via Bluetooth Low Energy
- ✅ **Smart Match Algorithm** — Match score based on common languages and interests
- ✅ **Invite System** — Send connection invites before chatting (accept/reject flow)
- ✅ **Encrypted Messaging** — AES-128 end-to-end encrypted chat (after invite accepted)
- ✅ **Chats Hub** — View all conversations, pending invites, and sent requests
- ✅ **Job Board** — Browse full-time, freelance, and gig opportunities
- ✅ **Events** — Discover tech meetups/hackathons with check-in functionality
- ✅ **Offline Mode** — SQLite caching for offline access
- ✅ **Demo Mode** — Fully functional with mock data (no real credentials needed)

## Getting Started

### Prerequisites
- Android Studio (Arctic Fox or newer recommended)
- JDK 17+
- Android SDK 34

### Setup

1. **Open in Android Studio:**
   - File → Open → Select the `GitLinked/` folder
   - Wait for Gradle sync to complete

2. **GitHub OAuth (Optional for demo):**
   - Go to [GitHub Developer Settings](https://github.com/settings/developers)
   - Create a new OAuth App
   - Set callback URL: `gitlinked://callback`
   - Copy Client ID and Client Secret
   - Update in `utils/Constants.java`

3. **Run:**
   - Select an emulator or physical device
   - Click ▶️ Run
   - Use "Continue without login (Demo)" to see all features

## Architecture

```
com.example.gitlinked/
├── activities/     # UI screens (Login, Main, Nearby, Profile, Chat, Chats, Jobs, Events)
├── adapters/       # RecyclerView adapters (Developer, Chat, Job, Event)
├── api/            # GitHub API (Retrofit) + OAuth
├── bluetooth/      # BLE Manager, Scanner, Advertiser
├── database/       # SQLite (DBHelper, UserDao, MessageDao, JobDao, ConnectionDao)
├── fragments/      # Nearby, Chat, Profile fragments
├── models/         # Data models (User, Repository, Message, Job, Event, MatchResult, ConnectionRequest)
├── services/       # Background services (BLE, Sync)
└── utils/          # Constants, MatchUtils, EncryptionUtil, LocationUtils
```

## Connection Flow

```
Developer A discovers Developer B via BLE/Nearby
    ↓
A taps "Send Invite" on B's profile
    ↓
B sees invite in Chats tab → "Pending Invites"
    ↓
B taps "Accept" → Both are now connected
    ↓
A and B can now chat with encrypted messaging
```

## Match Algorithm

```
match = (common_languages / total_unique_languages) × 70
      + (common_interests / total_unique_interests) × 30
```

## What Makes This App Unique

| Feature | GitLinked | LinkedIn | GitHub |
|---------|-----------|----------|--------|
| Real-time proximity discovery | ✅ BLE | ❌ | ❌ |
| Developer-specific networking | ✅ | ❌ Generic | ✅ Code only |
| GitHub-based identity | ✅ | ❌ | ✅ |
| Local economy (Jobs/Gigs) | ✅ | ✅ | ❌ |
| Invite → Accept → Chat flow | ✅ | ✅ | ❌ |
| Encrypted messaging | ✅ AES | ❌ | ❌ |

## Tech Stack

- **Language:** Java (Groovy DSL build system)
- **UI:** XML Layouts + Material Design Components
- **Networking:** Retrofit 2 + Gson
- **Database:** SQLite (custom DAOs)
- **BLE:** Android BluetoothLeScanner + BluetoothLeAdvertiser
- **Images:** Glide + CircleImageView
- **Security:** AES-128 encryption for messages

## License

This project is for educational/lab purposes.
