
package fiji.plugin.trackmate.action.oneat;

import static fiji.plugin.trackmate.detection.DetectorKeys.KEY_TARGET_CHANNEL;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_SPLITTING_MAX_DISTANCE;
import static fiji.plugin.trackmate.tracking.TrackerKeys.KEY_GAP_CLOSING_MAX_FRAME_GAP;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;


import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;

import static fiji.plugin.trackmate.gui.Fonts.SMALL_FONT;

public class OneatExporterPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private static File oneatdivisionfile;
	private  static File oneatapoptosisfile;

	private  int detchannel = 1;
	private  double sizeratio = 0.75;
	private double linkdist = 250;
	private  int deltat = 10;
	private  int tracklet = 2;
	private boolean createlinks = true;
	private boolean breaklinks = false;
	
	private JButton Loaddivisioncsvbutton;
	private JButton Loadapoptosiscsvbutton;
	private JFormattedTextField MinTracklet;
	private JFormattedTextField DetectionChannel;
	private JFormattedTextField TimeGap;
	private JFormattedTextField MotherDaughterLinkDist;

	private JCheckBox CreateNewLinks;
	private JCheckBox BreakCurrentLinks;
	
	public OneatExporterPanel(final Settings settings,final Map<String, Object> trackmapsettings, 
			final Map<String, Object> detectorsettings, final Model model) {

		
		
		detchannel = detectorsettings.get(KEY_TARGET_CHANNEL)!=null? (int) detectorsettings.get(KEY_TARGET_CHANNEL): 1;
		linkdist =  trackmapsettings.get(KEY_SPLITTING_MAX_DISTANCE)!=null? (double) trackmapsettings.get(KEY_SPLITTING_MAX_DISTANCE): 250;
		deltat = trackmapsettings.get(KEY_GAP_CLOSING_MAX_FRAME_GAP)!=null? (int) trackmapsettings.get(KEY_GAP_CLOSING_MAX_FRAME_GAP):10;
		
		final GridBagLayout gridBagLayout = new GridBagLayout();
		setLayout( gridBagLayout );

		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.anchor = GridBagConstraints.WEST;
		gbc.insets = new Insets( 5, 5, 5, 5 );
		final JLabel label = new JLabel("Press enter after changing a text box");
		
		gbc.gridx = 0;
		gbc.gridy = 0;
		add(label, gbc);
		gbc.gridy++;

		Loaddivisioncsvbutton = new JButton("Load Oneat mitosis detections From CSV");
		add(Loaddivisioncsvbutton, gbc);
		
        gbc.gridx++;
		Loadapoptosiscsvbutton = new JButton("Load Oneat apoptosis detections From CSV");
		add(Loadapoptosiscsvbutton, gbc);
		gbc.gridx--;
		gbc.gridy++;
		
		final JLabel lblDetectionChannel = new JLabel( "Integer label detection channel (press enter after choosing)::" );
		add( lblDetectionChannel, gbc );
		gbc.gridx++;
		
		
		DetectionChannel = new JFormattedTextField();
		DetectionChannel.setValue(detchannel);
		DetectionChannel.setColumns( 4 );
		DetectionChannel.setFont(new Font("Arial", Font.PLAIN, 10));
		add(DetectionChannel, gbc);
		gbc.gridy++;
		gbc.gridx--;
		
		final JLabel lblMinTracklet = new JLabel( "Minimum length of tracklet:" );
		add( lblMinTracklet, gbc );
		gbc.gridx++;
		
		MinTracklet = new JFormattedTextField();
		MinTracklet.setValue(tracklet);
		MinTracklet.setColumns( 4 );
		MinTracklet.setFont(new Font("Arial", Font.PLAIN, 10));
		add(MinTracklet, gbc);
		
		gbc.gridy++;
		gbc.gridx--;
		
		final JLabel lblTimeGap = new JLabel( "Allowed timegap between oneat & TM events :" );
		add( lblTimeGap, gbc );
		gbc.gridx++;
		
		
		TimeGap = new JFormattedTextField();
		TimeGap.setValue(deltat);
		TimeGap.setColumns( 4 );
		TimeGap.setFont(new Font("Arial", Font.PLAIN, 10));
		add(TimeGap, gbc);
		gbc.gridy++;
		gbc.gridx--;
		
		
		final JLabel lblMotherDaughterLinkDist = new JLabel( "Linking distance between mother & daughters :" );
		add( lblMotherDaughterLinkDist, gbc );
		gbc.gridx++;
		
		MotherDaughterLinkDist = new JFormattedTextField();
		MotherDaughterLinkDist.setValue(linkdist);
		MotherDaughterLinkDist.setFont(new Font("Arial", Font.PLAIN, 10));
		MotherDaughterLinkDist.setColumns(4);
		add(MotherDaughterLinkDist, gbc);
		gbc.gridy++;
		gbc.gridx--;
		
		CreateNewLinks = new JCheckBox("Create new mitosis events (Verified by oneat, missed by TM) ");
		CreateNewLinks.setSelected(createlinks);
		CreateNewLinks.setHorizontalTextPosition(SwingConstants.LEFT);
		add(CreateNewLinks, gbc);
		gbc.gridx++;

		BreakCurrentLinks = new JCheckBox("Break current mitosis events (Labelled by TM, Unverfied by oneat ) ");
		BreakCurrentLinks.setSelected(breaklinks);
		BreakCurrentLinks.setHorizontalTextPosition(SwingConstants.LEFT);
		add(BreakCurrentLinks, gbc);

		Loaddivisioncsvbutton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent a) {

				JFileChooser csvfile = new JFileChooser();
				FileFilter csvfilter = new FileFilter() {
					// Override accept method
					public boolean accept(File file) {

						// if the file extension is .log return true, else false
						if (file.getName().endsWith(".csv")) {
							return true;
						}
						return false;
					}

					@Override
					public String getDescription() {

						return null;
					}
				};

				csvfile.setCurrentDirectory(new File(settings.imp.getOriginalFileInfo().directory));
				csvfile.setDialogTitle("Mitosis Detection file");
				csvfile.setFileSelectionMode(JFileChooser.FILES_ONLY);
				csvfile.setFileFilter(csvfilter);

				int showparent = csvfile.showOpenDialog(getParent());
				if (showparent == JFileChooser.APPROVE_OPTION)

					oneatdivisionfile = new File(csvfile.getSelectedFile().getPath());
				
				if (showparent == JFileChooser.CANCEL_OPTION)
					oneatdivisionfile = null;
				
			}

		});

		Loadapoptosiscsvbutton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent a) {

				JFileChooser csvfile = new JFileChooser();
				FileFilter csvfilter = new FileFilter() {
					// Override accept method
					public boolean accept(File file) {

						// if the file extension is .log return true, else false
						if (file.getName().endsWith(".csv")) {
							return true;
						}
						return false;
					}

					@Override
					public String getDescription() {

						return null;
					}
				};

				csvfile.setCurrentDirectory(new File(settings.imp.getOriginalFileInfo().directory));
				csvfile.setDialogTitle("Apoptosis Detection file");
				csvfile.setFileSelectionMode(JFileChooser.FILES_ONLY);
				csvfile.setFileFilter(csvfilter);
				int showparent = csvfile.showOpenDialog(getParent());
				if (showparent == JFileChooser.APPROVE_OPTION)

					oneatapoptosisfile = new File(csvfile.getSelectedFile().getPath());
				
				if (showparent == JFileChooser.CANCEL_OPTION)
					oneatapoptosisfile = null;
				
			}

		});
		
		DetectionChannel.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				detchannel = ( ( Number ) DetectionChannel.getValue() ).intValue();
				System.out.println(detchannel + " " + "action" );
			}
		});
		
		
		
		
		
		CreateNewLinks.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				
				if (e.getStateChange() == ItemEvent.SELECTED)
					  createlinks = true;
				if (e.getStateChange() == ItemEvent.DESELECTED)
					  createlinks = false;
				
			}
		});
		
		BreakCurrentLinks.addItemListener(new ItemListener() {
			
			@Override
			public void itemStateChanged(ItemEvent e) {
				
				if (e.getStateChange() == ItemEvent.SELECTED)
					  breaklinks = true;
				if (e.getStateChange() == ItemEvent.DESELECTED)
					  breaklinks = false;
			}
		});
		
		
		
		MinTracklet.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				tracklet = ((Number) MinTracklet.getValue()).intValue();
				
			}
		});
		
		
		TimeGap.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				deltat = ((Number) TimeGap.getValue()).intValue();
				
			}
		});
		
		
		MotherDaughterLinkDist.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				
				linkdist = ( ( Number ) MotherDaughterLinkDist.getValue() ).doubleValue();
				
			}
		});
		
		
	}

	public double getLinkDist() {
		
		
		return linkdist;
	}
	
	
	public int getDetectionChannel() {
		
		
		return detchannel;
	}
	
	public double getSizeRatio() {
		
		
		return sizeratio;
	}
	
	public int getMinTracklet() {
		
		return tracklet;
	}
	
	public boolean getBreakLinks() {
		
		return breaklinks;
	}
	
	public boolean getCreateLinks() {
		
		return createlinks;
	}
	
	public int getTimeGap() {
		
		return deltat;
	}
	
	public File getMistosisFile() {
		
		
		return oneatdivisionfile;
	}
	 
	
	public File getApoptosisFile() {
		
		return oneatapoptosisfile;
	}
	




}