package java.util.zip;

import java.util.Arrays;

import com.jcraft.jzlib.GZIPException;
import com.jcraft.jzlib.ZStream;

import elemental2.dom.DomGlobal;

@SuppressWarnings("deprecation")
public class Deflater {

	private final ZStreamRef zsRef;
	private byte[] buf = new byte[0];
	private int off, len;
	private int level, strategy;
	private boolean setParams;
	private boolean finish, finished;
	private long bytesRead;
	private long bytesWritten;

	/**
	 * Compression method for the deflate algorithm (the only one currently
	 * supported).
	 */
	public static final int DEFLATED = 8;

	/**
	 * Compression level for no compression.
	 */
	public static final int NO_COMPRESSION = 0;

	/**
	 * Compression level for fastest compression.
	 */
	public static final int BEST_SPEED = 1;

	/**
	 * Compression level for best compression.
	 */
	public static final int BEST_COMPRESSION = 9;

	/**
	 * Default compression level.
	 */
	public static final int DEFAULT_COMPRESSION = -1;

	/**
	 * Compression strategy best used for data consisting mostly of small
	 * values with a somewhat random distribution. Forces more Huffman coding
	 * and less string matching.
	 */
	public static final int FILTERED = 1;

	/**
	 * Compression strategy for Huffman coding only.
	 */
	public static final int HUFFMAN_ONLY = 2;

	/**
	 * Default compression strategy.
	 */
	public static final int DEFAULT_STRATEGY = 0;

	/**
	 * Compression flush mode used to achieve best compression result.
	 *
	 * @see Deflater#deflate(byte[], int, int, int)
	 * @since 1.7
	 */
	public static final int NO_FLUSH = 0;

	/**
	 * Compression flush mode used to flush out all pending output; may
	 * degrade compression for some compression algorithms.
	 *
	 * @see Deflater#deflate(byte[], int, int, int)
	 * @since 1.7
	 */
	public static final int SYNC_FLUSH = 2;

	/**
	 * Compression flush mode used to flush out all pending output and
	 * reset the deflater. Using this mode too often can seriously degrade
	 * compression.
	 *
	 * @see Deflater#deflate(byte[], int, int, int)
	 * @since 1.7
	 */
	public static final int FULL_FLUSH = 3;

	/**
	 * Creates a new compressor using the specified compression level.
	 * If 'nowrap' is true then the ZLIB header and checksum fields will
	 * not be used in order to support the compression format used in
	 * both GZIP and PKZIP.
	 * @param level the compression level (0-9)
	 * @param nowrap if true then use GZIP compatible compression
	 */
	public Deflater(int level, boolean nowrap) {
		this.level = level;
		this.strategy = DEFAULT_STRATEGY;
		this.zsRef = new ZStreamRef(init(level, DEFAULT_STRATEGY, nowrap));
	}

	/**
	 * Creates a new compressor using the specified compression level.
	 * Compressed data will be generated in ZLIB format.
	 * @param level the compression level (0-9)
	 */
	public Deflater(int level) {
		this(level, false);
	}

	/**
	 * Creates a new compressor with the default compression level.
	 * Compressed data will be generated in ZLIB format.
	 */
	public Deflater() {
		this(DEFAULT_COMPRESSION, false);
	}

	/**
	 * Sets input data for compression. This should be called whenever
	 * needsInput() returns true indicating that more input data is required.
	 * @param b the input data bytes
	 * @param off the start offset of the data
	 * @param len the length of the data
	 * @see Deflater#needsInput
	 */
	public void setInput(byte[] b, int off, int len) {
		if (b== null) {
			throw new NullPointerException();
		}
		if (off < 0 || len < 0 || off > b.length - len) {
			throw new ArrayIndexOutOfBoundsException();
		}
		synchronized (zsRef) {
			this.buf = b;
			this.off = off;
			this.len = len;
		}
	}

	/**
	 * Sets input data for compression. This should be called whenever
	 * needsInput() returns true indicating that more input data is required.
	 * @param b the input data bytes
	 * @see Deflater#needsInput
	 */
	public void setInput(byte[] b) {
		setInput(b, 0, b.length);
	}

