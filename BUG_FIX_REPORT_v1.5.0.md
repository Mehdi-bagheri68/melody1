# گزارش کامل تحلیل و رفع باگ‌ها و بهبودهای UI/UX — نسخه ۱.۵.۰ (`v1.5.0` - Build 5)

این مستند بر اساس گزارش ممیزی فنی و تجربه کاربری نسخه ۱.۴ (`تحلیل نسخه 1.4`) تدوین شده و وضعیت رفع تک‌تک ۱۴ باگ فنی (`BUG-01` تا `BUG-14`) و ۱۴ مورد تجربه کاربری (`UI-01` تا `UI-14`) را همراه با فایل‌ها و تست‌های مربوطه شرح می‌دهد.

---

## بخش اول: رفع باگ‌های فنی و منطقی (`BUG-01` تا `BUG-14`)

### ۱. `BUG-01` (بحرانی): حذف کامل جایگزینی پنهان آهنگ نامعتبر با آهنگ دیگر
* **ریشه مشکل در نسخه ۱.۴:** در `MusicPlayerController.resolvePlayableUrl` و `MusicRepository.sanitizeStoredTrack`، در صورت خالی یا نامعتبر بودن لینک استریم، به طور خودکار لینک ثابت `"Mohsen Chavoshi - Roode Sookhte"` یا آرایه `fallbackIranStreams` بر اساس `track.id % size` جایگزین می‌شد که باعث می‌شد عنوان و کاور یک آهنگ نمایش داده شود اما صدای آهنگ دیگری پخش شود.
* **اقدام انجام شده در `v1.5.0`:**
  * کلاس اختصاصی `SourceResolver.kt` طراحی و پیاده‌سازی شد که **فقط و فقط** لینک‌های معتبر متعلق به **همان قطعه** (با تطبیق دقیق `id` یا زوج `title + artist`) را استخراج می‌کند.
  * تمامی مراجع به لینک جایگزین ثابت (`Mohsen Chavoshi - Roode Sookhte`) و لیست چرخشی `fallbackIranStreams` از `MusicPlayerController.kt` و `MusicRepository.kt` به طور کامل حذف شدند.
  * در صورتی که هیچ منبع معتبری برای آهنگ انتخاب‌شده در دسترس نباشد، پلیر وضعیت شفاف `ErrorCategory.SOURCE_UNAVAILABLE` («فایل صوتی این آهنگ در حال حاضر در دسترس نیست») را ثبت کرده و پخش کاذب انجام نمی‌دهد.

### ۲. `BUG-02` (بحرانی): طبقه‌بندی خطاها و سقف تلاش مجدد هوشمند (`Bounded Exponential Backoff`)
* **ریشه مشکل در نسخه ۱.۴:** در `onPlayerError` تمام خطاها با یک حلقه بی‌نهایت (`while (isActive)`) هر ۳ ثانیه تکرار می‌شدند؛ حتی برای خطاهای دائمی مانند `404 Not Found` یا خطای فرمت/کدک.
* **اقدام انجام شده در `v1.5.0`:**
  * متد `classifyPlaybackError(error: PlaybackException)` در `MusicPlayerController.kt` پیاده‌سازی شد که خطاها را به ۴ دسته تقسیم می‌کند:
    1. `SOURCE_NOT_FOUND` (کدهای HTTP `400, 401, 403, 404, 410`) -> بدون تکرار روی همان URL؛ در صورت وجود Mirror دیگر از همان آهنگ، سوییچ به Mirror بعدی و در غیر این صورت توقف و اعلام خطا.
    2. `DECODER_FORMAT` (خطاهای Parser/Extractor/Decoder) -> توقف فوری بدون Retry بی‌فایده.
    3. `RATE_LIMITED` (کد HTTP `429`) -> رعایت هدر `Retry-After` با سقف محدود.
    4. `NETWORK_TRANSIENT` (قطعی شبکه، Timeout، DNS یا `5xx`) -> تلاش مجدد با **Exponential Backoff و Jitter** (`1.5s, 3s, 6s, 12s, 20s, 30s`) تا سقف حداکثر **`MAX_RETRY_ATTEMPTS = 6`**.
  * پس از رسیدن به سقف تلاش خودکار، حلقه متوقف شده و دکمه **«تلاش مجدد دستی / Retry»** در `MiniPlayer` و `FullPlayerSheet` در اختیار کاربر قرار می‌گیرد.

