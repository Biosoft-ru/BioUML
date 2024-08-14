package biouml.model;

public interface Role
{
    /** @returns diagram element associated with this role. */
    public DiagramElement getDiagramElement();

    /** Creates copy of the object and associate it with specified diagram element. */
    public Role clone(DiagramElement de);
}
