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


import static nl.lxtreme.ols.util.NumberUtils.*;

import java.awt.*;
import java.beans.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.locks.*;
import java.util.logging.*;
import java.lang.*;

import javax.swing.*;

import nl.lxtreme.ols.api.acquisition.*;
import nl.lxtreme.ols.api.data.*;
import nl.lxtreme.ols.api.data.annotation.AnnotationListener;
import nl.lxtreme.ols.api.tools.*;
import nl.lxtreme.ols.tool.base.annotation.*;
import nl.lxtreme.ols.util.*;
import nl.lxtreme.ols.util.NumberUtils.BitOrder;
import nl.lxtreme.ols.util.analysis.*;


/**
 * Provides a task for analyzing SBUS traces.
 */
public class SBUSAnalyserTask implements ToolTask<SBUSDataSet>
{
  // CONSTANTS
  private static final Logger LOG = Logger.getLogger( SBUSAnalyserTask.class.getName() );
  public static final String PROPERTY_AUTO_DETECT_MODE = "AutoDetectSBUSMode";
  public static final double BIT_PERIOD = 10e-6;
  public static final double HIGH_SPEED_RATE = 6e-3;
  public static final double LOW_SPEED_RATE = 14e-3;
  public static final int BITS_PER_FRAME = 300;
  public static final int START_BYTE = 0xF0; // Actual bits are logic-level inverted
  public static final int END_BYTE = 0x00;   // Actual bits are logic-level inverted
  public static final BitOrder BIT_ORDER = NumberUtils.BitOrder.MSB_FIRST;
  public static final String FRAME_DECODED = "FRAME DECODED";
  public static final String FRAME_LOST = "FRAME LOST";
  
  // VARIABLES
  private final ToolContext context;
  private final ToolProgressListener progressListener;
  private final AnnotationListener annotationListener;
  private final PropertyChangeSupport pcs;

  private int dataIdx;
  private int clockIdx;
  private SBUSMode sbusMode;
  private int bitCount;
  
  // CONSTRUCTORS

  /**
   * Creates a new {@link SBUSAnalyserTask} instance.
   * 
   * @param aContext
   * @param aProgressListener
   */
  public SBUSAnalyserTask( final ToolContext aContext, final ToolProgressListener aProgressListener,
      final AnnotationListener aAnnotationListener )
  {
    this.context = aContext;
    this.progressListener = aProgressListener;
    this.annotationListener = aAnnotationListener;

    this.pcs = new PropertyChangeSupport( this );
    this.dataIdx  = -1;
    this.clockIdx = -1;
    this.bitCount =  12;
  }

  // METHODS

  /**
   * Adds the given property change listener.
   * 
   * @param aListener
   *          the listener to add, cannot be <code>null</code>.
   */
  public void addPropertyChangeListener( final PropertyChangeListener aListener )
  {
    this.pcs.addPropertyChangeListener( aListener );
  }

  /**
   * This is the SBUS protocol decoder core The decoder scans for a decode start
   * event like CS high to low edge or the trigger of the captured data. After
   * this the decoder starts to decode the data by the selected mode, number of
   * bits and bit order. The decoded data are put to a JTable object directly.
   * 
   * @see javax.swing.SwingWorker#doInBackground()
   */
  @Override
  public SBUSDataSet call() throws Exception
  {
    if ( LOG.isLoggable( Level.FINE ) )
    {
      LOG.fine( "datamask   = 0x" + Integer.toHexString( 1 << this.dataIdx ) );
    }

    final int startOfDecode = this.context.getStartSampleIndex();
    final int endOfDecode = this.context.getEndSampleIndex();

    // Initialize the channel labels + clear any existing annotations...
    prepareResults();

    if ( ( this.sbusMode == null ) || ( this.sbusMode == SBUSMode.AUTODETECT ) )
    {
      LOG.log( Level.INFO, "Detecting which SBUS mode is most probably used..." );
      this.sbusMode = detectSBUSMode( startOfDecode, endOfDecode );
    }

    // Notify any listeners of the detected mode...
    this.pcs.firePropertyChange( PROPERTY_AUTO_DETECT_MODE, null, this.sbusMode );

    SBUSDataSet decodedData = new SBUSDataSet( startOfDecode, endOfDecode, this.context.getData() );

    // Check if sample rate was set high enough, ie: Rs >= 200 kHz
    if ( decodedData.getSampleRate() < 200000 )
    {
    	JOptionPane.showMessageDialog(null, 
    			"ERROR: Sample rate is too low. Sample at >= 200 kHz.", 
    			"Sample Rate Error",  
                JOptionPane.ERROR_MESSAGE);
    }
    else
    {
	    // Perform the actual decoding of the data line(s)...
	    LOG.log( Level.INFO, "Calling clockDataOnEdge()" );
	    decodedData = clockDataOnEdge( decodedData );	    	  
    }
    return decodedData;
  }

