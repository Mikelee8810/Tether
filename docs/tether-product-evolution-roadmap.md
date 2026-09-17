# Tether — Product Evolution Roadmap

> **Status:** approved product direction, not yet implemented.
>
> This document captures the next major Tether product improvements agreed on September 17, 2026. It exists so future Claude/Codex/agent work does not have to reconstruct the decisions from chat history.

## 1. Locked product direction

These are constraints, not suggestions.

- **Tether keeps its Liquid Glass visual identity.** Do not replace it with a generic Material 3 / Android-looking redesign.
- Android/Pixel remains the first-class target.
- Existing relationship-counting rules in `docs/relationship-rules.md` remain authoritative.
- Tether measures **Mike's effort to maintain relationships**, not generic social engagement or incoming attention.
- Existing tracking, reminders, Shizuku, connectors, identity resolution, Orbit, Daily Spark, call overlay, AI Wingman, and widget behavior should be extended rather than replaced without evidence.
- New features should reduce mental overhead. They must not turn Tether into a CRM that requires constant manual administration.
- Major UI work still follows `AGENTS.md`: build one representative state, render it on the Pixel emulator, review the screenshot, fix it, then scale.

## 2. What we are implementing

1. Voice-first relationship memory capture
2. Contextual life follow-ups / promises
3. Today's Tethers daily priority queue
4. Orbit relationship constellation / relationship map
5. Richer relationship brief and call overlay
6. Actionable home-screen widget
7. ADHD Rescue Mode

These features are related and should share one underlying context model rather than becoming seven disconnected mini-products.

---

# Feature 1 — Voice-first relationship memory capture

## Goal

After a meaningful interaction, Tether should make it nearly effortless to save the one thing Mike may want to remember later.

Examples:

- "Marcus has an interview Friday."
- "Mom's appointment is next Tuesday."
- "Ashley is moving to Queens in October."
- "Ask Chris how the new manager is working out."

## UX

### Trigger points

Offer the capture affordance after:

- a completed phone call
- a manually logged in-person interaction
- a manual quick-log event
- other connectors only when Tether can confidently identify a completed meaningful interaction

Do **not** interrupt after every tiny interaction.

### Interaction

Use a small Liquid Glass bottom sheet / overlay:

**Anything worth remembering?**

- one-line text field
- microphone button
- `Save`
- `Skip`

Voice capture should convert speech to text, then save the text. Audio should not be retained by default.

If Android on-device speech recognition is available, prefer it. Fall back to the system speech-recognition path when needed.

The capture flow must be dismissible in one tap and must never block the user from returning to the phone/message workflow.

## Data model

Add a dedicated entity instead of stuffing everything into `Person.notes`.

```kotlin
@Entity(
    tableName = "relationship_memories",
    foreignKeys = [ForeignKey(
        entity = Person::class,
        parentColumns = ["id"],
        childColumns = ["personId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("personId"), Index("createdAt"), Index("interactionId")],
)
data class RelationshipMemory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val interactionId: Long? = null,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val occurredAt: Long? = null,
    val source: String = "manual",
    val pinned: Boolean = false,
)
```

`interactionId` may reference the interaction that caused the prompt. Do not require it for manually created memories.

## Surfaces

Memories should appear in:

- person timeline/profile
- search results
- relationship brief
- call overlay
- AI Wingman context
- future follow-up extraction

## Acceptance criteria

- Saving a typed memory takes no more than the interaction prompt + one confirmation.
- Voice capture produces editable text before final save.
- Dismissing the prompt creates nothing.
- Multiple memories can exist for one person.
- Deleting a person cascades their memories.
- Backup/export includes memories.
- No raw microphone audio is retained unless a future feature explicitly changes that behavior.

---

# Feature 2 — Contextual life follow-ups / promises

## Goal

Tether should remember **why** Mike should reconnect, not only that a reminder interval expired.

Examples:

- "Ask Marcus how the interview went."
- "Check on Mom after her appointment."
- "Ashley gets back from vacation Friday."
- "I told Chris I'd send him that link."

## UX

Follow-ups can be created from:

