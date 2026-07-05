package app.eob.me

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LauncherIconTest {
    @Test
    fun manifestUsesCustomLauncherIcon() {
        val manifest = readResFile("../AndroidManifest.xml")
        assertTrue(manifest.contains("android:icon=\"@mipmap/ic_launcher\""))
        assertTrue(manifest.contains("android:roundIcon=\"@mipmap/ic_launcher_round\""))
    }

    @Test
    fun adaptiveLauncherReferencesEobmeLogo() {
        val adaptiveIcon = readResFile("mipmap-anydpi-v26/ic_launcher.xml")
        val adaptiveRound = readResFile("mipmap-anydpi-v26/ic_launcher_round.xml")
        assertTrue(adaptiveIcon.contains("@drawable/eobmelogo"))
        assertTrue(adaptiveRound.contains("@drawable/eobmelogo"))
        assertTrue(adaptiveIcon.contains("@drawable/ic_launcher_background"))
        assertFalse(adaptiveIcon.contains("ic_launcher_foreground"))
    }

    @Test
    fun eobmeLogoAssetsExistForAllDensities() {
        val resRoot = resolveResRoot()
        assertTrue(File(resRoot, "drawable/eobmelogo.png").isFile)
        assertTrue(File(resRoot, "mipmap-anydpi-v26/eobmelogo.png").isFile)
        listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi").forEach { density ->
            assertTrue(
                "Missing mipmap-$density/ic_launcher.png",
                File(resRoot, "mipmap-$density/ic_launcher.png").isFile
            )
            assertTrue(
                "Missing mipmap-$density/ic_launcher_round.png",
                File(resRoot, "mipmap-$density/ic_launcher_round.png").isFile
            )
        }
    }

    @Test
    fun defaultAndroidRobotLauncherAssetsRemoved() {
        val resRoot = resolveResRoot()
        assertFalse(File(resRoot, "drawable/ic_launcher_foreground.xml").exists())
        listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi").forEach { density ->
            assertFalse(File(resRoot, "mipmap-$density/ic_launcher.webp").exists())
            assertFalse(File(resRoot, "mipmap-$density/ic_launcher_round.webp").exists())
        }
    }

    @Test
    fun launcherBackgroundUsesEobmeBlueGradient() {
        val background = readResFile("drawable/ic_launcher_background.xml")
        assertTrue(background.contains("#2498EA"))
        assertTrue(background.contains("#0E45BE"))
        assertFalse(background.contains("#3DDC84"))
    }

    private fun resolveResRoot(): File {
        val candidates = listOf(
            File("src/main/res"),
            File("app/src/main/res")
        )
        return candidates.first { File(it, "mipmap-anydpi-v26/ic_launcher.xml").isFile }
    }

    private fun readResFile(relativePath: String): String {
        val file = File(resolveResRoot(), relativePath)
        assertTrue("Missing resource file: $relativePath", file.isFile)
        return file.readText()
    }
}
