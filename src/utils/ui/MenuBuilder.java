package utils.ui;

import utils.io.IntScanner;
import utils.io.StringScanner;

/**
 * A class with method chaining operations for creating consistent 
 * command-line interface (CLI) menus.
 * 
 * <p>Example usage:
 * <pre>{@code
 * int choice = MenuBuilder.create()
 *     .setHeader("System Dashboard")
 *     .setOptions("View Users", "Edit Settings")
 *     .setFooter("Exit")
 *     .render();
 * }</pre>
 */
public final class MenuBuilder {
    private String header;
    private String[] options;
    private String footer;
    private String prompt;
    private int width = 40;

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
     * @param prompt The prompt text to display before waiting for input
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
        System.out.println();
        printHorizontalLine();
        printCentered(header);
        printHorizontalLine();
        
        if(options != null) {
            System.out.println();
            for(int i = 0; i < options.length; i++) {
                System.out.printf("\t%d. %s\n", i+1, options[i]);
            }
        }
        
        if(footer != null) {
            System.out.println("\n\t0. " + footer + "\n");
        }
        
        printHorizontalLine();
        System.out.println();
        return IntScanner.scan(prompt != null ? prompt : "Enter option (0-" + options.length + "): ");
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