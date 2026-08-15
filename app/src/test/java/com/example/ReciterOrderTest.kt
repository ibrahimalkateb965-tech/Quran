package com.example

import com.example.data.model.Reciter
import org.junit.Assert.assertTrue
import org.junit.Test

class ReciterOrderTest {

    @Test
    fun testReciterOrder_JibreelAndTablawiAfterMinshawi() {
        val reciters = Reciter.DEFAULT_RECITERS
        
        val lastMinshawiIndex = reciters.indexOfLast { it.nameArabic.contains("المنشاوي") }
        val jibreelIndex = reciters.indexOfFirst { it.nameArabic.contains("جبريل") }
        val tablawiIndex = reciters.indexOfFirst { it.nameArabic.contains("الطبلاوي") }
        val firstHusaryIndex = reciters.indexOfFirst { it.nameArabic.contains("الحصري") }

        assertTrue("Minshawi must exist in the reciter list", lastMinshawiIndex != -1)
        assertTrue("Jibreel must exist in the reciter list", jibreelIndex != -1)
        assertTrue("Tablawi must exist in the reciter list", tablawiIndex != -1)
        assertTrue("Husary must exist in the reciter list", firstHusaryIndex != -1)

        assertTrue("Jibreel should be positioned after Minshawi", jibreelIndex > lastMinshawiIndex)
        assertTrue("Tablawi should be positioned after Jibreel", tablawiIndex > jibreelIndex)
        assertTrue("Husary should be positioned after Tablawi", firstHusaryIndex > tablawiIndex)
    }
}
