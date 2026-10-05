/*
 * Modern UI.
 * Copyright (C) 2025 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.mc.text;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import icyllis.arc3d.core.Rect2f;
import icyllis.modernui.mc.GradientRectangleRenderState;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.EmptyArea;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EffectGlyph;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.gui.GlyphRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4fc;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;

/**
 * When this object is created, all glyphs are added to the font atlas.
 */
// Only used for vanilla GUI rendering
public class ModernPreparedText implements Font.PreparedText {

    public static final ModernPreparedText EMPTY = new ModernPreparedText(
            1, 0, false, 0, 0,
            0, 0, 0, 0, null,
            new ArrayList<>(), false, 0,
            null, null, null, new ArrayList<>()
    );

    private final float density;
    private final float shadowOffset;
    private final boolean dropShadow;
    private final int color;
    private final int bgColor;
    public final float x;
    private final float top;
    private final float xAdj;
    private final float yAdj;
    private final ScreenRectangle bounds;
    private final ArrayList<TextRun> runs;
    private final boolean hasEffect;
    private final float totalAdvance;
    private final BakedGlyph[] glyphs;
    private final float[] positions;
    private final int[] flags;
    private final ArrayList<TextRenderable> customRenderables;

    ModernPreparedText(float density, float shadowOffset, boolean dropShadow, int color,
                       int bgColor, float x, float top, float xAdj, float yAdj, ScreenRectangle bounds,
                       ArrayList<TextRun> runs, boolean hasEffect, float totalAdvance,
                       BakedGlyph[] glyphs, float[] positions, int[] flags,
                       ArrayList<TextRenderable> customRenderables) {
        this.density = density;
        this.shadowOffset = shadowOffset;
        this.dropShadow = dropShadow;
        this.color = color;
        this.bgColor = bgColor;
        this.x = x;
        this.top = top;
        this.xAdj = xAdj;
        this.yAdj = yAdj;
        this.bounds = bounds;
        this.runs = runs;
        this.hasEffect = hasEffect;
        this.totalAdvance = totalAdvance;
        this.glyphs = glyphs;
        this.positions = positions;
        this.flags = flags;
        this.customRenderables = customRenderables;
    }

