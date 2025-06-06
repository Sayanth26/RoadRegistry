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
