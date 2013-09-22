package de.tivano.flash.swf.common;
/**
 * The contents of this file are subject to the Spark Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the
 * License on the Spark web site
 * (http://www.tivano.de/opensource/flash). 
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific terms governing rights and limitations
 * under the License. 
 *
 * The Initial Developer of Spark is Tivano Software GmbH. The
 * original Spark and portions created by Tivano Software GmbH are
 * Copyright Tivano Software GmbH. All Rights Reserved. 
 *
 * Contributor(s):
 *      Richard Kunze, Tivano Software GmbH.
 *
 * $Id: BitInputStream.java,v 1.7 2002/05/22 17:11:17 richard Exp $
 */

import java.io.*;

/**
 * An <code>InputStream</code> for handling unstructured bit data.
 *
 * <p>Unlike "normal" java input streams that assume a byte as the
 * smallest unit of data, this input streams treats its data as an
 * unstructured stream of bits. It provides support to read byte data
 * from any position (not neccessary aligned to a byte boundary) in
 * the stream, as well as utility methods to read primitive data types
 * containing a different number of bits than 8.</p>
 *
 * <p><em>Caution: This class is <strong>not</strong> thread safe. It is
 * assumed that an instance of <code>BitInputStream</code> is only
 * used by one thread at a time. If you need a thread safe bit input
 * stream, derive from this class and synchronize the
 * <code>readUBits()</code> method. You may also have to synchronize
 * all of the <code>read()</code> methods if the underlying input stream is
 * not already thread safe.</em></p>
 *
 * @author Richard Kunze
 */

public class BitInputStream extends FilterInputStream {

    /** The next few unread bits */
    private long buffer = 0;

    /** The current bit position in the buffer */
    private int bitsLeft = 0;

    /** The bit buffer saved from <code>mark()</code> */
    private long bufferMarkSave = 0;

    /**
     * The current bit position in the buffer saved from
     * <code>mark()</code>
     */
    private int bitsLeftMarkSave = 0;

    /** Temporary buffer for holding data from the underlying stream. */
    private byte[] bufferTmp = new byte[7];

    /**
     * Creates a new <code>BitInputStream</code> instance.
     *
     * @param in an <code>InputStream</code> value
     * @see FilterInputStream#FilterInputStream
     */
    public BitInputStream(InputStream in)
    {
    	super(in);
    }
	
    /**
     * Read up to 56 bits from the stream, interpreted as an unsigned
     * integer.
     * 
     * <p>The value is assumed to be in MSB (most significant bit
     * first) format.  This method blocks until input data is
     * available, the end of the stream is detected, or an exception
     * is thrown.</p>
     *
     * @param n the number of bits to read. If <code>n</code> is 0,
     * nothing will be read and 0 will be returned.
     * @return the value read.
     * @exception IndexOutOfBoundsException if <code>n</code> is not in
     * the range of 0 to 56. In this case, data may be lost and the
     * stream should not be used anymore.
     * @exception EOFException if less than <code>n</code> bits can be
     * read before encountering the end of the stream. Note that you
     * can still read the remaining bits in this case by calling
     * <code>readUBits()</code> again with a lower <code>n</code>. You
     * can get the number of remaining bits from
     * <code>countRemainingBits()</code> 
     * @see #countRemainingBits()
     */
    public long readUBits(int n) throws IOException
    {
		// Note: As this method is called quite frequently, I'm
		// optimizing for speed instead of code readability. Consult
		// your local C, Assembler or other bit fiddling wizard if you
		// have trouble understanding it.
	
		// Try to read as many bytes as necessary
		// The super.read() throws an IndexOutOfBoundsException if n >> 56
		int byteCount = (n + 7 - bitsLeft) >>> 3;
		int bytesRead = super.read(bufferTmp, 0, byteCount);
	
		// Shift as many bytes into the buffer as have been actually read
		int pos = 0;
		switch (bytesRead) {
		case 7:
		    buffer = (buffer << 8) | (bufferTmp[pos++] & 0xFFL);
		case 6:
		    buffer = (buffer << 8) | (bufferTmp[pos++] & 0xFFL);
		case 5:
		    buffer = (buffer << 8) | (bufferTmp[pos++] & 0xFFL);
		case 4:
		    buffer = (buffer << 8) | (bufferTmp[pos++] & 0xFFL);
		case 3:
		    buffer = (buffer << 8) | (bufferTmp[pos++] & 0xFFL);
		case 2:
		    buffer = (buffer << 8) | (bufferTmp[pos++] & 0xFFL);
		case 1:
		    buffer = (buffer << 8) | (bufferTmp[pos++] & 0xFFL);
		case 0:
		    break;
		case -1:
		    // If we're at the end of the underlying stream, simply
		    // return what we've still left in the bit buffer
		    bytesRead = 0;
		    break;
		default:
		    // Paranoia code. This should never happen.
		    throw new Error("Ooops. I seem to have read " + bytesRead +
				    " bytes into a 7 byte array without an " +
				    "Exception. Something's definitely " +
				    "fishy here, please debug...");
		}
	
		// Count the bits we've read from the underlying stream.
		bitsLeft += (bytesRead << 3);
	
		// OK, we're done reading bits. Now check if we've got enough
		// bits to satisfy the request
		if (bytesRead != byteCount)
		    throw new EOFException();
		
		// Calculate the return value, and keep the remaining bits in
		// the buffer. There are never more than 7 bits remaining.
		bitsLeft -= n;
		long retval = buffer >>> bitsLeft;
		long mask   = (0x7F  >>> (7 - bitsLeft));
		buffer &= mask;
	
		return retval;
    }

