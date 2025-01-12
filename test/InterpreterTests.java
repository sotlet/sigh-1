import norswap.autumn.AutumnTestFixture;
import norswap.autumn.Grammar;
import norswap.autumn.Grammar.rule;
import norswap.autumn.ParseResult;
import norswap.autumn.positions.LineMapString;
import norswap.sigh.SemanticAnalysis;
import norswap.sigh.SighGrammar;
import norswap.sigh.ast.SighNode;
import norswap.sigh.interpreter.Interpreter;
import norswap.sigh.interpreter.InterpreterException;
import norswap.sigh.interpreter.Null;
import norswap.uranium.Reactor;
import norswap.uranium.SemanticError;
import norswap.utils.IO;
import norswap.utils.TestFixture;
import norswap.utils.data.wrappers.Pair;
import norswap.utils.visitors.Walker;
import org.testng.annotations.Test;
import javax.lang.model.type.NullType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertThrows;

public final class InterpreterTests extends TestFixture {


    // ---------------------------------------------------------------------------------------------

    private final SighGrammar grammar = new SighGrammar();
    private final AutumnTestFixture autumnFixture = new AutumnTestFixture();

    {
        autumnFixture.runTwice = false;
        autumnFixture.bottomClass = this.getClass();
    }

    // ---------------------------------------------------------------------------------------------

    private Grammar.rule rule;

    // ---------------------------------------------------------------------------------------------

    private void check (String input, Object expectedReturn) {
        assertNotNull(rule, "You forgot to initialize the rule field.");
        check(rule, input, expectedReturn, null);
    }

    // ---------------------------------------------------------------------------------------------

    private void check (String input, Object expectedReturn, String expectedOutput) {
        assertNotNull(rule, "You forgot to initialize the rule field.");
        check(rule, input, expectedReturn, expectedOutput);
    }

    // ---------------------------------------------------------------------------------------------

    private void check (rule rule, String input, Object expectedReturn, String expectedOutput) {

        autumnFixture.rule = rule;
        ParseResult parseResult = autumnFixture.success(input);
        SighNode root = parseResult.topValue();

        Reactor reactor = new Reactor();
        Walker<SighNode> walker = SemanticAnalysis.createWalker(reactor);
        Interpreter interpreter = new Interpreter(reactor);
        walker.walk(root);
        reactor.run();
        Set<SemanticError> errors = reactor.errors();

        if (!errors.isEmpty()) {
            LineMapString map = new LineMapString("<test>", input);
            String report = reactor.reportErrors(it ->
                it.toString() + " (" + ((SighNode) it).span.startString(map) + ")");
            //            String tree = AttributeTreeFormatter.format(root, reactor,
            //                    new ReflectiveFieldWalker<>(SighNode.class, PRE_VISIT, POST_VISIT));
            //            System.err.println(tree);
            throw new AssertionError(report);
        }

        Pair<String, Object> result = IO.captureStdout(() -> interpreter.interpret(root));
        assertEquals(result.b, expectedReturn);
        if (expectedOutput != null) assertEquals(result.a, expectedOutput);
    }

    // ---------------------------------------------------------------------------------------------

    private void checkExpr (String input, Object expectedReturn, String expectedOutput) {
        rule = grammar.root;
        check("return " + input, expectedReturn, expectedOutput);
    }

    // ---------------------------------------------------------------------------------------------

    private void checkExpr (String input, Object expectedReturn) {
        rule = grammar.root;
        check("return " + input, expectedReturn);
    }

    // ---------------------------------------------------------------------------------------------

