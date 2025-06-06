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