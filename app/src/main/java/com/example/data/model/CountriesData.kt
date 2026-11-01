package com.example.data.model

object CountriesData {
    val iran = Country(
        code = "IR",
        nameFa = "ایران",
        nameEn = "Iran",
        flag = "🇮🇷",
        defaultSearchTerm = "Homayoun Shajarian",
        topArtists = listOf(
            "همایون شجریان",
            "محسن چاوشی",
            "ابی",
            "گوگوش",
            "شادمهر عقیلی",
            "محسن یگانه",
            "سیاوش قمیشی",
            "علیرضا قربانی",
            "سیروان خسروی",
            "هایده"
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
            "Quevedo",
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
            "Stromae",
            "Daft Punk",
            "Indila",
            "David Guetta",
            "Mylène Farmer"
        )
    )

    val germany = Country(
        code = "DE",
        nameFa = "آلمان",
        nameEn = "Germany",
        flag = "🇩🇪",
        defaultSearchTerm = "Rammstein",
        topArtists = listOf(
            "Rammstein",
            "Robin Schulz",
            "Zedd",
            "Milky Chance",
            "Herbert Grönemeyer"
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

    // Iran is first/primary as requested!
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

    val moodCategories: List<MoodCategory> = listOf(
        MoodCategory(
            id = "everyday",
            titleFa = "روزمره",
            titleEn = "Everyday",
            query = "acoustic chill",
            iconEmoji = "☕"
        ),
        MoodCategory(
            id = "driving",
            titleFa = "رانندگی",
            titleEn = "Driving",
            query = "road trip drive",
            iconEmoji = "🚗"
        ),
        MoodCategory(
            id = "happy",
            titleFa = "شاد و پرانرژی",
            titleEn = "Upbeat",
            query = "dance pop party",
            iconEmoji = "🎉"
        ),
        MoodCategory(
            id = "relax",
            titleFa = "آرامش‌بخش",
            titleEn = "Relaxing",
            query = "ambient piano calm",
            iconEmoji = "🌿"
        ),
        MoodCategory(
            id = "focus",
            titleFa = "تمرکز و مطالعه",
            titleEn = "Focus",
            query = "instrumental focus study",
            iconEmoji = "🧠"
        ),
        MoodCategory(
            id = "workout",
            titleFa = "ورزش و فیتنس",
            titleEn = "Workout",
            query = "workout energy beats",
            iconEmoji = "⚡"
        )
    )
}
