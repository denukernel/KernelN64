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

import java.nio.ByteBuffer;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import javax.media.opengl.GL;


/** State owner and command facade; implementation helpers share this state.
 * Keep state fields here for compatibility with existing save states. */
public class TextureCache {

	// GBI
	// Image formats
	public static final int G_IM_FMT_RGBA = 0;
	public static final int G_IM_FMT_YUV = 1;
	public static final int G_IM_FMT_CI = 2;
	public static final int G_IM_FMT_IA = 3;
	public static final int G_IM_FMT_I = 4;
	// Image sizes
	public static final int G_IM_SIZ_4b = 0;
	public static final int G_IM_SIZ_8b = 1;
	public static final int G_IM_SIZ_16b = 2;
	public static final int G_IM_SIZ_32b = 3;
	public static final int G_IM_SIZ_DD = 5;

	public static final int TEXTUREMODE_NORMAL = 0;
	public static final int TEXTUREMODE_TEXRECT = 1;
	public static final int TEXTUREMODE_BGIMAGE = 2;
//    public static final int TEXTUREMODE_FRAMEBUFFER=	3;

	public static final int LOADTYPE_BLOCK = 0;
	public static final int LOADTYPE_TILE = 1;

	public static final int CHANGED_TMEM = 0x008;

	static final int CRC32_POLYNOMIAL = 0x04C11DB7;

	static class BgImage {
		public int address;
		public int width;
		public int height;
		public int format;
		public int size;
		public int palette;
	};

	static class TextureImage {
		public int format;
		public int size;
		public int width;
		public int bpl;
		public int address;
	};

	static class TexRect {
		public int width;
		public int height;
	};

	static class Texture {
		public float scales;
		public float scalet;
		public int level;
		public int on;
		public int tile;
	};

	// used by Gdp
	public static class gDPTile {
		int format, size, line, tmem, palette;

//        public FrameBufferStack.FrameBuffer frameBuffer;
		public int maskt, masks;
		public int shiftt, shifts;
		public float fuls, fult, flrs, flrt;
		public int uls, ult, lrs, lrt;

		public int mirrort; // : 1;
		public int clampt; // : 1;
		public int pad0; // : 30;

		public int mirrors; // : 1;
		public int clamps; // : 1;
		public int pad1; // : 30;

		public void setCmt(int value) {
			pad0 = (value >> 2) & 0x3FFFFFFF;
			clampt = (value >> 1) & 1;
			mirrort = (value) & 1;
		}

		public int getCmt() {
			return ((pad0 & 0x3FFFFFFF) << 2) | ((clampt & 1) << 1) | (mirrort & 1);
		}

		public void setCms(int value) {
			pad1 = (value >> 2) & 0x3FFFFFFF;
			clamps = (value >> 1) & 1;
			mirrors = (value) & 1;
		}

		public int getCms() {
			return ((pad1 & 0x3FFFFFFF) << 2) | ((clamps & 1) << 1) | (mirrors & 1);
		}

		public final int powof(int dim) {
			int num = 1;
			int i = 0;
			while (num < dim) {
				num <<= 1;
				i++;
			}
			return i;
		}

	};

	static interface InterleaveFunc {
		public void Interleave(byte[] mem, int off, int numDWords);
	};

	final InterleaveFunc DWordInterleave = new InterleaveFunc() {
		public final void Interleave(byte[] mem, int off, int numDWords) {
			numDWords = off + (numDWords << 3);
			for (int i = off; i < numDWords; i += 8) {
				System.arraycopy(mem, i, tmp, 0, 4);
				System.arraycopy(mem, i + 4, mem, i, 4);
				System.arraycopy(tmp, 0, mem, i + 4, 4);
			}
		}
	};

