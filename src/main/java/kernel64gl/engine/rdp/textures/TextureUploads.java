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

package kernel64gl.engine.rdp.textures;

import javax.media.opengl.GL;
import static kernel64gl.engine.rdp.textures.TextureCache.*;

/** Texture lookup, sizing, GPU upload, activation, and cache eviction. */
final class TextureUploads {
    private TextureUploads() {}

	static void update(TextureCache state, boolean IA16, int t, float scaleX, float scaleY, boolean linear) {
		if (t < 0)
			t = 0;
		if (t > 1)
			t = 1; // textureTile only has 2 entries (0 and 1)

		int maxTexels;

		int tileWidth;
		int maskWidth;
		int loadWidth;
		int lineWidth;
		int clampWidth;
		int height;
		int tileHeight;
		int maskHeight;
		int loadHeight;
		int lineHeight;
		int clampHeight;
		int width;


		if (state.textureMode == TEXTUREMODE_BGIMAGE) {
			state.updateBackground(IA16, linear);
			return;
		}


		// Valid ranges for N64:
		// format: 0..4 (RGBA, YUV, CI, IA, I)
		// size: 0..3 (4b, 8b, 16b, 32b)

		int fmt = state.textureTile[t].format;
		int siz = state.textureTile[t].size;

		// If corrupted, clamp to safe defaults
		if (fmt < 0 || fmt > 4)
			fmt = G_IM_FMT_RGBA;
		if (siz < 0 || siz > 3)
			siz = G_IM_SIZ_16b;

		// Apply clamped values back
		state.textureTile[t].format = fmt;
		state.textureTile[t].size = siz;

		maxTexels = ImageFormat.imageFormat[state.textureTile[t].size][state.textureTile[t].format].maxTexels;

		// Here comes a bunch of code that just calculates the texture size...I wish
		// there was an easier way...
		tileWidth = state.textureTile[t].lrs - state.textureTile[t].uls + 1;
		tileHeight = state.textureTile[t].lrt - state.textureTile[t].ult + 1;

		maskWidth = 1 << state.textureTile[t].masks;
		maskHeight = 1 << state.textureTile[t].maskt;

		loadWidth = state.loadTile.lrs - state.loadTile.uls + 1;
		loadHeight = state.loadTile.lrt - state.loadTile.ult + 1;

		lineWidth = state.textureTile[t].line << ImageFormat.imageFormat[state.textureTile[t].size][state.textureTile[t].format].lineShift;

		if (lineWidth != 0) // Don't allow division by zero
			lineHeight = StrictMath.min(maxTexels / lineWidth, tileHeight);
		else
			lineHeight = 0;

		if (state.textureMode == TEXTUREMODE_TEXRECT) {
			int texRectWidth = state.texRect.width - state.textureTile[t].uls;
			int texRectHeight = state.texRect.height - state.textureTile[t].ult;

			if ((state.textureTile[t].masks != 0) && ((maskWidth * maskHeight) <= maxTexels))
				width = maskWidth;
			else if ((tileWidth * tileHeight) <= maxTexels)
				width = tileWidth;
			else if ((tileWidth * texRectHeight) <= maxTexels)
				width = tileWidth;
			else if ((texRectWidth * tileHeight) <= maxTexels)
				width = state.texRect.width;
			else if ((texRectWidth * texRectHeight) <= maxTexels)
				width = state.texRect.width;
			else if (state.loadType == LOADTYPE_TILE)
				width = loadWidth;
			else
				width = lineWidth;

			if ((state.textureTile[t].maskt != 0) && ((maskWidth * maskHeight) <= maxTexels))
				height = maskHeight;
			else if ((tileWidth * tileHeight) <= maxTexels)
				height = tileHeight;
			else if ((tileWidth * texRectHeight) <= maxTexels)
				height = state.texRect.height;
			else if ((texRectWidth * tileHeight) <= maxTexels)
				height = tileHeight;
			else if ((texRectWidth * texRectHeight) <= maxTexels)
				height = state.texRect.height;
			else if (state.loadType == LOADTYPE_TILE)
				height = loadHeight;
			else
				height = lineHeight;

		} else {
			if ((state.textureTile[t].masks != 0) && ((maskWidth * maskHeight) <= maxTexels))
				width = maskWidth; // Use mask width if set and valid
			else if ((tileWidth * tileHeight) <= maxTexels)
				width = tileWidth; // else use tile width if valid
			else if (state.loadType == LOADTYPE_TILE)
				width = loadWidth; // else use load width if load done with LoadTile
			else
				width = lineWidth; // else use line-based width

			if ((state.textureTile[t].maskt != 0) && ((maskWidth * maskHeight) <= maxTexels))
				height = maskHeight;
			else if ((tileWidth * tileHeight) <= maxTexels)
				height = tileHeight;
			else if (state.loadType == LOADTYPE_TILE)
				height = loadHeight;
			else
				height = lineHeight;
		}

		clampWidth = state.textureTile[t].clamps != 0 ? tileWidth : width;
		clampHeight = state.textureTile[t].clampt != 0 ? tileHeight : height;

		if (clampWidth > 256)
			state.textureTile[t].clamps = 0;
		if (clampHeight > 256)
			state.textureTile[t].clampt = 0;

		// Make sure masking is valid
		if (maskWidth > width) {
			state.textureTile[t].masks = state.textureTile[t].powof(width);
			maskWidth = 1 << state.textureTile[t].masks;
		}

		if (maskHeight > height) {
			state.textureTile[t].maskt = state.textureTile[t].powof(height);
			maskHeight = 1 << state.textureTile[t].maskt;
		}
		int src = 0;
		int bpl = 0;
		int lineBytes = 0;

		src = state.textureTile[t].tmem << 3;
		bpl = width << state.textureTile[t].size >> 1;
		lineBytes = state.textureTile[t].line << 3;
		state.crc32.reset();

		int realSrc = state.textureTile[t].tmem << 3;
		int tmemSize = state.tmem.array().length; // normally 4096

		if (realSrc < 0 || realSrc >= tmemSize)
			return;
		if (bpl < 1)
			bpl = 1;
		if (bpl > tmemSize)
			bpl = tmemSize;

		state.crc32.reset();

		for (int y = 0; y < height; y++) {
			if (realSrc + bpl > tmemSize)
				break; // <<< HARD STOP to prevent crash
			state.crc32.update(state.tmem.array(), realSrc, bpl);
			realSrc += lineBytes;

			if (realSrc < 0 || realSrc >= tmemSize)
				break;
		}

		if (state.textureTile[t].format == G_IM_FMT_CI) {
			if (state.textureTile[t].size == G_IM_SIZ_4b) {
				state.crc32.update(state.paletteCRC16.array(), state.textureTile[t].palette << 2, 4);
			} else if (state.textureTile[t].size == G_IM_SIZ_8b) {
				state.crc32.update(state.paletteCRC256.array(), 0, 4);
			}
		}
		int crc = (int) state.crc32.getValue();
		/******************************************************
		 * N64 CORRUPTION-SAFE PATCH FOR KERNELN64 TEXTURECACHE
		 ******************************************************/

		// Avoid negative or zero dimensions
		if (tileWidth < 1)
			tileWidth = 1;
		if (tileHeight < 1)
			tileHeight = 1;

		if (maskWidth < 1)
			maskWidth = 1;
		if (maskHeight < 1)
			maskHeight = 1;

		if (loadWidth < 1)
			loadWidth = 1;
		if (loadHeight < 1)
			loadHeight = 1;

		if (state.texRect.width < 1)
			state.texRect.width = 1;
		if (state.texRect.height < 1)
			state.texRect.height = 1;

		if (width < 1)
			width = 1;
		if (height < 1)
			height = 1;

		// TMEM line stride cannot be zero
		if (lineWidth < 1)
			lineWidth = 1;

		// Clamp real clamp sizes (now initialized)
		if (clampWidth < 1)
			clampWidth = 1;
		if (clampHeight < 1)
			clampHeight = 1;

		// Prevent ridiculous values on corrupted ROMs
		if (width > 1024)
			width = 1024;
		if (height > 1024)
			height = 1024;

		if (clampWidth > 1024)
			clampWidth = 1024;
		if (clampHeight > 1024)
			clampHeight = 1024;

		// Mask bit sanity
		if (state.textureTile[t].masks < 0)
			state.textureTile[t].masks = 0;
		if (state.textureTile[t].maskt < 0)
			state.textureTile[t].maskt = 0;
		if (state.textureTile[t].masks > 12)
			state.textureTile[t].masks = 12;
		if (state.textureTile[t].maskt > 12)
			state.textureTile[t].maskt = 12;

		// TMEM safe area
		if ((state.textureTile[t].tmem << 3) >= 4096)
			state.textureTile[t].tmem = 0;

		// END safety block

		for (CachedTexture tex : kernel64gl.engine.config.RendererSettings.cacheTextures ? state.stack.candidates(crc) : java.util.List.<CachedTexture>of()) {

			// ⚠ SKIP unsafe cached textures (corrupted by TMEM chaos)
			if (tex.width < 1 || tex.height < 1 || tex.width > 2048 || tex.height > 2048 || tex.clampWidth < 1
					|| tex.clampHeight < 1 || tex.maskS < 0 || tex.maskT < 0 || tex.maskS > 12 || tex.maskT > 12
					|| tex.mirrorS < 0 || tex.mirrorT < 0 || tex.mirrorS > 1 || tex.mirrorT > 1 || tex.clampS < 0
					|| tex.clampT < 0 || tex.clampS > 1 || tex.clampT > 1) {

				// skip broken entry
				continue;
			}

			// normal matching
			if ((tex.crc == crc) && (tex.width == width) && (tex.height == height) && (tex.clampWidth == clampWidth)
					&& (tex.clampHeight == clampHeight) && (tex.maskS == state.textureTile[t].masks)
					&& (tex.maskT == state.textureTile[t].maskt) && (tex.mirrorS == state.textureTile[t].mirrors)
					&& (tex.mirrorT == state.textureTile[t].mirrort) && (tex.clampS == state.textureTile[t].clamps)
					&& (tex.clampT == state.textureTile[t].clampt) && (tex.format == state.textureTile[t].format)
					&& (tex.size == state.textureTile[t].size)) {

				state.activateTexture(t, tex, linear);
				state.hits++;
				return;
			}

		}

		state.misses++;

		// If multitexturing, set the appropriate texture
		if (state.ARB_multitexture)
			state.gl.glActiveTexture(GL.GL_TEXTURE0 + t);

		state.current[t] = new CachedTexture();
		state.prune();
		state.stack.addTop(state.current[t]);
		state.gl.glBindTexture(GL.GL_TEXTURE_2D, state.current[t].glName[0]);

		state.current[t].address = state.textureImage.address;
		state.current[t].crc = crc;
		state.current[t].format = state.textureTile[t].format;
		state.current[t].size = state.textureTile[t].size;
		state.current[t].width = width;
		state.current[t].height = height;
		state.current[t].clampWidth = clampWidth;
		state.current[t].clampHeight = clampHeight;
		state.current[t].palette = state.textureTile[t].palette;
		state.current[t].maskS = state.textureTile[t].masks;
		state.current[t].maskT = state.textureTile[t].maskt;
		state.current[t].mirrorS = state.textureTile[t].mirrors;
		state.current[t].mirrorT = state.textureTile[t].mirrort;
		state.current[t].clampS = state.textureTile[t].clamps;
		state.current[t].clampT = state.textureTile[t].clampt;
		state.current[t].line = state.textureTile[t].line;
		state.current[t].tMem = state.textureTile[t].tmem;

		if (state.current[t].clampS != 0)
			state.current[t].realWidth = state.current[t].pow2(clampWidth);
		else if (state.current[t].mirrorS != 0)
			state.current[t].realWidth = maskWidth << 1;
		else
			state.current[t].realWidth = state.current[t].pow2(width);
		if (state.current[t].clampT != 0)
			state.current[t].realHeight = state.current[t].pow2(clampHeight);
		else if (state.current[t].mirrorT != 0)
			state.current[t].realHeight = maskHeight << 1;
		else
			state.current[t].realHeight = state.current[t].pow2(height);
		state.current[t].scaleS = 1.0f / (float) (state.current[t].realWidth);
		state.current[t].scaleT = 1.0f / (float) (state.current[t].realHeight);
		state.current[t].shiftScaleS = 1.0f;
		state.current[t].shiftScaleT = 1.0f;
		state.current[t].offsetS = state.enable2xSaI ? 0.25f : 0.5f;
		state.current[t].offsetT = state.enable2xSaI ? 0.25f : 0.5f;
		if (state.textureTile[t].shifts > 10)
			state.current[t].shiftScaleS = (float) (1 << (16 - state.textureTile[t].shifts));
		else if (state.textureTile[t].shifts > 0)
			state.current[t].shiftScaleS /= (float) (1 << state.textureTile[t].shifts);
		if (state.textureTile[t].shiftt > 10)
			state.current[t].shiftScaleT = (float) (1 << (16 - state.textureTile[t].shiftt));
		else if (state.textureTile[t].shiftt > 0)
			state.current[t].shiftScaleT /= (float) (1 << state.textureTile[t].shiftt);

		state.current[t].load(IA16, state.textureBitDepth, state.tmem, state.gl);
		state.activateTexture(t, state.current[t], linear);
		state.cachedBytes += state.current[t].textureBytes;
        state.stack.index(state.current[t]);
	}

