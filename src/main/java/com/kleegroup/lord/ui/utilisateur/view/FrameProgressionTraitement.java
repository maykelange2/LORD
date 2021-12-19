package com.kleegroup.lord.ui.utilisateur.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.util.ResourceBundle;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import com.kleegroup.lord.moteur.Fichier;
import com.kleegroup.lord.moteur.util.IHierarchieSchema;


/**
 * Classe qui affiche la progression du traitement.
 */
public class FrameProgressionTraitement extends javax.swing.JPanel {

    /**
     * 
     */
    private static final long serialVersionUID = 6322149529433749422L;

    private javax.swing.JLabel jLblTotalProgress;

    private javax.swing.JLabel jLblFileProgress;

    private javax.swing.JProgressBar jProgressFile;

    private javax.swing.JProgressBar jProgressTotal;

    private javax.swing.JTable jTblEtatFichiers;

    private javax.swing.JPanel jPanel1;

    private final ResourceBundle resourceMap = ResourceBundle
	    .getBundle("resources." + getClass().getSimpleName());

    /** Creates new form NewJPanel. */
    public FrameProgressionTraitement() {
	initComponents();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    private void initComponents() {

	jTblEtatFichiers = new javax.swing.JTable();
	jLblTotalProgress = new javax.swing.JLabel();
	jLblFileProgress = new javax.swing.JLabel();
	jProgressTotal = new javax.swing.JProgressBar();
	jProgressFile = new javax.swing.JProgressBar();
	jPanel1 = new JPanel();
	

	jTblEtatFichiers.setDefaultRenderer(Object.class,
		new CustomTableCellRenderer());

	jTblEtatFichiers.setName("jTblEtatFichiers"); // NOI18N
	jTblEtatFichiers.setShowHorizontalLines(false);
	jTblEtatFichiers.setShowVerticalLines(false);
	jTblEtatFichiers.setBorder(new LineBorder(java.awt.Color.BLACK));
	jTblEtatFichiers.setIntercellSpacing(new Dimension(10, 0));

	jLblTotalProgress.setText(resourceMap.getString("jLabel1.text")); // NOI18N
	jLblTotalProgress.setName("jLabel1"); // NOI18N

	jLblFileProgress.setText(resourceMap.getString("jLabel2.text")); // NOI18N
	jLblFileProgress.setName("jLabel2"); // NOI18N

	jProgressTotal.setValue(0);
	jProgressTotal.setName("jProgresGlobal"); // NOI18N
	jProgressTotal.setStringPainted(true);

	jProgressFile.setName("jProgresFichier"); // NOI18N
	jProgressFile.setStringPainted(true);

	//jTblEtatFichiers.set

	createLayout();

    }

    private void createLayout() {
	setLayout(new java.awt.GridBagLayout());
	jPanel1.setLayout(new java.awt.GridBagLayout());
	GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridx = 1;
	gridBagConstraints.gridy = 1;
	gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
	gridBagConstraints.weightx = 1.0;
	gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
	gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
	jPanel1.add(jProgressFile, gridBagConstraints);

	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridx = 0;
	gridBagConstraints.gridy = 0;
	gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
	gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
	jPanel1.add(jLblTotalProgress, gridBagConstraints);

	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridx = 0;
	gridBagConstraints.gridy = 1;
	gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
	gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
	jPanel1.add(jLblFileProgress, gridBagConstraints);

	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridx = 1;
	gridBagConstraints.gridy = 0;
	gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
	gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
	gridBagConstraints.weightx = 1.0;
	gridBagConstraints.insets = new java.awt.Insets(2, 5, 2, 5);
	jPanel1.add(jProgressTotal, gridBagConstraints);

	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridx = 0;
	gridBagConstraints.gridy = 0;
	gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
	gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
	add(jPanel1, gridBagConstraints);

	gridBagConstraints = new java.awt.GridBagConstraints();
	gridBagConstraints.gridx = 0;
	gridBagConstraints.gridy = 1;
	gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
	gridBagConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
	gridBagConstraints.weightx = 1.0;
	gridBagConstraints.weighty = 1.0;
	gridBagConstraints.insets = new java.awt.Insets(5, 5, 5, 5);
	add(jTblEtatFichiers, gridBagConstraints);
    }

    /**
     * @param value la progression du fichier en cours. (entre 0 et 100).
     */
    public void setFileProgressValue(int value) {
	jProgressFile.setValue(value);
    }

    /**
     * @param value la progression totale. (entre 0 et 100).
     */
    public void setTotalProgressValue(int value) {
	jProgressTotal.setValue(value);
    }

    /**
     * @param tm le modèle de la table (liste des fichiers+etat).
     */
    public void setTableModel(TableModel tm) {
	jTblEtatFichiers.setModel(tm);
    }

    /**
     * @param etat si true affiche les barres de progression.
     */
    public void setVisibleBarresProgression(boolean etat) {
	jPanel1.setVisible(etat);
    }
    
    static class CustomTableCellRenderer extends DefaultTableCellRenderer {
	    private static final long serialVersionUID = 1L;

	    @Override
	    public Component getTableCellRendererComponent(JTable table,
		    Object valeur, boolean isSelected, boolean hasFocus,
		    int row, int column) {
		final Object aAfficher =  table.getModel().getValueAt(row, 0);
		if (aAfficher instanceof IHierarchieSchema){
		if (((IHierarchieSchema)aAfficher).isFichier()){
		    final Fichier f = (Fichier) aAfficher;
		    final String msg = valeur.toString();
		    if (f.getEtatFichier() == Fichier.ETAT.EN_COURS_DE_VERIFICATION) {
			final javax.swing.JLabel lbl = new javax.swing.JLabel();
			final Font oldFont = lbl.getFont();
			lbl.setFont(new Font(oldFont.getName(), Font.BOLD,
				oldFont.getSize() + 2));
			lbl.setText(msg);
			return lbl;
		    }
		}else if (((IHierarchieSchema)aAfficher).isCategorie()){
		    final Component c=super.getTableCellRendererComponent(table, valeur,
				isSelected, hasFocus, row, column);
		    c.setFont(new Font(getFont().getName(), Font.BOLD,
				getFont().getSize() + 4));
		    return c;
		}
		}
		
		return super.getTableCellRendererComponent(table, valeur,
			isSelected, hasFocus, row, column);
	    }

	}
}
