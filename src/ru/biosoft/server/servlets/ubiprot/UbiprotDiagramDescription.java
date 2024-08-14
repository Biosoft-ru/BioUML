package ru.biosoft.server.servlets.ubiprot;

import biouml.standard.type.Protein;

public class UbiprotDiagramDescription
{
    protected Protein substrate;
    protected Protein[] e1;
    protected Protein[] e2;
    protected Protein[] e3;
    protected Protein dub;
    protected Protein ubp;

    protected String preModification;
    protected String postModification;

    public Protein getDub()
    {
        return dub;
    }
    public void setDub(Protein dub)
    {
        this.dub = dub;
    }
    public Protein[] getE1()
    {
        return e1;
    }
    public void setE1(Protein[] e1)
    {
        this.e1 = e1;
    }
    public Protein[] getE2()
    {
        return e2;
    }
    public void setE2(Protein[] e2)
    {
        this.e2 = e2;
    }
    public Protein[] getE3()
    {
        return e3;
    }
    public void setE3(Protein[] e3)
    {
        this.e3 = e3;
    }
    public Protein getSubstrate()
    {
        return substrate;
    }
    public void setSubstrate(Protein substrate)
    {
        this.substrate = substrate;
    }
    public Protein getUbp()
    {
        return ubp;
    }
    public void setUbp(Protein ubp)
    {
        this.ubp = ubp;
    }
    public String getPreModification()
    {
        return preModification;
    }
    public void setPreModification(String preModification)
    {
        this.preModification = preModification;
    }
    public String getPostModification()
    {
        return postModification;
    }
    public void setPostModification(String postModification)
    {
        this.postModification = postModification;
    }
}
