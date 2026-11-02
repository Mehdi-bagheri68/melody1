package com.example.data.model

object CountriesData {
    val iran = Country(
        code = "IR",
        nameFa = "ایران",
        nameEn = "Iran",
        flag = "🇮🇷",
        defaultSearchTerm = "محسن چاوشی",
        topArtists = listOf(
            "محسن چاوشی",
            "همایون شجریان",
            "محمدرضا شجریان",
            "محسن یگانه",
            "علیرضا قربانی",
            "سیروان خسروی",
            "بهنام بانی",
            "عرفان طهماسبی",
            "رضا بهرام",
            "گرشا رضایی",
            "حامیم",
            "مجید رضوی",
            "فریدون آسرایی",
            "احسان خواجه امیری",
            "علی لهراسبی"
        )
    )

    val turkey = Country(
        code = "TR",
        nameFa = "ترکیه",
        nameEn = "Turkey",
        flag = "🇹🇷",
        defaultSearchTerm = "Tarkan",
        topArtists = listOf(
            "İbrahim Tatlıses",
            "Tarkan",
            "Mahsun Kırmızıgül",
            "Emrah",
            "Ramil Sedali",
            "Turkish Club DJ"
        )
    )

    val usa = Country(
        code = "US",
        nameFa = "ایالات متحده",
        nameEn = "United States",
        flag = "🇺🇸",
        defaultSearchTerm = "The Weeknd",
        topArtists = listOf(
            "The Weeknd",
            "Eminem",
            "Dr. Dre & Snoop Dogg",
            "Rema & Selena Gomez",
            "Sean Paul",
            "BoyWithUke",
            "Tiagz"
        )
    )

    val uk = Country(
        code = "GB",
        nameFa = "بریتانیا",
        nameEn = "United Kingdom",
        flag = "🇬🇧",
        defaultSearchTerm = "Ed Sheeran",
        topArtists = listOf(
            "Ed Sheeran",
            "Bring Me The Horizon",
            "D-Block Europe",
            "Sami Yusuf",
            "London Sessions"
        )
    )

    val spain = Country(
        code = "ES",
        nameFa = "اسپانیا",
        nameEn = "Spain",
        flag = "🇪🇸",
        defaultSearchTerm = "Gipsy Kings",
        topArtists = listOf(
            "Gipsy Kings",
            "Antonio Banderas",
            "Cris MJ",
            "Koli Band",
            "Spanish Guitar Masters"
        )
    )

    val france = Country(
        code = "FR",
        nameFa = "فرانسه",
        nameEn = "France",
        flag = "🇫🇷",
        defaultSearchTerm = "Adriano Celentano",
        topArtists = listOf(
            "Gipsy Kings (France)",
            "Adriano Celentano",
            "Paris Piano & Acoustic",
            "French Touch Electro"
        )
    )

    val germany = Country(
        code = "DE",
        nameFa = "آلمان",
        nameEn = "Germany",
        flag = "🇩🇪",
        defaultSearchTerm = "Modern Talking",
        topArtists = listOf(
            "Modern Talking",
            "Berlin Euro Electronic",
            "Dekamond Piano",
            "German Guitar Sessions"
        )
    )

    val allCountries: List<Country> = listOf(
        iran,
        turkey,
        usa,
        uk,
        spain,
        france,
        germany
    )

    fun getCountryByCode(code: String): Country {
        return allCountries.find { it.code.equals(code, ignoreCase = true) } ?: iran
    }

