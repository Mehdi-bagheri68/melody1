# گزارش ارزیابی و نقشه راه توسعه — MeloGroove (`v1.5.0`)

> مخزن: `Mehdi-bagheri68/melody1` (branch `main`)
> نوع بررسی: **مرور استاتیک کد** (clone عمومی). برنامه build و اجرا نشده و تست‌ها اجرا نشده‌اند.
> زبان: فارسی، با اصطلاحات فنی انگلیسی.

---

## ۱. خلاصه اجرایی

MeloGroove یک پلیر آنلاین Android با Kotlin، Jetpack Compose، Room، Media3/ExoPlayer و Coil است. ساختار پایه (دوزبانه RTL/LTR، سرویس پخش پس‌زمینه، کش، Retry با Backoff) قابل قبول است و ۱۴ باگ گزارش `v1.5.0` عمدتاً درست رفع شده‌اند. اما سه مشکل ساختاری باعث می‌شود پروژه با افزودن قابلیت جدید شکننده شود:

| # | یافته اصلی | اثر |
|---|-----------|-----|
| 1 | **دیتای آهنگ، خواننده و کاور داخل کد hard-code شده** (≈۱۰۰۰ خط در `MusicRepository.kt`) و کاورها به **آهنگ** وصل‌اند نه به **خواننده** | عکس خواننده‌های نامرتبط (مثال شما: بهنام بانی) |
| 2 | **منبع پخش، سایت‌های دانلود غیررسمی** (`musics-fa.com`، `rozmusic.com`) با scraping | ریسک حقوقی و رد شدن در Google Play؛ شکنندگی |
| 3 | **Room با `fallbackToDestructiveMigration`** | هر تغییر schema (لازمه هر دو نیازمندی جدید) علاقه‌مندی‌ها و پلی‌لیست‌های کاربر را پاک می‌کند |

**توصیه ترتیب اجرا:** Phase 0 (پایدارسازی) ← Phase 1 (مدل Artist/Group + رفع کاور؛ نیازمندی ۲) ← Phase 2 (لاگین ایمیل؛ نیازمندی ۱) ← Phase 3 (بهبود معماری و تست).

---

## ۲. دامنه بررسی

**بررسی شد:** `build.gradle.kts`، `libs.versions.toml`، `AndroidManifest.xml`، `Models.kt`، لایه Room و `UserPreferences`، `MeloGrooveApp`، `ItunesApi`، بخش‌های آهنگ و کاور در `MusicRepository`، `CountriesData`، `TrackArtwork`، `HomeScreen` (بخش New Releases)، ابتدای `MainActivity` و `MainViewModel`، CI، تست‌ها و مستندات.

**به‌طور کامل بررسی نشد (پیشنهاد Pass دوم):** `MusicPlayerController` (۱۰۵۱ خط)، `FullPlayerSheet`، `LibraryScreen`، `SettingsScreen`، `SearchScreen`، `DrivingModeScreen`. یافته‌های مربوط به این فایل‌ها با برچسب «نیاز به تایید» آمده‌اند.

**اعداد استخراج‌شده از کد (قابل بازتولید با اسکریپت ضمیمه):**
- ۷۰ آهنگ curated، ۲۶ فایل کاور؛ **۲۴ کاور از ۲۶ تا بین بیش از یک خواننده مشترک است**.
- `cover_1016.jpg` بین ۶ خواننده (از جمله The Weeknd، Tarkan، سیروان خسروی) و `cover_1017.jpg` بین ۴ خواننده مشترک است.
- ۱۷۸ رشته دوزبانه inline، ۳۱ مورد `contentDescription = null`، ۶ تست unit (فقط Resolver/Parser/Bidi).

---

## ۳. معماری فعلی

```
UI (Compose, Screen sealed class بدون navigation-compose)
  └─ MainViewModel (بدون DI؛ از MeloGrooveApp.instance می‌خواند)
       ├─ MusicRepository (۱۵۴۸ خط: دیتای hard-coded + iTunes + scraping + Room)
       │    ├─ SourceResolver / MusicsFaHtmlParser
       │    └─ Room: favorites, history, playlists, playlist_tracks (v3)
       ├─ MusicPlayerController (singleton، ۱۰۵۱ خط) ↔ MediaPlaybackService
       └─ UserPreferences (SharedPreferences)
```

نقاط قوت: Flow/StateFlow یکدست، لغو Jobهای قبلی، کش با TTL، Backoff سقف‌دار، لاگ release امن، پشتیبانی Bidi.

