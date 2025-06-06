package com.roadregistry;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.junit.jupiter.api.*;

/**
 * JUnit 5 tests for Person class (new implementation).
 *
 * Tests:
 *   - addPerson()
 *   - updatePersonalDetails()
 *   - addDemeritPoints()
 *
 * These tests operate on the real files "persons.txt" and "demerits.txt" in the project root.
 * Each @BeforeEach clears those files so tests run in isolation.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS) //Tells JUnit to create only one instance of PersonTest for all tests
class PersonTest {

    private static final String PERSON_FILE = "persons.txt";
    private static final String DEMERIT_FILE = "demerits.txt";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd-MM-yyyy");

    // check if persons.txt and demerits.txt exist before any tests run
    @BeforeAll
    void checkFilesExist() throws IOException {
        File persons = new File(PERSON_FILE);
        if (!persons.exists()) {
            persons.createNewFile();
        }
        File demerits = new File(DEMERIT_FILE);
        if (!demerits.exists()) {
            demerits.createNewFile();
        }
    }
    //Clean slate after each test
    @BeforeEach
    void clearFiles() throws IOException {
        // Clear the contents of persons.txt and demerits.txt before each test
        try (BufferedWriter w = new BufferedWriter(new FileWriter(PERSON_FILE, false))) {
            w.write("");
        }
        try (BufferedWriter w = new BufferedWriter(new FileWriter(DEMERIT_FILE, false))) {
            w.write("");
        }
    }

    // --------------------------------------------
    // Tests for addPerson()
    // --------------------------------------------
    //TEST : Check if Person is Valid when provided with a valid ID
    @Test
    @DisplayName("addPerson(): valid data should append correctly")
    void testAddPerson_Valid() throws IOException {
        Person p = new Person(
                "23ab!#XYKZ", // valid ID: 2,3 digits 2–9, ≥2 specials between 3–8, last two uppercase letters
                "John",
                "Doe",
                "12|Main Street|Melbourne|Victoria|Australia",
                "15-04-1995"
        );
        boolean result = p.addPerson();
        assertTrue(result, "Valid person should be added");

        // Read persons.txt and verify content
        List<String> lines = readAllLines(PERSON_FILE);
        assertEquals(1, lines.size(), "persons.txt should contain exactly one line");
        String[] part = lines.get(0).split(",", -1);
        assertEquals("23ab!#XYKZ", part[0]);
        assertEquals("John", part[1]);
        assertEquals("Doe", part[2]);
        assertEquals("12|Main Street|Melbourne|Victoria|Australia", part[3]);
        assertEquals("15-04-1995", part[4]);
        assertEquals("0", part[5], "Initial demeritTotal must be 0");
        assertEquals("false", part[6], "Initial isSuspended must be false");
    }
    //TEST : Check if Person is Valid when provided with a ID which only has 7 chars
    @Test
    @DisplayName("addPerson(): invalid ID length should fail")
    void testAddPerson_InvalidIDLength() throws IOException {
        Person p = new Person(
                "2a$b%Z7",          // only 7 chars, invalid
                "Jane",
                "Smith",
                "5|High Rd|Melbourne|Victoria|Australia",
                "01-01-1990"
        );
        boolean result = p.addPerson();
        assertFalse(result, "addPerson should return false for invalid ID length");

        List<String> lines = readAllLines(PERSON_FILE);
        assertTrue(lines.isEmpty(), "persons.txt must remain empty");
    }
    //TEST : Check if Person is Valid when provided with a ID with no Special non-alphanumeric char between 3-8 positions
    @Test
    @DisplayName("addPerson(): missing special chars in ID should fail")
    void testAddPerson_MissingSpecialChars() throws IOException {
        Person p = new Person(
                "2345678ABC",                  // no special non-alphanumeric between positions 3–8
                "Alice",
                "Brown",
                "8|Queen St|Ballarat|Victoria|Australia",
                "20-12-1985"
        );
        boolean result = p.addPerson();
        assertFalse(result, "addPerson should return false when ID lacks required special characters");

        List<String> lines = readAllLines(PERSON_FILE);
        assertTrue(lines.isEmpty(), "persons.txt must remain empty");
    }
    //TEST : Check if Person is Valid when provided with a address not in Victoria
    @Test
    @DisplayName("addPerson(): address not in Victoria should fail")
    void testAddPerson_InvalidAddressState() throws IOException {
        Person p = new Person(
                "24!@%GHJKAZ",
                "Bob",
                "White",
                "10|King St|Sydney|New South Wales|Australia",  // state is New South Wales, invalid
                "10-10-1992"
        );
        boolean result = p.addPerson();
        assertFalse(result, "addPerson should return false for invalid address state");

        List<String> lines = readAllLines(PERSON_FILE);
        assertTrue(lines.isEmpty(), "persons.txt must remain empty");
    }
    //TEST : Check if Person is Valid when provided with a invalid Date format YYYY-MM-DD
    @Test
    @DisplayName("addPerson(): birthdate wrong format should fail")
    void testAddPerson_InvalidBirthdateFormat() throws IOException {
        Person p = new Person(
                "25&*#AZCDXY",
                "Carol",
                "Green",
                "2|Park Ave|Geelong|Victoria|Australia",
                "1995-04-15"  // wrong format (should be dd-MM-yyyy)
        );
        boolean result = p.addPerson();
        assertFalse(result, "addPerson should return false for invalid birthdate format");

        List<String> lines = readAllLines(PERSON_FILE);
        assertTrue(lines.isEmpty(), "persons.txt must remain empty");
    }

    // --------------------------------------------
    // Tests for updatePersonalDetails()
    // --------------------------------------------

    private void writePersonLine(String line) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(PERSON_FILE, true))) {
            w.write(line);
            w.newLine();
        }
    }
    //Testing with a Valid name change for adult while leaving everything else unchanged
    @Test
    @DisplayName("updatePersonalDetails(): valid name change for adult should succeed")
    void testUpdatePersonalDetails_ValidNameChange() throws IOException {
        // Pre-seed persons.txt with an adult (born in 1995 → age ~30)
        writePersonLine("23ab!#XYKZ,John,Doe,12|Main Street|Melbourne|Victoria|Australia,15-04-1995,0,false");

        // Create Person object with updated fields
        Person p = new Person();
        p.setPersonID("23ab!#XYKZ");             // same ID to locate record
        p.setFirstName("Jonathan");               // changed first name
        p.setLastName("Doe");                     // same last name
        p.setAddress("12|Main Street|Melbourne|Victoria|Australia"); // same address
        p.setBirthdate("15-04-1995");             // same birthdate

        boolean result = p.updatePersonalDetails();
        assertTrue(result, "updatePersonalDetails should return true for a valid name change");

        // Verify that persons.txt has the updated first name
        List<String> lines = readAllLines(PERSON_FILE);
        assertEquals(1, lines.size());
        String[] tokens = lines.get(0).split(",", -1);
        assertEquals("23ab!#XYKZ", tokens[0]);
        assertEquals("Jonathan", tokens[1], "First name should have been updated to 'Jonathan'");
        assertEquals("Doe", tokens[2]);
        assertEquals("12|Main Street|Melbourne|Victoria|Australia", tokens[3]);
        assertEquals("15-04-1995", tokens[4]);
        assertEquals("0", tokens[5]);
        assertEquals("false", tokens[6]);
    }
    //Test by trying to change Address as an Under-18 User
    @Test
    @DisplayName("updatePersonalDetails(): under-18 cannot change address")
    void testUpdatePersonalDetails_Under18CannotChangeAddress() throws IOException {
        // Pre-seed a minor (born 10-08-2010 → age ~14)
        writePersonLine("24!@%GHJKAZ,Emily,Brown,5|High Rd|Melbourne|Victoria|Australia,10-08-2010,0,false");

        Person p = new Person();
        p.setPersonID("24!@%GHJKAZ");             // locate the record
        p.setFirstName("Emily");
        p.setLastName("Brown");
        p.setAddress("7|High Rd|Melbourne|Victoria|Australia"); // attempt to change
        p.setBirthdate("10-08-2010");             // same birthdate

        boolean result = p.updatePersonalDetails();
        assertFalse(result, "Minor should not be allowed to change address");

        // Ensure the file was not modified
        List<String> lines = readAllLines(PERSON_FILE);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).contains("5|High Rd|Melbourne|Victoria|Australia"),
                "Address should remain unchanged for under-18");
    }
    //Test by changing Birthdate along with other attributes
    @Test
    @DisplayName("updatePersonalDetails(): changing birthdate with other fields should fail")
    void testUpdatePersonalDetails_ChangeBirthdateWithOtherFields() throws IOException {
        // Pre-seed an adult (born 20-12-2000 → age ~24)
        writePersonLine("25&*#AZCDXY,Carol,Green,2|Park Ave|Geelong|Victoria|Australia,20-12-2000,0,false");

        Person p = new Person();
        p.setPersonID("25&*#AZCDXY");             // locate record
        p.setFirstName("Carolyn");                 // attempt to change firstName as well
        p.setLastName("Green");
        p.setAddress("2|Park Ave|Geelong|Victoria|Australia");
        p.setBirthdate("21-12-2000");             // changed birthdate

        boolean result = p.updatePersonalDetails();
        assertFalse(result, "Cannot change birthdate and other fields simultaneously");

        // Ensure original line remains intact
        List<String> lines = readAllLines(PERSON_FILE);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).startsWith("25&*#AZCDXY,Carol,Green,"), "Record should not have changed");
    }
    //Test to change ID when original first digit is even
    @Test
    @DisplayName("updatePersonalDetails(): change ID when original first digit even should fail")
    void testUpdatePersonalDetails_ChangeIDEvenFirstDigit() throws IOException {
        // Pre-seed a person whose ID begins with '2' (even)
        writePersonLine("24!@%GHJKAZ,David,White,8|Queen St|Ballarat|Victoria|Australia,20-03-1990,0,false");

        Person p = new Person();
        p.setPersonID("26NEW!ABCD");               // attempt to change ID to new valid ID
        p.setFirstName("David");
        p.setLastName("White");
        p.setAddress("8|Queen St|Ballarat|Victoria|Australia");
        p.setBirthdate("20-03-1990");

        boolean result = p.updatePersonalDetails();
        assertFalse(result, "Should not permit ID change when original first digit is even");

        // Ensure the file was not modified
        List<String> lines = readAllLines(PERSON_FILE);
        assertEquals(1, lines.size());
        assertTrue(lines.get(0).startsWith("24!@%GHJKAZ,"), "Original ID should remain unchanged");
    }
    //Test to change address and last name
    @Test
    @DisplayName("updatePersonalDetails(): valid address and last name change for adult")
    void testUpdatePersonalDetails_ValidAddressAndLastNameChange() throws IOException {
        // Pre-seed an adult
        writePersonLine("23ab!#XYKZ,Michael,Clark,3|Elm St|Bendigo|Victoria|Australia,05-05-1980,0,false");

        Person p = new Person();
        p.setPersonID("23ab!#XYKZ");             // locate record
        p.setFirstName("Michael");                // same
        p.setLastName("Clarke");                  // changed last name
        p.setAddress("5|Elm St|Bendigo|Victoria|Australia"); // changed address
        p.setBirthdate("05-05-1980");             // same birthdate

        boolean result = p.updatePersonalDetails();
        assertTrue(result, "Valid address and last name change for adult should succeed");

        // Verify the file has the updated last name and address
        List<String> lines = readAllLines(PERSON_FILE);
        assertEquals(1, lines.size());
        String[] tokens = lines.get(0).split(",", -1);
        assertEquals("23ab!#XYKZ", tokens[0]);
        assertEquals("Michael", tokens[1]);
        assertEquals("Clarke", tokens[2], "Last name should have been updated to 'Clarke'");
        assertEquals("5|Elm St|Bendigo|Victoria|Australia", tokens[3], "Address should have been updated");
        assertEquals("05-05-1980", tokens[4]);
        assertEquals("0", tokens[5]);
        assertEquals("false", tokens[6]);
    }

    // --------------------------------------------
    // Tests for addDemeritPoints()
    // --------------------------------------------

    //
    @Test
    @DisplayName("addDemeritPoints(): valid addition below threshold (adult) should not suspend")
    void testAddDemeritPoints_AdultBelowThreshold() throws Exception {
        // Pre-seed an adult with birthdate 10-10-2000
        writePersonLine("23ab!#XYKZ,Sarah,Jones,12|Main St|Melbourne|Victoria|Australia,10-10-2000,0,false");

        // Create Person object, set ID, then populate demeritPoints map
        Person p = new Person();
        p.setPersonID("23ab!#XYKZ");

        // Offense on 01-05-2025, 4 points
        Date offenseDate = DATE_FORMAT.parse("01-05-2025");
        p.getDemeritPoints().put(offenseDate, 4);

        String result = p.addDemeritPoints();
        assertEquals("Success", result, "Valid demerit addition should return 'Success'");

        // Check demerits.txt contains one record
        List<String> deLines = readAllLines(DEMERIT_FILE);
        assertEquals(1, deLines.size(), "demerits.txt should have one line");
        String[] dtokens = deLines.get(0).split(",", -1);
        assertEquals("23ab!#XYKZ", dtokens[0]);
        assertEquals("01-05-2025", dtokens[1]);
        assertEquals("4", dtokens[2]);

        // Check persons.txt now shows demerit total = 4, isSuspended = false
        List<String> personLines = readAllLines(PERSON_FILE);
        assertEquals(1, personLines.size());
        String[] ptokens = personLines.get(0).split(",", -1);
        assertEquals("23ab!#XYKZ", ptokens[0]);
        assertEquals("Sarah", ptokens[1]);
        assertEquals("Jones", ptokens[2]);
        assertEquals("12|Main St|Melbourne|Victoria|Australia", ptokens[3]);
        assertEquals("10-10-2000", ptokens[4]);
        assertEquals("4", ptokens[5], "Demerit total should be updated to 4");
        assertEquals("false", ptokens[6], "isSuspended should remain false");
    }

    @Test
    @DisplayName("addDemeritPoints(): addition pushes adult over threshold → suspend")
    void testAddDemeritPoints_AdultOverThreshold() throws Exception {
        // Pre-seed adult born 10-04-2002 => age ~23
        writePersonLine("26&*%LMNOAB,Brian,Lee,3|Elm Rd|Melbourne|Victoria|Australia,10-04-2002,0,false");

        // Also pre-seed two existing demerits within last two years (05-07-2023:5, 15-02-2024:8)
        writeDemeritLine("26&*%LMNOAB", "05-07-2023", 5);
        writeDemeritLine("26&*%LMNOAB", "15-02-2024", 8);

        Person p = new Person();
        p.setPersonID("26&*%LMNOAB");
        Date newOffense = DATE_FORMAT.parse("20-04-2025");
        p.getDemeritPoints().put(newOffense, 3);

        String result = p.addDemeritPoints();
        assertEquals("Success", result, "Adding demerits that push over threshold should return 'Success'");

        // Check demerits.txt now has 3 lines
        List<String> deLines = readAllLines(DEMERIT_FILE);
        assertEquals(3, deLines.size(), "demerits.txt should have three lines total");

        // Check persons.txt: total points = 5 + 8 + 3 = 16; isSuspended should be true
        List<String> personLines = readAllLines(PERSON_FILE);
        assertEquals(1, personLines.size());
        String[] ptokens = personLines.get(0).split(",", -1);
        assertEquals("26&*%LMNOAB", ptokens[0]);
        assertEquals("Brian", ptokens[1]);
        assertEquals("Lee", ptokens[2]);
        assertEquals("3|Elm Rd|Melbourne|Victoria|Australia", ptokens[3]);
        assertEquals("10-04-2002", ptokens[4]);
        assertEquals("16", ptokens[5], "Total demerit points should be updated to 16");
        assertEquals("true", ptokens[6], "isSuspended should now be true");
    }

    @Test
    @DisplayName("addDemeritPoints(): invalid offensedate format should fail")
    void testAddDemeritPoints_InvalidDateFormat() throws Exception {
        // Pre-seed a valid person
        writePersonLine("25!@#PQRSXY,Olivia,Smith,1|River Rd|Geelong|Victoria|Australia,15-12-2003,0,false");

        Person p = new Person();
        p.setPersonID("25!@#PQRSXY");
        // Put invalid date string into demeritPoints key by parsing a wrong format → parseDate will produce null
        // But since our Person.addDemeritPoints iterates only on valid Date objects in the map,
        // passing a parse failure is not straightforward. Instead, we simulate by passing a null key:

        p.getDemeritPoints().put(null, 2);

        String result = p.addDemeritPoints();
        assertEquals("Failed", result, "addDemeritPoints should return 'Failed' if an invalid Date is present");

        // Ensure demerits.txt is still empty
        List<String> deLines = readAllLines(DEMERIT_FILE);
        assertTrue(deLines.isEmpty(), "demerits.txt must remain empty on failure");

        // Ensure persons.txt is unchanged
        List<String> personLines = readAllLines(PERSON_FILE);
        assertEquals(1, personLines.size());
        String[] ptokens = personLines.get(0).split(",", -1);
        assertEquals("0", ptokens[5]);
        assertEquals("false", ptokens[6]);
    }

    @Test
    @DisplayName("addDemeritPoints(): points outside 1–6 should fail")
    void testAddDemeritPoints_InvalidPointsRange() throws Exception {
        // Pre-seed person
        writePersonLine("24X%^LMNXY,Ethan,Brown,9|Hill St|Shepparton|Victoria|Australia,30-11-2004,0,false");

        Person p = new Person();
        p.setPersonID("24X%^LMNXY");
        Date offenseDate = DATE_FORMAT.parse("01-06-2025");
        p.getDemeritPoints().put(offenseDate, 7); // invalid points

        String result = p.addDemeritPoints();
        assertEquals("Failed", result, "addDemeritPoints should return 'Failed' if points out of range 1–6");

        // Ensure no lines in demerits.txt and persons.txt unchanged
        assertTrue(readAllLines(DEMERIT_FILE).isEmpty(), "demerits.txt must remain empty");
        List<String> personLines = readAllLines(PERSON_FILE);
        assertEquals(1, personLines.size());
        String[] ptokens = personLines.get(0).split(",", -1);
        assertEquals("0", ptokens[5]);
        assertEquals("false", ptokens[6]);
    }

    @Test
    @DisplayName("addDemeritPoints(): under-21 accumulating >6 within 2 years → suspend")
    void testAddDemeritPoints_Under21OverThreshold() throws Exception {
        // Pre-seed a minor (born 01-01-2006 → age ~19)
        writePersonLine("27&*ZABCXY,Lily,Adams,4|Forest Ave|Ballarat|Victoria|Australia,01-01-2006,0,false");

        // Pre-existing demerits within 2 years: 10-07-2024:4, 20-11-2024:3
        writeDemeritLine("27&*ZABCXY", "10-07-2024", 4);
        writeDemeritLine("27&*ZABCXY", "20-11-2024", 3);

        Person p = new Person();
        p.setPersonID("27&*ZABCXY");
        Date newOffense = DATE_FORMAT.parse("15-05-2025");
        p.getDemeritPoints().put(newOffense, 2);

        String result = p.addDemeritPoints();
        assertEquals("Success", result, "Minor should be suspended if total > 6 within 2 years");

        // Check demerits.txt now has 3 records
        List<String> deLines = readAllLines(DEMERIT_FILE);
        assertEquals(3, deLines.size(), "demerits.txt should have three lines");

        // Check persons.txt: total = 4 + 3 + 2 = 9; under 21 → threshold 6 → isSuspended true
        List<String> personLines = readAllLines(PERSON_FILE);
        assertEquals(1, personLines.size());
        String[] ptokens = personLines.get(0).split(",", -1);
        assertEquals("27&*ZABCXY", ptokens[0]);
        assertEquals("Lily", ptokens[1]);
        assertEquals("Adams", ptokens[2]);
        assertEquals("4|Forest Ave|Ballarat|Victoria|Australia", ptokens[3]);
        assertEquals("01-01-2006", ptokens[4]);
        assertEquals("9", ptokens[5], "Total demerit points should be updated to 9");
        assertEquals("true", ptokens[6], "isSuspended should now be true");
    }
    // --------------------------------------------
    // Utility methods for file I/O
    // --------------------------------------------

    /**
     * Reads all lines from the given file and returns them as a List of Strings.
     */
    private List<String> readAllLines(String filename) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isBlank()) {
                    lines.add(line);
                }
            }
        }
        return lines;
    }

    /**
     * Helper to pre-seed a single demerit line into demerits.txt:
     *   personID,offenseDate,points
     */
    private void writeDemeritLine(String personID, String dateStr, int points) throws IOException {
        String record = String.join(",", personID, dateStr, Integer.toString(points));
        try (BufferedWriter w = new BufferedWriter(new FileWriter(DEMERIT_FILE, true))) {
            w.write(record);
            w.newLine();
        }
    }
}
