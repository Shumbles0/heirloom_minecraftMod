package com.shumbles.gearoverhaul.client;

import com.shumbles.gearoverhaul.codex.CodexComponents;
import com.shumbles.gearoverhaul.codex.CodexEntries;
import com.shumbles.gearoverhaul.codex.CodexItems;
import com.shumbles.gearoverhaul.enchant.ArcaneAttribute;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Full-size open-book reading view. The left page is a scrollable, collapsible tree of the
 * player's UNLOCKED chapters — rarity groups, then gear-type items, then the five-level
 * band chapters — with the always-on overview pinned at the top. The right page shows the
 * selected entry and scrolls for long text. Pure client-side; recipe text is built from the
 * bundled tempering tables via {@link CodexContent}.
 */
public class CodexReadingScreen extends Screen {
	private static final int COVER = 0xFF3A2A1A;
	private static final int PAGE = 0xFFEFE4C8;
	private static final int SPINE = 0xFF241A10;
	private static final int INK = 0xFF3A2A18;
	private static final int INK_FADED = 0xFF6B5A40;
	private static final int SELECTED_BG = 0x553A2A18;
	private static final int HOVER_BG = 0x2A3A2A18;
	private static final int ROW_HEIGHT = 12;
	private static final float BODY_SCALE = 1.0f;
	private static final int RECIPE_CELL = 18;
	private static final int RECIPE_BLOCK_H = 11 + 3 * RECIPE_CELL + 6;

	private static final int T_OVERVIEW = 0;
	private static final int T_RARITY = 1;
	private static final int T_ITEM = 2;
	private static final int T_BAND = 3;
	private static final int T_MILESTONE_GROUP = 4;
	private static final int T_MILESTONE = 5;
	private static final int T_RITUAL = 6;
	private static final int T_BASICS = 7;
	private static final int T_ARCANE = 8;
	private static final int T_ARCANE_GROUP = 9;

	private record Row(int type, int depth, Text label, int chapter, CodexEntries.Rarity rarity, int gearOrdinal) {
		boolean selectable() {
			return type == T_OVERVIEW || type == T_BAND || type == T_MILESTONE || type == T_RITUAL
				|| type == T_BASICS || type == T_ARCANE;
		}

		boolean collapsible() {
			return type == T_RARITY || type == T_ITEM || type == T_MILESTONE_GROUP || type == T_ARCANE_GROUP;
		}
	}

	private final CodexEntries.Track track;

	private final List<Integer> unlocked = new ArrayList<>();
	private final EnumSet<CodexEntries.Rarity> expandedRarities = EnumSet.noneOf(CodexEntries.Rarity.class);
	private final Set<Integer> expandedItems = new HashSet<>();
	private boolean expandedMilestones = false;
	private final Set<Integer> expandedArcaneDirections = new HashSet<>();

	private List<Row> rows = new ArrayList<>();
	private int selected = CodexEntries.OVERVIEW;
	private int leftScroll = 0;
	private int rightScroll = 0;

	private int cachedBodyFor = Integer.MIN_VALUE;
	private List<OrderedText> body = new ArrayList<>();
	/** Ritual apparatus recipes drawn inline in the scroll flow, and the body line they start on. */
	private List<RitualRecipes.Recipe> bodyRecipes = null;
	private int recipeStartLine = -1;

	private int bx, by, bw, bh;
	private int listX, listX2, listTop;
	private int rightX, rightX2, rightTop;
	private int pageTop, pageBottom;
	private int rowsVisible, rightRowsVisible, rightLineHeight;

