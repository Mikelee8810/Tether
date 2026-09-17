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

### Round 1

1. Voice-first relationship memory capture
2. Contextual life follow-ups / promises
3. Today's Tethers daily priority queue
4. Orbit relationship constellation / relationship map
5. Richer relationship brief and call overlay
6. Actionable home-screen widget
7. ADHD Rescue Mode

### Round 2

8. You Owe a Reply state
9. Pre-meeting relationship briefs
10. Ask Tether relationship-aware search
11. Tether MCP / agent bridge
12. Android system shortcuts / Quick Settings
13. Connection Review
14. Call Recorder integration with optional transcription and context extraction

These features should share one underlying context model rather than becoming disconnected mini-products.

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
9. unanswered incoming communication / You Owe a Reply state
10. an imminent calendar meeting with that person

Do not use opaque AI ranking as the primary engine. The user should be able to understand why someone appears.

### Suggested initial weighting

Start simple and tune from behavior/tests:

- unanswered incoming message needing reply: +110
- overdue follow-up: +100
- due follow-up today: +80
- very overdue relationship: +70
- imminent meeting needing prep: +60
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
6. latest call-summary context when Call Recorder integration is enabled

Example:

> **Marcus** · Friend  
> Last spoke 18 days ago  
> Ask how Friday's interview went  
> Moving to Queens next month

## After-call behavior

After the call ends, offer the Feature 1 memory prompt:

**Anything worth remembering?**

If the Call Recorder integration matched a recording, the prompt may additionally offer:

- `Use recording to help remember`
- `Transcribe later`
- `Skip`

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

# Feature 8 — You Owe a Reply

## Goal

Distinguish a normal overdue relationship from the more actionable state where someone reached out and Mike has not responded.

Examples:

- `Marcus texted Tuesday · no reply yet`
- `Mom sent a message 6 hours ago · reply pending`
- `Missed call from Ashley · no follow-up yet`

This is **not** counted as Mike's relationship effort. It is a separate attention signal derived from incoming communication history.

## Core logic

For direct one-to-one channels, compute reply debt from interaction order:

1. find the latest relevant incoming interaction that does not count toward the relationship timer
2. look for a later outgoing interaction from Mike that qualifies as a response/contact
3. if none exists, expose `owesReply = true`
4. clear the state once a qualifying outgoing interaction occurs

Do not treat every incoming notification as reply debt. Ignore noisy/system-like events and channels where Tether cannot confidently determine one-to-one conversation state.

## UX

Use a distinct state from `Overdue`:

**You owe a reply**

Show the last incoming channel + relative time and offer the correct action:

- Reply
- Call
- Snooze
- Ignore this one

`Ignore this one` suppresses that specific reply-debt item; it must not alter the relationship timer.

## Data model

Prefer deriving this state from interaction history initially rather than adding another source-of-truth table.

If per-item dismissal is needed, add a tiny acknowledgement entity keyed by incoming interaction ID.

```kotlin
@Entity(tableName = "reply_debt_acknowledgements")
data class ReplyDebtAcknowledgement(
    @PrimaryKey val interactionId: Long,
    val acknowledgedAt: Long = System.currentTimeMillis(),
)
```

## Integration

- high priority input to Today's Tethers
- visible on person profile
- actionable from widget/notifications
- usable by Connection Review
- Ask Tether may answer queries such as `Who do I owe a reply?`

## Acceptance criteria

- incoming SMS without later outgoing response creates reply debt
- later outgoing reply/call resolves it
- incoming noise that cannot be matched confidently does not create debt
- ignoring one item does not fake a relationship interaction
- no duplicate reply-debt alerts for the same incoming interaction

---

# Feature 9 — Pre-meeting relationship briefs

## Goal

Before a real calendar event with a known person, surface just enough Tether context to make the interaction easier.

Example:

> **Lunch with Marcus in 20 min**  
> Last spoke 3 weeks ago  
> Ask about Friday's interview  
> Moving to Queens next month

## Privacy-preserving calendar behavior

Tether's existing Calendar connector intentionally avoids event titles/notes. Preserve that default.

For pre-meeting briefs, use only the minimum needed metadata:

- event start/end
- attendee identifiers needed to match known Tether people
- Mike's attendance state when available

Do not require storing titles, descriptions, conference notes, or event bodies.