    /**
     * Returns country-tailored Explore & Mood categories so switching countries dynamically
     * updates both the categories/genres and the playlists for that specific country.
     */
    fun getMoodCategoriesForCountry(country: Country): List<MoodCategory> {
        return when (country.code.uppercase()) {
            "TR" -> listOf(
                MoodCategory(
                    id = "new_releases",
                    titleFa = "بروزترین‌های ترکیه",
                    titleEn = "Turkey New Releases",
                    query = "new_releases",
                    colorStartHex = 0xFFE53935,
                    colorEndHex = 0xFF8E0000,
                    coverUrl = "file:///android_asset/covers/cover_1024.jpg",
                    isNewRelease = true
                ),
                MoodCategory(
                    id = "pop",
                    titleFa = "ترکیش پاپ (Tarkan)",
                    titleEn = "Turkish Pop Hits",
                    query = "pop",
                    colorStartHex = 0xFFD81B60,
                    colorEndHex = 0xFF6A0080,
                    coverUrl = "file:///android_asset/covers/cover_1016.jpg"
                ),
                MoodCategory(
                    id = "traditional",
                    titleFa = "آرابسک و تاتلیس",
                    titleEn = "Arabesk & Tatlıses",
                    query = "traditional",
                    colorStartHex = 0xFF8E24AA,
                    colorEndHex = 0xFF38006B,
                    coverUrl = "file:///android_asset/covers/cover_1015.jpg"
                ),
                MoodCategory(
                    id = "party",
                    titleFa = "استانبول کلاب و شاد",
                    titleEn = "Istanbul Party & Dance",
                    query = "party",
                    colorStartHex = 0xFFFB8C00,
                    colorEndHex = 0xFFBF360C,
                    coverUrl = "file:///android_asset/covers/cover_1017.jpg"
                ),
                MoodCategory(
                    id = "driving",
                    titleFa = "رانندگی در استانبول",
                    titleEn = "Bosphorus Night Drive",
                    query = "driving",
                    colorStartHex = 0xFF039BE5,
                    colorEndHex = 0xFF004BA0,
                    coverUrl = "file:///android_asset/covers/cover_1013.jpg"
                ),
                MoodCategory(
                    id = "relax",
                    titleFa = "آرامش و آکوستیک ترکی",
                    titleEn = "Turkish Chill & Acoustic",
                    query = "relax",
                    colorStartHex = 0xFF00897B,
                    colorEndHex = 0xFF003D33,
                    coverUrl = "file:///android_asset/covers/cover_1011.jpg"
                ),
                MoodCategory(
                    id = "workout",
                    titleFa = "انرژی و ریتم ترکی",
                    titleEn = "Turkish Workout Beats",
                    query = "workout",
                    colorStartHex = 0xFF7CB342,
                    colorEndHex = 0xFF33691E,
                    coverUrl = "file:///android_asset/covers/cover_1019.jpg"
                ),
                MoodCategory(
                    id = "focus",
                    titleFa = "عاشقانه و دامار ترکی",
                    titleEn = "Turkish Romantic (Damar)",
                    query = "focus",
                    colorStartHex = 0xFFF4511E,
                    colorEndHex = 0xFF870000,
                    coverUrl = "file:///android_asset/covers/cover_1005.jpg"
                )
            )

            "US" -> listOf(
                MoodCategory(
                    id = "new_releases",
                    titleFa = "بروزترین‌های آمریکا",
                    titleEn = "US New Releases",
                    query = "new_releases",
                    colorStartHex = 0xFF1E88E5,
                    colorEndHex = 0xFF0D47A1,
                    coverUrl = "file:///android_asset/covers/cover_1016.jpg",
                    isNewRelease = true
                ),
                MoodCategory(
                    id = "pop",
                    titleFa = "بیلبورد هات ۱۰۰ و پاپ",
                    titleEn = "Billboard Pop Hits",
                    query = "pop",
                    colorStartHex = 0xFFE53935,
                    colorEndHex = 0xFF8E0000,
                    coverUrl = "file:///android_asset/covers/cover_1018.jpg"
                ),
                MoodCategory(
                    id = "traditional",
                    titleFa = "هیپ‌هاپ و رپ آمریکا",
                    titleEn = "US Hip-Hop & Rap",
                    query = "traditional",
                    colorStartHex = 0xFF8E24AA,
                    colorEndHex = 0xFF38006B,
                    coverUrl = "file:///android_asset/covers/cover_1002.jpg"
                ),
                MoodCategory(
                    id = "party",
                    titleFa = "پارتی و کلاب میامی",
                    titleEn = "US Club & Party",
                    query = "party",
                    colorStartHex = 0xFFD81B60,
                    colorEndHex = 0xFF6A0080,
                    coverUrl = "file:///android_asset/covers/cover_1024.jpg"
                ),
                MoodCategory(
                    id = "driving",
                    titleFa = "بزرگراه و رانندگی شبانه",
                    titleEn = "LA Highway Night Drive",
                    query = "driving",
                    colorStartHex = 0xFFFB8C00,
                    colorEndHex = 0xFFBF360C,
                    coverUrl = "file:///android_asset/covers/cover_1013.jpg"
                ),
                MoodCategory(
                    id = "relax",
                    titleFa = "آر اند بی و چیل",
                    titleEn = "Late Night R&B & Chill",
                    query = "relax",
                    colorStartHex = 0xFF00897B,
                    colorEndHex = 0xFF003D33,
                    coverUrl = "file:///android_asset/covers/cover_1011.jpg"
                ),
                MoodCategory(
                    id = "workout",
                    titleFa = "باشگاه و انرژی بالا",
                    titleEn = "US Gym & Workout",
                    query = "workout",
                    colorStartHex = 0xFF7CB342,
                    colorEndHex = 0xFF33691E,
                    coverUrl = "file:///android_asset/covers/cover_1019.jpg"
                ),
                MoodCategory(
                    id = "focus",
                    titleFa = "عاشقانه و ووکال آمریکا",
                    titleEn = "US Romantic & Soul",
                    query = "focus",
                    colorStartHex = 0xFFF4511E,
                    colorEndHex = 0xFF870000,
                    coverUrl = "file:///android_asset/covers/cover_1023.jpg"
                )
            )

            "GB" -> listOf(
                MoodCategory(
                    id = "new_releases",
                    titleFa = "بروزترین‌های بریتانیا",
                    titleEn = "UK Chart New Releases",
                    query = "new_releases",
                    colorStartHex = 0xFF3949AB,
                    colorEndHex = 0xFF1A237E,
                    coverUrl = "file:///android_asset/covers/cover_1020.jpg",
                    isNewRelease = true
                ),
                MoodCategory(
                    id = "pop",
                    titleFa = "بریت‌پاپ و چارت لندن",
                    titleEn = "UK Pop & Chart",
                    query = "pop",
                    colorStartHex = 0xFFE53935,
                    colorEndHex = 0xFF8E0000,
                    coverUrl = "file:///android_asset/covers/cover_1016.jpg"
                ),
                MoodCategory(
                    id = "traditional",
                    titleFa = "راک و آلترناتیو بریتانیا",
                    titleEn = "British Rock & Metal",
                    query = "traditional",
                    colorStartHex = 0xFF6D4C41,
                    colorEndHex = 0xFF3E2723,
                    coverUrl = "file:///android_asset/covers/cover_1004.jpg"
                ),
                MoodCategory(
                    id = "party",
                    titleFa = "کلاب و دنس لندن",
                    titleEn = "London Club & Dance",
                    query = "party",
                    colorStartHex = 0xFFD81B60,
                    colorEndHex = 0xFF6A0080,
                    coverUrl = "file:///android_asset/covers/cover_1017.jpg"
                ),
                MoodCategory(
                    id = "driving",
                    titleFa = "رانندگی در جاده‌های انگلیس",
                    titleEn = "UK Night Drive",
                    query = "driving",
                    colorStartHex = 0xFFFB8C00,
                    colorEndHex = 0xFFBF360C,
                    coverUrl = "file:///android_asset/covers/cover_1013.jpg"
                ),
                MoodCategory(
                    id = "relax",
                    titleFa = "آکوستیک و هوای بارانی",
                    titleEn = "Rainy London Acoustic",
                    query = "relax",
                    colorStartHex = 0xFF00897B,
                    colorEndHex = 0xFF003D33,
                    coverUrl = "file:///android_asset/covers/cover_1012.jpg"
                ),
                MoodCategory(
                    id = "workout",
                    titleFa = "ورزش و انرژی بریتانیا",
                    titleEn = "UK Bass & Workout",
                    query = "workout",
                    colorStartHex = 0xFF039BE5,
                    colorEndHex = 0xFF004BA0,
                    coverUrl = "file:///android_asset/covers/cover_1019.jpg"
                ),
                MoodCategory(
                    id = "focus",
                    titleFa = "بالاد و احساسی انگلیسی",
                    titleEn = "British Ballads & Chill",
                    query = "focus",
                    colorStartHex = 0xFF8E24AA,
                    colorEndHex = 0xFF38006B,
                    coverUrl = "file:///android_asset/covers/cover_1007.jpg"
                )
            )

            "ES" -> listOf(
                MoodCategory(
                    id = "new_releases",
                    titleFa = "بروزترین‌های اسپانیا",
                    titleEn = "Spain New Releases",
                    query = "new_releases",
                    colorStartHex = 0xFFF4511E,
                    colorEndHex = 0xFFBF360C,
                    coverUrl = "file:///android_asset/covers/cover_1025.jpg",
                    isNewRelease = true
                ),
                MoodCategory(
                    id = "traditional",
                    titleFa = "فلامنکو و جیپسی کینگز",
                    titleEn = "Flamenco & Gipsy Kings",
                    query = "traditional",
                    colorStartHex = 0xFFE53935,
                    colorEndHex = 0xFF8E0000,
                    coverUrl = "file:///android_asset/covers/cover_1006.jpg"
                ),
                MoodCategory(
                    id = "pop",
                    titleFa = "لاتین پاپ و مادرید",
                    titleEn = "Spanish & Latin Pop",
                    query = "pop",
                    colorStartHex = 0xFFD81B60,
                    colorEndHex = 0xFF6A0080,
                    coverUrl = "file:///android_asset/covers/cover_1017.jpg"
                ),
                MoodCategory(
                    id = "party",
                    titleFa = "فیستا، ایبیزا و رگتون",
                    titleEn = "Ibiza Fiesta & Reggaeton",
                    query = "party",
                    colorStartHex = 0xFFFB8C00,
                    colorEndHex = 0xFFE65100,
                    coverUrl = "file:///android_asset/covers/cover_1024.jpg"
                ),
                MoodCategory(
                    id = "driving",
                    titleFa = "جاده ساحلی بارسلونا",
                    titleEn = "Spanish Coast Drive",
                    query = "driving",
                    colorStartHex = 0xFF039BE5,
                    colorEndHex = 0xFF004BA0,
                    coverUrl = "file:///android_asset/covers/cover_1014.jpg"
                ),
                MoodCategory(
                    id = "relax",
                    titleFa = "گیتار آرامش‌بخش اسپانیایی",
                    titleEn = "Spanish Guitar Chill",
                    query = "relax",
                    colorStartHex = 0xFF00897B,
                    colorEndHex = 0xFF003D33,
                    coverUrl = "file:///android_asset/covers/cover_1011.jpg"
                ),
                MoodCategory(
                    id = "workout",
                    titleFa = "ریتم تند لاتین و ورزش",
                    titleEn = "Latin Cardio Workout",
                    query = "workout",
                    colorStartHex = 0xFF7CB342,
                    colorEndHex = 0xFF33691E,
                    coverUrl = "file:///android_asset/covers/cover_1019.jpg"
                ),
                MoodCategory(
                    id = "focus",
                    titleFa = "عاشقانه اسپانیایی (Amor)",
                    titleEn = "Spanish Romance",
                    query = "focus",
                    colorStartHex = 0xFF8E24AA,
                    colorEndHex = 0xFF38006B,
                    coverUrl = "file:///android_asset/covers/cover_1027.jpg"
                )
            )

            "FR" -> listOf(
                MoodCategory(
                    id = "new_releases",
                    titleFa = "بروزترین‌های فرانسه",
                    titleEn = "France New Releases",
                    query = "new_releases",
                    colorStartHex = 0xFF1E88E5,
                    colorEndHex = 0xFF1565C0,
                    coverUrl = "file:///android_asset/covers/cover_1018.jpg",
                    isNewRelease = true
                ),
                MoodCategory(
                    id = "pop",
                    titleFa = "پاپ فرانسوی و پاریس",
                    titleEn = "French Pop & Chanson",
                    query = "pop",
                    colorStartHex = 0xFFD81B60,
                    colorEndHex = 0xFF880E4F,
                    coverUrl = "file:///android_asset/covers/cover_1021.jpg"
                ),
                MoodCategory(
                    id = "party",
                    titleFa = "فرنچ تاچ و الکترو",
                    titleEn = "French Touch & Club",
                    query = "party",
                    colorStartHex = 0xFF8E24AA,
                    colorEndHex = 0xFF4A148C,
                    coverUrl = "file:///android_asset/covers/cover_1016.jpg"
                ),
                MoodCategory(
                    id = "driving",
                    titleFa = "رانندگی شبانه در پاریس",
                    titleEn = "Paris Night Drive",
                    query = "driving",
                    colorStartHex = 0xFFFB8C00,
                    colorEndHex = 0xFFBF360C,
                    coverUrl = "file:///android_asset/covers/cover_1013.jpg"
                ),
                MoodCategory(
                    id = "traditional",
                    titleFa = "کلاسیک و گیتار جنوب فرانسه",
                    titleEn = "French Heritage & Gypsy",
                    query = "traditional",
                    colorStartHex = 0xFFE53935,
                    colorEndHex = 0xFF8E0000,
                    coverUrl = "file:///android_asset/covers/cover_1005.jpg"
                ),
                MoodCategory(
                    id = "relax",
                    titleFa = "کافه پاریس و پیانو",
                    titleEn = "Paris Café & Piano",
                    query = "relax",
                    colorStartHex = 0xFF00897B,
                    colorEndHex = 0xFF004D40,
                    coverUrl = "file:///android_asset/covers/cover_1008.jpg"
                ),
                MoodCategory(
                    id = "workout",
                    titleFa = "انرژی و دنس فرانسوی",
                    titleEn = "French Dance Workout",
                    query = "workout",
                    colorStartHex = 0xFF7CB342,
                    colorEndHex = 0xFF33691E,
                    coverUrl = "file:///android_asset/covers/cover_1019.jpg"
                ),
                MoodCategory(
                    id = "focus",
                    titleFa = "عاشقانه فرانسوی (Amour)",
                    titleEn = "Romantique Paris",
                    query = "focus",
                    colorStartHex = 0xFFF4511E,
                    colorEndHex = 0xFF870000,
                    coverUrl = "file:///android_asset/covers/cover_1023.jpg"
                )
            )

            "DE" -> listOf(
                MoodCategory(
                    id = "new_releases",
                    titleFa = "بروزترین‌های آلمان",
                    titleEn = "Germany New Releases",
                    query = "new_releases",
                    colorStartHex = 0xFFFB8C00,
                    colorEndHex = 0xFFE65100,
                    coverUrl = "file:///android_asset/covers/cover_1016.jpg",
                    isNewRelease = true
                ),
                MoodCategory(
                    id = "pop",
                    titleFa = "یوروپاپ و مدرن تاکینگ",
                    titleEn = "Euro-Pop & Modern Talking",
                    query = "pop",
                    colorStartHex = 0xFFE53935,
                    colorEndHex = 0xFF8E0000,
                    coverUrl = "file:///android_asset/covers/cover_1001.jpg"
                ),
                MoodCategory(
                    id = "party",
                    titleFa = "تکنو و کلاب برلین",
                    titleEn = "Berlin Techno & Club",
                    query = "party",
                    colorStartHex = 0xFF8E24AA,
                    colorEndHex = 0xFF38006B,
                    coverUrl = "file:///android_asset/covers/cover_1024.jpg"
                ),
                MoodCategory(
                    id = "driving",
                    titleFa = "رانندگی در اتوبان آلمان",
                    titleEn = "Autobahn High-Speed Drive",
                    query = "driving",
                    colorStartHex = 0xFF039BE5,
                    colorEndHex = 0xFF004BA0,
                    coverUrl = "file:///android_asset/covers/cover_1013.jpg"
                ),
                MoodCategory(
                    id = "traditional",
                    titleFa = "راک و الکترونیک آلمان",
                    titleEn = "German Rock & Synth",
                    query = "traditional",
                    colorStartHex = 0xFF546E7A,
                    colorEndHex = 0xFF263238,
                    coverUrl = "file:///android_asset/covers/cover_1002.jpg"
                ),
                MoodCategory(
                    id = "relax",
                    titleFa = "پیانو و بیکلام آلمانی",
                    titleEn = "Piano & Instrumental Chill",
                    query = "relax",
                    colorStartHex = 0xFF00897B,
                    colorEndHex = 0xFF003D33,
                    coverUrl = "file:///android_asset/covers/cover_1015.jpg"
                ),
                MoodCategory(
                    id = "workout",
                    titleFa = "پاور بیتس و ورزش برلین",
                    titleEn = "Berlin Power Workout",
                    query = "workout",
                    colorStartHex = 0xFF7CB342,
                    colorEndHex = 0xFF33691E,
                    coverUrl = "file:///android_asset/covers/cover_1019.jpg"
                ),
                MoodCategory(
                    id = "focus",
                    titleFa = "ملودیک و تمرکز عمیق",
                    titleEn = "Melodic Deep Focus",
                    query = "focus",
                    colorStartHex = 0xFFD81B60,
                    colorEndHex = 0xFF6A0080,
                    coverUrl = "file:///android_asset/covers/cover_1007.jpg"
                )
            )

            else -> listOf(
                MoodCategory(
                    id = "new_releases",
                    titleFa = "بروزترین‌های ایران",
                    titleEn = "New Releases",
                    query = "new_releases",
                    colorStartHex = 0xFF7CB342,
                    colorEndHex = 0xFF33691E,
                    coverUrl = "file:///android_asset/covers/cover_1017.jpg",
                    isNewRelease = true
                ),
                MoodCategory(
                    id = "pop",
                    titleFa = "پاپ و هیت ایرانی",
                    titleEn = "Persian Pop Hits",
                    query = "pop",
                    colorStartHex = 0xFFE53935,
                    colorEndHex = 0xFF8E0000,
                    coverUrl = "file:///android_asset/covers/cover_1016.jpg"
                ),
                MoodCategory(
                    id = "party",
                    titleFa = "پارتی و شاد",
                    titleEn = "Party & Dance",
                    query = "party",
                    colorStartHex = 0xFFD81B60,
                    colorEndHex = 0xFF6A0080,
                    coverUrl = "file:///android_asset/covers/cover_1024.jpg"
                ),
                MoodCategory(
                    id = "driving",
                    titleFa = "رانندگی و جاده",
                    titleEn = "Road & Drive",
                    query = "driving",
                    colorStartHex = 0xFFFB8C00,
                    colorEndHex = 0xFFBF360C,
                    coverUrl = "file:///android_asset/covers/cover_1013.jpg"
                ),
                MoodCategory(
                    id = "traditional",
                    titleFa = "سنتی و اصیل",
                    titleEn = "Traditional",
                    query = "traditional",
                    colorStartHex = 0xFF8E24AA,
                    colorEndHex = 0xFF38006B,
                    coverUrl = "file:///android_asset/covers/cover_1015.jpg"
                ),
                MoodCategory(
                    id = "relax",
                    titleFa = "پاییزی و آرامش",
                    titleEn = "Chill & Autumn",
                    query = "relax",
                    colorStartHex = 0xFFF4511E,
                    colorEndHex = 0xFF870000,
                    coverUrl = "file:///android_asset/covers/cover_1011.jpg"
                ),
                MoodCategory(
                    id = "workout",
                    titleFa = "ورزش و انرژی",
                    titleEn = "Workout & Club",
                    query = "workout",
                    colorStartHex = 0xFF039BE5,
                    colorEndHex = 0xFF004BA0,
                    coverUrl = "file:///android_asset/covers/cover_1019.jpg"
                ),
                MoodCategory(
                    id = "focus",
                    titleFa = "عاشقانه و احساسی",
                    titleEn = "Romantic & Chill",
                    query = "focus",
                    colorStartHex = 0xFF00897B,
                    colorEndHex = 0xFF003D33,
                    coverUrl = "file:///android_asset/covers/cover_1005.jpg"
                )
            )
        }
    }

    val moodCategories: List<MoodCategory> = getMoodCategoriesForCountry(iran)
}