### ۳. `BUG-03` (بالا): اعتبارسنجی دقیق `MediaItem` پیش از `seekTo` در بازیابی پخش
* **ریشه مشکل در نسخه ۱.۴:** در `attemptResumeCurrentTrack` بر اساس متغیرهای `lastKnownPositionMs` و `exoPlayer.mediaItemCount > 0` مستقیماً `seekTo` صدا زده می‌شد که هنگام تغییر سریع ترک می‌توانست ترک جدید را به ثانیه ترک قبلی ببرد.
* **اقدام انجام شده در `v1.5.0`:**
  * پیش از هرگونه `seekTo` در `attemptResumeCurrentTrack`، بررسی می‌شود که:
    * `exoPlayer.currentMediaItem?.mediaId == track.id.toString()`
    * و URI فعلی پلیر دقیقاً برابر با `streamCandidate.url` باشد.
  * مقدار `resumePos` با `durationMs` واقعی پلیر Clamp می‌شود (`coerceIn(0L, safeMax)`).
  * در تمامی متدهای `playTrack`، `stopAndClearPlayback` و `STATE_ENDED`، وضعیت‌های `retryJob`، `_isRetrying`، `_retryCount`، `_playbackError` و `lastKnownPositionMs` ریست می‌شوند.

### ۴. `BUG-04` (بالا): جداسازی و مقاوم‌سازی پارسر HTML (`MusicsFaHtmlParser`)
* **ریشه مشکل در نسخه ۱.۴:** استخراج HTML با Regex مستقیم درون `MusicRepository` انجام می‌شد و در برابر صفحات کپچا/ضدربات یا HTML Entityها آسیب‌پذیر بود.
* **اقدام انجام شده در `v1.5.0`:**
  * منطق پارسینگ به کلاس مستقل `MusicsFaHtmlParser.kt` منتقل شد.
  * تشخیص صفحات ضدربات (`Cloudflare`، `cf-browser-verification`، `captcha`، `Attention Required`) اضافه شد تا از استخراج داده ناقص جلوگیری شود.
  * پاکسازی کامل HTML Entities (`&#8211;`، `&amp;` و ...) و اعتبارسنجی DTO (`isValidAudioStreamUrl`) پیش از ساخت `Track` اعمال شد.
  * سلامت هاست‌ها در `SourceResolver` ثبت می‌شود تا هاست‌های دارای خطای مکرر موقتاً در اولویت پایین‌تر قرار گیرند.

### ۵. `BUG-05` (بالا): حذف زمان جعلی `215000L` (`03:35`) و دریافت زمان واقعی از ExoPlayer
* **ریشه مشکل در نسخه ۱.۴:** تمام ترک‌های استخراج‌شده از وب مقدار ثابت `durationMs = 215000L` می‌گرفتند که باعث نمایش زمان نادرست در کارت‌ها و نوار Seek می‌شد.
* **اقدام انجام شده در `v1.5.0`:**
  * برای ترک‌های فاقد متادیتای زمان، مقدار `durationMs = 0L` (نامشخص) قرار داده شد.
  * ویژگی `Track.durationFormatted` برای مقادیر `<= 0L` عبارت `"--:--"` را برمی‌گرداند.
  * به محض ورود `ExoPlayer` به وضعیت `Player.STATE_READY`، مقدار واقعی `exoPlayer.duration` خوانده شده و هم در `_durationMs` و هم در `_currentTrack.value.durationMs` به‌روزرسانی می‌شود.

