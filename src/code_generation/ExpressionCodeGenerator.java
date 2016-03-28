package code_generation;

import utility.StringUtility;
import ast.BooleanLiteral;
import ast.CharacterLiteral;
import ast.ClassInstanceCreationExpression;
import ast.Expression;
import ast.IntegerLiteral;
import ast.MethodDeclaration;
import ast.MethodInvocation;
import ast.Modifier;
import ast.NullLiteral;
import ast.PrefixExpression;
import ast.PrefixExpression.Operator;
import ast.QualifiedName;
import ast.SimpleName;
import ast.StringLiteral;
import ast.TypeDeclaration;
import environment.NameHelper;
import environment.TraversalVisitor;
import exceptions.NameException;

public class ExpressionCodeGenerator extends TraversalVisitor {

    private static final String FALSE = "0x0";
    private static final String TRUE = "0xffffffff";
    private int stringLitCounter = 0;
    
    /*
     * Literals
     */
    // String is Object.
    public void visit(StringLiteral node) throws Exception {
        // can use counter because string literal is not global.
        stringLitCounter++;
        // TODO:integrate stringLitData into data section.
        StringBuilder stringLitData = new StringBuilder();
        StringUtility.appendLine(stringLitData, "STRING_" + stringLitCounter + ":" + "\t; define label for string literal");
        StringUtility.appendLine(stringLitData, "\t" + "dw " + '\'' + node.value + '\'');

        node.attachCode("mov eax, " + "STRING_" + stringLitCounter);
    }

    public void visit(NullLiteral node) throws Exception {
        node.attachCode("\tmov eax, " + FALSE + "\n");
    }

    public void visit(BooleanLiteral node) throws Exception {
        String booleanText;
        if (node.value == true) {
            booleanText = "\tmov eax, " + TRUE;
        } else {
            booleanText = "\tmov eax, " + FALSE;
        }
        node.attachCode(booleanText + "\n");
    }

    public void visit(CharacterLiteral node) throws Exception {
        String charText;

        // Assuming octal is valid.
        if (node.value.length() > 3) {
            charText = "\tmov eax, " + "0o" + node.value.substring(1);
        } else {
            charText = "\tmov eax, " + "'" + node.value + "'";
        }

        node.attachCode(charText + "\n");
    }

    public void visit(IntegerLiteral node) throws Exception {
        String intText;
        intText = "\tmov eax, " + node.value + "\n";
        node.attachCode(intText);
    }
    
    @Override
    public void visit(PrefixExpression node) throws Exception {
        StringBuilder prefixText = new StringBuilder();
        if (node.expr != null) {
            if (node.op.equals(Operator.MINUS)) {
                if (node.expr instanceof IntegerLiteral) {
                    ((IntegerLiteral)node.expr).value = "-" + ((IntegerLiteral)node.expr).value;
                } else {
                    StringUtility.appendIndLn(prefixText, "move eax, -eax" + "\t; negation operation");
                }
                
            } else if (node.op.equals(Operator.NOT)) {
                StringUtility.appendIndLn(prefixText, "move eax, !eax" + "\t; logical negation operation");
            }
            node.expr.accept(this);
            String exprCode = node.expr.getCode();
            prefixText.insert(0, exprCode);
        }
        node.attachCode(prefixText.toString());
    }


    /*
     * OO features
     */
    
    @Override
    public void visit(ClassInstanceCreationExpression node) throws Exception {
    	StringBuilder sb = new StringBuilder();    	
    	// generate code for arguments
    	int numArgs = 0;
    	if (node.arglist != null) {
	    	for (numArgs =0; numArgs < node.arglist.size() ; numArgs++) {
	    		Expression expr = node.arglist.get(numArgs);
	    		expr.accept(this);
	    		StringUtility.appendLine(sb, expr.getCode());
	    		StringUtility.appendIndLn(sb, "push eax \t; push argument " + numArgs);
	    	}    	
    	}
    	
    	// malloc
    	TypeDeclaration tDecl = node.type.getDeclaration();
    	int objSize = tDecl.getFieldOffSetList().size() + 2;
    	StringUtility.appendIndLn(sb, "mov eax, 4*" + objSize + "\t; size of object");
    	StringUtility.appendIndLn(sb, "call __malloc");
    	StringUtility.appendIndLn(sb, "push eax \t; push object address");
    	
    	// call field initializer
    	StringUtility.appendIndLn(sb, "call " + SigHelper.instanceFieldInitSig(node.type));

    	// implicit super call
    	if (tDecl.superClass != null) {
    		MethodDeclaration superConstructor = getDefaultConstructor(tDecl.superClass.getDeclaration());
        	StringUtility.appendIndLn(sb, "call " + SigHelper.getMethodSigWithImp(superConstructor));
    	}
    	
    	// call actual constructor
    	StringUtility.appendIndLn(sb, "call " + SigHelper.getMethodSigWithImp(node.getConstructor()));
    	
    	// clean up
    	StringUtility.appendIndLn(sb, "pop eax \t; pop object address back in eax");
    	StringUtility.appendIndLn(sb, "add esp, 4 * " + numArgs);
    	node.attachCode(sb.toString());
    }
    
