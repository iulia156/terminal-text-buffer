package terminal;

import java.util.ArrayDeque;
import java.util.Arrays;

public class TerminalBuffer {
    private int width;
    private int height;
    private final int maxScrollback;

    // Fixed-size array for the visible screen; index 0 is the top row
    private final Line[] screen;

    // Lines that scrolled off the top; oldest at front, newest at back
    private final ArrayDeque<Line> scrollback;

    // Current cursor position (0-indexed)
    private int cursorCol;
    private int cursorRow;

    // Style applied to all subsequent write operations
    private TextStyle currentStyle;

    public TerminalBuffer(int width, int height, int maxScrollback) {
        this.width = width;
        this.height = height;
        this.maxScrollback = maxScrollback;
        this.screen = new Line[height];
        for (int i = 0; i < height; i++) {
            screen[i] = new Line(width);
        }
        this.scrollback = new ArrayDeque<>();
        this.cursorCol = 0;
        this.cursorRow = 0;
        this.currentStyle = TextStyle.DEFAULT;
    }

    public TerminalBuffer(int width, int height) {
        this(width, height, 1000);
    }

    public int getWidth()     { return width; }
    public int getHeight()    { return height; }
    public int getCursorCol() { return cursorCol; }
    public int getCursorRow() { return cursorRow; }
    public TextStyle getCurrentStyle() { return currentStyle; }

    // All cursor movement funnels through here to guarantee bounds are always respected
    public void setCursor(int col, int row) {
        this.cursorCol = clamp(col, 0, width - 1);
        this.cursorRow = clamp(row, 0, height - 1);
    }

    public void moveCursorUp(int n) {
        setCursor(cursorCol, cursorRow - n);
    }

    public void moveCursorDown(int n) {
        setCursor(cursorCol, cursorRow + n);
    }

    public void moveCursorLeft(int n) {
        setCursor(cursorCol - n, cursorRow);
    }

    public void moveCursorRight(int n) {
        setCursor(cursorCol + n, cursorRow);
    }

    public void setStyle(TextStyle style) {
        this.currentStyle = style;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // Core scroll operation: pushes the top screen line into scrollback
    // and adds a fresh empty line at the bottom
    public void insertEmptyLine() {
        Line topLine = screen[0];
        // Copy before shifting since storing a reference would let screen edits corrupt scrollback
        scrollback.addLast(topLine.copy());

        if (scrollback.size() > maxScrollback) {
            scrollback.removeFirst();
        }

        // Shift all lines up by one slot (reassigns references, no cell data copied)
        for (int i = 0; i < height-1; i++) {
            screen[i] = screen[i + 1];
        }

        screen[height-1] = new Line(width);
    }

    // Wipes the visible screen but preserves scrollback history
    public void clearScreen() {
        for (int i = 0; i < height; i++) {
            screen[i] = new Line(width);
        }
        setCursor(0, 0);
    }

    // Wipes everything including scrollback history
    public void clearAll() {
        clearScreen();
        scrollback.clear();
    }

    // Fills the entire current row with a character; null means empty cells
    public void fillLine(Character character) {
        Line line = screen[cursorRow];
        line.fill(character, currentStyle);
    }

    // Overwrites characters at cursor position, but does not shift existing content
    // Wide characters occupy 2 cells: the left cell holds the char, the right is a continuation marker
    public void writeText(String text) {
        Line line = screen[cursorRow];

        for (int i = 0; i < text.length(); i++) {
            if (cursorCol >= width) break;
            char c = text.charAt(i);
            if (isWideChar(c)) {
                // Not enough room for a wide char, fill with empty and stop
                if (cursorCol + 1 >= width) {
                    line.setCell(cursorCol, Cell.EMPTY);
                    break;
                }
                line.setCell(cursorCol, new Cell(c, currentStyle, true, false));
                line.setCell(cursorCol + 1, new Cell(null, currentStyle, false, true));
                cursorCol += 2;
            } else {
                line.setCell(cursorCol, new Cell(c, currentStyle));
                cursorCol++;
            }
        }
    }

    // Inserts characters at cursor, shifting existing content right
    // Content pushed past the line edge is dropped (trade-off: no full wrap preservation)
    public void insertText(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            // Wrap to next line if cursor is past the edge
            if (cursorCol >= width) {
                if (cursorRow == height - 1) {
                    // On last row scroll instead of moving down
                    insertEmptyLine();
                    cursorRow = height - 1;
                } else {
                    cursorRow++;
                }
                cursorCol = 0;
            }

            Line line = screen[cursorRow];

            // Shift cells right to make room; must go right-to-left to avoid overwriting
            for (int col = width - 1; col > cursorCol; col--) {
                line.setCell(col, line.getCell(col - 1));
            }
            line.setCell(cursorCol, new Cell(c, currentStyle));
            cursorCol++;
        }
    }

    // Single cell access

    public Cell getCellFromScreen(int col, int row) {
        if (col < 0 || col >= width || row < 0 || row >= height) {
            throw new IndexOutOfBoundsException(
                    "Position (" + col + ", " + row + ") out of screen bounds"
            );
        }
        return screen[row].getCell(col);
    }

