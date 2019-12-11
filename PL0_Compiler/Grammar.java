package PL0_Compiler;

import java.util.*;

public class Grammar
{
    private Vn S;                                       // 语法的起始状态
    private Production[] P;                             // 语法的产生式
    private Symbol[][] first, follow;                   // 非终结符的first集和follow集
    private int[][] select;                             // 由非终结符与读入的终结符选择的产生式id
    private final int ILLEGAL;                          // 因无合适产生式可选所返回的非法值

    @SuppressWarnings("all")
    public Grammar()
    {
        S = Vn.Program;
        P = Production.PL0Production();
        first = new Symbol[Vn.values().length][];
        follow = new Symbol[Vn.values().length][];
        select = new int[Vn.values().length][Symbol.values().length];
        ILLEGAL = -1;
        for(int[] si : select)
            Arrays.fill(si, ILLEGAL);
        calcSelectSet();
    }

    public Vn getS()
    {
        return S;
    }

    private void calcSelectSet() {
        for(Vn vn : Vn.values())
            calcFirstSet(vn);
        for(Vn vn : Vn.values())
            calcFollowSet(vn);
        Symbol[] selected; int sid; Production prod;
        for(int i=0;i<P.length;i++) {
            prod = P[i];
            selected = calcSelectSet(prod); sid = prod.s.ordinal();
            for(Symbol vt : selected)
                select[sid][vt.ordinal()] = i;
        }
    }

    /**
     * 计算非终结符的first集
     * @param vn 非终结符
     */
    private void calcFirstSet(Vn vn)
    {
        int vnid = vn.ordinal(); Object[] p; int reallen = 0;
        if(first[vnid] == null) {
            first[vnid] = new Symbol[Symbol.values().length];
            for(Production prod : P) {
                if(prod.s != vn)
                    continue;
                p = prod.p;
                if(p[0] instanceof Symbol) {
                    if(notContains(first[vnid], (Symbol)p[0]))
                        first[vnid][reallen++] = (Symbol)p[0];
                } else {
                    int j; Vn var; int varid; boolean havenul;
                    for(j=0;j<p.length;j++) {
                        if(p[j] instanceof Symbol) {
                            if(notContains(first[vnid], (Symbol)p[j]))
                                first[vnid][reallen++] = (Symbol)p[j];
                            break;
                        }
                        var = (Vn)p[j]; varid = var.ordinal(); havenul = false;
                        if(var == vn)
                            throw new RuntimeException(var + " has already existed, " + "isn't a LL_1 grammar");
                        else if(first[varid] == null)
                            calcFirstSet(var);
                        for(int k=0;k<first[varid].length;k++) {
                            if(first[varid][k] != Symbol.nul) {
                                if(notContains(first[vnid], first[varid][k]))
                                    first[vnid][reallen++] = first[varid][k];
                            } else
                                havenul = true;
                        }
                        if(!havenul)
                            break;
                    }
                    if(j == p.length && notContains(first[vnid], Symbol.nul))
                        first[vnid][reallen++] = Symbol.nul;
                }
            }
            first[vnid] = Arrays.copyOf(first[vnid], reallen);
        }
    }

    /**
     * 计算非终结符的follow集
     * @param vn 非终结符
     */
    private void calcFollowSet(Vn vn)
    {
        int vnid = vn.ordinal();
        if(follow[vnid] == null) {
            follow[vnid] = new Symbol[Symbol.values().length]; int reallen = 0;
            if(vn == S)                                                             // 给起始符号的follow集添加#
                follow[vnid][reallen++] = Symbol.period;
            for(Production prod : P) {
                // 对每个产生式，找vn的位置，将vn后继符号的first集去除nul后添入vn的follow集，直到后继符号的first集不含nul
                int i = 0, varid; boolean havenul; Object[] p = prod.p; Vn var;
                for(;i<p.length;i++) {
                    if(p[i].equals(vn))
                        break;
                }
                if(i == p.length)                                                   // 没找到该非终结符则跳过该规则
                    continue;
                i++;
                for(;i<p.length;i++) {
                    // 否则遍历其后继符号
                    if(p[i] instanceof Symbol) {
                        if(notContains(follow[vnid], (Symbol)p[i]))
                            follow[vnid][reallen++] = (Symbol)p[i];
                        break;
                    }
                    var = (Vn)p[i]; varid = var.ordinal(); havenul = false;
                    for(Symbol vt : first[varid]) {
                        if (vt != Symbol.nul) {
                            if (notContains(follow[vnid], vt))
                                follow[vnid][reallen++] = vt;
                        } else
                            havenul = true;
                    }
                    if(!havenul)
                        break;
                }
                // 若所有后继符号的first集都含nul，则将生成产生式的非终结符的follow集添入该非终结符的follow集
                // 特别地，若生成产生式的非终结符是当前终结符，则不做任何操作
                if(i == p.length && prod.s != vn) {
                    var = prod.s; varid = var.ordinal();
                    if (follow[varid] == null)
                        calcFollowSet(var);
                    for (Symbol vt : follow[varid])
                        if (notContains(follow[vnid], vt))
                            follow[vnid][reallen++] = vt;
                }
            }
            follow[vnid] = Arrays.copyOf(follow[vnid], reallen);
        }
    }

