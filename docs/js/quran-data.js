/**
 * قاعدة بيانات القرآن الكريم، السور، القراء، ومحرك تنظيف النصوص القرآنية
 */

const BASE_AUDIO_URL = "https://verse.mp3quran.net/data/";

const RECITERS_LIST = [
  { id: "akhdar", nameArabic: "ابراهيم الأخضر", nameEnglish: "Ibrahim Akhdar", serverIdentifier: `${BASE_AUDIO_URL}Ibrahim_Akhdar_32kbps/` },
  { id: "aldosary", nameArabic: "ابراهيم الدوسري قراءة ورش", nameEnglish: "Ibrahim Aldosary", serverIdentifier: `${BASE_AUDIO_URL}warsh_ibrahim_aldosary_128kbps/` },
  { id: "shaheen", nameArabic: "أحمد خليل شاهين", nameEnglish: "Ahmed Khalil Shaheen", serverIdentifier: `${BASE_AUDIO_URL}Ahmad_Shaheen_128kbps/` },
  { id: "neana", nameArabic: "أحمد نعينع", nameEnglish: "Ahmed Neana", serverIdentifier: `${BASE_AUDIO_URL}Ahmed_Neana_128kbps/` },
  { id: "alaqimy", nameArabic: "أكرم العلقمي", nameEnglish: "Akram AlAlaqimy", serverIdentifier: `${BASE_AUDIO_URL}Akram_AlAlaqimy_128kbps/` },
  { id: "tunaiji", nameArabic: "خليفة الطنيجي", nameEnglish: "Khalifa Al-Tunaiji", serverIdentifier: `${BASE_AUDIO_URL}khalefa_al_tunaiji_64kbps/` },
  { id: "abdulbasit", nameArabic: "عبدالباسط عبد الصمد مرتل", nameEnglish: "AbdulBaset AbdulSamad", serverIdentifier: `${BASE_AUDIO_URL}Abdul_Basit_Murattal_64kbps/` },
  { id: "abdulbasit_mujawwad", nameArabic: "عبدالباسط عبد الصمد مجود", nameEnglish: "AbdulBaset AbdulSamad Mujawwad", serverIdentifier: `${BASE_AUDIO_URL}Abdul_Basit_Mujawwad_128kbps/` },
  { id: "abdulbasit_warsh", nameArabic: "عبدالباسط عبد الصمد قراءة ورش", nameEnglish: "AbdulBaset AbdulSamad Warsh", serverIdentifier: `${BASE_AUDIO_URL}warsh_Abdul_Basit_128kbps/` },
  { id: "hudhaify", nameArabic: "عبدالرحمن الحذيفي", nameEnglish: "Abdul Rahman Al-Hudhaify", serverIdentifier: `${BASE_AUDIO_URL}Hudhaify_128kbps/` },
  { id: "ayyoub", nameArabic: "محمد أيوب", nameEnglish: "Muhammad Ayyoub", serverIdentifier: `${BASE_AUDIO_URL}Muhammad_Ayyoub_128kbps/` },
  { id: "jibreel", nameArabic: "محمد جبريل", nameEnglish: "Muhammad Jibreel", serverIdentifier: `${BASE_AUDIO_URL}Muhammad_Jibreel_128kbps/` },
  { id: "minshawi", nameArabic: "محمد صديق المنشاوي مرتل", nameEnglish: "Mohamed Siddiq Al-Minshawi", serverIdentifier: `${BASE_AUDIO_URL}Minshawy_Murattal_128kbps/` },
  { id: "minshawi_mujawwad", nameArabic: "محمد صديق المنشاوي مجود", nameEnglish: "Mohamed Siddiq Al-Minshawi Mujawwad", serverIdentifier: `${BASE_AUDIO_URL}Minshawy_Mujawwad_128kbps/` },
  { id: "tablaway", nameArabic: "محمد الطبلاوي", nameEnglish: "Mohammad Al-Tablaway", serverIdentifier: `${BASE_AUDIO_URL}Mohammad_al_Tablaway_128kbps/` },
  { id: "husary", nameArabic: "محمود خليل الحصري مرتل", nameEnglish: "Mahmoud Khalil Al-Husary", serverIdentifier: `${BASE_AUDIO_URL}Husary_128kbps/` },
  { id: "husary_mujawwad", nameArabic: "محمود خليل الحصري مجود", nameEnglish: "Mahmoud Khalil Al-Husary Mujawwad", serverIdentifier: `${BASE_AUDIO_URL}Husary_128kbps_Mujawwad/` },
  { id: "husary_muallim", nameArabic: "محمود خليل الحصري معلم", nameEnglish: "Mahmoud Khalil Al-Husary Muallim", serverIdentifier: `${BASE_AUDIO_URL}Husary_Muallim_128kbps/` },
  { id: "albanna", nameArabic: "محمود علي البنا", nameEnglish: "Mahmoud Ali Al-Banna", serverIdentifier: `${BASE_AUDIO_URL}mahmoud_ali_al_banna_32kbps/` },
  { id: "salamah", nameArabic: "ياسر سلامة", nameEnglish: "Yaser Salamah", serverIdentifier: `${BASE_AUDIO_URL}Yaser_Salamah_128kbps/` }
];