	final InterleaveFunc QWordInterleave = new InterleaveFunc() {
		public final void Interleave(byte[] mem, int off, int numDWords) {
			numDWords = off + (numDWords << 3);
			for (int i = off; i < numDWords; i += 16) {
				System.arraycopy(mem, i, tmp, 0, 4);
				System.arraycopy(mem, i + 8, mem, i, 4);
				System.arraycopy(tmp, 0, mem, i + 8, 4);
				System.arraycopy(mem, i + 4, tmp, 0, 4);
				System.arraycopy(mem, i + 12, mem, i + 4, 4);
				System.arraycopy(tmp, 0, mem, i + 12, 4);
			}
		}
	};

	// used by OpenGlGdp.updateStates
	public int changed;
	// called by OpenGlGdp.drawTexturedRectangle
	public gDPTile[] textureTile = new gDPTile[2];
	public CachedTexture[] current = new CachedTexture[2];

    /** Maximum mip tile offset supplied by gSPTexture. */
    public int textureLevel() { return texture.level; }

	boolean ARB_multitexture; // TNT, GeForce, Rage 128, Radeon
	int cachedBytes;
	CachedTextureStack stack = new CachedTextureStack();
	private Texture texture = new Texture();
	gDPTile[] tiles = new gDPTile[8];
	gDPTile loadTile = new gDPTile();
    int stateLoadTileIndex = 7;

    public void stateLoaded() {
        loadTile = tiles[stateLoadTileIndex];
        resetTextureTile();
        current[0] = null;
        current[1] = null;
        changed = -1;
    }
    /** Drop host uploads while preserving guest tiles, image descriptors and TMEM. */
    public void invalidateHostTextures() {
        for(CachedTexture texture=stack.top;texture!=null;){
            CachedTexture next=texture.lower;
            if(texture!=dummy)stack.remove(texture);
            texture=next;
        }
        current[0]=current[1]=null;
        cachedBytes=dummy==null?0:dummy.textureBytes;
        changed=-1;
    }
	TexRect texRect = new TexRect();
	TextureImage textureImage = new TextureImage();
	int loadType;
	ByteBuffer paletteCRC16 = ByteBuffer.allocate(64);
	ByteBuffer paletteCRC256 = ByteBuffer.allocate(4);
	int textureMode;
	GL gl;
	int maxBytes;
	int textureBitDepth;
	private final byte[] tmp = new byte[4];
	int hits;
	int misses;
	private int[] glNoiseNames = new int[32];
	CachedTexture dummy;
	BgImage bgImage = new BgImage();
	Checksum crc32;
	ByteBuffer rdram;
	ByteBuffer tmem;
	int rdramSize;
	boolean enable2xSaI;
	int[] crcTable = new int[256];
	private int maxTextureUnits; // TNT = 2, GeForce = 2-4, Rage 128 = 2, Radeon = 3-6

	// called by OpenGl
	public TextureCache() {

	}

	// called by Gdp.constructor
	public void construct() {
		for (int i = 0; i < tiles.length; i++)
			tiles[i] = new gDPTile();
		for (int i = 0; i < textureTile.length; i++)
			textureTile[i] = new gDPTile();
		loadTile = tiles[7];
		textureTile[0] = tiles[0];
		textureTile[1] = tiles[1];
	}

	// called by OpenGlGdp.config
	public void config(int maxBytes, int textureBitDepth) {
		this.maxBytes = maxBytes;
		this.textureBitDepth = textureBitDepth;
		enable2xSaI = false;
	}

	// called by OpenGlGdp.init
	public void init(GL gl, ByteBuffer rdram, ByteBuffer tmem, int maxTextureUnits, boolean ARB_multitexture) {
		this.gl = gl;
		this.rdram = rdram;
		this.tmem = tmem;
		this.maxTextureUnits = maxTextureUnits;
		this.ARB_multitexture = ARB_multitexture;
		ImageFormat.settMem(tmem);
		this.rdramSize = rdram.capacity();
		ByteBuffer dummyTexture = ByteBuffer.allocateDirect(16 * 4);

		current[0] = null;
		current[1] = null;
		stack.init(gl);
		cachedBytes = 0;

		gl.glGenTextures(32, glNoiseNames, 0);

		ByteBuffer noise = ByteBuffer.allocateDirect(64 * 64 * 4);
		for (int i = 0; i < 32; i++) {
			gl.glBindTexture(GL.GL_TEXTURE_2D, glNoiseNames[i]);

			Random rand = new Random();

			for (int y = 0; y < 64; y++) {
				for (int x = 0; x < 64; x++) {
					byte random = (byte) (rand.nextInt() & 0xFF);
					noise.put(y * 64 * 4 + x * 4, random);
					noise.put(y * 64 * 4 + x * 4 + 1, random);
					noise.put(y * 64 * 4 + x * 4 + 2, random);
					noise.put(y * 64 * 4 + x * 4 + 3, random);
				}
			}
			gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA8, 64, 64, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, noise);
		}

