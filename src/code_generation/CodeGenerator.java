package code_generation;

import ast.WhileStatement;
import environment.TraversalVisitor;

public class CodeGenerator extends TraversalVisitor {

    private static final String FALSE = "0x0";
    private static final String TRUE = "0xffffffff";
    // instead of using delimiter all the time, list may be a better choice...
    private static final String delimiter = "\n";
    private Integer loopCount = 0;
    StringBuilder assemblyText = new StringBuilder();

    public void visit(WhileStatement node) throws Exception {
        loopCount++;
        StringBuilder whileAssemblyText = new StringBuilder();
        whileAssemblyText.append("LOOP_" + loopCount + ":" + delimiter);

        if (node.whileCondition != null) {
            node.whileCondition.accept(this);
        }

        whileAssemblyText.append("cmp eax, " + FALSE + delimiter);
        whileAssemblyText.append("je LOOP_END_" + loopCount + delimiter);

        whileAssemblyText.append("LOOP_BLOCK_" + loopCount + ":" + delimiter);
        if (node.whileStatement != null) {
            node.whileStatement.accept(this);
        }
        whileAssemblyText.append("jmp LOOP_" + loopCount + delimiter);
        whileAssemblyText.append("LOOP_END_" + loopCount + ":" + delimiter);

        assemblyText.append(whileAssemblyText + delimiter);
        visitNextStatement(node);
    }
}
