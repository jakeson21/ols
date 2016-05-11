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


import static nl.lxtreme.ols.util.ExportUtils.HtmlExporter.*;
import static nl.lxtreme.ols.util.StringUtils.*;
import static nl.lxtreme.ols.util.swing.SwingComponentUtils.*;

import java.awt.*;
import java.beans.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.List;
import java.util.logging.*;

import javax.swing.*;

import nl.lxtreme.ols.api.*;
import nl.lxtreme.ols.api.tools.*;
import nl.lxtreme.ols.api.util.*;
import nl.lxtreme.ols.tool.base.*;
import nl.lxtreme.ols.tool.base.ToolUtils.RestorableAction;
import nl.lxtreme.ols.util.*;
import nl.lxtreme.ols.util.ExportUtils.CsvExporter;
import nl.lxtreme.ols.util.ExportUtils.HtmlExporter;
import nl.lxtreme.ols.util.ExportUtils.HtmlExporter.Element;
import nl.lxtreme.ols.util.ExportUtils.HtmlExporter.MacroResolver;
import nl.lxtreme.ols.util.ExportUtils.HtmlFileExporter;
import nl.lxtreme.ols.util.NumberUtils.BitOrder;
import nl.lxtreme.ols.util.swing.*;
import nl.lxtreme.ols.util.swing.component.*;

import org.osgi.framework.*;


/**
 * The Dialog Class
 *
 * @author Frank Kunz The dialog class draws the basic dialog with a grid
 *         layout. The dialog consists of three main parts. A settings panel, a
 *         table panel and three buttons.
 */
