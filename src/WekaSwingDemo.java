import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

// http://www.jfree.org/jfreechart/
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

// http://www.cs.waikato.ac.nz/ml/weka/
import weka.core.converters.ConverterUtils.DataSink;
import weka.core.converters.ConverterUtils.DataSource;
import weka.core.Attribute;
import weka.core.Instances;
import weka.clusterers.Cobweb;
import weka.clusterers.EM;
import weka.clusterers.XMeans;

public class WekaSwingDemo {

	// Parent frame of entire GUI
	private JFrame frame;
	// Persistent scope of JFileChooser means previous user directory choice remains in memory
	private JFileChooser fc = new JFileChooser();
	// mnCluster object must be accessible so that it can be set to enable by
	// mntmOpen or reset button ActionListener methods
	JMenu mnCluster;
	// Weka data holder object
	private Instances instances;
	// Scatterplot data holder object
	private XYSeriesCollection clusters;
	// Top-level generic JFreeChart object
	private JFreeChart chart;
	// Scatterplot Swing component object; inherits from JPanel
	private ChartPanel cp;
	// Source of data (e.g., filename, "Simulated Data", etc.)
	private String dsname;
	
	/**
	* Launch the application.
	*/
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					WekaSwingDemo window = new WekaSwingDemo();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public WekaSwingDemo() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		// Outer frame of entire GUI window
		frame = new JFrame("WekaSwingDemo");
		frame.setBounds(100, 100, 500, 500);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// Drawing panel, where xy Cartesian plot of the data points will go
		clusters = new XYSeriesCollection();
		chart = ChartFactory.createScatterPlot("", "X", "Y", clusters,
				PlotOrientation.VERTICAL, true, true, false);
		cp = new ChartPanel(chart);
		frame.getContentPane().add(cp, BorderLayout.CENTER);
		