### ۶. `BUG-06` (متوسط): ایمن‌سازی کش جستجو (`Mutex` + `TTL` + کلید نرمال‌شده + سینک علاقه‌مندی‌ها)
* **ریشه مشکل در نسخه ۱.۴:** `searchCache` یک `mutableMapOf` معمولی بدون همگام‌سازی همزمان، بدون انقضا (`TTL`) و با کلید خام بود و وضعیت `isFavorite` در نتایج کش‌شده قدیمی می‌ماند.
* **اقدام انجام شده در `v1.5.0`:**
  * دسترسی به `searchCache` با `Mutex` ایمن‌سازی شد (`searchCacheMutex.withLock`).
  * زمان انقضای ۵ دقیقه‌ای (`CACHE_TTL_MS = 5 * 60 * 1000L`) و سقف ۴۰ ورودی (`MAX_CACHE_ENTRIES = 40`) اعمال گردید.
  * کلید کش با `buildNormalizedCacheKey` (حذف فاصله‌های اضافه، تبدیل حروف عربی `ي/ك` به فارسی `ی/ک` و lowercase) ساخته می‌شود.
  * هنگام بازگرداندن نتایج از کش، وضعیت `isFavorite` به صورت زنده با آخرین `favoriteIds` دیتابیس Room ادغام می‌شود (`mergeFavoriteStatus`).

### ۷. `BUG-07` و `BUG-08` (متوسط): مدیریت صحیح Loading و لغو `Job`های قبلی هنگام تغییر سریع کشور/مود
* **ریشه مشکل در نسخه ۱.۴:** در `loadTrendingForCountry` مقدار `_isLoadingTrending.value = true` ست نمی‌شد و در تغییر سریع کشور، مود، خواننده یا پلی‌لیست، درخواست‌های قبلی لغو نمی‌شدند (احتمال Race Condition).
* **اقدام انجام شده در `v1.5.0`:**
  * در `MainViewModel.kt` متغیرهای `trendingJob`، `moodJob`، `artistJob`، `playlistTracksJob` و `searchJob` تعریف شدند و پیش از هر درخواست جدید، `Job` قبلی `cancel()` می‌شود.
  * در ابتدای `loadTrendingForCountry` مقدار `_isLoadingTrending.value = true` قرار داده شد، قبل از اعمال خروجی بررسی می‌شود که `_selectedCountry.value.code == country.code` باشد، و در بلوک `finally` مقدار `_isLoadingTrending.value = false` تضمین می‌گردد.

### ۸. `BUG-09` و `BUG-10` (بالا): واقعی‌سازی سوییچ کیفیت صدا (`AudioQuality`) و حالت بدون فیلترشکن (`iranNoVpnMode`)
* **ریشه مشکل در نسخه ۱.۴:** تغییر `AudioQuality` فقط متغیر UI را عوض می‌کرد و در `FullPlayerSheet` متن ثابت `"No-VPN Direct Stream"` نمایش داده می‌شد.
* **اقدام انجام شده در `v1.5.0`:**
  * در `SourceResolver` و `MusicPlayerController.setAudioQuality`، برای سرورهایی که هر دو نسخه `(320).mp3` و `(128).mp3` را ارائه می‌دهند، URL استریم واقعاً بین نسخه `320kbps` (در حالت `HIGH`) و نسخه `128kbps` (در حالت `STANDARD` و `DATA_SAVER`) با حفظ ثانیه پخش فعلی سوییچ می‌شود و پارامتر `trackSelectionParameters.maxAudioBitrate` نیز در ExoPlayer تنظیم می‌گردد.
  * در صورتی که ترک فقط یک کیفیت داشته باشد، در `FullPlayerSheet` به صورت صادقانه برچسب «تک‌کیفیت (Single Stream)» نمایش داده می‌شود.
  * گزینه `iranNoVpnMode` در `UserPreferences` و `SettingsScreen` اضافه شد که به کاربر اجازه می‌دهد بین حالت «فقط سرورهای مستقیم بدون نیاز به VPN» و «استفاده همزمان از سرورهای بین‌المللی iTunes» انتخاب کند.

