import norswap.autumn.AutumnTestFixture;
import norswap.autumn.positions.LineMapString;
import norswap.sigh.SemanticAnalysis;
import norswap.sigh.SighGrammar;
import norswap.sigh.ast.SighNode;
import norswap.sigh.interpreter.InterpreterException;
import norswap.uranium.Reactor;
import norswap.uranium.UraniumTestFixture;
import norswap.utils.visitors.Walker;
import org.testng.annotations.Test;

/**
 * NOTE(norswap): These tests were derived from the {@link InterpreterTests} and don't test anything
 * more, but show how to idiomatically test semantic analysis. using {@link UraniumTestFixture}.
 */
public final class SemanticAnalysisTests extends UraniumTestFixture
{
    // ---------------------------------------------------------------------------------------------

    private final SighGrammar grammar = new SighGrammar();
    private final AutumnTestFixture autumnFixture = new AutumnTestFixture();

    {
        autumnFixture.rule = grammar.root();
        autumnFixture.runTwice = false;
        autumnFixture.bottomClass = this.getClass();
    }

    private String input;

    @Override protected Object parse (String input) {
        this.input = input;
        return autumnFixture.success(input).topValue();
    }

    @Override protected String astNodeToString (Object ast) {
        LineMapString map = new LineMapString("<test>", input);
        return ast.toString() + " (" + ((SighNode) ast).span.startString(map) + ")";
    }

    // ---------------------------------------------------------------------------------------------

