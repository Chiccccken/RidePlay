package com.shilapi.xcertplay

import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import com.shilapi.xcertplay.host.R
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class RidePlayOverviewShortcutsTest {
    private val screen = Robolectric.buildActivity(DiPlayActivity::class.java).get()
    @After fun cleanup() { RidePlayOrientation.save(screen, RidePlayOrientation.Mode.DEFAULT); screen.finish() }
    private fun views(view: View): Sequence<View> = sequence {
        yield(view)
        if (view is ViewGroup) for (i in 0 until view.childCount) yieldAll(views(view.getChildAt(i)))
    }
    private fun quick(): LinearLayout = ReflectionHelpers.callInstanceMethod(screen, "quickSettingsCard")
    private fun direction(root: View): Button = views(root).filterIsInstance<Button>().single {
        it.text.toString().startsWith(screen.getString(R.string.settings_orientation_lock) + " · ")
    }
    @Test fun overviewAndDisplayUseTheSameOrientationPreference() {
        for ((mode, label) in listOf(
            RidePlayOrientation.Mode.PORTRAIT to R.string.settings_orientation_portrait,
            RidePlayOrientation.Mode.LANDSCAPE to R.string.settings_orientation_landscape,
        )) {
            RidePlayOrientation.save(screen, mode)
            val full = LinearLayout(screen)
            ReflectionHelpers.callInstanceMethod<Unit>(screen, "orientationLockControls",
                ReflectionHelpers.ClassParameter.from(LinearLayout::class.java, full))
            assertEquals(direction(full).text.toString(), direction(quick()).text.toString())
            assertTrue(direction(quick()).text.toString().endsWith(screen.getString(label)))
        }
    }
    @Test fun overviewRetainsFourQuickControls() {
        val all = views(quick()).toList()
        assertEquals(4, all.count { it is Button || it is Switch })
        assertNotNull(direction(quick()))
    }
    @Test fun vehicleCategoryRetainsItsGenericIcon() {
        val icon = ReflectionHelpers.callInstanceMethod<Int>(screen, "settingsCategoryIcon",
            ReflectionHelpers.ClassParameter.from(SettingsCategory::class.java, SettingsCategory.VEHICLE))
        assertEquals(R.drawable.ic_dp_car, icon)
        val card = ReflectionHelpers.callInstanceMethod<LinearLayout>(screen, "settingsSummaryCard",
            ReflectionHelpers.ClassParameter.from(SettingsCategory::class.java, SettingsCategory.VEHICLE),
            ReflectionHelpers.ClassParameter.from(String::class.java, "KTM"))
        val image = views(card).filterIsInstance<ImageView>().single()
        assertNotNull(image.drawable)
        assertNotNull(image.imageTintList)
    }
}
