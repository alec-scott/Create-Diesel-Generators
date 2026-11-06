import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Splits 1.20-style connected-texture sheets ({@code <name>_connected.png}) into the per-tile sprites Create Fly
 * loads ({@code <name>_connected/<index>.png}, index 0 being the base texture itself), then deletes the sheet.
 *
 * <p>The tile -> sheet cell tables were derived by pixel-matching Create Fly's split tiles against the sheets they
 * replaced (Create Fly commit 48e17b9e "Update CTModel sprite"; andesite/brass casing for omnidirectional,
 * (creative) fluid tank for rectangle). 2x2 sheets (horizontal kryppers, vertical, Railways' vertical pinkmachine)
 * keep their 1.20 indices, so tile N is cell N.</p>
 *
 * <p>Usage, from the repo root: {@code java porting-tools/SplitConnectedTextures.java src/main/resources/assets/railways/textures}</p>
 */
public class SplitConnectedTextures {
    private static final int[] TWO_BY_TWO = {0, 1, 2, 3};
    // AllCTTypes.CROSS (4x4 sheet, AXIS_ALIGNED flags: tile index = cell)
    private static final int[] CROSS = {-1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    // AllCTTypes.RECTANGLE (4x4 sheet)
    private static final int[] RECTANGLE = {-1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 13, 14, 15};
    // AllCTTypes.OMNIDIRECTIONAL (8x8 sheet)
    private static final int[] OMNIDIRECTIONAL = {-1,
        1, 2, 3, 8, 9, 10, 11, 12, 13, 16, 17, 18, 19, 20, 21, 24, 25, 26, 27, 28, 29, 30,
        32, 33, 34, 35, 36, 37, 38, 40, 41, 42, 43, 44, 45, 46, 48, 49, 50, 51, 52, 53, 54, 56, 57, 58};

    public static void main(String[] args) throws IOException {
        List<Path> sheets;
        try (Stream<Path> files = Files.walk(Path.of(args[0]))) {
            sheets = files.filter(p -> p.getFileName().toString().endsWith("_connected.png")).sorted().toList();
        }
        for (Path sheetPath : sheets) {
            String name = sheetPath.getFileName().toString();
            Path base = sheetPath.resolveSibling(name.replace("_connected.png", ".png"));
            BufferedImage sheet = ImageIO.read(sheetPath.toFile());
            // CDG: a few sheets (distillation_tank_pipes, oil barrel colours) have no sibling base texture; they are 4x4
            int size = Files.exists(base) ? ImageIO.read(base.toFile()).getWidth() : sheet.getWidth() / 4;
            int cells = sheet.getWidth() / size;
            boolean cross = name.startsWith("diesel_engine_big");
            int[] map = cross ? CROSS : switch (cells) {
                case 2 -> TWO_BY_TWO;
                case 4 -> RECTANGLE;
                case 8 -> OMNIDIRECTIONAL;
                default -> throw new IllegalStateException(sheetPath + ": unexpected " + cells + "x" + cells + " sheet");
            };

            Path dir = sheetPath.resolveSibling(name.replace(".png", ""));
            Files.createDirectories(dir);
            for (int tile = 1; tile < map.length; tile++) {
                int cell = map[tile];
                BufferedImage out = sheet.getSubimage((cell % cells) * size, (cell / cells) * size, size, size);
                ImageIO.write(out, "png", dir.resolve(tile + ".png").toFile());
            }
            Files.delete(sheetPath);
        }
        System.out.println("split " + sheets.size() + " connected texture sheets");
    }
}
