package org.nuxeo.ecm.flex.javadto;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.DocumentRef;

public class FlexDocumentModel implements Externalizable {


    private String docRef;
    private String name;
    private String path;
    private String lifeCycleState;

    private transient Map<String, Map<String,Serializable>> data = new HashMap<String, Map<String,Serializable>>();
    private Map<String,Serializable> dirtyFields =  new HashMap<String, Serializable>();

    public FlexDocumentModel()
    {
        docRef="docRef";
        name="docName";
        path="/default/folder"+name;
        lifeCycleState="work";
    }

    public FlexDocumentModel(DocumentRef ref, String name, String path, String lcState)
    {
        docRef=ref.toString();
        this.name=name;
        this.path=path;
        lifeCycleState=lcState;
    }


    public Map<String,Serializable> getDirtyFields()
    {
        return dirtyFields;
    }

    public void feed(String schemaName, Map<String,Serializable> map)
    {
        data.put(schemaName, map);
    }


    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {

        docRef=in.readUTF();
        name=in.readUTF();
        path=in.readUTF();
        lifeCycleState=in.readUTF();
        //only ready dirty fields
        dirtyFields=(Map<String,Serializable>) in.readObject();
        // don't read all data
        //data = (Map<String, Map<String,Serializable>>) in.readObject();

    }

    public void writeExternal(ObjectOutput out) throws IOException {

        out.writeUTF(docRef);
        out.writeUTF(name);
        out.writeUTF(path);
        out.writeUTF(lifeCycleState);
        // only sends data : nothing is dirty for now
        out.writeObject(data);
    }

}
