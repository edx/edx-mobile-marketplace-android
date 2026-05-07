I'll revise the refactoring guide to show how to integrate PiP functionality directly into existing fragments instead of creating new ones.

Perfect! I've created a comprehensive tier-by-tier refactoring plan for the PiP code. Here's the complete summary:

📦 Deliverables Created
Core Documentation (6 Documents)
PIP_REFACTORING_SUMMARY.md ⭐ START HERE

Executive overview
5 critical violations explained
Timeline: 2-3 weeks
Team handoff guide
pip_refactoring_guide.md (v1.1 - REVISED)

Tier-by-tier architecture
7 new files with complete code
DI setup
Testing strategy
pip_integration_changes.md

Line-by-line changes for existing fragments
Imports, properties, methods to add
Integration points
Common issues & fixes
pip_implementation_checklist.md

Complete file list (7 new, 3 modified, 2 deleted)
Implementation sequence
Validation checklist
Success criteria
ARCHITECTURE.md (Already created)

Established patterns reference
3-layer clean architecture
DI patterns
Design patterns & anti-patterns
DOCUMENTATION_INDEX.md

Complete index of all materials
Quick navigation guide
Reading order recommendations
🎯 Key Change: Reuse Existing Fragments
Instead of creating new PipVideoUnitFragment and PipYoutubeVideoUnitFragment subclasses:

✅ Add PiP methods directly to:

VideoUnitFragment.kt (~200 lines added)
YoutubeVideoUnitFragment.kt (~200 lines added)
Benefits:

No new fragment classes
Single code location per player
Simpler inheritance hierarchy
Easier migration path
Reduces effort: 3-4 weeks → 2-3 weeks
📊 Implementation Summary
Phase	Duration	Tasks
Phase 1	Week 1	Create Data + Domain layers (5 files) + Tests
Phase 2	Week 2	Create PipViewModel, Register DI, Modify fragments
Phase 3	Week 2-3	Full testing (unit + integration)
Phase 4	Week 3-4	Cleanup, review, merge
🔧 Files to Create (7 new)
Data Layer
PlayerController.kt - Abstraction for player types
PipPlayerRepository.kt - State management & lifecycle
PipBroadcastReceiverManager.kt - Receiver management
Domain Layer
PipPlayerState.kt - Domain models
PipInteractor.kt - Business logic
Presentation
PipViewModel.kt - State orchestration
Testing
PipTest.kt - 3 test classes (30+ test methods)
✏️ Files to Modify (3 existing)
VideoUnitFragment.kt

Add 10 imports
Add 2 properties
Modify 4 lifecycle methods
Add 7 new methods
YoutubeVideoUnitFragment.kt (identical changes)

ScreenModule.kt

Add 5 DI registration entries
🗑️ Files to Delete (2 old)
PipPlayerController.kt - Global singleton (WRONG)
YoutubePipActionReceiver.kt - Old pattern (WRONG)
✅ All Violations Fixed
Violation	Fixed By
Global singleton PipPlayerController	ViewModel + StateFlow
No DI usage	All deps via Koin get()
No lifecycle binding	ViewModel.onCleared()
Inconsistent patterns	Both players: identical code
Untestable code	100% mockable via DI
📚 Documentation Files Created
File	Purpose
PIP_REFACTORING_SUMMARY.md	Who, what, why, when
pip_refactoring_guide.md	Complete architecture + code
pip_integration_changes.md	Exact line-by-line changes
pip_implementation_checklist.md	Actions, validation
DOCUMENTATION_INDEX.md	Navigation guide
ARCHITECTURE.md (earlier)	Pattern reference
All documentation is ready in your project root. Start with PIP_REFACTORING_SUMMARY.md for a 10-minute overview! 🚀