If an event matches multiple known people, show a compact group brief rather than separate notifications for every person.

## Timing

Default: one brief approximately **20 minutes before** the event.

Rules:

- no brief for declined/cancelled events
- no brief if no known Tether person is matched
- suppress duplicate alerts if event time shifts slightly
- respect notification permissions and quiet behavior
- deep-link to a temporary meeting-brief sheet

## Brief contents

Per person, choose only the most useful items:

1. last meaningful interaction
2. one open follow-up
3. one pinned/relevant memory
4. one upcoming milestone if timely
5. latest call summary when available

## Acceptance criteria

- future attendee matching works without storing event body text
- brief opens to the correct matched people
- rescheduled/cancelled events recover correctly
- no duplicate briefs
- works offline with calendar data already available on device

---

# Feature 10 — Ask Tether

## Goal

Let Mike retrieve relationship memory in normal language instead of remembering where a note lives.

Examples:

- `Who was moving to Queens?`
- `Who do I owe a reply?`
- `What did Marcus and I talk about last time?`
- `Who in my family haven't I talked to recently?`
- `What promises did I make this week?`
- `Which calls mentioned apartments?`

## Architecture

Build this in layers so AI is useful but never the only path.

### Layer 1 — deterministic queries

Handle common structured questions directly from local data:

- overdue / due soon
- reply debt
- follow-ups
- birthdays / anniversaries
- categories
- last contact
- recent calls

### Layer 2 — full-text relationship search

Index:

- person names
- notes/talking points
- relationship memories
- follow-up text
- relationship-edge labels
- call summaries/transcripts when enabled

Prefer a local FTS index first.

### Layer 3 — optional natural-language interpretation

A user-selected AI provider may translate a natural-language question into a constrained local query or summarize already-retrieved results.

Do not allow the model to invent people, dates, promises, or interactions that are not present in Tether data.

## UX

- global search entry point
- voice input allowed
- results grouped by people, follow-ups, memories, and calls when useful
- every answer should link back to the source person/interaction/memory rather than becoming an untraceable chatbot answer

## Acceptance criteria

- common structured questions work with AI disabled
- FTS finds memory/follow-up content
- answers link to underlying records
- empty/no-match states are explicit
- AI failure falls back cleanly

---

# Feature 11 — Tether MCP / Agent Bridge

## Goal

Allow trusted agents such as ChatGPT, Claude, or Codex to work with Tether through narrowly scoped operations instead of free-form database access.

Examples:

- `Who are my top Today's Tethers?`
- `Get Marcus's relationship brief.`
- `Remember that Chris starts his new job Monday.`
- `Create a follow-up to ask Mom about her appointment.`
- `Log that I saw Ashley today.`

## Security model

Do **not** expose the Room database directly.

Start with an allowlisted API surface:

### Read operations

- list today's recommendations
- search people
- get person brief
- list open follow-ups
- list reply debt
- search memories
- get recent interactions

### Write operations

Writes are separate and explicit:

- create memory
- create follow-up
- log manual interaction
- snooze reminder
- mark follow-up complete

Dangerous/destructive operations such as deleting people, merging identities, clearing history, or bulk changes are **not** part of the initial agent API.

## Initial transport

Do not run an unauthenticated internet server inside the Android app.

Preferred first implementation:

- a small `tools/tether-mcp` companion service for the user's computer
- communication with the Android app through an explicit, versioned local bridge during development (ADB/app commands or another authenticated local transport)
- read-only mode first
- write operations added only after the read path is tested and permissioned

A later always-available remote transport may be designed separately if needed; it must not be smuggled into the first implementation.

## Acceptance criteria

- no raw SQL/database exposure
- every write maps to one documented Tether domain operation
- read-only mode can be enabled independently
- agent writes are auditable in Tether
- unavailable phone/bridge returns a clear error rather than stale invented data

---

# Feature 12 — Android system shortcuts / Quick Settings

## Goal

Make the most common Tether actions available without hunting for the app.

## Quick Settings tiles

Initial tiles:

### `Quick Log`

Tap → open Quick Log immediately.

### `Remember`

Tap → open voice/text memory capture with person selection.

Do not create a tile for every feature. Keep the system surface tiny.

## App shortcuts

Long-press Tether icon:

