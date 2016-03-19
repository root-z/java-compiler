package code_generation;

import ast.ASTNode;
import ast.ArrayType;
import ast.FieldDeclaration;
import ast.MethodDeclaration;
import ast.PrimitiveType;
import ast.PrimitiveType.Value;
import ast.SimpleName;
import ast.SimpleType;
import ast.Type;
import ast.TypeDeclaration;
import ast.VariableDeclaration;

public class SigHelper {

    public static String getTypeSig(Type type) {
        String sigName = null;
        if (type instanceof SimpleType) {
            SimpleType stype = (SimpleType)type;
            sigName = stype.getDeclaration().getFullName().replace('.', '/');
        } else if (type instanceof PrimitiveType) {
            PrimitiveType ptype = (PrimitiveType)type;
            if (ptype.value.equals(Value.BOOLEAN)) {
                sigName = "Z";
            } else {
                sigName = ptype.toString().substring(0, 1).toUpperCase();
            }
        } else if (type instanceof ArrayType) {
            ArrayType atype = (ArrayType) type;
            String arrayTypeName = getTypeSig(atype.type);
            if (atype.type instanceof SimpleType) {
                sigName = "[L" + arrayTypeName;
            } else {
                sigName = "[" + arrayTypeName;
            }
        }
        return sigName;
    }

    public static String getMethodSig(MethodDeclaration md) {
        StringBuilder methodSig = new StringBuilder();
        ASTNode typeNode = md.getParent();
        methodSig.append(getClassSig(typeNode));
        if (md.isConstructor) {
            methodSig.append("/<init>(");
        } else {
            methodSig.append("/" + md.id + "(");
        }
        if (md.parameters != null) {
            for (VariableDeclaration varDec : md.parameters) {
                methodSig.append(getTypeSig(varDec.type));
            }
        }
        methodSig.append(")");
//        if (md.isConstructor) {
//            methodSig.append("V");
//        } else {
//            // Check void?
//            methodSig.append(getTypeSig(md.returnType));
//        }
        return methodSig.toString();
    }

    public static String getFieldSig(FieldDeclaration fd) {
        StringBuilder fieldSig = new StringBuilder();
        ASTNode typeNode = fd.getParent();
        fieldSig.append(getClassSig(typeNode));
        fieldSig.append("/");
        fieldSig.append(fd.id);
        return fieldSig.toString();
    }

    private static String getClassSig(ASTNode typeNode) {
        String classSig = null;
        if (typeNode instanceof TypeDeclaration) {
            TypeDeclaration typeDec = (TypeDeclaration) typeNode;
            String name = typeDec.getFullName();
            SimpleType simpleType = new SimpleType(new SimpleName(name));
            classSig = getTypeSig(simpleType);
        }
        return classSig;
    }
}
