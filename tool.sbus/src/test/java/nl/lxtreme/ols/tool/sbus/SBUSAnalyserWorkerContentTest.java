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
 * Copyright (C) 2010-2011 J.W. Janssen, www.lxtreme.nl
 */
package nl.lxtreme.ols.tool.sbus;


import static org.junit.Assert.*;

import java.net.*;
import java.util.*;

import nl.lxtreme.ols.api.acquisition.*;
import nl.lxtreme.ols.api.data.annotation.AnnotationListener;
import nl.lxtreme.ols.api.tools.*;
import nl.lxtreme.ols.test.*;
import nl.lxtreme.ols.test.data.*;
import nl.lxtreme.ols.util.NumberUtils.BitOrder;

import org.junit.*;
import org.mockito.*;


/**
 * @author jawi
 */
public class SBUSAnalyserWorkerContentTest
{

  /**
   * @param aDataSet
   * @param aEventName
   * @return
   */
  private static void assertDataEvents( final SBUSDataSet aDataSet, final String aEventName, final int... aExpectedData )
  {
    final Iterator<SBUSData> spiDataIter = aDataSet.getData().iterator();

    int i = 0;
    while ( spiDataIter.hasNext() && ( i < aExpectedData.length ) )
    {
      final SBUSData data = spiDataIter.next();
      if ( aEventName.equals( data.getDataName() ) )
      {
        assertEquals( aExpectedData[i], data.getDataValue() );
        i++;
      }
    }
    assertEquals( "Not all data events were seen?!", aExpectedData.length, i );
  }

  /**
   * Test method for
   * {@link nl.lxtreme.ols.tool.sbus.SBUSAnalyserTask#doInBackground()}.
   */
  @Test
  public void testAnalyzeDataFile1() throws Exception
  {
//    SBUSDataSet result = analyseDataFile( "spi_8bit_4.ols", 8, SBUSMode.MODE_0, BitOrder.MSB_FIRST, false, -1, 1, 0, 3 );
//    assertNotNull( result );
//
//    assertDataEvents( result, SBUSDataSet.SBUS_MOSI, 'T', 'E', 'S', 'T', ' ', 'T', 'E', 'S', 'T', ' ', 'T', 'E', 'S',
//        'T', ' ', 'T', 'E', 'S', 'T', ' ', 'T', 'E', 'S', 'T', ' ', 'T', 'E', 'S', 'T', ' ', 'T', 'E', 'S', 'T', ' ',
//        'T', 'E', 'S', 'T', ' ', 'T', 'E', 'S', 'T', ' ', 'T', 'E', 'S', 'T', ' ', 'T', 'E', 'S' );
  }

  /**
   * Test method for
   * {@link nl.lxtreme.ols.tool.sbus.SBUSAnalyserTask#doInBackground()}.
   */
  @Test
  public void testAnalyzeDataFile2() throws Exception
  {
//    SBUSDataSet result = analyseDataFile( "spi_8bit_1.ols", 8, SBUSMode.MODE_2, BitOrder.LSB_FIRST, true, -1, 1, 3, 2 );
//    assertNotNull( result );
//
//    assertDataEvents( result, SBUSDataSet.SBUS_MOSI, 147, 0, 0, 211, 192, 1, 193, 8, 166, 177, 176 );
  }

  /**
   * Analyses the data file identified by the given resource name.
   * 
   * @param aResourceName
   *          the name of the resource (= data file) to analyse, cannot be
   *          <code>null</code>.
   * @return the analysis results, never <code>null</code>.
   * @throws Exception
   *           in case of exceptions.
   */
  private SBUSDataSet analyseDataFile( final String aResourceName, final int aBitCount, final SBUSMode aMode,
      final BitOrder aBitOrder, final boolean aHonourCS, final int... aChannels ) throws Exception
  {
    URL resource = ResourceUtils.getResource( getClass(), aResourceName );
    AcquisitionResult container = DataTestUtils.getCapturedData( resource );
    ToolContext toolContext = DataTestUtils.createToolContext( container, 0, container.getValues().length - 1 );
//
    ToolProgressListener tpl = Mockito.mock( ToolProgressListener.class );
    AnnotationListener al = Mockito.mock( AnnotationListener.class );
//
    SBUSAnalyserTask worker = new SBUSAnalyserTask( toolContext, tpl, al );
//    worker.setBitCount( aBitCount - 1 );
//    worker.setHonourCS( aHonourCS );
//    worker.setReportCS( false );
//    worker.setSBUSMode( aMode );
//    worker.setOrder( aBitOrder );
//    if ( aChannels.length > 0 )
//    {
//      worker.setIO1Index( aChannels[0] );
//    }
//    if ( aChannels.length > 1 )
//    {
//      worker.setIO0Index( aChannels[1] );
//    }
//    if ( aChannels.length > 2 )
//    {
//      worker.setCSIndex( aChannels[2] );
//    }
//    if ( aChannels.length > 3 )
//    {
//      worker.setSCKIndex( aChannels[3] );
//    }

    return worker.call();
  }
}
