package gov.nih.nci.evs.api.model.sparql;

public class Head
{
    private String[] vars;

    public String[] getVars ()
    {
        return vars;
    }

    public void setVars (String[] vars)
    {
        this.vars = vars;
    }

    @Override
    public String toString()
    {
        return "ClassPojo [vars = "+vars+"]";
    }
}