	static void updateBackground(TextureCache state, boolean IA16, boolean linear) {
		// calculate bgImage crc
		int numBytes = state.bgImage.width * state.bgImage.height << state.bgImage.size >>> 1;
		state.crc32.reset();
		state.crc32.update(state.rdram.array(), state.bgImage.address, numBytes);
		if (state.bgImage.format == G_IM_FMT_CI) {
			if (state.bgImage.size == G_IM_SIZ_4b) {
				state.crc32.update(state.paletteCRC16.array(), state.bgImage.palette << 2, 4);
			} else if (state.bgImage.size == G_IM_SIZ_8b) {
				state.crc32.update(state.paletteCRC256.array(), 0, 4);
			}
		}
		int crc = (int) state.crc32.getValue();

		// find and activate bgImage
		for (CachedTexture tex : kernel64gl.engine.config.RendererSettings.cacheTextures ? state.stack.candidates(crc) : java.util.List.<CachedTexture>of()) {
			if ((tex.crc == crc) && (tex.width == state.bgImage.width) && (tex.height == state.bgImage.height)
					&& (tex.format == state.bgImage.format) && (tex.size == state.bgImage.size)) {
				state.activateTexture(0, tex, linear);
				state.hits++;
				return;
			}

		}

		// bgImage not found
		state.misses++;

		// If multitexturing, set the appropriate texture
		if (state.ARB_multitexture)
			state.gl.glActiveTexture(GL.GL_TEXTURE0);

		state.current[0] = new CachedTexture();
		state.prune();
		state.stack.addTop(state.current[0]);
		state.gl.glBindTexture(GL.GL_TEXTURE_2D, state.current[0].glName[0]);

		state.current[0].address = state.bgImage.address;
		state.current[0].crc = crc;
		state.current[0].format = state.bgImage.format;
		state.current[0].size = state.bgImage.size;
		state.current[0].width = state.bgImage.width;
		state.current[0].height = state.bgImage.height;
		state.current[0].clampWidth = state.bgImage.width;
		state.current[0].clampHeight = state.bgImage.height;
		state.current[0].palette = state.bgImage.palette;
		state.current[0].maskS = 0;
		state.current[0].maskT = 0;
		state.current[0].mirrorS = 0;
		state.current[0].mirrorT = 0;
		state.current[0].clampS = 1;
		state.current[0].clampT = 1;
		state.current[0].line = 0;
		state.current[0].tMem = 0;

		state.current[0].realWidth = state.current[0].pow2(state.bgImage.width);
		state.current[0].realHeight = state.current[0].pow2(state.bgImage.height);
		state.current[0].scaleS = 1.0f / (float) (state.current[0].realWidth);
		state.current[0].scaleT = 1.0f / (float) (state.current[0].realHeight);
		state.current[0].shiftScaleS = 1.0f;
		state.current[0].shiftScaleT = 1.0f;

		state.current[0].loadBackground(IA16, state.textureBitDepth, state.rdram, state.gl, state.bgImage.width, state.bgImage.height, state.bgImage.size,
				state.bgImage.address);
		state.activateTexture(0, state.current[0], linear);
		state.cachedBytes += state.current[0].textureBytes;
        state.stack.index(state.current[0]);
	}

