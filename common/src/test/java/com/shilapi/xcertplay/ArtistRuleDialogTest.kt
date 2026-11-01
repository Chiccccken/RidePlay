package com.shilapi.xcertplay

import android.os.Looper
import org.robolectric.Shadows.shadowOf
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import java.io.File
import java.util.Locale
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowAlertDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], qualifiers = "xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ArtistRuleDialogTest {
    private fun views(v: View): Sequence<View> = sequence {
        yield(v)
        if (v is ViewGroup) for (i in 0 until v.childCount) yieldAll(views(v.getChildAt(i)))
    }
    private fun open(activity: DiPlayActivity, key: String?, changed: () -> Unit): AlertDialog {
        DiPlayActivity::class.java.getDeclaredMethod("editArtistRule", String::class.java, kotlin.jvm.functions.Function0::class.java)
            .apply { isAccessible = true }.invoke(activity, key, changed)
        shadowOf(Looper.getMainLooper()).idle()
        return ShadowAlertDialog.getLatestAlertDialog()
    }
    @Test fun addEditDeleteAndEmptyOriginalValidationUseTheRealDialog() {
        val controller = Robolectric.buildActivity(DiPlayActivity::class.java, Intent().putExtra("page", "settings")).setup()
        val activity = controller.get()
        var count = 0
        try {
            val first = open(activity, null) { count++ }
            first.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            assertTrue(first.isShowing)
            assertTrue(ArtistNameOverrides.load(activity).isEmpty())
            val fields = views(first.window!!.decorView).filterIsInstance<EditText>().toList()
            fields[0].setText("想想XiangXiang"); fields[1].setText("Xiang Xiang")
            first.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            assertEquals("Xiang Xiang", ArtistNameOverrides.load(activity)["想想XiangXiang"])
            val second = open(activity, "想想XiangXiang") { count++ }
            val edited = views(second.window!!.decorView).filterIsInstance<EditText>().toList()
            assertFalse(edited[0].isEnabled)
            edited[1].setText("想想")
            second.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            assertEquals("想想", ArtistNameOverrides.load(activity)["想想XiangXiang"])
            val third = open(activity, "想想XiangXiang") { count++ }
            views(third.window!!.decorView).filterIsInstance<EditText>().last().setText("")
            third.getButton(AlertDialog.BUTTON_POSITIVE).performClick()
            assertTrue(ArtistNameOverrides.load(activity).isEmpty())
            assertEquals(3, count)
        } finally {
            ShadowAlertDialog.getLatestAlertDialog()?.dismiss()
            ArtistNameOverrides.save(activity, emptyMap())
            controller.pause().stop().destroy()
        }
    }
    @Test fun editorScreenshotsAtLargeFontInCompactFullAndRtlLayouts() {
        val controller = Robolectric.buildActivity(DiPlayActivity::class.java, Intent().putExtra("page", "settings")).setup()
        val activity = controller.get()
        try {
            for ((name, widthDp, locale) in listOf(
                Triple("editor-compact-zh", 320, "zh-CN"), Triple("editor-full-en", 960, "en-US"),
                Triple("editor-compact-ar", 320, "ar-SA"))) {
                val language = Locale.forLanguageTag(locale)
                val config = Configuration(activity.resources.configuration).apply {
                    fontScale = 1.5f; setLocale(language); setLayoutDirection(language)
                }
                @Suppress("DEPRECATION")
                activity.resources.updateConfiguration(config, activity.resources.displayMetrics)
                val dialog = open(activity, null) {}
                val decor = dialog.window!!.decorView
                val density = activity.resources.displayMetrics.density
                val width = (widthDp * density).toInt()
                decor.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec((600 * density).toInt(), View.MeasureSpec.AT_MOST))
                decor.layout(0, 0, width, decor.measuredHeight)
                val bitmap = Bitmap.createBitmap(width, decor.measuredHeight, Bitmap.Config.ARGB_8888)
                try {
                    decor.draw(Canvas(bitmap))
                    val dir = File("build/reports/pinyin-metadata-ui").apply { mkdirs() }
                    File(dir, "$name.png").outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
                } finally { bitmap.recycle(); dialog.dismiss() }
            }
        } finally { controller.pause().stop().destroy() }
    }
}
