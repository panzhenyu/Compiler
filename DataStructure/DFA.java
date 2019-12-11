package DataStructure;

import java.util.*;

public class DFA
{
    private int pos_state;                                                  // 状态机的起始状态
    private final int ILLEGALSTATE;                                         // 状态机的非法状态
    private HashSet<Integer> start_state;                                   // 起始状态集合
    private HashMap<Integer, Integer> end_state_type;                       // 终止状态到终止类型的映射
    private final String[] REGEX;                                           // 等价的正则表达式
    private final NFA NFA;                                                  // 等价的nfa
    private final int NFAS;                                                 // nfa的起始状态
    private final HashSet<Integer> NFAE;                                    // nfa的终止状态集
    private int row_len;                                                    // 状态表行长度，即状态数
    private int column_len;                                                 // 状态表列长度，即规则数
    private int[][] table;                                                  // 状态表
    private TreeSet<Integer>[] nullEnclosed;                                // nfa各状态的空闭包
    private ArrayList<String> regularMap;                                   // 规则集
    private HashMap<TreeSet<Integer>, Integer> stateMap;                    // 状态集
    private boolean[] specialCh;

    public DFA(String[] regex, int[] ends)
    {
        reset();
        ILLEGALSTATE = -1;
        checkEnds(ends);
        start_state = new HashSet<>();
        end_state_type = new HashMap<>();
        REGEX = regex.clone();
        regularMap = new ArrayList<>();
        NFA = new NFA(REGEX, ends);
        NFAS = NFA.getStartState();
        NFAE = NFA.getEndState();
        for(String s : NFA.getRegulars())
            if(!s.contentEquals("null"))
                regularMap.add(s);
        row_len = 0;
        column_len = regularMap.size();
        table = new int[10000][column_len];
        stateMap = new HashMap<>();
        specialCh = new boolean[256];
        specialCh['('] = true;
        specialCh[')'] = true;
        specialCh['*'] = true;
        specialCh['+'] = true;
        specialCh['|'] = true;
        optimize();
        minimize();
    }

    private void checkEnds(int[] ends)
    {
        HashSet<Integer> visit = new HashSet<>();
        for(int i : ends)
        {
            if(!visit.add(i))
                throw new IllegalArgumentException("end state cannot be same");
            else if(i == ILLEGALSTATE)
                throw new IllegalArgumentException("end state cannot contains ILLEGALSTATE:" + ILLEGALSTATE);
        }
    }

    public String[] getRegex()
    {
        return this.REGEX;
    }

    @SuppressWarnings("unchecked")
    private void optimize()
    {
        ArrayList<State> row = NFA.getTable().getRows(); nullEnclosed = new TreeSet[row.size()];
        // 计算各个状态的空闭包
        for(int i=0;i<row.size();i++) {
            nullEnclosed[i] = calcEnclosedPack(row.get(i).getS());
        }
        // 找到带初始状态的闭包，将其作为新的初始状态
        int s = 0;
        for(;s<nullEnclosed.length;s++)
            if(nullEnclosed[s] != null && nullEnclosed[s].contains(NFA.getStartState()))
                break;
        this.createDFATable(s);
    }

    private void minimize()
    {
    }

    private void createDFATable(int start)
    {
        // 从初始状态的闭包开始，对每个规则列求执行规则后的状态集，并将其作为新的状态(如果之前没有的话)，不断构造dfa状态表直到不再有新状态出现
        start_state.add(this.row_len); stateMap.put(nullEnclosed[start], row_len++);

        Queue<TreeSet<Integer>> sq = new LinkedList<>();
        sq.add(nullEnclosed[start]); TreeSet<Integer> pos, actset; int posid, regularid; String regular;
        while(!sq.isEmpty()) {
            pos = sq.poll();
            if(pos == null)
                continue;
            posid = stateMap.get(pos);
            // 判别当前状态是否包含初态或终态
            if (pos.contains(NFAS))
                this.start_state.add(posid);
            for(Integer s : pos) {
                if (NFAE.contains(s)) {
                    end_state_type.put(posid, s);
                    break;
                }
            }
            for(int i=0;i<column_len;i++) {
                regularid = i; regular = regularMap.get(regularid);
                actset = getActionPack(pos, regular);
                if(actset.isEmpty()) {
                    // 动作集为空，则该动作非法
                    table[posid][regularid] = ILLEGALSTATE;
                } else {
                    // 若新状态，则将其加入队列，并创建映射
                    if (!stateMap.containsKey(actset)) {
                        sq.add(actset);
                        stateMap.put(actset, row_len++);
                    }
                    table[posid][regularid] = stateMap.get(actset);                      // 设置转移后的状态值
                }
            }
        }
    }