    /**
     * Read up to 56 bits from the stream, interpreted as a signed
     * number in two's complement.
     *
     * <p>The value is assumed to be in MSB (most significant bit
     * first) format. This method blocks until input data is
     * available, the end of the stream is detected, or an exception
     * is thrown.</p>
     *
     * <p>Note: <code>readSBits(1)</code> returns -1 if the bit is set!</p>
     * @param n the number of bits to read.
     * @return the value read.
     * @exception IllegalArgumentExcetion if <code>n</code> is not in
     * the range of 0 to 56
     * @exception EOFException if less than <code>n</code> bits can be
     * read before encountering the end of the stream. Note that you
     * can still read the remaining bits in this case by calling
     * <code>readUBits()</code> again with a lower <code>n</code>. You
     * can get the number of remaining bits from
     * <code>countRemainingBits()</code>
     * @see #countRemainingBits()
     */
    public long readSBits(int n) throws IOException
    {
    	return signExpand(readUBits(n), n);
    }

    /**
     * Expand the sign of an <code>n</code>-bit value.
     * @param value the value
     * @param n the number of bits
     */
    private long signExpand(long val, int n)
    {
		long wrap = 1L<<(n-1);
		if (val >= wrap) {
		    val -= wrap << 1;
		}
		return val;
    }

    /**
     * Read the remainig bits up to the next byte boundary.
     *
     * <p>If the stream currently is at a byte boundary, this method
     * will read nothing and return 0.</p>
     *
     * <p>Note that this method will <em>not</em> work correctly if an
     * <code>EOFExcpetion</code> has been thrown from a previous call
     * to <code>getUBits()</code> or <code>getSBits()</code>. In this
     * case, it will return the (up to) 8 bits next to the end of the
     * stream, and discard any remaining bits before that.</p>
     *
     * @return a value between 0 and 127, representing the remaining
     * bits to the next byte boundary counted from the start of the
     * stream.
     *
     * @see #countRemainingBits()
     */
    public byte readToByteBoundary()
    {
		byte retval = (byte)buffer;
		buffer = 0;
		bitsLeft = 0;
		return retval;
    }

    /**
     * Get the number of remaining bits to the next byte boundary or
     * to the end of the stream.
     *
     * <p>This method usually returns the number of bits left before
     * the next byte boundary. However, if a previous call to
     * <code>getUBits()</code> or <code>getSBits()</code> resulted in
     * an <code>EOFException</code>, this method returns the total
     * number of bits left on the stream.</p>
     *
     * @return the number of bits remaining. Usually a value between 0
     * and 7, at the end of the stream a value between 0 and 55.
     */
    public int countRemainingBits() {
    	return bitsLeft;
    }

