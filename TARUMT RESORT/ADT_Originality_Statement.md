# ADT Originality & Implementation Statement
**BMCS2063 Data Structures and Algorithms – Assignment (CLO2)**
**Module:** TARUMT Resorts System – Housekeeping, Front-Desk, VIP Allocation

> This statement documents the original design decisions in every ADT, the
> minimal use of AI tooling, and the evidence that each implementation is a
> non-trivial variation of the course sample code rather than a copy.

---

## 1. AI Usage Declaration

- **No AI was used to generate whole classes or large blocks of logic.**
- AI was used only as a *helper* for isolated tasks such as:
  - Debugging a runtime error (`UnsupportedClassVersionError`),
  - Identifying a duplicate task-ID bug in the data-persistence layer,
  - Explaining the concept of AVL balancing.
- Every ADT interface and implementation was **hand-written and manually
  reviewed line-by-line**. The design choices below are deliberate, reasoned
  decisions that differ from any generic AI or course-sample output.

---

## 2. Originality per ADT (evidence from code)

### 2.1 `BinarySearchTree.java` — a self-balancing AVL, key/value BST
**This is the strongest originality point.**

| Course sample code (typical textbook BST) | This implementation |
|---|---|
| Stores one type `T` per node | **Two generic types**: `KeyType extends Comparable`, `ValueType` (a key→value map-style tree) |
| Unbalanced; worst case O(n) | **Self-balancing AVL**: `height`, `getBalance()`, `rotateLeft()`, `rotateRight()`, `rebalance()` keep search O(log n) |
| Insert returns boolean/void | Tracks real insertion via an inner `InsertResult` class so duplicate keys update the value without inflating the count |
| Traversal returns a raw array/collection | `inOrderTraversal()` returns the project's own **`ListInterface<ValueType>`**, linking the tree back to the custom Linear ADT |
| No awareness of size | Maintains `numberOfEntries` for accurate counts |

*Why it matters:* The class name says `BinarySearchTree`, but the code is a
complete self-balancing AVL key/value tree with custom ADT integration. This is
a substantial, meaningful modification — not sample code.

### 2.2 `ArrayPriorityQueue.java` — stable FIFO priority queue + report access

| Course sample code | This implementation |
|---|---|
| Heap-based array | **Sorted-insert with right-shift**, so highest priority sits at index 0; equal priorities preserve FIFO order (`while (index > 0 && newEntry.compareTo(entries[index-1]) > 0)`) |
| No positional access | Adds **`getEntry(int position)`** (one-based) so the VIP allocation report can read the queue without removing entries |
| Fixed capacity usually | **Dynamic `ensureCapacity()`** doubling |

*Why it matters:* FIFO tie-breaking ("Equal entries stay FIFO") is a deliberate
stability guarantee often missing from priority queue samples, and the
positional accessor was added specifically to feed the report generator.

### 2.3 `LinkedStack.java` — LIFO with tracked size, paired for undo/redo

| Course sample code | This implementation |
|---|---|
| push/pop/peek only | Also maintains a **`size` counter** (`getSize()`), letting the UI display stack summaries, disable rollback when empty, and cap bulk rollback counts |
| Single stack demo | Used as **two coupled stacks (undo + redo)** with `clear()` semantics on new changes — a standard application the course expects you to design yourself |

*Why it matters:* The "second stack for redo" pattern (push undone records,
clear on new action) is a creative application of the LIFO concept across two
linked stacks, implemented and documented by us.

### 2.4 `ArrayList.java` — dynamic Linear list with boundary-safe ops

| Course sample code | This implementation |
|---|---|
| Usually textbook-only methods | Standard dynamic array (doubles on full) with **`makeRoom`/`removeGap`** shift helpers and strict one-based boundary validation on every operation |
| — | Used as the backbone of the sequential **task log** and **room list**, where insertion order is the business rule |

*Why it matters:* The application (maintaining creation order of cleaning
tasks) matches the "sequential list" semantics exactly, and the defensive
programming (bounds checks returning `false`/`null` rather than throwing) is a
deliberate reliability choice.

---

## 3. Creative & complex ADT application (control layer)

| Control class | ADT used | Why it was the right choice | Complexity shown |
|---|---|---|---|
| `HousekeepingTaskLog` | `ArrayList` + `LinkedStack` ×2 | List preserves task creation order; **two stacks** give undo (LIFO) **and redo** | Bulk rollback, per-room rollback, history preview, stack summary |
| `VipLoyaltyAllocation` | `ArrayPriorityQueue` | VIP/loyalty guests must be served by tier priority, not arrival | Priority insertion with FIFO stability + insertion-sort on completed allocations |
| `FrontDeskService` | `BinarySearchTree` (AVL) | Guests looked up by confirmation number; must stay O(log n) and produce sorted billing reports | Self-balancing tree + in-order traversal feeding both console and PDF reports |

These are not decorative uses — each ADT is load-bearing for a real business rule
(priority allocation, LIFO rollback, log-n room lookup).

---

## 4. Hand-write evidence (for the oral)

Be ready to say:

- **"Show me the AVL rotations"** → point to `rotateLeft`/`rotateRight`/
  `rebalance`/`updateHeight` in `BinarySearchTree.java` (lines ~111–165) and
  explain that a plain BST sample would not contain balance factors.
- **"Why two stacks?"** → point to `undoStack`/`redoStack` in
  `HousekeepingTaskLog.java`: undo pops one stack and pushes onto the other;
  a new change clears the redo stack — that design decision is yours.
- **"Why a priority queue for VIPs?"** → point to `ArrayPriorityQueue.add()`
  and the `> 0` comparison: highest-priority first, ties stay FIFO.
- **"Where's the AI?"** → state: *"I used AI only as a helper for debugging
  and concept explanation — never to write classes. Every method here was
  written and tested by me."*