    @Override protected void configureSemanticAnalysis (Reactor reactor, Object ast) {
        Walker<SighNode> walker = SemanticAnalysis.createWalker(reactor);
        walker.walk(((SighNode) ast));
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testLiteralsAndUnary() {
        successInput("return 42");
        successInput("return 42.0");
        successInput("return \"hello\"");
        successInput("return (42)");
        successInput("return [1, 2, 3]");
        successInput("return true");
        successInput("return false");
        successInput("return null");
        successInput("return !false");
        successInput("return !true");
        successInput("return !!true");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testNumericBinary() {
        successInput("return 1 + 2");
        successInput("return 2 - 1");
        successInput("return 2 * 3");
        successInput("return 2 / 3");
        successInput("return 3 / 2");
        successInput("return 2 % 3");
        successInput("return 3 % 2");

        successInput("return 1.0 + 2.0");
        successInput("return 2.0 - 1.0");
        successInput("return 2.0 * 3.0");
        successInput("return 2.0 / 3.0");
        successInput("return 3.0 / 2.0");
        successInput("return 2.0 % 3.0");
        successInput("return 3.0 % 2.0");

        successInput("return 1 + 2.0");
        successInput("return 2 - 1.0");
        successInput("return 2 * 3.0");
        successInput("return 2 / 3.0");
        successInput("return 3 / 2.0");
        successInput("return 2 % 3.0");
        successInput("return 3 % 2.0");

        successInput("return 1.0 + 2");
        successInput("return 2.0 - 1");
        successInput("return 2.0 * 3");
        successInput("return 2.0 / 3");
        successInput("return 3.0 / 2");
        successInput("return 2.0 % 3");
        successInput("return 3.0 % 2");

        failureInputWith("return 2 + true", "Trying to add Int with Bool");
        failureInputWith("return true + 2", "Trying to add Bool with Int");
        //failureInputWith("return 2 + [1]", "Trying to add Int with Int[]");
        //failureInputWith("return [1] + 2", "Trying to add Int[] with Int");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testOtherBinary() {
        successInput("return true && false");
        successInput("return false && true");
        successInput("return true && true");
        successInput("return true || false");
        successInput("return false || true");
        successInput("return false || false");

        failureInputWith("return false || 1",
            "Attempting to perform binary logic on non-boolean type: Int");
        failureInputWith("return 2 || true",
            "Attempting to perform binary logic on non-boolean type: Int");

        successInput("return 1 + \"a\"");
        successInput("return \"a\" + 1");
        successInput("return \"a\" + true");

        successInput("return 1 == 1");
        successInput("return 1 == 2");
        successInput("return 1.0 == 1.0");
        successInput("return 1.0 == 2.0");
        successInput("return true == true");
        successInput("return false == false");
        successInput("return true == false");
        successInput("return 1 == 1.0");

        failureInputWith("return true == 1", "Trying to compare incomparable types Bool and Int");
        failureInputWith("return 2 == false", "Trying to compare incomparable types Int and Bool");

        successInput("return \"hi\" == \"hi\"");
        successInput("return [1] == [1]");

        successInput("return 1 != 1");
        successInput("return 1 != 2");
        successInput("return 1.0 != 1.0");
        successInput("return 1.0 != 2.0");
        successInput("return true != true");
        successInput("return false != false");
        successInput("return true != false");
        successInput("return 1 != 1.0");

        failureInputWith("return true != 1", "Trying to compare incomparable types Bool and Int");
        failureInputWith("return 2 != false", "Trying to compare incomparable types Int and Bool");

        successInput("return \"hi\" != \"hi\"");
        successInput("return [1] != [1]");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testVarDecl() {
        successInput("var x: Int = 1; return x");
        successInput("var x: Int = x; return x");
        successInput("var x: Float = 2.0; return x");

        successInput("var x: Int = 0; return x = 3");
        successInput("var x: String = \"0\"; return x = \"S\"");

        failureInputWith("var x: Int = true", "expected Int but got Bool");
        failureInputWith("return x + 1", "Could not resolve: x");
        failureInputWith("return x + 1; var x: Int = 2", "Variable used before declaration: x");

        // implicit conversions
        successInput("var x: Float = 1 ; x = 2");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testRootAndBlock () {
        successInput("return");
        successInput("return 1");
        successInput("return 1; return 2");

        successInput("print(\"a\")");
        successInput("print(\"a\" + 1)");
        successInput("print(\"a\"); print(\"b\")");

        successInput("{ print(\"a\"); print(\"b\") }");

        successInput(
            "var x: Int = 1;" +
            "{ print(\"\" + x); var x: Int = 2; print(\"\" + x) }" +
            "print(\"\" + x)");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testCalls() {
        successInput(
            "fun add (a: Int, b: Int): Int { return a + b } " +
            "return add(4, 7)");

        successInput(
            "struct Point { var x: Int; var y: Int }" +
            "return $Point(1, 2)");

        successInput("var str: String = null; return print(str + 1)");

        failureInputWith("return print(1)", "argument 0: expected String but got Int");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testArrayStructAccess() {
        successInput("return [1][0]");
        successInput("return [1.0][0]");
        successInput("return [1, 2][1]");

        successInput("return [1, 1]*[2, 2]");
        successInput("return [1, 1]/[0, 2]");
        successInput("return [[1, 2, 3], [4, 5, 6]]");
        successInput("return [[[1], [2], [3]], [[4], [5], [6]]]");
        successInput("return ([[0,1],[2,3]] @ [[1,1],[2,2]])[0][0]");
        successInput("return ([[0,1],[2,3]] @ [[1],[2]])[0]");
        successInput("return ([0,1] @ [[1],[2]])");
        successInput("var x: Int[2][2][2]; var y: Int[2][2][2];" +
            "x[0][0][0]=1;x[1][0][0]=2;x[0][0][1]=3;x[1][0][1]=4;" +
            "x[0][1][0]=5;x[1][1][0]=6;x[0][1][1]=7;x[1][1][1]=8;"+
            "y[0][0][0]=1;y[1][0][0]=3;y[0][0][1]=5;y[1][0][1]=7;" +
            "y[0][1][0]=9;y[1][1][0]=11;y[0][1][1]=13;y[1][1][1]=15;"+
            "var z:Int[][][]=y/x;" +
            "return z[0][1][1]");
        successInput("var x: Int[2][2]; var y: Int[2][2];" +
            "x[0][0]=1;x[1][0]=2;" +
            "y[0][0]=4;y[1][0]=5;" +
            "var z:Int[][]=x*y;" +
            "return z[1][0]");

        failureInputWith("return [1][true]", "Indexing an array using a non-Int-valued expression");
        failureInputWith("return [1,2,3]*[1,2]", "Trying to operate on arrays with different dimensions: [3] and [2]");
        failureInputWith("return [1,2,3]*[[1],[2],[3]]", "Trying to operate on arrays with different dimensions: [3] and [3, 1]");
        failureInputWith("return [[1,3],[2,4],[3,5]]+[[1],[2],[3]]", "Trying to operate on arrays with different dimensions: [3, 2] and [3, 1]");

        failureInputWith("return (1 @ [[1],[2]])[0][0]","Trying to dotproduct Int with Int[][]");

        successInput("return [].length");

        successInput("return [1].length");
        successInput("return [1, 2].length");

        successInput("return [4, 2].avg");
        successInput("return [1, 3, 4, 2].count");
        successInput("return [4, 2].sum");
        successInput("return [4, 2].nDim");
        successInput("return [[4, 2],[1,3]].nDim");
        successInput("return [[4, 2],[1,3]].sum");
        successInput("return [[4, 2],[1,3]].avg");
        successInput("return [[4, 2],[1,3]].count");
        successInput("return [[4, 2],[3]].count");
        successInput("return [1, 3, 4, 2].count");
        successInput("return [4, 2].sum");
        successInput("return ([4, 2]+[1, 1])[0]");
        successInput("return ([4, 2]+[1, 1])[1]");
        successInput("return ([4, 2]*[2, 3])[0]");
        successInput("return ([4, 2]*[2, 3])[1]");



        successInput("var array: Int[] = null; return array[0]");
        successInput("var array: Int[] = null; return array.length");

        successInput("var x: Int[] = [0, 1]; x[0] = 3; return x[0]");
        successInput("var x: Int[] = []; x[0] = 3; return x[0]");
        successInput("var x: Int[] = null; x[0] = 3");

        successInput(
            "struct P { var x: Int; var y: Int }" +
            "return $P(1, 2).y");

        successInput(
            "struct P { var x: Int; var y: Int }" +
            "var p: P = null;" +
            "return p.y");

        successInput(
            "struct P { var x: Int; var y: Int }" +
            "var p: P = $P(1, 2);" +
            "p.y = 42;" +
            "return p.y");

        successInput(
            "struct P { var x: Int; var y: Int }" +
            "var p: P = null;" +
            "p.y = 42");

        failureInputWith(
            "struct P { var x: Int; var y: Int }" +
            "return $P(1, true)",
            "argument 1: expected Int but got Bool");

        failureInputWith(
            "struct P { var x: Int; var y: Int }" +
            "return $P(1, 2).z",
            "Trying to access missing field z on struct P");
    }

    @Test public void testArrayScalarOperation(){
        successInput("return ([1,2,3]+2)[0]");
        successInput("return ([1,2,3]+2)[1]");
        successInput("return ([1,2,3]+2)[2]");
        successInput("return ([[1],[2],[3]]+2)[0][0]");
        successInput("return ([[1],[2],[3]]+2)[1][0]");
        successInput("return (2+[1,2,3])[0]");
        successInput("return (2+[1,2,3])[1]");
        successInput("return (2+[1,2,3])[2]");
        successInput("return (2+[[1],[2],[3]])[0][0]");
        successInput("return (2+[[1],[2],[3]])[1][0]");
        successInput("return ([1,2,3]*2)[0]");
        successInput("return ([1,2,3]*2)[1]");
        successInput("return ([1,2,3]*2)[2]");
        successInput("return ([[1],[2],[3]]*2)[0][0]");
        successInput("return ([[1],[2],[3]]*2)[1][0]");
        successInput("return (2*[1,2,3])[0]");
        successInput("return (2*[1,2,3])[1]");
        successInput("return (2*[1,2,3])[2]");
        successInput("return (2*[[1],[2],[3]])[0][0]");
        successInput("return (2*[[1],[2],[3]])[1][0]");
        successInput("return ([1,2,3]-2)[0]");
        successInput("return ([1,2,3]-2)[1]");
        successInput("return ([1,2,3]-2)[2]");
        successInput("return ([[1],[2],[3]]-2)[0][0]");
        successInput("return ([[1],[2],[3]]-2)[1][0]");
        successInput("return (2-[1,2,3])[0]");
        successInput("return (2-[1,2,3])[1]");
        successInput("return (2-[1,2,3])[2]");
        successInput("return (2-[[1],[2],[3]])[0][0]");
        successInput("return (2-[[1],[2],[3]])[1][0]");
        successInput("return ([1,2,3]/2)[0]");
        successInput("return ([1,2,3]/2)[1]");
        successInput("return ([1,2,3]/2)[2]");
        successInput("return ([[1],[2],[3]]/2)[0][0]");
        successInput("return ([[1],[2],[3]]/2)[1][0]");
        successInput("return (2/[1,2,3])[0]");
        successInput("return (2/[1,2,3])[1]");
        successInput("return (2/[1,2,3])[2]");
        successInput("return (2/[[1],[2],[3]])[0][0]");
        successInput("return (2/[[1],[2],[3]])[1][0]");
    }

    @Test public void testClassDeclaration(){

        successInput(
            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } }"+
                " var p: Fraction = $Fraction(1, 2)" +
                " var x: Fraction[] = [$Fraction(1, 1),$Fraction(2, 2)]" +
                "return x[0].to_Number()");

        successInput(
            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } }"+

                " var x: Fraction[] = [$Fraction(1, 2),$Fraction(1, 4)];" +
                " var y: Fraction[] = [$Fraction(2, 2),$Fraction(1, 1)];" +
                " var z: Fraction[] = x+y; " +
                "return z[0].num");

        successInput(
            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Float { return num/den } }"+
                " var p: Fraction = $Fraction(1, 2)" +
                "return p.den");

        failureInputWith(
            "var num : Int =2;" +
                "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Float { return num/den } }"+
                " var p: Fraction = $Fraction(1, 2)" +
                "return p.to_Number()","You cannot define a attribut and a variable with the same name");





    }
    @Test public void testArrayDeclaration(){
        successInput("var x: Int[2]; return x[0]");
        successInput("var x: Int[1][2][3]; return x");
        successInput("var x: Int[1][2][3]; return x[0]");
        successInput("var x: Int[1][2][3]; return x[0][0]");
        successInput("var x: Int[1][2][3]; return x[0][0][0]");
        successInput("var x: Int[1][2][3]; x[0][0][0]=3; return x[0][0][0]");
        successInput("var x: Int[2]; var y: Int[2];" +
            "x[0]=1;x[1]=1;" +
            "y[0]=4;y[1]=5;" +
            "var z:Int[]=x+y;" +
            "return z[0]");
        successInput("var x: Int[2]; var y: Int[2];" +
            "x[0]=1;x[1]=1;" +
            "y[0]=4;y[1]=5;" +
            "var z:Int[]=x+y;" +
            "return z[1]");

        successInput("var x: Int[2][1]; var y: Int[2][1];" +
            "x[0][0]=1;x[1][0]=1;" +
            "y[0][0]=4;y[1][0]=5;" +
            "var z:Int[][]=x+y;" +
            "return z[0][0]");
        failureInputWith("var x: Int[2][\"size\"]","Illegal size for array declaration: \"size\"");
        failureInputWith("var x: Int[2][2.3]","Illegal size for array declaration: 2.3");
        failureInputWith("var x: Int[2][-3]","Illegal size for array declaration: -3");

        successInput("var x: Int[2][1]; var y: Int[][]=[[4],[5]];" +
            "x[0][0]=1;x[1][0]=1;" +
            "var z:Int[][]=x+y;" +
            "return z[0][0]");

        successInput("var x: Int [3][4]"+
            "x[0][0] = 30;x[1][1] = 21;x[2][2] = 78;x[2][3] = 45;"+
            "var y: Int [][] = [[1,2,3,4,5],[5,4,3,2,1],[5,10,15,20,25],[1,3,5,7,11]]"+
            "var z: Int [][] = x @ y "+
            "return z[0][0]");
    }



    // ---------------------------------------------------------------------------------------------

    @Test
    public void testIfWhile () {
        successInput("if (true) return 1 else return 2");
        successInput("if (false) return 1 else return 2");
        successInput("if (false) return 1 else if (true) return 2 else return 3 ");
        successInput("if (false) return 1 else if (false) return 2 else return 3 ");

        successInput("var i: Int = 0; while (i < 3) { print(\"\" + i); i = i + 1 } ");

        failureInputWith("if 1 return 1",
            "If statement with a non-boolean condition of type: Int");
        failureInputWith("while 1 return 1",
            "While statement with a non-boolean condition of type: Int");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testInference() {
        successInput("var array: Int[] = []");
        successInput("var array: String[] = []");
        successInput("fun use_array (array: Int[]) {} ; use_array([])");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testTypeAsValues() {
        successInput("struct S{} ; return \"\"+ S");
        successInput("struct S{} ; var type: Type = S ; return \"\"+ type");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testUnconditionalReturn()
    {
        successInput("fun f(): Int { if (true) return 1 else return 2 } ; return f()");

        failureInputWith("fun f(): Int { if (true) return 1 } ; return f()",
            "Missing return in function");
    }

    // ---------------------------------------------------------------------------------------------
}