---

## ۴. یافته‌ها: باگ و ریسک فنی

شدت: **P0** بحرانی، **P1** بالا، **P2** متوسط، **P3** کم.

### ۴.۱ کاور و داده (مرتبط با نیازمندی ۲)

| ID | شدت | یافته | ریشه |
|----|-----|-------|------|
| N-01 | P0 | عکس یک خواننده روی آهنگ خواننده‌های دیگر نمایش داده می‌شود. مثلاً `cover_1017.jpg` (عکس واقعی بهنام بانی با متن «Cheshat Mige») برای Koli Band، Rema & Selena Gomez و Turkish Club DJ هم استفاده شده | `artworkUrl` روی هر `Track` به‌صورت دستی ست شده و مالک (owner) ندارد |
| N-02 | P1 | کاور Mood Cardها همان کاور آهنگ/خواننده‌های دیگر است (مثلاً `cover_1016` در چند mood) و تصویری گمراه‌کننده می‌سازد | `MoodCategory.coverUrl` به asset آهنگ اشاره می‌کند |
| N-03 | P1 | بخش «بروزترین انتشارها» در واقع **کل کاتالوگ کشور** است که با `releaseYear` sort شده. چون تقریباً همه ۲۰۲۶ هستند، ترتیب عملاً دلخواه است و چند آهنگ پشت‌سرهم از یک خواننده می‌آید. برچسب `NEW ${releaseYear}` هم از دیتای دستی می‌آید | `getInitialNewReleasesSync` فقط sort می‌کند؛ نه تاریخ دقیق دارد نه سقف per-artist |
| N-04 | P1 | بعضی کاورها واترمارک سایت ثالث (`Musics-Fa.com`) دارند | کاورها از سایت منبع کپی شده‌اند |
| N-05 | P2 | `Track.artist` یک `String` آزاد است؛ تطبیق خواننده با نام (مثلاً `"Gipsy Kings (France)"` در برابر `"Gipsy Kings"`) خطاپذیر است و feat./گروه‌ها قابل مدل‌سازی نیستند | نبود موجودیت `Artist` |
| N-06 | P2 | کاور و متادیتا در جدول‌های favorites/history/playlist_tracks کپی می‌شوند (denormalized)؛ با اصلاح کاور، رکوردهای ذخیره‌شده قدیمی می‌مانند | طراحی Room |
| N-07 | P2 | شناسه‌های آهنگ‌های scrape‌شده از `hashCode()` ساخته می‌شود؛ احتمال برخورد (collision) و ناپایداری بین نسخه‌ها | `MusicsFaHtmlParser`، `MusicRepository:170` |

### ۴.۲ امنیت، حقوقی و انطباق

| ID | شدت | یافته | توصیه |
|----|-----|-------|-------|
| N-08 | P0 | پخش مستقیم از سایت‌های دانلود غیررسمی و scraping HTML آن‌ها، شامل آثار بین‌المللی (Eminem، Rema، Modern Talking و ...). این برای انتشار در Google Play و برای مسئولیت حقوقی پرریسک است | حرکت به منبع دارای مجوز (بخش ۸.۲) |
| N-09 | P1 | `firebase.appcheck.debug` با `implementation` اضافه شده، یعنی provider دیباگ در build release هم می‌ماند | `debugImplementation` یا حذف |
| N-10 | P1 | `isMinifyEnabled = false` و `proguard-rules.pro` تقریباً خالی در release | فعال‌سازی R8 + قوانین Moshi/Room |
| N-11 | P2 | `usesCleartextTraffic="true"` | محدودسازی با `network_security_config` (فقط دامنه‌های ضروری) |
| N-12 | P2 | `allowBackup="true"` با قوانین backup پیش‌فرض نمونه | قوانین صریح؛ بعد از لاگین، توکن‌ها در backup نباشند |
| N-13 | P3 | وابستگی‌های `firebase-ai` و `appcheck-recaptcha` بدون هیچ استفاده در کد (و بدون `google-services.json`) | حذف تا لاگین Firebase واقعاً اضافه شود |

### ۴.۳ دیتابیس

| ID | شدت | یافته |
|----|-----|-------|
| N-14 | P0 | `fallbackToDestructiveMigration()` و `exportSchema = false` در Room v3. با اولین تغییر schema، دیتای کاربر پاک می‌شود. **هر دو نیازمندی جدید به تغییر schema نیاز دارند** (ستون `artistId`، `userId`، جدول گروه‌ها) |

