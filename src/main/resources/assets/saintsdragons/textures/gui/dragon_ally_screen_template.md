# Dragon Ally Screen GUI Texture Template

## Dimensions: 256x200 pixels

### Layout Structure:
```
┌─────────────────────────────────────────────────────────────┐
│ Title: "Dragon Ally Management" (centered, top 10px)        │
├─────────────────────────────────────────────────────────────┤
│ Username Input: [________________] [Add Ally] [Remove Ally] │
│ (20px from left, 30px from top, 150px wide input)          │
├─────────────────────────────────────────────────────────────┤
│ Ally Count: "Allies: X/10" (20px from left, 80px from top) │
├─────────────────────────────────────────────────────────────┤
│ Ally List: (starts at 100px from top)                       │
│   - PlayerName1                                             │
│   - PlayerName2                                             │
│   - PlayerName3                                             │
│   ...                                                       │
│   (scrollable, 8 visible entries max)                      │
├─────────────────────────────────────────────────────────────┤
│ Close Button: [Cancel] (bottom right, 30px from edges)      │
└─────────────────────────────────────────────────────────────┘
```

### Color Scheme Suggestions:
- **Background**: Dark medieval theme (#2C1810 or #1A1A1A)
- **Border**: Gold/brass accent (#D4AF37 or #B8860B)
- **Text**: Light colors (#F5F5DC or #FFFFFF)
- **Buttons**: Metallic (#8B7355 or #696969)
- **Input Field**: Slightly lighter background (#3A3A3A)

### Design Elements:
1. **Medieval/Dragon Theme**: Use fantasy elements like:
   - Dragon scales pattern in background
   - Ornate borders with dragon motifs
   - Ancient scroll-like texture
   - Mystical glowing effects

2. **Button Styles**:
   - Raised/3D appearance
   - Hover effects (lighter color)
   - Pressed effects (darker color)

3. **Input Field**:
   - Inset appearance
   - Cursor indicator
   - Focus highlight

### File Requirements:
- **Format**: PNG with transparency support
- **Size**: Exactly 256x200 pixels
- **Location**: `src/main/resources/assets/saintsdragons/textures/gui/dragon_ally_screen.png`
- **Color Depth**: 32-bit RGBA recommended

### Alternative Simple Design:
If you want a simpler approach, you could use:
- **Solid background**: #2C1810 (dark brown)
- **Simple border**: 2px solid #D4AF37 (gold)
- **Clean, minimal buttons**: Flat design with hover states
- **Standard Minecraft-style**: Match vanilla GUI aesthetics

### Tools for Creation:
- **Pixel Art**: Aseprite, Piskel, or GIMP
- **Vector Graphics**: Inkscape (export as PNG)
- **Online**: Canva, Figma, or similar web tools
- **Minecraft-specific**: Use existing Minecraft GUI textures as reference

### Testing:
After creating the texture, test it by:
1. Building the mod: `./gradlew.bat build`
2. Running the client: `./gradlew.bat runClient`
3. Getting a Dragon Ally Book from creative menu
4. Right-clicking on a tamed dragon to open the GUI