    /**
     * 计算产生式的select集
     * @param prod 产生式
     * @return 产生式的select集
     */
    private Symbol[] calcSelectSet(Production prod)
    {
        Object[] p = prod.p; Vn s = prod.s; Symbol[] ret = new Symbol[Symbol.values().length];
        int i = 0, vnid, reallen = 0; boolean havenul;
        do {
            havenul = false;
            if(p[i] instanceof Symbol) {
                if(p[i] == Symbol.nul)
                    havenul = true;
                else if(notContains(ret, (Symbol)p[i]))
                    ret[reallen++] = (Symbol)p[i];
                break;
            }
            vnid = ((Vn)p[i]).ordinal();
            for(Symbol vt : first[vnid]) {
                if (vt != Symbol.nul) {
                    if(notContains(ret, vt))
                        ret[reallen++] = vt;
                } else
                    havenul = true;
            }
        } while(havenul && ++i<p.length);
        // 若所有的符号都是非终结符，且其first集都含有nul，则将生成产生式的非终结符的follow集加入该产生式的select集
        if(havenul) {
            for(Symbol vt : follow[s.ordinal()]) {
                if (notContains(ret, vt))
                    ret[reallen++] = vt;
            }
        }
        ret = Arrays.copyOf(ret, reallen);
        return ret;
    }

    /**
     * 返回vn在读入vt时应采用的产生式
     * @param vn 待变换的非终结符
     * @param vt 当前读入的终结符
     */

    @SuppressWarnings("all")
    public Object[] select(Vn vn, Symbol vt)
    {
        int vnid = vn.ordinal(), vtid = vt.ordinal(), pid = select[vnid][vtid];
        if(pid == ILLEGAL)
            return null;
        return P[pid].p.clone();
    }

    private boolean analyse(Symbol[] p) {
        Stack<Object> gs = new Stack<>(); Object pos; gs.add(S); Object[] buff;
        for(Symbol vt : p) {
            pos = gs.pop();
            while(pos instanceof Vn) {
                buff = select((Vn)pos, vt);
                if(buff == null)                                                // 无合适产生式，语法错误
                    return false;
                for(int i=buff.length-1;i>=0;i--)
                    gs.push(buff[i]);                                           // 将产生式逆向压栈
                do {
                    pos = gs.pop();
                } while(pos == Symbol.nul);                                         // 将空终结符出队
            }
            if(pos != vt)                                                       // 终结符不匹配，则存在语法错误
                return false;
        }
        return gs.isEmpty();
    }

    private static boolean notContains(Symbol[] va, Symbol vt)
    {
        for(Symbol var : va)
            if(var != null && var.equals(vt))
                return false;
        return true;
    }

    private void showFirst()
    {
        System.out.println("----------FIRST-SET----------");
        for(Vn vn : Vn.values()) {
            System.out.print(vn + ":");
            for(Symbol vt : first[vn.ordinal()])
                System.out.print(" " + vt);
            System.out.println();
        }
        System.out.println("----------END-FIRST----------");
    }

    private void showFollow()
    {
        System.out.println("----------FOLLOW-SET----------");
        for(Vn vn : Vn.values()) {
            System.out.print(vn + ":");
            for(Symbol vt : follow[vn.ordinal()])
                System.out.print(" " + vt);
            System.out.println();
        }
        System.out.println("----------END-FOLLOW----------");
    }

    private void showSelect()
    {
        Vn[] vn = Vn.values(); Symbol[] vt = Symbol.values(); String formatstr = "%-20s";
        System.out.println("----------SELECT-TABLE----------");
        System.out.print("                    ");
        for(int i=1;i<vt.length;i++)
            System.out.format(formatstr, vt[i]);
        System.out.println();
        for(int i=0;i<select.length;i++) {
            System.out.format(formatstr, vn[i]);
            for(int j=1;j<select[i].length;j++)
                System.out.format(formatstr, select[i][j]);
            System.out.println();
        }
        System.out.println("------------END-TABLE-----------");
    }

    public static void main(String[] args)
    {
        Grammar g = new Grammar();
        g.calcSelectSet();
        g.showFirst();
        g.showFollow();
        g.showSelect();
        System.out.println(g.analyse(new Symbol[] {
                Symbol.varsym, Symbol.ident,
                Symbol.semicolon, Symbol.callsym,
                Symbol.ident, Symbol.period}));
    }
}
