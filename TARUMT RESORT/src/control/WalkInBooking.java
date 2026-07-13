package control;

import boundary.WalkInBookingUI;
import utility.MessageUI;

/**
 * Control class stub for Walk-In Registrations & Standard Booking (team member integration point).
 *
 * @author Your Name
 */
public class WalkInBooking {

  private final WalkInBookingUI bookingUI = new WalkInBookingUI();

  public void runWalkInBookingModule() {
    int choice;
    do {
      choice = bookingUI.getMenuChoice();
      switch (choice) {
        case 0:
          MessageUI.displayInfoMessage("Returning to main menu...");
          break;
        case 1:
          bookingUI.inputGuestName();
          bookingUI.inputContactNumber();
          bookingUI.inputRoomType();
          MessageUI.displaySuccessMessage(
              "[Prototype] Walk-in registered. (Integrate with team's linear queue ADT)");
          MessageUI.pressEnterToContinue();
          break;
        case 2:
          bookingUI.inputGuestName();
          bookingUI.inputRoomType();
          bookingUI.inputNumberOfNights();
          bookingUI.displayBookingConfirmation(
              "  [Prototype] Standard booking created - awaiting team integration.");
          MessageUI.pressEnterToContinue();
          break;
        case 3:
          bookingUI.displayBookingQueue("");
          MessageUI.pressEnterToContinue();
          break;
        case 4:
          MessageUI.displayInfoMessage(
              "[Prototype] Process next booking - to be implemented by team member.");
          MessageUI.pressEnterToContinue();
          break;
        case 5:
          bookingUI.inputBookingId();
          MessageUI.displaySuccessMessage(
              "[Prototype] Booking cancellation - to be implemented by team member.");
          MessageUI.pressEnterToContinue();
          break;
        default:
          MessageUI.displayInvalidChoiceMessage();
      }
    } while (choice != 0);
  }
}