  /**
   * Removes the given property change listener.
   * 
   * @param aListener
   *          the listener to remove, cannot be <code>null</code>.
   */
  public void removePropertyChangeListener( final PropertyChangeListener aListener )
  {
    this.pcs.removePropertyChangeListener( aListener );
  }

  /**
   * Sets theSBUS Data channel index.
   * 
   * @param aIndex
   *          the index of the "master-out slave-in"/IO0 channel.
   */
  public void setDataIndex( final int aIndex )
  {
    this.dataIdx = aIndex;
  }

  /**
   * Sets theSBUS Data channel index.
   * 
   * @param aIndex
   *          the index of the "master-out slave-in"/IO0 channel.
   */
  public void setClockIndex( final int aIndex )
  {
    this.clockIdx = aIndex;
  }

//  /**
//   * Sets the order in which bits in a SBUS datagram are transmitted.
//   * 
//   * @param aOrder
//   *          the bit order to use, cannot be <code>null</code>.
//   */
//  public void setOrder( final BitOrder aOrder )
//  {
//    this.bitOrder = aOrder;
//  }

  /**
   * Sets which SBUS mode should be used for the analysis process.
   * 
   * @param aMode
   *          the SBUS mode to set, cannot be <code>null</code>.
   */
  public void setSBUSMode( final SBUSMode aMode )
  {
    this.sbusMode = aMode;
  }

  /**
   * Decodes the SBUS-data on a given clock edge.
   * 
   * @param aDataSet
   *          the decoded data to fill;
   * @param aMode
   *          the SBUS mode defining the edges on which data can be sampled and
   *          on which edges data can change.
   */
  private SBUSDataSet clockDataOnEdge( final SBUSDataSet aDataSet)
  {
    final AcquisitionResult data = this.context.getData();
    
    final double sampleRate = data.getSampleRate();
    final double samplesPerBitPeriod = sampleRate * SBUSAnalyserTask.BIT_PERIOD;
    final int dataMask = ( 1 << this.dataIdx ); // IO0
    
    // Convert 
    CapturedData newData = createBitSampledSBUSFrames();
    final int[] values = newData.getValues();
    final long[] times = newData.getTimestamps();
    
    SBUSDataSet aNewDataSet = new SBUSDataSet( 0, values.length-1, newData);
    int datavalue = 0;
    ArrayList<Integer> SBUSbytes = new ArrayList<Integer>();
    SBUSDecoder decoder = new SBUSDecoder();
    int byteNdx = 0;
    int frameCount = 1;
    int lastFrameLossCount = 0;
    int idx=0;
    while (idx < values.length-1)
    {
    	// Reports bits
    	if ( times[idx+1] - times[idx] <= samplesPerBitPeriod )
    	{
    		// Collect into 8E2 bytes: 1 startbit + 8 databit + 1 paritybit + 2 stopbit
    		datavalue = 0;
    		int byteStartIdx = idx;
    		for ( int p=11; p>=0 && idx < values.length-1; p--)
    		{
    		  if ( (values[idx] & dataMask) == 1 ) // accumulate 12-byte values
    		  {
    		    datavalue |= 1 << p;
    		  }
    			idx++;
    		} 
    		SBUSbytes.add(Integer.valueOf((datavalue >> 3) & 0xFF)); // mask off a byte and invert
    		reportData ( aNewDataSet, byteStartIdx, idx, datavalue, datavalue, times, frameCount, byteNdx );
    		byteNdx++;
    		
    		if ( byteNdx==25 ) // Full frame received
    		{
    			frameCount++;
    			// Decode Frame Here
    			decoder.setInfoBytes(SBUSbytes);
    			decoder.process();
    			if ( lastFrameLossCount == decoder.getFrameLoss() )
    			{
    			  aNewDataSet.reportEvent(this.dataIdx, 0, FRAME_DECODED, idx, decoder.toString() );
    			}
    			else
    			{
    			  aNewDataSet.reportEvent(this.dataIdx, 0, FRAME_LOST, idx, decoder.toString() );
    			}
    			SBUSbytes.clear();
    			lastFrameLossCount = decoder.getFrameLoss();
    		}
    	}
    	else
    	{
    		idx++;
    		byteNdx = 0;
    	}
    	
		this.progressListener.setProgress( getPercentage( idx, aNewDataSet.getStartOfDecode(), aNewDataSet.getEndOfDecode() ) );
    	
    	// Reports Byte values
//        if ( ( this.dataIdx >= 0 ) && ( ( values[idx] & dataMask ) != 0 ) )
//        {
//        	datavalue |= ( 1 << bitIdx );
//        }
//        bitIdx--;
//        
//        if ( bitIdx < 0 )
//        {
//    	try {
//	    		this.progressListener.setProgress( getPercentage( idx, aNewDataSet.getStartOfDecode(), aNewDataSet.getEndOfDecode() ) );
//	    		reportData ( aNewDataSet, idx-this.bitCount, idx, datavalue, values[idx] & clockMask, times );
//	    		bitIdx = this.bitCount;
//	    		datavalue = 0;
//	    	} catch(Exception e){
//	    		
//	    	}
//        }
		
    }
    
    return aNewDataSet;
  }

