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

import icyllis.modernui.core.Core;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = Core.class, remap = false)
public class MixinCore {

    /**
     * @author Antigravity
     * @reason Replace GLFW.glfwGetTime() with System.nanoTime() on SDL3 backend
     */
    @Overwrite
    public static long timeNanos() {
        return System.nanoTime();
    }

    /**
     * @author Antigravity
     * @reason Replace GLFW.glfwGetTime() with System.nanoTime() on SDL3 backend
     */
    @Overwrite
    public static long timeMillis() {
        return System.nanoTime() / 1_000_000L;
    }

    /**
     * @author Antigravity
     * @reason Avoid GLFW termination on SDL3 backend
     */
    @Overwrite
    public static void terminate() {
        // no-op on SDL3
    }
}