### ۴.۴ Build و CI

| ID | شدت | یافته |
|----|-----|-------|
| N-15 | P1 | `gradlew` و `gradle-wrapper.jar` در مخزن نیست؛ workflow به `gradle` نصب‌شده روی runner تکیه می‌کند (نسخه ممکن است با AGP `9.1.1` ناسازگار باشد) |
| N-16 | P1 | `signingConfigs.debugConfig` به `debug.keystore` اشاره می‌کند که در `.gitignore` است؛ build روی CI احتمالاً با خطای keystore شکست می‌خورد (نیاز به تایید با اجرای workflow) |
| N-17 | P2 | حدود **۳۱۳MB** فایل APK (۶ نسخه × ۲ مسیر) داخل git؛ repo حجیم می‌شود | استفاده از GitHub Releases و حذف APK از git (با `git filter-repo`) |
| N-18 | P2 | فقط debug APK ساخته می‌شود؛ release امضاشده/AAB در CI نیست |

### ۴.۵ معماری و کیفیت کد

| ID | شدت | یافته | توصیه |
|----|-----|-------|-------|
| N-19 | P2 | God classها: `MusicRepository` (۱۵۴۸)، `CommonComponents` (۱۲۶۴)، `MusicPlayerController` (۱۰۵۱) | تفکیک: `CatalogDataSource`، `RemoteSearchSource`، `ArtworkResolver` و ... |
| N-20 | P2 | `MainViewModel` بدون DI و با `MeloGrooveApp.instance` (singleton سراسری)؛ تست‌پذیری پایین | Hilt یا حداقل `ViewModelProvider.Factory` |
| N-21 | P2 | ناوبری با `sealed class Screen` و state دستی، `navigation-compose` کامنت شده. `Screen.PlaylistDetail(playlist)` شیء کامل را نگه می‌دارد؛ بعد از process death بازیابی نمی‌شود (نیاز به تایید) | `navigation-compose` با route و آرگومان id |
| N-22 | P3 | ۱۷۸ رشته دوزبانه inline به‌جای `strings.xml` | انتقال به resource و `values-fa` |
| N-23 | P3 | پوشش تست: فقط ۶ تست unit؛ بدون تست Repository، ViewModel و UI | بخش ۹ |

---

## ۵. یافته‌ها: UI/UX

| ID | شدت | یافته | پیشنهاد |
|----|-----|-------|---------|
| U-01 | P1 | هم‌پوشانی عکس بین خواننده‌ها (N-01 و N-02) | بخش ۸ |
| U-02 | P2 | Placeholder فعلی فقط یک آیکون `Album` است؛ در لیست‌های طولانی یکنواخت و بی‌هویت است | آواتار با **حرف اول نام خواننده** و رنگ ثابت مشتق از `artistId` |
| U-03 | P2 | شلف «New» و «Trending» از یک لیست مشابه ساخته می‌شوند و محتوای تکراری نمایش می‌دهند | Group Engine با تنوع (`perArtistCap`) و حذف تکرار بین شلف‌ها |
| U-04 | P2 | `contentPadding bottom = 130.dp` عدد جادویی؛ با MiniPlayer متغیر یا حالت landscape ممکن است محتوا زیر پلیر برود | مشتق‌کردن از ارتفاع واقعی MiniPlayer/`WindowInsets` |
| U-05 | P2 | ۳۱ مورد `contentDescription = null` (دسترسی‌پذیری TalkBack) | توصیف برای دکمه‌های کنترلی؛ `null` فقط برای عناصر صرفاً تزئینی |
| U-06 | P3 | بدون صفحه `Login/Register`، بدون پروفایل کاربر و خروج | نیازمندی ۱ |
| U-07 | P3 | کاورهای بزرگ با `ContentScale.Crop` روی عکس‌های پرتره ممکن است صورت را ببرند | `Alignment.TopCenter` برای تصویر خواننده |
| U-08 | نیاز به تایید | سایز Touch target، رفتار صفحه Driving Mode و Landscape/Tablet بررسی نشد | تست دستی روی ۳ سایز صفحه |

---

## ۶. نیازمندی ۱ — لاگین با ایمیل

### ۶.۱ فرض‌ها
- ورود **اجباری** است (کاربر بدون لاگین به Home نمی‌رسد).
- روش: **ایمیل + رمز عبور** با تایید ایمیل و بازیابی رمز.
- ورود مهمان در نسخه اول وجود ندارد.

