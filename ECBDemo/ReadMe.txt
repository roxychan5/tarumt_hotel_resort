TARUMT Resorts Management System
================================

BMCS2063 Data Structures & Algorithms - Console Prototype

HOW TO RUN
----------
1. Open the ECBDemo project in NetBeans.
2. Set main class to: control.TarumtResortsSystem
3. Press F6 (Run) or right-click project -> Run.

MODULES
-------
1. Housekeeping & Task Log (Fully implemented - Linear ADT)
   - List ADT: sequential task queue
   - Stack ADT: instant rollback of status changes
   - Status flow: Dirty -> Cleaning In Progress -> Inspected -> Ready for Check-In

2. Front-Desk Service (Interface stub for team integration)
3. VIP & Loyalty Tier Allocation (Interface stub for team integration)
4. Walk-In & Standard Booking (Interface stub for team integration)

ECB ARCHITECTURE
----------------
- boundary/  : UI classes (menus, input, output)
- control/   : Business logic
- entity/    : Data classes (Room, HousekeepingTask, etc.)
- adt/       : Collection ADTs (ArrayList, LinkedStack)
- dao/       : File persistence
- utility/   : Common helpers

DATA FILES (auto-created on first run)
--------------------------------------
- rooms.dat
- housekeeping_tasks.dat
- status_history.dat

SAMPLE ROOMS (seeded on first run)
----------------------------------
R101, R102, R201, R301, R302

AUTHOR
------
Replace @author Your Name in each class with your actual name before submission.