	/**
	 * Sets preset dictionary for compression. A preset dictionary is used
	 * when the history buffer can be predetermined. When the data is later
	 * uncompressed with Inflater.inflate(), Inflater.getAdler() can be called
	 * in order to get the Adler-32 value of the dictionary required for
	 * decompression.
	 * @param b the dictionary data bytes
	 * @param off the start offset of the data
	 * @param len the length of the data
	 * @see Inflater#inflate
	 * @see Inflater#getAdler
	 */
	public void setDictionary(byte[] b, int off, int len) {
		if (b == null) {
			throw new NullPointerException();
		}
		if (off < 0 || len < 0 || off > b.length - len) {
			throw new ArrayIndexOutOfBoundsException();
		}
		synchronized (zsRef) {
			ensureOpen();
			setDictionary(zsRef.address(), b, off, len);
		}
	}

	/**
	 * Sets preset dictionary for compression. A preset dictionary is used
	 * when the history buffer can be predetermined. When the data is later
	 * uncompressed with Inflater.inflate(), Inflater.getAdler() can be called
	 * in order to get the Adler-32 value of the dictionary required for
	 * decompression.
	 * @param b the dictionary data bytes
	 * @see Inflater#inflate
	 * @see Inflater#getAdler
	 */
	public void setDictionary(byte[] b) {
		setDictionary(b, 0, b.length);
	}

	/**
	 * Sets the compression strategy to the specified value.
	 *
	 * <p> If the compression strategy is changed, the next invocation
	 * of {@code deflate} will compress the input available so far with
	 * the old strategy (and may be flushed); the new strategy will take
	 * effect only after that invocation.
	 *
	 * @param strategy the new compression strategy
	 * @exception IllegalArgumentException if the compression strategy is
	 *                                     invalid
	 */
	public void setStrategy(int strategy) {
		switch (strategy) {
		case DEFAULT_STRATEGY:
		case FILTERED:
		case HUFFMAN_ONLY:
			break;
		default:
			throw new IllegalArgumentException();
		}
		synchronized (zsRef) {
			if (this.strategy != strategy) {
				this.strategy = strategy;
				setParams = true;
			}
		}
	}

	/**
	 * Sets the compression level to the specified value.
	 *
	 * <p> If the compression level is changed, the next invocation
	 * of {@code deflate} will compress the input available so far
	 * with the old level (and may be flushed); the new level will
	 * take effect only after that invocation.
	 *
	 * @param level the new compression level (0-9)
	 * @exception IllegalArgumentException if the compression level is invalid
	 */
	public void setLevel(int level) {
		if ((level < 0 || level > 9) && level != DEFAULT_COMPRESSION) {
			throw new IllegalArgumentException("invalid compression level");
		}
		synchronized (zsRef) {
			if (this.level != level) {
				this.level = level;
				setParams = true;
			}
		}
	}

	/**
	 * Returns true if the input data buffer is empty and setInput()
	 * should be called in order to provide more input.
	 * @return true if the input data buffer is empty and setInput()
	 * should be called in order to provide more input
	 */
	public boolean needsInput() {
		synchronized (zsRef) {
			return len <= 0;
		}
	}

	/**
	 * When called, indicates that compression should end with the current
	 * contents of the input buffer.
	 */
	public void finish() {
		synchronized (zsRef) {
			finish = true;
		}
	}

	/**
	 * Returns true if the end of the compressed data output stream has
	 * been reached.
	 * @return true if the end of the compressed data output stream has
	 * been reached
	 */
	public boolean finished() {
		synchronized (zsRef) {
			return finished;
		}
	}

	/**
	 * Compresses the input data and fills specified buffer with compressed
	 * data. Returns actual number of bytes of compressed data. A return value
	 * of 0 indicates that {@link #needsInput() needsInput} should be called
	 * in order to determine if more input data is required.
	 *
	 * <p>This method uses {@link #NO_FLUSH} as its compression flush mode.
	 * An invocation of this method of the form {@code deflater.deflate(b, off, len)}
	 * yields the same result as the invocation of
	 * {@code deflater.deflate(b, off, len, Deflater.NO_FLUSH)}.
	 *
	 * @param b the buffer for the compressed data
	 * @param off the start offset of the data
	 * @param len the maximum number of bytes of compressed data
	 * @return the actual number of bytes of compressed data written to the
	 *         output buffer
	 */
	public int deflate(byte[] b, int off, int len) {
		return deflate(b, off, len, NO_FLUSH);
	}