### ۶.۲ مقایسه گزینه‌ها

| گزینه | مزایا | معایب | رتبه |
|-------|-------|-------|------|
| A. Firebase Auth (Email/Password) | سریع، امن، ایمیل تایید/ریست آماده، بدون سرور | **احتمال فیلتر/تحریم دسترسی از ایران** (ریسک اصلی)، وابستگی به Google | ۱ (با `AuthRepository` انتزاعی) |
| B. Firebase Email-Link (بدون رمز) | UX ساده | نیاز به پیکربندی دامنه/deep link؛ پس از توقف Dynamic Links باید روش فعلی را در مستندات تایید کرد | ۳ |
| C. بک‌اند اختصاصی (JWT + Refresh Token) | کنترل کامل، قابل میزبانی در زیرساخت قابل‌دسترس | هزینه توسعه/نگهداری، مسئولیت امنیت و ارسال ایمیل | ۲ (اگر A روی شبکه ایران جواب نداد) |
| D. OTP ایمیلی با سرور | بدون رمز | نیاز به سرور و سرویس ایمیل | ۴ |

**ریسک کلیدی:** نقطه فروش برنامه «پخش بدون فیلترشکن برای ایران» است. اگر سرویس لاگین بدون VPN در دسترس نباشد، کاربر هدف وارد برنامه نمی‌شود. **پیش از پیاده‌سازی کامل، یک Prototype بسیار کوچک (signUp/signIn) روی اینترنت‌های واقعی ایران تست شود.** به همین دلیل لایه Auth باید پشت یک interface باشد تا جایگزینی Firebase با بک‌اند اختصاصی بدون تغییر UI ممکن باشد.

### ۶.۳ جریان کاربر (State Machine)

```
Splash(Loading) ─┬─ onboarding انجام نشده ─────────────► Onboarding ─► Login
                 ├─ SignedOut ─────────────────────────► Login ⇄ Register
                 │                                         └► ForgotPassword
                 ├─ SignedIn && !emailVerified ────────► VerifyEmail (ارسال مجدد، بررسی)
                 └─ SignedIn && emailVerified ─────────► Home
Settings ► Sign out (توقف پخش، پاک‌کردن state) / Delete account
```

### ۶.۴ تغییرات لازم در پروژه

1. فعال‌سازی `libs.firebase.auth` در `app/build.gradle.kts` (فقط Auth؛ ۴ خط Google Sign-In لازم نیست) + `kotlinx-coroutines-play-services` برای `await()`.
2. افزودن `google-services.json` واقعی (فعلاً وجود ندارد و plugin روی `WARN` است).
3. حذف `firebase-ai` و `appcheck.debug` از release (N-09، N-13).
4. افزودن `Screen.Splash/Login/Register/ForgotPassword/VerifyEmail` به `sealed class Screen`.
5. تعیین مسیر شروع بر اساس `combine(authState, onboardingCompleted)` (نه فقط در `init`).
6. داده‌های کاربر (علاقه‌مندی/تاریخچه/پلی‌لیست) به `userId` وابسته شوند (Migration، بخش ۸.۵).
7. حذف حساب داخل برنامه (الزام Google Play برای اپ‌های دارای ثبت‌نام).
8. رشته‌های جدید در `strings.xml` (fa/en) و پشتیبانی RTL فرم‌ها.

### ۶.۵ پیاده‌سازی پیشنهادی (اسکلت)

```kotlin
// ---- domain ----
sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val uid: String, val email: String, val emailVerified: Boolean) : AuthState
}

sealed interface AuthResult {
    data object Success : AuthResult
    data class Failure(val reason: AuthError) : AuthResult
}
enum class AuthError { INVALID_EMAIL, WEAK_PASSWORD, WRONG_CREDENTIALS, EMAIL_IN_USE,
                       NETWORK, TOO_MANY_REQUESTS, REQUIRES_RECENT_LOGIN, UNKNOWN }

interface AuthRepository {
    val authState: StateFlow<AuthState>
    suspend fun signUp(email: String, password: String): AuthResult
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun sendVerificationEmail(): AuthResult
    suspend fun refreshUser(): AuthResult            // بعد از تایید ایمیل
    suspend fun sendPasswordReset(email: String): AuthResult
    suspend fun signOut()
    suspend fun deleteAccount(): AuthResult
}
```

