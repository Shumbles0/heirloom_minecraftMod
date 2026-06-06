import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * One-shot: recolours the vanilla bookshelf + enchanting-table textures into the Heirloom
 * "arcane" set. Not part of the build. Run from the project root after extracting the vanilla
 * PNGs into tools/vanilla_src/:
 *   javac -d tools/out tools/RecolorEnchantAssets.java && java -cp tools/out RecolorEnchantAssets
 *
 * - Bookshelf + planks  -> deep blue / dark purple (hue forced, detail preserved).
 * - Enchanting table     -> blue, with the brightest (light-blue) accents flipped to bright red.
 */
public class RecolorEnchantAssets {
	static final String SRC = "tools/vanilla_src/";
	static final String OUT = "src/main/resources/assets/heirloom/textures/block/";

	public static void main(String[] args) throws Exception {
		new File(OUT).mkdirs();
		// Deep blue / dark purple (~262 deg). Frame planks a touch darker than the spines.
		recolour(SRC + "bookshelf.png",   OUT + "arcane_bookshelf.png",     0.728f, 1.0f, 0.95f, false);
		recolour(SRC + "oak_planks.png",  OUT + "arcane_bookshelf_end.png", 0.728f, 0.95f, 0.72f, false);
		// Table: blue (~222 deg), brightest accents -> bright red.
		recolour(SRC + "enchanting_table_top.png",    OUT + "advanced_enchanting_table_top.png",    0.615f, 1.0f, 1.0f, true);
		recolour(SRC + "enchanting_table_side.png",   OUT + "advanced_enchanting_table_side.png",   0.615f, 1.0f, 1.0f, true);
		recolour(SRC + "enchanting_table_bottom.png", OUT + "advanced_enchanting_table_bottom.png", 0.615f, 1.0f, 1.0f, true);
		System.out.println("Recolour complete.");
	}

	static void recolour(String in, String outPath, float hue, float satScale, float valScale, boolean invertBright)
			throws Exception {
		BufferedImage img = ImageIO.read(new File(in));
		int w = img.getWidth(), h = img.getHeight();
		BufferedImage o = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int argb = img.getRGB(x, y);
				int a = (argb >>> 24) & 0xFF;
				if (a == 0) {
					o.setRGB(x, y, 0);
					continue;
				}
				int r = (argb >> 16) & 0xFF, g = (argb >> 8) & 0xFF, b = argb & 0xFF;
				float[] hsb = Color.RGBtoHSB(r, g, b, null);
				float s = Math.min(1f, hsb[1] * satScale);
				float v = Math.min(1f, hsb[2] * valScale);
				if (hsb[1] < 0.10f) {
					s = Math.max(s, 0.40f); // tint near-grey pixels so the wood/stone reads coloured
				}
				int rgb;
				if (invertBright && v > 0.72f) {
					rgb = Color.HSBtoRGB(0.0f, 0.95f, Math.max(v, 0.9f)); // light-blue accent -> bright red
				} else {
					rgb = Color.HSBtoRGB(hue, s, v);
				}
				o.setRGB(x, y, (a << 24) | (rgb & 0xFFFFFF));
			}
		}
		ImageIO.write(o, "png", new File(outPath));
		System.out.println("wrote " + outPath);
	}
}
