package com.shumbles.gearoverhaul.client;

import com.shumbles.gearoverhaul.codex.CodexComponents;
import com.shumbles.gearoverhaul.codex.CodexEntries;
import com.shumbles.gearoverhaul.codex.CodexItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
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

	private static final int T_OVERVIEW = 0;
	private static final int T_RARITY = 1;
	private static final int T_ITEM = 2;
	private static final int T_BAND = 3;
	private static final int T_MILESTONE_GROUP = 4;
	private static final int T_MILESTONE = 5;
	private static final int T_RITUAL = 6;

	private record Row(int type, int depth, Text label, int chapter, CodexEntries.Rarity rarity, int gearOrdinal) {
		boolean selectable() {
			return type == T_OVERVIEW || type == T_BAND || type == T_MILESTONE || type == T_RITUAL;
		}

		boolean collapsible() {
			return type == T_RARITY || type == T_ITEM || type == T_MILESTONE_GROUP;
		}
	}

	private final CodexEntries.Track track;

	private final List<Integer> unlocked = new ArrayList<>();
	private final EnumSet<CodexEntries.Rarity> expandedRarities = EnumSet.noneOf(CodexEntries.Rarity.class);
	private final Set<Integer> expandedItems = new HashSet<>();
	private boolean expandedMilestones = false;

	private List<Row> rows = new ArrayList<>();
	private int selected = CodexEntries.OVERVIEW;
	private int leftScroll = 0;
	private int rightScroll = 0;

	private int cachedBodyFor = Integer.MIN_VALUE;
	private List<OrderedText> body = new ArrayList<>();

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
		out.add(new Row(T_OVERVIEW, 0, CodexEntries.title(CodexEntries.OVERVIEW), CodexEntries.OVERVIEW, null, -1));

		Set<Integer> have = new HashSet<>(unlocked);

		if (track == CodexEntries.Track.TEMPERING) {
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
		}
		// ARCANE: nothing inscribable yet — only the overview shows.

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
			boolean leaf = row.type() == T_BAND || row.type() == T_OVERVIEW || row.type() == T_MILESTONE;
			int color = leaf ? INK : INK_FADED;
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
			: CodexEntries.isMilestone(selected) ? "Milestone"
			: CodexEntries.isRitual(selected) ? "Ritual"
			: CodexEntries.rarityOf(selected).label;
		drawScaled(context, Text.literal(headerLabel), rightX, pageTop + 12, 1.1f, INK_FADED);
		drawScaled(context, CodexEntries.title(selected), rightX, pageTop + 28, 1.5f, INK);

		int y = rightTop + 14;
		for (int i = 0; i < rightRowsVisible; i++) {
			int idx = rightScroll + i;
			if (idx >= body.size()) {
				break;
			}
			drawScaledOrdered(context, body.get(idx), rightX, y, BODY_SCALE, INK);
			y += rightLineHeight;
		}
		if (rightScroll > 0) {
			context.drawText(this.textRenderer, Text.literal("^"), rightX2 - 8, rightTop, INK_FADED, false);
		}
		if (rightScroll < maxRightScroll()) {
			context.drawText(this.textRenderer, Text.literal("v"), rightX2 - 8, pageBottom - 10, INK_FADED, false);
		}
	}

	private void ensureBody() {
		if (cachedBodyFor == selected) {
			return;
		}
		cachedBodyFor = selected;
		rightScroll = 0;
		List<Text> source = selected == CodexEntries.OVERVIEW ? CodexContent.overviewLines()
			: CodexEntries.isMilestone(selected) ? CodexContent.milestoneLines(selected)
			: CodexEntries.isRitual(selected) ? CodexContent.ritualLines(selected)
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