    private void checkThrows (String input, Class<? extends Throwable> expected) {
        assertThrows(expected, () -> check(input, null));
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testLiteralsAndUnary () {
        checkExpr("42", 42L);
        checkExpr("42.0", 42.0d);
        checkExpr("\"hello\"", "hello");
        checkExpr("(42)", 42L);
        checkExpr("[1, 2, 3]", new Object[]{1L, 2L, 3L});
        checkExpr("true", true);
        checkExpr("false", false);
        checkExpr("null", Null.INSTANCE);
        checkExpr("!false", true);
        checkExpr("!true", false);
        checkExpr("!!true", true);
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testNumericBinary () {
        checkExpr("1 + 2", 3L);
        checkExpr("2 - 1", 1L);
        checkExpr("2 * 3", 6L);
        checkExpr("2 / 3", 0L);
        checkExpr("3 / 2", 1L);
        checkExpr("2 % 3", 2L);
        checkExpr("3 % 2", 1L);

        checkExpr("1.0 + 2.0", 3.0d);
        checkExpr("2.0 - 1.0", 1.0d);
        checkExpr("2.0 * 3.0", 6.0d);
        checkExpr("2.0 / 3.0", 2d / 3d);
        checkExpr("3.0 / 2.0", 3d / 2d);
        checkExpr("2.0 % 3.0", 2.0d);
        checkExpr("3.0 % 2.0", 1.0d);

        checkExpr("1 + 2.0", 3.0d);
        checkExpr("2 - 1.0", 1.0d);
        checkExpr("2 * 3.0", 6.0d);
        checkExpr("2 / 3.0", 2d / 3d);
        checkExpr("3 / 2.0", 3d / 2d);
        checkExpr("2 % 3.0", 2.0d);
        checkExpr("3 % 2.0", 1.0d);

        checkExpr("1.0 + 2", 3.0d);
        checkExpr("2.0 - 1", 1.0d);
        checkExpr("2.0 * 3", 6.0d);
        checkExpr("2.0 / 3", 2d / 3d);
        checkExpr("3.0 / 2", 3d / 2d);
        checkExpr("2.0 % 3", 2.0d);
        checkExpr("3.0 % 2", 1.0d);

        checkExpr("2 * (4-1) * 4.0 / 6 % (2+1)", 1.0d);
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testOtherBinary () {
        checkExpr("true  && true",  true);
        checkExpr("true  || true",  true);
        checkExpr("true  || false", true);
        checkExpr("false || true",  true);
        checkExpr("false && true",  false);
        checkExpr("true  && false", false);
        checkExpr("false && false", false);
        checkExpr("false || false", false);

        checkExpr("1 + \"a\"", "1a");
        checkExpr("\"a\" + 1", "a1");
        checkExpr("\"a\" + true", "atrue");

        checkExpr("1 == 1", true);
        checkExpr("1 == 2", false);
        checkExpr("1.0 == 1.0", true);
        checkExpr("1.0 == 2.0", false);
        checkExpr("true == true", true);
        checkExpr("false == false", true);
        checkExpr("true == false", false);
        checkExpr("1 == 1.0", true);
        checkExpr("[1] == [1]", false);

        checkExpr("1 != 1", false);
        checkExpr("1 != 2", true);
        checkExpr("1.0 != 1.0", false);
        checkExpr("1.0 != 2.0", true);
        checkExpr("true != true", false);
        checkExpr("false != false", false);
        checkExpr("true != false", true);
        checkExpr("1 != 1.0", false);

        checkExpr("\"hi\" != \"hi2\"", true);
        checkExpr("[1] != [1]", true);

         // test short circuit
        checkExpr("true || print(\"x\") == \"y\"", true, "");
        checkExpr("false && print(\"x\") == \"y\"", false, "");
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testVarDecl () {
        check("var x: Int = 1; return x", 1L);
        check("var x: Float = 2.0; return x", 2d);

        check("var x: Int = 0; return x = 3", 3L);
        check("var x: String = \"0\"; return x = \"S\"", "S");

        // implicit conversions
        check("var x: Float = 1; x = 2; return x", 2.0d);
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testRootAndBlock () {
        rule = grammar.root;
        check("return", null);
        check("return 1", 1L);
        check("return 1; return 2", 1L);

        check("print(\"a\")", null, "a\n");
        check("print(\"a\" + 1)", null, "a1\n");
        check("print(\"a\"); print(\"b\")", null, "a\nb\n");

        check("{ print(\"a\"); print(\"b\") }", null, "a\nb\n");

        check(
            "var x: Int = 1;" +
            "{ print(\"\" + x); var x: Int = 2; print(\"\" + x) }" +
            "print(\"\" + x)",
            null, "1\n2\n1\n");
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testCalls () {
        check(
            "fun add (a: Int, b: Int): Int { return a + b } " +
                "return add(4, 7)",
            11L);

        HashMap<String, Object> point = new HashMap<>();
        point.put("x", 1L);
        point.put("y", 2L);

        check(
            "struct Point { var x: Int; var y: Int }" +
                "return $Point(1, 2)",
            point);

        check("var str: String = null; return print(str + 1)", "null1", "null1\n");
    }

    // ---------------------------------------------------------------------------------------------
    @Test public void testNullArray(){
        rule=grammar.root;
        check("return [].length", 0L);
        check("return [].avg", 0D);
        check("return [].count", 0L);
        check("return [].sum", 0L);
        check("return [].nDim", 1L);
    }

    @Test public void testArrayScalarOperation(){
        rule=grammar.root;
        check("return ([1,2,3]+2)[0]",3L);
        check("return ([1,2,3]+2)[1]",4L);
        check("return ([1,2,3]+2)[2]",5L);
        check("return ([[1],[2],[3]]+2)[0][0]",3L);
        check("return ([[1],[2],[3]]+2)[1][0]",4L);
        check("return (2+[1,2,3])[0]",3L);
        check("return (2+[1,2,3])[1]",4L);
        check("return (2+[1,2,3])[2]",5L);
        check("return (2+[[1],[2],[3]])[0][0]",3L);
        check("return (2+[[1],[2],[3]])[1][0]",4L);
        check("return ([1,2,3]*2)[0]",2L);
        check("return ([1,2,3]*2)[1]",4L);
        check("return ([1,2,3]*2)[2]",6L);
        check("return ([[1],[2],[3]]*2)[0][0]",2L);
        check("return ([[1],[2],[3]]*2)[1][0]",4L);
        check("return (2*[1,2,3])[0]",2L);
        check("return (2*[1,2,3])[1]",4L);
        check("return (2*[1,2,3])[2]",6L);
        check("return (2*[[1],[2],[3]])[0][0]",2L);
        check("return (2*[[1],[2],[3]])[1][0]",4L);
        check("return ([1,2,3]-2)[0]",-1L);
        check("return ([1,2,3]-2)[1]",0L);
        check("return ([1,2,3]-2)[2]",1L);
        check("return ([[1],[2],[3]]-2)[0][0]",-1L);
        check("return ([[1],[2],[3]]-2)[1][0]",0L);
        check("return (2-[1,2,3])[0]",1L);
        check("return (2-[1,2,3])[1]",0L);
        check("return (2-[1,2,3])[2]",-1L);
        check("return (2-[[1],[2],[3]])[0][0]",1L);
        check("return (2-[[1],[2],[3]])[1][0]",0L);
        check("return ([1,2,3]/2)[0]",0L);
        check("return ([1,2,3]/2)[1]",1L);
        check("return ([1,2,3]/2)[2]",1L);
        check("return ([[1],[2],[3]]/2)[0][0]",0L);
        check("return ([[1],[2],[3]]/2)[1][0]",1L);
        checkThrows("return (2/[1,2,3])[0]",InterpreterException.class);
        checkThrows("return (2/[1,2,3])[1]",InterpreterException.class);
        checkThrows("return (2/[1,2,3])[2]",InterpreterException.class);
        checkThrows("return (2/[[1],[2],[3]])[0][0]",InterpreterException.class);
        checkThrows("return (2/[[1],[2],[3]])[1][0]",InterpreterException.class);
        check("return ([1,2,3]%2)[0]",1L);
        check("return ([1,2,3]%2)[1]",0L);
        check("return ([1,2,3]%2)[2]",1L);
        check("return ([[1],[2],[3]]%2)[0][0]",1L);
        check("return ([[1],[2],[3]]%2)[1][0]",0L);
        checkThrows("return (2%[1,2,3])[0]",InterpreterException.class);
        checkThrows("return (2%[1,2,3])[1]",InterpreterException.class);
        checkThrows("return (2%[1,2,3])[2]",InterpreterException.class);
        checkThrows("return (2%[[1],[2],[3]])[0][0]",InterpreterException.class);
        checkThrows("return (2%[[1],[2],[3]])[1][0]",InterpreterException.class);

        check("var x: Int[2][1]; var y: Int=5;" +
            "x[0][0]=51;x[1][0]=32;" +
            "var z:Int[][]=x%y;" +
            "return z[1][0]", 2L);
        check("var x: Int[2][1]; var y: Int=5;" +
            "x[0][0]=51;x[1][0]=32;" +
            "var z:Int[][]=x+y;" +
            "return z[1][0]", 37L);
        check("var x: Int[2][1]; var y: Int=5;" +
            "x[0][0]=51;x[1][0]=32;" +
            "var z:Int[][]=x-y;" +
            "return z[1][0]", 27L);
        check("var x: Int[2][1]; var y: Int=5;" +
            "x[0][0]=51;x[1][0]=32;" +
            "var z:Int[][]=x*y;" +
            "return z[1][0]", 160L);
        check("var x: Int[2][1]; var y: Int=5;" +
            "x[0][0]=51;x[1][0]=32;" +
            "var z:Int[][]=x/y;" +
            "return z[1][0]", 6L);
        checkThrows("var x: Int[2][1]; var y: Int=5;" +
            "x[0][0]=51;x[1][0]=32;" +
            "var z:Int[][]=y/x;" +
            "return z[1][0]", InterpreterException.class);
        checkThrows("var x: Int[2][1]; var y: Int=5;" +
            "x[0][0]=51;x[1][0]=32;" +
            "var z:Int[][]=y%x;" +
            "return z[1][0]", InterpreterException.class);

    }

    @Test public void testClassDeclaration(){
        rule = grammar.root;

        check(
            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Float { return num/den } }"+
                " var p: Fraction = $Fraction(1, 2)" +
                "return p.num",
            1L);

        check(
            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Float { return num/den } }"+
                " var p: Fraction = $Fraction(1, 2)" +
                "return p.den",
            2L);

        check(
            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } }"+

                " var p: Fraction = $Fraction(2, 2)" +
                "return p.to_Number()",
            1L);
        check(
            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } }"+

                " var x: Fraction[] = [$Fraction(2, 2),$Fraction(1, 1)]" +
                "return x[0].num",
            2L);
        check(
            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } }"+

