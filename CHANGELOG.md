# Changelog — MeloGroove Android Music Player

All notable changes and audit bug fixes from `v1.0.0` to `v1.9.0` are documented here.
See `VERSION_HISTORY.md`, `GOOGLE_DRIVE_SETUP_GUIDE.md`, `BUG_FIX_REPORT_v1.6.0.md`, and `BUG_FIX_REPORT_v1.5.0.md` for full Persian & English technical reports.

## [1.9.0] - 2026-11-08 (Build 9)
### Added
- **Red 3-Day Free Trial Button (`start_trial_button` on `GoogleLoginScreen`):** Added an active Crimson Red 3-Day Free Trial button on the login screen allowing users who do not wish to sign in with their Gmail/email account to enter and use the full application for 3 days (72 hours) without entering an account or phone OTP.
- **Persistent 72-Hour Trial Timer & Expiration Guard (`UserPreferences`, `AppConfig`, `MainViewModel`):** Persists the initial trial start timestamp (`trialStartedAtMs`), displays live remaining days/hours on both the Login screen and Settings screen, and automatically prompts for Google Sign-In once the 3-day period expires.

## [1.8.0] - 2026-11-08 (Build 8)
### Added
- **Strict Onboarding Guard (5 Mandatory Stages):** Enforced strict sequential progression in `MainActivity` & `MainViewModel`: (1) Google Sign-In (`GoogleLoginScreen`) -> (2) Admin Google Drive Profile Sync (`syncUserFromDrive`) -> (3) Mandatory Mobile Phone OTP Verification (`PhoneVerificationScreen`) -> (4) Country Selection (`OnboardingScreen`) -> (5) Main Music Player & User Profile Management (`ProfileScreen`).
- **Official Google Authentication (`GoogleLoginScreen`):** Integrated Jetpack `CredentialManager` (`GetSignInWithGoogleOption` & `GetGoogleIdOption`) + Firebase Google Auth extracting `uid`, `email`, `displayName`, and `photoURL`, with a built-in Direct Gmail Fallback form for emulators/devices without Play Services.
- **Mandatory Mobile OTP Verification (`PhoneVerificationScreen`):** Added configurable `OTP_MODE` (`SMART_FALLBACK`, `EMAIL_OR_GAS_OTP`, `FIREBASE_FREE`), dial-code selector defaulting to Iran (`+98`), strict Iran (`^(\+98|0)?9\d{9}$`) & international mobile regex validation, 6 auto-focus OTP digit boxes, 60-second resend countdown timer, and Smart Fallback code helper banner.
- **Centralized Admin Google Drive Storage (`DriveStorageService` & `google-apps-script/Code.gs`):** Implemented `DriveStorageService` to store/sync each user's JSON record (`user_<uid>.json`) and master Google Sheet row in the Project Owner's Google Drive via `GOOGLE_DRIVE_WEBHOOK_URL` alongside instant local caching in `UserPreferences`.
- **User Profile Management (`ProfileScreen`):** Added full profile editor (`displayName`, `bio`, multi-select `favoriteGenres`, `selectedCountry`, verified phone badge, immutable Gmail badge, Google Drive Webhook URL configuration, and 1-tap Apps Script copy).

## [1.7.0] - 2026-11-07 (Build 7)
### Changed
- **Glassy Bottom Navigation Bar (V5 Row 3 Style):** Implemented `GlassyBottomNavBar` featuring a floating frosted-glass capsule with a dynamic cubic-bezier concave top cradle and an elevated glowing circular orb that smoothly animates to whichever tab is selected (supporting both RTL Persian and LTR English layouts).

## [1.6.0] - 2026-11-02 (Build 6)
### Added & Fixed (Architecture Roadmap & Audit N-01 to N-16, U-01 to U-12, Sections 5-8)
- **N-01 / U-01 / U-02 / U-07:** Enforced strict 1-to-1 artwork ownership (`ArtworkResolver.BUNDLED_COVER_OWNERS`) across all 26 bundled covers (`cover_1001.jpg`..`cover_1027.jpg`). No track ever displays another artist's cover photo. Added deterministic two-tone artist monogram avatars (`ArtworkRef.Initials`) for unowned tracks and changed `TrackArtwork` image alignment to `Alignment.TopCenter`.
- **N-02:** Removed artist cover photos from `MoodCategory` cards (`coverUrl = ""`) and rendered dedicated mood emblems in `ModernMoodCard`.
- **N-03 / U-03 (Section 5):** Added `GroupEngine` (`GroupKind.SMART` and `GroupKind.CURATED`) with `perArtistCap = 1` on `new-releases` to guarantee artist diversity and eliminate consecutive single-artist flooding.
- **N-04:** Externalized catalog definitions into `assets/catalog/artists.json`, `assets/catalog/groups.json`, and `assets/catalog/tracks.json` loaded via `CatalogDataSource`.
- **N-06 / N-07 / N-14:** Added `IdGenerator.stable64BitId` (SHA-256 64-bit IDs replacing `String.hashCode()`) and upgraded `MeloGrooveDatabase` to version 4 with non-destructive `MIGRATION_3_4` + `fallbackToDestructiveMigrationOnDowngrade()`.
- **Section 6 (No-VPN Email Auth):** Added `AuthRepository`, `LocalEmailAuthRepository`, `UserAccountEntity`, and `AuthScreen` (`Login`, `Register`, `ForgotPassword`, `VerifyEmail`) with salted SHA-256 hashing, rate-limit cooldown, sign-out, and account deletion in `SettingsScreen`.
- **Settings UI Cleanup:** Removed internal per-version changelog topics from `SettingsScreen` so end-users only see clean user-facing settings.
- **N-09 to N-13:** Added `network_security_config.xml`, configured `backup_rules.xml`, `data_extraction_rules.xml`, and `proguard-rules.pro`, and commented out unused Firebase dependencies in `app/build.gradle.kts`.

