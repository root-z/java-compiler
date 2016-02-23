package environment;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import ast.*;

public class Hierarchy {
	Set<TypeDeclaration> visited;
	
	public Hierarchy(List<AST> trees) {
		buildHierarchy(trees);
		checkHierarchy(trees);
	}
	
	/**
	 * fills the inherit environment for each type declaration
	 * @param trees
	 */
	public void buildHierarchy(List<AST> trees) {
		visited = new TreeSet<TypeDeclaration>();
		for (AST tree : trees) {
			if (tree.root.types.size() > 0)
				buildInherit(tree.root.types.get(0));
		}
	}

	private void buildInherit(TypeDeclaration typeDecl) {
		
		if (visited.contains(typeDecl)) {
			// if already visited, skip.
			// this is possible because of the super class calls
			return;
		}
		
		Environment inheritEnv = typeDecl.getEnvironment().getEnclosing();
		
		for (Type itf : typeDecl.interfaces) {
			TypeDeclaration itfDecl = itf.getDeclaration();
			if (! visited.contains(itfDecl)) {
				buildInherit(itfDecl);
			}
			Environment superEnv = itfDecl.getEnvironment();
			inherit(inheritEnv, superEnv);
		}
		
		// parent class
		if (typeDecl.superClass != null) {
			TypeDeclaration superDecl = typeDecl.superClass.getDeclaration();
			if (! visited.contains(superDecl)) {
				// if the super class has not been processed, do it first
				buildInherit(superDecl);
			}
			
			Environment superEnv = superDecl.getEnvironment();
			inherit(inheritEnv, superEnv);
		}
		
		visited.add(typeDecl);
		
	}
	
	public void inherit(Environment inheritEnv, Environment superEnv) {
		Environment superInherit = superEnv.getEnclosing();
		
		// fields from super class, overriding might happen
		for (String field : superInherit.fields.keySet()) {
			inheritEnv.addField(field, superEnv.fields.get(field));
		}
		for (String field : superEnv.fields.keySet()) {
			inheritEnv.addField(field, superEnv.fields.get(field));
		}
		
		//methods from superclass, overriding might happen
		for (String method : superInherit.methods.keySet()) {
			inheritEnv.addMethod(method, superInherit.methods.get(method));
		}
		for (String method : superEnv.methods.keySet()) {
			inheritEnv.addMethod(method, superEnv.methods.get(method));
		}
	}
	
	public void checkHierarchy(List<AST> trees) {
		
	}
}