                " var x: Fraction[] = [$Fraction(2, 2),$Fraction(1, 1)]" +
                "return x[0].den",
            2L);

         check(
            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } }"+

                " var x: Fraction[] = [$Fraction(2, 2),$Fraction(1, 4)];" +
                " var y: Fraction[] = [$Fraction(2, 2),$Fraction(1, 1)];" +
                "return x[0].to_Number()",
            1L);

        check("class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } }"+
                "class InverseFraction { var num2: Int; var den2: Int " +
                " fun to_Number(): Int { return den2/num2 } }"+
                " var x: Fraction[] = [$Fraction(5, 2),$Fraction(1, 4)];" +
                " var y: InverseFraction[] = [$InverseFraction(5, 2),$InverseFraction(1, 1)];" +
                "return x[0].to_Number()==y[0].to_Number()",
            false);


         check(
            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den }" +
                " fun plus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) + (den*x.num),den+x.den);" +
                "   return y; " +
                "}" +
                "}" +
                "var p: Fraction = $Fraction(2, 2)" +
                "var o: Fraction = $Fraction(4,4)" +
                "var n: Fraction = p.plus(o)"+
                "return n.num",
            16L);


        check(
            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den }" +
                " fun plus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) + (den*x.num),den*x.den);" +
                "   return y; " +
                "}" +
                "}" +
                "var p: Fraction = $Fraction(2, 2)" +
                "var o: Fraction = $Fraction(4,4)" +
                "var n: Fraction = p.plus(o)"+
                "return n.den",
            8L);


          check(
              "class Fraction { var num: Int; var den: Int " +
                  " fun to_Number(): Int { return num/den } "+
                  " fun minus(x:Fraction) : Fraction { " +
                  "   var y: Fraction = $Fraction((num*x.den) - (den*x.num),den*x.den);" +
                  "   return y;}" +
                  " fun plus(x:Fraction) : Fraction { " +
                  "   var y: Fraction = $Fraction((num*x.den) + (den*x.num),den*x.den);" +
                  "   return y;}" +
                  " fun div(x:Fraction) : Fraction { " +
                  "   var y: Fraction = $Fraction(num*x.den,den*x.num);" +
                  "   return y;}" +
                  " fun mul(x:Fraction) : Fraction { " +
                  "   var y: Fraction = $Fraction(num*x.num,den*x.den);" +
                  "   return y;}" +
                  "} " +
                  " var x: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                  " var y: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                  " var z: Fraction[] = x+y; " +

                "return z[0].num",
            8L);
        check(

            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } "+
                " fun minus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) - (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun plus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) + (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun div(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.den,den*x.num);" +
                "   return y;}" +
                " fun mul(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.num,den*x.den);" +
                "   return y;}" +
                "} " +
                " var x: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var y: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var z: Fraction[] = x+y; " +


                "return z[0].den",
            4L);
        check(
            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } "+
                " fun minus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) - (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun plus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) + (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun div(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.den,den*x.num);" +
                "   return y;}" +
                " fun mul(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.num,den*x.den);" +
                "   return y;}" +
                "} " +
                " var x: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var y: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var z: Fraction[] = x+y; " +

                "return z[1].num",
            4L);
        check(

            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } "+
                " fun minus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) - (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun plus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) + (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun div(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.den,den*x.num);" +
                "   return y;}" +
                " fun mul(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.num,den*x.den);" +
                "   return y;}" +
                "} " +
                " var x: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var y: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var z: Fraction[] = x+y; " +

                "return z[1].den",
            1L);
        check(
            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } "+
                " fun minus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) - (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun plus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) + (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun div(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.den,den*x.num);" +
                "   return y;}" +
                " fun mul(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.num,den*x.den);" +
                "   return y;}" +
                "} " +
                " var x: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var y: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var z: Fraction[] = x-y; " +

                "return z[0].num",
            0L);
        check(

            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } "+
                " fun minus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) - (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun plus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) + (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun div(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.den,den*x.num);" +
                "   return y;}" +
                " fun mul(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.num,den*x.den);" +
                "   return y;}" +
                "} " +
                " var x: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var y: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var z: Fraction[] = x-y; " +


                "return z[0].den",
            4L);
        check(
            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } "+
                " fun minus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) - (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun plus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) + (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun div(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.den,den*x.num);" +
                "   return y;}" +
                " fun mul(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.num,den*x.den);" +
                "   return y;}" +
                "} " +
                " var x: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var y: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var z: Fraction[] = x-y; " +

                "return z[1].num",
            0L);
        check(

            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } "+
                " fun minus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) - (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun plus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) + (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun div(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.den,den*x.num);" +
                "   return y;}" +
                " fun mul(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.num,den*x.den);" +
                "   return y;}" +
                "} " +
                " var x: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var y: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var z: Fraction[] = x-y; " +

                "return z[1].den",
            1L);
        check(
            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } "+
                " fun minus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) - (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun plus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) + (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun div(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.den,den*x.num);" +
                "   return y;}" +
                " fun mul(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.num,den*x.den);" +
                "   return y;}" +
                "} " +
                " var x: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var y: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var z: Fraction[] = x*y; " +

                "return z[0].num",
            4L);
        check(

            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } "+
                " fun minus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) - (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun plus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) + (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun div(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.den,den*x.num);" +
                "   return y;}" +
                " fun mul(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.num,den*x.den);" +
                "   return y;}" +
                "} " +
                " var x: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var y: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var z: Fraction[] = x*y; " +


                "return z[0].den",
            4L);
        check(
            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } "+
                " fun minus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) - (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun plus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) + (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun div(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.den,den*x.num);" +
                "   return y;}" +
                " fun mul(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.num,den*x.den);" +
                "   return y;}" +
                "} " +
                " var x: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var y: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var z: Fraction[] = x*y; " +

                "return z[1].num",
            4L);
        check(

            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } "+
                " fun minus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) - (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun plus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) + (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun div(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.den,den*x.num);" +
                "   return y;}" +
                " fun mul(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.num,den*x.den);" +
                "   return y;}" +
                "} " +
                " var x: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var y: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var z: Fraction[] = x*y; " +

                "return z[1].den",
            1L);
        check(
            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } "+
                " fun minus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) - (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun plus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) + (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun div(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.den,den*x.num);" +
                "   return y;}" +
                " fun mul(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.num,den*x.den);" +
                "   return y;}" +
                "} " +
                " var x: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var y: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var z: Fraction[] = x/y; " +

                "return z[0].num",
            4L);
        check(

            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } "+
                " fun minus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) - (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun plus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) + (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun div(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.den,den*x.num);" +
                "   return y;}" +
                " fun mul(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.num,den*x.den);" +
                "   return y;}" +
                "} " +
                " var x: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var y: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var z: Fraction[] = x/y; " +


                "return z[0].den",
            4L);
        check(
            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } "+
                " fun minus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) - (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun plus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) + (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun div(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.den,den*x.num);" +
                "   return y;}" +
                " fun mul(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.num,den*x.den);" +
                "   return y;}" +
                "} " +
                " var x: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var y: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var z: Fraction[] = x/y; " +

                "return z[1].num",
            2L);
        check(

            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } "+
                " fun minus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) - (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun plus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) + (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun div(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.den,den*x.num);" +
                "   return y;}" +
                " fun mul(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.num,den*x.den);" +
                "   return y;}" +
                "} " +
                " var x: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var y: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var z: Fraction[] = x/y; " +

                "return z[1].den",
            2L);

        check(

            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } "+
                " fun minus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) - (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun plus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) + (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun div(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.den,den*x.num);" +
                "   return y;}" +
                " fun mul(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.num,den*x.den);" +
                "   return y;}" +
                " fun modulo(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num%x.den,den%x.num);" +
                "   return y;}" +
                "} " +
                " var x: Fraction[] = [$Fraction(3, 2),$Fraction(2, 1)];" +
                " var y: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var z: Fraction[] = x%y; " +
                "return z[0].num",
            1L);
        check(
            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } "+
                " fun minus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) - (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun plus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) + (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun div(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.den,den*x.num);" +
                "   return y;}" +
                " fun mul(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.num,den*x.den);" +
                "   return y;}" +
                " fun modulo(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num%x.den,den%x.num);" +
                "   return y;}" +
                "} " +
                " var x: Fraction[] = [$Fraction(3, 2),$Fraction(2, 1)];" +
                " var y: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var z: Fraction[] = x%y; " +
                "return z.sum",
            0L);
        check(
            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } "+
                " fun minus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) - (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun plus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) + (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun div(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.den,den*x.num);" +
                "   return y;}" +
                " fun mul(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.num,den*x.den);" +
                "   return y;}" +
                " fun modulo(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num%x.den,den%x.num);" +
                "   return y;}" +
                "} " +
                " var x: Fraction[] = [$Fraction(3, 2),$Fraction(2, 1)];" +
                " var y: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var z: Fraction[] = x%y; " +
                "return z.count",
            2L);
        check(
            "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } "+
                " fun minus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) - (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun plus(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction((num*x.den) + (den*x.num),den*x.den);" +
                "   return y;}" +
                " fun div(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.den,den*x.num);" +
                "   return y;}" +
                " fun mul(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num*x.num,den*x.den);" +
                "   return y;}" +
                " fun modulo(x:Fraction) : Fraction { " +
                "   var y: Fraction = $Fraction(num%x.den,den%x.num);" +
                "   return y;}" +
                "} " +
                " var x: Fraction[] = [$Fraction(3, 2),$Fraction(2, 1)];" +
                " var y: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var z: Fraction[] = x%y; " +
                "return z.avg",
            0D);
         checkThrows(
                    "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } "+
                    "} " +
                " var x: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var y: Fraction[] = [$Fraction(2, 2),$Fraction(2, 1)];" +
                " var z: Fraction[] = x+y; " +
                "return z[0].num",
             InterpreterException.class);

        checkThrows(
            "var num : Int = 12;" +
                "class Fraction { var num: Int; var den: Int " +
                " fun to_Number(): Int { return num/den } }"+

                " var x: Fraction[] = [$Fraction(2, 2),$Fraction(1, 1)];" +
                "print(\"\"+num);" +
                "return x[1].num;",AssertionError.class);

    }


    @Test
    public void testArrayStructAccess () {
        checkExpr("[1][0]", 1L);
        checkExpr("[1.0][0]", 1d);
        checkExpr("[1, 2][1]", 2L);

        checkExpr("[1].length", 1L);
        checkExpr("[1, 2].length", 2L);
        checkExpr("[].sum",0L);
        checkExpr("[].avg",0D);
        checkExpr("[].count",0L);
        checkExpr("[].nDim",1L);
        checkExpr("[[]].nDim",2L);
        checkExpr("[4, 2].avg", 3D);
        checkExpr("[1, 3, 4, 2].count", 4L);
        checkExpr("[4, 2].sum", 6L);
        checkExpr("[4, 2].nDim", 1L);
        checkExpr("[1, 3, 4, 2].nDim",1L);
        checkExpr("[[4, 2],[1,3]].nDim", 2L);
        checkExpr("[[4, 2],[1,3]].sum", 10L);
        checkExpr("[[4, 2],[1,3]].avg", 2.5D);
        checkExpr("[[4, 2],[1,3]].count", 4L);
        checkExpr("[[4, 2],[3]].count", 3L);
        checkExpr("[[[4, 2],[1,3]],[[1,1],[2,2]]].nDim", 3L);
        checkExpr("[[[4, 2],[1,3]],[[1,1],[2,2]]].sum", 16L);
        checkExpr("[[[4, 2],[1,3]],[[1,1],[2,2]]].avg", 2D);
        checkExpr("[[[4, 2],[1,3]],[[1,1],[2,2]]].count", 8L);
        checkExpr("[[[4, 2],[1,3]],[[1,1]]].count", 6L);

        checkExpr("([4, 2]+[1, 1])[0]", 5L );
        checkExpr("([4, 2]+[1, 1])[1]", 3L );
        checkExpr("([4, 2]*[2, 3])[0]", 8L );
        checkExpr("([4, 2]*[2, 3])[1]", 6L );
        checkExpr("([4, 2]-[1, 1])[0]", 3L );
        checkExpr("([4, 2]-[1, 1])[1]", 1L );
        checkExpr("([4, 2]/[1, 1])[0]", 4L );
        checkExpr("([3, 2]/[2, 1])[0]", 1L );
        checkExpr("([4, 2]%[3, 3])[0]", 1L );
        checkExpr("([4, 2]%[3, 3])[1]", 2L );
        checkExpr("([3.0, 2.0]/[2.0, 1.0])[0]", 1.5d );

        checkThrows("return ([4, 2]/[0, 1])[1]", InterpreterException.class );

        checkExpr("[[1, 2, 3], [4, 5, 6]][0][1]", 2L);


        checkExpr("[[[1], [2], [3]], [[4], [5], [6]]][1][2][0]",6L);

        checkThrows("var array: Int[] = null; return array[0]", NullPointerException.class);
        checkThrows("var array: Int[] = null; return array.length", NullPointerException.class);

        check("var x: Int[] = [0, 1]; x[0] = 3; return x[0]", 3L);
        checkThrows("var x: Int[] = []; x[0] = 3; return x[0]",
            ArrayIndexOutOfBoundsException.class);
        checkThrows("var x: Int[] = null; x[0] = 3",
            NullPointerException.class);

        check(
            "struct P { var x: Int; var y: Int }" +
                "return $P(1, 2).y",
            2L);

        checkThrows(
            "struct P { var x: Int; var y: Int }" +
                "var p: P = null;" +
                "return p.y",
            NullPointerException.class);

        check(
            "struct P { var x: Int; var y: Int }" +
                "var p: P = $P(1, 2);" +
                "p.y = 42;" +
                "return p.y",
            42L);

        checkThrows(
            "struct P { var x: Int; var y: Int }" +
                "var p: P = null;" +
                "p.y = 42",
            NullPointerException.class);
    }

    @Test public void dummyTest(){
        rule = grammar.root;

    }

    @Test public void testArrayDeclaration(){
        rule = grammar.root;
        //check("var x:Int=3; return x", 3L);
        check("var x: Int[2]; return x[0]",0L);
        check("var x: String[2][2]; return x[0][1]", null);
        check("var x: Int[1][2][3]; return x[0][0][0]", 0L);
        check("var x: Int[1][2][3]; x[0][0][0]=3; return x[0][0][0]", 3L);
        check("var x: Int[2]; var y: Int[2];" +
            "x[0]=1;x[1]=1;" +
            "y[0]=4;y[1]=5;" +
            "var z:Int[]=x+y;" +
            "return z[0]", 5L);
        check("var x: Int[2]; var y: Int[2];" +
            "x[0]=1;x[1]=1;" +
            "y[0]=4;y[1]=5;" +
            "var z:Int[]=x+y;" +
            "return z[1]", 6L);
        check("var x: Int[2]; var y: Int[2];" +
            "x[0]=1;x[1]=1;" +
            "y[0]=4;y[1]=5;" +
            "var z:Int[]=x-y;" +
            "return z[0]", -3L);
        check("var x: Int[2]; var y: Int[2];" +
            "x[0]=1;x[1]=1;" +
            "y[0]=4;y[1]=5;" +
            "var z:Int[]=x-y;" +
            "return z[1]", -4L);
        check("var x: Int[2]; var y: Int[2];" +
            "x[0]=1;x[1]=2;" +
            "y[0]=4;y[1]=5;" +
            "var z:Int[]=x*y;" +
            "return z[0]", 4L);
        check("var x: Int[2]; var y: Int[2];" +
            "x[0]=1;x[1]=2;" +
            "y[0]=4;y[1]=5;" +
            "var z:Int[]=x*y;" +
            "return z[1]", 10L);
        checkThrows("var x: Int[2]; var y: Int[2];" +
            "x[0]=1;x[1]=1;" +
            "y[0]=4;y[1]=0;" +
            "var z:Int[]=x/y;", InterpreterException.class);
        checkThrows("var x: Int[2]; var y: Float[2];" +
        "x[0]=1;x[1]=1;" +
            "y[0]=4.0;y[1]=5.0;" +
            "var z:Int[]=x+y;" +
            "return z[1]", InterpreterException.class);

        check("var x: Float[2]; var y: Float[2];" +
            "x[0]=1.5;x[1]=1.4;" +
            "y[0]=4.0;y[1]=5.0;" +
            "var z:Float[]=x+y;" +
            "return z[0]", 5.5D);
        check("var x: Float[2]; var y: Float[2];" +
            "x[0]=1.5;x[1]=1.4;" +
            "y[0]=4.0;y[1]=5.0;" +
            "var z:Float[]=x+y;" +
            "return z[1]", 6.4D);
        check("var x: Float[2]; var y: Float[2];" +
            "x[0]=1.5;x[1]=1.4;" +
            "y[0]=4.0;y[1]=5.0;" +
            "var z:Float[]=x-y;" +
            "return z[0]", -2.5D);
        check("var x: Float[2]; var y: Float[2];" +
            "x[0]=1.5;x[1]=1.4;" +
            "y[0]=4.0;y[1]=5.0;" +
            "var z:Float[]=x-y;" +
            "return z[1]", -3.6D);
        check("var x: Float[2]; var y: Float[2];" +
            "x[0]=1.5;x[1]=2.3;" +
            "y[0]=4.0;y[1]=5.0;" +
            "var z:Float[]=x*y;" +
            "return z[0]", 6D);
        check("var x: Float[2]; var y: Float[2];" +
            "x[0]=1.5;x[1]=2.3;" +
            "y[0]=4.0;y[1]=5.0;" +
            "var z:Float[]=x*y;" +
            "return z[1]", 11.5D);
        checkThrows("var x: Float[2]; var y: Float[2];" +
            "x[0]=1.5;x[1]=1.4;" +
            "y[0]=4.0;y[1]=0.0;" +
            "var z:Float[]=x/y;", InterpreterException.class);

        check("var x: Int[2][1]; var y: Int[2][1];" +
            "x[0][0]=1;x[1][0]=1;" +
            "y[0][0]=4;y[1][0]=5;" +
            "var z:Int[][]=x+y;" +
            "return z[0][0]", 5L);
        check("var x: Int[2][1]; var y: Int[2][1];" +
            "x[0][0]=1;x[1][0]=1;" +
            "y[0][0]=4;y[1][0]=5;" +
            "var z:Int[][]=x+y;" +
            "return z[1][0]", 6L);
       check("var x: Int[2][1]; var y: Int[2][1];" +
            "x[0][0]=1;x[1][0]=1;" +
            "y[0][0]=4;y[1][0]=5;" +
            "var z:Int[][]=x-y;" +
            "return z[0][0]", -3L);
        check("var x: Int[2][1]; var y: Int[2][1];" +
            "x[0][0]=1;x[1][0]=1;" +
            "y[0][0]=4;y[1][0]=5;" +
            "var z:Int[][]=x/y;" +
            "return z[0][0]", 0L);
        checkThrows("var x: Int[2][1]; var y: Int[2][1];" +
            "x[0][0]=1;x[1][0]=1;" +
            "y[0][0]=0;y[1][0]=5;" +
            "var z:Int[][]=x/y;" +
            "return z[0][0]", InterpreterException.class);
        check("var x: Int[2][1]; var y: Int[2][1];" +
            "x[0][0]=1;x[1][0]=1;" +
            "y[0][0]=4;y[1][0]=5;" +
            "var z:Int[][]=x-y;" +
            "return z[1][0]", -4L);
        check("var x: Int[2][1]; var y: Int[2][1];" +
            "x[0][0]=1;x[1][0]=1;" +
            "y[0][0]=4;y[1][0]=5;" +
            "var z:Int[][]=x*y;" +
            "return z[0][0]", 4L);
        check("var x: Int[2][1]; var y: Int[2][1];" +
            "x[0][0]=1;x[1][0]=2;" +
            "y[0][0]=4;y[1][0]=5;" +
            "var z:Int[][]=x*y;" +
            "return z[1][0]", 10L);
        check("var x: Int[2][2][2]; var y: Int[2][2][2];" +
            "x[0][0][0]=1;x[1][0][0]=2;x[0][0][1]=3;x[1][0][1]=4;" +
            "x[0][1][0]=5;x[1][1][0]=6;x[0][1][1]=7;x[1][1][1]=8;"+
            "y[0][0][0]=1;y[1][0][0]=3;y[0][0][1]=5;y[1][0][1]=7;" +
            "y[0][1][0]=9;y[1][1][0]=11;y[0][1][1]=13;y[1][1][1]=15;"+
            "var z:Int[][][]=x*y;" +
            "return z[0][0][0]", 1L);
        check("var x: Int[2][2][2]; var y: Int[2][2][2];" +
            "x[0][0][0]=1;x[1][0][0]=2;x[0][0][1]=3;x[1][0][1]=4;" +
            "x[0][1][0]=5;x[1][1][0]=6;x[0][1][1]=7;x[1][1][1]=8;"+
            "y[0][0][0]=1;y[1][0][0]=3;y[0][0][1]=5;y[1][0][1]=7;" +
            "y[0][1][0]=9;y[1][1][0]=11;y[0][1][1]=13;y[1][1][1]=15;"+
            "var z:Int[][][]=x*y;" +
            "return z[0][0][1]", 15L);
        check("var x: Int[2][2][2]; var y: Int[2][2][2];" +
            "x[0][0][0]=1;x[1][0][0]=2;x[0][0][1]=3;x[1][0][1]=4;" +
            "x[0][1][0]=5;x[1][1][0]=6;x[0][1][1]=7;x[1][1][1]=8;"+
            "y[0][0][0]=1;y[1][0][0]=3;y[0][0][1]=5;y[1][0][1]=7;" +
            "y[0][1][0]=9;y[1][1][0]=11;y[0][1][1]=13;y[1][1][1]=15;"+
            "var z:Int[][][]=x*y;" +
            "return z[0][1][1]", 91L);
        check("var x: Int[2][2][2]; var y: Int[2][2][2];" +
            "x[0][0][0]=1;x[1][0][0]=2;x[0][0][1]=3;x[1][0][1]=4;" +
            "x[0][1][0]=5;x[1][1][0]=6;x[0][1][1]=7;x[1][1][1]=8;"+
            "y[0][0][0]=1;y[1][0][0]=3;y[0][0][1]=5;y[1][0][1]=7;" +
            "y[0][1][0]=9;y[1][1][0]=11;y[0][1][1]=13;y[1][1][1]=15;"+
            "var z:Int[][][]=x+y;" +
            "return z[0][1][1]", 20L);

        check("var x: Int[2][2][2]; var y: Int[2][2][2];" +
            "x[0][0][0]=1;x[1][0][0]=2;x[0][0][1]=3;x[1][0][1]=4;" +
            "x[0][1][0]=5;x[1][1][0]=6;x[0][1][1]=7;x[1][1][1]=8;"+
            "y[0][0][0]=1;y[1][0][0]=3;y[0][0][1]=5;y[1][0][1]=7;" +
            "y[0][1][0]=9;y[1][1][0]=11;y[0][1][1]=13;y[1][1][1]=15;"+
            "var z:Int[][][]=x-y;" +
            "return z[0][1][1]", -6L);

        check("var x: Int[2][2][2]; var y: Int[2][2][2];" +
            "x[0][0][0]=1;x[1][0][0]=2;x[0][0][1]=3;x[1][0][1]=4;" +
            "x[0][1][0]=5;x[1][1][0]=6;x[0][1][1]=7;x[1][1][1]=8;"+
            "y[0][0][0]=1;y[1][0][0]=3;y[0][0][1]=5;y[1][0][1]=7;" +
            "y[0][1][0]=9;y[1][1][0]=11;y[0][1][1]=13;y[1][1][1]=15;"+
            "var z:Int[][][]=y/x;" +
            "return z[0][1][1]", 1L);
        check("var x: Int[3][2]; var y: Int[2][2];" +
            "x[0][0]=1;x[1][0]=2;" +
            "y[0][0]=4;y[1][0]=5;" +
            "var z:Int[][]=x@y;" +
            "return z[1][0]", 8L);

        check("var x: Int [3][4]"+
            "x[0][0] = 30;x[1][1] = 21;x[2][2] = 78;x[2][3] = 45;"+
            "var y: Int [][] = [[1,2,3,4,5],[5,4,3,2,1],[5,10,15,20,25],[1,3,5,7,11]]"+
            "var z: Int [][] = x @ y "+
            "return z[0][0]",30L);

        check("var x: Int[2]; x[0]=2;x[1]=4;" +
            "return x.sum", 6L);

        check("var x: Int[2]; x[0]=2;x[1]=4;" +
            "return x.avg", 3.0D);

        check("var x: Int[2]; x[0]=2;x[1]=4;" +
            "return x.count", 2L);

        check("var x: Int[]=[2,4]" +
            "return x.sum", 6L);

        check("var x: Int[]=[2,4]" +
            "return x.avg", 3.0D);

        check("var x: Int[]=[2,4]" +
            "return x.count", 2L);

        check("var x: Float[2]; x[0]=2.5;x[1]=4.3;" +
            "return x.sum", 6.8D);

        check("var x: Float[2]; x[0]=2.5;x[1]=4.3;" +
            "return x.avg", 3.4D);

        check("var x: Float[2]; x[0]=2.5;x[1]=4.3;" +
            "return x.count", 2L);

        check("var x: Float[]=[2.5,4.3]" +
            "return x.sum", 6.8D);

        check("var x: Float[]=[2.5,4.3]" +
            "return x.avg", 3.4D);

        check("var x: Float[]=[2.5,4.3]" +
            "return x.count", 2L);

        check("var x: String[3]" +
            "return x.count", 3L);
        check("var x: String[3]" +
            "return x.sum", 0L);
        check("var x: String[3]" +
            "return x.avg", 0D);

    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testIfWhile () {
        check("if (true) return 1 else return 2", 1L);
        check("if (false) return 1 else return 2", 2L);
        check("if (false) return 1 else if (true) return 2 else return 3 ", 2L);
        check("if (false) return 1 else if (false) return 2 else return 3 ", 3L);

        check("var i: Int = 0; while (i < 3) { print(\"\" + i); i = i + 1 } ", null, "0\n1\n2\n");
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testInference () {
        check("var array: Int[] = []", null);
        check("var array: String[] = []", null);
        check("fun use_array (array: Int[]) {} ; use_array([])", null);
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testTypeAsValues () {
        check("struct S{} ; return \"\"+ S", "S");
        check("struct S{} ; var type: Type = S ; return \"\"+ type", "S");
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testUnconditionalReturn()
    {
        rule = grammar.root;
        check("fun f(): Int { if (true) return 1 else return 2 } ; return f()", 1L);
    }

    // ---------------------------------------------------------------------------------------------

    // NOTE(norswap): Not incredibly complete, but should cover the basics.
}
