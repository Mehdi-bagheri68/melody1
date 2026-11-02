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

    val usa = Country(
        code = "US",
        nameFa = "ایالات متحده",
        nameEn = "United States",
        flag = "🇺🇸",
        defaultSearchTerm = "The Weeknd",
        topArtists = listOf(
            "The Weeknd",
            "Taylor Swift",
            "Kendrick Lamar",
            "Billie Eilish",
            "Bruno Mars",
            "Post Malone"
        )
    )

    val spain = Country(
        code = "ES",
        nameFa = "اسپانیا",
        nameEn = "Spain",
        flag = "🇪🇸",
        defaultSearchTerm = "Rosalia",
        topArtists = listOf(
            "Rosalía",
            "Enrique Iglesias",
            "El Principe Gitano",
            "C. Tangana",
            "Alejandro Sanz"
        )
    )

    val uk = Country(
        code = "GB",
        nameFa = "بریتانیا",
        nameEn = "United Kingdom",
        flag = "🇬🇧",
        defaultSearchTerm = "Adele",
        topArtists = listOf(
            "Adele",
            "Ed Sheeran",
            "Coldplay",
            "Dua Lipa",
            "Harry Styles",
            "Queen"
        )
    )

    val france = Country(
        code = "FR",
        nameFa = "فرانسه",
        nameEn = "France",
        flag = "🇫🇷",
        defaultSearchTerm = "Stromae",
        topArtists = listOf(
            "Claude Debussy",
            "Stromae",
            "Daft Punk",
            "Indila",
            "David Guetta"
        )
    )

    val germany = Country(
        code = "DE",
        nameFa = "آلمان",
        nameEn = "Germany",
        flag = "🇩🇪",
        defaultSearchTerm = "Beethoven",
        topArtists = listOf(
            "Ludwig van Beethoven",
            "Rammstein",
            "Robin Schulz",
            "Zedd"
        )
    )

    val turkey = Country(
        code = "TR",
        nameFa = "ترکیه",
        nameEn = "Turkey",
        flag = "🇹🇷",
        defaultSearchTerm = "Tarkan",
        topArtists = listOf(
            "Tarkan",
            "Sezen Aksu",
            "Ezhel",
            "Semicenk",
            "Simge"
        )
    )

    val allCountries: List<Country> = listOf(
        iran,
        usa,
        spain,
        uk,
        france,
        germany,
        turkey
    )

    fun getCountryByCode(code: String): Country {
        return allCountries.find { it.code.equals(code, ignoreCase = true) } ?: iran
    }

    // Modern Radio Javan / Spotify inspired vibrant mood & explore tiles with real bundled photographic covers
    val moodCategories: List<MoodCategory> = listOf(
        MoodCategory(
            id = "new_releases",
            titleFa = "بروزترین‌ها",
            titleEn = "New Releases",
            query = "new_releases",
            colorStartHex = 0xFF7CB342,
            colorEndHex = 0xFF33691E,
            coverUrl = "file:///android_asset/covers/cover_1017.jpg",
            isNewRelease = true
        ),
        MoodCategory(
            id = "pop",
            titleFa = "پاپ و هیت",
            titleEn = "Pop Hits",
            query = "pop",
            colorStartHex = 0xFFE53935,
            colorEndHex = 0xFF8E0000,
            coverUrl = "file:///android_asset/covers/cover_1016.jpg"
        ),
        MoodCategory(
            id = "party",
            titleFa = "پارتی و شاد",
            titleEn = "Party & Dance",
            query = "dance pop party",
            colorStartHex = 0xFFD81B60,
            colorEndHex = 0xFF6A0080,
            coverUrl = "file:///android_asset/covers/cover_1024.jpg"
        ),
        MoodCategory(
            id = "driving",
            titleFa = "رانندگی و جاده",
            titleEn = "Road & Drive",
            query = "road trip drive",
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
            query = "ambient piano calm",
            colorStartHex = 0xFFF4511E,
            colorEndHex = 0xFF870000,
            coverUrl = "file:///android_asset/covers/cover_1011.jpg"
        ),
        MoodCategory(
            id = "workout",
            titleFa = "ورزش و انرژی",
            titleEn = "Workout & Club",
            query = "workout energy beats",
            colorStartHex = 0xFF039BE5,
            colorEndHex = 0xFF004BA0,
            coverUrl = "file:///android_asset/covers/cover_1019.jpg"
        ),
        MoodCategory(
            id = "focus",
            titleFa = "عاشقانه و احساسی",
            titleEn = "Romantic & Chill",
            query = "romantic focus study",
            colorStartHex = 0xFF00897B,
            colorEndHex = 0xFF003D33,
            coverUrl = "file:///android_asset/covers/cover_1005.jpg"
        )
    )
}
