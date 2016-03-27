package code_generation;

import java.util.List;
import java.util.Map;

import utility.StringUtility;
import ast.TypeDeclaration;

public class UglyTableBuilder {
    private static final Map<TypeDeclaration, List<String>> ugly = OffSet.ugly;
    private static StringBuilder uglyText = new StringBuilder();

    public static void build() {
        StringUtility.appendLine(uglyText, "section .text " + uglyText);
        for (TypeDeclaration typeDec : ugly.keySet()) {
            String typeSig = SigHelper.getClassSigWithUgly(typeDec);
            StringUtility.appendLine(uglyText, "global " + typeSig);
            StringUtility.appendIndLn(uglyText, typeSig + ":");
            List<String> methodSigs = ugly.get(typeDec);
            for (String methodSig : methodSigs) {
                StringUtility.appendLine(uglyText, "dd " + methodSig, 2);
            }
        }
    }

    public static String getUgly() {
        return uglyText.toString();
    }
}
