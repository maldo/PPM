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
 * $Id: BitOutputStream.java,v 1.5 2002/05/22 17:11:17 richard Exp $
 */

import java.io.*;

/**
 * An <code>OutputStream</code> for handling unstructured bit data.
 *
 * <p>Unlike "normal" java output streams that assume a byte as the
 * smallest unit of data, this output streams treats its data as an
 * unstructured stream of bits. It provides support to write byte data
 * at any position (not neccessary aligned to a byte boundary) in the
 * stream, as well as utility methods to write primitive data types
 * containing a different number of bits than 8.</p>
 *
 * <p><em>Caution: This class is <strong>not</strong> thread safe. It is
 * assumed that an instance of <code>BitOutputStream</code> is only
 * used by one thread at a time. If you need a thread safe bit output
 * stream, derive from this class and synchronize the
 * <code>writeBits()</code> method. You may also have to synchronize
 * all of the <code>write()</code> methods if the underlying output stream is
 * not already thread safe.</em></p>
 *
 * @author Richard Kunze
 */

public class BitOutputStream extends FilterOutputStream
{
    /** The current bit position in the buffer */
    private int bitsLeft = 0;

    /** The remaining bits that are not yet written to the underlying
     * stream */
    private byte buffer = 0;

    /** temporary buffer for holding data to write to the underlying stream. */
    private byte[] bufferTmp = new byte[8];

    /**
     * Creates a new <code>BitOutputStream</code> instance.
     *
     * @param out an <code>OutputStream</code> value
     * @see FilterOutputStream#FilterOutputStream
     */
    public BitOutputStream(OutputStream out)
    {
    	super(out);
    }
	
    /**
     * Write up to 56 bits to the stream.
     * 
     * <p>The value is written in MSB (most significant bit
     * first) format.</p>
     *
     * @param value the value to write.
     * @param n the number of bits to write. If <code>n</code> is 0,
     * nothing will be written.
     * @exception IndexOutOfBoundsException if <code>n</code> is not in
     * the range of 0 to 56.
     * @exception IOException if something goes wrong writing the
     * data to the underlying stream.
     */
    public void writeBits(long value, int n) throws IOException
    {
		// Note: As this method is called quite frequently, I'm
		// optimizing for speed instead of code readability. Consult
		// your local C, Assembler or other bit fiddling wizard if you
		// have trouble understanding it.
	
		value = (value & (-1L >>> (64-n))) | (((long)buffer) << n);
		n += bitsLeft;
		int remaining = n%8;
		int bytes = n/8;
		switch (bytes) {
		case 8:
		    bufferTmp[0] = (byte)(value >> (56+remaining));
		case 7:
		    bufferTmp[1] = (byte)(value >> (48+remaining));
		case 6:
		    bufferTmp[2] = (byte)(value >> (40+remaining));
		case 5:
		    bufferTmp[3] = (byte)(value >> (32+remaining));
		case 4:
		    bufferTmp[4] = (byte)(value >> (24+remaining));
		case 3:
		    bufferTmp[5] = (byte)(value >> (16+remaining));
		case 2:
		    bufferTmp[6] = (byte)(value >> (8+remaining));
		case 1:
		    bufferTmp[7] = (byte)(value >> remaining);
		    out.write(bufferTmp, 8-bytes, bytes);
		case 0:
		    bitsLeft = remaining;
		    // Store the remaining bits. Don't bother with masking out
		    // the 8-remaining bits in the low order byte of value -
		    // we're simply ignoring them the next time around...
		    buffer = (byte)value;
		    break;
		default:
		    throw new IndexOutOfBoundsException(Integer.toString(n));
		}
    }

    /**
     * Fill the remaining bits up to the next byte boundary with 0.
     * <p>If the stream currently is at a byte boundary, this method
     * will write nothing.</p>
     *
     * @see #countRemainingBits()
     */
    public void padToByteBoundary() throws IOException
    {
    	writeBits(0L, countRemainingBits());
    }

    /**
     * Get the number of remaining bits to the next byte boundary.
     *
     * @return the number of bits remaining. A value between 0
     * and 7.
     */
    public int countRemainingBits()
    {
    	return (8-bitsLeft)%8;
    }

    /**
     * Discard the bits written since the last byte boundary.
     */
    public void discardRemainingBits()
    {
		bitsLeft = 0;
		buffer   = 0;
    }

