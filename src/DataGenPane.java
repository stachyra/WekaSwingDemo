import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class DataGenPane {
        
	private JTextField fNPts     = new JTextField(); // Number of points in each cluster
	private JTextField fXCtrd    = new JTextField(); // X position of centroid
	private JTextField fYCtrd    = new JTextField(); // Y position of centroid
	private JTextField fHStd     = new JTextField(); // Horizontal standard deviation
	private JTextField fVStd     = new JTextField(); // Vertical standard deviation
	private JTextField fRotAngle = new JTextField(); // XY to HV rotation angle
	
	// ArrayList variables to hold the contents of the JTextFields above
	private ArrayList<Integer> npts     = new ArrayList<Integer>();
	private ArrayList<Float>   xctrd    = new ArrayList<Float>();
	private ArrayList<Float>   yctrd    = new ArrayList<Float>();
	private ArrayList<Float>   hstd     = new ArrayList<Float>();
	private ArrayList<Float>   vstd     = new ArrayList<Float>();
	private ArrayList<Float>   rotangle = new ArrayList<Float>();
	
	/*
	 * Show window with 6 JLabels, 6 JTextField inputs, an "autopopulate" button
	 * to fill in the JTextFields with reasonable defaults, and OK/Cancel buttons 
	 */
    public int showDialog(Component parentComponent) {
    	// Create a blank panel with items stacked vertically
    	JPanel panel = new JPanel(new GridLayout(0,1));
    	// Add 6 labels and 6 input fields
    	panel.add(new JLabel("Number of points in each cluster"));
    	panel.add(fNPts);
    	panel.add(new JLabel("Cluster centroid x position"));
    	panel.add(fXCtrd);
    	panel.add(new JLabel("Cluster centroid y position"));
    	panel.add(fYCtrd);
    	panel.add(new JLabel("Horizontal standard deviation"));
    	panel.add(fHStd);
    	panel.add(new JLabel("Vertical standard deviation"));
    	panel.add(fVStd);
    	panel.add(new JLabel("Rotation angle (deg) of std dev relative to xy axes"));
    	panel.add(fRotAngle);
    	// Add a button to fill in the 6 JTextField fields with automatically
    	// generated default values
    	JButton ap = new JButton("AutoPopulate");
    	ap.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent ev) {
				int n = (int) (3 + (8*Math.random())); // Number of clusters
				String tnpts = "";
				String txctrd = "";
				String tyctrd = "";
				String thstd = "";
				String tvstd = "";
				String trotangle = "";
				// Loop through clusters
				for (int ii=0; ii<n; ii++) {
					// Generate reasonable default text for each field
					tnpts = tnpts.concat("100");
					if (ii<(n-1)) tnpts = tnpts.concat(", ");
					txctrd = txctrd.concat(String.format("%.1f", (10*Math.random()-5)));
					if (ii<(n-1)) txctrd = txctrd.concat(", ");
					tyctrd = tyctrd.concat(String.format("%.1f", (10*Math.random()-5)));
					if (ii<(n-1)) tyctrd = tyctrd.concat(", ");
					thstd = thstd.concat(String.format("%.1f", (Math.random()+0.1)));
					if (ii<(n-1)) thstd = thstd.concat(", ");
					tvstd = tvstd.concat(String.format("%.1f", (Math.random()+0.1)));
					if (ii<(n-1)) tvstd = tvstd.concat(", ");
					trotangle = trotangle.concat(String.format("%.1f", (180*Math.random()-90)));
					if (ii<(n-1)) trotangle = trotangle.concat(", ");
				}
				// Add previously generated default text to each input field
				fNPts.setText(tnpts);
				fXCtrd.setText(txctrd);
				fYCtrd.setText(tyctrd);
				fHStd.setText(thstd);
				fVStd.setText(tvstd);
				fRotAngle.setText(trotangle);
			}
    	});
    	// Attach the autopopulate button to the main panel 
    	panel.add(ap);
    	// Becomes true when all values in the 6 JTextFields are found to be valid;
    	// assume false until proven otherwise
    	boolean success = false;
    	int retval = JOptionPane.CANCEL_OPTION;
    	// Keep popping up the panel until the user either provides valid inputs
    	// or preseses the "cancel" button
    	while (success == false) {
    		retval = JOptionPane.showConfirmDialog(parentComponent, panel,
            	 	     "Simulated Data Generation", JOptionPane.OK_CANCEL_OPTION,
            		     JOptionPane.PLAIN_MESSAGE);
    		// Permit the user to exit the loop by pressing the cancel button
    		if (retval == JOptionPane.CANCEL_OPTION) {
    			return retval;
    		}
    		// Attempt to parse user inputs from 6 JTextFields 
    		success = loadInput();
    		// If inputs can't be parsed, show the user an error acknowledgement message
    		if (success == false) {
    			JOptionPane.showMessageDialog(parentComponent,
    				"Inputs must be equal length, comma separated, and numeric only",
    				"Input Error", JOptionPane.ERROR_MESSAGE);
    		}
    	}
        return retval;
    }
    
    /*
     * Attempt to read in and validate the inputs in the 6 JTextFields
     */
    private boolean loadInput() {
    	// Get text in JTextField and split it into comma separated tokens
    	String[] tokens = fNPts.getText().split(",");
    	// Translate all tokens to integer values, or else return failure
    	for (String tok:tokens) {
    		try {
    			npts.add(Integer.valueOf(tok.trim()));
    		} catch(Exception ex) {
    			reZeroParams();
    			return false;
    		}
    	}
    	// Similar to previous: read in text in JTextField as comma separated
    	// tokens, then replace with float values or else return failure
    	tokens = fXCtrd.getText().split(",");
    	for (String tok:tokens) {
    		try {
    			xctrd.add(Float.valueOf(tok));
    		} catch(Exception ex) {
    			reZeroParams();
    			return false;
    		}
    	}
    	tokens = fYCtrd.getText().split(",");
    	for (String tok:tokens) {
    		try {
    			yctrd.add(Float.valueOf(tok));
    		} catch(Exception ex) {
    			reZeroParams();
    			return false;
    		}
    	}
    	tokens = fHStd.getText().split(",");
    	for (String tok:tokens) {
    		try {
    			hstd.add(Float.valueOf(tok));
    		} catch(Exception ex) {
    			reZeroParams();
    			return false;
    		}
    	}
    	tokens = fVStd.getText().split(",");
    	for (String tok:tokens) {
    		try {
    			vstd.add(Float.valueOf(tok));
    		} catch(Exception ex) {
    			reZeroParams();
    			return false;
    		}
    	}
    	tokens = fRotAngle.getText().split(",");
    	for (String tok:tokens) {
    		try {
    			rotangle.add(Float.valueOf(tok));
    		} catch(Exception ex) {
    			reZeroParams();
    			return false;
    		}
    	}
    	// Make sure all fields have equal number of tokens, otherwise return failure
        int n = npts.size();
        if ((xctrd.size() != n) ||
            (yctrd.size() != n) ||
            (hstd.size() != n) ||
            (vstd.size() != n) ||
            (rotangle.size() != n)) {
        	    return false;
        }
    	return true;
    }
    
    /*
     * Reinitialize all 6 JTextField values to blanks
     */
    private void reZeroParams() {
    	npts.clear();
    	xctrd.clear();
    	yctrd.clear();
    	hstd.clear();
    	vstd.clear();
    	rotangle.clear();
    }
    
    /* 
     * Translate 6 JTextField user inputs to Weka instances
     */
    public Instances getInstances() {
    	// Create attributes labeled "x" and "y"
    	Attribute x = new Attribute("x");
    	Attribute y = new Attribute("y");
    	FastVector fvAttInfo = new FastVector();
    	fvAttInfo.addElement(x);
    	fvAttInfo.addElement(y);
    	int ntot = 0; // Total number of data points or instances
    	// Loop through all clusters and count up total number of points from
    	// all clusters
    	for (int ii=0; ii<npts.size(); ii++) {
    		ntot += npts.get(ii);
    	}
    	// Initialize a type of Weka-specific array so that it will be sufficiently
    	// large enough to hold all data from all clusters
    	Instances data = new Instances("Simulated Data", fvAttInfo, ntot);
    	Random ran = new Random();
    	// Loop through clusters
    	for (int ii=0; ii<npts.size(); ii++) {
    		// Calculate sine and cosine of ellipsoid rotation angle
    		float ca = (float) Math.cos(rotangle.get(ii) * Math.PI / 180);
    		float sa = (float) Math.sin(rotangle.get(ii) * Math.PI / 180);
    		// Loop through number of points within each cluster
    		for (int ij=0; ij<npts.get(ii); ij++) {
    			// Generate two separate distributions of Gaussian distributed points,
    			// with standard deviations that are the same for all points within the
    			// same cluster, but different for points in different clusters
    			float hran = hstd.get(ii) * (float) ran.nextGaussian();
    			float vran = vstd.get(ii) * (float) ran.nextGaussian();
    			// Rotate Gaussian distributed points by random rotation angle.
    			// with one angle per cluster
    			float xran = ca * hran - sa * vran;
    			float yran = sa * hran + ca * vran;
    			// Add random horizontal and vertical offsets, one offset per cluster
    			float xpos = xctrd.get(ii) + xran;
    			float ypos = yctrd.get(ii) + yran;
    			// Create empty instance with two attribute values
    			Instance tmp = new Instance(2);
    			// Add artifically generated values to empty instance
    			tmp.setValue(x, xpos);
    			tmp.setValue(y, ypos);
    			// Add newly populated instance to the main Weka data object created
    			// initially at the top of this function
    			data.add(tmp);
    		}
    	}
    	// Return the data object to the user
    	return data;
    }
    
    /*
     * Test method to check that other methods are all functioning correctly
     */
    public static void main(String[] args) {
    	DataGenPane dgp = new DataGenPane();
		int retval = dgp.showDialog(null);
		if (retval == JOptionPane.OK_OPTION) {
			Instances instances = dgp.getInstances();
			int nrows = instances.numInstances();
			int ncols = instances.numAttributes();
			System.out.println(String.format("A total of %d instances have been generated:",
				nrows));
			double data[] = new double[ncols];
			for (int ii=0; ii<nrows; ii++) {
				data = instances.instance(ii).toDoubleArray();
				System.out.println(String.format("%4d:  %7.3f  %7.3f", ii+1, data[0],
					data[1]));
			}
		} else {
			System.out.println("User hit the cancel button!");
		}
    }
}