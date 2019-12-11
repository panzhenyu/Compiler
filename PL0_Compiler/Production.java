package PL0_Compiler;

import java.util.Arrays;

public class Production
{
    private final static int MAX_PROD_NUM;
    Vn s;
    Object[] p;                                     // 产生式是包含终结符和非终结符的串

    static {
        MAX_PROD_NUM = 1000000;
    }

    private Production(Vn s, Object... p)
    {
        this.s = s;
        this.p = p.clone();
    }

    public String toString()
    {
        StringBuilder r = new StringBuilder(s.toString() + " ");
        for(Object o : p) {
            r.append(o);
            r.append(" ");
        }
        r.deleteCharAt(r.length() - 1);
        return r.toString();
    }

    public static Production[] PL0Production()
    {
        Production[] prods = new Production[MAX_PROD_NUM];
        int idx = 0;
        /* 过程 */
        prods[idx++] = new Production(Vn.Program,
                Vn.Procedure, Symbol.period);
        // 常量声明
        prods[idx++] = new Production(Vn.Procedure,
                Symbol.constsym, Vn.ConstDeclaration, Vn.NextConst, Symbol.semicolon, Vn.Procedure);
        prods[idx++] = new Production(Vn.ConstDeclaration,
                Symbol.ident, Symbol.eql, Symbol.number);
        prods[idx++] = new Production(Vn.NextConst,
                Symbol.comma, Vn.ConstDeclaration, Vn.NextConst);
        prods[idx++] = new Production(Vn.NextConst,
                Symbol.nul);
        // 变量声明
        prods[idx++] = new Production(Vn.Procedure,
                Symbol.varsym, Vn.VarDeclaration, Symbol.semicolon, Vn.Procedure);
//        prods[idx++] = new Production(Vn.Procedure,
//                Symbol.varsym, Vn.VarDeclaration, Vn.NextVar, Symbol.semicolon, Vn.Procedure);
//        prods[idx++] = new Production(Vn.VarDeclaration,
//                Symbol.ident);
//        prods[idx++] = new Production(Vn.NextVar,
//                Symbol.comma, Vn.VarDeclaration, Vn.NextVar);
//        prods[idx++] = new Production(Vn.NextVar,
//                Symbol.nul);
        // 过程声明
        prods[idx++] = new Production(Vn.Procedure,
                Symbol.procsym, Symbol.ident, Symbol.semicolon, Vn.ChildProc, Symbol.semicolon, Vn.Procedure);  // e
        // 语句序列
        prods[idx++] = new Production(Vn.Procedure,
                Vn.Sequence);

        /* 语句序列 */
        prods[idx++] = new Production(Vn.Sequence,
                Vn.Statement, Vn.NextStatement);
        prods[idx++] = new Production(Vn.NextStatement,
                Symbol.semicolon, Vn.Statement);
        prods[idx++] = new Production(Vn.NextStatement,
                Symbol.nul);
        // 读与写
        prods[idx++] = new Production(Vn.Statement,
                Symbol.readsym, Symbol.lparen, Symbol.ident, Symbol.rparen);
        prods[idx++] = new Production(Vn.Statement,
                Symbol.writesym, Symbol.lparen, Symbol.ident, Symbol.rparen);
        // 变量赋值
        prods[idx++] = new Production(Vn.Statement,
                Symbol.ident, Symbol.becomes, Vn.Expression);
        // 过程调用
        prods[idx++] = new Production(Vn.Statement,
                Symbol.callsym, Symbol.ident);
        // 子语句序列
        prods[idx++] = new Production(Vn.Statement,
                Symbol.beginsym, Vn.Sequence, Symbol.endsym);
        // 条件转移
        prods[idx++] = new Production(Vn.Statement,
                Symbol.ifsym, Vn.RelationExp, Symbol.thensym, Vn.Statement);
        // while循环
        prods[idx++] = new Production(Vn.Statement,
                Symbol.whilesym, Vn.RelationExp, Symbol.dosym, Vn.Statement);

        /* 条件 */
        prods[idx++] = new Production(Vn.RelationExp,
                Symbol.oddsym, Vn.Expression);
        prods[idx++] = new Production(Vn.RelationExp,
                Vn.Expression, Vn.R, Vn.Expression);
        prods[idx++] = new Production(Vn.R, Symbol.eql);
        prods[idx++] = new Production(Vn.R, Symbol.neq);
        prods[idx++] = new Production(Vn.R, Symbol.lss);
        prods[idx++] = new Production(Vn.R, Symbol.gtr);
        prods[idx++] = new Production(Vn.R, Symbol.leq);
        prods[idx++] = new Production(Vn.R, Symbol.geq);

        /* 表达式 */
        prods[idx++] = new Production(Vn.Expression,
                Symbol.plus, Vn.Item, Vn.ExpItem);
        prods[idx++] = new Production(Vn.Expression,
                Symbol.minus, Vn.Item, Vn.ExpItem);
        prods[idx++] = new Production(Vn.Expression,
                Vn.Item, Vn.ExpItem);
        // 子项为空
        prods[idx++] = new Production(Vn.ExpItem,
                Symbol.nul);
        // 子项为+项
        prods[idx++] = new Production(Vn.ExpItem,
                Symbol.plus, Vn.Item);
        // 子项为-项
        prods[idx++] = new Production(Vn.ExpItem,
                Symbol.minus, Vn.Item);

        /* 项 */
        prods[idx++] = new Production(Vn.Item,
                Vn.Factor, Vn.NextFact);
        // 子因子为空
        prods[idx++] = new Production(Vn.NextFact,
                Symbol.nul);
        // 子因子为*因子
        prods[idx++] = new Production(Vn.NextFact,
                Symbol.times, Vn.Factor);
        // 子因子为/因子
        prods[idx++] = new Production(Vn.NextFact,
                Symbol.slash, Vn.Factor);

        /* 因子 */
        prods[idx++] = new Production(Vn.Factor,
                Symbol.ident);
        prods[idx++] = new Production(Vn.Factor,
                Symbol.number);
        prods[idx++] = new Production(Vn.Factor,
                Symbol.lparen, Vn.Expression, Symbol.rparen);
        return Arrays.copyOf(prods, idx);
    }
}
