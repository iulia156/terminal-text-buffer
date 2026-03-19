package terminal;

public class Line {
    private final int width;
    private final Cell[] cells;

    public Line(int width) {
        this.width = width;
        this.cells = new Cell[width];
        java.util.Arrays.fill(cells, Cell.EMPTY);
    }

    private Line(int width, Cell[] cells) {
        this.width = width;
        this.cells = cells.clone();
    }

    public int getWidth() { return width; }

    public Cell getCell(int col) {
        checkBounds(col);
        return cells[col];
    }

    public void setCell(int col, Cell cell) {
        checkBounds(col);
        cells[col] = cell;
    }

    public void fill(Character character, TextStyle style) {
        Cell cell = new Cell(character, style);
        java.util.Arrays.fill(cells, cell);
    }

    public void clear() {
        java.util.Arrays.fill(cells, Cell.EMPTY);
    }

    public String toDisplayString() {
        StringBuilder sb = new StringBuilder(width);
        for (Cell cell : cells) {
            sb.append(cell.isEmpty() ? ' ' : cell.getCharacter());
        }
        return sb.toString();
    }

    public Line copy() {
        return new Line(width, cells);
    }

    private void checkBounds(int col) {
        if (col < 0 || col >= width) {
            throw new IndexOutOfBoundsException(
                    "Column " + col + " out of bounds (width=" + width + ")"
            );
        }
    }
}
