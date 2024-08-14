package ru.biosoft.math.model;

public interface ASTVisitor
{
    void visitStart(AstStart start) throws Exception;

    void visitNode(Node node) throws Exception;
}
