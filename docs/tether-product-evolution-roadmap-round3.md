# Tether — Product Evolution Roadmap, Round 3

> **Status:** approved implementation direction.
>
> This document is a companion to `docs/tether-product-evolution-roadmap.md`. The six items below are **kept/approved**, not merely brainstorm candidates. Future Claude/Codex/agent work should treat them as part of Tether's planned product evolution.

## Round 3 approved additions

15. Care Seasons / temporary relationship priority
16. Source-linked Call Moments
17. Little Things / durable person facts
18. Best Time + Best Way to Reach
19. Only Group Contact Lately
20. Tether Cue / ambient relationship context

These extend the same product goal: **Tether should become a relationship second brain, not a prettier reminder list.**

---

# Feature 15 — Care Seasons

## Goal

Represent temporary periods when someone deserves more attention than their normal recurring reminder cadence.

Examples:

- `Mom is recovering from surgery — check in every 3 days until Oct 5.`
- `Marcus has an interview Friday — elevated attention through next week.`
- `Ashley is grieving — gentle check-ins for the next month.`
- `Chris just moved — check in a little more often for two weeks.`

A Care Season is **not** a permanent category change and should not overwrite the person's normal reminder interval.

## UX

From a person profile, memory, follow-up, Call Moment, or Tether Cue, allow:

`Start a Care Season`

Fields:

- short reason/title
- starts now or chosen date
- optional end date
- temporary check-in cadence
- intensity/preset such as Gentle, Regular, Close Support
- optional linked memory/call/follow-up

Keep setup extremely short. Presets should cover most cases.

When active, the profile can show a small Liquid Glass state such as:

> ❤️ **Care Season**  
> Recovering from surgery · through Oct 5  
> Check in every 3 days

## Data model

```kotlin
enum class CareSeasonStatus { ACTIVE, ENDED, DISMISSED }
enum class CareSeasonSource { MANUAL, MEMORY, FOLLOW_UP, CALL_MOMENT, AI_SUGGESTION }

@Entity(
    tableName = "care_seasons",
    foreignKeys = [ForeignKey(
        entity = Person::class,
        parentColumns = ["id"],
        childColumns = ["personId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("personId"), Index("startsAt"), Index("endsAt"), Index("status")],
)
data class CareSeason(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val title: String,
    val startsAt: Long = System.currentTimeMillis(),
    val endsAt: Long? = null,
    val temporaryIntervalDays: Int? = null,
    val source: CareSeasonSource = CareSeasonSource.MANUAL,
    val sourceRecordId: Long? = null,
    val status: CareSeasonStatus = CareSeasonStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
)
```

## Reminder integration

Care Seasons temporarily influence attention without replacing permanent settings.

Rules:

1. compute the person's normal relationship due state
2. compute any active Care Season cadence
3. use the **more attentive** active cadence while the season is active
4. when the season ends, immediately fall back to the person's normal cadence
5. do not rewrite `reminderIntervalDays` just to simulate the temporary state

A Care Season may raise a person's Today’s Tethers priority and may appear in a pre-meeting/call brief.

## Ending behavior

A Care Season ends when:

- its end date passes
- Mike manually ends it
- an accepted AI suggestion explicitly sets an end

Do not infer that a sensitive situation is over merely because contact occurred.

## Acceptance criteria

- starting a Care Season never destroys the normal reminder configuration
- ending it restores normal cadence automatically
- active season is visible on person profile
- Today’s Tethers can explain `Care Season: recovering from surgery`
- season works without AI
- backup/export includes it
- no guilt language if a Care Season check-in is missed

---

# Feature 16 — Source-linked Call Moments

## Goal

When Tether learns something from a linked Call Recorder transcript, preserve **where it came from** so memories and follow-ups have receipts instead of becoming floating AI claims.

Example:

> **Marcus has an interview Friday**  
> 🎙️ From call Sep 16 · 12:43

Tap the source and jump to the corresponding transcript/recording moment when available.

## Relationship to Feature 14

Feature 14 links Tether to the separate Call Recorder app and optionally produces transcripts/summaries.

Feature 16 adds **source anchoring** on top of that bridge.

Call Recorder remains the owner of raw audio. Tether stores relationship context plus stable source references.

## Data model

Use a generic source anchor so memories, follow-ups, Little Things, and other future records can all point back to evidence.

```kotlin
enum class SourceAnchorType { CALL_TRANSCRIPT, INTERACTION, MEMORY, MANUAL }

@Entity(
    tableName = "source_anchors",
    indices = [Index("sourceType"), Index("sourceRecordKey")],
)
data class SourceAnchor(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceType: SourceAnchorType,
    val sourceRecordKey: String,
    val personId: Long? = null,
    val interactionId: Long? = null,
    val startOffsetMillis: Long? = null,
    val endOffsetMillis: Long? = null,
    val excerpt: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
```

