package ast;

import java.util.List;

import parser.ParseTree;
import exceptions.ASTException;

public class AssignmentExpression extends Expression {
    public Expression lhs;
    public Expression expr;

    public AssignmentExpression(ParseTree pt) throws ASTException {
        List<ParseTree> subtrees = pt.getChildren();
        lhs = Expression.parseExpression(subtrees.get(0));
        expr = Expression.parseExpression(subtrees.get(2));
    }

    public void accept(Visitor v) throws Exception {
        v.visit(this);
    }
}
