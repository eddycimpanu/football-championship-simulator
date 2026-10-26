package org.example.console;

import java.util.Scanner;

// Reads from the keyboard; a wrong answer never stops the program, it just asks again.
public class ConsoleInput {

    private final Scanner scanner;

    public ConsoleInput(Scanner scanner) {
        this.scanner = scanner;
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt + " ");
            String line = readLine();
            try {
                int value = Integer.parseInt(line.trim());
                if (value >= min && value <= max) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // not a number: ask again
            }
            System.out.println("Please type a number between " + min + " and " + max + ".");
        }
    }

    public String readText(String prompt) {
        while (true) {
            System.out.print(prompt + " ");
            String line = readLine().trim();
            if (!line.isEmpty()) {
                return line;
            }
        }
    }

    public boolean readYesNo(String prompt) {
        while (true) {
            System.out.print(prompt + " (y/n) ");
            String line = readLine().trim().toLowerCase();
            if (line.equals("y") || line.equals("yes")) {
                return true;
            }
            if (line.equals("n") || line.equals("no")) {
                return false;
            }
        }
    }

    public void waitForEnter() {
        System.out.print("Press ENTER to continue...");
        readLine();
    }

    private String readLine() {
        if (!scanner.hasNextLine()) {
            throw new IllegalStateException("No more input");
        }
        return scanner.nextLine();
    }
}