Add nullable `sourceAnchorId` to Tether-owned records that can be grounded in a source, starting with:

- `RelationshipMemory`
- `FollowUp`
- `PersonFact` / Little Things

## Transcript segmentation

When transcription is enabled:

- keep timestamps/segment offsets when the transcription engine can supply them
- extracted suggestions should retain the relevant segment range
- if exact offsets are unavailable, store a best-effort call-level source rather than inventing precision

## UX

A source chip should be compact:

`🎙 Sep 16 call · 12:43`

Tap:

1. open the matching Tether call detail
2. highlight the relevant transcript segment if present
3. offer `Open recording` in Call Recorder if the recording is still available

Do not expose giant transcript excerpts in everyday screens.

## Trust behavior

AI-extracted claims remain suggestions until accepted.

If the transcript says:

`My interview is Friday.`

Tether may suggest:

`Add memory: Marcus has an interview Friday`

Only after Mike accepts does it become durable relationship context.

The accepted memory keeps the source anchor.

## Acceptance criteria

- accepted call-derived memory/follow-up can navigate back to its source
- deleted recording degrades to `source recording unavailable` without deleting accepted Tether context
- missing transcript offsets never produce fake timestamps
- Ask Tether answers can cite the source call/segment
- backup/export handles anchors deliberately without duplicating raw audio

---

# Feature 17 — Little Things

## Goal

Store durable person facts that are useful repeatedly but do not belong as chronological memories.

Examples:

- Partner: Jasmine
- Dog: Rocky
- Favorite food: Thai
- Prefers: Texting
- Gift idea: Air fryer
- Usually free: Sundays
- Hates: Surprise phone calls
- Kids: Maya and Jordan

This should feel like **remembering the little human details**, not filling out CRM fields.

## Data model

```kotlin
enum class PersonFactSource { MANUAL, MEMORY, CALL_MOMENT, AI_SUGGESTION }

@Entity(
    tableName = "person_facts",
    foreignKeys = [ForeignKey(
        entity = Person::class,
        parentColumns = ["id"],
        childColumns = ["personId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("personId"), Index("key")],
)
data class PersonFact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personId: Long,
    val key: String,
    val value: String,
    val source: PersonFactSource = PersonFactSource.MANUAL,
    val sourceAnchorId: Long? = null,
    val pinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
```

Do not force a rigid schema. Preset keys may help, but custom `key/value` must remain available.

## Creation paths

Facts may be:

- entered manually
- promoted from a relationship memory
- suggested from a call transcript
- suggested by AI Wingman from already-approved context

AI never silently writes a person fact.

Example:

> Heard in your call with Marcus:  
> `My dog's name is Rocky.`  
> **Save to Little Things?** `Dog → Rocky`

## UX

On the person screen, show only a few useful facts by default.

Avoid a 40-field contact form.

Suggested structure:

**Little Things**

`🐶 Rocky` · `🍜 Thai food` · `💬 Prefers text`

Tap to expand/edit all.

## Integration

Little Things can feed:

- relationship/call brief
- pre-meeting brief
- Ask Tether
- Tether Cue
- AI Wingman
- gift/occasion prompts in the future

Do not automatically put every fact into every brief. Select only contextually useful facts.

## Acceptance criteria

- add/edit/delete fact quickly
- duplicate fact suggestions are de-duplicated or offered as updates
- AI/call-derived facts require confirmation
- source-linked facts retain their source anchor
- facts survive backup/export
- Ask Tether can search them

---

# Feature 18 — Best Time + Best Way to Reach

## Goal

Use real interaction history to suggest the channel and time window most likely to fit the relationship naturally.

Examples:

- `Chris · Best bet: text`
- `Mom · You usually connect by phone around 7–9 PM`
- `Marcus · Calls work better than messages`

This is descriptive guidance from Mike's own history, not a prediction about another person's psychology.

## Initial architecture

Start as a deterministic analytics engine. Do not make this AI-dependent.

Create a `ReachabilityPatternEngine` that summarizes:

- successful outgoing interactions by channel
- answered outgoing calls
- outgoing messages followed by meaningful continuation where detectable
- typical local-hour buckets for successful interactions
- recent vs stale history
- minimum sample size

## Confidence rules

Do not show a recommendation from one random event.

Initial rules should require a minimum useful history, for example:

- at least 3 successful events for a channel claim
- at least 4–5 events before suggesting a time window
- recent events weighted more heavily than very old events

If confidence is weak, simply show nothing.

## Time windows

Use broad human-readable buckets rather than fake precision:

- Morning
- Afternoon
- Early evening
- Evening
- Late night

If a strong pattern exists, a narrower display such as `7–9 PM` is acceptable.

## UX

Use this only where actionable:

