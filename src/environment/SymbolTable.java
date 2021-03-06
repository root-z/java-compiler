package environment;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ast.AST;
import ast.TypeDeclaration;
import ast.Visitor;
import exceptions.NameException;

/**
 * created so that it is easier to maintain environment stack through different visitors
 * and if necessary to store links to all environments
 * @author zanel
 *
 */
public class SymbolTable {
    Environment curr;
    private static HashMap<String, TypeDeclaration> global = null;
    private static HashMap<String, List<String>> globalPackages = null;
    private static final String OBJ = "java.lang.Object";
    private static final String OBJ_ITF = "joosc.lang.ObjInterface";

    public SymbolTable() {
        curr = null;
    }

    public void openScope(Environment.EnvType scopeType) {
        curr = new Environment(curr, scopeType);

    }

    public void closeScope() {
        curr = curr.enclosing;
    }

    public Environment currentScope() {
        return curr;
    }

    /**
     * The global environment contains all the classes with fully qualified
     * names
     * 
     * @param trees
     * @throws NameException
     */
    public static void buildGlobal(List<AST> trees) throws NameException {
        global = new HashMap<String, TypeDeclaration>();
        globalPackages = new HashMap<String, List<String>>();
        for (AST ast : trees) {
            String fullName = "";
            String pkgName = "";
            List<String> pkgCls = null;
            if (ast.root.pkg != null) {
                pkgName = ast.root.pkg.name.toString();
                fullName += pkgName + ".";
            }

            pkgCls = globalPackages.get(pkgName);
            if (pkgCls == null) {
                pkgCls = new LinkedList<String>();
                globalPackages.put(pkgName, pkgCls);
            }

            // if no types are defined, add nothing
            if (ast.root.types.size() != 0) {
                // only look at first one because well no private classes or
                // interfaces
                TypeDeclaration type = ast.root.types.get(0);
                fullName += type.id;
                pkgCls.add(fullName);
                if (global.get(fullName) != null) {
                    throw new NameException("Duplicate Type");
                }
                global.put(fullName, type);

            }
        }
    }

    public static Map<String, TypeDeclaration> getGlobal() {
        if (global == null)
            throw new RuntimeException("build global environment first.");
        return global;
    }

    /**
     * a map from package name to the classes in the package
     * 
     * @return
     */
    public static Map<String, List<String>> getAllPackages() {
        if (globalPackages == null) {
            throw new RuntimeException("build global environment first.");
        }
        return globalPackages;
    }

    /**
     * example of how buildGlobal and SemanticsVisitor will be used.
     * 
     * @param trees
     * @throws Exception
     */
    public static void buildEnvs(List<AST> trees) throws Exception {
        buildGlobal(trees);
        Visitor sv = new TopDeclVisitor();
        for (AST tree : trees) {
            tree.root.accept(sv);
        }
    }

    public static TypeDeclaration getObjRef() {
        return global.get(OBJ);
    }

    public static TypeDeclaration getObjectInterfaceRef() {
        return global.get(OBJ_ITF);
    }

}
