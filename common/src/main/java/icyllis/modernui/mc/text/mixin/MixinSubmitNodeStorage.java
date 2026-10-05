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

import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Defers in-world text rendering to a higher order (after living entity base and layer models such as armor),
 * resolving depth-test and translucent blending sorting issues with shaders (Iris) and vanilla layers.
 */
@Mixin(SubmitNodeStorage.class)
public class MixinSubmitNodeStorage {

    @ModifyArg(
            method = {"submitText", "submitTextBackground"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/SubmitNodeStorage;order(I)Lnet/minecraft/client/renderer/SubmitNodeCollection;"
            ),
            index = 0
    )
    private int modifyTextOrder(int order) {
        return 100;
    }
}
