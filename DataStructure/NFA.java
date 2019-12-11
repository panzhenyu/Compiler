package DataStructure;

import java.util.*;

public class NFA {
    private final int START_STATE;
    private final HashSet<Integer> END_STATE;
    private int next_s;
    private StateTable table;
    private String[] oriRegex;

    private NFA() {
        START_STATE = 0;
        END_STATE = new HashSet<>();
        next_s = 1;
        table = new StateTable();
    }

    public NFA(String[] regex, int[] ends) {
        this();
        if(regex.length != ends.length)
            throw new IllegalArgumentException("regex's length must equal to ends");
        table.addRow(new State(START_STATE, false)); StateColumn sc;
        oriRegex = regex.clone();
        for(int i=0;i<regex.length;i++)
        {
            sc = new StateColumn(regex[i]);
            sc.append(new State(ends[i], true));
            if(END_STATE.contains(ends[i]) || ends[i] >= 0)
                throw new IllegalArgumentException(ends[i] + " is exist or end state cannot be passive");
            END_STATE.add(ends[i]);
            table.addColumn(sc);
        }
        this.regexToNFA();
    }

    StateTable getTable()
    {
        return this.table;
    }

    int getStartState()
    {
        return this.START_STATE;
    }

    HashSet<Integer> getEndState()
    {
        return this.END_STATE;
    }

    private void regexToNFA() {
        if(table == null)
            throw new NullPointerException("table cannot be null");
//         正则规则检测
//        if(!isLegalRegex(regex))
//            throw new IllegalArgumentException("illegal regex:" + regex);

        Queue<String> scq = new LinkedList<>(); String cname; String[] subs; char[] namearray;
        StateColumn sc; int lbracket, i, firstWord;
        for(i=0;i<oriRegex.length;i++)
            scq.add(oriRegex[i]);
        while(!scq.isEmpty()) {
            cname = scq.poll();
            if(cname == null)
                throw new NullPointerException();
            // 去除两端括号
            firstWord = this.getPrePart(cname);
            if(cname.contentEquals("null") || (sc = this.table.getColumnByName(cname)) == null)
                continue;
            while (firstWord == cname.length() - 1
                    && cname.charAt(0) == '('
                    && cname.charAt(cname.length() - 1) == ')') {
                cname = cname.substring(1, cname.length() - 1);
                firstWord = this.getPrePart(cname);
                sc.setName(cname);
            }
            // 以下情况无需继续分解
            if (cname.length() == 1
                    || (cname.charAt(0) == '\\' && cname.length() == 2))
                continue;
            // 若存在不在括号内的|，则优先使用or规则，否则一直使用and规则，直到碰到最后一个字母为*或+的规则
            lbracket = 0; namearray = cname.toCharArray();
            for(i=0;i<namearray.length;i++) {
                if(namearray[i] == '\\')
                    i++;
                else if(namearray[i] == '(')
                    lbracket++;
                else if(namearray[i] == ')')
                    lbracket--;
                else if(namearray[i] == '|' && lbracket == 0)
                    break;
            }
            if(i == namearray.length) {
                // 处理()* or ()+的情况
                if(firstWord + 1 == namearray.length - 1) {
                    // 若首个字的下一个字母为+或者*则使用相应的规则，否则继续拆分
                    char endch = namearray[firstWord + 1];
                    if(endch == '*')
                        subs = this._ZeroOrMore(sc);
                    else if(endch == '+')
                        subs = this._OnceOrMore(sc);
                    else
                        subs = this._And(sc);
                } else {
                    subs = this._And(sc);
                }
            } else {
                subs = this._Or(sc, i);
            }
            for(i=0;i<subs.length;i++)
                scq.add(subs[i]);
        }
    }

    private boolean isLegalRegex(String regex)
    {
        return regex != null;
    }

    private int getPrePart(String regular)
    {
        // 向前找一个单元
        char[] reg = regular.toCharArray(); int ret = 0, lbracket;
        if(reg[0] == '\\')
            ret++;
        else if(reg[0] == '(')
        {
            lbracket = 1; ret++;
            for(;ret<reg.length;ret++) {
                if (reg[ret] == '(')
                    lbracket++;
                else if (reg[ret] == ')')
                    lbracket--;
                if(lbracket == 0)
                    break;
            }
        }
        return ret;
    }

    private String[] _ZeroOrMore(StateColumn c) {
        String regular = c.getName();
        if(regular.charAt(regular.length() - 1) != '*')                      // 检测是否含*
            throw new IllegalCallerException(regular + "doesn't have * or illegal position of *");

        State val; String sub = regular.substring(0, regular.length() - 1);
        StateColumn nullColumn_1 = new StateColumn("null"),
                    subColumn = new StateColumn(sub),
                    nullColumn_2 = new StateColumn("null");
        ArrayList<State> extendedRows = new ArrayList<>(); ArrayList<TreeSet<State>> tmpStateSets = new ArrayList<>();

        for(int i=0;i<c.size();i++)                                         // 为新增的三列添加目标状态
        {
            if(c.get(i).isEmpty())
                val = new State(null, false);
            else
            {
                val = new State(next_s++, false);
                extendedRows.add(val.clone());                              // 应使用新状态的拷贝，以防被列状态影响
                tmpStateSets.add(c.get(i));                                 // 旧列会被删除，因此可以直接添加引用
            }
            nullColumn_1.append(val);
            subColumn.append(new State(null, false));
            nullColumn_2.append(new State(null, false));
        }
        for(int i=0;i<extendedRows.size();i++)                              // 新增行会导致列长度改变
        {
            nullColumn_1.append(new State(null, false));
            subColumn.append(new State(extendedRows.get(i).getS(), false));
            nullColumn_2.append(tmpStateSets.get(i));
        }
        this.table.addRows(extendedRows);                                   // 应先增加行，否则在添加列时新旧列长度会不一致
        this.table.removeColumn(c);
        this.table.addColumn(nullColumn_1);
        this.table.addColumn(subColumn);
        this.table.addColumn(nullColumn_2);
        return new String[] {"null", sub};
    }