- a relationship memory
- the person screen
- quick log
- call-end memory capture
- AI suggestion, but only after user confirmation

Each follow-up has:

- short action/reason
- optional due date/time
- optional linked memory
- state: open / completed / dismissed
- optional priority

A follow-up notification must deep-link directly to that person and that follow-up.

Completing a follow-up does **not** automatically count as relationship contact unless an actual interaction is also logged.

## Data model

```kotlin
enum class FollowUpStatus { OPEN, COMPLETED, DISMISSED }

enum class FollowUpSource { MANUAL, MEMORY, AI_SUGGESTION, CONNECTOR }

@Entity(
    tableName = "follow_ups",
    foreignKeys = [ForeignKey(
        entity = Person::class,
        parentColumns = ["id"],
        childColumns = ["personId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("personId"), Index("dueAt"), Index("status")],
)
data class FollowUp(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val memoryId: Long? = null,
    val text: String,
    val dueAt: Long? = null,
    val status: FollowUpStatus = FollowUpStatus.OPEN,
    val source: FollowUpSource = FollowUpSource.MANUAL,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
)
```

## Extraction behavior

Do not silently create follow-ups from private notes.

If AI detects a likely promise or future event, show a suggestion such as:

> Create follow-up: "Ask Marcus how the interview went" — Friday

Then let Mike accept, edit, or dismiss it.

If a cloud AI provider is used, reuse the existing provider settings and clearly indicate when relationship context is being sent to that provider. A cloud AI provider is optional, not required for manually created follow-ups.

## Reminder integration

Follow-up notifications are separate from normal relationship cadence reminders.

Rules:

- never send duplicate alerts for the same follow-up
- if the follow-up is completed, suppress pending notifications
- respect snooze/quiet behavior
- a due follow-up can increase a person's Today’s Tethers priority
- if a relationship reminder and follow-up are both due, prefer one combined notification when practical

## Acceptance criteria

- Follow-up can be created in under 10 seconds.
- Completing it does not falsely advance the relationship timer.
- Due follow-ups surface on the dashboard/person screen.
- Dismissed follow-ups stay out of reminders.
- Follow-ups survive reboot/process death.
- Backup/export includes follow-ups.

---

# Feature 3 — Today's Tethers

## Goal

Replace decision overload with a tiny, stable daily queue of the people who most deserve attention **today**.

The user should not have to scan 20 overdue contacts and decide where to start.

## UX

The current Daily Spark evolves into a `Today's Tethers` section.

Default size: **up to 3 people**.

Each item must show a plain-English reason, for example:

- `12 days overdue`
- `Birthday tomorrow`
- `Follow up: interview Friday`
- `You meant to ask about the move`

The list should remain stable for the local calendar day unless:

- the user completes/logs an interaction
- the user snoozes/dismisses the recommendation
- a materially more urgent event appears

Do not reshuffle on every app open.

## Ranking model

Create a deterministic `TodayPriorityEngine` that produces a score plus human-readable reasons.

Inputs should include:

1. active due/overdue relationship state
2. very-overdue severity
3. open follow-up due today/overdue
4. birthday/anniversary or important date proximity
5. category / relationship interval
6. recent user effort
7. snooze/pause state
8. whether the person was already shown recently

Do not use opaque AI ranking as the primary engine. The user should be able to understand why someone appears.

### Suggested initial weighting

Start simple and tune from behavior/tests:

- overdue follow-up: +100
- due follow-up today: +80
- very overdue relationship: +70
- overdue relationship: +50
- birthday/anniversary within 48h: +45
- due soon: +25
- shown yesterday but skipped: -10
- snoozed/paused: excluded

These are implementation defaults, not permanent product truth.

## Persistence

Add a lightweight daily selection record so the queue does not randomly change.

```kotlin
@Entity(
    tableName = "daily_tether_selections",
    indices = [Index(value = ["localDate", "personId"], unique = true)],
)
data class DailyTetherSelection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val localDate: String,
    val personId: Long,
    val rank: Int,
    val reasonCode: String,
    val reasonText: String,
    val dismissed: Boolean = false,
)
```

## Acceptance criteria