		dummy = CachedTexture.getDummy();
		prune();
		stack.addTop(dummy);

		gl.glBindTexture(GL.GL_TEXTURE_2D, dummy.glName[0]);
		gl.glTexImage2D(GL.GL_TEXTURE_2D, 0, GL.GL_RGBA8, 2, 2, 0, GL.GL_RGBA, GL.GL_UNSIGNED_BYTE, dummyTexture);

		cachedBytes = dummy.textureBytes;

        // Adler-32 collides for SM64's O/U glyphs after TMEM interleaving,
        // causing one character's cached texture to replace the other.
		crc32 = new CRC32();

		for (int i = 0; i < maxTextureUnits; i++)
			activateDummy(i);

		crcBuildTable();
	}

	// called by OpenGl.addTriangle
	public float getTexS(int t, float s) {

		return (s * current[t].shiftScaleS * texture.scales - textureTile[t].fuls + current[t].offsetS)
				* current[t].scaleS;

	}

	// called by OpenGl.addTriangle
	public float getTexT(int t, float s) {

		return (s * current[t].shiftScaleT * texture.scalet - textureTile[t].fult + current[t].offsetT)
				* current[t].scaleT;

	}

	// called by OpenGlGdp.updateStates
	public void activateDummy(int t) {
		if (ARB_multitexture)
			gl.glActiveTexture(GL.GL_TEXTURE0 + t);
		gl.glBindTexture(GL.GL_TEXTURE_2D, dummy.glName[0]);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
		gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
	}

	// called by Gdp.gDPTexture
	public void setTexture(float sc, float tc, int level, int tile, int on) {
		texture.scales = sc;
		texture.scalet = tc;
		if (texture.scales == 0.0f)
			texture.scales = 1.0f;
		if (texture.scalet == 0.0f)
			texture.scalet = 1.0f;
		texture.level = level;
		texture.on = on;
		texture.tile = tile;
		textureTile[0] = tiles[tile];
		textureTile[1] = tiles[(tile < 7) ? (tile + 1) : tile];
	}

	// called by Gdp.gDPSetTextureImage
	public void setTextureImage(int format, int size, int width, int address) {
		textureImage.format = format;
		textureImage.size = size;
		textureImage.width = width;
		textureImage.address = address;
		textureImage.bpl = textureImage.width << textureImage.size >> 1;
	}

	public void setTextureTile(float lrs, float lrt, int tile, float s, float t, float dsdx, float dtdy) {
		textureTile[0] = tiles[tile];
		textureTile[1] = tiles[tile < 7 ? tile + 1 : tile];

//        float lrs = s + (lrx - ulx - 1) * dsdx;
//        float lrt = t + (lry - uly - 1) * dtdy;

		if (textureMode == TEXTUREMODE_NORMAL)
			textureMode = TEXTUREMODE_TEXRECT;

		texRect.width = (int) (StrictMath.max(lrs, s) + dsdx);
		texRect.height = (int) (StrictMath.max(lrt, t) + dtdy);
	}

	// called by Gdp.gDPTextureRectangle
	public void resetTextureTile() {
		textureTile[0] = tiles[texture.tile];
		textureTile[1] = tiles[texture.tile < 7 ? texture.tile + 1 : texture.tile];
	}


	// called by Gdp.gDPSetTile
	public void setTile(int format, int size, int line, int tmem, int tile, int palette, int cmt, int cms, int maskt,
			int masks, int shiftt, int shifts) {
		if (((size == G_IM_SIZ_4b) || (size == G_IM_SIZ_8b)) && (format == G_IM_FMT_RGBA))
			format = G_IM_FMT_CI;

		tiles[tile].format = format;
		tiles[tile].size = size;
		tiles[tile].line = line;
		tiles[tile].tmem = tmem;
		tiles[tile].palette = palette;
		tiles[tile].setCmt(cmt);
		tiles[tile].setCms(cms);
		tiles[tile].maskt = maskt;
		tiles[tile].masks = masks;
		tiles[tile].shiftt = shiftt;
		tiles[tile].shifts = shifts;
		if (tiles[tile].masks == 0)
			tiles[tile].clamps = 1;
		if (tiles[tile].maskt == 0)
			tiles[tile].clampt = 1;
	}

	// called by Gdp.gDPSetTileSize
	public void setTileSize(int tile, int uls, int ult, int lrs, int lrt, float fuls, float fult, float flrs,
			float flrt) {

		tiles[tile].uls = uls;
		tiles[tile].ult = ult;
		tiles[tile].lrs = lrs;
		tiles[tile].lrt = lrt;
		tiles[tile].fuls = fuls;
		tiles[tile].fult = fult;
		tiles[tile].flrs = flrs;
		tiles[tile].flrt = flrt;
	}

	// called by Gdp.gDPLoadTile
	public void loadTile(int tile) {
		TextureTransfers.loadTile(this, tile);
	}

	// called by Gdp.gDPLoadBlock
	public void loadBlock(int tile, int uls, int ult, int lrs, int dxt) {
		TextureTransfers.loadBlock(this, tile, uls, ult, lrs, dxt);
	}

	// called by Gdp.gDPLoadTLUT
	public void loadLUT(int tile) {
		TextureTransfers.loadLUT(this, tile);
	}

	void prune() {
		TextureUploads.prune(this);
	}

	void activateTexture(int t, CachedTexture texture, boolean linear) {
		TextureUploads.activateTexture(this, t, texture, linear);
	}

	// called by OpenGlGdp.updateStates
	public void update(boolean IA16, int t, float scaleX, float scaleY, boolean linear) {
		TextureUploads.update(this, IA16, t, scaleX, scaleY, linear);
	}

	// Private Methods /////////////////////////////////////////////////////////

	// called by update()
	void updateBackground(boolean IA16, boolean linear) {
		TextureUploads.updateBackground(this, IA16, linear);
	}

	// called by loadLUT
	int crcCalculatePalette(int crc, ByteBuffer buffer, int count) {
		return TextureTransfers.crcCalculatePalette(this, crc, buffer, count);
	}

	// called by init
	private void crcBuildTable() {
		TextureTransfers.crcBuildTable(this);
	}
	public void reset() {
	    changed = 0;
	    current[0] = null;
	    current[1] = null;
	    stack.init(gl);
	    cachedBytes = 0;
	    textureMode = 0;
	    loadType = 0;
	    hits = 0;
	    misses = 0;

	    for (int i = 0; i < tiles.length; i++) {
	        tiles[i].format = 0;
	        tiles[i].size = 0;
	        tiles[i].line = 0;
	        tiles[i].tmem = 0;
	        tiles[i].palette = 0;
	        tiles[i].maskt = 0;
	        tiles[i].masks = 0;
	        tiles[i].shiftt = 0;
	        tiles[i].shifts = 0;
	        tiles[i].uls = 0;
	        tiles[i].ult = 0;
	        tiles[i].lrs = 0;
	        tiles[i].lrt = 0;
	        tiles[i].fuls = 0f;
	        tiles[i].fult = 0f;
	        tiles[i].flrs = 0f;
	        tiles[i].flrt = 0f;
	    }

	    texture.scales = 1f;
	    texture.scalet = 1f;
	    texture.level = 0;
	    texture.on = 0;
	    texture.tile = 0;
	}

	int reflect(int ref, int ch) {
		return TextureTransfers.reflect(this, ref, ch);
	}

}