    private String[] _Or(StateColumn c, int orIndex) {
        String regular = c.getName();
        if(regular.charAt(orIndex) != '|')
            throw new IllegalCallerException(regular + "doesn't have | in " + orIndex);
        String sub1 = regular.substring(0, orIndex), sub2 = regular.substring(orIndex + 1, regular.length());
        StateColumn[] newcols = new StateColumn[] {c.clone(), c.clone()};   // |不会产生新状态，只会产生两个列名不同的新列
        newcols[0].setName(sub1);
        newcols[1].setName(sub2);
        this.table.removeColumn(c);
        this.table.addColumn(newcols[0]);
        this.table.addColumn(newcols[1]);
        return new String[] {sub1, sub2};
    }

    private String[] _OnceOrMore(StateColumn c) {                           // 修改后可与*共用一个函数
        String regular = c.getName();
        if(regular.charAt(regular.length() - 1) != '+')                     // 检测是否含+
            throw new IllegalCallerException(regular + "doesn't have +");

        State val; String sub = regular.substring(0, regular.length() - 1);
        StateColumn subColumn_1 = new StateColumn(sub),
                    subColumn_2 = new StateColumn(sub),
                    nullColumn = new StateColumn("null");
        ArrayList<State> extendedRows = new ArrayList<>(); ArrayList<TreeSet<State>> tmpStateSets = new ArrayList<>();

        for(int i=0;i<c.size();i++)                                         // 为新增的三列添加目标状态
        {
            if(c.get(i).isEmpty())
                val = new State(null, false);
            else
            {
                val = new State(next_s++, false);
                extendedRows.add(val.clone());                              // 应使用新状态的拷贝，以防被列状态影响
                tmpStateSets.add(c.get(i));                                 // 旧列会被删除，因此可以直接添加引用
            }
            subColumn_1.append(val);
            subColumn_2.append(new State(null, false));
            nullColumn.append(new State(null, false));
        }
        for(int i=0;i<extendedRows.size();i++)                              // 新增行会导致列长度改变
        {
            subColumn_1.append(new State(null, false));
            subColumn_2.append(new State(extendedRows.get(i).getS(), false));
            nullColumn.append(tmpStateSets.get(i));
        }
        this.table.addRows(extendedRows);                                   // 应先增加行，否则在添加列时新旧列长度会不一致
        this.table.removeColumn(c);
        this.table.addColumn(subColumn_1);
        this.table.addColumn(subColumn_2);
        this.table.addColumn(nullColumn);
        return new String[] {"null", sub};
    }

     private String[] _And(StateColumn c, int sep) {
         String regular = c.getName(), sub1, sub2;
         if(sep < 1 || sep == regular.length()) {
            throw new IllegalArgumentException("sep cannot smaller than 1 or bigger than regular.length()");
        }
        sub1 = regular.substring(0, sep);
        sub2 = regular.substring(sep, regular.length());

        StateColumn sc1 = new StateColumn(sub1), sc2 = new StateColumn(sub2); State s;
        ArrayList<State> extendedRows = new ArrayList<>(); ArrayList<TreeSet<State>> tmpStateSets = new ArrayList<>();

        for(int i=0;i<c.size();i++)
        {
            if(c.get(i).isEmpty())
                s = new State(null, false);
            else
            {
                s = new State(next_s++, false);
                extendedRows.add(s.clone());                                // 添加s的拷贝，防止与列共用同一状态的引用
                tmpStateSets.add(c.get(i));
            }
            sc1.append(s);
            sc2.append(new State(null, false));
        }
        for(int i=0;i<extendedRows.size();i++)                              // 存在新增行，带插入的列也要增长
        {
            sc1.append(new State(null, false));
            sc2.append(tmpStateSets.get(i));
        }
        this.table.addRows(extendedRows);                                   // 先增加行，以更新列的长度，否则会插入失败
        this.table.removeColumn(c);                                         // 先删除旧列
        this.table.addColumn(sc1);
        this.table.addColumn(sc2);
        return new String[] {sub1, sub2};
    }

    private String[] _And(StateColumn c)
    {
        char[] cname = c.getName().toCharArray(); int sep = this.getPrePart(c.getName()) + 1;
        if(sep < cname.length && (cname[sep] == '*' || cname[sep] == '+'))
            sep++;
        return this._And(c, sep);
    }

    public String[] getRegulars()
    {
        return this.table.getRegulars();
    }

    TreeSet<Integer> getStateSet(int rowidx, String regular)
    {
        TreeSet<Integer> ret = new TreeSet<>();
        if(rowidx >= 0 && rowidx <= table.getRows().size() || END_STATE.contains(rowidx))
            for(State s: this.table.getStateSet(rowidx, regular))
                ret.add(s.getS());
        return ret;
    }

    public void printTable() {
        this.table.printTable();
    }

    public static void main(String[] args)
    {
        NFA n = new NFA(new String[] {"\\(|\\)"}, new int[] {-1});
        n.printTable();
    }
}
