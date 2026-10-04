/*
 * Modern UI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
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

package icyllis.modernui.mc;

import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import icyllis.arc3d.core.RawPtr;
import icyllis.arc3d.vulkan.VulkanBackendContext;
import icyllis.arc3d.vulkan.VulkanImage;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class VulkanModIntegration {

    private VulkanModIntegration() {
    }

    public static VulkanBackendContext wrapContext() {
        throw new UnsupportedOperationException("VulkanMod is not available for Minecraft 26.3");
    }

    public static void replaceMainImageViewWithSwizzle(GpuTextureView textureView, short swizzle) {
        throw new UnsupportedOperationException("VulkanMod is not available for Minecraft 26.3");
    }

    public static GpuTexture gpuTextureFromVulkanImage(GpuTexture currentTexture, GpuTextureView currentTextureView,
                                                      @RawPtr VulkanImage arc3dVulkanImage) {
        throw new UnsupportedOperationException("VulkanMod is not available for Minecraft 26.3");
    }

    public static GpuTexture wrapTextureImageFromArc3D(@RawPtr VulkanImage arc3dVulkanImage) {
        throw new UnsupportedOperationException("VulkanMod is not available for Minecraft 26.3");
    }

    public static void syncImageLayoutFromArc3D(GpuTexture vulkanTexture, @RawPtr VulkanImage arc3dVulkanImage) {
    }

    public static void syncImageLayoutFromVulkan(GpuTexture vulkanTexture, @RawPtr VulkanImage arc3dVulkanImage) {
    }

    public static boolean sameImage(GpuTexture vulkanTexture, @RawPtr VulkanImage arc3dVulkanImage) {
        return false;
    }

    public static void addFrameOp(Runnable runnable) {
        runnable.run();
    }
}