- Log interaction
- Add memory
- Search / Ask Tether
- Today's Tethers

Use dynamic shortcuts for recent/relevant people only if this proves useful and does not expose sensitive context on the launcher.

## Acceptance criteria

- shortcuts deep-link to the exact intended surface
- back navigation returns cleanly
- unavailable permissions show useful recovery state
- actions work without loading the full dashboard first

---

# Feature 13 — Connection Review

## Goal

Provide a calm weekly/monthly reflection on relationship maintenance without friendship scores, streak punishment, or shame mechanics.

Example:

> **September so far**  
> Reconnected with 9 people  
> Completed 6 follow-ups  
> 3 important dates coming up  
> 2 conversations waiting on your reply  
> 4 people quietly drifting

## Metrics

Useful measures include:

- unique people Mike reached out to
- interactions by type/channel
- follow-ups completed
- reply-debt items resolved
- upcoming milestones
- people newly overdue / recovered from overdue
- relationship categories receiving very little effort
- call-recording memories/follow-ups created from conversations

Avoid:

- friendship quality scores
- leaderboards
- `you failed` language
- streak-loss mechanics
- comparing people against each other as if relationships are sales leads

## UX

- lightweight card/sheet, not a finance dashboard
- default weekly snapshot plus monthly view
- every item can drill down to the relevant people
- may surface one suggested next move, but should not turn the review into a task avalanche

## Acceptance criteria

- all stats derive from real Tether records
- zero-data periods look normal, not broken
- archived/track-only people are handled intentionally
- no score claims to measure relationship quality

---

# Feature 14 — Call Recorder integration

## Goal

Use Mike's separate `Call-Recorder` app as an optional high-context source for Tether without merging the two apps into one product.

**Call Recorder owns recording. Tether owns relationship context.**

## Existing Call Recorder capabilities to leverage

The Call Recorder app already maintains recording records with best-effort metadata including:

- SAF content URI / stable recording identity
- display name and relative path
- recording timestamp
- duration
- phone number when available
- incoming/outgoing direction when available
- file size

The integration should consume a clean metadata bridge rather than scraping filenames or directly crawling the user's recording folder from Tether.

## Cross-app bridge

Preferred design: add an explicit **read-only integration surface** to Call Recorder.

Possible implementation choices, in preferred order:

1. signature/custom-permission-protected `ContentProvider` exposing recording metadata and grantable content URIs
2. explicit bound service with a tiny versioned AIDL/API
3. explicit export/share action for a selected recording as fallback

Do not have Tether request broad storage access merely to discover recordings.

The bridge should support queries such as:

- recordings around a timestamp
- recordings matching a normalized phone number
- one recording by stable recording key

## Matching a recording to a Tether interaction

Use multiple signals:

1. normalized phone number
2. call direction
3. start timestamp proximity
4. duration proximity when available
5. associated Tether call interaction

High-confidence match → link automatically.

Ambiguous match → ask once or leave unlinked.

Never attach a recording to a person based only on fuzzy display name.

## Tether data model

Store a reference, not a duplicate audio file.

```kotlin
enum class TranscriptState { NONE, QUEUED, READY, FAILED }

@Entity(
    tableName = "recorded_call_refs",
    foreignKeys = [
        ForeignKey(
            entity = Person::class,
            parentColumns = ["id"],
            childColumns = ["personId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Interaction::class,
            parentColumns = ["id"],
            childColumns = ["interactionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("personId"), Index("interactionId", unique = true), Index("recordingKey", unique = true)],
)
data class RecordedCallRef(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val interactionId: Long,
    val sourcePackage: String = "com.kitsumed.shizucallrecorder",
    val recordingKey: String,
    val contentUri: String? = null,
    val recordedAt: Long,
    val durationMillis: Long? = null,
    val transcriptState: TranscriptState = TranscriptState.NONE,
    val transcript: String? = null,
    val summary: String? = null,
)
```

If provider URIs are not guaranteed durable across app restarts/upgrades, persist only a stable recording key and resolve a fresh URI from Call Recorder when needed.

## Transcription

Transcription is optional and separate from recording.

### Default behavior

- link recording metadata automatically when confidence is high
- do **not** automatically transcribe every call by default
- after a recorded call, offer `Use recording to help remember`

### Transcription paths

Prefer, in order:

