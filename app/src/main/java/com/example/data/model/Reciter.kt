package com.example.data.model

data class Reciter(
    val id: String,
    val nameArabic: String,
    val nameEnglish: String,
    val serverIdentifier: String
) {
    companion object {
        private const val BASE_URL = "https://verse.mp3quran.net/data/"

        private fun getTypePriority(name: String): Int {
            return when {
                name.contains("مرتل") -> 1
                name.contains("مجود") -> 2
                name.contains("معلم") -> 3
                name.contains("ورش") -> 4
                else -> 5
            }
        }

        private fun getBaseName(name: String): String {
            return name.replace(" مرتل", "")
                .replace(" مجود", "")
                .replace(" معلم", "")
                .replace(" قراءة ورش", "")
        }

        private val customOrder = listOf(
            "ابراهيم الأخضر",
            "ابراهيم الدوسري",
            "أحمد نعينع",
            "أحمد خليل شاهين",
            "أكرم العلقمي",
            "خليفة الطنيجي",
            "عبدالباسط عبد الصمد",
            "عبدالرحمن الحذيفي",
            "محمد أيوب",
            "محمد صديق المنشاوي",
            "محمد جبريل",
            "محمد الطبلاوي",
            "محمود خليل الحصري",
            "محمود علي البنا",
            "ياسر سلامة"
        )

        private fun getCustomOrderPriority(name: String): Int {
            val baseName = getBaseName(name)
            val index = customOrder.indexOf(baseName)
            return if (index != -1) index else 999
        }

        val DEFAULT_RECITERS = listOf(
            Reciter("akhdar", "ابراهيم الأخضر", "Ibrahim Akhdar", "${BASE_URL}Ibrahim_Akhdar_32kbps/"),
            Reciter("aldosary", "ابراهيم الدوسري قراءة ورش", "Ibrahim Aldosary", "${BASE_URL}warsh_ibrahim_aldosary_128kbps/"),
            Reciter("shaheen", "أحمد خليل شاهين", "Ahmed Khalil Shaheen", "${BASE_URL}Ahmad_Shaheen_128kbps/"),
            Reciter("neana", "أحمد نعينع", "Ahmed Neana", "${BASE_URL}Ahmed_Neana_128kbps/"),
            Reciter("alaqimy", "أكرم العلقمي", "Akram AlAlaqimy", "${BASE_URL}Akram_AlAlaqimy_128kbps/"),
            Reciter("tunaiji", "خليفة الطنيجي", "Khalifa Al-Tunaiji", "${BASE_URL}khalefa_al_tunaiji_64kbps/"),
            Reciter("abdulbasit", "عبدالباسط عبد الصمد مرتل", "AbdulBaset AbdulSamad", "${BASE_URL}Abdul_Basit_Murattal_64kbps/"),
            Reciter("abdulbasit_mujawwad", "عبدالباسط عبد الصمد مجود", "AbdulBaset AbdulSamad Mujawwad", "${BASE_URL}Abdul_Basit_Mujawwad_128kbps/"),
            Reciter("abdulbasit_warsh", "عبدالباسط عبد الصمد قراءة ورش", "AbdulBaset AbdulSamad Warsh", "${BASE_URL}warsh_Abdul_Basit_128kbps/"),
            Reciter("hudhaify", "عبدالرحمن الحذيفي", "Abdul Rahman Al-Hudhaify", "${BASE_URL}Hudhaify_128kbps/"),
            Reciter("ayyoub", "محمد أيوب", "Muhammad Ayyoub", "${BASE_URL}Muhammad_Ayyoub_128kbps/"),
            Reciter("jibreel", "محمد جبريل", "Muhammad Jibreel", "${BASE_URL}Muhammad_Jibreel_128kbps/"),
            Reciter("minshawi", "محمد صديق المنشاوي مرتل", "Mohamed Siddiq Al-Minshawi", "${BASE_URL}Minshawy_Murattal_128kbps/"),
            Reciter("minshawi_mujawwad", "محمد صديق المنشاوي مجود", "Mohamed Siddiq Al-Minshawi Mujawwad", "${BASE_URL}Minshawy_Mujawwad_128kbps/"),
            Reciter("tablaway", "محمد الطبلاوي", "Mohammad Al-Tablaway", "${BASE_URL}Mohammad_al_Tablaway_128kbps/"),
            Reciter("husary", "محمود خليل الحصري مرتل", "Mahmoud Khalil Al-Husary", "${BASE_URL}Husary_128kbps/"),
            Reciter("husary_mujawwad", "محمود خليل الحصري مجود", "Mahmoud Khalil Al-Husary Mujawwad", "${BASE_URL}Husary_128kbps_Mujawwad/"),
            Reciter("husary_muallim", "محمود خليل الحصري معلم", "Mahmoud Khalil Al-Husary Muallim", "${BASE_URL}Husary_Muallim_128kbps/"),
            Reciter("albanna", "محمود علي البنا", "Mahmoud Ali Al-Banna", "${BASE_URL}mahmoud_ali_al_banna_32kbps/"),
            Reciter("salamah", "ياسر سلامة", "Yaser Salamah", "${BASE_URL}Yaser_Salamah_128kbps/")
        ).sortedWith(compareBy({ getCustomOrderPriority(it.nameArabic) }, { getTypePriority(it.nameArabic) }))
    }
}