    /**
     * Check if the current read position is at a byte boundary with
     * regard to the underlying byte stream.
     */
    public boolean isAtByteBoundary() {
    	return bitsLeft == 0;
    }

    /**
     * Write a single bit.
     * @param value the value to write
     * @exception IOException if an IO error occurs
     */
    public void writeBit(boolean value) throws IOException
    {
		writeBits(value?1:0, 1);
    }

    /**
     * Write a single byte.
     * @param value the value to write
     * <p>This is identical to <code>write</code>.</p>
     * @exception IOException if an IO error occurs
     */
    public void writeByte(byte value) throws IOException
    {
    	write(value);
    }
    
    /**
     * Write a 16 bit word in MSB (most significant byte
     * first) order.
     * @param value the value to write
     * @exception IOException if an IO error occurs
     */
    public void writeW16MSB(int value) throws IOException
    {
    	writeBits(value, 16);
    }

    /**
     * Write a 16 bit word in LSB (least significant byte
     * first) order.
     * @param value the value to write
     * @exception IOException if an IO error occurs
     */
    public void writeW16LSB(int value) throws IOException
    {
		// write() ignores the high order bits, so there is no need
		// to mask them out.
		write(value);
		write(value >>> 8);
    }

    /**
     * Write a 32 bit word in MSB (most significant byte
     * first) order.
     * @param value the value to write
     * @exception IOException if an IO error occurs
     */
    public void writeW32MSB(int value) throws IOException
    {
		writeBits(value, 32);
    }

    /**
     * Write a 32 bit word in LSB (least significant byte
     * first) order.
     * @param value the value to write
     * @exception IOException if an IO error occurs
     */
    public void writeW32LSB(int value) throws IOException
    {
		// write() ignores the high order bits, so there is no need
		// to mask them out.
		write(value);
		write(value >>> 8);
		write(value >>> 16);
		write(value >>> 24);
    }

    /**
     * Writes the specified byte to this output stream.
     * @param b the byte to write.
     * @exception IOException if an IO error occurs
     */
    public void write(int b) throws IOException
    {
		if (bitsLeft == 0) out.write(b);
		else writeBits(b, 8);
    }

    /**
     * Writes <code>len</code> bytes from the specified byte array
     * starting at offset <code>off</code> to this output stream.
     * @param b the data.
     * @param off the start offset in the data.
     * @param len the number of bytes to write.
     * @exception IOException  if an I/O error occurs. In particular,
     * an <code>IOException</code> is thrown if the output stream is
     * closed.
     */
    public void write(byte[] b, int off, int len) throws IOException
    {
		// If we're at a byte boundary, we can use the (probably) more
		// efficient method of the underlying stream. If not, we have
		// to go through writeBits().
		if (bitsLeft == 0) out.write(b, off, len);
		else {
		    for (int i=off; i<off+len; i++) writeBits(b[i], 8);
		}
    }

    /**
     * Flushes this output stream and forces any buffered output bytes
     * to be written out.
     *
     * <p>Because the underlying output stream only handles complete
     * bytes, flushing is only possible if the stream is at a byte
     * boundary with respect to the underlying stream. If this is not
     * the case, this method will throw an
     * <code>IllegalStateException</code>.</p>
     * @exception IOException if an  I/O error occurs.
     * @exception IllegalStateException if the stream is not currently
     * at a byte boundary.
     * @see #isAtByteBoundary
     * @see #padAndFlush
     */
    public void flush() throws IOException
    {
		if (bitsLeft != 0) {
		    throw new IllegalStateException(
		      "Not at a byte boundary - cannot flush.");
		}
		super.flush();
    }

    /**
     * Fill the remaining bits up to the next byte boundary with 0 and
     * flush the stream.
     * This convenience method does exactly the same as
     * <code>padToByteBoundary(); flush();</code>.
     * @exception IOException if an  I/O error occurs.
     * @see #flush
     */
    public void padAndFlush() throws IOException
    {
		padToByteBoundary();
		flush();
    }

    /**
     * Closes this output stream and releases any system resources
     * associated with this stream.
     * <p>If the stream is currently not at a byte boundary, the
     * remaining bits are implicitly filled with 0.</p>
     * @exception IOException if an  I/O error occurs.
     */
    public void close() throws IOException
    {
		padToByteBoundary();
		flush();
    }
}