    /**
     * Returns the number of bits that can be read (or skipped over)
     * from this input stream without blocking by the next caller of a
     * method on this input stream.
     * @throws IOException if an I/O error occurs
     */
    public long availableBits() throws IOException
    {
    	return available()*8 + bitsLeft;
    }

    /**
     * Check if the current read position is at a byte boundary with
     * regard to the underlying byte stream.
     */
    public boolean isAtByteBoundary() {
    	return bitsLeft == 0;
    }

    /**
     * Skip the remaining bits up to the next byte boundary.
     * Note that this method will <em>not</em> work correctly if an
     * <code>EOFExcpetion</code> has been thrown from a previous call
     * to <code>getUBits()</code> or <code>getSBits()</code>. In this
     * case, it will skip to the end of the stream.
     * @return the number of bits skipped
     */
    public int skipToByteBoundary()
    {
		int count = countRemainingBits();
		readToByteBoundary();
		return count;
    }

    /**
     * Reads the next byte of data from the input stream.
     *
     * <p>The value byte is returned as an int in the range 0 to
     * 255. If there are less than 7 bits left on the stream, this
     * method reads and returns the remaining bits.
     * If no more data is available because the end of the stream has
     * been reached, the value -1 is returned. This method blocks
     * until input data is available, the end of the stream is
     * detected, or an exception is thrown.</p>
     *
     * @return a value between 0 and 255, or -1 if there is no more data
     * available.
     */
    public int read() throws IOException
    {
		try {
		    return (bitsLeft==0?super.read():(int)readUBits(8));
		} catch (EOFException e) {
		    if (countRemainingBits() > 0) return readToByteBoundary();
		    else return -1;
		}
    }

    /**
     * Read up to <code>len</code> bytes from the stream.
     * <p>If there are less than <code>n*8</code> bits available, the
     * last byte read will hold the value of the remaining bits up to the
     * end of stream</p>.
     * @return the number of bytes read, or -1 if there is no more
     * data available. 
     */
    public int read(byte[] buffer, int off, int len) throws IOException
    {
		// Delegate to super.read() if at byte boundary
		if (bitsLeft==0) return super.read(buffer, off, len);
	
		// Check explictitly for buffer==null to make sure a
		// NullPointerException is thrown even when off==len==0. Needed to
		// conform to the InputStream.read() documentation
		if (buffer==null) throw new NullPointerException();
	
		// Check explicitly for off<0, len<0 and off+len >
		// buffer.length to make sure an IndexOutfBoudsException is
		// thrown and the stream state is not changed. Needed to
		// conform to the InputStream.read() documentation
		if (len<0 || off<0 || off+len > buffer.length) {
		    throw new IndexOutOfBoundsException();
		}
		
		int end = off + len;
		for (int pos = off; pos < end; pos++) {
		    int tmp = read();
		    if (tmp == -1) return pos - off;
		    buffer[pos] = (byte)tmp;
		}
		return len;
    }

    /**
     * Skips over and discards up to <code>n * 8</code> bits of data
     * from the input stream.
     * 
     * <p>This method skips bytes instead of bits for compatibility
     * with "normal" input streams. Use <code>skipBits()</code> for
     * skipping an arbitrary number of bits</p>
     *
     * <p>Note: If this method is used to skip all the way to the end of the
     * stream and the stream is currently not at a byte boundary, the
     * incomplete last byte is not counted, but it <em>is</em> skipped
     * nonetheless.</p>
     *
     * @param n the number of bytes to skip
     * @return the number of bytes actually skipped.
     *
     * @see #skipBits
     */
    public long skip(long n) throws IOException
    {
		final long CHUNK_SIZE = Long.MAX_VALUE / 8;
		long bytesSkipped = 0;
		while (n > CHUNK_SIZE) {
		    long bitsSkipped = skipBits(Long.MAX_VALUE);
		    if (bitsSkipped != Long.MAX_VALUE) {
			return bytesSkipped + bitsSkipped / 8;
		    }
		    n -= CHUNK_SIZE;
		    bytesSkipped += CHUNK_SIZE;
		}
		bytesSkipped += skipBits(n*8) / 8;
		return bytesSkipped;
    }

