package com.example.data.model

object CountriesData {
    val iran = Country(
        code = "IR",
        nameFa = "ایران",
        nameEn = "Iran",
        flag = "🇮🇷",
        defaultSearchTerm = "Homayoun Shajarian",
        topArtists = listOf(
            "محسن چاوشی",
            "محسن یگانه",
            "محمدرضا شجریان",
            "همایون شجریان",
            "شادمهر عقیلی",
            "هایده",
            "ابی",
            "گوگوش",
            "علیرضا قربانی",
            "سیروان خسروی"
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

    // Modern Radio Javan / Spotify inspired vibrant mood & explore tiles
    val moodCategories: List<MoodCategory> = listOf(
        MoodCategory(
            id = "new_releases",
            titleFa = "بروزترین‌ها",
            titleEn = "New Releases",
            query = "new_releases",
            colorStartHex = 0xFF7CB342,
            colorEndHex = 0xFF558B2F,
            coverUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=400&q=80",
            isNewRelease = true
        ),
        MoodCategory(
            id = "pop",
            titleFa = "پاپ و هیت",
            titleEn = "Pop Hits",
            query = "pop",
            colorStartHex = 0xFFE53935,
            colorEndHex = 0xFFB71C1C,
            coverUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=400&q=80"
        ),
        MoodCategory(
            id = "party",
            titleFa = "پارتی و شاد",
            titleEn = "Party & Dance",
            query = "dance pop party",
            colorStartHex = 0xFFD81B60,
            colorEndHex = 0xFF880E4F,
            coverUrl = "https://images.unsplash.com/photo-1492684223066-81342ee5ff30?w=400&q=80"
        ),
        MoodCategory(
            id = "driving",
            titleFa = "رانندگی و جاده",
            titleEn = "Road & Drive",
            query = "road trip drive",
            colorStartHex = 0xFFFB8C00,
            colorEndHex = 0xFFE65100,
            coverUrl = "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?w=400&q=80"
        ),
        MoodCategory(
            id = "traditional",
            titleFa = "سنتی و اصیل",
            titleEn = "Traditional",
            query = "traditional",
            colorStartHex = 0xFF8E24AA,
            colorEndHex = 0xFF4A148C,
            coverUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&q=80"
        ),
        MoodCategory(
            id = "relax",
            titleFa = "پاییزی و آرامش",
            titleEn = "Chill & Autumn",
            query = "ambient piano calm",
            colorStartHex = 0xFFF4511E,
            colorEndHex = 0xFFBF360C,
            coverUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=400&q=80"
        ),
        MoodCategory(
            id = "workout",
            titleFa = "ورزش و انرژی",
            titleEn = "Workout & Club",
            query = "workout energy beats",
            colorStartHex = 0xFF039BE5,
            colorEndHex = 0xFF01579B,
            coverUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=400&q=80"
        ),
        MoodCategory(
            id = "focus",
            titleFa = "تمرکز و بیکلام",
            titleEn = "Instrumental",
            query = "instrumental focus study",
            colorStartHex = 0xFF00897B,
            colorEndHex = 0xFF004D40,
            coverUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=400&q=80"
        )
    )
}