    public Cell getCellFromScrollback(int col, int row) {
        if (col < 0 || col >= width || row < 0 || row >= scrollback.size()) {
            throw new IndexOutOfBoundsException(
                    "Position (" + col + ", " + row + ") out of scrollback bounds"
            );
        }
        // ArrayDeque has no index access; converting to array is a known trade-off
        Line[] lines = scrollback.toArray(new Line[0]);
        return lines[row].getCell(col);
    }


    // Attribute access

    public TextStyle getStyleFromScreen(int col, int row) {
        return getCellFromScreen(col, row).getStyle();
    }

    public TextStyle getStyleFromScrollback(int col, int row) {
        return getCellFromScrollback(col, row).getStyle();
    }


    // Line access

    public String getScreenLine(int row) {
        if (row < 0 || row >= height) {
            throw new IndexOutOfBoundsException(
                    "Row " + row + " out of screen bounds"
            );
        }
        return screen[row].toDisplayString();
    }

    public String getScrollbackLine(int row) {
        if (row < 0 || row >= scrollback.size()) {
            throw new IndexOutOfBoundsException(
                    "Row " + row + " out of scrollback bounds"
            );
        }
        Line[] lines = scrollback.toArray(new Line[0]);
        return lines[row].toDisplayString();
    }


    // Full content access

    public String getScreenContent() {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < height; row++) {
            sb.append(screen[row].toDisplayString());
            if (row < height - 1) sb.append("\n");
        }
        return sb.toString();
    }

    // Returns scrollback on top, screen below; matches what a user sees when scrolled up
    public String getAllContent() {
        StringBuilder sb = new StringBuilder();
        Line[] scrollbackLines = scrollback.toArray(new Line[0]);

        for (Line scrollbackLine : scrollbackLines) {
            sb.append(scrollbackLine.toDisplayString());
            sb.append("\n");
        }

        sb.append(getScreenContent());
        return sb.toString();
    }

    public int getScrollbackSize() {
        return scrollback.size();
    }

    private boolean isWideChar(char c) {
        return Character.UnicodeBlock.of(c) == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.HIRAGANA || Character.UnicodeBlock.of(c) == Character.UnicodeBlock.KATAKANA;
    }

    // Resize strategy: rewrap all content (scrollback + screen) to the new width,
    // then redistribute into scrollback and screen at the new height
    public void resize(int newWidth, int newHeight) {
        // Combine scrollback and screen into one ordered list (oldest first)
        java.util.List<Line> allLines = new java.util.ArrayList<>(scrollback);
        allLines.addAll(Arrays.asList(screen));

        // Rewrap every line to the new width
        java.util.List<Line> rewrapped = new java.util.ArrayList<>();
        for (Line oldLine : allLines) {
            rewrapped.addAll(rewrapLine(oldLine, newWidth));
        }

        scrollback.clear();
        Line[] newScreen = new Line[newHeight];

        if (rewrapped.size() <= newHeight) {
            // All content fits on screen, pad top with empty lines
            int offset = newHeight - rewrapped.size();
            for (int i = 0; i < newHeight; i++) {
                if (i < offset) {
                    newScreen[i] = new Line(newWidth);
                } else {
                    newScreen[i] = rewrapped.get(i - offset);
                }
            }
        } else {
            // Excess lines go to scrollback, most recent lines fill the screen
            int scrollbackCount = rewrapped.size() - newHeight;
            for (int i = 0; i < scrollbackCount; i++) {
                if (scrollback.size() >= maxScrollback) break;
                scrollback.addLast(rewrapped.get(i));
            }
            for (int i = 0; i < newHeight; i++) {
                newScreen[i] = rewrapped.get(scrollbackCount + i);
            }
        }

        this.width = newWidth;
        this.height = newHeight;
        System.arraycopy(newScreen, 0, screen, 0, newHeight);
        setCursor(cursorCol, cursorRow);
    }

    // Rewraps a single line to a new width, handling wide characters correctly
    // Continuation cells are skipped; they are regenerated from their wide char owner
    private java.util.List<Line> rewrapLine(Line oldLine, int newWidth) {
        java.util.List<Line> result = new java.util.ArrayList<>();
        Line current = new Line(newWidth);
        int col = 0;

        for (int i = 0; i < oldLine.getWidth(); i++) {
            Cell cell = oldLine.getCell(i);
            if (cell.isContinuation()) continue;

            if (col >= newWidth) {
                result.add(current);
                current = new Line(newWidth);
                col = 0;
            }

            // Wide char that doesn't fit on remaining space; wrap it to next line
            if (cell.isWide() && col + 1 >= newWidth) {
                current.setCell(col, Cell.EMPTY);
                result.add(current);
                current = new Line(newWidth);
                col = 0;
            }

            current.setCell(col, cell);
            if (cell.isWide()) {
                // Regenerate the continuation cell on the right
                current.setCell(col + 1, new Cell(null, cell.getStyle(), false, true));
                col += 2;
            } else {
                col++;
            }
        }
        result.add(current);
        return result;
    }

}
