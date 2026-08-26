TARUMT Resorts Management System
================================

BMCS2063 Data Structures & Algorithms - Console Prototype

HOW TO RUN (VS Code - recommended, no JDK setup needed)
---------------------------------------------------------
1. Open the "TARUMT RESORT" folder in VS Code (File -> Open Folder).
2. The Java extension auto-detects the installed JDK, so you do nothing
   for the JDK - it is already installed and configured on this machine.
3. Open src\control\TarumtResortsSystem.java.
4. Click the "Run" link that appears directly above  main(String[] args)
   (a blue arrow). Or press F5.

HOW TO RUN (command prompt, from the TARUMT RESORT folder)
----------------------------------------------------------
  javac --release 8 -encoding UTF-8 -cp lib\pdfbox-app-3.0.3.jar -d build\classes src\adt\*.java src\boundary\*.java src\control\*.java src\dao\*.java src\entity\*.java src\utility\*.java
  java -Dfile.encoding=UTF-8 -cp build\classes;lib\pdfbox-app-3.0.3.jar control.TarumtResortsSystem

Note: Java programs need a JVM to run, so a JDK (or JRE) is always
required on the machine. The only JDK-independent alternative would be to
package the program into a .exe with an embedded JRE, which is not needed
here because a JDK is already installed.

MODULES
-------
1. VIP & Loyalty Tier Priority Room Allocation (Non-Linear ADT)
2. Housekeeping & Task Log (Linear ADT)
   - Two-stack undo/redo for room-status changes
   - Bulk and room-specific LIFO rollback
   - Stack history preview and undo/redo statistics
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
- data\redo_history.txt
- data\products.txt
- data\loyalty_members.txt

SAMPLE ROOMS (seeded on first run)
----------------------------------
R101, R102, R201, R301, R302

AUTHOR
------
Replace @author Your Name in each class with your actual name before submission.
