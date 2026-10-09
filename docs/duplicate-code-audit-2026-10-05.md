# Code Duplication Audit - 2026-10-05

This document catalogs duplicated, redundant, and copy-pasted code structures identified across the `runelite-plugin-better-map-2` codebase. Detection was conducted using token-based clone analysis (`jscpd`) with comment and license-header suppression, followed by manual structural review.

Baseline codebase scan:
- **Analyzed source files:** 137 Java source files in `src/main/java`
- **Identified code clones:** 87 distinct clone pairs in production Java
- **Duplicated production lines:** ~2,593 lines (~5.78% of total production lines)
- **Duplicated test lines:** ~1,840 lines across test harnesses

---

## Duplication Summary Matrix

| # | Cluster / Pattern | Affected Files | Clone Type | Est. Duplicated Lines | Potential Lines Saved | Risk Level |
|---|-------------------|----------------|------------|-----------------------|-----------------------|------------|
| 1 | **Spatial Chunk Indexing & Bounding Queries** | 5 files (`GroundItemIndex`, `MonsterIndex`, `PoiIndex`, `ShopIndex`, `PoiDetails`) | Exact & Parametric | ~140 lines | ~110 lines | Very Low |
| 2 | **Top-Bar Chrome Chip / Button Rendering** | 1 file (`MapChromeRenderer`) | Parametric | ~180 lines | ~130 lines | Low |
| 3 | **Floating Marker Hover Label Rendering** | 3 files (`BoatMarkerRenderer`, `MonsterMarkerRenderer`, `LayerMarkerRenderer`) | Exact | ~60 lines | ~45 lines | Low |
| 4 | **Nearest 2D Entity Distance Search** | 3 files (`BoatTracker`, `PortNoticeBoard`, `SailingPort`) | Exact | ~55 lines | ~40 lines | Very Low |
| 5 | **TSV Resource File Parsing** | 3 files (`OverlayCluster`, `OverlayFloor`, `DungeonPoiOverrides`) | Exact | ~65 lines | ~45 lines | Very Low |
| 6 | **Rune Pouch & Container Item Counting** | 1 file (`BankPickupRequirements`) | Exact | ~40 lines | ~25 lines | Low |
| 7 | **Search Result & Hover Detail Resolution** | 1 file (`MapFinderRenderer`) | Parametric | ~50 lines | ~35 lines | Low |
| 8 | **Layer / Cluster Icon & Exclamation Drawing** | 1 file (`LayerMarkerRenderer`) | Exact | ~35 lines | ~25 lines | Very Low |
| 9 | **Test Harness Config Mocks & Graphics Stubs** | 5+ test files (`ClueButtonTest`, `DestinationAndBoatsButtonTest`, etc.) | Structural | ~300+ lines | ~200 lines | None |

---

## Detailed Duplication Findings

### 1. Spatial Chunk Indexing & Bounding Queries

