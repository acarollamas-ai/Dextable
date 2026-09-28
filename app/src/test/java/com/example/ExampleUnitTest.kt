package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.core.packages.PackageManagerEngine
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {

  @Test
  fun testAptInstallUpgradeHandledGracefully() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val pkgEngine = PackageManagerEngine(context)

    // User runs 'apt install upgrade'
    val result = pkgEngine.executeAptCommand(listOf("install", "upgrade"))
    assertTrue(result.success)
    assertTrue(result.output.contains("apt upgrade") || result.output.contains("atualizado"))
  }

  @Test
  fun testAptUpdateAndAptUpgrade() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val pkgEngine = PackageManagerEngine(context)

    val updateRes = pkgEngine.executeAptCommand(listOf("update"))
    assertTrue(updateRes.success)
    assertTrue(updateRes.output.contains("Reading package lists") || updateRes.output.contains("Hit:"))

    val upgradeRes = pkgEngine.executeAptCommand(listOf("upgrade"))
    assertTrue(upgradeRes.success)
    assertTrue(upgradeRes.output.contains("Reading package lists"))
  }

  @Test
  fun testAptInstallPackageAndList() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val pkgEngine = PackageManagerEngine(context)

    val installRes = pkgEngine.executeAptCommand(listOf("install", "curl", "-y"))
    assertTrue(installRes.success)

    val listRes = pkgEngine.executeAptCommand(listOf("list", "--installed"))
    assertTrue(listRes.success)
    assertTrue(listRes.output.contains("curl"))
  }

  @Test
  fun testAptCacheCommands() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val pkgEngine = PackageManagerEngine(context)

    val searchRes = pkgEngine.handleAptCacheCommand(listOf("search", "python"))
    assertTrue(searchRes.success)
    assertTrue(searchRes.output.contains("python3"))
  }
}
