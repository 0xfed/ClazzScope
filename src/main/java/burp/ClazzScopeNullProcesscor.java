package burp;

import java.awt.event.ActionListener;

public class ClazzScopeNullProcesscor implements IIntruderPayloadProcessor {
    private final BurpGui guiManager;
    private final IExtensionHelpers helpers;

    public ClazzScopeNullProcesscor(BurpGui guiManager, IExtensionHelpers helpers) {
        this.guiManager = guiManager;
        this.helpers = helpers;
    }

    @Override
    public String getProcessorName() {
        return "ClazzScope Do not thing";
    }

    @Override
    public byte[] processPayload(byte[] currentPayload, byte[] originalPayload, byte[] baseValue) {

        String oldclassName = helpers.bytesToString(currentPayload);
        String library = oldclassName.substring(oldclassName.lastIndexOf(",")+1);
        oldclassName = oldclassName.substring(0, oldclassName.length() - library.length()-1);

        guiManager.addClassNotFound(oldclassName, library);
            
            
        return currentPayload;
    }
}
