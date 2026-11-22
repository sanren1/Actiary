package com.actiary.data.appearance

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLanguageTest {
    @Test
    fun fromStorageParsesKnownValuesAndFallsBackToSystem() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromStorage(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromStorage(""))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromStorage("UNKNOWN"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromStorage("SYSTEM"))
        assertEquals(AppLanguage.ZH, AppLanguage.fromStorage("ZH"))
        assertEquals(AppLanguage.EN, AppLanguage.fromStorage("EN"))
    }

    @Test
    fun languageTagsMatchExpectedLocales() {
        assertNull(AppLanguage.SYSTEM.languageTag)
        assertEquals("zh-CN", AppLanguage.ZH.languageTag)
        assertEquals("en", AppLanguage.EN.languageTag)
    }
}
