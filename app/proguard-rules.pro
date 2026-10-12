# R8 rules for Quran Blind App (com.aistudio.quranblind.a11y)
# Library rules (Media3, Coroutines, Compose, Hilt, kotlinx.serialization)
# are supplied by each AAR's consumer rules — do not add blanket keeps here.

# Readable stack traces in Play Console (mapping.txt is uploaded with the AAB)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Media playback service. The manifest entry is already kept by AAPT2;
# this line documents intent and guards against manifest refactors.
-keep class com.example.service.QuranAudioService