    private MethodDeclaration getDefaultConstructor(TypeDeclaration superClass) throws Exception {
    	MethodDeclaration result = null;
    	for (MethodDeclaration constructor : superClass.getEnvironment().constructors.values()) {
    		if (constructor.parameters.size() == 0)
    			result = constructor;
    	}
		if (result == null)
			throw new Exception("No default constructor is defined for super class: " + superClass.id);
    	
    	return result;
	}

	/**
     * Deals with method names
     */
    @Override
    public void visit(MethodInvocation node) throws Exception {
    	StringBuilder sb = new StringBuilder();

    	// generate code for arguments
    	int numArgs = 0;
    	if (node.arglist != null) {
	    	for (numArgs =0; numArgs < node.arglist.size() ; numArgs++) {
	    		Expression expr = node.arglist.get(numArgs);
	    		expr.accept(this);
	    		StringUtility.appendLine(sb, expr.getCode());
	    		StringUtility.appendIndLn(sb, "push eax \t; push argument " + numArgs);
	    	}
    	}
    	
    	if (node.id != null) {
    		// Primary.ID(...)
    		node.expr.accept(this);	// generate code for Primary expression
    		StringUtility.appendLine(sb, node.getCode());	// by this point eax should contain address to object return by Primary expression
    		StringUtility.appendIndLn(sb, "push eax \t; push object for method invocation");
    		MethodDeclaration mDecl = (MethodDeclaration) node.id.getDeclaration();	// the actual method being called
    		// call method
    		StringUtility.appendIndLn(sb, generateMethodCall(mDecl));
    		
    	} else {
    		// Name(...)
    		if (node.expr instanceof SimpleName) {	// SimpleName(...), implicit this
    			SimpleName sn = (SimpleName) node.expr;
    			StringUtility.appendIndLn(sb, "mov eax, [ebp + 8] \t; move object address to eax"); // this only happens in the method of the same class
    			StringUtility.appendIndLn(sb, "push eax \t; push object address");
    			MethodDeclaration mDecl = (MethodDeclaration) sn.getDeclaration();
    			StringUtility.appendIndLn(sb, generateMethodCall(mDecl));

    		} else {	// QualifiedName(...)
    			QualifiedName qn = (QualifiedName) node.expr;
    			MethodDeclaration mDecl = (MethodDeclaration) qn.getDeclaration();
    			if (qn.getQualifier().getDeclaration() instanceof TypeDeclaration) {	// static methods
    				StringUtility.appendIndLn(sb, "push 0 \t; place holder because there is no this object for static method");
    				StringUtility.appendIndLn(sb, generateMethodCall(mDecl));
    			} else {	// instance method
    				qn.accept(this); 	// generate code from name (accessing instance field, or local variable
    				StringUtility.appendLine(sb, qn.getCode());
    	    		StringUtility.appendIndLn(sb, "push eax \t; push object for method invocation");
    	    		// call method
    	    		StringUtility.appendIndLn(sb, generateMethodCall(mDecl));
    			}
    		}
    	}
    	
		// clean up
		StringUtility.appendIndLn(sb, "add esp, " + (numArgs+1) + "*4" + "\t; caller cleanup arguments.");	
		node.attachCode(sb.toString());
    	
    }
    
    /**
     * generating actual method call by offset, assumes that the object is in eax
     * @param mDecl
     * @return
     * @throws NameException
     * @throws Exception
     */
    private String generateMethodCall(MethodDeclaration mDecl) throws NameException, Exception {
    	String call;
    	TypeDeclaration tDecl = (TypeDeclaration) mDecl.getParent();
    	if (mDecl.modifiers.contains(Modifier.STATIC)) {
    		call = "call " + SigHelper.getMethodSigWithImp(mDecl);	// call static methods
    		
    	} else {	// instance method
			// generate method call
			if (tDecl.isInterface) {	// interface method
				int offset = OffSet.getInterfaceMethodOffset(NameHelper.mangle(mDecl));
				call = "call [[eax] + " + offset + "*4] \t; call interface method.";		//TODO: check if the level of indirection is proper 
			} else {
				int offset = tDecl.getMethodOffSet(NameHelper.mangle(mDecl));
				call = "call [[eax] + " + offset + "*4] \t; call class method.";
			}
    	}
		return call;
    }
    
}
