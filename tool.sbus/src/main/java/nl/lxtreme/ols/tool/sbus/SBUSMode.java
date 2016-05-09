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
 * Modes
 * <p>
 *
 *
 * </p>
 * 
 * <pre>
 *
 *
 * </pre>
 */
public enum SBUSMode
{
  /**
   * Tries to auto-detect the SBUS-mode, by determining the time between SBUS 
   * packets: 14ms for LOW_SPEED or 7ms for HIGH_SPEED
   */
  AUTODETECT, //
  /**
   */
  LOW_SPEED, //
  /**
   */
  HIGH_SPEED; //
  
  // METHODS
	
	public static String getModeString(SBUSMode mode)
	{
		String modeStr = null;
		if ( mode == AUTODETECT )
		{
			modeStr = "Unknown";
		}
		else if ( mode == LOW_SPEED )
		{
			modeStr = "Low Speed";
		}
		else
		{
			modeStr = "High Speed";
		}
		return modeStr;
	}
  /**
   * Returns the data change edge, on which the MISO/MOSI lines are allowed to
   * change.
   * 
   * @param aMode
   *          the SBUS mode to return the data change edge for, cannot be
   *          <code>null</code>.
   * @return the data change edge.
   */
  public Edge getDataChangeEdge()
  {
    return getSampleEdge().invert();
  }

  /**
   * Returns the data sample edge, on which the MISO/MOSI lines are to be
   * sampled.
   * 
   * @param aMode
   *          the SBUS mode to return the sample edge for, cannot be
   *          <code>null</code>.
   * @return the sample clock edge.
   */
  public Edge getSampleEdge()
  {
    return Edge.RISING;
  }
}