	/**
	 * Compresses the input data and fills specified buffer with compressed
	 * data. Returns actual number of bytes of compressed data. A return value
	 * of 0 indicates that {@link #needsInput() needsInput} should be called
	 * in order to determine if more input data is required.
	 *
	 * <p>This method uses {@link #NO_FLUSH} as its compression flush mode.
	 * An invocation of this method of the form {@code deflater.deflate(b)}
	 * yields the same result as the invocation of
	 * {@code deflater.deflate(b, 0, b.length, Deflater.NO_FLUSH)}.
	 *
	 * @param b the buffer for the compressed data
	 * @return the actual number of bytes of compressed data written to the
	 *         output buffer
	 */
	public int deflate(byte[] b) {
		return deflate(b, 0, b.length, NO_FLUSH);
	}

	/**
	 * Compresses the input data and fills the specified buffer with compressed
	 * data. Returns actual number of bytes of data compressed.
	 *
	 * <p>Compression flush mode is one of the following three modes:
	 *
	 * <ul>
	 * <li>{@link #NO_FLUSH}: allows the deflater to decide how much data
	 * to accumulate, before producing output, in order to achieve the best
	 * compression (should be used in normal use scenario). A return value
	 * of 0 in this flush mode indicates that {@link #needsInput()} should
	 * be called in order to determine if more input data is required.
	 *
	 * <li>{@link #SYNC_FLUSH}: all pending output in the deflater is flushed,
	 * to the specified output buffer, so that an inflater that works on
	 * compressed data can get all input data available so far (In particular
	 * the {@link #needsInput()} returns {@code true} after this invocation
	 * if enough output space is provided). Flushing with {@link #SYNC_FLUSH}
	 * may degrade compression for some compression algorithms and so it
	 * should be used only when necessary.
	 *
	 * <li>{@link #FULL_FLUSH}: all pending output is flushed out as with
	 * {@link #SYNC_FLUSH}. The compression state is reset so that the inflater
	 * that works on the compressed output data can restart from this point
	 * if previous compressed data has been damaged or if random access is
	 * desired. Using {@link #FULL_FLUSH} too often can seriously degrade
	 * compression.
	 * </ul>
	 *
	 * <p>In the case of {@link #FULL_FLUSH} or {@link #SYNC_FLUSH}, if
	 * the return value is {@code len}, the space available in output
	 * buffer {@code b}, this method should be invoked again with the same
	 * {@code flush} parameter and more output space.
	 *
	 * @param b the buffer for the compressed data
	 * @param off the start offset of the data
	 * @param len the maximum number of bytes of compressed data
	 * @param flush the compression flush mode
	 * @return the actual number of bytes of compressed data written to
	 *         the output buffer
	 *
	 * @throws IllegalArgumentException if the flush mode is invalid
	 * @since 1.7
	 */
	public int deflate(byte[] b, int off, int len, int flush) {
		if (b == null) {
			throw new NullPointerException();
		}
		if (off < 0 || len < 0 || off > b.length - len) {
			throw new ArrayIndexOutOfBoundsException();
		}
		synchronized (zsRef) {
			ensureOpen();
			if (flush == NO_FLUSH || flush == SYNC_FLUSH ||
					flush == FULL_FLUSH) {
				int thisLen = this.len;
				int n = deflateBytes(zsRef.address(), b, off, len, flush);
				bytesWritten += n;
				bytesRead += (thisLen - this.len);
				return n;
			}
			throw new IllegalArgumentException();
		}
	}

	/**
	 * Returns the ADLER-32 value of the uncompressed data.
	 * @return the ADLER-32 value of the uncompressed data
	 */
	public int getAdler() {
		synchronized (zsRef) {
			ensureOpen();
			return getAdler(zsRef.address());
		}
	}

	/**
	 * Returns the total number of uncompressed bytes input so far.
	 *
	 * <p>Since the number of bytes may be greater than
	 * Integer.MAX_VALUE, the {@link #getBytesRead()} method is now
	 * the preferred means of obtaining this information.</p>
	 *
	 * @return the total number of uncompressed bytes input so far
	 */
	public int getTotalIn() {
		return (int) getBytesRead();
	}

