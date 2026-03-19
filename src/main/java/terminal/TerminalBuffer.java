package terminal;

import java.util.ArrayDeque;

public class TerminalBuffer {
    private final int width;
    private final int height;
    private final int maxScrollback;

    private final Line[] screen;
    private final ArrayDeque<Line> scrollback;

    private int cursorCol;
    private int cursorRow;

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

    public void insertEmptyLine() {
        Line topLine = screen[0];
        scrollback.addLast(topLine.copy());

        if (scrollback.size() > maxScrollback) {
            scrollback.removeFirst();
        }

        for (int i = 0; i < height-1; i++) {
            screen[i] = screen[i + 1];
        }

        screen[height-1] = new Line(width);
    }

    public void clearScreen() {
        for (int i = 0; i < height; i++) {
            screen[i] = new Line(width);
        }
        setCursor(0, 0);
    }

    public void clearAll() {
        clearScreen();
        scrollback.clear();
    }

    public void fillLine(Character character) {
        Line line = screen[cursorRow];
        line.fill(character, currentStyle);
    }

    public void writeText(String text) {
        Line line = screen[cursorRow];

        for (int i = 0; i < text.length(); i++) {
            if (cursorCol >= width) break;
            char c = text.charAt(i);
            if (isWideChar(c)) {
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

    public void insertText(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (cursorCol >= width) {
                if (cursorRow == height - 1) {
                    insertEmptyLine();
                    cursorRow = height - 1;
                } else {
                    cursorRow++;
                }
                cursorCol = 0;
            }

            Line line = screen[cursorRow];

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

}
