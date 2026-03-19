package terminal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

public class TerminalBufferTest {
    @Test
    void initialCursorIsAtTopLeft() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24);
        assertEquals(0, buffer.getCursorCol());
        assertEquals(0, buffer.getCursorRow());
    }

    @Test
    void initialScreenIsEmpty() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24);
        for (int row = 0; row < 24; row++) {
            for (int col = 0; col < 80; col++) {
                assertTrue(buffer.getCellFromScreen(col, row).isEmpty());
            }
        }
    }

    @Test
    void initialStyleIsDefault() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24);
        assertEquals(TextStyle.DEFAULT, buffer.getCurrentStyle());
    }

    @Test
    void setCursorMovesToPosition() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24);
        buffer.setCursor(10, 5);
        assertEquals(10, buffer.getCursorCol());
        assertEquals(5, buffer.getCursorRow());
    }

    @Test
    void cursorCannotGoLeftOfBounds() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24);
        buffer.setCursor(0, 0);
        buffer.moveCursorLeft(10);
        assertEquals(0, buffer.getCursorCol());
    }

    @Test
    void cursorCannotGoRightOfBounds() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24);
        buffer.setCursor(79, 0);
        buffer.moveCursorRight(10);
        assertEquals(79, buffer.getCursorCol());
    }

    @Test
    void cursorCannotGoAboveTopRow() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24);
        buffer.setCursor(0, 0);
        buffer.moveCursorUp(5);
        assertEquals(0, buffer.getCursorRow());
    }

    @Test
    void cursorCannotGoBelowBottomRow() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24);
        buffer.setCursor(0, 23);
        buffer.moveCursorDown(5);
        assertEquals(23, buffer.getCursorRow());
    }

    @Test
    void cursorMovesCorrectlyInAllDirections() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24);
        buffer.setCursor(10, 10);
        buffer.moveCursorUp(3);
        assertEquals(7, buffer.getCursorRow());
        buffer.moveCursorDown(5);
        assertEquals(12, buffer.getCursorRow());
        buffer.moveCursorLeft(4);
        assertEquals(6, buffer.getCursorCol());
        buffer.moveCursorRight(2);
        assertEquals(8, buffer.getCursorCol());
    }

    @Test
    void writeTextPlacesCharactersAtCursor() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24);
        buffer.setCursor(0, 0);
        buffer.writeText("Hello");
        assertEquals("Hello", buffer.getScreenLine(0).substring(0, 5));
    }

    @Test
    void writeTextAdvancesCursor() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24);
        buffer.setCursor(0, 0);
        buffer.writeText("Hello");
        assertEquals(5, buffer.getCursorCol());
    }

    @Test
    void writeTextOverwritesExistingContent() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24);
        buffer.setCursor(0, 0);
        buffer.writeText("Hello");
        buffer.setCursor(0, 0);
        buffer.writeText("World");
        assertEquals("World", buffer.getScreenLine(0).substring(0, 5));
    }

    @Test
    void writeTextStopsAtEndOfLine() {
        TerminalBuffer buffer = new TerminalBuffer(10, 24);
        buffer.setCursor(8, 0);
        buffer.writeText("ABCDE");
        assertEquals('A', buffer.getCellFromScreen(8, 0).getCharacter());
        assertEquals('B', buffer.getCellFromScreen(9, 0).getCharacter());
        assertEquals(10, buffer.getCursorCol());
    }

    @Test
    void writeTextUsesCurrentStyle() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24);
        TextStyle redBold = TextStyle.DEFAULT.withForeground(TerminalColor.RED).withBold(true);
        buffer.setStyle(redBold);
        buffer.writeText("A");
        TextStyle actual = buffer.getCellFromScreen(0, 0).getStyle();
        assertEquals(TerminalColor.RED, actual.getForeground());
        assertTrue(actual.isBold());
    }

    @Test
    void writeTextAtDifferentRowsIsIndependent() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24);
        buffer.setCursor(0, 0);
        buffer.writeText("Hello");
        buffer.setCursor(0, 1);
        buffer.writeText("World");
        assertEquals("Hello", buffer.getScreenLine(0).substring(0, 5));
        assertEquals("World", buffer.getScreenLine(1).substring(0, 5));
    }

    @Test
    void insertTextShiftsExistingContent() {
        TerminalBuffer buffer = new TerminalBuffer(10, 24);
        buffer.setCursor(0, 0);
        buffer.writeText("Hello");
        buffer.setCursor(2, 0);
        buffer.insertText("X");
        assertEquals("HeXllo", buffer.getScreenLine(0).substring(0, 6));
    }

    @Test
    void insertTextWrapsToNextLine() {
        TerminalBuffer buffer = new TerminalBuffer(5, 24);
        buffer.setCursor(0, 0);
        buffer.writeText("Hello");
        buffer.insertText("AB");
        assertEquals(2, buffer.getCursorCol());
        assertEquals(1, buffer.getCursorRow());
    }

    @Test
    void insertTextScrollsWhenOnLastLine() {
        TerminalBuffer buffer = new TerminalBuffer(5, 2);
        buffer.setCursor(0, 0);
        buffer.writeText("Hello");
        buffer.setCursor(0, 1);
        buffer.writeText("World");
        buffer.insertText("X");
        assertEquals(1, buffer.getScrollbackSize());
    }

    @Test
    void insertEmptyLineScrollsTopLineToScrollback() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24);
        buffer.setCursor(0, 0);
        buffer.writeText("Hello");
        buffer.insertEmptyLine();
        assertEquals(1, buffer.getScrollbackSize());
        assertEquals("Hello", buffer.getScrollbackLine(0).substring(0, 5));
    }

    @Test
    void insertEmptyLineShiftsScreenUp() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24);
        buffer.setCursor(0, 0);
        buffer.writeText("First");
        buffer.setCursor(0, 1);
        buffer.writeText("Second");
        buffer.insertEmptyLine();
        assertEquals("First", buffer.getScrollbackLine(0).substring(0, 5));
        assertEquals("Second", buffer.getScreenLine(0).substring(0, 6));
    }

    @Test
    void scrollbackRespectsMaxSize() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24, 3);
        for (int i = 0; i < 10; i++) {
            buffer.insertEmptyLine();
        }
        assertEquals(3, buffer.getScrollbackSize());
    }

    @Test
    void scrollbackIsIndependentFromScreen() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24);
        buffer.setCursor(0, 0);
        buffer.writeText("Hello");
        buffer.insertEmptyLine();
        // Modify screen, scrollback should be unaffected
        buffer.setCursor(0, 0);
        buffer.writeText("Changed");
        assertEquals("Hello", buffer.getScrollbackLine(0).substring(0, 5));
    }

    @Test
    void clearScreenWipesAllCells() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24);
        buffer.setCursor(0, 0);
        buffer.writeText("Hello");
        buffer.clearScreen();
        assertTrue(buffer.getCellFromScreen(0, 0).isEmpty());
    }

    @Test
    void clearScreenResetsCursorToTopLeft() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24);
        buffer.setCursor(10, 10);
        buffer.clearScreen();
        assertEquals(0, buffer.getCursorCol());
        assertEquals(0, buffer.getCursorRow());
    }

    @Test
    void clearScreenPreservesScrollback() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24);
        buffer.insertEmptyLine();
        buffer.clearScreen();
        assertEquals(1, buffer.getScrollbackSize());
    }

    @Test
    void clearAllWipesScreenAndScrollback() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24);
        buffer.insertEmptyLine();
        buffer.clearAll();
        assertEquals(0, buffer.getScrollbackSize());
        assertTrue(buffer.getCellFromScreen(0, 0).isEmpty());
    }

    @Test
    void fillLineFillesEntireCurrentRow() {
        TerminalBuffer buffer = new TerminalBuffer(5, 24);
        buffer.setCursor(2, 1);
        buffer.fillLine('*');
        assertEquals("*****", buffer.getScreenLine(1));
    }

    @Test
    void fillLineWithNullClearsRow() {
        TerminalBuffer buffer = new TerminalBuffer(5, 24);
        buffer.setCursor(0, 0);
        buffer.writeText("Hello");
        buffer.setCursor(0, 0);
        buffer.fillLine(null);
        assertTrue(buffer.getCellFromScreen(0, 0).isEmpty());
    }

    @Test
    void fillLineUsesCurrentStyle() {
        TerminalBuffer buffer = new TerminalBuffer(5, 24);
        buffer.setStyle(TextStyle.DEFAULT.withForeground(TerminalColor.BLUE));
        buffer.fillLine('*');
        assertEquals(TerminalColor.BLUE, buffer.getCellFromScreen(0, 0).getStyle().getForeground());
    }

    @Test
    void getScreenContentReturnsAllLines() {
        TerminalBuffer buffer = new TerminalBuffer(5, 3);
        buffer.setCursor(0, 0);
        buffer.writeText("Hello");
        buffer.setCursor(0, 1);
        buffer.writeText("World");
        String content = buffer.getScreenContent();
        assertTrue(content.contains("Hello"));
        assertTrue(content.contains("World"));
    }

    @Test
    void getAllContentReturnsScrollbackThenScreen() {
        TerminalBuffer buffer = new TerminalBuffer(5, 2);
        buffer.setCursor(0, 0);
        buffer.writeText("First");
        buffer.insertEmptyLine();
        buffer.setCursor(0, 0);
        buffer.writeText("Secnd");
        String all = buffer.getAllContent();
        assertTrue(all.indexOf("First") < all.indexOf("Secnd"));
    }

    @Test
    void outOfBoundsAccessThrowsException() {
        TerminalBuffer buffer = new TerminalBuffer(80, 24);
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.getCellFromScreen(80, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.getCellFromScreen(0, 24));
        assertThrows(IndexOutOfBoundsException.class, () -> buffer.getCellFromScreen(-1, 0));
    }

}
