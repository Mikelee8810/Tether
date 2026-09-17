# RelationshipRadar Implementation Plan

> **Next-wave product work:** See [`docs/tether-product-evolution-roadmap.md`](tether-product-evolution-roadmap.md) for the approved roadmap covering voice memory capture, contextual follow-ups, Today's Tethers, Orbit constellation mapping, richer relationship briefs/call overlay, actionable widgets, and ADHD Rescue Mode.

## Phase 1 — Android Foundation
- Native Android app (Kotlin + Jetpack Compose)
- Local-first encrypted storage
- Shizuku integration layer
- Permission onboarding

## Phase 2 — Data Collection
- Contacts
- Call history
- SMS/MMS
- Notification listener framework
- Calendar
- Manual interactions

## Phase 3 — Relationship Engine
- Contact categories
- Reminder schedules
- Snooze
- Pause
- Archive/restore
- Merge suggestions

## Phase 4 — Communication Connectors
- Messenger
- WhatsApp
- Instagram
- Telegram
- Signal
- Discord
- Future connectors through modular system

## Phase 5 — UI Capability
- Relationship dashboard
- Contact profiles
- Interaction timeline
- Quick in-person log
- Settings

**Phase 5 means the UI exists. It does not mean the product is visually or operationally finished.**

## Phase 6 — Visual Direction & Product Identity
- Study strong real-product references
- Lock hierarchy, typography, spacing, color roles, surfaces, imagery/avatars, motion, and navigation principles
- Define explicit anti-patterns
- Define representative screens and states
- Build and render one representative screen first
- Review screenshot quality before scaling the system across the app
- Run a final rendered-screen audit after the redesign

## Phase 7 — Product Flow & UI Testing
- Repository/database integration tests
- Connector integration tests
- Compose UI/instrumentation tests for critical flows
- End-to-end tests for the core relationship-maintenance journeys
- Screenshot/golden regression tests for representative visual states
- Regression tests for fixed production defects

## Phase 8 — Data, Security & Privacy Hardening
- Reconcile the encrypted-storage promise with the actual database implementation
- Verify Room migrations and failure recovery
- Define backup, restore, export, and corruption-recovery behavior
- Audit sensitive permissions and exported Android components
- Audit notification-listener and Shizuku boundaries
- Verify that private message content is not retained where the product promises metadata-only handling

## Phase 9 — Performance & Accessibility
- Test release-mode startup and common journeys
- Review R8/minification/shrinking configuration
- Add Baseline Profile / startup optimization where useful
- Check scrolling, animation jank, memory, background work, battery, crash/ANR risk, process death, and reboot recovery
- Test TalkBack, content descriptions, contrast, touch targets, large fonts, light/dark, and dynamic color

## Phase 10 — Release Gate
- CI builds and tests every change
- Production-quality gate completed with evidence
- Major screens reviewed from actual screenshots
- Real representative Pixel-device verification after emulator testing and explicit authorization
- Signing/versioning/release notes/recovery path documented
- No unresolved critical privacy, data-loss, reliability, or visual-quality blockers

See `docs/production-quality-gate.md`.

## Definition of done

No phase is complete because an agent says it is. Record evidence: tests, screenshots, device runs, benchmarks, checked-in artifacts, or explicit review results.

Use the labels **Prototype → Feature-complete → Beta-ready → Production-ready** accurately.