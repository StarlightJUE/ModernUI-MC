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

package icyllis.modernui.mc.mixin;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GLFW.class, remap = false)
public class MixinGLFW {

    @Inject(method = "<clinit>", at = @At("HEAD"), cancellable = true)
    private static void onClinit(CallbackInfo ci) {
        ci.cancel();
    }

    /**
     * @author Antigravity
     * @reason Emulate glfwGetTime() on SDL3 backend
     */
    @Overwrite
    public static double glfwGetTime() {
        return System.nanoTime() / 1_000_000_000.0;
    }

    /**
     * @author Antigravity
     * @reason Stub glfwCreateStandardCursor on SDL3 backend
     */
    @Overwrite
    public static long glfwCreateStandardCursor(int shape) {
        return 0L;
    }

    /**
     * @author Antigravity
     * @reason Stub glfwInit on SDL3 backend
     */
    @Overwrite
    public static boolean glfwInit() {
        return true;
    }
}
