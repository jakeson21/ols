/*
 * OpenBench LogicSniffer / SUMP project
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *
 * 
 * Copyright (C) 2010-2011 - J.W. Janssen, http://www.lxtreme.nl
 */
package nl.lxtreme.ols.tool.sbus;
import nl.lxtreme.ols.api.acquisition.*;
import nl.lxtreme.ols.api.data.*;


/**
 * @author jajans
 */
public final class SBUSDataSet extends BaseDataSet<SBUSData>
{
  // CONSTANTS
	  public static final String SBUS_DATA = "DATA"; // raw 12-bit value
	  public static final String SBUS_BYTE = "BYTE"; // 8 bit data within 12-bit byte

  // VARIABLES
	  
	  private int frameCount;
	  
  // CONSTRUCTORS

  /**
   * Creates a new SBUSDataSet instance.
   */
  public SBUSDataSet( final int aStartOfDecode, final int aEndOfDecode, final AcquisitionResult aData )
  {
    super( aStartOfDecode, aEndOfDecode, aData );
    frameCount = 0;
  }

  /** 12 bit byte value
   * @param aTimeValue
   */
  public void reportSBUSData( final int aChannelIdx, final int aStartIdx, final int aEndIdx, final int aDataValue, final int aFrameIndex )
  {
    final int idx = size();
    addData( new SBUSData( idx, aChannelIdx, SBUS_DATA, aDataValue, aStartIdx, aEndIdx, aFrameIndex, -1 ) );
    if ( aFrameIndex > frameCount ) frameCount = aFrameIndex;
  }
  
  /** 8 bit data value from 12 bit byte
   * @param aTimeValue
   */
  public void reportSBUSByte( final int aChannelIdx, final int aStartIdx, final int aEndIdx, final int aDataValue, final int aFrameIndex, final int aByteIndex )
  {
    final int idx = size();
    addData( new SBUSData( idx, aChannelIdx, SBUS_BYTE, aDataValue, aStartIdx, aEndIdx, aFrameIndex, aByteIndex ) );
    if ( aFrameIndex > frameCount ) frameCount = aFrameIndex;
  }
  
  /** 8 bit data value from 12 bit byte
   * @param aTimeValue
   */
  public void reportEvent( final int aIdx, final int aChannelIdx, final String aEvent, final int aSampleIdx, final String aMessage )
  {
    final int idx = size();
    addData( new SBUSData( idx, aChannelIdx, aEvent, aSampleIdx, aMessage ) );
  }
  
  public int getFrameCount()
  {
	  return frameCount;
  }
}