1. on-device transcription when practical on the target Pixel
2. user-selected local model/service
3. explicit cloud transcription when the user has enabled it

If cloud transcription is used, disclose that audio leaves the device before upload. Do not silently reuse a text-chat API key for an audio endpoint that provider does not support.

## From transcript to useful Tether context

A transcript may produce **suggestions**, not silent facts.

Possible extracted candidates:

- relationship memory
- promise/follow-up
- important date/event
- topic/talking point
- person relationship link

Example:

> **From your call with Marcus:**  
> `Interview is Friday` → Add memory  
> `Ask me how it went next week` → Create follow-up

Mike can accept, edit, or dismiss each suggestion.

## Person timeline

A matched call can show:

- call timestamp/duration
- `Recording available`
- play/open in Call Recorder
- transcript state
- short approved summary
- accepted memories/follow-ups generated from it

Do not dump a full transcript into the main relationship timeline by default.

## Ask Tether integration

When transcription is enabled, questions may search transcript/summaries, for example:

- `Which call did we talk about the apartment in?`
- `What did Marcus say about his interview?`
- `Did I promise Chris anything last week?`

Every answer must link back to the source call and transcript segment or summary record.

## Retention / deletion behavior

- deleting a recording in Call Recorder must leave Tether in a safe `recording unavailable` state
- deleting a transcript/summary in Tether must not delete the original audio unless the user explicitly chooses a cross-app delete action later
- Tether backup/export should not duplicate raw recording audio by default
- transcript/summary backup behavior must be explicit

## Legal / consent boundary

Tether must not bypass or weaken Call Recorder's recording controls, exclusions, consent/legal notices, or user intent.

Tether's integration begins **after a recording exists** or after Call Recorder explicitly exposes it through the bridge.

## Acceptance criteria

- matched recording links to the correct Tether call interaction
- ambiguous recordings do not silently attach to the wrong person
- Tether can function normally when Call Recorder is not installed
- Call Recorder can function normally when Tether is not installed
- no broad storage permission is required solely for this integration
- transcription is optional
- generated memories/follow-ups require confirmation
- deleted/unavailable recording is handled gracefully

---

# 3. Shared architecture changes

## Database

The new features require Room schema version bumps and explicit migrations for:

- `relationship_memories`
- `follow_ups`
- `relationship_edges`
- `daily_tether_selections`
- `reply_debt_acknowledgements` if acknowledgement persistence is required
- `recorded_call_refs`

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
- reply debt
- upcoming matched meetings
- recorded calls/transcript state
- Connection Review aggregates

Keep ranking logic in a dedicated engine/domain layer, not Compose UI.

## Search

Current dashboard search already includes name/category/notes/talking points. Extend search to surface:

- memory text
- follow-up text
- relationship labels
- call summaries/transcripts when enabled

Search results should still resolve cleanly back to a person or source record, not become an untraceable document-search UI.

## Backup / export

Update `BackupManager` to preserve all new Tether-owned entities.

Import/restore must handle missing newer sections gracefully when restoring an older backup.

Raw Call Recorder audio remains owned by Call Recorder and is not copied into Tether backups by default.

## AI Wingman

Feed the Wingman only the smallest useful context:

- current relevant talking point
- selected/pinned recent memory
- relevant open follow-up
- optional approved recent call summary

Do not dump an entire relationship history or full transcript into the prompt.

AI remains an enhancement. Core reminders, memories, priorities, Orbit links, widget actions, reply debt, meeting briefs, and call-recording linkage must work without it.

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

1. Add reply-debt derivation + acknowledgement behavior.
2. Add `TodayPriorityEngine`.
3. Add reason codes/text.
4. Add `DailyTetherSelection` persistence.
5. Convert Daily Spark into Today's Tethers.
6. Add Rescue Mode.

**Evidence before moving on:** ranking tests, reply-debt tests, stable-day tests, screenshots for normal + 20-overdue Rescue Mode.

## Phase C — Relationship context surfaces

1. Upgrade call overlay.
2. Add after-call memory capture.
3. Surface memories/follow-ups on person screen.
4. Feed selected context into AI Wingman.
5. Add pre-meeting brief.

**Evidence before moving on:** call-overlay state tests, calendar matching tests, and rendered screenshots.

## Phase D — Call Recorder bridge