    /**
     * Skips over and discards up to <code>n</code> bits of data from the
     * input stream.
     * @param n the number of bits to skip.
     * @return the number of bits actually skipped.
     */
    public long skipBits(long n) throws IOException
    {
		// Don't simply go through repeated calls to readBits() for
		// efficiency reasons...
		if (n < 0) return 0;
		
		int buffered  = countRemainingBits();
		if (n < buffered) {
		    // this will always succeed, because there are enough bytes
		    // left in the buffer to not trigger a read on the
		    // underlying stream.
		    readUBits((int)n);
		    return n;
		}
	
		// OK, we've got to really skip bytes on the underlying
		// stream...
		
		long remaining = n;
		// first, skip to the next byte boundary to align the bit
		// stream with the underlying stream
		skipToByteBoundary();
		remaining -= buffered;
	
		// Now, use the underlying stream's skip() method to skip
		// as many entire bytes as possible.
		remaining -= super.skip(remaining/8) * 8;
	
		// Finally, skip as many remaining bits as possible by calling
		// readUBits().
		try {
		    readUBits((int)remaining);
		    remaining = 0;
		} catch (EOFException e) {
		    remaining -= countRemainingBits();
		    skipToByteBoundary();
		} catch (IndexOutOfBoundsException e) {
		    // We're getting here when remaining > 56 holds. In this
		    // case, super.skip() already reached the end of the
		    // stream, so we can simply collect the remaining bits
		    // from the buffer and return. We don't even have to worry
		    // about readUBits() possibling losing some data, because
		    // it's the *purpose* of skip() to lose this data :-)
		    remaining -= countRemainingBits();
		    skipToByteBoundary();
		}
		return n - remaining;
    }

    /**
     * Read a single bit.
     * @exception IOException if an IO error occurs
     * @exception EOFException if not enough data is available
     * @return <code>true</code> if the bit read is 1,
     * <code>false</code> if it is 0.
     */
    public boolean readBit() throws IOException
    {
    	return readUBits(1) != 0;
    }

    /**
     * Read an unsigned byte.
     * @exception IOException if an IO error occurs
     * @exception EOFException if not enough data is available
     * @return a value in the range of 0 through 255
     */
    public int readUByte() throws IOException
    {
    	return (int)readUBits(8);
    }
    
    /**
     * Read an unsigned 16 bit word in MSB (most significant byte
     * first) order.
     * @exception IOException if an IO error occurs
     * @return a value in the range of 0 through 65535
     * @exception EOFException if not enough data is available
     */
    public int readUW16MSB() throws IOException
    {
    	return (int)readUBits(16);
    }

    /**
     * Read an unsigned 16 bit word in LSB (least significant byte
     * first) order.
     * @exception IOException if an IO error occurs
     * @return a value in the range of 0 through 65535
     * @exception EOFException if not enough data is available
     */
    public int readUW16LSB() throws IOException
    {
    	return (int)(readUBits(8) | readUBits(8) << 8);
    }

    /**
     * Read an unsigned 32 bit word in MSB (most significant byte
     * first) order.
     * @exception IOException if an IO error occurs
     * @return a value in the range of 0 through 4294967295
     * @exception EOFException if not enough data is available
     */
    public long readUW32MSB() throws IOException
    {
    	return readUBits(32);
    }

    /**
     * Read an unsigned 32 bit word in LSB (least significant byte
     * first) order.
     * @exception IOException if an IO error occurs
     * @return a value in the range of 0 through 4294967295
     * @exception EOFException if not enough data is available
     */
    public long readUW32LSB() throws IOException
    {
		return readUBits(8) |
		       readUBits(8) << 8  |
		       readUBits(8) << 16 |
		       readUBits(8) << 24;
    }
    
    /**
     * Read a signed byte.
     * @exception IOException if an IO error occurs
     * @exception EOFException if not enough data is available
     * @return a value in the range of -128 through 127
     * @exception EOFException if not enough data is available
     */
    public byte readSByte() throws IOException
    {
    	return (byte)readSBits(8);
    }
    
