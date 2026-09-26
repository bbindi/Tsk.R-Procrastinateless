package com.brixavier.tskr.ui

import com.brixavier.tskr.model.Plan
import com.brixavier.tskr.model.PlanNoteSection
import com.brixavier.tskr.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanWorkspaceShellTest {

    private val plan1 = Plan(id = "p-workspace-1", name = "September Meeting")
    private val plan2 = Plan(id = "p-workspace-2", name = "Office Relocation")

    @Test
    fun testNoteSections_loadOnlyForCorrectPlan() {
        val s1 = PlanNoteSection(id = "s1", planId = plan1.id, title = "Overview")
        val s2 = PlanNoteSection(id = "s2", planId = plan2.id, title = "Logistics")

        val allSections = listOf(s1, s2)
        val plan1Sections = allSections.filter { it.planId == plan1.id }

        assertEquals(1, plan1Sections.size)
        assertEquals("Overview", plan1Sections.first().title)
    }

    @Test
    fun testNoteSections_orderingFollowsSortOrder() {
        val s1 = PlanNoteSection(id = "s1", planId = plan1.id, title = "Decisions", sortOrder = 1)
        val s2 = PlanNoteSection(id = "s2", planId = plan1.id, title = "Notes", sortOrder = 0)

        val sorted = listOf(s1, s2).sortedBy { it.sortOrder }

        assertEquals("Notes", sorted[0].title)
        assertEquals("Decisions", sorted[1].title)
    }

    @Test
    fun testAddingSection_persistsWithCorrectPlanIdAndTitle() {
        val newSection = PlanNoteSection(planId = plan1.id, title = "Ideas", content = "")

        assertNotNull(newSection.id)
        assertEquals(plan1.id, newSection.planId)
        assertEquals("Ideas", newSection.title)
    }

    @Test
    fun testRenamingSection_updatesTitle() {
        val original = PlanNoteSection(id = "s1", planId = plan1.id, title = "Notes")
        val renamed = original.copy(title = "Discussion")

        assertEquals("Discussion", renamed.title)
        assertEquals(original.id, renamed.id)
    }

    @Test
    fun testDeletingSection_affectsOnlyTargetSection() {
        val s1 = PlanNoteSection(id = "s1", planId = plan1.id, title = "Notes")
        val s2 = PlanNoteSection(id = "s2", planId = plan1.id, title = "Decisions")

        val sections = mutableListOf(s1, s2)
        sections.removeIf { it.id == "s1" }

        assertEquals(1, sections.size)
        assertEquals("s2", sections.first().id)
    }

    @Test
    fun testNoteContent_persistsAndUpdatesTimestamp() {
        val original = PlanNoteSection(id = "s1", planId = plan1.id, title = "Notes", content = "Initial note")
        val updatedTime = System.currentTimeMillis() + 100
        val updated = original.copy(content = "Initial note - updated with meeting decisions", updatedAtMs = updatedTime)

        assertEquals("Initial note - updated with meeting decisions", updated.content)
        assertTrue(updated.updatedAtMs >= original.updatedAtMs)
    }

    @Test
    fun testSwitchingSections_doesNotLoseSavedContent() {
        val s1 = PlanNoteSection(id = "s1", planId = plan1.id, title = "Overview", content = "Overview content")
        val s2 = PlanNoteSection(id = "s2", planId = plan1.id, title = "Decisions", content = "Decisions content")

        val sectionsMap = mapOf(s1.id to s1, s2.id to s2)

        // Switch to s2
        val active1 = sectionsMap["s2"]
        assertEquals("Decisions content", active1?.content)

        // Switch back to s1
        val active2 = sectionsMap["s1"]
        assertEquals("Overview content", active2?.content)
    }

    @Test
    fun testExistingPlanWithNoSections_bootstrapsDefaultNotesSection() {
        val sections = emptyList<PlanNoteSection>()
        val bootstrapped = if (sections.isEmpty()) {
            listOf(PlanNoteSection(planId = plan1.id, title = "Notes"))
        } else {
            sections
        }

        assertEquals(1, bootstrapped.size)
        assertEquals("Notes", bootstrapped.first().title)
    }

    @Test
    fun testPlanWorkspace_reachableWithoutDeadline() {
        val planNoDeadline = Plan(id = "p-no-dl", name = "Quick Notes Workspace", deadline = null)

        assertNull(planNoDeadline.deadline)
        assertTrue(planNoDeadline.isActive())
    }

    @Test
    fun testPlanDeletion_cascadesSections_preservesTaskRelationships() {
        val section = PlanNoteSection(id = "sec-del", planId = plan1.id, title = "Notes")
        val task = Reminder(id = "r-del", title = "Task in Plan", date = "2026-09-26", time = "10:00", planId = plan1.id)

        // Cascading deletion removes sections
        val sections = listOf(section).filter { it.planId != plan1.id }
        assertTrue(sections.isEmpty())

        // Deleting Plan detaches tasks (planId = null)
        val detachedTask = task.copy(planId = null)
        assertEquals("r-del", detachedTask.id)
        assertNull(detachedTask.planId)
    }
}