	public CodexReadingScreen(CodexEntries.Track track) {
		super(Text.literal("Codex"));
		this.track = track;
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player != null) {
			unlocked.addAll(CodexComponents.getUnlocked(CodexItems.findCodex(mc.player)));
		}
	}

	@Override
	protected void init() {
		bw = (int) (this.width * 0.9);
		bh = (int) (this.height * 0.9);
		bx = (this.width - bw) / 2;
		by = (this.height - bh) / 2;

		int pad = Math.max(14, bw / 32);
		int spine = Math.max(4, bw / 80);
		int cx = bx + bw / 2;
		pageTop = by + pad;
		pageBottom = by + bh - pad - 26;

		listX = bx + pad + 10;
		listX2 = cx - spine - 10;
		listTop = pageTop + 30;
		rightX = cx + spine + 14;
		rightX2 = bx + bw - pad - 12;
		rightTop = pageTop + 30;
		rowsVisible = Math.max(1, (pageBottom - listTop) / ROW_HEIGHT);
		rightLineHeight = (int) (this.textRenderer.fontHeight * BODY_SCALE + 3);
		rightRowsVisible = Math.max(1, (pageBottom - rightTop) / rightLineHeight);

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> this.close())
			.dimensions(this.width / 2 - 40, by + bh - 24, 80, 20).build());

		rebuildRows();
	}

	// ---- tree ---------------------------------------------------------------

	private void rebuildRows() {
		List<Row> out = new ArrayList<>();
		out.add(new Row(T_OVERVIEW, 0, Text.literal(CodexContent.overviewTitle(track)), CodexEntries.OVERVIEW, null, -1));

		Set<Integer> have = new HashSet<>(unlocked);

		if (track == CodexEntries.Track.TEMPERING) {
			out.add(new Row(T_BASICS, 0, Text.literal("The Smith's Basics"), CodexEntries.basicsIndex(), null, -1));
			for (CodexEntries.Rarity rarity : CodexEntries.Rarity.values()) {
				List<Integer> ordinals = ordinalsInRarity(rarity, have);
				if (ordinals.isEmpty()) {
					continue;
				}
				boolean rOpen = expandedRarities.contains(rarity);
				out.add(new Row(T_RARITY, 0, Text.literal((rOpen ? "v " : "> ") + rarity.label), -1, rarity, -1));
				if (!rOpen) {
					continue;
				}
				for (int ordinal : ordinals) {
					boolean iOpen = expandedItems.contains(ordinal);
					Text name = CodexEntries.gearName(CodexEntries.ITEMS.get(ordinal));
					out.add(new Row(T_ITEM, 1, Text.literal((iOpen ? "v " : "> ")).copy().append(name), -1, rarity, ordinal));
					if (!iOpen) {
						continue;
					}
					for (int band = 0; band < CodexEntries.BANDS; band++) {
						int chapter = CodexEntries.chapterIndex(ordinal, band);
						if (have.contains(chapter)) {
							out.add(new Row(T_BAND, 2, Text.literal(CodexEntries.BAND_LABELS[band]), chapter, rarity, ordinal));
						}
					}
				}
			}
			// Milestones: a rarity-agnostic group below the rarities, one row per inscribed piece.
			List<Integer> milestones = new ArrayList<>();
			for (int index : CodexEntries.allMilestones()) {
				if (have.contains(index)) {
					milestones.add(index);
				}
			}
			if (!milestones.isEmpty()) {
				out.add(new Row(T_MILESTONE_GROUP, 0,
					Text.literal((expandedMilestones ? "v " : "> ") + "Milestones"), -1, null, -1));
				if (expandedMilestones) {
					for (int index : milestones) {
						out.add(new Row(T_MILESTONE, 1,
							Text.literal(CodexEntries.milestoneKindOf(index).gearName), index, null, -1));
					}
				}
			}
		} else if (track == CodexEntries.Track.RITUAL) {
			// Rituals are a flat list of inscribed gear-type entries.
			for (int index : CodexEntries.allRituals()) {
				if (have.contains(index)) {
					out.add(new Row(T_RITUAL, 0,
						Text.literal(CodexEntries.ritualKindOf(index).gearName), index, null, -1));
				}
			}
		} else if (track == CodexEntries.Track.ARCANE) {
			// Directions Index pinned first (when known), then learned attributes grouped by direction.
			int indexEntry = CodexEntries.arcaneBase();
			if (have.contains(indexEntry)) {
				out.add(new Row(T_ARCANE, 0, CodexEntries.title(indexEntry), indexEntry, null, -1));
			}
			ArcaneAttribute[] attrs = ArcaneAttribute.values();
			int i = 0;
			int dirOrdinal = 0;
			while (i < attrs.length) {
				String dir = attrs[i].direction;
				List<Integer> entries = new ArrayList<>();
				int j = i;
				while (j < attrs.length && attrs[j].direction.equals(dir)) {
					int idx = CodexEntries.arcaneAttributeIndex(j);
					if (have.contains(idx)) {
						entries.add(idx);
					}
					j++;
				}
				if (!entries.isEmpty()) {
					boolean open = expandedArcaneDirections.contains(dirOrdinal);
					out.add(new Row(T_ARCANE_GROUP, 0, Text.literal((open ? "v " : "> ") + dir), -1, null, dirOrdinal));
					if (open) {
						for (int idx : entries) {
							out.add(new Row(T_ARCANE, 1, CodexEntries.title(idx), idx, null, -1));
						}
					}
				}
				i = j;
				dirOrdinal++;
			}
		}

		rows = out;
		leftScroll = Math.min(leftScroll, maxLeftScroll());
	}

	/** Gear ordinals in {@code rarity} that have at least one unlocked chapter. */
	private List<Integer> ordinalsInRarity(CodexEntries.Rarity rarity, Set<Integer> have) {
		List<Integer> out = new ArrayList<>();
		for (int ordinal = 0; ordinal < CodexEntries.ITEMS.size(); ordinal++) {
			if (CodexEntries.ITEMS.get(ordinal).rarity() != rarity) {
				continue;
			}
			for (int band = 0; band < CodexEntries.BANDS; band++) {
				if (have.contains(CodexEntries.chapterIndex(ordinal, band))) {
					out.add(ordinal);
					break;
				}
			}
		}
		return out;
	}

	private int maxLeftScroll() {
		return Math.max(0, rows.size() - rowsVisible);
	}

	private int maxRightScroll() {
		return Math.max(0, body.size() - rightRowsVisible);
	}

	// ---- input --------------------------------------------------------------

	@Override
	public boolean mouseClicked(Click click, boolean doubled) {
		if (click.button() == 0 && click.x() >= listX - 4 && click.x() <= listX2 && click.y() >= listTop) {
			int row = (int) ((click.y() - listTop) / ROW_HEIGHT);
			int idx = leftScroll + row;
			if (row >= 0 && row < rowsVisible && idx < rows.size()) {
				onRowClicked(rows.get(idx));
				return true;
			}
		}
		return super.mouseClicked(click, doubled);
	}

	private void onRowClicked(Row row) {
		if (row.collapsible()) {
			if (row.type() == T_RARITY) {
				toggle(expandedRarities, row.rarity());
			} else if (row.type() == T_ITEM) {
				toggle(expandedItems, row.gearOrdinal());
			} else if (row.type() == T_ARCANE_GROUP) {
				toggle(expandedArcaneDirections, row.gearOrdinal());
			} else {
				expandedMilestones = !expandedMilestones;
			}
			rebuildRows();
		} else if (row.selectable()) {
			selected = row.chapter();
			rightScroll = 0;
		}
	}

	private static <T> void toggle(Set<T> set, T value) {
		if (!set.remove(value)) {
			set.add(value);
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (verticalAmount != 0) {
			int step = -(int) Math.signum(verticalAmount);
			if (mouseX < bx + bw / 2.0) {
				leftScroll = Math.max(0, Math.min(maxLeftScroll(), leftScroll + step));
			} else {
				rightScroll = Math.max(0, Math.min(maxRightScroll(), rightScroll + step));
			}
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	// ---- render -------------------------------------------------------------

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		super.renderBackground(context, mouseX, mouseY, delta);

		int pad = Math.max(14, bw / 32);
		int spine = Math.max(4, bw / 80);
		int cx = bx + bw / 2;
		context.fill(bx, by, bx + bw, by + bh, COVER);
		context.fill(bx + pad, pageTop, cx - spine, pageBottom, PAGE);
		context.fill(cx + spine, pageTop, bx + bw - pad, pageBottom, PAGE);
		// Spine spans exactly the two pages, so it reads as a clean centre divider.
		context.fill(cx - spine, pageTop, cx + spine, pageBottom, SPINE);

		// Left page: the collapsible tree.
		drawScaled(context, Text.literal("Contents"), listX, pageTop + 8, 1.5f, INK);
		for (int i = 0; i < rowsVisible; i++) {
			int idx = leftScroll + i;
			if (idx >= rows.size()) {
				break;
			}
			Row row = rows.get(idx);
			int rowX = listX + row.depth() * 10;
			int rowY = listTop + i * ROW_HEIGHT;
			boolean hover = mouseX >= listX - 2 && mouseX <= listX2 && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
			if (row.selectable() && row.chapter() == selected) {
				context.fill(listX - 2, rowY - 1, listX2, rowY + ROW_HEIGHT - 2, SELECTED_BG);
			} else if (hover) {
				context.fill(listX - 2, rowY - 1, listX2, rowY + ROW_HEIGHT - 2, HOVER_BG);
			}
			int color = row.selectable() ? INK : INK_FADED;
			context.drawText(this.textRenderer, row.label(), rowX, rowY, color, false);
		}
		if (leftScroll > 0) {
			context.drawText(this.textRenderer, Text.literal("^"), listX2 - 8, pageTop + 10, INK_FADED, false);
		}
		if (leftScroll < maxLeftScroll()) {
			context.drawText(this.textRenderer, Text.literal("v"), listX2 - 8, pageBottom - 10, INK_FADED, false);
		}

		// Right page: the selected entry.
		ensureBody();
		String headerLabel = selected == CodexEntries.OVERVIEW ? "Overview"
			: CodexEntries.isBasics(selected) ? "Basics"
			: CodexEntries.isMilestone(selected) ? "Milestone"
			: CodexEntries.isRitual(selected) ? "Ritual"
			: CodexEntries.isArcane(selected) ? "Arcane"
			: CodexEntries.rarityOf(selected).label;
		drawScaled(context, Text.literal(headerLabel), rightX, pageTop + 12, 1.1f, INK_FADED);
		Text pageTitle = selected == CodexEntries.OVERVIEW
			? Text.literal(CodexContent.overviewTitle(track)) : CodexEntries.title(selected);
		drawScaled(context, pageTitle, rightX, pageTop + 28, 1.5f, INK);

		int contentTop = rightTop + 14;
		// Clip the scrolling content (text + recipe grids) to the page, so a recipe block that's
		// only partly scrolled into view is cut off at the edge instead of vanishing whole.
		context.enableScissor(rightX - 2, contentTop, rightX2 + 2, pageBottom);
		int y = contentTop;
		for (int i = rightScroll; i < body.size(); i++) {
			if (y + rightLineHeight > pageBottom) {
				break;
			}
			drawScaledOrdered(context, body.get(i), rightX, y, BODY_SCALE, INK);
			y += rightLineHeight;
		}

		// Apparatus recipe grids: drawn at their reserved line positions within the scroll flow,
		// whenever any part is on-page (the scissor above clips the rest). They stay rendered for
		// as long as this entry is open and reset when another entry is selected (see ensureBody).
		if (bodyRecipes != null) {
			int hy = contentTop + (recipeStartLine - rightScroll) * rightLineHeight;
			if (hy < pageBottom && hy + rightLineHeight > contentTop) {
				context.drawText(this.textRenderer, Text.literal("Apparatus"), rightX, hy, INK_FADED, false);
			}
			for (int r = 0; r < bodyRecipes.size(); r++) {
				int line = recipeStartLine + 1 + r * recipeLines();
				int ry = contentTop + (line - rightScroll) * rightLineHeight;
				if (ry < pageBottom && ry + RECIPE_BLOCK_H > contentTop) {
					drawRecipe(context, bodyRecipes.get(r), rightX, ry);
				}
			}
		}
		context.disableScissor();

		if (rightScroll > 0) {
			context.drawText(this.textRenderer, Text.literal("^"), rightX2 - 8, rightTop, INK_FADED, false);
		}
		if (rightScroll < maxRightScroll()) {
			context.drawText(this.textRenderer, Text.literal("v"), rightX2 - 8, pageBottom - 10, INK_FADED, false);
		}
	}

	/** Body lines reserved per recipe block (its pixel height, rounded up to whole lines). */
	private int recipeLines() {
		return (RECIPE_BLOCK_H + rightLineHeight - 1) / rightLineHeight;
	}

	/** Draws one crafting recipe as a 3x3 item grid, an arrow, and the result. */
	private void drawRecipe(DrawContext context, RitualRecipes.Recipe recipe, int x, int y) {
		context.drawText(this.textRenderer, recipe.result().getName(), x, y, INK, false);
		int gridY = y + 11;
		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 3; c++) {
				int cx = x + c * RECIPE_CELL;
				int cy = gridY + r * RECIPE_CELL;
				context.fill(cx, cy, cx + 16, cy + 16, 0x55000000);
				ItemStack stack = recipe.cells()[r * 3 + c];
				if (!stack.isEmpty()) {
					context.drawItem(stack, cx, cy);
				}
			}
		}
		int arrowX = x + 3 * RECIPE_CELL + 5;
		int midY = gridY + RECIPE_CELL;
		context.drawText(this.textRenderer, Text.literal("→"), arrowX, midY + 4, INK, false);
		int resultX = arrowX + 16;
		context.fill(resultX, midY, resultX + 16, midY + 16, 0x55000000);
		context.drawItem(recipe.result(), resultX, midY);
		context.drawStackOverlay(this.textRenderer, recipe.result(), resultX, midY);
	}

	private void ensureBody() {
		if (cachedBodyFor == selected) {
			return;
		}
		cachedBodyFor = selected;
		rightScroll = 0;
		List<Text> source = selected == CodexEntries.OVERVIEW ? CodexContent.overviewLines(track)
			: CodexEntries.isBasics(selected) ? CodexContent.basicsLines()
			: CodexEntries.isMilestone(selected) ? CodexContent.milestoneLines(selected)
			: CodexEntries.isRitual(selected) ? CodexContent.ritualLines(selected)
			: CodexEntries.isArcane(selected) ? CodexContent.arcaneLines(selected)
			: CodexContent.chapterLines(selected);
		int wrap = (int) ((rightX2 - rightX) / BODY_SCALE);
		List<OrderedText> wrapped = new ArrayList<>();
		for (Text line : source) {
			if (line.getString().isEmpty()) {
				wrapped.add(OrderedText.EMPTY);
			} else {
				wrapped.addAll(this.textRenderer.wrapLines(StringVisitable.plain(line.getString()), wrap));
			}
		}
		// Ritual entries reserve blank lines at the end of the body for their apparatus recipe
		// grid(s), so the grids live in the scroll flow and scroll together with the rite text.
		bodyRecipes = null;
		recipeStartLine = -1;
		if (CodexEntries.isRitual(selected)) {
			List<RitualRecipes.Recipe> rs = RitualRecipes.forKind(CodexEntries.ritualKindOf(selected));
			if (!rs.isEmpty()) {
				bodyRecipes = rs;
				wrapped.add(OrderedText.EMPTY);          // gap before the "Apparatus" block
				recipeStartLine = wrapped.size();        // the "Apparatus" header sits on this line
				int reserve = 1 + rs.size() * recipeLines();
				for (int i = 0; i < reserve; i++) {
					wrapped.add(OrderedText.EMPTY);
				}
			}
		}
		// Trailing blank lines so the last real line can scroll clear of the page's bottom edge.
		for (int i = 0; i < 4; i++) {
			wrapped.add(OrderedText.EMPTY);
		}
		body = wrapped;
	}

	private void drawScaled(DrawContext context, Text text, int px, int py, float scale, int color) {
		context.getMatrices().pushMatrix();
		context.getMatrices().scale(scale, scale);
		context.drawText(this.textRenderer, text, Math.round(px / scale), Math.round(py / scale), color, false);
		context.getMatrices().popMatrix();
	}

	private void drawScaledOrdered(DrawContext context, OrderedText text, int px, int py, float scale, int color) {
		context.getMatrices().pushMatrix();
		context.getMatrices().scale(scale, scale);
		context.drawText(this.textRenderer, text, Math.round(px / scale), Math.round(py / scale), color, false);
		context.getMatrices().popMatrix();
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