- No more than three default recommendations.
- Every recommendation explains itself.
- Queue is stable during the day.
- Logging contact immediately removes/resolves the appropriate recommendation.
- Snoozed/paused people do not appear.
- Queue works entirely offline.

---

# Feature 4 — Orbit relationship constellation

## Goal

Turn Orbit into a genuinely useful visual map of how people connect to one another, while preserving its Liquid Glass / constellation identity.

Examples:

- Mike → sister → sister's husband → niece
- Mike → Chris → coworker/friend group
- Mike → Marcus → mutual friend Jordan

This is **not** a corporate org chart.

## Data model

Add explicit person-to-person relationships.

```kotlin
enum class RelationshipEdgeSource { MANUAL, CONTACT_HINT, AI_SUGGESTION }

@Entity(
    tableName = "relationship_edges",
    indices = [
        Index("fromPersonId"),
        Index("toPersonId"),
        Index(value = ["fromPersonId", "toPersonId", "relationType"], unique = true),
    ],
)
data class RelationshipEdge(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fromPersonId: Long,
    val toPersonId: Long,
    val relationType: String,
    val note: String? = null,
    val source: RelationshipEdgeSource = RelationshipEdgeSource.MANUAL,
    val confidence: Float = 1f,
    val createdAt: Long = System.currentTimeMillis(),
)
```

Use foreign keys if practical with the chosen delete behavior. If one person is archived, keep the edge unless product behavior later says otherwise. If a person is permanently deleted, remove edges safely.

## UX

Keep `Orbit` as the second major tab, but allow two internal views:

- `People` — current new/uncategorized/discovery behavior
- `Constellation` — interactive relationship map

Constellation principles:

- Mike is the visual center/root
- close/direct relationships are nearest
- linked people cluster naturally
- Liquid Glass nodes/lines are part of the visual identity
- color/status may communicate relationship state, but do not make the graph unreadable
- tap a person → profile
- tap an edge → relationship detail/edit
- add link → choose two people + relation type

Do not attempt an unlimited graph on day one. Start with a bounded first-degree + second-degree view around a selected person.

## Suggested relationship types

Allow presets plus custom text:

- family
- partner/spouse
- friend
- coworker
- neighbor
- mutual friend
- parent/child
- sibling
- custom

## Acceptance criteria

- User can create, edit, and remove a relationship edge.
- Graph remains usable with at least 30 tracked people.
- Selecting a person recenters or focuses the graph.
- Archived contacts remain distinguishable rather than silently disappearing.
- Relationship edges are included in backup/export.
- Visual implementation must be screenshot-reviewed on the Pixel emulator before scaling.

---

# Feature 5 — Rich relationship brief + call overlay

## Goal

When a call comes in or Mike opens a person, Tether should instantly answer:

**Who is this person, what was going on last time, and what should I remember right now?**

## Existing foundation

Tether already has `CallOverlayService`, `PhoneStateReceiver`, person notes, talking points, last effort, birthdays/anniversaries, and relationship status.

Extend those pieces. Do not replace the call overlay architecture unless a real limitation requires it.

## Brief content

Keep the overlay intentionally short. Maximum useful content should fit without making someone read a CRM during a ringing phone.

Priority order:

1. person name + relationship/category
2. last meaningful interaction / `last spoke`
3. highest-priority open follow-up
4. most recent pinned/relevant memory
5. upcoming important date when near enough to matter

Example:

> **Marcus** · Friend  
> Last spoke 18 days ago  
> Ask how Friday's interview went  
> Moving to Queens next month

## After-call behavior

After the call ends, offer the Feature 1 memory prompt:

**Anything worth remembering?**

If an open follow-up appears to have been addressed, offer:

`Mark follow-up complete?`

Do not auto-complete it just because a call happened.

## Acceptance criteria

- Overlay never hides critical system call controls.
- Useful brief renders quickly from local data.
- No more than one primary follow-up + one context memory shown by default.
- After-call memory capture is available but dismissible.
- Call overlay remains useful even when AI is disabled/unconfigured.

---

# Feature 6 — Actionable home-screen widget

