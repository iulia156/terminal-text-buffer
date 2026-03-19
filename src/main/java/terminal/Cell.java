package terminal;

public class Cell {
    public static final Cell EMPTY = new Cell();

    private final Character character;
    private final TextStyle style;
    private final boolean isWide;
    private final boolean isContinuation;

    public Cell() {
        this(null, TextStyle.DEFAULT, false, false);
    }

    public Cell(Character character, TextStyle style) {
        this(character, style, false, false);
    }

    public Cell(Character character, TextStyle style, boolean isWide, boolean isContinuation) {
        this.character = character;
        this.style = style;
        this.isWide = isWide;
        this.isContinuation = isContinuation;
    }

    public Character getCharacter()    { return character; }
    public TextStyle getStyle()        { return style; }
    public boolean isWide()            { return isWide; }
    public boolean isContinuation()    { return isContinuation; }
    public boolean isEmpty()           { return character == null; }
}
