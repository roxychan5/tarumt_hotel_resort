TARUMT Resorts Management System
================================

BMCS2063 Data Structures & Algorithms - Console Prototype

HOW TO RUN
----------
Option A (Windows): double-click run.bat in the TARUMT RESORT folder.

Option B (command prompt, from the TARUMT RESORT folder):
  javac -d build\classes src\adt\*.java src\boundary\*.java src\control\*.java src\dao\*.java src\entity\*.java src\utility\*.java
  java -cp build\classes control.TarumtResortsSystem

Option C (NetBeans): open the TARUMT RESORT folder as the project, set the
main class to control.TarumtResortsSystem, then press F6.

You need a JDK (not only a JRE). The program does not require Apache Ant to
run by using Option A or B.

MODULES
-------
1. VIP & Loyalty Tier Priority Room Allocation (Non-Linear ADT)
2. Housekeeping & Task Log (Linear ADT)
   - List ADT: sequential task queue
   - Stack ADT: instant rollback of status changes
   - Status flow: Dirty -> Cleaning In Progress -> Inspected -> Ready for Check-In

3. Front-Desk Service (Non-Linear ADT & Searching)
4. Loyalty & Rewards Service
   - Member profiles and personalised promotions
   - Points accumulation, redemption and tier progression
   - Notifications for expiring points and tier upgrades

ECB ARCHITECTURE
----------------
- boundary/  : UI classes (menus, input, output)
- control/   : Business logic
- entity/    : Data classes (Room, HousekeepingTask, etc.)
- adt/       : Collection ADTs (ArrayList, LinkedStack)
- dao/       : File persistence
- utility/   : Common helpers

DATA FILES (readable text files, auto-created on first save)
--------------------------------------
- data\rooms.txt
- data\housekeeping_tasks.txt
- data\status_history.txt
- data\guest_records.txt
- data\products.txt
- data\loyalty_members.txt
- data\standard_bookings.txt
- data\walk_in_guests.txt

SAMPLE ROOMS (seeded on first run)
----------------------------------
R101, R102, R201, R301, R302

AUTHOR
------
Replace @author Your Name in each class with your actual name before submission.