public final class SBUSProtocolAnalysisDialog extends BaseToolDialog<SBUSDataSet> implements ExportAware<SBUSDataSet>,
    PropertyChangeListener
{
  // INNER TYPES

  /**
   * Provides a combobox renderer for {@link BitOrder} enums.
   */
  static class BitOrderItemRenderer extends EnumItemRenderer<BitOrder>
  {
    // CONSTANTS

    private static final long serialVersionUID = 1L;

    // METHODS

  }

  /**
   * Provides a combobox renderer for SBUSMode enums.
   */
  static class SBUSModeRenderer extends EnumItemRenderer<SBUSMode>
  {
    // CONSTANTS

    private static final long serialVersionUID = 1L;

    // METHODS

    /**
     * @see nl.lxtreme.ols.util.swing.component.EnumItemRenderer#getDisplayValue(java.lang.Enum)
     */
    @Override
    protected String getDisplayValue( final SBUSMode aValue )
    {
      switch ( aValue )
      {
      	case AUTODETECT:
          return "Autodetect";
      	case LOW_SPEED:
          return "Low Speed";
        case HIGH_SPEED:
          return "High Speed";

      }
      // Strange, we shouldn't be here...
      LOG.warning( "We should not be here actually! Value = " + aValue );
      return super.getDisplayValue( aValue );
    }

    /**
     * @see nl.lxtreme.ols.util.swing.component.EnumItemRenderer#getToolTip(java.lang.Object)
     */
    @Override
    protected String getToolTip( final Object aValue )
    {
      switch ( ( SBUSMode )aValue )
      {
        case AUTODETECT:
          return "Tries to the SBUS speed based on packet rate.";
        case LOW_SPEED:
          return "SBUS Packet period = 14 ms";
        case HIGH_SPEED:
          return "SBUS Packet period = 7 ms";
      }
      // Strange, we shouldn't be here...
      LOG.warning( "We should not be here actually! Value = " + aValue );
      return super.getToolTip( aValue );
    }
  }

  // CONSTANTS

  private static final long serialVersionUID = 1L;
  private static final Logger LOG = Logger.getLogger( SBUSProtocolAnalysisDialog.class.getName() );

  // VARIABLES

  private JComboBox data;
  private JComboBox mode; // LOW_SPEED analog 14ms, HIGH_SPEED digital 7ms
  private JEditorPane outText;

  private RestorableAction runAnalysisAction;
  private Action exportAction;
  private Action closeAction;

  private SBUSMode detectedSBUSMode;

  // CONSTRUCTORS

  /**
   * Creates a new SBUSProtocolAnalysisDialog instance.
   *
   * @param aOwner
   *          the owner of this dialog;
   * @param aToolContext
   *          the tool context;
   * @param aContext
   *          the OSGi bundle context to use;
   * @param aTool
   *          the {@link SBUSAnalyser} tool.
   */
  public SBUSProtocolAnalysisDialog( final Window aOwner, final ToolContext aToolContext, final BundleContext aContext,
      final SBUSAnalyser aTool )
  {
    super( aOwner, aToolContext, aContext, aTool );

    initDialog();

    setLocationRelativeTo( getOwner() );
  }

  // METHODS

  /**
   * {@inheritDoc}
   */
  @Override
  public void exportToFile( final File aOutputFile, final ExportFormat aFormat ) throws IOException
  {
    if ( ExportFormat.HTML.equals( aFormat ) )
    {
      storeToHtmlFile( aOutputFile, getLastResult() );
    }
    else if ( ExportFormat.CSV.equals( aFormat ) )
    {
      storeToCsvFile( aOutputFile, getLastResult() );
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void propertyChange( final PropertyChangeEvent aEvent )
  {
    final String name = aEvent.getPropertyName();

    if ( SBUSAnalyserTask.PROPERTY_AUTO_DETECT_MODE.equals( name ) )
    {
      SwingComponentUtils.invokeOnEDT( new Runnable()
      {
        @Override
        public void run()
        {
          final Object value = aEvent.getNewValue();

          setAutoDetectSBUSMode( ( SBUSMode )value );
        }
      } );
    }
  }

  /**
   * @see nl.lxtreme.ols.api.Configurable#readPreferences(nl.lxtreme.ols.api.UserSettings)
   */
  @Override
  public void readPreferences( final UserSettings aSettings )
  {
    // Issue #114: avoid setting illegal values...
    setComboBoxIndex( this.data, aSettings, "data" );

    this.mode.setSelectedIndex( aSettings.getInt( "mode", this.mode.getSelectedIndex() ) );

//    // Make sure the settings are reflected in the UI...
//    updateSBUSFIModeSettings( ( SBUSFIMode )this.spifiMode.getSelectedItem() );
  }

  /**
   * @see nl.lxtreme.ols.tool.base.ToolDialog#reset()
   */
  @Override
  public void reset()
  {
    this.outText.setText( getEmptyHtmlPage() );
    this.outText.setEditable( false );

    this.runAnalysisAction.restore();

    setControlsEnabled( true );

    this.exportAction.setEnabled( false );
  }

  /**
   * Sets the auto detected SBUS mode to the given value.
   *
   * @param aMode
   *          the detected SBUS mode, cannot be <code>null</code>.
   */
  public void setAutoDetectSBUSMode( final SBUSMode aMode )
  {
    this.detectedSBUSMode = aMode;
  }

  /**
   * @see nl.lxtreme.ols.api.Configurable#writePreferences(nl.lxtreme.ols.api.UserSettings)
   */
  @Override
  public void writePreferences( final UserSettings aSettings )
  {
    aSettings.putInt( "data", this.data.getSelectedIndex() );
    aSettings.putInt( "mode", this.mode.getSelectedIndex() );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void onToolEnded( final SBUSDataSet aAnalysisResult )
  {
    try
    {
      final String htmlPage;
      if ( aAnalysisResult != null )
      {
        htmlPage = toHtmlPage( null /* aFile */, aAnalysisResult );
        this.exportAction.setEnabled( !aAnalysisResult.isEmpty() );
      }
      else
      {
        htmlPage = getEmptyHtmlPage();
        this.exportAction.setEnabled( false );
      }

      this.outText.setText( htmlPage );
      this.outText.setEditable( false );

      this.runAnalysisAction.restore();
    }
    catch ( final IOException exception )
    {
      // Make sure to handle IO-interrupted exceptions properly!
      if ( !HostUtils.handleInterruptedException( exception ) )
      {
        // Should not happen in this situation!
        throw new RuntimeException( exception );
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void onToolStarted()
  {
    // No-op
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void prepareToolTask( final ToolTask<SBUSDataSet> aToolTask )
  {
    SBUSAnalyserTask toolTask = ( SBUSAnalyserTask )aToolTask;

    toolTask.setDataIndex( this.data.getSelectedIndex() );
    toolTask.setSBUSMode( ( SBUSMode )this.mode.getSelectedItem() );

    // Register ourselves as property change listener...
    toolTask.addPropertyChangeListener( this );
  }

  /**
   * set the controls of the dialog enabled/disabled
   *
   * @param aEnable
   *          status of the controls
   */
  @Override
  protected void setControlsEnabled( final boolean aEnable )
  {
    this.data.setEnabled( aEnable );
    this.mode.setEnabled( aEnable );

    this.closeAction.setEnabled( aEnable );
    this.exportAction.setEnabled( aEnable );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected boolean validateToolSettings()
  {
	  
//    BitSet bitset = new BitSet();
//    bitset.set( this.cs.getSelectedIndex() );
//    bitset.set( this.sck.getSelectedIndex() );
//    int expectedBitCount = 2;
//
//    final SBUSFIMode protocol = ( SBUSFIMode )this.spifiMode.getSelectedItem();
//    if ( SBUSFIMode.DUAL.equals( protocol ) )
//    {
//      // Both MOSI/IO0 & MISO/IO1 should be defined...
//      if ( ( this.mosi.getSelectedIndex() < 1 ) || ( this.miso.getSelectedIndex() < 1 ) )
//      {
//        JErrorDialog.showDialog( getOwner(), "Cannot start analysis!", "Invalid settings detected!",
//            "For dual-mode SBUS, you need to assign both IO0 and IO1." );
//        return false;
//      }
//
//      bitset.set( this.mosi.getSelectedIndex() - 1 );
//      bitset.set( this.miso.getSelectedIndex() - 1 );
//      expectedBitCount += 2;
//    }
//    else if ( SBUSFIMode.QUAD.equals( protocol ) )
//    {
//      // All IO0..3 should be defined...
//      if ( ( this.mosi.getSelectedIndex() < 1 ) || ( this.miso.getSelectedIndex() < 1 ) || //
//          ( this.io2.getSelectedIndex() < 1 ) || ( this.io3.getSelectedIndex() < 1 ) )
//      {
//        JErrorDialog.showDialog( getOwner(), "Cannot start analysis!", "Invalid settings detected!",
//            "For quad-mode SBUS, you need to assign IO0, IO1, IO2 and IO3." );
//        return false;
//      }
//
//      bitset.set( this.mosi.getSelectedIndex() - 1 );
//      bitset.set( this.miso.getSelectedIndex() - 1 );
//      bitset.set( this.io2.getSelectedIndex() - 1 );
//      bitset.set( this.io3.getSelectedIndex() - 1 );
//      expectedBitCount += 4;
//    }
//    else
//    {
//      if ( this.miso.getSelectedIndex() > 0 )
//      {
//        bitset.set( this.miso.getSelectedIndex() - 1 );
//        expectedBitCount++;
//      }
//      if ( this.mosi.getSelectedIndex() > 0 )
//      {
//        bitset.set( this.mosi.getSelectedIndex() - 1 );
//        expectedBitCount++;
//      }
//    }
//
//    if ( bitset.cardinality() != expectedBitCount )
//    {
//      JErrorDialog.showDialog( getOwner(), "Cannot start analysis!", "Invalid settings detected!",
//          "Not all signals are assigned to unique channels." );
//      return false;
//    }

    return true;
  }

  /**
   * Creates the HTML template for exports to HTML.
   *
   * @param aExporter
   *          the HTML exporter instance to use, cannot be <code>null</code>.
   * @return a HTML exporter filled with the template, never <code>null</code>.
   */
  private HtmlExporter createHtmlTemplate( final HtmlExporter aExporter )
  {
    aExporter.addCssStyle( "body { font-family: sans-serif; } " );
    aExporter.addCssStyle( "table { border-width: 1px; border-spacing: 0px; border-color: gray;"
        + " border-collapse: collapse; border-style: solid; margin-bottom: 15px; } " );
    aExporter.addCssStyle( "table th { border-width: 1px; padding: 2px; border-style: solid; border-color: gray;"
        + " background-color: #C0C0FF; text-align: left; font-weight: bold; font-family: sans-serif; } " );
    aExporter.addCssStyle( "table td { border-width: 1px; padding: 2px; border-style: solid; border-color: gray;"
        + " font-family: monospace; } " );
    aExporter.addCssStyle( ".date { text-align: right; font-size: x-small; margin-bottom: 15px; } " );
    aExporter.addCssStyle( ".w100 { width: 100%; } " );
    aExporter.addCssStyle( ".w35 { width: 35%; } " );
    aExporter.addCssStyle( ".w30 { width: 30%; } " );
    aExporter.addCssStyle( ".w15 { width: 15%; } " );
    aExporter.addCssStyle( ".w10 { width: 10%; } " );
    aExporter.addCssStyle( ".w8 { width: 8%; } " );
    aExporter.addCssStyle( ".w7 { width: 7%; } " );

    final Element body = aExporter.getBody();
    body.addChild( H1 ).addContent( "SBUS Analysis results" );
    body.addChild( HR );
    body.addChild( DIV ).addAttribute( "class", "date" ).addContent( "Generated: ", "{date-now}" );

    Element table, tr, thead, tbody;

    table = body.addChild( TABLE ).addAttribute( "class", "w100" );
    tbody = table.addChild( TBODY );
    tr = tbody.addChild( TR );
    tr.addChild( TH ).addAttribute( "colspan", "2" ).addContent( "Configuration" );
    tr = tbody.addChild( TR );
    tr.addChild( TD ).addAttribute( "class", "w30" ).addContent( "SBUS mode" );
    tr.addChild( TD ).addContent( "{detected-sbus-mode}" );

    table = body.addChild( TABLE ).addAttribute( "class", "w100" );
    thead = table.addChild( THEAD );
    tr = thead.addChild( TR );
    tr.addChild( TH ).addAttribute( "class", "w30" ).addAttribute( "colspan", "2" );
    tr.addChild( TH ).addAttribute( "class", "w35" ).addAttribute( "colspan", "4" ).addContent( "DATA" );
    tr = thead.addChild( TR );
    tr.addChild( TH ).addAttribute( "class", "w15" ).addContent( "Index" );
    tr.addChild( TH ).addAttribute( "class", "w15" ).addContent( "Time" );
    tr.addChild( TH ).addAttribute( "class", "w10" ).addContent( "Hex" );
    tr.addChild( TH ).addAttribute( "class", "w10" ).addContent( "Bin" );
    tr.addChild( TH ).addAttribute( "class", "w8" ).addContent( "Dec" );
    tr.addChild( TH ).addAttribute( "class", "w7" ).addContent( "ASCII" );
    tbody = table.addChild( TBODY );
    tbody.addContent( "{decoded-data}" );

    return aExporter;
  }

  /**
   * @return
   */
  private JPanel createPreviewPane()
  {
    final JPanel panTable = new JPanel( new GridLayout( 1, 1, 0, 0 ) );

    this.outText = new JEditorPane( "text/html", getEmptyHtmlPage() );
    this.outText.setEditable( false );

    panTable.add( new JScrollPane( this.outText ) );

    return panTable;
  }

  /**
   * @return
   */
  private JPanel createSettingsPane()
  {
    final int channelCount = getData().getChannels();

    final JPanel settings = new JPanel( new SpringLayout() );

    SpringLayoutUtils.addSeparator( settings, "Settings" );

    settings.add( createRightAlignedLabel( "DATA" ) );
    this.data = SwingComponentUtils.createChannelSelector( channelCount, 0 );
    settings.add( this.data );

    settings.add( createRightAlignedLabel( "SBUS Mode" ) );
    this.mode = new JComboBox( SBUSMode.values() );
    this.mode.setSelectedIndex( 0 );
    this.mode.setRenderer( new SBUSModeRenderer() );
    settings.add( this.mode );

    SpringLayoutUtils.makeEditorGrid( settings, 10, 4 );

    return settings;
  }

  /**
   * Generates an empty HTML page.
   *
   * @return String with HTML data.
   */
  private String getEmptyHtmlPage()
  {
    final HtmlExporter exporter = createHtmlTemplate( ExportUtils.createHtmlExporter() );
    return exporter.toString( new MacroResolver()
    {
      @Override
      public Object resolve( final String aMacro, final Element aParent )
      {
        if ( "date-now".equals( aMacro ) )
        {
          final DateFormat df = DateFormat.getDateInstance( DateFormat.FULL );
          return df.format( new Date() );
        }
        return null;
      }
    } );
  }

  /**
   * Initializes this dialog.
   */
  private void initDialog()
  {
    setMinimumSize( new Dimension( 640, 480 ) );

    final JComponent settingsPane = createSettingsPane();
    final JComponent previewPane = createPreviewPane();

    final JPanel contentPane = new JPanel( new GridBagLayout() );
    contentPane.add( settingsPane, new GridBagConstraints( 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.NORTH,
        GridBagConstraints.NONE, new Insets( 2, 0, 2, 0 ), 0, 0 ) );
    contentPane.add( previewPane, new GridBagConstraints( 1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.NORTH,
        GridBagConstraints.BOTH, new Insets( 2, 0, 2, 0 ), 0, 0 ) );

    final JButton runAnalysisButton = ToolUtils.createRunAnalysisButton( this );
    this.runAnalysisAction = ( RestorableAction )runAnalysisButton.getAction();

    final JButton exportButton = ToolUtils.createExportButton( this );
    this.exportAction = exportButton.getAction();
    this.exportAction.setEnabled( false );

    final JButton closeButton = ToolUtils.createCloseButton();
    this.closeAction = closeButton.getAction();

    final JComponent buttons = SwingComponentUtils.createButtonPane( runAnalysisButton, exportButton, closeButton );

    SwingComponentUtils.setupWindowContentPane( this, contentPane, buttons, runAnalysisButton );
  }

  /**
   * exports the table data to a CSV file
   *
   * @param aFile
   *          File object
   */
  private void storeToCsvFile( final File aFile, final SBUSDataSet aDataSet )
  {
    try
    {
      final CsvExporter exporter = ExportUtils.createCsvExporter( aFile );

      exporter.setHeaders( "index", "start-time", "end-time", "event?", "event-type", "SBUS Data" );

      final List<SBUSData> decodedData = aDataSet.getData();
      for ( int i = 0; i < decodedData.size(); i++ )
      {
        final SBUSData ds = decodedData.get( i );

        final String startTime = Unit.Time.format( aDataSet.getTime( ds.getStartSampleIndex() ) );
        final String endTime = Unit.Time.format( aDataSet.getTime( ds.getStartSampleIndex() ) );
        final String sbusDataValue = ds.isSbusData() ? Integer.toString( ds.getDataValue() ) : null;

        exporter.addRow( Integer.valueOf( i ), startTime, endTime, Boolean.valueOf( ds.isEvent() ), ds.getEventName(),
        		sbusDataValue );
      }

      exporter.close();
    }
    catch ( final IOException exception )
    {
      // Make sure to handle IO-interrupted exceptions properly!
      if ( !HostUtils.handleInterruptedException( exception ) )
      {
        LOG.log( Level.WARNING, "CSV export failed!", exception );
      }
    }
  }

  /**
   * stores the data to a HTML file
   *
   * @param aFile
   *          file object
   */
  private void storeToHtmlFile( final File aFile, final SBUSDataSet aDataSet )
  {
    try
    {
      toHtmlPage( aFile, aDataSet );
    }
    catch ( final IOException exception )
    {
      // Make sure to handle IO-interrupted exceptions properly!
      if ( !HostUtils.handleInterruptedException( exception ) )
      {
        LOG.log( Level.WARNING, "HTML export failed!", exception );
      }
    }
  }

  /**
   * generate a HTML page
   *
   * @param aDataSet
   *          the data set to create the HTML page for, cannot be
   *          <code>null</code>.
   * @return String with HTML data
   */
  private String toHtmlPage( final File aFile, final SBUSDataSet aDataSet ) throws IOException
  {
    final int bitCount = 12; //= Integer.parseInt( ( String )this.bits.getSelectedItem() );
    final int bitAdder = ( ( bitCount % 4 ) != 0 ) ? 1 : 0;
    final int hexPerByte = bitCount/4 + bitAdder;

    final MacroResolver macroResolver = new MacroResolver()
    {
      @Override
      public Object resolve( final String aMacro, final Element aParent )
      {
        if ( "date-now".equals( aMacro ) )
        {
          final DateFormat df = DateFormat.getDateInstance( DateFormat.FULL );
          return df.format( new Date() );
        }
        else if ( "detected-sbus-mode".equals( aMacro ) )
        {
          String result = "<unknown>";
          switch ( SBUSProtocolAnalysisDialog.this.detectedSBUSMode )
          {
            case LOW_SPEED:
              result = "Low Speed";
              break;
            case HIGH_SPEED:
              result = "High Speed";
              break;
            default:
              break;
          }
          return result;
        }
        else if ( "decoded-data".equals( aMacro ) )
        {
          final List<SBUSData> decodedData = aDataSet.getData();
          Element tr;

          for ( int i = 0; i < decodedData.size(); i++ )
          {
            final SBUSData ds = decodedData.get( i );

            if ( ds.isEvent() )
            {
            	// unknown event
            	String bgColor = "#728FCE";
            	String eventName = ds.getEventName();
            	if ( eventName.equals( "FRAME LOST" ))
            	{
            	  bgColor = "#F75D59";
            	}

              tr = aParent.addChild( TR ).addAttribute( "style", "background-color: " + bgColor + ";" );
              tr.addChild( TD ).addContent( String.valueOf( i ) );
              tr.addChild( TD ).addContent( Unit.Time.format( aDataSet.getTime( ds.getStartSampleIndex() ) ) );
              tr.addChild( TD ).addContent( eventName );
              tr.addChild( TD ).addAttribute( "colspan", "3" ).addContent( ds.getDataString() );
//            tr.addChild( TD );
//            tr.addChild( TD );

            }
            else if ( ds.isData() )
            {
              final int sampleIdx = ds.getStartSampleIndex();

              tr = aParent.addChild( TR );
              tr.addChild( TD ).addContent( String.valueOf( i ) );
              tr.addChild( TD ).addContent( Unit.Time.format( aDataSet.getTime( sampleIdx ) ) );

              int dataValue = ds.isSbusData() ? ds.getDataValue() : 0;

              // Try to coalesce equal timestamps...
              if ( ( i + 1 ) < decodedData.size() )
              {
                final SBUSData nextDS = decodedData.get( i + 1 );
                if ( nextDS.getStartSampleIndex() == sampleIdx )
                {
                  dataValue = nextDS.isSbusData() ? nextDS.getDataValue() : dataValue;
                  // Make sure to skip this DS in the next iteration...
                  i++;
                }
              }
              // MOSI value first, MISO value next...
              addDataValues( tr, i, sampleIdx, dataValue );
            }
          }
        }

        return null;
      }

      /**
       * @param aTableRow
       * @param aIdx
       * @param aSampleIdx
       * @param aValue
       */
      private void addDataValues( final Element aTableRow, final int aIdx, final int aSampleIdx, final int aValue )
      {
        aTableRow.addChild( TD ).addContent( "0x", integerToHexString( aValue, hexPerByte ) );
        aTableRow.addChild( TD ).addContent( "0b", integerToBinString( aValue, bitCount ) );
        aTableRow.addChild( TD ).addContent( String.valueOf( aValue ) );
        aTableRow.addChild( TD ).addContent( toASCII( aValue ) );
      }
    };

    if ( aFile == null )
    {
      final HtmlExporter exporter = createHtmlTemplate( ExportUtils.createHtmlExporter() );
      return exporter.toString( macroResolver );
    }
    else
    {
      final HtmlFileExporter exporter = ( HtmlFileExporter )createHtmlTemplate( ExportUtils.createHtmlExporter( aFile ) );
      exporter.write( macroResolver );
      exporter.close();
    }

    return null;
  }
}