1. Define/version the Call Recorder read-only integration contract.
2. Implement metadata exposure in Call Recorder.
3. Implement Tether-side recording discovery/matching.
4. Add `RecordedCallRef` persistence.
5. Add open/play/deep-link to Call Recorder.
6. Add opt-in transcription pipeline.
7. Add transcript-to-memory/follow-up suggestions.

**Evidence before moving on:** cross-app matching tests, wrong-person ambiguity tests, unavailable/deleted recording recovery, and transcript opt-in verification.

## Phase E — Orbit constellation

1. Add `RelationshipEdge` entity/repository.
2. Add manual edge editor.
3. Build bounded constellation around one selected person.
4. Add Orbit People / Constellation switching.
5. Add second-degree expansion and polish after the representative graph passes screenshot review.

**Evidence before moving on:** graph data tests + Pixel screenshot review with small, medium, and dense datasets.

## Phase F — Search + agent surfaces

1. Add local full-text index.
2. Add Ask Tether deterministic query handlers.
3. Add optional natural-language query interpretation.
4. Build read-only Tether agent API.
5. Add local `tools/tether-mcp` companion.
6. Add explicit write operations with audit trail after read-only path is stable.

**Evidence before moving on:** source-linked answers, no-hallucination tests for empty data, permission/audit tests for agent writes.

## Phase G — System surfaces

1. Drive widget ranking from Today's Tethers.
2. Add call/message/log/snooze widget actions.
3. Add compact + medium/large widget layouts.
4. Add Quick Settings tiles.
5. Add launcher/app shortcuts.
6. Verify refresh/deep-link behavior after state changes.

**Evidence before moving on:** widget/tile/shortcut interaction tests on emulator and screenshots across supported sizes.

## Phase H — Connection Review

1. Add aggregate queries.
2. Build weekly/monthly review sheet.
3. Add drill-down to people/source records.
4. Verify no score/streak language leaks into the UX.

---

# 5. Cross-feature edge cases

The implementation must explicitly test:

- person merged after memories/follow-ups/recording links exist
- mistaken identity split/unmerge
- archived person with open follow-up
- restored person
- phone contact deleted/recreated
- incoming message followed by outgoing reply across midnight/timezone change
- multiple incoming messages before one reply
- group-message noise incorrectly appearing as reply debt
- timezone/DST change around follow-up or meeting brief
- calendar event rescheduled/cancelled
- reboot before/after due follow-up
- reminder permission revoked
- AI provider missing/failing
- speech recognition unavailable/offline
- Call Recorder not installed
- Call Recorder recording deleted after Tether linked it
- ambiguous call-recording match
- transcript failure/partial transcript
- no phone number for Call/Message widget action
- multiple phone numbers
- duplicate connector interaction
- 100+ tracked contacts
- 100+ overdue contacts
- very long names/memory/transcript text
- dark/light theme
- large font scaling
- process death while capture sheet is open
- agent bridge unavailable/offline
- agent tries unsupported/destructive write

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
- Call Recorder as a separate recording product
- source-linked answers and suggestions

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
- treating an incoming message as Mike's effort
- silently transcribing every recorded call
- attaching a recording based on fuzzy name only
- exposing raw Room/SQL through the agent bridge
- redesigning the app into generic Material 3 styling

---

# 7. Documentation cleanup required during implementation

The repository currently contains privacy/storage wording that is stricter than the actual implementation in places. Do not keep a false promise merely because it was written earlier.

Before release, documentation must accurately describe:

- what stays on device
- what can be sent to a user-selected AI/transcription provider
- how API keys/settings are stored
- whether database encryption is implemented or not
- backup/export behavior
- how Call Recorder audio is linked and whether transcripts/summaries are stored
- what the agent bridge can read/write

This is a documentation/implementation alignment task. It does **not** require abandoning cloud AI, transcription, or the current product direction.

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
10. cross-app/agent permissions are verified where relevant

Use the maturity labels from `docs/production-quality-gate.md`. Do not call the app production-ready until that gate is satisfied.

---

# 9. Short version for future agents

If you only remember one thing:

**Tether should become a relationship second brain, not a prettier reminder list.**

It should remember the small human details, choose a tiny number of people who matter today, explain why, help Mike act immediately, learn useful context from explicitly linked calls when enabled, and stay out of the way afterward.