const SURAH_LIST = [
  { id: 1, nameArabic: "الفاتحة", nameEnglish: "Al-Fatihah", ayahCount: 7, revelationType: "مكية", startPage: 1 },
  { id: 2, nameArabic: "البقرة", nameEnglish: "Al-Baqarah", ayahCount: 286, revelationType: "مدنية", startPage: 2 },
  { id: 3, nameArabic: "آل عمران", nameEnglish: "Aal-E-Imran", ayahCount: 200, revelationType: "مدنية", startPage: 50 },
  { id: 4, nameArabic: "النساء", nameEnglish: "An-Nisa", ayahCount: 176, revelationType: "مدنية", startPage: 77 },
  { id: 5, nameArabic: "المائدة", nameEnglish: "Al-Ma'idah", ayahCount: 120, revelationType: "مدنية", startPage: 106 },
  { id: 6, nameArabic: "الأنعام", nameEnglish: "Al-An'am", ayahCount: 165, revelationType: "مكية", startPage: 128 },
  { id: 7, nameArabic: "الأعراف", nameEnglish: "Al-A'raf", ayahCount: 206, revelationType: "مكية", startPage: 151 },
  { id: 8, nameArabic: "الأنفال", nameEnglish: "Al-Anfal", ayahCount: 75, revelationType: "مدنية", startPage: 177 },
  { id: 9, nameArabic: "التوبة", nameEnglish: "At-Tawbah", ayahCount: 129, revelationType: "مدنية", startPage: 187 },
  { id: 10, nameArabic: "يونس", nameEnglish: "Yunus", ayahCount: 109, revelationType: "مكية", startPage: 208 },
  { id: 11, nameArabic: "هود", nameEnglish: "Hud", ayahCount: 123, revelationType: "مكية", startPage: 221 },
  { id: 12, nameArabic: "يوسف", nameEnglish: "Yusuf", ayahCount: 111, revelationType: "مكية", startPage: 235 },
  { id: 13, nameArabic: "الرعد", nameEnglish: "Ar-Ra'd", ayahCount: 43, revelationType: "مدنية", startPage: 249 },
  { id: 14, nameArabic: "إبراهيم", nameEnglish: "Ibrahim", ayahCount: 52, revelationType: "مكية", startPage: 255 },
  { id: 15, nameArabic: "الحجر", nameEnglish: "Al-Hijr", ayahCount: 99, revelationType: "مكية", startPage: 262 },
  { id: 16, nameArabic: "النحل", nameEnglish: "An-Nahl", ayahCount: 128, revelationType: "مكية", startPage: 267 },
  { id: 17, nameArabic: "الإسراء", nameEnglish: "Al-Isra", ayahCount: 111, revelationType: "مكية", startPage: 282 },
  { id: 18, nameArabic: "الكهف", nameEnglish: "Al-Kahf", ayahCount: 110, revelationType: "مكية", startPage: 293 },
  { id: 19, nameArabic: "مريم", nameEnglish: "Maryamm", ayahCount: 98, revelationType: "مكية", startPage: 305 },
  { id: 20, nameArabic: "طه", nameEnglish: "Taha", ayahCount: 135, revelationType: "مكية", startPage: 312 },
  { id: 21, nameArabic: "الأنبياء", nameEnglish: "Al-Anbiya", ayahCount: 112, revelationType: "مكية", startPage: 322 },
  { id: 22, nameArabic: "الحج", nameEnglish: "Al-Hajj", ayahCount: 78, revelationType: "مدنية", startPage: 332 },
  { id: 23, nameArabic: "المؤمنون", nameEnglish: "Al-Mu'minun", ayahCount: 118, revelationType: "مكية", startPage: 342 },
  { id: 24, nameArabic: "النور", nameEnglish: "An-Nur", ayahCount: 64, revelationType: "مدنية", startPage: 350 },
  { id: 25, nameArabic: "الفرقان", nameEnglish: "Al-Furqan", ayahCount: 77, revelationType: "مكية", startPage: 359 },
  { id: 26, nameArabic: "الشعراء", nameEnglish: "Ash-Shu'ara", ayahCount: 227, revelationType: "مكية", startPage: 367 },
  { id: 27, nameArabic: "النمل", nameEnglish: "An-Naml", ayahCount: 93, revelationType: "مكية", startPage: 377 },
  { id: 28, nameArabic: "القصص", nameEnglish: "Al-Qasas", ayahCount: 88, revelationType: "مكية", startPage: 385 },
  { id: 29, nameArabic: "العنكبوت", nameEnglish: "Al-Ankabut", ayahCount: 69, revelationType: "مكية", startPage: 396 },
  { id: 30, nameArabic: "الروم", nameEnglish: "Ar-Rum", ayahCount: 60, revelationType: "مكية", startPage: 404 },
  { id: 31, nameArabic: "لقمان", nameEnglish: "Luqman", ayahCount: 34, revelationType: "مكية", startPage: 411 },
  { id: 32, nameArabic: "السجدة", nameEnglish: "As-Sajdah", ayahCount: 30, revelationType: "مكية", startPage: 415 },
  { id: 33, nameArabic: "الأحزاب", nameEnglish: "Al-Ahzab", ayahCount: 73, revelationType: "مدنية", startPage: 418 },
  { id: 34, nameArabic: "سبأ", nameEnglish: "Saba", ayahCount: 54, revelationType: "مكية", startPage: 428 },
  { id: 35, nameArabic: "فاطر", nameEnglish: "Fatir", ayahCount: 45, revelationType: "مكية", startPage: 434 },
  { id: 36, nameArabic: "يس", nameEnglish: "Yasin", ayahCount: 83, revelationType: "مكية", startPage: 440 },
  { id: 37, nameArabic: "الصافات", nameEnglish: "As-Saffat", ayahCount: 182, revelationType: "مكية", startPage: 445 },
  { id: 38, nameArabic: "ص", nameEnglish: "Sad", ayahCount: 88, revelationType: "مكية", startPage: 453 },
  { id: 39, nameArabic: "الزمر", nameEnglish: "Az-Zumar", ayahCount: 75, revelationType: "مكية", startPage: 458 },
  { id: 40, nameArabic: "غافر", nameEnglish: "Ghafir", ayahCount: 85, revelationType: "مكية", startPage: 467 },
  { id: 41, nameArabic: "فصلت", nameEnglish: "Fussilat", ayahCount: 54, revelationType: "مكية", startPage: 477 },
  { id: 42, nameArabic: "الشورى", nameEnglish: "Ash-Shura", ayahCount: 53, revelationType: "مكية", startPage: 483 },
  { id: 43, nameArabic: "الزخرف", nameEnglish: "Az-Zukhruf", ayahCount: 89, revelationType: "مكية", startPage: 489 },
  { id: 44, nameArabic: "الدخان", nameEnglish: "Ad-Dukhan", ayahCount: 59, revelationType: "مكية", startPage: 496 },
  { id: 45, nameArabic: "الجاثية", nameEnglish: "Al-Jathiyah", ayahCount: 37, revelationType: "مكية", startPage: 499 },
  { id: 46, nameArabic: "الأحقاف", nameEnglish: "Al-Ahqaf", ayahCount: 35, revelationType: "مكية", startPage: 502 },
  { id: 47, nameArabic: "محمد", nameEnglish: "Muhammad", ayahCount: 38, revelationType: "مدنية", startPage: 507 },
  { id: 48, nameArabic: "الفتح", nameEnglish: "Al-Fath", ayahCount: 29, revelationType: "مدنية", startPage: 511 },
  { id: 49, nameArabic: "الحجرات", nameEnglish: "Al-Hujurat", ayahCount: 18, revelationType: "مدنية", startPage: 515 },
  { id: 50, nameArabic: "ق", nameEnglish: "Qaf", ayahCount: 45, revelationType: "مكية", startPage: 518 },
  { id: 51, nameArabic: "الذاريات", nameEnglish: "Adh-Dhariyat", ayahCount: 60, revelationType: "مكية", startPage: 520 },
  { id: 52, nameArabic: "الطور", nameEnglish: "At-Tur", ayahCount: 49, revelationType: "مكية", startPage: 523 },
  { id: 53, nameArabic: "النجم", nameEnglish: "An-Najm", ayahCount: 62, revelationType: "مكية", startPage: 526 },
  { id: 54, nameArabic: "القمر", nameEnglish: "Al-Qamar", ayahCount: 55, revelationType: "مكية", startPage: 528 },
  { id: 55, nameArabic: "الرحمن", nameEnglish: "Ar-Rahman", ayahCount: 78, revelationType: "مدنية", startPage: 531 },
  { id: 56, nameArabic: "الواقعة", nameEnglish: "Al-Waqi'ah", ayahCount: 96, revelationType: "مكية", startPage: 534 },
  { id: 57, nameArabic: "الحديد", nameEnglish: "Al-Hadid", ayahCount: 29, revelationType: "مدنية", startPage: 537 },
  { id: 58, nameArabic: "المجادلة", nameEnglish: "Al-Mujadila", ayahCount: 22, revelationType: "مدنية", startPage: 542 },
  { id: 59, nameArabic: "الحشر", nameEnglish: "Al-Hashr", ayahCount: 24, revelationType: "مدنية", startPage: 545 },
  { id: 60, nameArabic: "الممتحنة", nameEnglish: "Al-Mumtahanah", ayahCount: 13, revelationType: "مدنية", startPage: 549 },
  { id: 61, nameArabic: "الصف", nameEnglish: "As-Saff", ayahCount: 14, revelationType: "مدنية", startPage: 551 },
  { id: 62, nameArabic: "الجمعة", nameEnglish: "Al-Jumu'ah", ayahCount: 11, revelationType: "مدنية", startPage: 553 },
  { id: 63, nameArabic: "المنافقون", nameEnglish: "Al-Munafiqun", ayahCount: 11, revelationType: "مدنية", startPage: 554 },
  { id: 64, nameArabic: "التغابن", nameEnglish: "At-Taghabun", ayahCount: 18, revelationType: "مدنية", startPage: 556 },
  { id: 65, nameArabic: "الطلاق", nameEnglish: "At-Talaq", ayahCount: 12, revelationType: "مدنية", startPage: 558 },
  { id: 66, nameArabic: "التحريم", nameEnglish: "At-Tahrim", ayahCount: 12, revelationType: "مدنية", startPage: 560 },
  { id: 67, nameArabic: "الملك", nameEnglish: "Al-Mulk", ayahCount: 30, revelationType: "مكية", startPage: 562 },
  { id: 68, nameArabic: "القلم", nameEnglish: "Al-Qalam", ayahCount: 52, revelationType: "مكية", startPage: 564 },
  { id: 69, nameArabic: "الحاقة", nameEnglish: "Al-Haaqqah", ayahCount: 52, revelationType: "مكية", startPage: 566 },
  { id: 70, nameArabic: "المعارج", nameEnglish: "Al-Ma'arij", ayahCount: 44, revelationType: "مكية", startPage: 568 },
  { id: 71, nameArabic: "نوح", nameEnglish: "Nuh", ayahCount: 28, revelationType: "مكية", startPage: 570 },
  { id: 72, nameArabic: "الجن", nameEnglish: "Al-Jinn", ayahCount: 28, revelationType: "مكية", startPage: 572 },
  { id: 73, nameArabic: "المزمل", nameEnglish: "Al-Muzzammil", ayahCount: 20, revelationType: "مكية", startPage: 574 },
  { id: 74, nameArabic: "المدثر", nameEnglish: "Al-Muddaththir", ayahCount: 56, revelationType: "مكية", startPage: 575 },
  { id: 75, nameArabic: "القيامة", nameEnglish: "Al-Qiyamah", ayahCount: 40, revelationType: "مكية", startPage: 577 },
  { id: 76, nameArabic: "الإنسان", nameEnglish: "Al-Insan", ayahCount: 31, revelationType: "مدنية", startPage: 578 },
  { id: 77, nameArabic: "المرسلات", nameEnglish: "Al-Mursalat", ayahCount: 50, revelationType: "مكية", startPage: 580 },
  { id: 78, nameArabic: "النبأ", nameEnglish: "An-Naba", ayahCount: 40, revelationType: "مكية", startPage: 582 },
  { id: 79, nameArabic: "النازعات", nameEnglish: "An-Nazi'at", ayahCount: 46, revelationType: "مكية", startPage: 583 },
  { id: 80, nameArabic: "عبس", nameEnglish: "Abasa", ayahCount: 42, revelationType: "مكية", startPage: 585 },
  { id: 81, nameArabic: "التكوير", nameEnglish: "At-Takwir", ayahCount: 29, revelationType: "مكية", startPage: 586 },
  { id: 82, nameArabic: "الانفطار", nameEnglish: "Al-Infitar", ayahCount: 19, revelationType: "مكية", startPage: 587 },
  { id: 83, nameArabic: "المطففين", nameEnglish: "Al-Mutaffifin", ayahCount: 36, revelationType: "مكية", startPage: 587 },
  { id: 84, nameArabic: "الانشقاق", nameEnglish: "Al-Inshiqaq", ayahCount: 25, revelationType: "مكية", startPage: 589 },
  { id: 85, nameArabic: "البروج", nameEnglish: "Al-Buruj", ayahCount: 22, revelationType: "مكية", startPage: 590 },
  { id: 86, nameArabic: "الطارق", nameEnglish: "At-Tariq", ayahCount: 17, revelationType: "مكية", startPage: 591 },
  { id: 87, nameArabic: "الأعلى", nameEnglish: "Al-A'la", ayahCount: 19, revelationType: "مكية", startPage: 591 },
  { id: 88, nameArabic: "الغاشية", nameEnglish: "Al-Ghashiyah", ayahCount: 26, revelationType: "مكية", startPage: 592 },
  { id: 89, nameArabic: "الفجر", nameEnglish: "Al-Fajr", ayahCount: 30, revelationType: "مكية", startPage: 593 },
  { id: 90, nameArabic: "البلد", nameEnglish: "Al-Balad", ayahCount: 20, revelationType: "مكية", startPage: 594 },
  { id: 91, nameArabic: "الشمس", nameEnglish: "Ash-Shams", ayahCount: 15, revelationType: "مكية", startPage: 595 },
  { id: 92, nameArabic: "الليل", nameEnglish: "Al-Layl", ayahCount: 21, revelationType: "مكية", startPage: 595 },
  { id: 93, nameArabic: "الضحى", nameEnglish: "Ad-Duha", ayahCount: 11, revelationType: "مكية", startPage: 596 },
  { id: 94, nameArabic: "الشرح", nameEnglish: "Ash-Sharh", ayahCount: 8, revelationType: "مكية", startPage: 596 },
  { id: 95, nameArabic: "التين", nameEnglish: "At-Tin", ayahCount: 8, revelationType: "مكية", startPage: 597 },
  { id: 96, nameArabic: "العلق", nameEnglish: "Al-Alaq", ayahCount: 19, revelationType: "مكية", startPage: 597 },
  { id: 97, nameArabic: "القدر", nameEnglish: "Al-Padr", ayahCount: 5, revelationType: "مكية", startPage: 598 },
  { id: 98, nameArabic: "البينة", nameEnglish: "Al-Bayyinah", ayahCount: 8, revelationType: "مدنية", startPage: 598 },
  { id: 99, nameArabic: "الزلزلة", nameEnglish: "Az-Zalzalah", ayahCount: 8, revelationType: "مدنية", startPage: 599 },
  { id: 100, nameArabic: "العاديات", nameEnglish: "Al-Adiyat", ayahCount: 11, revelationType: "مكية", startPage: 599 },
  { id: 101, nameArabic: "القارعة", nameEnglish: "Al-Qari'ah", ayahCount: 11, revelationType: "مكية", startPage: 600 },
  { id: 102, nameArabic: "التكاثر", nameEnglish: "At-Takathur", ayahCount: 8, revelationType: "مكية", startPage: 600 },
  { id: 103, nameArabic: "العصر", nameEnglish: "Al-Asr", ayahCount: 3, revelationType: "مكية", startPage: 601 },
  { id: 104, nameArabic: "الهمزة", nameEnglish: "Al-Humazah", ayahCount: 9, revelationType: "مكية", startPage: 601 },
  { id: 105, nameArabic: "الفيل", nameEnglish: "Al-Fil", ayahCount: 5, revelationType: "مكية", startPage: 601 },
  { id: 106, nameArabic: "قريش", nameEnglish: "Quraysh", ayahCount: 4, revelationType: "مكية", startPage: 602 },
  { id: 107, nameArabic: "الماعون", nameEnglish: "Al-Ma'un", ayahCount: 7, revelationType: "مكية", startPage: 602 },
  { id: 108, nameArabic: "الكوثر", nameEnglish: "Al-Kawthar", ayahCount: 3, revelationType: "مكية", startPage: 602 },
  { id: 109, nameArabic: "الكافرون", nameEnglish: "Al-Kafirun", ayahCount: 6, revelationType: "مكية", startPage: 603 },
  { id: 110, nameArabic: "النصر", nameEnglish: "An-Nasr", ayahCount: 3, revelationType: "مدنية", startPage: 603 },
  { id: 111, nameArabic: "المسد", nameEnglish: "Al-Masad", ayahCount: 5, revelationType: "مكية", startPage: 603 },
  { id: 112, nameArabic: "الإخلاص", nameEnglish: "Al-Ikhlas", ayahCount: 4, revelationType: "مكية", startPage: 604 },
  { id: 113, nameArabic: "الفلق", nameEnglish: "Al-Falaq", ayahCount: 5, revelationType: "مكية", startPage: 604 },
  { id: 114, nameArabic: "الناس", nameEnglish: "An-Nas", ayahCount: 6, revelationType: "مكية", startPage: 604 }
];

