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

/* Decoder derived from https://developer.mbed.org/users/Digixx/code/SBUS-Library_16channel/file/83e415034198/FutabaSBUS/FutabaSBUS.cpp
 * mbed R/C Futaba SBUS Library
 * Copyright (c) 2011-2012 digixx
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package nl.lxtreme.ols.tool.sbus;


import java.util.*;


/**
 * Class for SBUSDecoder
 * <p>
 * </p>
 * 
 * @author Jacob Miller
 */
public final class SBUSDecoder
{

  // CONSTANTS
  public static final int SBUS_FAILSAFE_INACTIVE = 0;
  public static final int SBUS_FAILSAFE_ACTIVE   = 1;
  public static final int SBUS_STARTBYTE         = 0xF0;
  public static final int SBUS_ENDBYTE           = 0x00;

  // VARIABLES

  private int[]     infoBytes;
  private int[]     channels;
  private int       failsafe;
  private long      goodFrames;
  private long      lostFrames;
  private long      decoderErrorFrames;
  // CONSTRUCTORS

  /**
   * @param aTime
   * @param aEvent
   */
  public SBUSDecoder( final List<Integer> aInfoBytesList )
  {
    if ( aInfoBytesList.size() != 25 )
      throw new java.util.InputMismatchException( "List was not the right size (25)" );

    this.infoBytes = new int[25];
    for ( int i = 0; i < 25; i++ )
      this.infoBytes[i] = aInfoBytesList.get(i).intValue();

    this.channels = new int[18];
    for ( int i = 0; i < 18; i++ )
      this.channels[i] = 0;

    this.goodFrames = 0;
    this.lostFrames = 0;
    this.decoderErrorFrames = 0;
    this.failsafe = SBUS_FAILSAFE_INACTIVE;
  }

  /**
   * @param aTime
   * @param aEvent
   */
  public SBUSDecoder( final int[] aInfoBytes, final int aSize )
  {
    if ( aSize != 25 )
      throw new java.util.InputMismatchException( "List was not the right size (25)" );

    this.infoBytes = new int[25];
    for ( int i = 0; i < 25; i++ )
      this.infoBytes[i] = aInfoBytes[i];

    this.channels = new int[18];
    for ( int i = 0; i < 18; i++ )
      this.channels[i] = 0;

    this.goodFrames = 0;
    this.lostFrames = 0;
    this.decoderErrorFrames = 0;
    this.failsafe = SBUS_FAILSAFE_INACTIVE;
  }

  // METHODS
  