## [1.5.0] - 2026-11-02 (Build 5)
### Fixed (Technical Audit BUG-01 to BUG-14)
- **BUG-01:** Removed hardcoded fallback stream substitution (`Mohsen Chavoshi - Roode Sookhte` and `fallbackIranStreams`). Added `SourceResolver` so only the requested track's own valid stream URLs are played, or `ErrorCategory.SOURCE_UNAVAILABLE` is surfaced transparently.
- **BUG-02:** Replaced infinite retry loop with error classification (`SOURCE_NOT_FOUND` for 401/403/404/410, `DECODER_FORMAT`, `RATE_LIMITED` for 429, `NETWORK_TRANSIENT` for timeouts/5xx) and bounded exponential backoff with jitter (`MAX_RETRY_ATTEMPTS = 6`) + manual retry action.
- **BUG-03:** Verified `currentMediaItem?.mediaId == track.id.toString()` and URI match before calling `seekTo` in `attemptResumeCurrentTrack`, and reset retry state on track changes/stops.
- **BUG-04:** Isolated HTML scraping in `MusicsFaHtmlParser` with Cloudflare/CAPTCHA challenge detection, HTML entity decoding, and host health tracking.
- **BUG-05:** Removed fake `215000L` (`03:35`) duration for scraped tracks; used `0L` (`"--:--"`) until `ExoPlayer` reaches `STATE_READY` and updates the real duration.
- **BUG-06:** Protected `searchCache` with `Mutex`, 5-minute TTL, max 40 entries, normalized keys, and live `favoriteIds` synchronization.
- **BUG-07 & BUG-08:** Set `_isLoadingTrending.value = true` at the start of `loadTrendingForCountry` with `finally` cleanup, and cancelled previous coroutine `Job`s (`trendingJob`, `moodJob`, `artistJob`, `playlistTracksJob`, `searchJob`) on rapid user switches.
- **BUG-09 & BUG-10:** Implemented real `320kbps` vs `128kbps` stream variant switching in `SourceResolver` + `ExoPlayer.trackSelectionParameters`, honest single-stream indicator in `FullPlayerSheet`, and operational `Iran No-VPN Direct Mode` toggle in `SettingsScreen`.
- **BUG-11 to BUG-14:** Bounded `WakeLock` to 30m, unregistered `NetworkCallback` on `release()`, disabled `HttpLoggingInterceptor.Level.BODY` in release builds with header redaction, added structured `AppLogger`, and restricted `sanitizeStoredTrack` matching to exact `id` or composite `title + artist`.

### Improved (UI/UX Audit UI-01 to UI-14)
- Adaptive `ModernMoodCard` height (`heightIn(min = 96.dp)`), stable `LazyColumn`/`LazyRow` keys, trending error/retry state on Home, collapsible filter bar & `ImeAction.Search` on Search, `rememberSaveable` tab state & item counts + delete confirmation dialogs on Library/PlaylistDetail, disabled Slider when duration is unknown, Vinyl rotation strictly tied to `isPlaying`, Unicode Bidi text isolation (`BidiHelper`), 48dp touch targets, and neutral vinyl artwork placeholder.

## [1.4.0] - 2026-11-02 (Build 4)
- Country-isolated catalogs & moods, foreground lock-screen playback with `WakeLock`/`WifiLock`, and full Notification/Lock Screen Seek/Next/Prev controls.

## [1.3.0] - 2026-11-02 (Build 3)
- Direct unfiltered full-track MP3 streaming and bundled local album covers in `assets/covers/`.

## [1.2.0] - 2026-11-02 (Build 2)
- Single-tap country onboarding and zero-frame RTL layout initialization.

## [1.1.0] - 2026-11-01 (Build 1)
- Bilingual Persian (RTL) & English (LTR) support, modern 2-column mood cards, horizontal carousels, and Artist Discography screen.

## [1.0.0] - 2026-11-01 (Build 1)
- Initial release of MeloGroove Online Music Player.
