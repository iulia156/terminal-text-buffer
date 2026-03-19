package terminal;

import java.util.Objects;

public class TextStyle {
    public static final TextStyle DEFAULT = new TextStyle();

    private final TerminalColor foreground;
    private final TerminalColor background;
    private final boolean bold;
    private final boolean italic;
    private final boolean underline;

    public TextStyle() {
        this(TerminalColor.DEFAULT, TerminalColor.DEFAULT, false, false, false);
    }

    public TextStyle(TerminalColor foreground, TerminalColor background, boolean bold, boolean italic, boolean underline) {
        this.foreground = foreground;
        this.background = background;
        this.bold = bold;
        this.italic = italic;
        this.underline = underline;
    }

    public TerminalColor getForeground() { return foreground; }
    public TerminalColor getBackground() { return background; }
    public boolean isBold()      { return bold; }
    public boolean isItalic()    { return italic; }
    public boolean isUnderline() { return underline; }

    public TextStyle withForeground(TerminalColor fg) {
        return new TextStyle(fg, background, bold, italic, underline);
    }
    public TextStyle withBackground(TerminalColor bg) {
        return new TextStyle(foreground, bg, bold, italic, underline);
    }
    public TextStyle withBold(boolean b) {
        return new TextStyle(foreground, background, b, italic, underline);
    }
    public TextStyle withItalic(boolean i) {
        return new TextStyle(foreground, background, bold, i, underline);
    }
    public TextStyle withUnderline(boolean u) {
        return new TextStyle(foreground, background, bold, italic, u);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TextStyle textStyle = (TextStyle) o;
        return bold == textStyle.bold && italic == textStyle.italic && underline == textStyle.underline && foreground == textStyle.foreground && background == textStyle.background;
    }

    @Override
    public int hashCode() {
        return Objects.hash(foreground, background, bold, italic, underline);
    }
}