	static void prune(TextureCache state) {
		while (state.cachedBytes > state.maxBytes) {
            CachedTexture victim=state.stack.bottom;
            if(victim==state.dummy)victim=victim.higher;
            // The dummy is permanent. Do not spin if it is the only entry.
            if(victim==null)break;
            state.cachedBytes-=victim.textureBytes;
            state.stack.remove(victim);
		}
	}

	static void activateTexture(TextureCache state, int t, CachedTexture texture, boolean linear) {
        linear = kernel64gl.engine.config.RendererSettings.bilinear && (linear || kernel64gl.engine.config.RendererSettings.forceBilinear);

		if (state.ARB_multitexture)
			state.gl.glActiveTexture(GL.GL_TEXTURE0 + t);
		// Bind the cached texture
		state.gl.glBindTexture(GL.GL_TEXTURE_2D, texture.glName[0]);

		if (linear) {
			state.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
			state.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
		} else {
			state.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
			state.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
		}
		// Set clamping modes
		state.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_S,
				texture.clampS != 0 ? GL.GL_CLAMP_TO_EDGE : GL.GL_REPEAT);
		state.gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_WRAP_T,
				texture.clampT != 0 ? GL.GL_CLAMP_TO_EDGE : GL.GL_REPEAT);

		state.stack.moveToTop(texture);
		state.current[t] = texture;
	}
}
