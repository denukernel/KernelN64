package kerneln64.ui;
import kerneln64.cheats.CheatEngine;
import kerneln64.cheats.CheatRepository;
import kerneln64.cheats.Sm64DecompCodes;
import kerneln64.Main;


import java.awt.*;
import java.awt.datatransfer.*;
import javax.swing.*;

/** Clipboard codes are converted before insertion; the editor always displays native codes. */
public final class CheatCodeInput extends JPanel {
    public final JComboBox<String> addressing=new JComboBox<>(new String[]{"US retail (automatic)","This build"});
    final JLabel feedback=new JLabel("Pasted US codes are converted automatically.");
    public CheatCodeInput(JTextArea area,Sm64DecompCodes build) {
        super(new BorderLayout(4,4));
        JPanel row=new JPanel(new FlowLayout(FlowLayout.LEFT,4,0));row.add(new JLabel("Paste addresses:"));row.add(addressing);add(row,BorderLayout.NORTH);add(feedback);
        addressing.setToolTipText("Choose This build when copying codes that already use this ROM's native addresses.");
        TransferHandler original=area.getTransferHandler();
        area.setTransferHandler(new TransferHandler() {
            @Override public boolean canImport(TransferSupport support){return support.isDataFlavorSupported(DataFlavor.stringFlavor);}
            @Override public boolean canImport(JComponent component,DataFlavor[] flavors){return java.util.Arrays.asList(flavors).contains(DataFlavor.stringFlavor);}
            @Override public boolean importData(TransferSupport support){return insert(support.getTransferable());}
            @Override public boolean importData(JComponent component,Transferable data){return insert(data);}
            private boolean insert(Transferable data) {
                try {
                    if(Main.decompCheats!=build)throw new IllegalArgumentException("The ROM changed; reopen the code editor");
                    String text=(String)data.getTransferData(DataFlavor.stringFlavor);
                    if(text.strip().matches("(?i)[0-9a-f]{1,8}")) {area.replaceSelection(text);feedback.setText("Inserted code fragment; full US code lines convert automatically.");return true;}
                    String converted=addressing.getSelectedIndex()==0?retail(text,build):text;
                    if(addressing.getSelectedIndex()!=0)CheatRepository.validator().setCodes(converted);
                    area.replaceSelection(converted);feedback.setText(addressing.getSelectedIndex()==0?"Pasted US codes converted to this build.":"Pasted native build codes.");return true;
                }catch(Exception error){feedback.setText("Paste failed: "+error.getMessage());feedback.setToolTipText(feedback.getText());return false;}
            }
            @Override public int getSourceActions(JComponent component){return original.getSourceActions(component);}
            @Override public void exportToClipboard(JComponent component,Clipboard clipboard,int action){original.exportToClipboard(component,clipboard,action);}
            @Override public void exportAsDrag(JComponent component,java.awt.event.InputEvent event,int action){original.exportAsDrag(component,event,action);}
        });
    }
    public static String retail(String text,Sm64DecompCodes build) {
        // Decode the existing legacy/XP64 dialect first, using the retail profile.
        CheatEngine retail=new CheatEngine();retail.setSm64Compatibility(true);retail.setCodes(text);
        String codes=retail.diagnosticCodes();
        if(codes.isBlank())throw new IllegalArgumentException("No enabled code lines to paste");
        return build.port(codes);
    }
}
