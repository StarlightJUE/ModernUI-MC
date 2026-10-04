/*
 * Modern UI.
 * Copyright (C) 2019-2026 BloCamLimb. All rights reserved.
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

package icyllis.modernui.mc.text.mixin;

import icyllis.modernui.mc.text.*;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nonnull;

@Mixin(Font.class)
public abstract class MixinFontRenderer {

    @Redirect(method = "<init>", at = @At(value = "NEW",
            target = "(Lnet/minecraft/client/StringSplitter$WidthProvider;)Lnet/minecraft/client/StringSplitter;"))
    private StringSplitter onNewSplitter(StringSplitter.WidthProvider widthProvider) {
        return new ModernStringSplitter(TextLayoutEngine.getInstance(), widthProvider);
    }

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    public Font.PreparedText prepareText(String text, float x, float y, int color, boolean dropShadow, int backgroundColor) {
        if (text == null || text.isEmpty()) {
            return ModernPreparedText.EMPTY;
        }
        TextLayout layout = TextLayoutEngine.getInstance().lookupVanillaLayout(text);
        return layout.prepareText(x, y, color, dropShadow, TextRenderType.MODE_NORMAL, backgroundColor);
    }

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    public Font.PreparedText prepareText(FormattedCharSequence text, float x, float y, int color, boolean dropShadow,
                                         boolean includeEmpty, int backgroundColor) {
        if (text == null || text == FormattedCharSequence.EMPTY) {
            return ModernPreparedText.EMPTY;
        }
        TextLayout layout = TextLayoutEngine.getInstance().lookupFormattedLayout(text);
        return layout.prepareText(x, y, color, dropShadow, TextRenderType.MODE_NORMAL, backgroundColor);
    }

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    public Font.PreparedText prepare8xTextOutline(FormattedCharSequence text, float x, float y, int outlineColor) {
        if (text == null || text == FormattedCharSequence.EMPTY) {
            return ModernPreparedText.EMPTY;
        }
        TextLayout layout = TextLayoutEngine.getInstance().lookupFormattedLayout(text);
        return layout.prepareText(x, y, outlineColor, false, TextRenderType.MODE_NORMAL, 0);
    }

    /**
     * Bidi and shaping always works no matter what language is in.
     * So we should analyze the original string without reordering.
     * Do not reorder, we have our layout engine.
     *
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Overwrite
    public String bidirectionalShaping(String text) {
        return text;
    }
}
