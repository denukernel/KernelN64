/**
 * Copyright 2005 Jason LaDere
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Originally based on Project64 code.
 *
 */


package gln64j.rdp.combiners;

import static gln64j.rdp.combiners.Combiners.*;

/** RDP mux operand decoding and encoding tables. */
final class CombinerMux {
    private CombinerMux() {}
    static final int saRGBExpanded[] = {
        COMBINED,			TEXEL0,			TEXEL1,				PRIMITIVE,
        SHADE,				ENVIRONMENT,		ONE,				NOISE,
        ZERO,				ZERO,			ZERO,				ZERO,
        ZERO,				ZERO,			ZERO,				ZERO
    };
    
    static final int sbRGBExpanded[] = {
        COMBINED,			TEXEL0,			TEXEL1,				PRIMITIVE,
        SHADE,				ENVIRONMENT,		CENTER,				K4,
        ZERO,				ZERO,			ZERO,				ZERO,
        ZERO,				ZERO,			ZERO,				ZERO
    };
    
    static final int mRGBExpanded[] = {
        COMBINED,			TEXEL0,			TEXEL1,				PRIMITIVE,
        SHADE,				ENVIRONMENT,		SCALE,				COMBINED_ALPHA,
        TEXEL0_ALPHA,                   TEXEL1_ALPHA,		PRIMITIVE_ALPHA,                SHADE_ALPHA,
        ENV_ALPHA,			LOD_FRACTION,		PRIM_LOD_FRAC,                  K5,
        ZERO,				ZERO,			ZERO,				ZERO,
        ZERO,				ZERO,			ZERO,				ZERO,
        ZERO,				ZERO,			ZERO,				ZERO,
        ZERO,				ZERO,			ZERO,				ZERO
    };
    
    static final int aRGBExpanded[] = {
        COMBINED,			TEXEL0,			TEXEL1,				PRIMITIVE,
        SHADE,				ENVIRONMENT,		ONE,				ZERO
    };
    
    static final int saAExpanded[] = {
        COMBINED,			TEXEL0_ALPHA,		TEXEL1_ALPHA,                   PRIMITIVE_ALPHA,
        SHADE_ALPHA,                    ENV_ALPHA,		ONE,				ZERO
    };
    
    static final int sbAExpanded[] = {
        COMBINED,			TEXEL0_ALPHA,		TEXEL1_ALPHA,                   PRIMITIVE_ALPHA,
        SHADE_ALPHA,                    ENV_ALPHA,		ONE,				ZERO
    };
    
    static final int mAExpanded[] = {
        LOD_FRACTION,                   TEXEL0_ALPHA,           TEXEL1_ALPHA,                   PRIMITIVE_ALPHA,
        SHADE_ALPHA,                    ENV_ALPHA,              PRIM_LOD_FRAC,                  ZERO,
    };
    
    static final int aAExpanded[] = {
        COMBINED,			TEXEL0_ALPHA,		TEXEL1_ALPHA,                   PRIMITIVE_ALPHA,
        SHADE_ALPHA,                    ENV_ALPHA,		ONE,				ZERO
    };
    
    static final int CCEncodeA[] = {
        0, 1, 2, 3, 4, 5, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 7, 15, 15, 6, 15
    };
    
    static final int CCEncodeB[] = {
        0, 1, 2, 3, 4, 5, 6, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 7, 15, 15, 15
    };
    
    static final int CCEncodeC[] = {
        0, 1, 2, 3, 4, 5, 31, 6, 7, 8, 9, 10, 11, 12, 13, 14, 31, 31, 15, 31, 31
    };
    
    static final int CCEncodeD[] = {
        0, 1, 2, 3, 4, 5, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 15, 6, 15
    };
    
    static final long ACEncodeA[] = {
        7, 7, 7, 7, 7, 7, 7, 7, 0, 1, 2, 3, 4, 5, 7, 7, 7, 7, 7, 6, 7
    };
    
    static final long ACEncodeB[] = {
        7, 7, 7, 7, 7, 7, 7, 7, 0, 1, 2, 3, 4, 5, 7, 7, 7, 7, 7, 6, 7
    };
    
    static final long ACEncodeC[] = {
        7, 7, 7, 7, 7, 7, 7, 7, 0, 1, 2, 3, 4, 5, 7, 6, 7, 7, 7, 7, 7
    };
    
    static final long ACEncodeD[] = {
        7, 7, 7, 7, 7, 7, 7, 7, 0, 1, 2, 3, 4, 5, 7, 7, 7, 7, 7, 6, 7
    };
    
}