    ModernPreparedText(float x, float top, int color, boolean dropShadow,
                       int preferredMode, int bgColor, float xAdj, float yAdj,
                       float density, BakedGlyph[] glyphs, TextLayout layout) {

        final float invDensity = 1.0f / density;
        float shadowOffset = 0;
        if (dropShadow) {
            shadowOffset = ModernTextRenderer.sShadowOffset;
            if (preferredMode == TextRenderType.MODE_NORMAL) {
                // align to screen pixel center in 2D
                shadowOffset = Math.round(shadowOffset * density) * invDensity;
            }
        }

        final var positions = layout.getPositions();
        final var flags = layout.getGlyphFlags();

        final float baseline = top + TextLayout.sBaselineOffset;

        AbstractTexture prevTexture = null;
        int prevMode = -1;
        RenderPipeline pipeline = null;

        AbstractTexture fontTexture = null;

        Rect2f bounds = Rect2f.makeInfiniteInverted();

        assert preferredMode == TextRenderType.MODE_NORMAL ||
                preferredMode == TextRenderType.MODE_SDF_FILL;
        if ((bgColor & 0xFF000000) != 0) {
            bounds.joinNoCheck(x - 1, top - 1,
                    x + layout.getTotalAdvance() + 1, top + 9);
        }

        ArrayList<TextRun> textRuns = new ArrayList<>();
        ArrayList<TextRenderable> customRenderables = new ArrayList<>();
        boolean glyphArrayIsCopied = false;

        for (int i = 0, e = glyphs.length; i < e; i++) {
            var vglyph = glyphs[i];
            if (vglyph == null) {
                continue;
            }
            final int bits = flags[i];
            if (!(vglyph instanceof ModernBakedGlyph glyph)) {
                // atlas sprite and player skin don't use style
                int glyphColor = color;
                if ((bits & CharacterStyle.IMPLICIT_COLOR_MASK) == 0) {
                    glyphColor = (color & 0xff000000) | (bits & 0xffffff);
                }
                int shadowColor = 0;
                if (dropShadow && (bits & CharacterStyle.NO_SHADOW_MASK) == 0) {
                    shadowColor = ARGB.scaleRGB(glyphColor, 0.25f);
                }
                var renderable = vglyph.createGlyph(
                        x + positions[i << 1] + xAdj,
                        top + positions[i << 1 | 1] + yAdj,
                        glyphColor, shadowColor,
                        Style.EMPTY,
                        0, 1
                );
                if (renderable != null) {
                    bounds.joinNoCheck(
                            renderable.left() - xAdj, renderable.top() - yAdj, renderable.right() - xAdj, renderable.bottom() - yAdj
                    );
                    customRenderables.add(renderable);
                }

                continue;
            }
            float rx;
            float ry;
            final float w;
            final float h;
            final int mode;
            final AbstractTexture texture;
            boolean fakeItalic = false;
            int ascent = 0;
            boolean isBitmapFont = false;
            boolean isColorEmoji = false;
            if ((bits & CharacterStyle.OBFUSCATED_MASK) != 0) {
                var chars = (GlyphManager.FastCharSet) glyph;
                int fastIndex = TextLayout.RANDOM.nextInt(chars.glyphs.size());
                glyph = chars.glyphs.get(fastIndex);
                // Determine the random glyph to be drawn in next frame
                if (!glyphArrayIsCopied) {
                    glyphArrayIsCopied = true;
                    glyphs = glyphs.clone();
                }
                glyphs[i] = glyph;
            }
            final Identifier textureIdentifier;
            if ((bits & CharacterStyle.ANY_BITMAP_REPLACEMENT) != 0) {
                final float scaleFactor;
                if (layout.getFont(i) instanceof BitmapFont bitmapFont) {
                    texture = GlyphManager.getInstance().getCurrentTexture(bitmapFont);
                    textureIdentifier = bitmapFont.getCurrentTextureName();
                    ascent = -glyph.y / TextLayoutEngine.BITMAP_SCALE;
                    scaleFactor = 1f / TextLayoutEngine.BITMAP_SCALE;
                    isBitmapFont = true;
                } else {
                    texture = GlyphManager.getInstance().getEmojiTexture();
                    textureIdentifier = GlyphManager.EMOJI_SHEET;
                    ascent = TextLayout.STANDARD_BASELINE_OFFSET;
                    scaleFactor = TextLayoutProcessor.sBaseFontSize / GlyphManager.EMOJI_BASE;
                    isColorEmoji = true;
                }
                fakeItalic = (bits & CharacterStyle.ITALIC_MASK) != 0;
                rx = x + positions[i << 1] + glyph.x * scaleFactor;
                ry = baseline + positions[i << 1 | 1] + glyph.y * scaleFactor;

                w = glyph.width * scaleFactor;
                h = glyph.height * scaleFactor;
                mode = TextRenderType.MODE_NORMAL; // for color emoji
            } else {
                mode = preferredMode;
                rx = x + positions[i << 1] + glyph.x * invDensity;
                ry = baseline + positions[i << 1 | 1] + glyph.y * invDensity;

                w = glyph.width * invDensity;
                h = glyph.height * invDensity;
                if (fontTexture == null) {
                    fontTexture = GlyphManager.getInstance().getFontTexture();
                }
                texture = fontTexture;
                textureIdentifier = GlyphManager.FONT_SHEET;
            }
            if (pipeline == null || prevTexture != texture || prevMode != mode) {
                // no need to check isBitmapFont
                prevTexture = texture;
                prevMode = mode;
                pipeline = TextRenderType.getPipelineForGui(mode, isBitmapFont);
                if (!textRuns.isEmpty()) {
                    textRuns.getLast().glyphEnd = i;
                }
                textRuns.add(new TextRun(pipeline, texture.getTextureView(),
                        // setup bilinear sampler for SDF text
                        mode == TextRenderType.MODE_SDF_FILL
                                ? RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR)
                                : texture.getSampler(),
                        textureIdentifier,
                        i, isColorEmoji, isBitmapFont,
                        preferredMode == TextRenderType.MODE_NORMAL));
            }
            float upSkew = 0;
            float downSkew = 0;
            if (fakeItalic) {
                upSkew = 0.25f * ascent;
                downSkew = 0.25f * (ascent - h);
            }
            bounds.joinNoCheck(
                    rx + downSkew, ry, rx + w + upSkew, ry + h
            );
        }
        if (!textRuns.isEmpty()) {
            textRuns.getLast().glyphEnd = glyphs.length;
        }

        if (layout.hasEffect()) {
            bounds.joinNoCheck(x, baseline + TextRenderEffect.STRIKETHROUGH_OFFSET,
                    x + layout.getTotalAdvance(), baseline + (TextRenderEffect.UNDERLINE_OFFSET + TextRenderEffect.UNDERLINE_THICKNESS));
        }

