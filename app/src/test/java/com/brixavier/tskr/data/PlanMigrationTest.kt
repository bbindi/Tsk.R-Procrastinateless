package com.brixavier.tskr.data

import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Test
import java.lang.reflect.Proxy
import java.sql.DriverManager

class PlanMigrationTest {

    @Test
    fun testRealMigration_12_to_13_executesSqlDirectlyOnRealSqLiteAndMigratesData() {
        // 1. Create a real in-memory SQLite database connection via JDBC
        val conn = DriverManager.getConnection("jdbc:sqlite::memory:")
        val stmt = conn.createStatement()

        // 2. Create Schema Version 12 Plans table
        stmt.execute("""
            CREATE TABLE `plans` (
                `id` TEXT NOT NULL,
                `name` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `icon` TEXT NOT NULL,
                `deadline` TEXT,
                `status` TEXT NOT NULL,
                `createdAtMs` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            );
        """.trimIndent())

        // 3. Insert representative legacy Plan rows
        val legacyPlans = listOf(
            Triple("p1", "Work Project", "💼"),
            Triple("p2", "Personal Goals", "🎯"),
            Triple("p3", "Tax Budget", "💰"),
            Triple("p4", "Home Repair", "🏠"),
            Triple("p5", "Ghana Trip", "✈️"),
            Triple("p6", "Birthday Event", "🎉"),
            Triple("p7", "Bible Study", "📖"),
            Triple("p8", "Art Design", "🎨"),
            Triple("p9", "Grocery List", "🛒"),
            Triple("p10", "Gym Workout", "🏋️"),
            Triple("p11", "Family Gathering", "👨‍👩‍👧‍👦"),
            Triple("p12", "Code Release", "📁"),
            Triple("p13", "Weekly Roadmap", "📅"),
            Triple("p14", "Blank Icon Plan", ""),
            Triple("p15", "Unknown Emoji Plan", "👽")
        )

        val nowMs = 1700000000000L
        val pstmt = conn.prepareStatement("""
            INSERT INTO `plans` (`id`, `name`, `description`, `icon`, `deadline`, `status`, `createdAtMs`)
            VALUES (?, ?, 'Preserved description', ?, '2026-12-31', 'ACTIVE', $nowMs)
        """.trimIndent())

        legacyPlans.forEach { (id, name, icon) ->
            pstmt.setString(1, id)
            pstmt.setString(2, name)
            pstmt.setString(3, icon)
            pstmt.executeUpdate()
        }
        pstmt.close()

        // 4. Create a SupportSQLiteDatabase Proxy that delegates execSQL calls directly to the real SQLite connection
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

        // 5. EXECUTE THE REAL PRODUCTION MIGRATION_12_13 OBJECT
        MIGRATION_12_13.migrate(dbProxy)

        // 6. VERIFY MIGRATED SCHEMA AND DATA IN REAL SQLITE
        val resultSet = stmt.executeQuery("SELECT id, name, description, icon, deadline, status, createdAtMs, category, categorySource FROM plans")

        val planMap = mutableMapOf<String, Map<String, Any?>>()
        while (resultSet.next()) {
            val id = resultSet.getString("id")
            planMap[id] = mapOf(
                "name" to resultSet.getString("name"),
                "desc" to resultSet.getString("description"),
                "icon" to resultSet.getString("icon"),
                "deadline" to resultSet.getString("deadline"),
                "status" to resultSet.getString("status"),
                "createdAtMs" to resultSet.getLong("createdAtMs"),
                "category" to resultSet.getString("category"),
                "categorySource" to resultSet.getString("categorySource")
            )
        }
        resultSet.close()
        stmt.close()
        conn.close()

        assertEquals(15, planMap.size)

        // Assert exact mapped categories
        assertEquals("WORK", planMap["p1"]?.get("category"))
        assertEquals("GOALS", planMap["p2"]?.get("category"))
        assertEquals("FINANCE", planMap["p3"]?.get("category"))
        assertEquals("HOME", planMap["p4"]?.get("category"))
        assertEquals("TRAVEL", planMap["p5"]?.get("category"))
        assertEquals("EVENTS", planMap["p6"]?.get("category"))
        assertEquals("SPIRITUAL", planMap["p7"]?.get("category"))
        assertEquals("CREATIVE", planMap["p8"]?.get("category"))
        assertEquals("SHOPPING", planMap["p9"]?.get("category"))
        assertEquals("HEALTH", planMap["p10"]?.get("category"))
        assertEquals("FAMILY", planMap["p11"]?.get("category"))
        assertEquals("PROJECTS", planMap["p12"]?.get("category"))
        assertEquals("PLANNING", planMap["p13"]?.get("category"))
        assertEquals("PERSONAL", planMap["p14"]?.get("category"))
        assertEquals("PERSONAL", planMap["p15"]?.get("category"))

        // Assert all migrated plans have categorySource = AUTOMATIC
        planMap.values.forEach { row ->
            assertEquals("AUTOMATIC", row["categorySource"])
        }

        // Assert existing fields remain 100% unchanged
        val sample = planMap["p1"]
        assertEquals("Work Project", sample?.get("name"))
        assertEquals("Preserved description", sample?.get("desc"))
        assertEquals("💼", sample?.get("icon"))
        assertEquals("2026-12-31", sample?.get("deadline"))
        assertEquals("ACTIVE", sample?.get("status"))
        assertEquals(nowMs, sample?.get("createdAtMs"))
    }
}
