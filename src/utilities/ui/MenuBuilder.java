package utilities.ui;

import utilities.io.IntScanner;

/**
 * A class with method chaining operations for creating consistent 
 * command-line interface (CLI) menus.
 * 
 * Example usage:
 * {@code
 * int choice = MenuBuilder.create()
 *     .setHeader("System Dashboard")
 *     .setOptions("View Users", "Edit Settings")
 *     .setFooter("Exit")
 *     .render();
 * }
 */
public final class MenuBuilder {
    private String header;
    private String[] options;
    private String footer;
    private String prompt;
    private int width = 50;

    // Private constructor, to prevent instantiation
    private MenuBuilder() {}

    /**
     * Creates a new MenuBuilder instance.
     * @return A new MenuBuilder object for fluent configuration
     */
    public static MenuBuilder create() {
        return new MenuBuilder();
    }

    /**
     * Sets the header text for the menu, centered automatically.
     * @param headerLines   One or more lines of header text
     * @return The current MenuBuilder instance for method chaining
     */
    public MenuBuilder setHeader(String... headerLines) {
        this.header = String.join("\n", headerLines);
        return this;
    }

    /**
     * Sets the menu options to be displayed as a numbered list.
     * @param options   Array of option descriptions
     * @return The current MenuBuilder instance for method chaining
     */
    public MenuBuilder setOptions(String... options) {
        this.options = options;
        return this;
    }

    /**
     * Sets the footer option displayed as option 0, for consistency.
     * @param footer    The description for the exit/logout option
     * @return The current MenuBuilder instance for method chaining
     */
    public MenuBuilder setFooter(String footer) {
        this.footer = footer;
        return this;
    }

    /**
     * Sets a custom input prompt for the menu. Optional.
     * @param prompt    The prompt text to display before waiting for input
     * @return The current MenuBuilder instance for method chaining
     */
    public MenuBuilder setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }

    /**
     * Sets the width of the menu in characters. Optional.
     * @param width     The total width of the menu (default: 40)
     * @return The current MenuBuilder instance for method chaining
     */
    public MenuBuilder setWidth(int width) {
        this.width = width;
        return this;
    }

    /**
     * Renders the configured menu and captures user menu input.
     * @return The user's selected option as an integer. 
     *         Returns 0 for footer option, 1-N for menu options
     */
    public int render() {
        // Handle header
        System.out.println();
        printHorizontalLine();
        printCentered(header);
        printHorizontalLine();
        
        // Handle options and sections
        int optionNumber = 1;
        int totalOptions = 0;
        if(options != null) {
            System.out.println();
            for (String item : options) {
                if (isSectionHeader(item)) {
                    if (totalOptions != 0) System.out.println();
                    handleSectionHeader(item);
                } else {
                    System.out.printf("\t%d. %s\n", optionNumber++, item);
                    totalOptions++;
                }
            }
        }
        
        // Handle footer
        if(footer != null) {
            System.out.println("\n\t0. " + footer + "\n");
        }
        
        printHorizontalLine();
        System.out.println();
        return IntScanner.scan(prompt != null ? prompt : "Enter option (0-" + totalOptions + "): ");
    }

    /**
     * Determines if a menu item represents a section header based on formatting.
     * Section headers are identified by "#" at the start.
     * @param item  The menu item text to check
     * @return true if the item is formatted as a section header, false otherwise
     */
    private boolean isSectionHeader(String item) {
        return item.startsWith("#");
    }
    
    /**
     * Process and displays a section header.
     * @param header    The raw header text starting with "#"
     */
    private void handleSectionHeader(String header) {
        String cleanHeader = header.substring(1).trim();
        String underline = "-".repeat(cleanHeader.length() + 4); // we add +4 lines for padding
        System.out.printf("\t%s\n\t%s\n", cleanHeader, underline);
    }

    /**
     * Helper function to print a horizontal line using dashes.
     */
    private void printHorizontalLine() {
        System.out.println("-".repeat(width));
    }

    /**
     * Helper function to print text centered within the menu width.
     * @param text  The text to center-align
     */
    private void printCentered(String text) {
        if(text == null) return;
        
        String[] lines = text.split("\n");
        for(String line : lines) {
            int padding = (width - line.length()) / 2;
            String format = "%" + (padding + line.length()) + "s%n";
            System.out.printf(format, line);
        }
    }
}