  private CapturedData createBitSampledSBUSFrames()
  {
    final AcquisitionResult data = this.context.getData();
    final int[] values = data.getValues();
    final long[] times = data.getTimestamps();
    final int sampleRate = data.getSampleRate();
    // TODO: check for not integer division
    final int samplesPerBitPeriod = ( int )( sampleRate * SBUSAnalyserTask.BIT_PERIOD );
    final double samplesPerFrame = samplesPerBitPeriod * SBUSAnalyserTask.BITS_PER_FRAME;
    final int dataMask = ( 1 << this.dataIdx ); // IO0

    // CapturedData parameters
    List<Integer> aValues = new ArrayList<Integer>();
    List<Long> aTimestamps = new ArrayList<Long>();
    long aTriggerPosition = 0;
    int aChannels = 255;
    int aEnabledChannels = 0x01 | 0x02; // channel 1=data and 2=clock
    long aAbsoluteLength = 0;
    int bitCounter = 0;
    // TODO: enforce evenly divisible

    int idx = 1;
    int N = values.length;
    // Find First Start of Frame
    while ( idx < N && times[idx] - times[idx - 1] < samplesPerFrame )
    {
      idx++;
    }
    while ( idx < N )
    {
      if ( times[idx] - times[idx - 1] > samplesPerFrame )
      {
        bitCounter = 0;
        idx++; // advance to first bit transition within frame
        if ( idx >= N )
        {
          break;
        }
        while ( idx < N && 
                bitCounter < SBUSAnalyserTask.BITS_PER_FRAME &&
                times[idx] - times[idx - 1] <= samplesPerFrame)
        {
          // min() catches the last 2 stop bits of the last byte
          // round() catches the cases where the sampling was off by a very small amount and was not exactly samplesPerBitPeriod
          long nBits = Math.min( Math.round(((float)times[idx] - times[idx - 1])/samplesPerBitPeriod),
              SBUSAnalyserTask.BITS_PER_FRAME - bitCounter );
          int dataSample = values[idx - 1] ^ 0x00FF; // bit value over the next nBits, inverted
          int dataValue = ( dataSample & dataMask );
          bitCounter += nBits;
          for ( int bit = 0; bit < nBits; bit++ )
          {
            aValues.add( Integer.valueOf( dataValue ) );
            aTimestamps.add( Long.valueOf( times[idx - 1] + samplesPerBitPeriod * bit ) ); // start
                                                                                           // time
          }
          idx++;
        }
        if ( bitCounter < SBUSAnalyserTask.BITS_PER_FRAME )
        {
          long nBits = SBUSAnalyserTask.BITS_PER_FRAME - bitCounter;
          int dataSample = values[idx - 1] ^ 0x00FF; // bit value over the next nBits, inverted
          int dataValue = ( dataSample & dataMask );
          bitCounter += nBits;
          for ( int bit = 0; bit < nBits; bit++ )
          {
            aValues.add( Integer.valueOf( dataValue ) );
            aTimestamps.add( Long.valueOf( times[idx - 1] + samplesPerBitPeriod * bit ) ); // start
          }
          idx++;
        }
        // Add in the an artificial end transition of the last frame bit
        aValues.add( Integer.valueOf(1) ); // inverted low
        aTimestamps.add( Long.valueOf( aTimestamps.get( aTimestamps.size() - 1 ).longValue() + samplesPerBitPeriod ) ); // start
        idx--;
      }
      else
      {
        idx++;
      }
    }

    aAbsoluteLength = aTimestamps.get( aTimestamps.size() - 1 ).longValue();

    // TODO: MEMORY LEAK?
    CapturedData resampledData = new CapturedData( aValues, aTimestamps, aTriggerPosition, sampleRate, aChannels,
        aEnabledChannels, aAbsoluteLength, false );

    return resampledData;
  } 
  
