package com.brixavier.tskr.personality

object MessagePools {
    const val FALLBACK_MESSAGE = "Stay focused."

    const val ONBOARDING = "onboarding"
    const val TASK_CREATED = "task_created"
    const val TASK_CREATED_LATE_NIGHT = "task_created_late_night"
    const val TASK_UPDATED = "task_updated"
    const val TASK_DELETED = "task_deleted"
    const val TASK_RESTORED = "task_restored"
    const val TASK_APPROACHING = "task_approaching"
    const val TASK_DUE_NORMAL = "task_due_normal"
    const val TASK_DUE_IMPORTANT = "task_due_important"
    const val TASK_OVERDUE_EARLY = "task_overdue_early"
    const val TASK_OVERDUE_LATE = "task_overdue_late"
    const val TASK_SNOOZED = "task_snoozed"
    const val TASK_SNOOZED_REPEATED = "task_snoozed_repeated"
    const val TASK_SNOOZED_REPEATED_HIGH = "task_snoozed_repeated_high"
    const val COMPLETION_NORMAL = "completion_normal"
    const val COMPLETION_EARLY = "completion_early"
    const val COMPLETION_RECURRING = "completion_recurring"
    const val COMPLETION_STREAK = "completion_streak"
    const val EMPTY_STATE_NO_TASKS = "empty_state_no_tasks"
    const val EMPTY_STATE_ALL_CAUGHT_UP = "empty_state_all_caught_up"
    const val MISSION_MODE_WARNING = "mission_mode_warning"
    const val MISSION_MODE_WARNING_PLAN = "mission_mode_warning_plan"
    const val MISSION_MODE_DUE = "mission_mode_due"
    const val MISSION_MODE_DUE_OVERDUE = "mission_mode_due_overdue"
    const val MISSION_MODE_DUE_SNOOZED = "mission_mode_due_snoozed"
    const val MISSION_MODE_DUE_SNOOZED_REPEATED = "mission_mode_due_snoozed_repeated"
    const val MISSION_MODE_DUE_RECURRING = "mission_mode_due_recurring"
    const val MISSION_MODE_DUE_PLAN = "mission_mode_due_plan"
    const val MISSION_MODE_COMPLETION = "mission_mode_completion"
    const val MISSION_MODE_COMPLETION_EARLY = "mission_mode_completion_early"
    const val MISSION_MODE_COMPLETION_STREAK = "mission_mode_completion_streak"
    const val MISSION_MODE_COMPLETION_RECURRING = "mission_mode_completion_recurring"
    const val WIDGET = "widget"
    const val WIDGET_OVERDUE = "widget_overdue"
    const val WIDGET_OVERDUE_LATE = "widget_overdue_late"

    const val PLAN_CREATED = "plan_created"
    const val PLAN_NO_TASKS = "plan_no_tasks"
    const val PLAN_PROGRESS_START = "plan_progress_start"
    const val PLAN_PROGRESS_MID = "plan_progress_mid"
    const val PLAN_PROGRESS_COMPLETE = "plan_progress_complete"
    const val PLAN_DEADLINE_APPROACHING = "plan_deadline_approaching"
    const val PLAN_DEADLINE_TODAY = "plan_deadline_today"
    const val PLAN_DEADLINE_PASSED = "plan_deadline_passed"
    const val PLAN_COMPLETED = "plan_completed"
    const val PLAN_ARCHIVED = "plan_archived"
    const val PLAN_REOPENED = "plan_reopened"

