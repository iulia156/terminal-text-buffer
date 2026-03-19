# Terminal Text Buffer

A Java implementation of a terminal text buffer — the core data structure used by terminal emulators to store and manipulate displayed text.

## Solution Overview

The buffer is built as a layered data model:

- `TerminalColor` defines the 16 standard ANSI colors
- `TextStyle` holds the foreground color, background color, and style flags (bold, italic, underline) for a single style state
- `Cell` represents one position in the grid, a character and its style
- `Line` is a fixed-width array of cells
- `TerminalBuffer` manages the screen grid, scrollback history, cursor position, and current style, and exposes all editing and access operations

The screen is a fixed-size `Line[]` array where index 0 is the top row. 
The scrollback is an `ArrayDeque<Line>` where the oldest lines are at the front and the newest at the back.

## Design Decisions and Trade-offs

### Immutable Cell and TextStyle
Both `Cell` and `TextStyle` are immutable, all their fields are `final`. This means they can be safely shared across multiple positions in the grid 
without risk of one write accidentally affecting another. For example, `Cell.EMPTY` and `TextStyle.DEFAULT` are shared singleton instances reused 
across the entire buffer. The trade-off is that every style change or cell write allocates a new object, but for a terminal buffer this is negligible.

### ArrayDeque for scrollback
`ArrayDeque` was chosen for scrollback because it supports efficient insertion at the back and removal from the front, exactly the access 
pattern needed when pushing new lines in and evicting old ones when the limit is reached. The trade-off is that `ArrayDeque` does not support 
index-based access, so reading a specific scrollback line requires converting to an array first via `toArray()`. This is slightly inefficient 
for frequent random access but acceptable given that scrollback reads are infrequent compared to screen writes.

### Dropping content at line end in insertText
When inserting text shifts content past the right edge of a line, that content is dropped rather than wrapped to the next line. A full 
implementation would preserve it, but this significantly increases complexity and is outside the scope of this task. The decision is 
documented here as a known limitation.

### ArrayDeque index access via toArray
As mentioned above, accessing scrollback by index requires calling `scrollback.toArray()` each time. An alternative would be to use 
`ArrayList` for scrollback, which supports `get(index)` directly. The reason `ArrayDeque` was kept is that its front-removal performance is 
better than `ArrayList`, which has to shift all elements on `remove(0)`. For a scrollback that can hold thousands of lines, this matters.

## Bonus Features

### Wide Characters
Characters such as CJK ideographs, Hiragana, and Katakana occupy 2 columns in a terminal. When a wide character is written, the left cell stores the 
character marked as `isWide = true`, and the right cell is filled with a continuation marker (`isContinuation = true`). The cursor advances by 2. 
If a wide character does not fit at the end of a line, the remaining cell is left empty and writing stops.

Detection is done via `Character.UnicodeBlock`, covering CJK Unified Ideographs, Hiragana, and Katakana. Emoji and other wide symbols outside 
these blocks are not currently detected. his is mentioned below as a future improvement.

### Resize
When the screen is resized, all content (scrollback + screen) is rewrapped to the new width and redistributed: the most recent lines fill the new 
screen, and any excess lines go into scrollback. Wide characters are handled correctly during rewrapping; continuation cells are skipped and regenerated 
from their wide character owner. If a wide character does not fit at the end of a rewrapped line, it is moved to the next line.

## Possible Future Improvements

### Full emoji and wide symbol support
The current wide character detection covers CJK ideographs, Hiragana, and Katakana via `Character.UnicodeBlock`. Emoji and other wide symbols are not 
detected. A complete implementation would use a Unicode character width table (such as the East Asian Width property) and process text as code 
points via `String.codePoints()` rather than `char`, since many emoji fall outside the Basic Multilingual Plane and cannot be represented as a single `char`.

### 256 color and true color support
The current implementation supports only the 16 standard ANSI colors. Modern terminals support 256-color mode and 24-bit true color (16 million 
colors). Supporting these would require replacing the `TerminalColor` enum with a richer color model; for example a sealed class with variants for 
default, indexed (256), and RGB.