  /**
   * Creates a new SBUSDecoder instance.
   */
  public SBUSDecoder()
  {
    this.infoBytes = new int[25];
    for ( int i = 0; i < 25; i++ )
      this.infoBytes[i] = 0;
    
    this.channels = new int[18];
    for ( int i = 0; i < 18; i++ )
      this.channels[i] = 0;
    
    this.goodFrames = 0;
    this.lostFrames = 0;
    this.decoderErrorFrames = 0;
    this.failsafe = SBUS_FAILSAFE_INACTIVE;
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals( final Object aObject )
  {
    if ( this == aObject )
    {
      return true;
    }
    if ( !super.equals( aObject ) || !( aObject instanceof SBUSDecoder ) )
    {
      return false;
    }

    final SBUSDecoder other = ( SBUSDecoder )aObject;
    if ( this.infoBytes != other.infoBytes )
    {
      return false;
    }

    return true;
  }

  /**
   * Returns whether this data is representing MOSI-data, or MISO-data.
   * 
   * @return the data name, can be <code>null</code>.
   */
  public int[] getInfoBytes()
  {
    return this.infoBytes;
  }
  
  public void setInfoBytes( final List<Integer> aInfoBytesList )
  {
    if ( aInfoBytesList.size() != 25 )
      throw new java.util.InputMismatchException( "List was not the right size (25)" );

    for ( int i = 0; i < 25; i++ )
      this.infoBytes[i] = aInfoBytesList.get(i).intValue();
    
    for ( int i = 0; i < 18; i++ )
      this.channels[i] = 0;
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + this.infoBytes.hashCode();
    return result;
  }

  /**
   * 
   * 
   * @return 
   */
  void process()
  {
    if (this.infoBytes[0] != SBUS_STARTBYTE || this.infoBytes[24] != SBUS_ENDBYTE) {
      //incorrect end byte, out of sync
      this.decoderErrorFrames++;
      return;
    }
    this.goodFrames++;

    channels[0]  = ((this.infoBytes[1]<<3 |this.infoBytes[2]>>5)                          & 0x07FF);
    channels[1]  = ((this.infoBytes[2]<<6 |this.infoBytes[3]>>2)                          & 0x07FF);
    channels[2]  = ((this.infoBytes[3]<<9 |this.infoBytes[4]<<2 | this.infoBytes[5]>>7)   & 0x07FF);
    channels[3]  = ((this.infoBytes[5]<<4 |this.infoBytes[6]>>4)                          & 0x07FF);
    channels[4]  = ((this.infoBytes[6]<<7 |this.infoBytes[7]>>1)                          & 0x07FF);
    channels[5]  = ((this.infoBytes[7]<<10|this.infoBytes[8]<<2 |this.infoBytes[9]>>6)    & 0x07FF);
    channels[6]  = ((this.infoBytes[9]<<5 |this.infoBytes[10]>>3)                         & 0x07FF);
    channels[7]  = ((this.infoBytes[10]<<8|this.infoBytes[11])                            & 0x07FF);
    channels[8]  = ((this.infoBytes[12]<<3|this.infoBytes[13]>>5)                         & 0x07FF);
    channels[9]  = ((this.infoBytes[13]<<6|this.infoBytes[14]>>2)                         & 0x07FF);
    channels[10] = ((this.infoBytes[14]<<9|this.infoBytes[15]<<1|this.infoBytes[16]>>7)   & 0x07FF);
    channels[11] = ((this.infoBytes[16]<<4|this.infoBytes[17]>>4)                         & 0x07FF);
    channels[12] = ((this.infoBytes[17]<<7|this.infoBytes[18]>>1)                         & 0x07FF);
    channels[13] = ((this.infoBytes[18]<<10|this.infoBytes[19]<<2|this.infoBytes[20]>>6)  & 0x07FF);
    channels[14] = ((this.infoBytes[20]<<5|this.infoBytes[21]>>3)                         & 0x07FF);
    
    channels[15] = ((this.infoBytes[21]<<8|this.infoBytes[22])                            & 0x07FF);

    // LSB is received first
    for ( int n=0; n<16; n++ )
      channels[n] = reverseData(channels[n]);
    
    if((this.infoBytes[23] & 0x0080) > 0) 
      this.channels[16] = 2047;
    else
      this.channels[16] = 0;
    
    if((this.infoBytes[23] & 0x0040) > 0)
      this.channels[17] = 2047;
    else
      this.channels[17] = 0;

    if ((this.infoBytes[23] & 0x0020) > 0) {
      lostFrames++;
    }
    
    if ((this.infoBytes[23] & 0x0010) > 0) {
      failsafe = SBUS_FAILSAFE_ACTIVE;
    } else {
      failsafe = SBUS_FAILSAFE_INACTIVE;
    }


  }

  /**
   * @return
   */
  int reverseData( final int data )
  {
    int tmpData = 0;
    int mask = 1;
    for (int i=0x400; i>0; i >>= 1) // 0x400 = 0b10000000000
    {
      if ( (data & mask) > 0 )
      {
        tmpData += i;
      }
      mask <<= 1;
    }
    return tmpData;
  }
  
  
  /**
   * @return
   */
  int getChannel( final int aChannel )
  {
    if (aChannel < 1 || aChannel > 18) {
      return 0;
    } else {
      return this.channels[aChannel - 1];
    }
  }
  
  /**
   * @return
   */
  int getNormalizedChannel( final int aChannel )
  {
    if (aChannel < 1 || aChannel > 18) {
      return 0;
    } else {
      return (int) Math.round(this.channels[aChannel - 1] / 9.92) - 100; //9.92 or 10.24?
    }
  }
  
  /**
   * @return
   */
  int getFailsafeStatus()
  {
    return this.failsafe;
  }
  
  /**
   * @return
   */
  int getFrameLoss()
  {
    return (int) ((this.lostFrames + this.decoderErrorFrames) * 100 / (this.goodFrames + this.lostFrames + this.decoderErrorFrames));
  }
  
  /**
   * @return
   */
  long getGoodFrames()
  {
    return this.goodFrames;
  }
  
  /**
   * @return
   */
  long getLostFrames()
  {
    return this.lostFrames;
  }
  
  /**
   * @return
   */
  long getDecoderErrorFrames()
  {
    return this.decoderErrorFrames;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return "Channels="
        + Arrays.toString( this.channels ) + " failsafe=" + this.failsafe + ", goodFrames=" + this.goodFrames
        + ", lostFrames=" + this.lostFrames + ", decoderErrorFrames=" + this.decoderErrorFrames;
  }
  
}
