package ru.biosoft.math.unitparser;

/* Generated By:JJTree: Do not edit this line. AstConstant.java Version 7.0 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=false,TRACK_TOKENS=false,NODE_PREFIX=Ast,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */

public
class AstConstant extends SimpleNode {
  public AstConstant(int id) {
    super(id);
  }

  public AstConstant(UnitParser p, int id) {
    super(p, id);
  }

  private Number value;
  public Number getValue()
  {
    return value;
  }
  public void setValue(Number val)
  {
    value = val;
  }

}
/* JavaCC - OriginalChecksum=9a7ee7a4cf1167b9ea64c7cb8096969c (do not edit this line) */