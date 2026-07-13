package app.eob.me

import androidx.compose.ui.graphics.Color
import app.eob.me.data.CptCategory
import app.eob.me.data.EobKnowledgeBase
import app.eob.me.ui.screens.categoryThemeColor
import org.junit.Assert.assertEquals
import org.junit.Test

class CptCategoryColorTest {
    @Test
    fun categoryThemeColorsMatchMedicalCodePalette() {
        assertEquals(Color(0xFF03A9F4), categoryThemeColor(CptCategory.OfficeVisit))
        assertEquals(Color(0xFF43A047), categoryThemeColor(CptCategory.Lab))
        assertEquals(Color(0xFFE53935), categoryThemeColor(CptCategory.Hospital))
        assertEquals(Color(0xFFB8C0CC), categoryThemeColor(CptCategory.XRay))
        assertEquals(Color(0xFF212121), categoryThemeColor(CptCategory.Dme))
        assertEquals(Color(0xFFFFC107), categoryThemeColor(CptCategory.Injection))
        assertEquals(Color(0xFF8E24AA), categoryThemeColor(CptCategory.Other))
    }

    @Test
    fun knowledgeBaseMapsUserExampleCodesToExpectedCategories() {
        assertEquals(CptCategory.OfficeVisit, EobKnowledgeBase.cptInfoFor("99213").category)
        assertEquals(CptCategory.OfficeVisit, EobKnowledgeBase.cptInfoFor("99214").category)
        assertEquals(CptCategory.OfficeVisit, EobKnowledgeBase.cptInfoFor("99385").category)
        assertEquals(CptCategory.Lab, EobKnowledgeBase.cptInfoFor("81004").category)
        assertEquals(CptCategory.Lab, EobKnowledgeBase.cptInfoFor("80035").category)
        assertEquals(CptCategory.Hospital, EobKnowledgeBase.cptInfoFor("99221").category)
        assertEquals(CptCategory.Hospital, EobKnowledgeBase.cptInfoFor("99222").category)
        assertEquals(CptCategory.Hospital, EobKnowledgeBase.cptInfoFor("99223").category)
        assertEquals(CptCategory.XRay, EobKnowledgeBase.cptInfoFor("71046").category)
        assertEquals(CptCategory.XRay, EobKnowledgeBase.cptInfoFor("73562").category)
        assertEquals(CptCategory.XRay, EobKnowledgeBase.cptInfoFor("74177").category)
        assertEquals(CptCategory.Injection, EobKnowledgeBase.cptInfoFor("J3420").category)
        assertEquals(CptCategory.Injection, EobKnowledgeBase.cptInfoFor("J0081").category)
        assertEquals(CptCategory.Injection, EobKnowledgeBase.cptInfoFor("J0013").category)
    }

    @Test
    fun unknownInjectionAndDentalCodesInferCategoryFromPrefix() {
        assertEquals(CptCategory.Injection, EobKnowledgeBase.cptInfoFor("J9999").category)
        assertEquals(CptCategory.XRay, EobKnowledgeBase.cptInfoFor("79999").category)
        assertEquals(CptCategory.Dme, EobKnowledgeBase.cptInfoFor("A9999").category)
        assertEquals(CptCategory.Other, EobKnowledgeBase.cptInfoFor("D5225").category)
    }
}
