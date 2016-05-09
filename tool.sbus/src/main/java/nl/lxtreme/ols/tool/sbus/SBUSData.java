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


import nl.lxtreme.ols.api.data.*;


/**
 * Class for SBUS dataset
 * <p>
 * A SBUS dataset consists of sample indexes, MISO/MOSI values, or it can have an
 * SBUS event.
 * </p>
 * 
 * @author Frank Kunz
 * @author J.W. Janssen
 */
public final class SBUSData extends BaseData<SBUSData>
{
  // VARIABLES

  private final int dataValue;
  private final String dataName;
  private final int frameIndex;
  private final int byteIndex;
  private final String dataString;

  // CONSTRUCTORS

  /**
   * @param aTime
   * @param aEvent
   */
  public SBUSData( final int aIdx, final int aChannelIdx, final String aEvent, final int aSampleIdx, final String aDataString )
  {
    super( aIdx, aChannelIdx, aSampleIdx, aEvent );
    this.dataValue = 0;
    this.dataName = null;
    this.frameIndex = -1;
    this.byteIndex = -1;
    this.dataString = aDataString;
  }

  /**
   * @param aTime
   * @param aMoSiValue
   * @param aMiSoValue
   */
  public SBUSData( final int aIdx, final int aChannelIdx, final String aDataName, final int aDataValue,
      final int aStartSampleIdx, final int aEndSampleIdx, final int aFrameIdx, final int aByteIndex )
  {
    super( aIdx, aChannelIdx, aStartSampleIdx, aEndSampleIdx );
    this.dataName = aDataName;
    this.dataValue = aDataValue;
    this.frameIndex = aFrameIdx;
    this.byteIndex = aByteIndex;
    this.dataString = null;
  }

  // METHODS

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
    if ( !super.equals( aObject ) || !( aObject instanceof SBUSData ) )
    {
      return false;
    }

    final SBUSData other = ( SBUSData )aObject;
    if ( this.dataValue != other.dataValue )
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
  public String getDataName()
  {
    return this.dataName;
  }

  /**
   * @return the MISO/MOSI data value.
   */
  public final int getDataValue()
  {
    return this.dataValue;
  }

  /**
   * @return the index into Frame of this value
   */
  public final int getFrameIndex()
  {
    return this.frameIndex;
  }

  /**
   * @return the index into Frame of this value
   */
  public final int getByteIndex()
  {
    return this.byteIndex;
  }

  /**
   * Returns the current value of dataString.
   * @return the dataString
   */
  public String getDataString()
  {
    return dataString;
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + this.dataValue;
    return result;
  }

  /**
   * @return
   */
  public boolean isData()
  {
    return ( this.dataName != null ) && !this.dataName.trim().isEmpty();
  }

  /**
   * @return
   */
  public final boolean isSbusData()
  {
    return SBUSDataSet.SBUS_DATA.equals( this.dataName );
  }

  /**
   * @return
   */
  public final boolean isSbusByte()
  {
    return SBUSDataSet.SBUS_BYTE.equals( this.dataName );
  }

//  /**
//   * @return
//   */
//  public final boolean isMosiData()
//  {
//    return SBUSDataSet.SBUS_MOSI.equals( this.dataName );
//  }
}
