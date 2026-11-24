package com.kingzcheung.xime.settings

import org.junit.Assert.*
import org.junit.Test

class DirectLanguageKeyTargetTest {
    @Test fun currentToEnglishTapMatchesGeneralResolverWithoutChangingEngineSchema() {
        val mode = LanguageSwitchMode.CURRENT_ENGLISH
        for (current in listOf("rime_ice", "t9_pinyin", "double_pinyin_flypy")) {
            for (selected in listOf(null, InputModes.ENGLISH)) {
                val resolved = EnglishSchemes.directKeyTarget(mode, InputLanguage.CHINESE, current, null, selected)
                assertEquals(EnglishSchemes.target(listOf(InputModes.english), selected, current, null), resolved)
                assertEquals(EnglishSchemes.Target(current, true), resolved)
            }
        }
    }

    @Test fun otherSwitchesStillResolveTheirActualLanguageAndProfile() {
        fun resolve(mode: LanguageSwitchMode? = LanguageSwitchMode.CURRENT_ENGLISH, from: InputLanguage = InputLanguage.CHINESE,
            schema: String = "rime_ice", field: String? = null, selected: String? = null) =
            EnglishSchemes.directKeyTarget(mode, from, schema, field, selected)
        assertNull(resolve(mode = null))
        assertNull(resolve(mode = LanguageSwitchMode.CYCLE))
        assertNull(resolve(mode = LanguageSwitchMode.SPECIFIC_PAIR))
        assertNull(resolve(from = InputLanguage.ENGLISH))
        assertNull(resolve(schema = ""))
        assertNull(resolve(field = "rime_ice"))
        assertNull(resolve(selected = InputModes.ENGLISH_WORDS))
        assertNull(resolve(selected = "user_imported_english_schema"))
    }
}
