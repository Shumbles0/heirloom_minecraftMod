import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.util.Random;

/**
 * One-shot generator for the L20 ritual blocks/items: distinct placeholder textures plus all
 * their blockstate/model/item-definition JSON. Not part of the mod build (lives in tools/).
 * Run from the project root with:  java tools/GenRitualAssets.java
 */
public class GenRitualAssets {
	static final String RES = "src/main/resources/assets/heirloom/";

	// id, base colour, accent colour, motif, seed
	static final String[][] BLOCKS = {
		{"ritual_forge",     "0x3a3a3a", "0xff7a1a", "speckle", "11"},
		{"quenching_trough", "0x6a6a72", "0x2a6cff", "water",   "12"},
		{"chopping_stump",   "0x7a5230", "0x4a2f18", "wood",    "13"},
		{"deepvein",         "0x2b2b30", "0x2ad6c0", "speckle", "14"},
		{"avalanche_cairn",  "0x8a8378", "0x5a544a", "speckle", "15"},
		{"skeet_launcher",   "0x4a6a3a", "0x2e4426", "rivets",  "16"},
		{"bullseye_target",  "0xd22f2f", "0xf0f0f0", "rings",   "17"},
		{"striking_plate",   "0x7d818c", "0x3c3f47", "rivets",  "18"},
		{"pressure_seal",    "0x2f6e6a", "0x1c413f", "rivets",  "19"},
		{"freeze_machine",   "0xa9e6f2", "0xffffff", "frost",   "20"},
		{"heatbed",          "0x2a1a14", "0xff5a1a", "grid",    "21"},
	};
	// id, base, accent, motif
	static final String[][] ITEMS = {
		{"suicide_charge", "0x222222", "0xd11a1a", "stripes"},
		{"clay_pigeon",    "0xe07a2a", "0x9a4f17", "disc"},
	};

	public static void main(String[] args) throws Exception {
		for (String[] b : BLOCKS) {
			String id = b[0];
			BufferedImage img = blockTex(16, parse(b[1]), parse(b[2]), b[3], Long.parseLong(b[4]));
			savePng(RES + "textures/block/" + id + ".png", img);
			write(RES + "blockstates/" + id + ".json",
				"{\n  \"variants\": {\n    \"\": { \"model\": \"heirloom:block/" + id + "\" }\n  }\n}\n");
			write(RES + "models/block/" + id + ".json",
				"{\n  \"parent\": \"minecraft:block/cube_all\",\n  \"textures\": { \"all\": \"heirloom:block/" + id + "\" }\n}\n");
			write(RES + "items/" + id + ".json",
				"{\n  \"model\": { \"type\": \"minecraft:model\", \"model\": \"heirloom:block/" + id + "\" }\n}\n");
		}
		for (String[] it : ITEMS) {
			String id = it[0];
			BufferedImage img = itemTex(32, parse(it[1]), parse(it[2]), it[3]);
			savePng(RES + "textures/item/" + id + ".png", img);
			write(RES + "models/item/" + id + ".json",
				"{\n  \"parent\": \"minecraft:item/generated\",\n  \"textures\": { \"layer0\": \"heirloom:item/" + id + "\" }\n}\n");
			write(RES + "items/" + id + ".json",
				"{\n  \"model\": { \"type\": \"minecraft:model\", \"model\": \"heirloom:item/" + id + "\" }\n}\n");
		}
		System.out.println("Generated " + (BLOCKS.length + ITEMS.length) + " ritual assets.");
	}

	// ---- block textures (16x16, opaque) ------------------------------------