// Fallback instant dataset for Surah Al-Fatihah (renders instantly in 0ms)
const FATIHAH_AYAH_FALLBACK = [
  { numberInSurah: 1, textArabic: "بِسْمِ ٱللَّهِ ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ", page: 1, juz: 1 },
  { numberInSurah: 2, textArabic: "ٱلْحَمْدُ لِلَّهِ رَبِّ ٱلْعَـٰلَمِينَ", page: 1, juz: 1 },
  { numberInSurah: 3, textArabic: "ٱلرَّحْمَـٰنِ ٱلرَّحِيمِ", page: 1, juz: 1 },
  { numberInSurah: 4, textArabic: "مَـٰلِكِ يَوْمِ ٱلدِّينِ", page: 1, juz: 1 },
  { numberInSurah: 5, textArabic: "إِيَّاكَ نَعْبُدُ وَإِيَّاكَ نَسْتَعِينُ", page: 1, juz: 1 },
  { numberInSurah: 6, textArabic: "ٱهْدِنَا ٱلصِّرَٰطَ ٱلْمُسْتَقِيمَ", page: 1, juz: 1 },
  { numberInSurah: 7, textArabic: "صِرَٰطَ ٱلَّذِينَ أَنْعَمْتَ عَلَيْهِمْ غَيْرِ ٱلْمَغْضُوبِ عَلَيْهِمْ وَلَا ٱلضَّآلِّينَ", page: 1, juz: 1 }
];