```kotlin
// ---- data (Firebase) ----
class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    scope: CoroutineScope
) : AuthRepository {

    private fun FirebaseUser?.toState(): AuthState =
        if (this == null) AuthState.SignedOut
        else AuthState.SignedIn(uid, email.orEmpty(), isEmailVerified)

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    override val authState: StateFlow<AuthState> = _state.asStateFlow()

    init {
        // این listener با reload() کاربر خودکار trigger نمی‌شود؛ در refreshUser() دستی emit می‌کنیم.
        auth.addAuthStateListener { _state.value = it.currentUser.toState() }
    }

    override suspend fun signIn(email: String, password: String): AuthResult = runAuth {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
    }

    override suspend fun refreshUser(): AuthResult = runAuth {
        auth.currentUser?.reload()?.await()
        _state.value = auth.currentUser.toState()
    }
    // ... signUp / sendVerificationEmail / reset / delete مشابه

    private suspend fun runAuth(block: suspend () -> Unit): AuthResult = try {
        block(); AuthResult.Success
    } catch (e: CancellationException) { throw e }
    catch (e: Exception) { AuthResult.Failure(e.toAuthError()) }
}
```

```kotlin
// ---- مسیر شروع در MainViewModel ----
val startScreen: StateFlow<Screen> = combine(
    auth.authState, userPreferences.onboardingCompleted
) { a, onboarded -> when {
    a is AuthState.Loading                              -> Screen.Splash
    !onboarded                                          -> Screen.Onboarding
    a is AuthState.SignedOut                            -> Screen.Login
    a is AuthState.SignedIn && !a.emailVerified         -> Screen.VerifyEmail
    else                                                -> Screen.Home
} }.stateIn(viewModelScope, SharingStarted.Eagerly, Screen.Splash)
```

### ۶.۶ Best practice و امنیت
- اعتبارسنجی: `Patterns.EMAIL_ADDRESS`، حداقل ۸ کاراکتر رمز، نمایش خطای مجزا برای هر فیلد.
- پیام «ایمیل یا رمز اشتباه است» بدون افشای وجود/نبود حساب (Email Enumeration Protection در Firebase روشن شود).
- Rate limit سمت کلاینت (غیرفعال‌کردن دکمه بعد از ارسال، Cooldown برای ارسال مجدد ایمیل) + Firebase App Check (provider واقعی Play Integrity در release).
- رمز عبور هرگز لاگ یا ذخیره نشود؛ `autofillHints` و `KeyboardType.Password` فعال شود.
- هنگام Sign out: `playerController.stopAndClearPlayback()`، پاک‌کردن کش جستجو و state کاربر.
- صفحه Privacy Policy و لینک حذف حساب (الزام Play).

### ۶.۷ معیار پذیرش
- کاربر بدون لاگین هیچ صفحه‌ای جز Login/Register/Forgot را نمی‌بیند (Back هم نمی‌تواند دور بزند).
- Session بعد از بستن و باز کردن برنامه حفظ می‌شود.
- ایمیل تایید‌نشده فقط VerifyEmail را می‌بیند.
- Sign out پخش را متوقف می‌کند و دیتای کاربر دیگر قابل مشاهده نیست.
- تمام پیام‌ها فارسی/انگلیسی و RTL صحیح هستند.

---

## ۷. وابستگی بین دو نیازمندی جدید

| موضوع مشترک | توضیح |
|-------------|-------|
| **Room Migration** | هر دو نیازمندی schema را عوض می‌کنند (`artistId`، جدول گروه‌ها، `userId`). پیشنهاد: یک Migration واحد `3 → 4` و جلوگیری از دو مایگریشن پشت‌سرهم |
| **منبع حقیقت (Source of Truth)** | بعد از لاگین، دیتای کاربر (علاقه‌مندی/پلی‌لیست) به `userId` وصل می‌شود؛ کاتالوگ (خواننده/آهنگ/گروه) سراسری و مستقل از کاربر می‌ماند |
| **ناوبری** | افزودن ۵ صفحه Auth و Route Guard، توجیه‌کننده مهاجرت به `navigation-compose` در Phase 3 است |
| **ترتیب پیشنهادی** | ابتدا نیازمندی ۲ (رفع باگ قابل‌مشاهده و بدون وابستگی به سرویس خارجی)، سپس نیازمندی ۱ |

---

## ۸. نیازمندی ۲ — گروه‌های داینامیک آهنگ + عکس متعلق به خواننده/گروه

