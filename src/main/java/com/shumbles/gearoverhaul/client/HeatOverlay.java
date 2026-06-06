package com.shumbles.gearoverhaul.client;

import com.shumbles.gearoverhaul.ritual.RitualBlocks;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.BlockPos;

/**
 * A purely client-side fire overlay for the boots' heat-bed walk: deep-red flames lick inward from
 * all four edges of the screen (the same "you're on fire" idea, recoloured from orange to a darker
 * red and mirrored to every side), building while the player stands on Heat-bed blocks and fading
 * once they step off. No effect on gameplay.
 */
public final class HeatOverlay {
	/** Deep-red flame palette, base → tip (kept low on green/blue so it reads redder than vanilla). */
	private static final int[][] FLAME = {
		{160, 12, 0}, {200, 24, 4}, {235, 40, 10}, {255, 64, 20}
	};
	private static final int SEGMENTS = FLAME.length;
	private static final int TONGUE_STEP = 10; // px between flame tongues along an edge
	private static final int TONGUE_WIDTH = 9;

	private static float heat = 0.0f;
	private static int tick = 0;

	private HeatOverlay() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			tick++;
			ClientPlayerEntity player = client.player;
			if (player == null || client.world == null) {
				heat = 0.0f;
				return;
			}
			BlockPos below = BlockPos.ofFloored(player.getX(), player.getBoundingBox().minY - 0.01, player.getZ());
			boolean onHeatBed = client.world.getBlockState(below).isOf(RitualBlocks.HEATBED);
			heat = onHeatBed
				? Math.min(1.0f, heat + 0.04f)
				: Math.max(0.0f, heat - 0.05f);
		});

		HudRenderCallback.EVENT.register((context, tickCounter) -> {
			if (heat <= 0.0f) {
				return;
			}
			renderFire(context, context.getScaledWindowWidth(), context.getScaledWindowHeight(), heat, tick);
		});
	}

	private static void renderFire(DrawContext ctx, int w, int h, float intensity, int t) {
		int depth = (int) (intensity * Math.min(w, h) * 0.20f);
		if (depth < 4) {
			return;
		}

		// A faint red glow band along each edge, so the screen reads as "burning" between tongues.
		int glow = (int) (intensity * 70);
		int clear = color(0, 90, 0, 0);
		ctx.fillGradient(0, h - depth, w, h, clear, color(glow, 90, 0, 0));
		ctx.fillGradient(0, 0, w, depth, color(glow, 90, 0, 0), clear);
		for (int i = 0; i < depth; i++) {
			int a = (int) (glow * (1.0f - (float) i / depth));
			int c = color(a, 90, 0, 0);
			ctx.fill(i, 0, i + 1, h, c);             // left
			ctx.fill(w - i - 1, 0, w - i, h, c);     // right
		}

		// Flickering flame tongues on all four edges.
		for (int x = 0; x < w; x += TONGUE_STEP) {
			horizontalTongue(ctx, x, h, -1, depth, intensity, t);       // bottom (rises up)
			horizontalTongue(ctx, x, 0, 1, depth, intensity, t + 37);   // top (descends)
		}
		for (int y = 0; y < h; y += TONGUE_STEP) {
			verticalTongue(ctx, 0, y, 1, depth, intensity, t + 71);     // left (reaches right)
			verticalTongue(ctx, w, y, -1, depth, intensity, t + 113);   // right (reaches left)
		}
	}

	private static void horizontalTongue(DrawContext ctx, int x, int baseY, int sign, int depth, float intensity, int t) {
		int th = (int) (depth * (0.45f + 0.55f * flicker(x, t)));
		for (int seg = 0; seg < SEGMENTS; seg++) {
			int y0 = baseY + sign * (th * seg / SEGMENTS);
			int y1 = baseY + sign * (th * (seg + 1) / SEGMENTS);
			int inset = seg;
			ctx.fill(x + inset, Math.min(y0, y1), x + TONGUE_WIDTH - inset, Math.max(y0, y1), segColor(seg, intensity));
		}
	}

	private static void verticalTongue(DrawContext ctx, int baseX, int y, int sign, int depth, float intensity, int t) {
		int th = (int) (depth * (0.45f + 0.55f * flicker(y, t)));
		for (int seg = 0; seg < SEGMENTS; seg++) {
			int x0 = baseX + sign * (th * seg / SEGMENTS);
			int x1 = baseX + sign * (th * (seg + 1) / SEGMENTS);
			int inset = seg;
			ctx.fill(Math.min(x0, x1), y + inset, Math.max(x0, x1), y + TONGUE_WIDTH - inset, segColor(seg, intensity));
		}
	}

	/** Two summed sine waves → a steady 0..1 flicker that animates with {@code t}. */
	private static float flicker(int seed, int t) {
		double a = Math.sin(seed * 0.7 + t * 0.30);
		double b = Math.sin(seed * 1.9 - t * 0.17);
		return (float) ((a * 0.5 + b * 0.5) * 0.5 + 0.5);
	}

	private static int segColor(int seg, float intensity) {
		int a = (int) (intensity * (155 - seg * 30));
		return color(a, FLAME[seg][0], FLAME[seg][1], FLAME[seg][2]);
	}

	private static int color(int a, int r, int g, int b) {
		a = Math.max(0, Math.min(255, a));
		return (a << 24) | (r << 16) | (g << 8) | b;
	}
}