    private TreeSet<Integer> getActionPack(TreeSet<Integer> ss, String regular)
    {
        TreeSet<Integer> acted = new TreeSet<>(), ret; StateTable nfaTable = NFA.getTable();
        for(Integer s : ss)
            acted.addAll(NFA.getStateSet(nfaTable.getStateByID(s), regular));
        ret = new TreeSet<>(acted);
        for(Integer s : acted)
            if(!NFAE.contains(s))                                                       // nfa终态无闭包
                ret.addAll(nullEnclosed[s]);
        return ret;
    }

    private TreeSet<Integer> calcEnclosedPack(int sid)
    {
        TreeSet<Integer> visit = new TreeSet<>(), ret = new TreeSet<>(); Queue<Integer> q = new LinkedList<>();
        TreeSet<Integer> tss; Iterator<Integer> itr; Integer ts, row; StateTable nfaTable = NFA.getTable();
        q.add(sid);
        while(!q.isEmpty()) {
            ts = q.poll(); visit.add(ts); ret.add(ts); row = nfaTable.getStateByID(ts);
            tss = NFA.getStateSet(row, "null"); itr = tss.iterator();
            while(itr.hasNext()) {
                ts = itr.next();
                if (!visit.contains(ts))
                    q.add(ts);
            }
        }
        return ret;
    }

    public void printTable()
    {
        System.out.println("---------------STATE-TABLE---------------");
        System.out.format("%-5s", "");
        for(String regular : regularMap)
            System.out.format("%-5s", regular);
        System.out.println();
        for(int i = 0; i< row_len; i++)
        {
            System.out.format("%-5s", "" + i);
            for(int j=0;j<column_len;j++)
                System.out.format("%-5s", table[i][j]);
            System.out.println();
        }
        System.out.println("end state type:" + end_state_type);
        System.out.println("-----------------ENDING------------------");
    }

    public void reset()
    {
        pos_state = 0;
    }

    public Integer getEndingType()
    {
        return end_state_type.get(this.pos_state);
    }

    public boolean isEndingState()
    {
        return end_state_type.containsKey(pos_state);
    }

    public boolean isLegalState()
    {
        return pos_state != ILLEGALSTATE;
    }

    public void action(char ch)
    {
        // 需要当前状态是否合法，是否有匹配该字符的规则存在，特别的，要考虑\c \d \w以及其他转义字符情况三种情况
        String s;
        if(specialCh[ch])
            s = "\\" + ch;
        else if(ch == '\n')
            s = "\\n";
        else if(ch == '\t')
            s = "\\t";
        else
            s = "" + ch;
        int col = regularMap.indexOf(s), pos = pos_state;
        int _c = regularMap.indexOf("\\c"), _d = regularMap.indexOf("\\d"), _w = regularMap.indexOf("\\w");
        boolean isLetter = Character.isLetter(ch), isDigit = Character.isDigit(ch);

        if(!isLegalState())
            throw new IllegalArgumentException("illegal pos state:" + pos);
        if(col < 0 || table[pos][col] == ILLEGALSTATE) {
            if(isLetter) {
                if(_c >= 0 && table[pos][_c] != ILLEGALSTATE)
                    pos_state = table[pos][_c];
                else if(_w >= 0 && table[pos][_w] != ILLEGALSTATE)
                    pos_state = table[pos][_w];
                else
                    pos_state = ILLEGALSTATE;
            } else if(isDigit) {
                if(_d >= 0 && table[pos][_d] != ILLEGALSTATE)
                    pos_state = table[pos][_d];
                else if(_w >= 0 && table[pos][_w] != ILLEGALSTATE)
                    pos_state = table[pos][_w];
                else
                    pos_state = ILLEGALSTATE;
            } else
                pos_state = ILLEGALSTATE;
        }
        else
            pos_state = table[pos][col];
    }

    public int match(String s)
    {
        reset();
        char[] sa = s.toCharArray(); int p = 0;
        while(isLegalState() && p < sa.length)
            action(sa[p++]);
        if(isLegalState())
            return p;
        return p - 1;
    }

    public static void main(String[] args)
    {
        DFA d = new DFA(
                new String[] {"(_|\\c)(\\w|_)*", "\\d+", "\\+|-|\\*|/|\\(|\\)|=|.|,|#|;", " |\\t", "\\n", "a*"},
                new int[] {-2, -3, -4, -5, -6, -7}
        );
        d.printTable();
        System.out.println(d.regularMap);
        System.out.println(d.stateMap);
        System.out.println(d.start_state);
        System.out.println(d.end_state_type);
    }
}
