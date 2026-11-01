package com.shilapi.xcertplay

import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ArtistNameOverridesTest {
    private val app get() = RuntimeEnvironment.getApplication()
    @After fun clean() = ArtistNameOverrides.save(app, emptyMap())
    @Test fun wholeNameReplacementPersistsAndDoesNotRunThroughPinyinAgain() {
        val rules = mapOf("想想XiangXiang" to "Xiang Xiang", "周杰伦" to "Jay Chou")
        ArtistNameOverrides.save(app, rules)
        assertEquals(rules, ArtistNameOverrides.load(app))
        assertEquals("Xiang Xiang", ArtistNameOverrides.displayed("想想XiangXiang", ArtistNameOverrides.load(app)))
        assertEquals("Jay Chou", ArtistNameOverrides.displayed("周杰伦", rules))
        assertEquals("Xiang Xiang Live", ArtistNameOverrides.displayed("想想 Live", rules))
        assertNull(ArtistNameOverrides.displayed(null, rules))
        ArtistNameOverrides.save(app, emptyMap())
        assertEquals("Zhou Jie Lun", ArtistNameOverrides.displayed("周杰伦", ArtistNameOverrides.load(app)))
    }
    @Test(expected = IllegalArgumentException::class) fun emptyDisplayNamesAreNotStored() {
        ArtistNameOverrides.save(app, mapOf("Artist" to ""))
    }
}