Today’s Tethers:

> **Chris**  
> You owe a reply · Text usually works best

Person profile:

> **Usually works:** Text · evenings

Do not clutter every contact card with analytics.

## Acceptance criteria

- recommendations require minimum sample size
- no recommendation shown when evidence is weak
- timezone changes do not corrupt historical interpretation
- engine uses only actual Tether interaction metadata
- user can disable/hide reachability suggestions
- no claim is phrased as certainty

---

# Feature 19 — Only Group Contact Lately

## Goal

Preserve Mike's chosen rule that group-chat participation counts as effort while separately noticing when a close relationship has had **no direct one-to-one contact for a long time**.

Example:

> **Jordan**  
> You’ve interacted in the group, but haven’t talked 1:1 in 74 days.

This is a secondary relationship-context signal, not a replacement for the existing effort clock.

## Core logic

Track two derived clocks:

1. **overall effort clock** — existing authoritative rule; group participation may count
2. **direct-contact clock** — only qualifying one-to-one interactions and direct in-person contact

Do not change the existing `countsTowardTimer` behavior merely to build this feature.

Create a derived `DirectContactStatusEngine` over interaction history.

## When to surface

Only surface when all are true:

- the person is a relationship where direct contact reasonably matters
- overall effort is being maintained partly/entirely by group interactions
- direct contact exceeds a meaningful threshold
- the signal has not been dismissed/snoozed recently

Do not nag about casual acquaintances who are naturally group-only relationships.

Category/relationship settings may eventually allow `Direct contact matters` as an override.

## UX

Keep wording neutral:

`Only group contact lately`

or

`No 1:1 catch-up in 74 days`

Actions:

- Message
- Call
- Not important for this relationship
- Snooze

`Not important for this relationship` should suppress this signal for that person without changing normal tracking.

## Acceptance criteria

- group interaction continues to count according to existing relationship rules
- direct-contact status is calculated separately
- direct message/call resolves the signal
- group-only relationships can opt out permanently
- no duplicate notifications
- Ask Tether can answer `Who have I only talked to in groups lately?`

---

# Feature 20 — Tether Cue

## Goal

Surface tiny pieces of useful relationship context **at the exact communication moment**, so Mike does not have to remember to open Tether first.

Examples:

Incoming message from Marcus:

> **Marcus**  
> 💭 Ask how Friday’s interview went

Outgoing/incoming call with Mom:

> ❤️ Care Season · recovering from surgery  
> Last call Tuesday · ask if medication helped

Chris calls:

> 💬 You promised to send that apartment link

Tether Cue is the ambient delivery layer for context already known by Tether.

## Architecture

Create a `CueEngine` that selects **at most one or two** high-value context items for a person and situation.

Possible inputs:

- open follow-up
- Care Season
- reply debt
- pinned/recent relationship memory
- Little Things fact
- upcoming important date
- source-linked Call Moment
- long direct-contact gap

Possible contexts:

- incoming call
- outgoing call
- incoming direct-message notification when identity confidence is high
- user-initiated message/call action from Tether
- pre-meeting brief

## Ranking

Cue ranking must be deterministic and explainable.

Suggested priority order:

1. urgent/open follow-up relevant now
2. active Care Season
3. explicit promise/Call Moment
4. important date within 48 hours
5. recent pinned memory
6. relevant Little Thing
7. direct-contact-gap context

Never show more context just because more context exists.

## Message surfaces

For phone calls, reuse/extend Tether's call overlay rather than creating competing overlays.

For messaging notifications, only show a Tether Cue when Tether can confidently identify the person and Android permits a respectful non-invasive surface.

Do not scrape or replace the contents of private messages merely to manufacture a cue.

If Android restrictions make an overlay inappropriate, use a Tether notification/action that opens the brief instead.

## Privacy

Lock-screen behavior must be configurable:

- Hide context when locked
- Show person name only
- Show full cue

Default should avoid exposing sensitive notes on a locked screen.

## Fatigue control

- do not show the same cue repeatedly within a short period
- dismissing a cue suppresses that exact cue temporarily
- calls/messages should never become a wall of Tether overlays
- if nothing useful exists, show nothing

## Acceptance criteria

- CueEngine returns at most two concise context items
- call cue reuses the existing call-overlay architecture
- lock-screen privacy setting is respected
- repeated notifications do not spam the same cue
- cue works without AI
- cue never creates or completes a relationship interaction by itself

---

# Round 3 shared architecture changes

## Database additions

Add explicit Room migrations for:

- `care_seasons`
- `source_anchors`
- `person_facts`

Potential small preference/state storage may also be needed for:

- direct-contact-signal opt-out per person
- cue dismissal/cooldown state
- reachability suggestion preference

Do not persist derived analytics that can be cheaply and correctly recomputed unless profiling proves a cache is needed.

