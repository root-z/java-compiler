package environment;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ast.ArrayType;
import ast.PrimitiveType;
import ast.SimpleName;
import ast.SimpleType;
import ast.Type;
import ast.TypeDeclaration;

public class TypeHelper {
    // t1 := t2
    public static boolean assignable(Type t1, Type t2) {
        if (t1 == null) {
            return false;
        }
        if (t2 == null) {
            return ((t1 instanceof SimpleType) || t1 instanceof ArrayType);
        }

        if (t1 instanceof PrimitiveType) {
            if (!(t2 instanceof PrimitiveType)) {
                return false;
            }
            PrimitiveType tau1 = (PrimitiveType) t1;
            PrimitiveType tau2 = (PrimitiveType) t2;
            return tau1.value == tau2.value ||
                     (tau1.value == PrimitiveType.Value.SHORT && tau2.value == PrimitiveType.Value.BYTE) ||
                     (tau1.value == PrimitiveType.Value.INT && tau2.value == PrimitiveType.Value.CHAR) ||
                     (tau1.value == PrimitiveType.Value.INT && tau2.value == PrimitiveType.Value.SHORT) ||
                     (tau1.value == PrimitiveType.Value.INT && tau2.value == PrimitiveType.Value.BYTE);
        } else if (t1 instanceof SimpleType) {
//            if (t1.getDeclaration().getFullName().equals("java.lang.Object")) {
//                return true;
//            }
            if (t2 instanceof PrimitiveType) {
                return false;
            }
            if (t2 instanceof SimpleType) {
                SimpleType tau1 = (SimpleType) t1;
                SimpleType tau2 = (SimpleType) t2;
                return inheritsFrom(tau1, tau2);
            }
            if (t2 instanceof ArrayType) {
                SimpleType tau1 = (SimpleType) t1;
                return tau1.getDeclaration().getFullName().equals("java.lang.Object") ||
                       tau1.getDeclaration().getFullName().equals("java.lang.Cloneable") ||
                       tau1.getDeclaration().getFullName().equals("java.io.Serializable");
            }
        } else if (t1 instanceof ArrayType) {
            if (!(t2 instanceof ArrayType)) {
                return false;
            }
            ArrayType tau1 = (ArrayType) t1;
            ArrayType tau2 = (ArrayType) t2;
            if (tau1.type instanceof PrimitiveType && tau2.type instanceof PrimitiveType) {
            	PrimitiveType tau1t = (PrimitiveType) tau1.type;
                PrimitiveType tau2t = (PrimitiveType) tau2.type;
                return tau1t.value == tau2t.value;
            }
            if (tau1.type instanceof SimpleType && tau2.type instanceof SimpleType) {
                SimpleType tau1t = (SimpleType) tau1.type;
                SimpleType tau2t = (SimpleType) tau2.type;
                return inheritsFrom(tau1t, tau2t);
            }
            return false;
        }

        //we should never get here
        return false;
    }

    /**
     * check if tau2 inherits from tau1.
     * TODO: add Object in here.
     * @param tau1
     * @param tau2
     * @return
     */
    public static boolean inheritsFrom(Type tau1, Type tau2) {
        TypeDeclaration tDecl1 = tau1.getDeclaration();
        if (tDecl1.getFullName().equals("java.lang.Object")) {
            return true;
        }
        
        if (!tDecl1.isInterface) {
            TypeDeclaration tDecl2 = tau2.getDeclaration();
            if (tDecl2.isInterface) {
                return false;
            }

            for ( ; ; ) {
                if (tDecl2.equals(tDecl1)) {
                    return true;
                }
                if (tDecl2.superClass == null) {
                    return false;
                }
                tDecl2 = tDecl2.superClass.getDeclaration();
            }
        }

        Set<TypeDeclaration> checked = new HashSet<>();
        List<Type> toCheck = new LinkedList<>();
        toCheck.add(tau2);

        while (!toCheck.isEmpty()) {
            TypeDeclaration tDecl = toCheck.remove(0).getDeclaration();
            if (!tDecl.isInterface) {
                if (tDecl.interfaces != null) {
                    toCheck.addAll(tDecl.interfaces);
                }
                if (tDecl.superClass != null) {
                    toCheck.add(tDecl.superClass);
                }
            }

            if (checked.contains(tDecl)) {
                continue;
            }
            if (tDecl.equals(tDecl1)) {
                return true;
            }
            if (tDecl.interfaces != null) {
                toCheck.addAll(tDecl.interfaces);
            }
            checked.add(tDecl);
        }

        return false;
    }
    
    public static boolean inheritsFrom(TypeDeclaration tau1, TypeDeclaration tau2) {
    	Type t1 = new SimpleType(new SimpleName(tau1.id));
    	t1.attachDeclaration(tau1);
    	Type t2 = new SimpleType(new SimpleName(tau2.id));
    	t2.attachDeclaration(tau2);
    	return inheritsFrom(t1, t2);
    }

}