function sanitizeUthmanicText(text) {
  if (!text) return "";
  const bareNoonNextLetters = "[يرملونصذثكجشقسدطزفتضظب]";
  const noonSukoonPattern = new RegExp(`(ن)[\\u0652\\u06DF\\u06E0\\u06E1](?=\\s*${bareNoonNextLetters})`, 'g');
  
  return text.replace(noonSukoonPattern, "$1")
    .replace(/\u06DF/g, '\u06E0')
    .replace(/\u06E4/g, '\u0653')
    .replace(/\u0600/g, "")
    .replace(/\u06DD/g, "")
    .replace(/\uFEFF/g, "")
    .replace(/\u200A/g, "")
    .replace(/\u2060/g, "");
}

const QuranDataManager = {
  rawQuranJson: null,
  cachedSurahs: new Map(),

  async init() {
    // Populate Fatihah in cache immediately
    this.cachedSurahs.set(1, FATIHAH_AYAH_FALLBACK.map(a => ({
      ...a,
      textArabic: sanitizeUthmanicText(a.textArabic)
    })));

    try {
      const response = await fetch('assets/data/quran.json');
      if (response.ok) {
        this.rawQuranJson = await response.json();
      }
    } catch (err) {
      console.warn('Could not fetch full quran.json eagerly:', err);
    }
  },

  getSurahById(id) {
    return SURAH_LIST.find(s => s.id === id) || SURAH_LIST[0];
  },

  getReciterById(id) {
    return RECITERS_LIST.find(r => r.id === id) || RECITERS_LIST[0];
  },

  async getAyahsForSurah(surahId) {
    if (this.cachedSurahs.has(surahId)) {
      return this.cachedSurahs.get(surahId);
    }

    if (!this.rawQuranJson) {
      try {
        const response = await fetch('assets/data/quran.json');
        this.rawQuranJson = await response.json();
      } catch (e) {
        console.error('Error fetching quran.json:', e);
        return surahId === 1 ? this.cachedSurahs.get(1) : [];
      }
    }

    const rawList = this.rawQuranJson[surahId.toString()] || [];
    const sanitizedList = rawList.map(item => ({
      numberInSurah: item.numberInSurah,
      globalNumber: item.globalNumber,
      textArabic: sanitizeUthmanicText(item.textArabic),
      page: item.page || 1,
      juz: item.juz || 1
    }));

    this.cachedSurahs.set(surahId, sanitizedList);
    return sanitizedList;
  },

  getAudioUrl(reciterServerIdentifier, surahId, ayahNumberInSurah) {
    const baseUrl = reciterServerIdentifier.endsWith('/') ? reciterServerIdentifier : `${reciterServerIdentifier}/`;
    const s = surahId.toString().padStart(3, '0');
    const a = ayahNumberInSurah.toString().padStart(3, '0');
    return `${baseUrl}${s}${a}.mp3`;
  }
};