## Domain engines

Add dedicated domain logic, not Compose-screen math:

- `CareSeasonEngine`
- `ReachabilityPatternEngine`
- `DirectContactStatusEngine`
- `CueEngine`

Extend `TodayPriorityEngine` to consider:

- active Care Season
- long direct-contact gap when relevant

## Search / Ask Tether

Extend Ask Tether/search to support:

- Care Seasons
- Little Things facts
- source-linked Call Moments
- direct-contact status
- reachability patterns where useful

Example queries:

- `Who is in a Care Season right now?`
- `What is Marcus's dog's name?`
- `What did Mom say about her medication on our last call?`
- `Who have I only talked to in groups lately?`
- `What's the best way to reach Chris?`

## Call Recorder bridge

Feature 14's cross-app bridge should preserve enough stable identity to support Feature 16 source anchors.

Where transcript timestamps are available, store/return them through Tether's transcription pipeline.

Do not modify raw Call Recorder audio merely to create anchors.

## Today’s Tethers

Suggested priority inputs now also include:

- active Care Season: strong boost
- Care Season check-in due: stronger boost
- direct-contact gap: modest boost only when relationship settings say it matters

Avoid letting one person monopolize the daily queue because several signals fire at once. Collapse multiple reasons into a single person recommendation with the best 1–2 explanations.

---

# Round 3 implementation order

## Phase I — Durable person context

1. Add `PersonFact` + DAO/repository.
2. Add Little Things UI.
3. Add source-anchor model.
4. Allow memories/follow-ups/facts to retain source anchors.
5. Add source chip/navigation for Call Moments.

**Evidence:** migration tests, fact CRUD tests, source-navigation tests, person-screen screenshot.

## Phase J — Temporary care state

1. Add `CareSeason` entity/repository.
2. Add Care Season create/edit/end flow.
3. Integrate temporary cadence with reminder calculations.
4. Integrate active Care Seasons into Today’s Tethers and briefs.

**Evidence:** cadence restoration tests, end-date tests, Today’s Tethers ranking tests, active-season screenshot.

## Phase K — Relationship-pattern intelligence

1. Implement `ReachabilityPatternEngine`.
2. Implement `DirectContactStatusEngine`.
3. Add minimum-sample/confidence rules.
4. Add opt-out for direct-contact significance.
5. Surface concise suggestions on person profile / Today’s Tethers.

**Evidence:** synthetic-history unit tests, timezone tests, group-vs-direct regression tests.

## Phase L — Ambient Tether Cue

1. Implement `CueEngine`.
2. Reuse call overlay for call cues.
3. Add safe messaging cue path where Android allows it.
4. Add lock-screen privacy setting.
5. Add cooldown/dismissal behavior.
6. Integrate Care Seasons, follow-ups, Call Moments, Little Things, dates, and direct-contact gaps.

**Evidence:** cue-ranking tests, spam/cooldown tests, lock-screen/privacy states, Pixel screenshot review.

---

# Round 3 edge cases

Explicitly test:

- overlapping Care Seasons for one person
- Care Season with no end date
- Care Season ending while reminder is snoozed
- accepted call fact after original recording is deleted
- transcript lacks timestamps
- transcript timestamp is approximate
- duplicate Little Things suggestion with changed value
- contradictory facts (`Dog: Rocky` then `Dog: Milo`)
- multiple phone/message channels with different usage patterns
- too little history for reachability suggestion
- daylight-saving/timezone change
- group chat plus direct message on same day
- group-only contact category that should suppress direct-contact warnings
- cue generated while screen is locked
- multiple simultaneous possible cues
- repeated notifications from same person
- Call Recorder absent/unavailable
- AI disabled

---

# Round 3 guardrails

## Keep

- Liquid Glass identity
- relationship context over CRM administration
- explainable deterministic core behavior
- explicit confirmation for AI-extracted facts
- Call Recorder as a separate recording app
- existing relationship-counting rules

## Avoid

- permanently changing cadence for temporary life situations
- assuming a medical/grief/life event is over because one contact happened
- treating AI transcript extraction as fact without confirmation
- storing raw audio in Tether
- fake precision about `best time to call`
- guilt around direct-contact gaps
- nagging group-only relationships that do not need 1:1 contact
- exposing sensitive cues on the lock screen by default
- showing a cue when Tether has nothing genuinely useful to add

---

# Round 3 definition of done

Each feature requires:

1. migrations and migration tests where data is persisted
2. deterministic domain tests
3. empty/failure/permission states
4. representative Pixel render/screenshots
5. accessibility basics
6. backup/export impact handled
7. Ask Tether/search impact handled where relevant
8. Call Recorder degradation path handled where relevant
9. docs updated to match behavior
10. no regression to existing relationship-counting rules
