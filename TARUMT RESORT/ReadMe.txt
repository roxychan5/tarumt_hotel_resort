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

DATA FILES (readable text files, auto-created on first save)
--------------------------------------
- data\rooms.txt
- data\housekeeping_tasks.txt
- data\status_history.txt
- data\guest_records.txt
- data\products.txt

SAMPLE ROOMS (seeded on first run)
----------------------------------
R101, R102, R201, R301, R302

AUTHOR
------
Replace @author Your Name in each class with your actual name before submission.
