package com.roadregistry;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Person class for the RoadRegistry platform. Provides:
 *   1. addPerson()             — validates instance fields, appends a new record to persons.txt.
 *   2. updatePersonalDetails() — updates an existing record in persons.txt according to rules.
 *   3. addDemeritPoints()      — adds demerit points (from the demeritPoints map) to demerits.txt,
 *                                and updates the suspension flag in persons.txt if thresholds are exceeded.
 *
 * Text files:
 *   - persons.txt:  personID,firstName,lastName,address,birthdate,demeritTotal,isSuspended
 *   - demerits.txt: personID,offenseDate,points
 *
 * NOTE: All dates are in "dd-MM-yyyy" format (e.g. "15-11-1990").
 */
public class Person {

    // Instance fields
    private String personID;
    private String firstName;
    private String lastName;
    private String address;
    private String birthdate; // Format "dd-mm-yyyy"
    private HashMap<Date, Integer> demeritPoints; // A variable that holds the demerit points with the offence day.
    private boolean isSuspended;

    // File paths
    private static final String PERSON_FILE = "persons.txt";
    private static final String DEMERIT_FILE = "demerits.txt";

    // Date formatter
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");



    //Default constructor. Initializes empty demeritPoints map.
    public Person() {
        this.demeritPoints = new HashMap<>();
        this.isSuspended = false;
    }
    /**
     * Required Parameters
     * @param personID   Exactly 10 chars; first two digits 2–9; at least two special chars in positions 3–8; last two uppercase.
     * @param firstName  Non-null.
     * @param lastName   Non-null.
     * @param address    "StreetNumber|Street|City|State|Country", where State must be "Victoria".
     * @param birthdate  "DD-MM-YYYY" format.
     */
    public Person(String personID,
                  String firstName,
                  String lastName,
                  String address,
                  String birthdate) {
        this.personID = personID;
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.birthdate = birthdate;
        this.demeritPoints = new HashMap<>();
        this.isSuspended = false;
    }
    /**
     //TODO: This method adds information about a person to a TXT file.
     //Condition 1: PersonID should be exactly 10 characters long;
     //the first two characters should be numbers between 2 and 9, there should be at least two special characters between characters 3 and 8,
     //and the last two characters should be upper case letters (A – Z). Example: "56s_d%&fAB"
     //Condition 2: The address of the Person should follow the following format: Street Number|Street|City|State|Country.
     //The State should be only Victoria. Example: 32|Highland Street|Melbourne|Victoria|Australia.
     //Condition 3: The format of the birth date of the person should follow the following format: DD-MM-YYYY. Example: 15-11-1990
     //Instruction: If the Person's information meets the above conditions and any other conditions you may want to consider,
     //the information should be inserted into a TXT file, and the addPerson function should return true.
     //Otherwise, the information should not be inserted into the TXT file, and the addPerson function should return false.
     * @return true if the person was successfully added; false otherwise.
     */
    public boolean addPerson() {
        // Validate personID
        if (!isValidPersonID(this.personID)) {
            return false;
        }

        // Validate address
        if (!isValidAddress(this.address)) {
            return false;
        }

        // Validate birthdate format
        Date birthDateObj = parseDate(this.birthdate);
        if (birthDateObj == null) {
            return false;
        }

        // Ensure persons.txt exists
        ensureFileExists(PERSON_FILE);

        // Build record: initially demeritTotal = 0 and isSuspended = false
        String record = String.join(",",
                this.personID,
                this.firstName,
                this.lastName,
                this.address,
                this.birthdate,
                "0",
                "false"
        );

        // Append to persons.txt
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PERSON_FILE, true))) {
            writer.write(record);
            writer.newLine();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    /**
     //TODO: This method allows updating a given person's ID, firstName, lastName, address and birthday in a TXT file.
     //Changing personal details will not affect their demerit points or the suspension status.
     // All relevant conditions discussed for the addPerson function also need to be considered and checked in the updatePerson function.
     //Condition 1: If a person is under 18, their address cannot be changed.
     //Condition 2: If a person's birthday is going to be changed, then no other personal detail (i.e, person's ID, firstName, lastName, address) can be changed.
     //Condition 3: If the first character/digit of a person's ID is an even number, then their ID cannot be changed.
     //Instruction: If the Person's updated information meets the above conditions and any other conditions you may want to consider,
     //the Person's information should be updated in the TXT file with the updated information, and the updatePersonalDetails function should return true.
     //Otherwise, the Person's updated information should not be updated in the TXT file, and the updatePersonalDetails function should return false.
     * @return true if the update was applied; false if any rule was violated or I/O error occurred.
     */
    public boolean updatePersonalDetails() {
        // Ensure persons.txt exists
        ensureFileExists(PERSON_FILE);

        List<String> allLines = new ArrayList<>();
        boolean updated = false;

        // Read all existing lines
        try (BufferedReader reader = new BufferedReader(new FileReader(PERSON_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                allLines.add(line);
            }
        } catch (IOException e) {
            return false;
        }

        List<String> updatedLines = new ArrayList<>();

        for (String line : allLines) {
            String[] part = line.split(",", -1);
            if (part.length < 7) {
                // Malformed line: keep as-is
                updatedLines.add(line);
                continue;
            }

            String existingID = part[0];
            String existingFirst = part[1];
            String existingLast = part[2];
            String existingAddress = part[3];
            String existingBirth = part[4];
            String existingDemeritTotal = part[5];
            String existingSuspended = part[6];

            if (!existingID.equals(this.personID)) {
                // Not the record we want to update
                updatedLines.add(line);
                continue;
            }

            // We found the matching record. Apply update rules.

            // Parse existing birthdate and compute age
            Date origBirthDate = parseDate(existingBirth);
            if (origBirthDate == null) {
                // Malformed birthdate: skip updating
                updatedLines.add(line);
                continue;
            }

            int age = calculateAgeYears(origBirthDate, new Date());

            // Check if birthdate is changing
            boolean birthChanged = !existingBirth.equals(this.birthdate);

            //Condition 2: If a person's birthday is going to be changed, then no other personal detail (i.e, person's ID, firstName, lastName, address) can be changed.
            if (birthChanged) {
                // Check that only birthdate differs; firstName, lastName, address, and personID must be identical
                if (!existingFirst.equals(this.firstName)
                        || !existingLast.equals(this.lastName)
                        || !existingAddress.equals(this.address)
                        || !existingID.equals(this.personID)) {
                    // Violation: cannot change other fields
                    updatedLines.add(line);
                    continue;
                }
                // Validate new birthdate format
                if (parseDate(this.birthdate) == null) {
                    updatedLines.add(line);
                    continue;
                }
            }

            //Condition 1: If a person is under 18, their address cannot be changed.
            if (!existingAddress.equals(this.address) && age < 18) {
                updatedLines.add(line);
                continue;
            }

            //Condition 3: If the first character/digit of a person's ID is an even number, then their ID cannot be changed.
            char firstChar = existingID.charAt(0);
            if (Character.isDigit(firstChar) && ((firstChar - '0') % 2 == 0)) {
                // If they are trying to change ID...
                if (!existingID.equals(this.personID)) {
                    updatedLines.add(line);
                    continue;
                }
            }

            // Validate new fields (non-null, non-empty)
            if (this.firstName == null || this.firstName.isBlank()
                    || this.lastName == null || this.lastName.isBlank()) {
                updatedLines.add(line);
                continue;
            }

            // If address changed, validate its format
            if (!existingAddress.equals(this.address) && !isValidAddress(this.address)) {
                updatedLines.add(line);
                continue;
            }

            // If ID changed, validate its format
            if (!existingID.equals(this.personID) && !isValidPersonID(this.personID)) {
                updatedLines.add(line);
                continue;
            }

            // All checks passed → construct updated line
            String newID = this.personID;
            String newFirst = this.firstName;
            String newLast = this.lastName;
            String newAddress = this.address;
            String newBirth = birthChanged ? this.birthdate : existingBirth;
            String newDemeritTotal = existingDemeritTotal;
            String newSuspended = existingSuspended;

            String updatedRecord = String.join(",",
                    newID,
                    newFirst,
                    newLast,
                    newAddress,
                    newBirth,
                    newDemeritTotal,
                    newSuspended
            );

            updatedLines.add(updatedRecord);
            updated = true;
        }

        // If an update was applied, overwrite persons.txt
        if (updated) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(PERSON_FILE, false))) {
                for (String outLine : updatedLines) {
                    writer.write(outLine);
                    writer.newLine();
                }
            } catch (IOException e) {
                return false;
            }
        }

        return updated;
    }

    /**
     //TODO: This method adds demerit points for a given person in a TXT file.
     //Condition 1: The format of the date of the offense should follow the following format: DD-MM-YYYY. Example: 15-11-1990
     //Condition 2: The demerit points must be a whole number between 1-6
     //Condition 3: If the person is under 21, the isSuspended variable should be set to true if the total demerit points within two years exceed 6.
     //If the person is over 21, the isSuspended variable should be set to true if the total demerit points within two years exceed 12.
     //Instruction: If the above conditions and any other conditions you may want to consider are met, the demerit points for a person should be inserted into the TXT file,
     //and the addDemeritPoints function should return "Sucess". Otherwise, the addDemeritPoints function should return "Failed".
     * @return "Success" or "Failed"
     */
    public String addDemeritPoints() {
        // Ensure both files exist
        ensureFileExists(PERSON_FILE);
        ensureFileExists(DEMERIT_FILE);

        // Validate that this.personID exists in persons.txt and retrieve original birthdate
        String originalBirth = null;
        List<String> personLines = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(PERSON_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                personLines.add(line);
            }
        } catch (IOException e) {
            return "Failed";
        }

        boolean foundPerson = false;
        String originalDemeritTotalStr = null;
        String originalSuspendedStr = null;

        for (String line : personLines) {
            String[] part = line.split(",", -1);
            if (part.length < 7) continue;
            if (part[0].equals(this.personID)) {
                foundPerson = true;
                originalBirth = part[4];
                originalDemeritTotalStr = part[5];
                originalSuspendedStr = part[6];
                break;
            }
        }
        if (!foundPerson) {
            return "Failed"; // No such person
        }

        // Parse original birthdate and compute age
        Date origBirthDate = parseDate(originalBirth);
        if (origBirthDate == null) {
            return "Failed";
        }
        int age = calculateAgeYears(origBirthDate, new Date());

        // Append each entry in this.demeritPoints to demerits.txt
        for (Map.Entry<Date, Integer> entry : this.demeritPoints.entrySet()) {
            Date offenseDate = entry.getKey();
            Integer pts = entry.getValue();

            if (offenseDate == null) {
                return "Failed";
            }
            //Condition 2: The demerit points must be a whole number between 1-6
            if (pts == null || pts < 1 || pts > 6) {
                return "Failed";
            }

            //Condition 1: The format of the date of the offense should follow the following format: DD-MM-YYYY. Example: 15-11-1990
            String offenseDateStr;
            try {
                offenseDateStr = DATE_FORMAT.format(offenseDate);
            } catch (NullPointerException ex) {
                // (Just in case another null appears unexpectedly)
                return "Failed";
            }

            // Write line: personID,offenseDate,points
            String demeritRecord = String.join(",",
                    this.personID,
                    offenseDateStr,
                    pts.toString()
            );
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(DEMERIT_FILE, true))) {
                writer.write(demeritRecord);
                writer.newLine();
            } catch (IOException e) {
                return "Failed";
            }
        }

        // Now compute total points within past 2 years for this.personID
        Date twoYearsAgo = subtractYears(new Date(), 2);
        int totalPoints = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(DEMERIT_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] part = line.split(",", -1);
                if (part.length < 3) continue;
                if (!part[0].equals(this.personID)) continue;
                Date offenseDate = parseDate(part[1]);
                if (offenseDate == null) continue;
                int pts = Integer.parseInt(part[2]);
                if (!offenseDate.before(twoYearsAgo)) {
                    totalPoints += pts;
                }
            }
        } catch (IOException e) {
            return "Failed";
        }

        //Condition 3: If the person is under 21, the isSuspended variable should be set to true if the total demerit points within two years exceed 6.
        //If the person is over 21, the isSuspended variable should be set to true if the total demerit points within two years exceed 12.
        boolean shouldSuspend = false;
        if (age < 21) {
            if (totalPoints > 6) {
                shouldSuspend = true;
            }
        } else {
            if (totalPoints > 12) {
                shouldSuspend = true;
            }
        }

        // Update suspension flag and total in persons.txt
        List<String> newPersonLines = new ArrayList<>();
        for (String line : personLines) {
            String[] part = line.split(",", -1);
            if (part.length < 7) {
                newPersonLines.add(line);
                continue;
            }
            if (part[0].equals(this.personID)) {
                String updatedRecord = String.join(",",
                        part[0],                         // personID
                        part[1],                         // firstName
                        part[2],                         // lastName
                        part[3],                         // address
                        part[4],                         // birthdate
                        String.valueOf(totalPoints),       // new total
                        String.valueOf(shouldSuspend)      // new suspended
                );
                newPersonLines.add(updatedRecord);
            } else {
                newPersonLines.add(line);
            }
        }

        // Overwrite persons.txt
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PERSON_FILE, false))) {
            for (String outLine : newPersonLines) {
                writer.write(outLine);
                writer.newLine();
            }
        } catch (IOException e) {
            return "Failed";
        }

        // Reflect suspension status in this object
        this.isSuspended = shouldSuspend;
        return "Success";
    }

    // ------------------------------------
    // Condition Validation methods
    // ------------------------------------
    /**
     * Validates personID:
     *   - Makes sure it is exactly 10 chars.
     *   - First two characters are digits 2–9.
     *   - At least two special (non-alphanumeric) chars between positions 3–8 inclusive.
     *   - Last two chars are uppercase letters (A–Z).
     */
    private boolean isValidPersonID(String id) {
        if (id == null || id.length() != 10) { //Makes sure it is exactly 10 chars or not null.
            return false;
        }
        // Check first two digits 2–9
        char c0 = id.charAt(0), c1 = id.charAt(1);
        if (!Character.isDigit(c0) || !Character.isDigit(c1)) {
            return false;
        }
        if (c0 < '2' || c0 > '9' || c1 < '2' || c1 > '9') {
            return false;
        }
        // Count special chars in positions 3–8
        int specialCount = 0;
        for (int i = 2; i <= 7; i++) { //includes 0
            char c = id.charAt(i);
            if (!Character.isLetterOrDigit(c)) {
                specialCount++;
            }
        }
        if (specialCount < 2) {
            return false;
        }
        // Last two characters must be uppercase letters
        char c8 = id.charAt(8), c9 = id.charAt(9);
        if (!Character.isUpperCase(c8) || !Character.isUpperCase(c9)) {
            return false;
        }
        return true;
    }

    //Condition 2: The address of the Person should follow the following format: Street Number|Street|City|State|Country.
    //The State should be only Victoria. Example: 32|Highland Street|Melbourne|Victoria|Australia.

    private boolean isValidAddress(String addr) {
        if (addr == null) {
            return false;
        }
        String[] parts = addr.split("\\|", -1);
        if (parts.length != 5) {
            return false;
        }
        String state = parts[3];
        return "Victoria".equals(state);
    }

    //Condition 3: The format of the birth date of the person should follow the following format: DD-MM-YYYY. Example: 15-11-1990
    private Date parseDate(String dateStr) {
        try {
            DATE_FORMAT.setLenient(false);
            return DATE_FORMAT.parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }

    /**
     * Calculates age in whole years between birthDate and referenceDate.
     */
    private int calculateAgeYears(Date birthDate, Date referenceDate) {
        Calendar birthCal = Calendar.getInstance();
        birthCal.setTime(birthDate);
        Calendar refCal = Calendar.getInstance();
        refCal.setTime(referenceDate);

        int years = refCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR);

        // If reference date’s month/day is before birth month/day, subtract 1
        int refMonth = refCal.get(Calendar.MONTH);
        int refDay = refCal.get(Calendar.DAY_OF_MONTH);
        int birthMonth = birthCal.get(Calendar.MONTH);
        int birthDay = birthCal.get(Calendar.DAY_OF_MONTH);

        if (refMonth < birthMonth || (refMonth == birthMonth && refDay < birthDay)) {
            years--;
        }
        return years;
    }

    /**
     * Returns a new Date that is 'years' years before the given date.
     */
    private Date subtractYears(Date original, int years) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(original);
        cal.add(Calendar.YEAR, -years);
        return cal.getTime();
    }

    /**
     * Ensures that a file with the given path exists (creates it if not).
     */
    private void ensureFileExists(String path) {
        File file = new File(path);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                // Ignore; calling code will handle if it tries to write and fails
            }
        }
    }
    //blank
    public String getPersonID() {
        return personID;
    }

    public void setPersonID(String personID) {
        this.personID = personID;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    public boolean isSuspended() {
        return isSuspended;
    }

    public void setSuspended(boolean suspended) {
        isSuspended = suspended;
    }

    public HashMap<Date, Integer> getDemeritPoints() {
        return demeritPoints;
    }

    public void setDemeritPoints(HashMap<Date, Integer> demeritPoints) {
        this.demeritPoints = demeritPoints;
    }
}
