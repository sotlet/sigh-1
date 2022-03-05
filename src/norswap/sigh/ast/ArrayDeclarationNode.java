package norswap.sigh.ast;

import norswap.autumn.positions.Span;
import norswap.utils.Util;

public class ArrayDeclarationNode extends DeclarationNode{
    public final String name;
    public final TypeNode type;
    public final ExpressionNode initializer;

    public ArrayDeclarationNode(Span span, Object name, Object type, Object initializer) {
        super(span);
        this.name = Util.cast(name, String.class);
        this.type = Util.cast(type, TypeNode.class);
        this.initializer = Util.cast(initializer, ExpressionNode.class);
    }

    @Override public String name () {
        return name;
    }

    @Override public String contents () {
        return "Array " + name;
    }

    @Override public String declaredThing () {
        return "Array";
    }
}