        ScreenRectangle finalBounds = null;
        if (!bounds.isEmpty()) {
            int L = (int) Math.floor(bounds.left()), T = (int) Math.floor(bounds.top()),
                    R = (int) Math.ceil(bounds.right() + (dropShadow ? shadowOffset : 0)),
                    B = (int) Math.ceil(bounds.bottom() + (dropShadow ? shadowOffset : 0));
            finalBounds = new ScreenRectangle(L, T, R - L, B - T);
        }


        this.density = density;
        this.shadowOffset = shadowOffset;
        this.dropShadow = dropShadow;
        this.color = color;
        this.bgColor = bgColor;
        this.x = x;
        this.top = top;
        this.xAdj = xAdj;
        this.yAdj = yAdj;
        this.bounds = finalBounds;
        this.runs = textRuns;
        this.hasEffect = layout.hasEffect();
        this.totalAdvance = layout.getTotalAdvance();
        this.glyphs = glyphs;
        this.positions = positions;
        this.flags = flags;
        this.customRenderables = customRenderables;
    }

    @Override
    public void visit(@Nonnull Font.GlyphVisitor glyphVisitor) {
        if (bounds != null) {
            glyphVisitor.acceptEmptyArea(new EmptyArea(bounds.left(), bounds.top(), bounds.width(), 7, bounds.height(), Style.EMPTY));
        }
        if ((bgColor & 0xFF000000) != 0) {
            try {
                EffectGlyph effectGlyph = GlyphManager.getInstance().getEffectGlyph();
                if (effectGlyph != null) {
                    glyphVisitor.acceptEffect(effectGlyph.createEffect(
                            x + xAdj - 1, top + yAdj - 1, x + xAdj + totalAdvance + 1, top + yAdj + 9,
                            -0.01f, bgColor, 0, 0
                    ));
                }
            } catch (Exception ignored) {
            }
        }
        for (int i = 0; i < customRenderables.size(); i++) {
            glyphVisitor.acceptRenderable(customRenderables.get(i));
        }
        for (int i = 0; i < runs.size(); i++) {
            glyphVisitor.acceptGlyph(new ModernRunRenderable(this, runs.get(i)));
        }
        if (hasEffect) {
            try {
                EffectGlyph effectGlyph = GlyphManager.getInstance().getEffectGlyph();
                if (effectGlyph != null) {
                    final float baseline = top + yAdj + TextLayout.sBaselineOffset;
                    final float startX = x + xAdj;
                    for (int i = 0, e = flags.length; i < e; i++) {
                        final int bits = flags[i];
                        if ((bits & CharacterStyle.EFFECT_MASK) == 0) {
                            continue;
                        }
                        int effectColor;
                        if ((bits & CharacterStyle.IMPLICIT_COLOR_MASK) != 0) {
                            effectColor = color;
                        } else {
                            effectColor = (color & 0xff000000) | (bits & 0xffffff);
                        }
                        int effectShadowColor = 0;
                        if (dropShadow && ModernTextRenderer.sAllowShadow) {
                            effectShadowColor = ARGB.scaleRGB(effectColor, 0.25f);
                        }
                        final float rx1 = startX + positions[i << 1];
                        final float rx2 = startX + ((i + 1 == e) ? totalAdvance : positions[(i + 1) << 1]);
                        if ((bits & CharacterStyle.STRIKETHROUGH_MASK) != 0) {
                            glyphVisitor.acceptEffect(effectGlyph.createEffect(
                                    rx1, baseline + TextRenderEffect.STRIKETHROUGH_OFFSET,
                                    rx2, baseline + TextRenderEffect.STRIKETHROUGH_OFFSET + TextRenderEffect.STRIKETHROUGH_THICKNESS,
                                    0.01f, effectColor, effectShadowColor, dropShadow ? shadowOffset : 0
                            ));
                        }
                        if ((bits & CharacterStyle.UNDERLINE_MASK) != 0) {
                            glyphVisitor.acceptEffect(effectGlyph.createEffect(
                                    rx1, baseline + TextRenderEffect.UNDERLINE_OFFSET,
                                    rx2, baseline + TextRenderEffect.UNDERLINE_OFFSET + TextRenderEffect.UNDERLINE_THICKNESS,
                                    0.01f, effectColor, effectShadowColor, dropShadow ? shadowOffset : 0
                            ));
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Nullable
    @Override
    public ScreenRectangle bounds() {
        return bounds;
    }

    @SuppressWarnings("ForLoopReplaceableByForEach")
    public void submitRuns(GuiRenderState renderState, Matrix3x2fc pose,
                           @Nullable ScreenRectangle scissor) {
        float x = this.x;
        float top = this.top;
        if (xAdj != 0 || yAdj != 0) {
            var newPose = new Matrix3x2f(pose);
            newPose.m20 = 0;
            newPose.m21 = 0;
            x += xAdj;
            top += yAdj;
            pose = newPose;
        }
        if ((bgColor & 0xFF000000) != 0) {
            // this is only used by CartographyTableScreen, emit as normal fills
            renderState.addGlyphToCurrentLayer(
                    new GradientRectangleRenderState(
                            RenderPipelines.GUI,
                            TextureSetup.noTexture(),
                            pose,
                            x - 1, top - 1,
                            x + totalAdvance + 1, top + 9,
                            bgColor, bgColor, bgColor, bgColor,
                            scissor, null
                    )
            );
        }
        for (int i = 0; i < customRenderables.size(); i++) {
            renderState.addGlyphToCurrentLayer(
                    new GlyphRenderState(pose, customRenderables.get(i), scissor)
            );
        }
        // For-index is 2x faster than enhanced-for
        for (int i = 0; i < runs.size(); i++) {
            var run = runs.get(i);
            renderState.addGlyphToCurrentLayer(
                    new TextRunRenderState(pose, run.pipeline,
                            TextureSetup.singleTextureWithLightmap(run.textureView, run.sampler),
                            scissor,
                            x, top, color, dropShadow,
                            glyphs, positions, flags,
                            run.glyphStart, run.glyphEnd,
                            run.isColorEmoji, run.isDirectMask,
                            density, shadowOffset)
            );
        }
        if (hasEffect) {
            renderState.addGlyphToCurrentLayer(
                    new TextEffectRenderState(pose,
                            scissor,
                            x, top, color, dropShadow,
                            positions, flags,
                            totalAdvance, shadowOffset)
            );
        }
    }

    /**
     * GPU-baked text sub run.
     */
    static class TextRun {

        public final RenderPipeline pipeline;
        public final GpuTextureView textureView;
        public final GpuSampler sampler;
        public final Identifier textureIdentifier;
        public final int glyphStart;
        public int glyphEnd;
        public final boolean isColorEmoji;
        public final boolean isBitmapFont;
        public final boolean isDirectMask;

        public TextRun(RenderPipeline pipeline, GpuTextureView textureView, GpuSampler sampler,
                       Identifier textureIdentifier,
                       int glyphStart, boolean isColorEmoji, boolean isBitmapFont, boolean isDirectMask) {
            this.pipeline = pipeline;
            this.textureView = textureView;
            this.sampler = sampler;
            this.textureIdentifier = textureIdentifier;
            this.glyphStart = glyphStart;
            this.isColorEmoji = isColorEmoji;
            this.isBitmapFont = isBitmapFont;
            this.isDirectMask = isDirectMask;
        }
    }

    public static class ModernRunRenderable implements TextRenderable.Styled {

        private final ModernPreparedText text;
        private final TextRun run;

        public ModernRunRenderable(ModernPreparedText text, TextRun run) {
            this.text = text;
            this.run = run;
        }

        @Nonnull
        @Override
        public RenderType renderType(Font.DisplayMode displayMode) {
            return TextRenderType.getOrCreate(run.textureIdentifier, displayMode, run.isBitmapFont);
        }

        @Nonnull
        @Override
        public GpuTextureView textureView() {
            return run.textureView;
        }

        @Nonnull
        @Override
        public RenderPipeline guiPipeline() {
            return run.pipeline;
        }

        @Override
        public float left() {
            return text.bounds != null ? text.bounds.left() : text.x + text.xAdj;
        }

        @Override
        public float top() {
            return text.bounds != null ? text.bounds.top() : text.top + text.yAdj;
        }

        @Override
        public float right() {
            return text.bounds != null ? text.bounds.right() : text.x + text.xAdj + text.totalAdvance;
        }

        @Override
        public float bottom() {
            return text.bounds != null ? text.bounds.bottom() : text.top + text.yAdj + 9;
        }

        @Nonnull
        @Override
        public Style style() {
            return Style.EMPTY;
        }

        @Override
        public void render(Matrix4fc pose, VertexConsumer consumer, int lightCoords, boolean shadow) {
            float invDensity = 1.0f / text.density;
            int a = text.color >>> 24;
            int r = text.color >> 16 & 0xff;
            int g = text.color >> 8 & 0xff;
            int b = text.color & 0xff;
            final float baseline = text.top + text.yAdj + TextLayout.sBaselineOffset;
            if (shadow) {
                buildPass(pose, consumer, lightCoords, invDensity, r >> 2, g >> 2, b >> 2, a, baseline, true, 0.0f);
            } else {
                if (text.dropShadow && ModernTextRenderer.sAllowShadow && !run.isColorEmoji) {
                    buildPass(pose, consumer, lightCoords, invDensity, r >> 2, g >> 2, b >> 2, a, baseline, true, 0.0f);
                }
                float z = (text.dropShadow && ModernTextRenderer.sAllowShadow && !run.isColorEmoji) ? 0.03f : 0.0f;
                buildPass(pose, consumer, lightCoords, invDensity, r, g, b, a, baseline, false, z);
            }
        }

        private void buildPass(Matrix4fc pose, VertexConsumer builder, int lightCoords,
                               float invDensity, final int startR, final int startG, final int startB, final int a,
                               float baseline, boolean isShadow, float z) {
            int r;
            int g;
            int b;
            var glyphs = text.glyphs;
            var positions = text.positions;
            var flags = text.flags;
            float x = text.x + text.xAdj;
            if (isShadow) {
                x += text.shadowOffset;
                baseline += text.shadowOffset;
            }
            for (int i = run.glyphStart; i < run.glyphEnd; i++) {
                var vglyph = glyphs[i];
                if (vglyph == null) {
                    continue;
                }
                if (!(vglyph instanceof ModernBakedGlyph glyph)) {
                    continue;
                }
                final int bits = flags[i];
                float rx;
                float ry;
                final float w;
                final float h;
                boolean fakeItalic = false;
                int ascent = 0;
                if ((bits & CharacterStyle.NO_SHADOW_MASK) != 0 && isShadow) {
                    continue;
                }
                if ((bits & CharacterStyle.ANY_BITMAP_REPLACEMENT) != 0) {
                    final float scaleFactor;
                    if (!run.isColorEmoji) {
                        ascent = -glyph.y / TextLayoutEngine.BITMAP_SCALE;
                        scaleFactor = 1f / TextLayoutEngine.BITMAP_SCALE;
                    } else {
                        assert !isShadow;
                        ascent = TextLayout.STANDARD_BASELINE_OFFSET;
                        scaleFactor = TextLayoutProcessor.sBaseFontSize / GlyphManager.EMOJI_BASE;
                    }
                    fakeItalic = (bits & CharacterStyle.ITALIC_MASK) != 0;
                    rx = x + positions[i << 1] + glyph.x * scaleFactor;
                    ry = baseline + positions[i << 1 | 1] + glyph.y * scaleFactor;
                    if (isShadow) {
                        rx += 1.0f - text.shadowOffset;
                        ry += 1.0f - text.shadowOffset;
                    }
                    w = glyph.width * scaleFactor;
                    h = glyph.height * scaleFactor;
                } else {
                    rx = x + positions[i << 1] + glyph.x * invDensity;
                    ry = baseline + positions[i << 1 | 1] + glyph.y * invDensity;
                    w = glyph.width * invDensity;
                    h = glyph.height * invDensity;
                }
                if (run.isDirectMask) {
                    rx = Math.round(rx * text.density) * invDensity;
                    ry = Math.round(ry * text.density) * invDensity;
                }
                if (run.isColorEmoji) {
                    r = 0xff;
                    g = 0xff;
                    b = 0xff;
                } else if ((bits & CharacterStyle.IMPLICIT_COLOR_MASK) != 0) {
                    r = startR;
                    g = startG;
                    b = startB;
                } else {
                    r = bits >> 16 & 0xff;
                    g = bits >> 8 & 0xff;
                    b = bits & 0xff;
                    if (isShadow) {
                        r >>= 2;
                        g >>= 2;
                        b >>= 2;
                    }
                }
                float upSkew = 0;
                float downSkew = 0;
                if (fakeItalic) {
                    upSkew = 0.25f * ascent;
                    downSkew = 0.25f * (ascent - h);
                }
                builder.addVertex(pose, rx + upSkew, ry, z)
                        .setColor(r, g, b, a)
                        .setUv(glyph.u1, glyph.v1)
                        .setLight(lightCoords);
                builder.addVertex(pose, rx + downSkew, ry + h, z)
                        .setColor(r, g, b, a)
                        .setUv(glyph.u1, glyph.v2)
                        .setLight(lightCoords);
                builder.addVertex(pose, rx + w + downSkew, ry + h, z)
                        .setColor(r, g, b, a)
                        .setUv(glyph.u2, glyph.v2)
                        .setLight(lightCoords);
                builder.addVertex(pose, rx + w + upSkew, ry, z)
                        .setColor(r, g, b, a)
                        .setUv(glyph.u2, glyph.v1)
                        .setLight(lightCoords);
            }
        }
    }
}