## Goal

The widget should let Mike act on a relationship without opening Tether first.

## Existing foundation

`RadarWidget` already lists people needing attention and deep-links to a person or Quick Log.

## New actions

For the highest-priority people, expose actions appropriate to available identifiers:

- **Call**
- **Message**
- **Snooze**
- **Log**

Do not use `Done` unless the action actually records a real interaction. `Log` is clearer and preserves the relationship-counting rules.

### Implementation notes

- Call/Message can launch appropriate system intents using the person's resolved phone identifier.
- Snooze should use a Glance callback/service/receiver path that updates reminder state without opening the full app.
- Log should deep-link directly to Quick Log with the person preselected.
- Refresh the widget after logging, snoozing, reminder-state changes, or daily-priority recalculation.

## Widget sizes

Support at least:

### Compact

- Today's top Tether
- reason
- Call / Message

### Medium/Large

- up to 3 Today's Tethers
- reason for each
- quick actions
- Quick Log entry point

## Acceptance criteria

- Actions work from a locked/home-screen context where Android permits them.
- No stale person remains after an interaction resolves their due state.
- Widget works when there are zero due people.
- Widget respects archived/paused/snoozed state.
- Widget does not expose private notes or memories in excessive detail on the home screen.

---

# Feature 7 — ADHD Rescue Mode

## Goal

When the backlog becomes large, Tether should reduce shame/decision paralysis instead of visually screaming that Mike has failed 18 people.

## Activation

Initial rule:

- automatically suggest Rescue Mode when **8 or more** people are due/overdue
- allow manual activation at any time
- remember dismissal so the suggestion does not nag repeatedly

The threshold is a tunable product default, not permanent doctrine.

## UX

When active, the main dashboard emphasizes only:

> **Forget the backlog. These 3 are enough for today.**

Show Today's Tethers and hide/de-emphasize the giant overdue backlog behind a deliberate `See everyone` action.

Do not reset or delete reminder state. Rescue Mode is presentation + prioritization, not data manipulation.

## Behavior

- maximum three suggested relationships at once
- user can replace one recommendation if it is emotionally/temporarily inappropriate
- replaced person should not immediately bounce back into the same session
- completing one can optionally reveal the next candidate, but never create an endless treadmill
- no streak-loss language, guilt language, red-wall UI, or punishment framing

## Acceptance criteria

- Rescue Mode never changes actual interaction timestamps.
- Leaving Rescue Mode restores the full dashboard with intact state.
- Suggestions are stable and explainable.
- Mode remains useful with notifications disabled.
- Screen is explicitly tested for high-backlog states (8, 20, 100 due people).

---

# 3. Shared architecture changes

## Database

The new features require a Room schema version bump and explicit migrations for:

- `relationship_memories`
- `follow_ups`
- `relationship_edges`
- `daily_tether_selections`

No destructive migration is acceptable for existing user data.

Add migration tests for every supported prior schema.

## Repository layer

Expose flows/queries for:

- memories per person
- latest/pinned memory
- open/due follow-ups
- relationship graph around person
- daily priority candidates
- persisted Today's Tethers selection

Keep ranking logic in a dedicated engine/domain layer, not Compose UI.

## Search

Current dashboard search already includes name/category/notes/talking points. Extend search to optionally surface:

- memory text
- follow-up text
- relationship labels

Search results should still resolve to a person, not become a document-search UI.

## Backup / export

Update `BackupManager` to preserve all new entities.

Import/restore must handle missing newer sections gracefully when restoring an older backup.

## AI Wingman

Feed the Wingman only the smallest useful context:

- current relevant talking point
- selected/pinned recent memory
- relevant open follow-up

Do not dump an entire relationship history into the prompt.

AI remains an enhancement. Core reminders, memories, priorities, Orbit links, widget actions, and call briefs must work without it.

---

# 4. Implementation order

Build in this order because each stage supplies data or behavior needed by the next one.

## Phase A — Context foundation

1. Add `RelationshipMemory` entity + DAO/repository methods.
2. Add typed/manual memory capture UI.
3. Add voice-to-text capture.
4. Add `FollowUp` entity + CRUD + due-query logic.
5. Integrate memories/follow-ups into backup/export.

