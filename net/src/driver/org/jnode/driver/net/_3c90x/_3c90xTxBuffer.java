/*
 * $Id$
 */
package org.jnode.driver.net._3c90x;

import org.jnode.net.SocketBuffer;
import org.jnode.net.ethernet.EthernetConstants;
import org.jnode.system.MemoryResource;
import org.jnode.system.ResourceManager;
import org.jnode.vm.VmAddress;

/**
 * @author epr
 */
public class _3c90xTxBuffer implements _3c90xConstants {

	private static final int DPD_SIZE = 16;
	private static final int FRAME_SIZE = EthernetConstants.ETH_FRAME_LEN;

	/** The actual data */
	private final byte[] data;
	/** MemoryResource mapper around data */
	private final MemoryResource mem;
	/** Offset within mem of first DPD */
	private final int firstDPDOffset;
	/** Offset within mem of first ethernet frame */
	private final int firstFrameOffset;
	/** 32-bit address first DPD */
	private final VmAddress firstDPDAddress;
	/** 32-bit address of first ethernet frame */
	private final VmAddress firstFrameAddress;

	/**
	 * Create a new instance
	 * @param rm
	 */
	public _3c90xTxBuffer(ResourceManager rm) {

		// Create a large enough buffer
		final int size = (DPD_SIZE + FRAME_SIZE) + 16 /*alignment*/;
		this.data = new byte[size];
		this.mem = rm.asMemoryResource(data);

		final VmAddress memAddr = mem.getAddress();
		int addr = VmAddress.as32bit(memAddr);
		int offset = 0;
		// Align on 16-byte boundary
		while ((addr & 15) != 0) {
			addr++;
			offset++;
		}

		this.firstDPDOffset = offset;
		this.firstDPDAddress = VmAddress.add(memAddr, firstDPDOffset);
		this.firstFrameOffset = firstDPDOffset + DPD_SIZE;
		this.firstFrameAddress = VmAddress.add(memAddr, firstFrameOffset);
	}

	/**
	 * Initialize this ring to its default (empty) state
	 */
	public void initialize(SocketBuffer src) 
	throws IllegalArgumentException {
		// Setup the DPD
		final int dpdOffset = firstDPDOffset;

		// Copy the data from the buffer
		final int len = src.getSize();
		if (len > FRAME_SIZE) {
			throw new IllegalArgumentException("Length must be <= ETH_FRAME_LEN");
		}
		src.get(data, firstFrameOffset, 0, len);
		// Set next DPD ptr
		mem.setInt(dpdOffset + 0, 0);
		// Set frame start header
		mem.setInt(dpdOffset + 4, 0x8000); // txIndicate
		// Set fragment address
		mem.setInt(dpdOffset + 8, VmAddress.as32bit(firstFrameAddress));
		// Set fragment size
		mem.setInt(dpdOffset + 12, len | (1 << 31));
	}

	/**
	 * Gets the address of the first DPD in this buffer.
	 */
	public VmAddress getFirstDPDAddress() {
		return firstDPDAddress;
	}
}