    private val allPools = mapOf(
        ONBOARDING to listOf(
            "The app that cares about your schedule more than you do. Possibly too much.",
            "Your tasks are waiting. They aren't angry, just... disappointed.",
            "Procrastination ends here. Or at least, it gets much noisier.",
            "We will remind you. We will find you. And we will notify you.",
            "Helping you remember things you'll probably still forget. But elegantly."
        ),
        TASK_CREATED to listOf(
            "Another task added. Future-you is already sighing.",
            "Successfully scheduled. Now comes the hard part: actually doing it.",
            "Task saved. I'll remember it, but will you?",
            "It is written. You can't un-write it. Well, you can, but I'll judge you.",
            "Great, another thing to nag you about. Let's do this. 🎯",
            "Task locked in. I'm starting the countdown. ⏰",
            "Your optimism in scheduling this is inspiring."
        ),
        TASK_CREATED_LATE_NIGHT to listOf(
            "2 AM? Bold time to start reorganizing your life.",
            "Creating tasks at this hour? I respect the commitment.",
            "It's late. At least future-you has a plan.",
            "Midnight productivity. Interesting strategy. I'll allow it.",
            "Scheduling tasks past midnight. Your optimism is noted."
        ),
        TASK_UPDATED to listOf(
            "Task updated. Modifying history, are we?",
            "I've updated the details. Please don't tell me you're just stalling.",
            "Changes saved. Stick to the new plan, alright?",
            "Got it. Adjusting the nagging parameters.",
            "Updated. Because apparently the first plan wasn't good enough."
        ),
        TASK_DELETED to listOf(
            "Task deleted. Did you actually do it, or just give up? I won't ask.",
            "Erased from existence. Like it never happened.",
            "Task removed. One less thing to worry about.",
            "Poof. It's gone. I'm assuming it was a mistake anyway.",
            "Deleted. Quitting is an option, I suppose. 😏"
        ),
        TASK_RESTORED to listOf(
            "Task restored. Oh, we're doing this again?",
            "It's back! Did you miss it?",
            "Welcome back to the active roster, little task.",
            "Task resurrected. Hope you are ready this time. 🚀",
            "Restored. I knew you couldn't stay away."
        ),
        TASK_APPROACHING to listOf(
            "Just a heads up, this is coming for you soon.",
            "You have time, but don't get too comfortable.",
            "Looming in the distance... a task approaches.",
            "I'm giving you advanced warning. Don't say I didn't tell you.",
            "The scheduled deadline is quietly creeping closer. ⏰",
            "Preparation prevents poor performance. Or so they say.",
            "It's on the horizon. Start mentally preparing."
        ),
        TASK_DUE_NORMAL to listOf(
            "It's time. Don't make me repeat myself.",
            "Your scheduled procrastination break is officially over.",
            "It's due. You can do this. I believe in you. Mildly.",
            "Time's up. Time to actually do the thing.",
            "The moment has arrived. Make it happen. 🎯",
            "A task is due. Surprised? You wrote it down.",
            "The clock is running. Time to check this off."
        ),
        TASK_DUE_IMPORTANT to listOf(
            "CRITICAL: Drop what you're doing. It's time. ⚠️",
            "This is not a drill. I repeat, not a drill.",
            "You marked this as important. Prove it.",
            "Urgent! Do not ignore this. Future-you will be furious.",
            "Maximum priority. Get moving, right now.",
            "I'm waving a giant red flag here. Pay attention! 🚨",
            "If you only do one thing today, make it this."
        ),
        TASK_OVERDUE_EARLY to listOf(
            "You're a little late. But I'll let it slide if you do it now.",
            "Missed the deadline by a bit. Let's pretend it was traffic.",
            "Overdue. Not great, not terrible. Yet.",
            "Fashionably late is for parties, not tasks.",
            "We're officially behind schedule. Pick up the pace.",
            "I noticed you haven't done this yet. Need a nudge?",
            "The schedule has slipped, but your momentum hasn't."
        ),
        TASK_OVERDUE_LATE to listOf(
            "Seriously? This is still sitting here?",
            "I'm starting to think you're deliberately ignoring me.",
            "Way overdue. Time is not on our side here.",
            "This task is gathering dust. Are we doing this or not?",
            "I'm judging you in binary. And it's not a 1. 🤖",
            "Is this task a joke to you? Because I don't get it.",
            "I'd do it for you if I had hands. But I don't. So get to work."
        ),
        TASK_SNOOZED to listOf(
            "Snoozed. I'll be back. I always come back.",
            "Kicking the can down the road, are we?",
            "5 minutes won't save you. But okay. 😅",
            "Snoozing doesn't make it disappear. Nice try though.",
            "Fine, take a breather. But I'm watching you.",
            "Delaying the inevitable. A classic human move.",
            "I'll give you a few minutes. Use them wisely."
        ),
        TASK_SNOOZED_REPEATED to listOf(
            "Again? I noticed. ⏰",
            "That's two snoozes. We're developing a pattern. 😅",
            "Second snooze. Your alarm and I are starting to feel ignored.",
            "Two snoozes in. The task isn't going away on its own.",
            "Another 5 minutes? Bold strategy. Let's see if it pays off."
        ),
        TASK_SNOOZED_REPEATED_HIGH to listOf(
            "Three snoozes? At this point, we're negotiating.",
            "Another snooze. I'm beginning to think 'later' is the actual plan. 😅",
            "Triple snooze detected. Your future self is quietly weeping.",
            "Three times snoozed. The task is practically part of the furniture now.",
            "Snooze streak active. Let me know when you're ready to execute. ⏰"
        ),
        COMPLETION_NORMAL to listOf(
            "Task completed. Finally.",
            "Crossed off the list. Feels good, doesn't it? 📋",
            "One less thing to nag you about. Good job.",
            "Look at you, being all responsible.",
            "Done. I'm mildly impressed.",
            "Task annihilated. Well played. 🚀",
            "You did it! I always believed in you. Mostly.",
            "Another victory for productivity."
        ),
        COMPLETION_EARLY to listOf(
            "Early? Well, look at you.",
            "You beat the clock. I wasn't ready for that.",
            "Done before the alarm. That's actually impressive. 🎯",
            "You finished early. The reminder didn't even get a chance to annoy you.",
            "Proactive completion. Look at us, staying ahead of schedule."
        ),
        COMPLETION_RECURRING to listOf(
            "Completed! Until next time... because it never really ends. 🔁",
            "Done for now. See you at the next occurrence.",
            "Crossed off. But don't get too comfortable, it's coming back.",
            "Finished. The cycle continues.",
            "You won the battle, but the recurring war rages on.",
            "Completed. Resetting the trap for next time.",
            "Done! Another occurrence checked off the roster.",
            "Great job! It's like deja vu all over again."
        ),
        COMPLETION_STREAK to listOf(
            "Look at that streak! You're unstoppable. 🔥",
            "Another day, another victory. The streak lives on! 🏆",
            "You're on fire! Metaphorically, of course.",
            "Consistency is key, and you've got the whole keyring.",
            "A streak like that deserves a reward. But all I have is text.",
            "Killing it! Keep this momentum going.",
            "You're making this look easy. Don't stop now.",
            "Flawless execution. The streak continues!"
        ),
        EMPTY_STATE_NO_TASKS to listOf(
            "Either you're incredibly organized, or you've forgotten to make a checklist. I'm choosing optimism.",
            "Nothing here yet. The silence is deafening.",
            "A blank canvas. Or a terrifying void. Your choice.",
            "Zero tasks. Are you a wizard?",
            "Add a task, or I might start inventing them for you."
        ),
        EMPTY_STATE_ALL_CAUGHT_UP to listOf(
            "Mission Complete! Your future self is proud of you.",
            "All caught up! You may now return to your scheduled doom-scrolling. 🏆",
            "Zero pending tasks. Is this what enlightenment feels like?",
            "You cleared the board. Take a well-deserved nap.",
            "Nothing left to do. I'm officially useless until tomorrow."
        ),
        MISSION_MODE_WARNING to listOf(
            "Warning! Your task is one week out. Do not let this surprise you!",
            "Alert! Incoming priority task on the horizon.",
            "This is your official Mission Mode early warning.",
            "Radar detects a massive task approaching. Prepare yourself.",
            "Advance notice: things are about to get real."
        ),
        MISSION_MODE_WARNING_PLAN to listOf(
            "Advance notice: A key task for your plan is coming up.",
            "Preparing the ground for your plan goal. Stay sharp.",
            "Target acquired on your plan horizon. Keep moving.",
            "Plan commitment ahead. Stay ahead of the deadline. 🎯",
            "Strategic roadmap alert: important milestone approaching."
        ),
        MISSION_MODE_DUE to listOf(
            "24 hours remaining! Panic mode optional, preparation mandatory!",
            "This task is DUE NOW! Do it now. Your future self has enough problems.",
            "MISSION CRITICAL: Drop everything and execute.",
            "No more delays. The mission starts now. 🚨",
            "Red alert. Get this done immediately."
        ),
        MISSION_MODE_DUE_OVERDUE to listOf(
            "We're past the deadline now. Let's handle this mission.",
            "Overdue territory. Time to clear the slate and execute.",
            "Better late than never. Let me know when you are ready.",
            "Deadline has passed. Time to face the music and complete it.",
            "Past due, but not lost. Execute now and recover."
        ),
        MISSION_MODE_DUE_SNOOZED to listOf(
            "We've bought ourselves extra time already. Let me know when you're ready.",
            "Snooze interval elapsed. The mission demands action now.",
            "Time's up for real this time. Let's get it done.",
            "No more snoozing. Time to finish what you started.",
            "Extension window closed. Execute the mission."
        ),
        MISSION_MODE_DUE_SNOOZED_REPEATED to listOf(
            "Snooze interval elapsed again. The mission requires execution, not negotiations.",
            "Repeated snooze detected. The deadline remains non-negotiable.",
            "Time's up for real this time. Multiple extensions expired. 🚨",
            "Extension window closed again. Execute the priority mission now.",
            "Another snooze cycle complete. It's showtime."
        ),
        MISSION_MODE_DUE_RECURRING to listOf(
            "Round two. Same mission, new occurrence. 🔁",
            "The routine continues. Let's knock out this occurrence.",
            "Another cycle of consistency. You know what to do.",
            "Recurring priority event active. Keep the routine alive.",
            "The cycle returns. Show it who is boss."
        ),
        MISSION_MODE_DUE_PLAN to listOf(
            "One more step toward the bigger picture. 🎯",
            "A key commitment for your plan. Let me know when you're ready to check it off.",
            "Crucial progress for your overarching goal. Time to execute.",
            "Plan objective active. This step pushes the goal forward.",
            "High impact milestone due. Execute for maximum progress."
        ),
        MISSION_MODE_COMPLETION to listOf(
            "Mission accomplished. The world is safe once more. 🫡",
            "Critical task neutralized. Excellent work.",
            "Mission success. Returning to base.",
            "You clutched it! Mission completed successfully. 🚀",
            "Priority threat eliminated. You earned a break."
        ),
        MISSION_MODE_COMPLETION_EARLY to listOf(
            "Mission crushed ahead of schedule! Outstanding initiative.",
            "You neutralized the threat before the clock ran out.",
            "Early victory locked in. Peak efficiency achieved.",
            "Target eliminated early. Excellent work.",
            "Finished ahead of deadline. Return to base and celebrate."
        ),
        MISSION_MODE_COMPLETION_STREAK to listOf(
            "Streak protected! Mission complete! 🔥",
            "Another victory locked in. Look at that momentum! 🏆",
            "Flawless execution. The streak continues!",
            "Unstoppable momentum. Priority task conquered.",
            "Masterclass performance. Streak intact and thriving."
        ),
        MISSION_MODE_COMPLETION_RECURRING to listOf(
            "Mission success. See you at the next occurrence.",
            "Cycle complete! Resetting the trap for next time. 🔁",
            "Done for today. Consistency wins again.",
            "Recurring commitment cleared. The schedule stays clean.",
            "Occurrence checked off. Until the routine repeats."
        ),
        WIDGET to listOf(
            "Your tasks are waiting. Faithfully.",
            "Let's see what future-you has agreed to deal with.",
            "Quick glance at your impending responsibilities.",
            "Widget initialized. Get to work.",
            "Always watching. Always waiting. Do your tasks."
        ),
        WIDGET_OVERDUE to listOf(
            "Your widget has one small complaint: that overdue task. ⏰",
            "Your task list has an overdue situation developing.",
            "That task has been waiting. Your widget has noticed.",
            "Overdue task detected on your home screen. Shall we clear it?",
            "Notice anything overdue here? Your schedule certainly does. 🎯"
        ),
        WIDGET_OVERDUE_LATE to listOf(
            "Still overdue. I see we're giving it character development. ⏰",
            "That task has been sitting here long enough to acquire squatters' rights.",
            "Way past due. Your home screen is starting to look like a backlog exhibit.",
            "Overdue task still pending. Time is not on our side here. 🤖",
            "That overdue item is becoming part of the home screen furniture."
        ),
        PLAN_CREATED to listOf(
            "Alright. Big ambitions. I respect the optimism. 🎯",
            "A fresh plan! Let's see how long our motivation lasts.",
            "Grand visions recorded. Now for the pesky execution part.",
            "Plan saved. I'm ready when you are. Or when you finally start.",
            "Another milestone on the roadmap of ambition."
        ),
        PLAN_NO_TASKS to listOf(
            "A noble goal. Now we just need to introduce it to some actual tasks.",
            "Plans are great, execution is better. Add a task when you're ready.",
            "This plan is currently operating entirely on good vibes. Let's add some tasks.",
            "An empty plan is just a daydream with a deadline. Let's populate it. 💡",
            "Waiting for your command. Add a task to get this show on the road."
        ),
        PLAN_PROGRESS_START to listOf(
            "First domino has fallen. Momentum has officially entered the chat. 🚀",
            "The journey of a thousand miles has begun. Don't trip on the first step.",
            "Progress detected! The plan is officially no longer theoretical.",
            "First task crossed off. We're actually doing this.",
            "And so it begins. Keep that energy going."
        ),
        PLAN_PROGRESS_MID to listOf(
            "Halfway there. Look at us, actually following through.",
            "50% complete. The summit is in sight, keep climbing.",
            "Solid progress! You're past the halfway mark of resistance.",
            "Look at those completion bars move. Very satisfying.",
            "We are firmly in the middle. Time to power through."
        ),
        PLAN_PROGRESS_COMPLETE to listOf(
            "100% of tasks completed. You actually pulled it off! 🏆",
            "Every single task checked off. Absolute masterclass in execution.",
            "All tasks complete! Feel free to bask in the glory.",
            "Peak productivity achieved. Well executed."
        ),
        PLAN_DEADLINE_APPROACHING to listOf(
            "That deadline is getting closer than personal space guidelines recommend. ⏰",
            "The deadline is quietly judging our pace.",
            "Approaching deadline alert! Time to shift into gear.",
            "The calendar is reminding us about that deadline. Just a heads up."
        ),
        PLAN_DEADLINE_TODAY to listOf(
            "The deadline is TODAY. Today is the day of destiny! ⚠️",
            "Zero days remaining until the deadline. Showtime!",
            "Today's the day the deadline arrives. Let's finish strong.",
            "Final countdown active. Goal deadline arrives today.",
            "The day has arrived. Push through the final stretch."
        ),
        PLAN_DEADLINE_PASSED to listOf(
            "The deadline has quietly slipped past us. Shall we adjust the timeline or conquer it today?",
            "Deadline came and went. No shame, let's pick up where we left off.",
            "Time travel isn't invented yet, but we can still crush this goal right now.",
            "Behind schedule, but the goal is still worth conquering.",
            "Past the target date. Let's clear the backlog and finish it."
        ),
        PLAN_COMPLETED to listOf(
            "Plan marked completed! Goal achieved and safely in the history books.",
            "Victory lap time. You set a goal and conquered it. 🎯",
            "Goal completed. Take a bow, you earned it.",
            "Masterpiece finished. Every milestone crossed off. 🏆",
            "Target achieved! Time to set an even bigger goal."
        ),
        PLAN_ARCHIVED to listOf(
            "Plan archived. Resting peacefully in the archives of ambition.",
            "Filed away for another day. No loose ends left behind.",
            "Archived. Until we meet again, old goal.",
            "Preserved in history. Stored safely in the archives.",
            "Moved to cold storage. Ambition paused for now."
        ),
        PLAN_REOPENED to listOf(
            "Plan reactivated! The comeback tour starts right now. 🚀",
            "Dusting off an old ambition. Let's give it another round.",
            "Reopened. Unfinished business awaits!",
            "Back in action! Time to finish what we started.",
            "Goal reactivated. Let's conquer the remaining steps."
        )
    )

    fun getPool(poolId: String): List<String> = allPools[poolId] ?: emptyList()
}
