#version 330
#extension GL_ARB_separate_shader_objects : require

// This file is part of Modern UI.
// Copyright (C) 2024-2026 BloCamLimb.
// Licensed under LGPL-3.0-or-later.

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
#include <minecraft:fog.glsl>
#endif

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:oit.glsl>

uniform sampler2D Sampler0;

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
layout(location = 0) in float sphericalVertexDistance;
layout(location = 1) in float cylindricalVertexDistance;
#endif

layout(location = 2) in vec4 vertexColor;
layout(location = 3) in vec2 texCoord0;

#ifndef OIT_ALPHA_ONLY
layout(location = 0) out vec4 fragColor;
#endif

vec4 calculateFinalColor(vec4 color) {
    #ifdef OIT_ACCUMULATE
    color = sampleColorForAccumulation(color);
    #endif

    #if !defined(IS_SEE_THROUGH) && !defined(IS_GUI)

    #ifdef OIT_ACCUMULATE
    vec4 fogColor = vec4(FogColor.rgb * color.a, FogColor.a);
    #else
    vec4 fogColor = FogColor;
    #endif

    color = apply_fog(color, sphericalVertexDistance, cylindricalVertexDistance, FogEnvironmentalStart, FogEnvironmentalEnd, FogRenderDistanceStart, FogRenderDistanceEnd, fogColor);
    #endif

    return color;
}

void main() {
    vec2 texSize = vec2(textureSize(Sampler0, 0));
    float dsum = 0.0;
    float wsum = 0.0;
    const int nstep = 3;
    const float w[3] = float[3](1.0, 2.0, 1.0);
    for (int i = 0; i < nstep; ++i) {
        for (int j = 0; j < nstep; ++j) {
            vec2 delta = vec2(float(i - 1), float(j - 1)) / texSize;
            float wij = w[i] * w[j];
            vec4 samp = textureLod(Sampler0, texCoord0 - delta, 0.0);
            float dist = samp.w - 127.0 / 255.0;
            dsum += wij * dist;
            wsum += wij;
        }
    }
    float dist = dsum / wsum;
    dist = abs(dist + 0.15) - 0.2;
    vec4 color = vertexColor * ColorModulator;
    color.a *= 1.0 - clamp(dist / fwidth(dist) + 0.5, 0.0, 1.0);
    if (color.a < 0.01) {
        discard;
    }

    #ifdef OIT_ALPHA_ONLY
    executeAlphaOnlyPhase(gl_FragCoord.z, color.a);
    #else
    fragColor = calculateFinalColor(color);
    #endif
}