		// Reset button on bottom panel
		JPanel buttonPanel = new JPanel();
		JButton reset = new JButton("Reset Cluster IDs");
		reset.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				removeClustAttr();
				reDraw(dsname);
			}
		});
		reset.setToolTipText("Delete cluster ID from memory and reset marker colors to black ");
		buttonPanel.add(reset);
		frame.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		
		// Menu bar
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		
		// File menu (all File menu items will be added immediately below, in sequence)
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		// Set file chooser properties
		fc.setFileFilter(new FileNameExtensionFilter(
				"Weka data files (.csv, .arff, .xrff)", "csv", "arff", "xrff"));
		fc.addChoosableFileFilter(new FileNameExtensionFilter(
				"Comma separated value (.csv)", "csv"));
		fc.addChoosableFileFilter(new FileNameExtensionFilter(
				"Attribute-relation file format (.arff)", "arff"));
		fc.addChoosableFileFilter(new FileNameExtensionFilter(
				"XML attribute relation file format (.xrff)", "xrff"));
		
		// File/Open menu item
		JMenuItem mntmOpen = new JMenuItem("Open");
		mntmOpen.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				// Pop up a file chooser window
				int returnVal = fc.showOpenDialog(frame);
				// If the user selects a file...
				if(returnVal == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					try {
						// Display wait cursor while loading and drawing the file
						frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						// Attempt to open the file
						Instances tmpinst =  DataSource.read(file.getAbsolutePath());
						int na = tmpinst.numAttributes();
						// Check that all columns are numeric; if not then return in failure
						for (int ii=0; ii<na; ii++) {
							if (tmpinst.attribute(ii).isNumeric() != true) {
								JOptionPane.showMessageDialog(frame,
									"File format not recognized; all\nattribute values must be numeric",
									"File Format Error", JOptionPane.ERROR_MESSAGE);
								return;
							}
						}
						// Attempt to recognize the number of attributes, and
						// recognize which one is the cluster attribute
						int ni = tmpinst.numInstances();
						if (na == 3) {
							// Loop through attributes
							for (int ii=(na-1); ii>=0; ii--) {
								// If attribute name contains the word cluster or class,
								// make it the cluster attribute
								String attname = tmpinst.attribute(ii).name();
								if ((attname.indexOf("cluster") != -1) ||
									(attname.indexOf("class") != -1)) {
									tmpinst.setClassIndex(ii);
									break;
								}
								// If above pattern match attempt fails, but there is an
								// attribute consisting of all integer values, make it the
								// cluster attribute
								boolean isallint = true;
								for (int ij=0; ij<ni; ij++) {
									double dval = tmpinst.instance(ij).value(ii);
									if (dval != ((int) dval)) {
										isallint = false;
									}
								}
								if (isallint == true) {
									tmpinst.setClassIndex(ii);
									break;
								}
							}
							// If we have 3 attributes but can't recognize which one is the
							// cluster attribute, return in failure
							if (tmpinst.classIndex() == -1) {
								JOptionPane.showMessageDialog(frame,
									"File format not recognized; data has three attributes\nbut cluster attribute is not recognized",
									"File Format Error", JOptionPane.ERROR_MESSAGE);
								return;
							}
						// Valid numbers of attributes are 2 or 3; if we have more or fewer than
						// this number, return in failure
						} else if (na != 2) {
							JOptionPane.showMessageDialog(frame, "File has " + na +
									" attributes; valid formats are 2 attributes\n(without clusters) or 3 attributes (with clusters included)",
									"File Format Error", JOptionPane.ERROR_MESSAGE);
							return;
						}
						// The tmpinst contents are validated, so copy them over to the actual
						// working copy of the data set
						instances = tmpinst;
						// Take note of the file name (will be used in plot title)
						dsname = file.getName();
						// Draw the data set on the screen
						reDraw(dsname);
						// Now that we have valid data, enable the clustering menu
						mnCluster.setEnabled(true);
					} catch (Exception ex) {
						// Alert user with popup window if file can't be loaded
						JOptionPane.showMessageDialog(frame, "Unable to read file " +
							file.getName(), "File I/O Error", JOptionPane.ERROR_MESSAGE);
					} finally {
						// Transition back from wait cursor to default cursor
						frame.setCursor(Cursor.getDefaultCursor());
					}
				}
			}
		});
		mnFile.add(mntmOpen);
		
		// File/Save_as menu item
		JMenuItem mntmSaveAs = new JMenuItem("Save as");
		mntmSaveAs.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				// Make no assumptions about preferred file name
				fc.setSelectedFile(new File(""));
				// Pop up a file chooser window
				int returnVal = fc.showSaveDialog(frame);
				// If the user selects a file...
				if(returnVal == JFileChooser.APPROVE_OPTION) {
					// Get the file name
					File file = fc.getSelectedFile();
					String fname = file.getName();
					// Get the file extension
					int idx = fname.lastIndexOf(".");
					String ext;
					if (idx >= 0 && idx < fname.length()) {
						ext = fname.substring(idx,fname.length());
					} else {
				    	ext = "";
				    }
				    // If the extension isn't recognized...
				    if ((ext.equals(".csv") == false) &&
				    	(ext.equals(".arff") == false) &&
				    	(ext.equals(".xrff") == false)) {
				    	// Get the current file filter and attempt to append something reasonable
				    	FileFilter ff = fc.getFileFilter();
				    	if (ff == fc.getAcceptAllFileFilter()) {
				    		file = new File(file.getAbsolutePath() + ".arff");
				    	} else {
				    		String[] choosext = ((FileNameExtensionFilter) ff).getExtensions();
				    		if (choosext.length > 1) {
				    			file = new File(file.getAbsolutePath() + ".arff");
				    		} else {
				    			file = new File(file.getAbsolutePath() + "." + choosext[0]);
				    		}
				    	}
				    }
				    try {
				    	// Attempt to save the file
				    	DataSink.write(file.getAbsolutePath(), instances);
				    } catch (Exception ex) {
						// Alert user with popup window if file can't be saved
						JOptionPane.showMessageDialog(frame, "Unable to save file " +
							file.getName(), "File I/O Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});
		mnFile.add(mntmSaveAs);
		
		// File/Exit menu item
		JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				System.exit(0);
			}
		});
		mnFile.add(mntmExit);
		
		// Data menu (all menu items will be added immediately below, in sequence)
		JMenu mnData = new JMenu("Data");
		menuBar.add(mnData);
		
		// Data/Generate menu item
		JMenuItem mntmGenerate = new JMenuItem("Generate");
		mntmGenerate.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				DataGenPane dgp = new DataGenPane();
				int retval = dgp.showDialog(frame);
				if (retval == JOptionPane.OK_OPTION) {
					instances = dgp.getInstances();
					dsname = "Simulated Data";
					reDraw(dsname);
					mnCluster.setEnabled(true);
				}
			}
		});
		mnData.add(mntmGenerate);
		
		// Cluster menu
		mnCluster = new JMenu("Cluster");
		mnCluster.setEnabled(false);
		menuBar.add(mnCluster);
		
		// Cluster/Cobweb menu item
		JMenuItem mntmCobweb = new JMenuItem("Cobweb");
		mntmCobweb.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				Cobweb cw = new Cobweb();
				try {
					// Display wait cursor in case Cobweb takes a while to execute
					frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					// Remove cluster attribute from instances variable
					removeClustAttr();
					// Calculate number of clusters and their defining parameters
					cw.buildClusterer(instances);
					int ni = instances.numInstances();
					int[] clsval = new int[ni];
					// Loop over all of the instances and get the cluster membership of each
					for (int ii=0; ii<ni; ii++) {
						clsval[ii] = cw.clusterInstance(instances.instance(ii));
					}
					// Record the cluster membership of each instance in a new cluster attribute
					addClustAttr(clsval);
					reDraw(String.format("%s with Cobweb", dsname));
					} catch (Exception ex) {
					// Alert user with popup window if clustering can't be completed
					JOptionPane.showMessageDialog(frame,
						"Unable to perform clustering",
						"Expectation Maximization Error", JOptionPane.ERROR_MESSAGE);
				} finally {
					// Transition back from wait cursor to default cursor
					frame.setCursor(Cursor.getDefaultCursor());
				}
			}
		});
		mnCluster.add(mntmCobweb);

		// Cluster/EM menu item
		JMenuItem mntmEm = new JMenuItem("EM");
		mntmEm.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				EM em = new EM();
				try {
					// Display wait cursor as the EM algorithm normally takes a while to execute
					frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					// Remove cluster attribute from instances variable
					removeClustAttr();
					// Calculate number of clusters and their defining parameters
					em.buildClusterer(instances);
					int ni = instances.numInstances();
					int[] clsval = new int[ni];
					// Loop over all of the instances and get the cluster membership of each
					for (int ii=0; ii<ni; ii++) {
						clsval[ii] = em.clusterInstance(instances.instance(ii));
					}
					// Record the cluster membership of each instance in a new cluster attribute
					addClustAttr(clsval);
					reDraw(String.format("%s with Expectation Maximization", dsname));
				} catch (Exception ex) {
					// Alert user with popup window if clustering can't be completed
					JOptionPane.showMessageDialog(frame,
						"Unable to perform clustering",
						"Expectation Maximization Error", JOptionPane.ERROR_MESSAGE);
				} finally {
					// Transition back from wait cursor to default cursor
					frame.setCursor(Cursor.getDefaultCursor());
				}
			}
		});
		mnCluster.add(mntmEm);
		
		// Cluster/XMeans menu item
		JMenuItem mntmXmeans = new JMenuItem("XMeans");
		mntmXmeans.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				XMeans xm = new XMeans();
				xm.setMinNumClusters(2);
				xm.setMaxNumClusters(20);
				try {
					// Display wait cursor in case XMeans takes a while to execute
					frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					// Remove cluster attribute from instances variable
					removeClustAttr();
					// Calculate number of clusters and their defining parameters
					xm.buildClusterer(instances);
					int ni = instances.numInstances();
					int[] clsval = new int[ni];
					// Loop over all of the instances and get the cluster membership of each
					for (int ii=0; ii<ni; ii++) {
						clsval[ii] = xm.clusterInstance(instances.instance(ii));
					}
					// Record the cluster membership of each instance in a new cluster attribute
					addClustAttr(clsval);
					reDraw(String.format("%s with XMeans", dsname));
				} catch (Exception ex) {
					// Alert user with popup window if clustering can't be completed
					JOptionPane.showMessageDialog(frame,
						"Unable to perform clustering",
						"XMeans Error", JOptionPane.ERROR_MESSAGE);
				} finally {
					// Transition back from wait cursor to default cursor
					frame.setCursor(Cursor.getDefaultCursor());
				}
			}
		});
		mnCluster.add(mntmXmeans);
	}
	
	/**
	 * Redraw the data and clusters
	 * 
	 * Inputs:
	 * 
	 *     String ttl:  An overall title for the plot panel
	 *                   
	 * 
	 */
	public void reDraw(String ttl) {
		// Get dimensions and class column index
		int nrows = instances.numInstances();
		int ncols = instances.numAttributes();
		int ic = instances.classIndex();
		int idx[] = new int[2];
		// If file has expected format...
		if ((ncols == 2 && ic == -1) || (ncols == 3 && ic != -1)) {
			// Delete all plots and start fresh
			clusters.removeAllSeries();
			// Declare variables
			Integer c[] = new Integer[nrows];
			double data[] = new double[ncols];
			// If the cluster attribute exists, it should normally be in the
			// third column (ic = 2, indexing from 0), but do make allowances
			// for other possibilities
			switch (ic) {
				case  0: idx[0] = 1; idx[1] = 2; break;
				case  1: idx[0] = 0; idx[1] = 2; break;
				case  2:
				case -1:
				default: idx[0] = 0; idx[1] = 1; break;
			}
			// Get all of the cluster ID numbers, if the cluster attribute is defined
			for (int ii=0; ii<nrows; ii++) {
				data = instances.instance(ii).toDoubleArray();
				if (ic != -1) c[ii] = (int) data[ic];
			}
			// If the cluster attribute is defined...
			if (ic != -1) {
				// Reduce the cluster ID values to a set
				Set<Integer> set = new TreeSet<Integer>(Arrays.asList(c));
				// Loop over all items in the set, and add instances from each cluster to
				// a new JFreeChart XYSeries object
				for (Object object : set) {
					Integer cls = (Integer) object;
					XYSeries series = new XYSeries(String.format("Cluster %d", cls));
					for (int ii=0; ii<nrows; ii++) {
						data = instances.instance(ii).toDoubleArray();
						if ((int) data[ic] == cls) {
							series.add(data[idx[0]], data[idx[1]]);
						}
					}
					clusters.addSeries(series);
				}
			// If no cluster attribute is defined, simply plot as raw data
			} else {
				XYSeries series = new XYSeries("Raw Unclustered Data");
				for (int ii=0; ii<nrows; ii++) {
					data = instances.instance(ii).toDoubleArray();
					series.add(data[idx[0]], data[idx[1]]);
				}
				clusters.addSeries(series);
  			}
  			// Add title
  			chart.setTitle(ttl);
  		// In theory, this code should never execute, as the validation code in
  		// the File/Open actionPerformed() method should ensure that only the first
  		// if clause immediately above this is ever true.  But we leave it in as a
  		// hedge against unforeseen user inputs or logic errors.
  		} else {
  			String errmsg;
  			if (ic == -1) {
				errmsg = "Data set has " + ncols +
					" attributes with no cluster attribute; valid\nformats are 2 attributes (without clusters) or 3 attributes (with clusters)";
			} else {
				errmsg = "Data set has " + ncols +
					" attributes including a cluster attribute; valid\nformats are 2 attributes (without clusters) or 3 attributes (with clusters)";
			}
			JOptionPane.showMessageDialog(frame, errmsg, "Data Set Format Error",
				JOptionPane.ERROR_MESSAGE);
		}   
	}
	
	/*
	 * Delete the cluster attribute from the instances variable
	 */
	public void removeClustAttr() {
		int ic = instances.classIndex(); // Index of cluster attribute
		// If the cluster attribute exists, delete it
		if (ic != -1) {
			instances.setClassIndex(-1);
			instances.deleteAttributeAt(ic);
		}
	}
	
	/*
	 * Create and append a cluster attribute to the instances variable
	 * 
	 * Inputs:
	 * 
	 *      int[] cval:  Array of cluster values, with same length and
	 *                   corresponding one-to-one with instances variable
	 * 
	 */
	public void addClustAttr(int[] cval) {
		// Create the cluster attribute and append it to the instances variable
		Attribute clsatt = new Attribute("cluster");
		instances.insertAttributeAt(clsatt, instances.numAttributes());
		// Figure out which column the cluster attribute is
		int na = instances.numAttributes();
		// Loop through all of the instances and set the value of the cluster attribute
		for (int ii=0; ii<cval.length; ii++) {
			instances.instance(ii).setValue(na-1, cval[ii]);
		}
		instances.setClassIndex(na-1);
	}
}