	static BufferedImage blockTex(int s, int base, int accent, String motif, long seed) {
		BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_RGB);
		Random r = new Random(seed);
		for (int y = 0; y < s; y++) {
			for (int x = 0; x < s; x++) {
				int c = shade(base, r.nextInt(21) - 10);
				if (x == 0 || y == 0 || x == s - 1 || y == s - 1) c = shade(base, -45);
				img.setRGB(x, y, c);
			}
		}
		double cx = s / 2.0 - 0.5, cy = s / 2.0 - 0.5;
		switch (motif) {
			case "speckle" -> {
				for (int i = 0; i < 26; i++) {
					int x = 1 + r.nextInt(s - 2), y = 1 + r.nextInt(s - 2);
					img.setRGB(x, y, shade(accent, r.nextInt(31) - 15));
				}
			}
			case "water" -> {
				for (int y = 3; y < s - 3; y++)
					for (int x = 3; x < s - 3; x++)
						img.setRGB(x, y, shade(accent, (int) (Math.sin((x + y) * 0.9) * 14)));
			}
			case "wood" -> {
				for (int y = 0; y < s; y++)
					for (int x = 0; x < s; x++) {
						double d = Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy));
						if (((int) (d * 1.3)) % 2 == 0 && x > 0 && y > 0 && x < s - 1 && y < s - 1)
							img.setRGB(x, y, shade(accent, r.nextInt(11) - 5));
					}
			}
			case "rivets" -> {
				int[][] pts = {{3, 3}, {s - 4, 3}, {3, s - 4}, {s - 4, s - 4}, {(int) cx, (int) cy}};
				for (int[] p : pts) {
					img.setRGB(p[0], p[1], shade(accent, -20));
					img.setRGB(p[0] + 1, p[1], accent);
					img.setRGB(p[0], p[1] + 1, accent);
					img.setRGB(p[0] + 1, p[1] + 1, shade(accent, 40));
				}
			}
			case "rings" -> {
				for (int y = 0; y < s; y++)
					for (int x = 0; x < s; x++) {
						double d = Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy));
						int ring = (int) (d / 1.7);
						int c = (ring % 2 == 0) ? base : accent;
						if (x == 0 || y == 0 || x == s - 1 || y == s - 1) c = shade(base, -45);
						img.setRGB(x, y, c);
					}
				img.setRGB((int) cx, (int) cy, 0x202020);
			}
			case "frost" -> {
				for (int i = 0; i < 18; i++) {
					int x = 1 + r.nextInt(s - 2), y = 1 + r.nextInt(s - 2);
					img.setRGB(x, y, accent);
				}
				for (int k = -s; k < s; k += 5)
					for (int x = 1; x < s - 1; x++) {
						int y = x + k;
						if (y > 0 && y < s - 1) img.setRGB(x, y, shade(accent, -10));
					}
			}
			case "grid" -> {
				for (int y = 1; y < s - 1; y++)
					for (int x = 1; x < s - 1; x++)
						if (x % 4 == 0 || y % 4 == 0)
							img.setRGB(x, y, shade(accent, r.nextInt(31) - 15));
			}
			default -> { }
		}
		return img;
	}

	// ---- item textures (32x32, transparent background) ---------------------

	static BufferedImage itemTex(int s, int base, int accent, String motif) {
		BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
		double cx = s / 2.0 - 0.5, cy = s / 2.0 - 0.5;
		if (motif.equals("disc")) {
			double rad = s * 0.42;
			for (int y = 0; y < s; y++)
				for (int x = 0; x < s; x++) {
					double d = Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy));
					if (d <= rad) img.setRGB(x, y, 0xFF000000 | (d > rad - 3 ? accent : base));
				}
		} else { // stripes: an armed charge — striped body + fuse
			int x0 = 6, x1 = s - 6, y0 = 9, y1 = s - 3;
			for (int y = y0; y < y1; y++)
				for (int x = x0; x < x1; x++) {
					boolean edge = (x == x0 || x == x1 - 1 || y == y0 || y == y1 - 1);
					int col = (((x + y) / 4) % 2 == 0) ? accent : base;
					if (edge) col = 0x111111;
					img.setRGB(x, y, 0xFF000000 | col);
				}
			for (int y = 3; y < y0; y++) img.setRGB(s / 2, y, 0xFF6a6a6a);   // fuse
			img.setRGB(s / 2, 2, 0xFFffd23a);                                 // spark
			img.setRGB(s / 2 - 1, 3, 0xFFffa31a);
			img.setRGB(s / 2 + 1, 3, 0xFFffa31a);
		}
		return img;
	}

	// ---- helpers -----------------------------------------------------------

	static int parse(String hex) { return (int) Long.parseLong(hex.substring(2), 16); }

	static int shade(int rgb, int d) {
		int r = clamp(((rgb >> 16) & 0xff) + d);
		int g = clamp(((rgb >> 8) & 0xff) + d);
		int b = clamp((rgb & 0xff) + d);
		return (r << 16) | (g << 8) | b;
	}

	static int clamp(int v) { return v < 0 ? 0 : (v > 255 ? 255 : v); }

	static void savePng(String path, BufferedImage img) throws Exception {
		File f = new File(path);
		f.getParentFile().mkdirs();
		ImageIO.write(img, "png", f);
	}

	static void write(String path, String content) throws Exception {
		File f = new File(path);
		f.getParentFile().mkdirs();
		try (FileWriter w = new FileWriter(f)) {
			w.write(content);
		}
	}
}