    /**
     * Read a signed 16 bit word in MSB (most significant byte
     * first) order.
     * @exception IOException if an IO error occurs
     * @return a value in the range of -32768 through 32767
     * @exception EOFException if not enough data is available
     */
    public short readSW16MSB() throws IOException
    {
    	return (short)readSBits(16);
    }

    /**
     * Read a signed 16 bit word in LSB (least significant byte
     * first) order.
     * @exception IOException if an IO error occurs
     * @return a value in the range of -32768 through 32767
     * @exception EOFException if not enough data is available
     */
    public short readSW16LSB() throws IOException
    {
    	return (short)signExpand(readUW16LSB(), 16);
    }

    /**
     * Read a signed 32 bit word in MSB (most significant byte
     * first) order.
     * @exception IOException if an IO error occurs
     * @return a value in the range of -2147483648 through 2147483647
     * @exception EOFException if not enough data is available
     */
    public int readSW32MSB() throws IOException
    {
    	return (int)readSBits(32);
    }

    /**
     * Read a signed 32 bit word in LSB (least significant byte
     * first) order.
     * @exception IOException if an IO error occurs
     * @return a value in the range of -2147483648 through 2147483647
     * @exception EOFException if not enough data is available
     */
    public int readSW32LSB() throws IOException
    {
    	return (int)signExpand(readUW32LSB(), 32);
    }

    /**
     * Marks the current position in this input stream.
     * <p>A subsequent call to the <code>reset()</code> method
     * repositions this stream at the last marked position so that
     * subsequent reads re-read the same bits.</p>
     * @param readlimit the maximum limit of bytes that can be read
     * before the mark position becomes invalid. the limit is given in
     * bytes instead of bits to maintain compatibility with ordinary
     * input streams.
     */
    public void mark(int readlimit)
    {
		super.mark(readlimit);
		// Save the buffer state only if mark is supported on the
		// underlying stream. If not, keep both at the initial value
		// of 0 to ensure that reset() resets to the start of the
		// stream if it does not throw an exception.
		if (markSupported()) {
		    bufferMarkSave   = buffer;
		    bitsLeftMarkSave = bitsLeft;
		}
    }

    /**
     * Repositions this stream to the position at the time the mark
     * method was last called on this input stream.
     *
     * <p>The exact behaviour of <code>reset()</code> depends on how
     * the underlying stream handles <code>reset()</code>. In
     * detail:</p>
     * <ul>
     * <li>If <code>markSupported()</code> returns <code>true</code>
     *   <ul>
     *     <li>If <code>super.reset()</code> throws an
     *        <code>IOException</code>, <code>reset()</code> throws an
     *        <code>IOExpcetion</code>.
     *     </li>
     *     <li>If <code>super.reset()</code> does not throw an
     *         exception, the stream is reset to a state such that all
     *         the bits read since the most recent call to
     *         <code>mark()</code> (or since the start of the stream,
     *         if <code>mark()</code> has not been called) will be
     *         resupplied to subsequent callers of the read method,
     *         followed by any bits that otherwise would have been the
     *         next input data as of the time of the call to
     *         <code>reset()</code>.
     *     </li>
     *   </ul>
     * </li>
     * <li>If <code>markSupported()</code> returns <code>false</code>
     *   <ul>
     *     <li>If <code>super.reset()</code> throws an
     *        <code>IOException</code>, <code>reset()</code> throws an
     *        <code>IOExpcetion</code>.
     *     </li>
     *     <li>If <code>super.reset()</code> does not throw an
     *         exception, the stream is reset to a fixed position
     *         depending on the underlying stream. This fixed position
     *         is always at a byte boundary.
     *     </li>
     *   </ul>
     * </li>
     * </ul>
     * @exception IOException  if this stream has not been marked or
     * if the mark has been invalidated.
     */
    public void reset() throws IOException
    {
		super.reset();
		buffer   = bufferMarkSave;
		bitsLeft = bitsLeftMarkSave;
    }
}
