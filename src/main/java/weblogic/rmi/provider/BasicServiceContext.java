package weblogic.rmi.provider;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class BasicServiceContext implements Externalizable {
    private static final long serialVersionUID = -1989708991725000930L;
    private int id;
    protected Object data;
    private boolean user;

    public BasicServiceContext() {
    }
    public BasicServiceContext(int var1, Object var2) {
        this(var1, var2, false);
    }

    public BasicServiceContext(int var1, Object var2, boolean var3) {
        this.id = var1;
        this.data = var2;
        this.user = var3;
    }
    public void writeExternal(ObjectOutput var1) throws IOException {
        var1.write(this.id);
        var1.writeBoolean(this.user);
        var1.writeObject(this.data);
    }

    public void readExternal(ObjectInput var1) throws IOException, ClassNotFoundException {
        this.id = var1.read();
        this.user = var1.readBoolean();
        this.data = var1.readObject();
    }

}