### ۸.۱ فرض‌ها
- «داینامیک» یعنی **گروه‌ها داده‌محور و قابل تغییر بدون انتشار نسخه جدید** هستند (نه اینکه لیست‌ها در کد ثابت باشند). گسترش اختیاری: کاربر هم بتواند «گروه هوشمند» بسازد.
- **قانون بنیادی:** هر تصویر **یک مالک (`ownerArtistId`)** دارد و فقط برای همان مالک نمایش داده می‌شود. اگر تصویر معتبر نبود، placeholder نمایش داده شود، نه تصویر دیگران.

### ۸.۲ منبع آهنگ و تصویر (تصمیم لازم)

| گزینه | مزایا | معایب |
|-------|-------|-------|
| تصاویر داخل `assets/artists/{artistId}.jpg` (offline) | بدون شبکه، سریع | افزایش حجم APK، به‌روزرسانی با انتشار نسخه |
| `artists.json` + تصاویر روی CDN/Storage خودتان | داینامیک، قابل مدیریت، **پیشنهادی** | نیاز به میزبانی و مجوز تصویر |
| API عمومی (مثلاً Deezer برای تصویر خواننده) | پوشش وسیع | **شرایط استفاده/مجوز باید بررسی شود**؛ وابستگی به شبکه |
| iTunes Search | کاور آلبوم رسمی هر آهنگ | تصویر خواننده ندارد، فقط کاور آلبوم |

**توصیه:** `Artist.imageUrl` از `artists.json` خودتان (با مجوز تصویر) + کاور آلبوم فقط وقتی `ownerArtistId` آن با خواننده آهنگ برابر است. برای پخش هم منبع دارای مجوز انتخاب شود (مثلاً **Audius**، **Jamendo** (Creative Commons)، پیش‌نمایش iTunes، یا کاتالوگ مجاز خودتان) تا N-08 حل شود.

### ۸.۳ مدل داده پیشنهادی

```kotlin
enum class ArtistType { SOLO, BAND, DUO, VARIOUS }

data class Artist(
    val id: String,                 // slug پایدار: "behnam-bani"
    val nameFa: String,
    val nameEn: String,
    val type: ArtistType,
    val countryCode: String,
    val imageUrl: String?,          // تصویر خود خواننده/گروه
    val aliases: List<String> = emptyList()   // برای تطبیق نام‌های مختلف
)

data class Artwork(
    val url: String,
    val ownerArtistId: String,      // مالک؛ الزامی
    val kind: ArtworkKind           // ALBUM_COVER | ARTIST_PHOTO
)

data class Track(
    /* ... فیلدهای فعلی ... */
    val artistId: String,
    val featuredArtistIds: List<String> = emptyList(),
    val artwork: Artwork?           // اختیاری؛ جایگزین artworkUrl
)

enum class GroupKind { SMART, CURATED, REMOTE }

data class GroupRule(
    val countries: Set<String> = emptySet(),
    val genres: Set<String> = emptySet(),
    val artistIds: Set<String> = emptySet(),
    val releasedAfter: String? = null,         // ISO date
    val minPlayCount: Int? = null,
    val sort: GroupSort = GroupSort.RELEASE_DESC,
    val perArtistCap: Int = 2,                 // تنوع: حداکثر آهنگ از هر خواننده
    val limit: Int = 20
)

data class TrackGroup(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val kind: GroupKind,
    val rule: GroupRule? = null,               // برای SMART
    val trackIds: List<Long> = emptyList(),    // برای CURATED
    val order: Int,
    val enabled: Boolean = true
)
```

### ۸.۴ منطق انتخاب تصویر (قلب رفع مشکل)

```kotlin
sealed interface ArtworkRef {
    data class Image(val url: String) : ArtworkRef
    data class Initials(val text: String, val seed: String) : ArtworkRef   // placeholder خودِ خواننده
}

object ArtworkResolver {
    fun resolve(track: Track, artists: Map<String, Artist>): ArtworkRef {
        val artist = artists[track.artistId]
        val art = track.artwork
        return when {
            // کاور فقط وقتی مجاز است که مالکش همین خواننده (یا feat. آهنگ) باشد
            art != null && art.url.isNotBlank() &&
                (art.ownerArtistId == track.artistId ||
                 art.ownerArtistId in track.featuredArtistIds) -> ArtworkRef.Image(art.url)
            !artist?.imageUrl.isNullOrBlank() -> ArtworkRef.Image(artist!!.imageUrl!!)
            else -> ArtworkRef.Initials(
                text = (artist?.nameEn ?: track.artist).take(1).uppercase(),
                seed = track.artistId
            )
        }
    }
}
```