### ۹. `BUG-11`، `BUG-12`، `BUG-13` و `BUG-14` (متوسط): مدیریت منابع، امنیت لاگ، لاگر ساختاریافته و تطبیق دقیق رکوردها
* **`BUG-11`:** زمان `WakeLock.acquire` به سقف ایمن ۳۰ دقیقه محدود شد و در `release()` آزادسازی کامل `WakeLock`، `WifiLock` و `unregisterNetworkCallback` تضمین گردید.
* **`BUG-12`:** در `ItunesApi.kt` سطح لاگ از `Level.BODY` به `Level.BASIC` (فقط در `BuildConfig.DEBUG` و `Level.NONE` در Release) تغییر یافت و هدرهای حساس (`Authorization`، `Cookie`، `Set-Cookie`) با `redactHeader` سانسور شدند.
* **`BUG-13`:** کلاس `AppLogger.kt` ایجاد شد و تمام بلوک‌های `catch (_: Exception)` خالی با لاگ‌های ساختاریافته و بدون افشای اطلاعات حساس جایگزین شدند.
* **`BUG-14`:** در `sanitizeStoredTrack` تطبیق رکوردها صرفاً بر اساس `id` یا تطبیق همزمان `title + artist` انجام می‌شود و هیچ رکوردی به اشتباه با آهنگ اول لیست جایگزین نمی‌شود.

---

## بخش دوم: بهبودهای رابط و تجربه کاربری (`UI-01` تا `UI-14`)

1. **`UI-01` و `UI-13`:** حذف ارتفاع ثابت `height(98.dp)` در `ModernMoodCard` و جایگزینی با `heightIn(min = 96.dp)` برای جلوگیری از بریده شدن متن در فونت‌های بزرگ سیستم، به همراه کلید پایدار (`key`) در تمامی لیست‌های `LazyColumn` و `LazyRow`.
2. **`UI-02`:** افزودن وضعیت خطا و دکمه «تلاش مجدد / Retry» در بخش ترندهای `HomeScreen` در صورت بروز خطا یا خالی بودن لیست.
3. **`UI-03` و `UI-04`:** افزودن پنل فیلتر جمع‌شونده (`فیلترها (N)`) همراه با دکمه «پاک کردن فیلترها / Clear All» و اکشن کیبورد `ImeAction.Search` در `SearchScreen`.
4. **`UI-05` و `UI-06`:** حفظ تب انتخاب‌شده در `LibraryScreen` با `rememberSaveable`، نمایش تعداد آیتم‌ها در عنوان تب‌ها (`علاقه‌مندی‌ها (N)`، `پلی‌لیست‌ها (N)`، `تاریخچه (N)`) و افزودن دیالوگ تأیید (`AlertDialog`) پیش از پاکسازی تاریخچه یا حذف پلی‌لیست.
5. **`UI-07`:** نمایش وضعیت شفاف تلاش مجدد (`تلاش X/6`) و دکمه «تلاش مجدد دستی» در `MiniPlayer` و `FullPlayerSheet` به جای بنر مبهم همیشگی.
6. **`UI-08`:** غیرفعال‌سازی `Slider` (`enabled = hasKnownDuration`) و نمایش `"--:--"` زمانی که مدت واقعی آهنگ هنوز مشخص نشده است (`durationMs <= 0L`).
7. **`UI-09`:** چرخش صفحه گرامافون (`Vinyl`) در `FullPlayerSheet` فقط زمانی که `isPlaying == true` است (توقف چرخش در حالت Buffering یا Error).
8. **`UI-10`:** نمایش شفاف کیفیت واقعی استریم در `FullPlayerSheet` و فعال‌سازی واقعی دکمه «پاکسازی حافظه کش موقت» در `SettingsScreen`.
9. **`UI-11`:** استفاده از `BidiHelper` (ایزوله‌سازی یونیکد RTL/LTR) برای نمایش صحیح متون ترکیبی فارسی و انگلیسی در کارت‌های آهنگ و پلیر.
10. **`UI-12`:** رعایت استاندارد حداقل ناحیه لمس `48dp` (`minimumInteractiveComponentSize()`) در تمام دکمه‌های کنترلی.
11. **`UI-14`:** حذف انتخاب تصادفی کاور آلبوم‌های دیگر (`fallbackLocalCovers[id % size]`) و جایگزینی با دیسک گرامافون خنثی و شیک در `TrackArtwork` در صورت نبودن تصویر کاور.
