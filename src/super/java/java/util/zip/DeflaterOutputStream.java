package java.util.zip;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class DeflaterOutputStream extends FilterOutputStream {
	/**
	 * Compressor for this stream.
	 */
	protected Deflater def;

	/**
	 * Output buffer for writing compressed data.
	 */
	protected byte[] buf;

	/**
	 * Indicates that the stream has been closed.
	 */

	private boolean closed = false;

	private final boolean syncFlush;

	/**
	 * Creates a new output stream with the specified compressor,
	 * buffer size and flush mode.

	 * @param out the output stream
	 * @param def the compressor ("deflater")
	 * @param size the output buffer size
	 * @param syncFlush
	 *        if {@code true} the {@link #flush()} method of this
	 *        instance flushes the compressor with flush mode
	 *        {@link Deflater#SYNC_FLUSH} before flushing the output
	 *        stream, otherwise only flushes the output stream
	 *
	 * @throws IllegalArgumentException if {@code size <= 0}
	 *
	 * @since 1.7
	 */
	public DeflaterOutputStream(OutputStream out,
			Deflater def,
			int size,
			boolean syncFlush) {
		super(out);
		if (out == null || def == null) {
			throw new NullPointerException();
		} else if (size <= 0) {
			throw new IllegalArgumentException("buffer size <= 0");
		}
		this.def = def;
		this.buf = new byte[size];
		this.syncFlush = syncFlush;
	}


	/**
	 * Creates a new output stream with the specified compressor and
	 * buffer size.
	 *
	 * <p>The new output stream instance is created as if by invoking
	 * the 4-argument constructor DeflaterOutputStream(out, def, size, false).
	 *
	 * @param out the output stream
	 * @param def the compressor ("deflater")
	 * @param size the output buffer size
	 * @exception IllegalArgumentException if {@code size <= 0}
	 */
	public DeflaterOutputStream(OutputStream out, Deflater def, int size) {
		this(out, def, size, false);
	}

	/**
	 * Creates a new output stream with the specified compressor, flush
	 * mode and a default buffer size.
	 *
	 * @param out the output stream
	 * @param def the compressor ("deflater")
	 * @param syncFlush
	 *        if {@code true} the {@link #flush()} method of this
	 *        instance flushes the compressor with flush mode
	 *        {@link Deflater#SYNC_FLUSH} before flushing the output
	 *        stream, otherwise only flushes the output stream
	 *
	 * @since 1.7
	 */
	public DeflaterOutputStream(OutputStream out,
			Deflater def,
			boolean syncFlush) {
		this(out, def, 512, syncFlush);
	}


	/**
	 * Creates a new output stream with the specified compressor and
	 * a default buffer size.
	 *
	 * <p>The new output stream instance is created as if by invoking
	 * the 3-argument constructor DeflaterOutputStream(out, def, false).
	 *
	 * @param out the output stream
	 * @param def the compressor ("deflater")
	 */
	public DeflaterOutputStream(OutputStream out, Deflater def) {
		this(out, def, 512, false);
	}

	boolean usesDefaultDeflater = false;


	/**
	 * Creates a new output stream with a default compressor, a default
	 * buffer size and the specified flush mode.
	 *
	 * @param out the output stream
	 * @param syncFlush
	 *        if {@code true} the {@link #flush()} method of this
	 *        instance flushes the compressor with flush mode
	 *        {@link Deflater#SYNC_FLUSH} before flushing the output
	 *        stream, otherwise only flushes the output stream
	 *
	 * @since 1.7
	 */
	public DeflaterOutputStream(OutputStream out, boolean syncFlush) {
		this(out, new Deflater(), 512, syncFlush);
		usesDefaultDeflater = true;
	}

	/**
	 * Creates a new output stream with a default compressor and buffer size.
	 *
	 * <p>The new output stream instance is created as if by invoking
	 * the 2-argument constructor DeflaterOutputStream(out, false).
	 *
	 * @param out the output stream
	 */
	public DeflaterOutputStream(OutputStream out) {
		this(out, false);
		usesDefaultDeflater = true;
	}

	/**
	 * Writes a byte to the compressed output stream. This method will
	 * block until the byte can be written.
	 * @param b the byte to be written
	 * @exception IOException if an I/O error has occurred
	 */
	@Override
	public void write(int b) throws IOException {
		byte[] buf = new byte[1];
		buf[0] = (byte)(b & 0xff);
		write(buf, 0, 1);
	}

	/**
	 * Writes an array of bytes to the compressed output stream. This
	 * method will block until all the bytes are written.
	 * @param b the data to be written
	 * @param off the start offset of the data
	 * @param len the length of the data
	 * @exception IOException if an I/O error has occurred
	 */
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (def.finished()) {
			throw new IOException("write beyond end of stream");
		}
		if ((off | len | (off + len) | (b.length - (off + len))) < 0) {
			throw new IndexOutOfBoundsException();
		} else if (len == 0) {
			return;
		}
		if (!def.finished()) {
			def.setInput(b, off, len);
			while (!def.needsInput()) {
				deflate();
			}
		}
	}

	/**
	 * Finishes writing compressed data to the output stream without closing
	 * the underlying stream. Use this method when applying multiple filters
	 * in succession to the same output stream.
	 * @exception IOException if an I/O error has occurred
	 */
	public void finish() throws IOException {
		if (!def.finished()) {
			def.finish();
			while (!def.finished()) {
				deflate();
			}
		}
	}

	/**
	 * Writes remaining compressed data to the output stream and closes the
	 * underlying stream.
	 * @exception IOException if an I/O error has occurred
	 */
	@Override
	public void close() throws IOException {
		if (!closed) {
			finish();
			if (usesDefaultDeflater)
				def.end();
			out.close();
			closed = true;
		}
	}

	/**
	 * Writes next block of compressed data to the output stream.
	 * @throws IOException if an I/O error has occurred
	 */
	protected void deflate() throws IOException {
		int len = def.deflate(buf, 0, buf.length);
		if (len > 0) {
			out.write(buf, 0, len);
		}
	}

	/**
	 * Flushes the compressed output stream.
	 *
	 * If {@link #DeflaterOutputStream(OutputStream, Deflater, int, boolean)
	 * syncFlush} is {@code true} when this compressed output stream is
	 * constructed, this method first flushes the underlying {@code compressor}
	 * with the flush mode {@link Deflater#SYNC_FLUSH} to force
	 * all pending data to be flushed out to the output stream and then
	 * flushes the output stream. Otherwise this method only flushes the
	 * output stream without flushing the {@code compressor}.
	 *
	 * @throws IOException if an I/O error has occurred
	 *
	 * @since 1.7
	 */
	@Override
	public void flush() throws IOException {
		if (syncFlush && !def.finished()) {
			int len = 0;
			while ((len = def.deflate(buf, 0, buf.length, Deflater.SYNC_FLUSH)) > 0)
			{
				out.write(buf, 0, len);
				if (len < buf.length)
					break;
			}
		}
		out.flush();
	}
}