#### Locations
- [`GroundItemIndex.java#L80-L83`](../src/main/java/com/bettermap/map/GroundItemIndex.java#L80-L83), [`#L200-L214`](../src/main/java/com/bettermap/map/GroundItemIndex.java#L200-L214), [`#L244-L263`](../src/main/java/com/bettermap/map/GroundItemIndex.java#L244-L263)
- [`MonsterIndex.java#L76-L79`](../src/main/java/com/bettermap/map/MonsterIndex.java#L76-L79), [`#L268-L281`](../src/main/java/com/bettermap/map/MonsterIndex.java#L268-L281), [`#L308-L326`](../src/main/java/com/bettermap/map/MonsterIndex.java#L308-L326)
- [`PoiIndex.java#L120-L123`](../src/main/java/com/bettermap/map/PoiIndex.java#L120-L123), [`#L823-L836`](../src/main/java/com/bettermap/map/PoiIndex.java#L823-L836), [`#L872-L890`](../src/main/java/com/bettermap/map/PoiIndex.java#L872-L890)
- [`ShopIndex.java#L77-L80`](../src/main/java/com/bettermap/map/ShopIndex.java#L77-L80), [`#L99-L117`](../src/main/java/com/bettermap/map/ShopIndex.java#L99-L117), [`#L248-L261`](../src/main/java/com/bettermap/map/ShopIndex.java#L248-L261)
- [`PoiDetails.java#L159-L162`](../src/main/java/com/bettermap/map/PoiDetails.java#L159-L162), [`#L1070-L1080`](../src/main/java/com/bettermap/map/PoiDetails.java#L1070-L1080)

#### Duplicated Code Snippet
Each class defines the identical bit-packing helper:
```java
private static long chunkKey(int plane, int chunkX, int chunkY)
{
    return (((long) plane & 0x3L) << 32) | (((long) (chunkX & 0xFFFF) << 16)) | ((long) (chunkY & 0xFFFF));
}
```
And the identical spatial chunk bounding iteration loop:
```java
final int minChunkX = minX >> 6;
final int maxChunkX = maxX >> 6;
final int minChunkY = minY >> 6;
final int maxChunkY = maxY >> 6;

for (int cx = minChunkX; cx <= maxChunkX; cx++)
{
    for (int cy = minChunkY; cy <= maxChunkY; cy++)
    {
        final List<T> chunkItems = chunkMap.get(chunkKey(plane, cx, cy));
        if (chunkItems == null)
        {
            continue;
        }
        for (T item : chunkItems)
        {
            // filter and collect
        }
    }
}
```

#### Recommendation
Extract `chunkKey` and a generic spatial query helper method into a shared package utility, e.g. `SpatialChunkIndex` or `MapChunkKey`:
```java
public final class MapChunkKey
{
    public static long of(int plane, int chunkX, int chunkY) { ... }
    public static <T> void forEachChunk(Map<Long, List<T>> map, int plane, int minX, int minY, int maxX, int maxY, Consumer<T> action) { ... }
}
```

---

### 2. Top-Bar Navigation Chip / Button Rendering

#### Locations
- [`MapChromeRenderer.java#L318-L348`](../src/main/java/com/bettermap/ui/MapChromeRenderer.java#L318-L348) (`drawClueButton`)
- [`MapChromeRenderer.java#L370-L405`](../src/main/java/com/bettermap/ui/MapChromeRenderer.java#L370-L405) (`drawQuestButton`)
- [`MapChromeRenderer.java#L420-L455`](../src/main/java/com/bettermap/ui/MapChromeRenderer.java#L420-L455) (`drawPlayerButton`)
- [`MapChromeRenderer.java#L470-L510`](../src/main/java/com/bettermap/ui/MapChromeRenderer.java#L470-L510) (`drawDestinationButton`)
- [`MapChromeRenderer.java#L525-L560`](../src/main/java/com/bettermap/ui/MapChromeRenderer.java#L525-L560) (`drawBoatsButton`)

#### Duplicated Code Snippet
All five chip buttons execute this identical sequence:
```java
final String label = ...;
final int textW = graphics.getFontMetrics().stringWidth(label);
final int x = ...;
final int y = (int) bounds.getMinY() + 8;
final int glyph = 12;
final Rectangle button = new Rectangle(x, y, glyph + 8 + textW + 14, 20);

final int rightLimit = layout.topRightStripRight(bounds)
    - (config.fullscreenMap() ? LEFT_TOOLBAR_BUTTON_SIZE + 8 : 0);
if (button.x + button.width > rightLimit)
{
    camera.setXButton(null);
    return;
}

graphics.setColor(CARD_BG);
graphics.fillRoundRect(button.x, button.y, button.width, button.height, 7, 7);
graphics.setColor(CARD_TITLE);
graphics.drawRoundRect(button.x, button.y, button.width, button.height, 7, 7);

// Draw glyph centered at (cx, cy)
final int cx = button.x + 7 + glyph / 2;
final int cy = button.y + button.height / 2;
...

graphics.setColor(CARD_TITLE);
graphics.drawString(label, button.x + 7 + glyph + 8, button.y + 14);
camera.setXButton(button);
```

#### Recommendation
Extract into a single parameterized method in `MapChromeRenderer`:
```java
private Rectangle drawTopBarChip(Graphics2D graphics, Rectangle bounds, int x, String label, Consumer<Point> glyphDrawer)
{
    // Common measurement, clipping, card background, card border, glyph invocation, and text drawing
}
```

---

### 3. Floating Marker Hover Label Geometry & Rendering

#### Locations
- [`BoatMarkerRenderer.java#L424-L436`](../src/main/java/com/bettermap/ui/markers/BoatMarkerRenderer.java#L424-L436)
- [`MonsterMarkerRenderer.java#L245-L265`](../src/main/java/com/bettermap/ui/markers/MonsterMarkerRenderer.java#L245-L265)
- [`LayerMarkerRenderer.java#L450-L465`](../src/main/java/com/bettermap/ui/markers/LayerMarkerRenderer.java#L450-L465)

#### Duplicated Code Snippet
```java
graphics.setFont(SMALL);
final int textW = graphics.getFontMetrics().stringWidth(label);
final int textX = sx - textW / 2;
final int textY = rect.y + iconH + 12;

final Rectangle labelRect = new Rectangle(textX - 4, textY - 10, textW + 8, 13);
if (bounds.intersects(labelRect))
{
    graphics.setColor(CARD_BG);
    graphics.fillRoundRect(labelRect.x, labelRect.y, labelRect.width, labelRect.height, 4, 4);
    graphics.setColor(borderColor);
    graphics.setStroke(MARKER_OUTLINE);
    graphics.drawRoundRect(labelRect.x, labelRect.y, labelRect.width, labelRect.height, 4, 4);
    graphics.setColor(textColor);
    graphics.drawString(label, textX, textY);
}
```

#### Recommendation
Create a static helper method in `com.bettermap.ui.markers.MarkerRenderUtil`:
```java
public static void drawHoverPill(Graphics2D graphics, Rectangle bounds, int centerX, int bottomY, String label, Color borderColor, Color textColor)
```

---

### 4. Nearest 2D Euclidean / Chebyshev Distance Search

#### Locations
- [`BoatTracker.java#L417-L433`](../src/main/java/com/bettermap/data/sailing/BoatTracker.java#L417-L433)
- [`PortNoticeBoard.java#L562-L579`](../src/main/java/com/bettermap/data/sailing/PortNoticeBoard.java#L562-L579)
- [`SailingPort.java#L197-L214`](../src/main/java/com/bettermap/data/sailing/SailingPort.java#L197-L214)

#### Duplicated Code Snippet
```java
final int dx = loc.getX() - worldX;
final int dy = loc.getY() - worldY;
if (Math.abs(dx) > maxRadius || Math.abs(dy) > maxRadius)
{
    continue;
}
final int distSq = dx * dx + dy * dy;
if (distSq <= bestDistSq)
{
    bestDistSq = distSq;
    best = candidate;
}
```

#### Recommendation
Extract a small distance helper method into `WorldPointUtil` or a local sailing spatial utility:
```java
public static int distanceSquaredChebyshevBounded(WorldPoint a, int worldX, int worldY, int maxRadius)
```

---

### 5. TSV Resource Parsing Boilerplate

#### Locations
- [`OverlayCluster.java#L160-L185`](../src/main/java/com/bettermap/data/OverlayCluster.java#L160-L185)
- [`OverlayFloor.java#L105-L130`](../src/main/java/com/bettermap/data/OverlayFloor.java#L105-L130)
- [`DungeonPoiOverrides.java#L89-L97, L127-L135`](../src/main/java/com/bettermap/data/DungeonPoiOverrides.java#L89-L97)

#### Duplicated Code Snippet
```java
try (BufferedReader reader = source instanceof BufferedReader
    ? (BufferedReader) source
    : new BufferedReader(source))
{
    String line;
    while ((line = reader.readLine()) != null)
    {
        if (line.isEmpty() || line.charAt(0) == '#')
        {
            continue;
        }
        final String[] f = line.split("\t", -1);
        if (f.length < minColumns)
        {
            continue;
        }
        ...
    }
}
```

#### Recommendation
Provide a shared TSV row consumer:
```java
public static void forEachTsvRow(Reader source, int minColumns, Consumer<String[]> rowConsumer) throws IOException
```

---

### 6. Rune Pouch Varbit Unpacking & Container Item Counting

#### Locations
- [`BankPickupRequirements.java#L394-L406, L456-L468`](../src/main/java/com/bettermap/pathfinding/transport/BankPickupRequirements.java#L394-L406) (Rune pouch varbits)
- [`BankPickupRequirements.java#L420-L439`](../src/main/java/com/bettermap/pathfinding/transport/BankPickupRequirements.java#L420-L439) (Item container merge)

#### Duplicated Code Snippet
Varbit reading loop:
```java
for (int i = 0; i < PathfinderConfig.RUNE_POUCH_RUNE_VARBITS.length; i++)
{
    int runeEnumId = client.getVarbitValue(PathfinderConfig.RUNE_POUCH_RUNE_VARBITS[i]);
    int runeId = runeEnumId > 0 ? runePouchEnum.getIntValue(runeEnumId) : 0;
    int runeAmount = client.getVarbitValue(PathfinderConfig.RUNE_POUCH_AMOUNT_VARBITS[i]);
    if (runeId > 0 && runeAmount > 0)
    {
        // merge into map
    }
}
```
Container merging:
```java
if (container != null)
{
    for (Item item : container.getItems())
    {
        if (item.getId() >= 0 && item.getQuantity() > 0)
        {
            totals.merge(item.getId(), item.getQuantity(), Integer::sum);
        }
    }
}
```

#### Recommendation
Extract private helpers `unpackRunePouchRunes(Client client, BiConsumer<Integer, Integer> consumer)` and `mergeContainerItems(ItemContainer container, Map<Integer, Integer> target)` inside `BankPickupRequirements`.

---

### 7. Search Result & Hover Detail Resolution

#### Locations
- [`MapFinderRenderer.java#L499-L525, L539-L565`](../src/main/java/com/bettermap/ui/MapFinderRenderer.java#L499-L525)

#### Duplicated Code Snippet
Both `flyoutHoveredItem` and `hoveredTarget.getResult()` execute identical POI lookup and 2-line description formatting:
```java
final PoiIndex.Poi poi = (poiIndex != null && pt != null) ? poiIndex.nearest(pt.getX(), pt.getY(), pt.getPlane(), 2) : null;
final PoiDetails.Detail detail = poi != null
    ? PoiDetails.getDetail(poi, pt.getX(), pt.getY(), pt.getPlane())
    : (pt != null ? PoiDetails.getDetailByPosition(pt.getX(), pt.getY(), pt.getPlane(), 6) : null);

if (detail != null)
{
    descLine1 = (detail.getCategory() != null ? detail.getCategory() : "Point of Interest") + " · " + regName;
    descLine2 = !detail.getLines().isEmpty() ? detail.getLines().get(0) : fallbackName;
}
else
{
    descLine1 = defaultCategory + " · " + regName;
    descLine2 = fallbackName;
}
```

#### Recommendation
Extract into a private helper `resolveDetailLines(WorldPoint pt, String regName, String defaultCategory, String fallbackName)`.

---

### 8. Layer / Cluster Marker Icon & Exclamation Glyph Drawing

#### Locations
- [`LayerMarkerRenderer.java#L258-L278, L442-L460`](../src/main/java/com/bettermap/ui/markers/LayerMarkerRenderer.java#L258-L278)

#### Duplicated Code Snippet
```java
if (isHovered)
{
    graphics.setColor(borderColor);
    graphics.setStroke(SYMBOL_BORDER_HOVER);
    graphics.drawOval(rect.x - 2, rect.y - 2, rect.width + 4, rect.height + 4);
}
if (icon != null)
{
    graphics.drawImage(icon, rect.x, rect.y, rect.width, rect.height, null);
}
else
{
    graphics.setColor(borderColor);
    graphics.setStroke(SYMBOL_BORDER);
    graphics.drawOval(rect.x, rect.y, rect.width - 1, rect.height - 1);
    graphics.setFont(SMALL);
    final int bangW = graphics.getFontMetrics().stringWidth("!");
    graphics.drawString("!", sx - bangW / 2, sy + 4);
}
```

#### Recommendation
Extract a private helper `drawZoneOrClusterIcon(Graphics2D graphics, Rectangle rect, int sx, int sy, BufferedImage icon, boolean isHovered, Color borderColor)`.

---

### 9. Test Suite Setup & Fixture Duplication

#### Locations
- [`ClueButtonTest.java`](../src/test/java/com/bettermap/ui/ClueButtonTest.java)
- [`DestinationAndBoatsButtonTest.java`](../src/test/java/com/bettermap/ui/DestinationAndBoatsButtonTest.java)
- [`DualTooltipBugFixTest.java`](../src/test/java/com/bettermap/ui/DualTooltipBugFixTest.java)
- [`WorldMapInputFinderTest.java`](../src/test/java/com/bettermap/map/WorldMapInputFinderTest.java)
- [`WorldMapInputLayerToggleTest.java`](../src/test/java/com/bettermap/map/WorldMapInputLayerToggleTest.java)

#### Duplicated Code Snippet
- Identical `TestConfig` mock implementations with dozens of stubbed methods.
- Repetitive `new BufferedImage(1000, 800, BufferedImage.TYPE_INT_ARGB).createGraphics()`.
- Static `@BeforeClass` data loading assertions.

#### Recommendation
Create a shared test base class `BaseMapUiTest` or `MockBetterMapConfig` in `src/test/java/com/bettermap/ui/` to eliminate ~200+ lines of mock configuration boilerplate.

---

## Action Plan & Verification

1. **Phase 1 (Isolated Internal Helpers):**
   - Refactor intra-file duplicates in `MapChromeRenderer`, `LayerMarkerRenderer`, and `BankPickupRequirements`.
   - Re-run `./gradlew test` after each method extraction.

2. **Phase 2 (Cross-Class Utilities):**
   - Create `MapChunkKey` / `SpatialChunkIndex` to deduplicate spatial indexing across `GroundItemIndex`, `MonsterIndex`, `PoiIndex`, `ShopIndex`, and `PoiDetails`.
   - Create `MarkerRenderUtil` for hover pills.

3. **Phase 3 (Test Harness Consolidation):**
   - Consolidate common mock configuration classes into a shared test fixture.
