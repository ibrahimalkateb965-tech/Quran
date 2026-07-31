package com.example.data.model

data class Reciter(
    val id: String,
    val nameArabic: String,
    val nameEnglish: String,
    val serverIdentifier: String
) {
    companion object {
        val DEFAULT_RECITERS = listOf(
            Reciter("husary", "الشيخ محمود خليل الحصري", "Mahmoud Khalil Al-Husary", "ar.husary"),
            Reciter("afasy", "الشيخ مشاري راشد العفاسي", "Mishary Rashid Al-Afasy", "ar.alafasy"),
            Reciter("minshawi", "الشيخ محمد صديق المنشاوي", "Mohamed Siddiq Al-Minshawi", "ar.minshawi"),
            Reciter("abdulbasit", "الشيخ عبد الباسط عبد الصمد", "AbdulBaset AbdulSamad", "ar.abdulbasitmurattal"),
            Reciter("maher", "الشيخ ماهر المعيقلي", "Maher Al-Muaiqly", "ar.mahermuaiqly"),
            Reciter("sufi", "الشيخ عبد الرشيد صوفي", "Abdul Rashid Sufi", "ar.abdulrashidsufi")
        )
    }
}