### ۸.۵ Room و Migration (الزامی)

1. `exportSchema = true` و `room.schemaLocation` در Gradle.
2. حذف `fallbackToDestructiveMigration()` و نوشتن `Migration(3, 4)`:
   - جدول‌های جدید: `artists`، `groups`، `group_tracks`.
   - ستون `artistId` در `favorite_tracks`، `listening_history`، `playlist_tracks` با مقدار پیش‌فرض و Backfill (تطبیق نام خواننده با `aliases`).
   - ستون `userId` برای داده‌های وابسته به کاربر (پس از لاگین) یا یک DB جدا برای هر کاربر.
3. تست Migration با `MigrationTestHelper`.

### ۸.۶ موتور گروه (Group Engine)

- یک تابع **pure** و قابل تست: `GroupEngine.evaluate(group, tracks, artists): List<Track>`.
- منبع تعریف گروه‌ها: v1 فایل `assets/groups.json`؛ v2 فایل از راه دور (ETag + کش دیسک + fallback به نسخه bundle). Remote Config فقط اگر Firebase قابل‌دسترس باشد.
- `HomeScreen` فهرست `List<GroupSection>` را روی `LazyColumn` رندر می‌کند؛ شلف «New» و «Trending» دیگر کد ثابت نیستند و فقط دو گروه SMART هستند:

```json
{ "id": "new-releases", "titleFa": "تازه‌ها", "titleEn": "New Releases",
  "kind": "SMART",
  "rule": { "sort": "RELEASE_DESC", "perArtistCap": 1, "limit": 20 } }
```

با `perArtistCap = 1` شلف «آهنگ جدید» هیچ‌وقت چند آهنگ پشت‌سرهم از یک خواننده نشان نمی‌دهد.

### ۸.۷ تست‌های ضدرگرسیون (مهم)