**Evidence before moving on:** migration tests, unit tests, representative memory/follow-up UI screenshot.

## Phase B — Daily intelligence

1. Add `TodayPriorityEngine`.
2. Add reason codes/text.
3. Add `DailyTetherSelection` persistence.
4. Convert Daily Spark into Today's Tethers.
5. Add Rescue Mode.

**Evidence before moving on:** ranking tests, stable-day tests, screenshots for normal + 20-overdue Rescue Mode.

## Phase C — Relationship context surfaces

1. Upgrade call overlay.
2. Add after-call memory capture.
3. Surface memories/follow-ups on person screen.
4. Feed selected context into AI Wingman.

**Evidence before moving on:** call-overlay state tests and rendered screenshots.

## Phase D — Orbit constellation

1. Add `RelationshipEdge` entity/repository.
2. Add manual edge editor.
3. Build bounded constellation around one selected person.
4. Add Orbit People / Constellation switching.
5. Add second-degree expansion and polish after the representative graph passes screenshot review.

**Evidence before moving on:** graph data tests + Pixel screenshot review with small, medium, and dense datasets.

## Phase E — Widget actions

1. Drive widget ranking from Today's Tethers.
2. Add call/message/log actions.
3. Add snooze callback.
4. Add compact + medium/large layouts.
5. Verify refresh behavior after state changes.

**Evidence before moving on:** widget interaction tests on emulator and screenshots across supported sizes.

---

# 5. Cross-feature edge cases

The implementation must explicitly test:

- person merged after memories/follow-ups exist
- mistaken identity split/unmerge
- archived person with open follow-up
- restored person
- phone contact deleted/recreated
- timezone/DST change around follow-up due date
- reboot before/after due follow-up
- reminder permission revoked
- AI provider missing/failing
- speech recognition unavailable/offline
- no phone number for Call/Message widget action
- multiple phone numbers
- duplicate connector interaction
- 100+ tracked contacts
- 100+ overdue contacts
- very long names/memory text
- dark/light theme
- large font scaling
- process death while capture sheet is open

---

# 6. Product guardrails

## Keep

- Liquid Glass aesthetic
- Pixel-first interaction quality
- low-friction quick actions
- local deterministic core behavior
- Tether's existing relationship-counting rules
- reminders separate from raw tracking
- confidence-aware identity matching
- explainable recommendations

## Avoid

- turning every interaction into a prompt
- forcing notes after every conversation
- generic CRM dashboards
- giant overdue counters as the emotional center of the app
- opaque AI ranking
- silently auto-creating promises/follow-ups
- AI as a dependency for core behavior
- auto-completing a follow-up just because contact occurred
- counting a reminder action as real contact
- redesigning the app into generic Material 3 styling

---

# 7. Documentation cleanup required during implementation

The repository currently contains privacy/storage wording that is stricter than the actual implementation in places. Do not keep a false promise merely because it was written earlier.

Before release, documentation must accurately describe:

- what stays on device
- what can be sent to a user-selected AI provider
- how API keys/settings are stored
- whether database encryption is implemented or not
- backup/export behavior

This is a documentation/implementation alignment task. It does **not** require abandoning cloud AI or the current product direction.

---

# 8. Definition of done for this roadmap

A feature in this document is not `done` because Kotlin exists.

For each feature:

1. required data migration exists and is tested
2. domain behavior has unit tests
3. failure/empty/denied states are defined
4. representative UI state is rendered on `Pixel_Radar`
5. screenshot is reviewed for Tether's actual visual identity
6. accessibility basics are checked
7. backup/export impact is handled
8. relevant end-to-end path works
9. docs are updated to match behavior

Use the maturity labels from `docs/production-quality-gate.md`. Do not call the app production-ready until that gate is satisfied.

---

# 9. Short version for future agents

If you only remember one thing:

**Tether should become a relationship second brain, not a prettier reminder list.**

It should remember the small human details, choose a tiny number of people who matter today, explain why, help Mike act immediately, and stay out of the way afterward.