  /**
   * Tries the detect what the clock polarity of the contained data values is.
   * Based on this we can make a "educated" guess what SBUS mode should be used
   * for the decoding of the remainder of data.
   * <p>
   * Currently, there is no way I can think of how the CPHA value can be
   * determined from the data. Hence, we can only determine the clock polarity
   * (CPOL), which also provides a good idea on what mode the SBUS-data is.
   * </p>
   * 
   * @param aStartIndex
   *          the starting sample index to use;
   * @param aEndIndex
   *          the ending sample index to use.
   * @return the presumed SBUS mode, either mode 0 or 2.
   */
	private SBUSMode detectSBUSMode(final int aStartIndex, final int aEndIndex) 
	{
		final AcquisitionResult data = this.context.getData();
		final long[] time = data.getTimestamps();
		final Frequency<Double> valueStats = new Frequency<Double>();

		// Determine the value of the clock line of each sample; the value that
		// occurs the most is probably the default polarity...
		for (int i = aStartIndex; i < aEndIndex-1; i++) {
			final double dt = (double)(time[i+1] - time[i]) / data.getSampleRate();
			if ( dt > SBUSAnalyserTask.BIT_PERIOD * (SBUSAnalyserTask.BITS_PER_FRAME))
				valueStats.addValue(Double.valueOf(dt));
		}

		SBUSMode result;

		// If the clock line's most occurring value is one, then
		// we're fairly sure that CPOL == 1...
		if ( Math.abs( valueStats.getHighestRanked().doubleValue() - SBUSAnalyserTask.LOW_SPEED_RATE ) < 
			 Math.abs( valueStats.getHighestRanked().doubleValue() - SBUSAnalyserTask.HIGH_SPEED_RATE )	) 
		{
			result = SBUSMode.LOW_SPEED;
		} else {
			result = SBUSMode.HIGH_SPEED;
		}
		
		return result;
	}

  /**
   * Determines the channel labels that are used in the annotations and reports
   * and clears any existing annotations on the decoded channels.
   */
  private void prepareResults()
  {
    if ( this.dataIdx >= 0 )
    {
      String label = ( SBUSDataSet.SBUS_DATA );
      this.annotationListener.clearAnnotations( this.dataIdx );
      this.annotationListener.onAnnotation( new ChannelLabelAnnotation( this.dataIdx, label ) );
    }
  }

  /**
   * Reports a set of data-bytes (both MISO and MOSI).
   * 
   * @param aDecodedData
   *          the data set to add the data event(s) to;
   * @param aStartIdx
   *          the starting sample index on which the data started;
   * @param aEndIdx
   *          the ending sample index on which the data ended;
   * @param aDataValue
   *          the DATA data value;
   */
  private void reportData( final SBUSDataSet aDecodedData, final int aStartIdx, final int aEndIdx, 
		  final int aDataValue, final int aDataByteValue, final long[] timestamps, final int aFrameIndex , final int aByteIndex )
  {
      if ( this.dataIdx >= 0 )
      {
        // Perform bit-order conversion on the full byte...
          final int datavalue = NumberUtils.convertBitOrder( aDataValue, ( this.bitCount + 1 ), BIT_ORDER );
//          final int databytevalue = NumberUtils.convertBitOrder( aDataByteValue, ( this.bitCount + 1 ), BIT_ORDER );

        String formatSpec = "0x%1$X";
        if ( Character.isLetterOrDigit( datavalue ) )
        {
          formatSpec = formatSpec.concat( " (%1$c)" );
        }

        this.annotationListener.onAnnotation( new SampleDataAnnotation( this.dataIdx, timestamps[aStartIdx],
        		timestamps[aEndIdx], String.format( formatSpec, Integer.valueOf( datavalue) ) ) );
        aDecodedData.reportSBUSData( this.dataIdx, aStartIdx, aEndIdx, datavalue, aFrameIndex );
        
//        this.annotationListener.onAnnotation( new SampleDataAnnotation( this.clockIdx, timestamps[aStartIdx],
//                timestamps[aEndIdx], String.format( formatSpec, Integer.valueOf( databytevalue ) ) ) );
//        aDecodedData.reportSBUSByte( this.clockIdx, aStartIdx, aEndIdx, databytevalue, aFrameIndex, aByteIndex );
      }
  }
}