```kotlin
@Test fun `every artwork belongs to the track's artist`() {
    catalog.tracks.forEach { t ->
        val a = t.artwork ?: return@forEach
        assertTrue("${t.title}: cover owner ${a.ownerArtistId} != ${t.artistId}",
            a.ownerArtistId == t.artistId || a.ownerArtistId in t.featuredArtistIds)
    }
}
@Test fun `no artwork file is shared between different owners`() {
    val owners = catalog.tracks.mapNotNull { it.artwork }
        .groupBy({ it.url }, { it.ownerArtistId })
    owners.forEach { (url, o) -> assertEquals("$url shared by $o", 1, o.toSet().size) }
}
@Test fun `new-releases group respects perArtistCap`() { /* ... */ }
```

با همین دو تست، وضعیت فعلی (۲۴ کاور مشترک) در CI شکست می‌خورد و تکرار آن غیرممکن می‌شود.

### ۸.۸ مهاجرت دیتای فعلی (گام‌به‌گام)

1. اسکریپت یک‌بارمصرف: تبدیل ۷۰ آهنگ hard-coded به `tracks.json` + استخراج `artists.json` (≈ ۴۰ خواننده).
2. برای هر خواننده تصویر اختصاصی تهیه شود (نام فایل = `artistId`)؛ کاورهای دارای واترمارک حذف شوند.
3. آهنگ‌هایی که تصویر مالک‌دار ندارند موقتاً Initials نمایش دهند.
4. حذف `getIranTracks()` تا `getGermanyTracks()` از `MusicRepository`.
5. `MoodCategory.coverUrl` جایگزین شود با گرادیان/آیکون یا تصویر اختصاصی mood.

### ۸.۹ معیار پذیرش
- در هیچ شلف، جستجو، پلیر و Mini Player عکسی برای آهنگ نمایش داده نمی‌شود مگر مالک آن همان خواننده/گروه (یا feat.) باشد.
- شلف «آهنگ جدید» حداکثر یک آهنگ از هر خواننده دارد (قابل تنظیم با JSON).
- افزودن یا تغییر گروه با تغییر JSON و بدون Build جدید ممکن است.
- Migration داده‌های قبلی کاربر (علاقه‌مندی‌ها، پلی‌لیست‌ها) را حفظ می‌کند.

---

## ۹. نقشه راه (Roadmap)

| Phase | هدف | کارها | تلاش* |
|-------|-----|-------|--------|
| **0** پایدارسازی | امن و قابل build | `gradlew` + wrapper، رفع CI و keystore (N-15، N-16)؛ `debugImplementation` برای App Check (N-09)؛ R8 (N-10)؛ حذف APK از git (N-17)؛ تصمیم درباره منبع پخش مجاز (N-08)؛ Room Schema Export + Migration (N-14) | M |
| **1** نیازمندی ۲ | Artist/Group/Artwork | مدل داده، JSON کاتالوگ، `ArtworkResolver`، `GroupEngine`، بازنویسی Home، تست‌های بخش ۸.۷ | L |
| **2** نیازمندی ۱ | لاگین ایمیل | Prototype تست از ایران ← `AuthRepository` ← صفحات ← Route Guard ← حذف حساب ← داده به‌ازای کاربر | L |
| **3** معماری | کاهش بدهی فنی | Hilt، `navigation-compose`، شکستن God classها، `strings.xml`، Accessibility | L |
| **4** کیفیت | تست و انتشار | تست Repository/ViewModel/UI، Baseline Profile، release AAB امضاشده در CI، Crash reporting | M |

\* S ≈ چند روز، M ≈ ۱ تا ۲ هفته، L ≈ ۲ تا ۴ هفته برای یک توسعه‌دهنده (تخمین تقریبی).

**جایگزین ترتیب:** اگر بک‌اند لاگین از قبل آماده یا Firebase روی شبکه ایران تایید شود، Phase 1 و 2 می‌توانند موازی اجرا شوند؛ فقط تغییر Room (Migration ۳→۴) باید یک‌بار و هماهنگ انجام شود تا دو Migration پشت‌سرهم لازم نشود.

---

## ۱۰. ریسک‌ها

| ریسک | احتمال | اثر | کاهش |
|------|--------|-----|-------|
| دسترسی‌ناپذیری Firebase بدون VPN در ایران | بالا (نیاز به تست) | بسیار بالا | Prototype زودهنگام + `AuthRepository` انتزاعی + گزینه بک‌اند اختصاصی |
| ریسک حقوقی/Play Policy (N-08) | بالا | بسیار بالا | منبع دارای مجوز، حذف scraping و واترمارک |
| حذف دیتای کاربر در Migration | بالا اگر مایگریشن نوشته نشود | بالا | Phase 0 و تست Migration |
| مجوز تصویر خواننده‌ها | متوسط | بالا | تصاویر دارای مجوز، یا Initials تا زمان تهیه |
| افزایش حجم/زمان build با Firebase | کم | کم | حذف وابستگی‌های بلااستفاده |
| تغییر شدید `MusicRepository` همزمان با قابلیت جدید | متوسط | متوسط | ابتدا Extract به `CatalogRepository` بدون تغییر رفتار، سپس تغییر منبع |

---

## ۱۱. نکات تکمیلی (Best Practices)

- ایجاد `ArtworkResolver` و `GroupEngine` به‌صورت pure و تست‌پذیر؛ UI فقط `ArtworkRef` را رندر کند.
- استفاده از `artistId` پایدار (slug) به‌جای نام نمایشی در هر relation.
- تصاویر خواننده در چند اندازه (۱۲۸/۳۲۰/۶۰۰) و فرمت WebP برای کاهش حجم.
- Coil با `placeholder` سبک و `error` برابر Initials؛ هرگز تصویر عمومی «شبیه» نمایش داده نشود.
- Semantic Versioning و Changelog خودکار؛ بدون commit فایل APK.

---

## ۱۲. پیوست: بازتولید آمار کاورها

```python
import re, collections
s = open('app/src/main/java/com/example/data/repository/MusicRepository.kt', encoding='utf8').read()
rows = re.findall(r'Track\(\s*id = (\d+)L,\s*title = "([^"]*)",\s*artist = "([^"]*)".*?artworkUrl = "([^"]*)"', s, re.S)
cov = collections.defaultdict(set)
for _, _, artist, url in rows:
    cov[url.split('/')[-1]].add(artist)
print({c: sorted(a) for c, a in cov.items() if len(a) > 1})   # کاورهای مشترک بین چند خواننده
```

---

*پایان گزارش. پیشنهاد گام بعد: (۱) Pass دوم روی `MusicPlayerController` و صفحات UI باقی‌مانده، (۲) پیاده‌سازی Phase 0 و Phase 1 روی یک branch جدا.*
