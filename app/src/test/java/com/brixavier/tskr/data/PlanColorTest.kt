package com.brixavier.tskr.data

import androidx.sqlite.db.SupportSQLiteDatabase
import com.brixavier.tskr.model.ColorSource
import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.model.PlanCategory
import com.brixavier.tskr.model.PlanCategoryColorMapping
import com.brixavier.tskr.model.PlanColor
import com.brixavier.tskr.ui.PlanColorResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.sql.DriverManager
import java.lang.reflect.Proxy

class PlanColorTest {

    @Test
    fun testEveryCategory_hasDeterministicDefaultColor() {
        for (category in PlanCategory.entries) {
            val color = PlanCategoryColorMapping.getDefaultColor(category)
            assertNotNull(color)
        }
    }

    @Test
    fun testPlanColor_parsingAndFallbacks() {
        assertEquals(PlanColor.BLUE, PlanColor.fromString("BLUE"))
        assertEquals(PlanColor.GREEN, PlanColor.fromString("green"))
        assertEquals(PlanColor.BLUE, PlanColor.fromString(null))
        assertEquals(PlanColor.BLUE, PlanColor.fromString(""))
        assertEquals(PlanColor.BLUE, PlanColor.fromString("UNKNOWN_COLOR"))
    }

    @Test
    fun testColorSource_parsingAndFallbacks() {
        assertEquals(ColorSource.CATEGORY, ColorSource.fromString("CATEGORY"))
        assertEquals(ColorSource.MANUAL, ColorSource.fromString("manual"))
        assertEquals(ColorSource.CATEGORY, ColorSource.fromString(null))
        assertEquals(ColorSource.CATEGORY, ColorSource.fromString(""))
        assertEquals(ColorSource.CATEGORY, ColorSource.fromString("UNKNOWN_SOURCE"))
    }

    @Test
    fun testEffectiveColorResolution_categorySource_followsCategory() {
        var plan = Plan(
            name = "Project Alpha",
            category = "WORK",
            categorySource = "AUTOMATIC",
            color = "SLATE",
            colorSource = "CATEGORY"
        )

        assertEquals(PlanColor.BLUE, plan.resolveEffectiveColor())

        // Change category -> effective color should update because colorSource == CATEGORY
        plan = plan.copy(category = "FINANCE")
        assertEquals(PlanColor.TEAL, plan.resolveEffectiveColor())
    }

    @Test
    fun testEffectiveColorResolution_manualSource_preservesManualColor() {
        val plan = Plan(
            name = "Project Beta",
            category = "WORK",
            categorySource = "MANUAL",
            color = "ROSE",
            colorSource = "MANUAL"
        )

        // Manual color is preserved even if category changes
        assertEquals(PlanColor.ROSE, plan.resolveEffectiveColor())

        val updatedPlan = plan.copy(category = "FINANCE")
        assertEquals(PlanColor.ROSE, updatedPlan.resolveEffectiveColor())
    }

    @Test
    fun testColorResolver_resolvesLightAndDarkThemes() {
        for (color in PlanColor.entries) {
            val lightColor = PlanColorResolver.resolve(color, isDark = false)
            val darkColor = PlanColorResolver.resolve(color, isDark = true)
            assertNotNull(lightColor)
            assertNotNull(darkColor)
        }
    }

    @Test
    fun testRealMigration_13_to_14_addsColorColumnsSuccessfully() {
        val conn = DriverManager.getConnection("jdbc:sqlite::memory:")
        val stmt = conn.createStatement()

        // Create Schema Version 13 Plans table
        stmt.execute("""
            CREATE TABLE `plans` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `icon` TEXT NOT NULL,
                `deadline` TEXT,
                `status` TEXT NOT NULL,
                `createdAtMs` INTEGER NOT NULL,
                `category` TEXT NOT NULL DEFAULT 'PERSONAL',
                `categorySource` TEXT NOT NULL DEFAULT 'AUTOMATIC',
                PRIMARY KEY(`id`)
            );
        """.trimIndent())

        stmt.execute("INSERT INTO `plans` (`id`, `name`, `description`, `icon`, `deadline`, `status`, `createdAtMs`, `category`, `categorySource`) VALUES ('p1', 'Work Plan', 'Desc', 'WORK', NULL, 'ACTIVE', 1700000000000, 'WORK', 'AUTOMATIC')")

        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                val sql = args[0] as String
                val s = conn.createStatement()
                s.execute(sql)
                s.close()
                null
            } else {
                null
            }
        } as SupportSQLiteDatabase

        // Execute MIGRATION_13_14
        MIGRATION_13_14.migrate(dbProxy)

        val rs = stmt.executeQuery("SELECT color, colorSource, name, category FROM plans WHERE id = 'p1'")
        assertTrue(rs.next())
        assertEquals("SLATE", rs.getString("color"))
        assertEquals("CATEGORY", rs.getString("colorSource"))
        assertEquals("Work Plan", rs.getString("name"))
        assertEquals("WORK", rs.getString("category"))

        rs.close()
        stmt.close()
        conn.close()
    }
}