	/**
	 * Returns the total number of uncompressed bytes input so far.
	 *
	 * @return the total (non-negative) number of uncompressed bytes input so far
	 * @since 1.5
	 */
	public long getBytesRead() {
		synchronized (zsRef) {
			ensureOpen();
			return bytesRead;
		}
	}

	/**
	 * Returns the total number of compressed bytes output so far.
	 *
	 * <p>Since the number of bytes may be greater than
	 * Integer.MAX_VALUE, the {@link #getBytesWritten()} method is now
	 * the preferred means of obtaining this information.</p>
	 *
	 * @return the total number of compressed bytes output so far
	 */
	public int getTotalOut() {
		return (int) getBytesWritten();
	}

	/**
	 * Returns the total number of compressed bytes output so far.
	 *
	 * @return the total (non-negative) number of compressed bytes output so far
	 * @since 1.5
	 */
	public long getBytesWritten() {
		synchronized (zsRef) {
			ensureOpen();
			return bytesWritten;
		}
	}

	/**
	 * Resets deflater so that a new set of input data can be processed.
	 * Keeps current compression level and strategy settings.
	 */
	public void reset() {
		synchronized (zsRef) {
			ensureOpen();
			reset(zsRef.address());
			finish = false;
			finished = false;
			off = len = 0;
			bytesRead = bytesWritten = 0;
		}
	}

	/**
	 * Closes the compressor and discards any unprocessed input.
	 * This method should be called when the compressor is no longer
	 * being used, but will also be called automatically by the
	 * finalize() method. Once this method is called, the behavior
	 * of the Deflater object is undefined.
	 */
	public void end() {
		synchronized (zsRef) {
			ZStream addr = zsRef.address();
			zsRef.clear();
			if (addr != null) {
				end(addr);
				buf = null;
			}
		}
	}

	/**
	 * Closes the compressor when garbage is collected.
	 */
	@Override
	protected void finalize() {
		end();
	}

	private void ensureOpen() {
		if (zsRef.address() == null)
			throw new NullPointerException("Deflater has been closed");
	}

	private static ZStream init(int level, int strategy, boolean nowrap) {
		try {
			return new com.jcraft.jzlib.Deflater(level, nowrap);
		} catch (GZIPException e) {
			return null;
		}
	}
	private static void setDictionary(ZStream addr, byte[] b, int off, int len) {
		addr.deflateSetDictionary(Arrays.copyOfRange(b, off, len), len);
	}
	private int deflateBytes(ZStream addr, byte[] b, int off, int len,
			int flush) {
		int this_off = this.off;
		int this_len = this.len;
		byte[] this_buf = this.buf;
		if(setParams) {
			addr.next_in = this_buf;
			addr.next_in_index = this_off;
			addr.avail_in = this_len;
			addr.next_out = b;
			addr.next_out_index = off;
			addr.avail_out = len;
			int res = addr.deflateParams(level, strategy);
			switch (res) {
			case 0:
				setParams = false;
				this_off += this_len - addr.avail_in;
				this.off = this_off;
				this.len = addr.avail_in;
				return len - addr.avail_out;

			case -5:
				setParams = false;
				return 0;

			default:
				throw new RuntimeException(addr.msg);
			}
		} else {
			boolean finish = this.finish;
			addr.next_in = this_buf;
			addr.next_in_index = this_off;
			addr.avail_in = this_len;
			addr.next_out = b;
			addr.next_out_index = off;
			addr.avail_out = len;
			int res = addr.deflate(finish ? 4 : flush);
			switch (res) {
			case 1:
				finished = true;
			case 0:
				this_off += this_len - addr.avail_in;
				this.off = this_off;
				this.len = addr.avail_in;
				return len - addr.avail_out;

			case -5:
				return 0;

			default:
				throw new RuntimeException(addr.msg);
			}
		}

	}
	private static int getAdler(ZStream addr) {
		return (int) addr.getAdler();
	}
	private static void reset(ZStream addr) {
		DomGlobal.console.error("Reset not supported");
	}
	private static void end(ZStream addr) {
		addr.deflateEnd();
	}
}
