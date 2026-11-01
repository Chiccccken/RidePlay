package com.shilapi.xcertplay

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29, 33])
class KtmMetadataTransformerTest {
    @Test fun hanIsConvertedAndLatinPunctuationRemainUnchanged() {
        assertEquals("君の名は", KtmMetadataTransformer.convert("君の名は"))
        assertEquals("Nv", KtmMetadataTransformer.convert("女"))
        assertEquals("Chong Qing Yin Yue", KtmMetadataTransformer.convert("重庆音乐"))
        assertEquals("Shan Yi Chun", KtmMetadataTransformer.convert("单依纯"))
        assertEquals("Qing Tian", KtmMetadataTransformer.convert("晴天"))
        assertEquals("Zhou Jie Lun", KtmMetadataTransformer.convert("周杰伦"))
        assertEquals("Love Qing Tian 2026!", KtmMetadataTransformer.convert("Love晴天2026!"))
        assertEquals("Beyoncé – LIVE / 2026 🎵", KtmMetadataTransformer.convert("Beyoncé – LIVE / 2026 🎵"))
        assertNull(KtmMetadataTransformer.convert(null))
        assertEquals("", KtmMetadataTransformer.convert(""))
        assertFalse(AirPlayPersistence.loadPinyinMetadata(RuntimeEnvironment.getApplication()))
    }
    @Test @Config(sdk = [28]) fun olderAndroidKeepsTheOriginal() {
        assertEquals("晴天", KtmMetadataTransformer.convert("晴天"))
    }
}
