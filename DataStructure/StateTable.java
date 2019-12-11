package DataStructure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

public class StateTable
{
    ArrayList<State> rows;
    ArrayList<StateColumn> columns;

    public StateTable()
    {
        this.rows = new ArrayList<>();
        this.columns = new ArrayList<>();
    }

    public boolean addRow(State s)
    {
        if(s == null)
            throw new NullPointerException("row state cannot be null");
        if(this.rows.indexOf(s) >= 0)
            throw new IllegalArgumentException("state" + s.toString() + " has already in state row");
        if(!this.rows.add(s))
            return false;
        for(StateColumn sc : columns)                                       // 增加行的同时列也增加空集
            sc.append(new State(null, false));
        return true;
    }

    public ArrayList<State> getRows()
    {
        return rows;
    }

    public String[] getRegulars()
    {
        int reglen = this.columns.size(); String[] ret = new String[reglen];
        for(int i=0;i<reglen;i++)
            ret[i] = this.columns.get(i).getName();
        return ret;
    }

    public void addRows(ArrayList<State> s)
    {
        if(s == null)
            throw new NullPointerException();
        for(State var : s)
            this.addRow(var.clone());
    }

    public void removeRow(int index)
    {
        if(index >= this.rows.size() || index < 0)
            throw new IndexOutOfBoundsException(index);
        this.rows.remove(index);
        for(StateColumn sc : this.columns)                              // 状态列中相应的状态集也需要删除
            sc.remove(index);
    }

    public void addColumn(StateColumn c)
    {
        if(this.columns.size() != 0)                                    // 检查新插入的列长度是否和旧列相等
        {
            int newColLen = c.column.size(), oldColLen = this.columns.get(0).column.size();
            if(newColLen != oldColLen)
                throw new IllegalArgumentException(
                        "column length mismatch with old:" + oldColLen + " and new:" + newColLen);
        }

        int pos = this.haveColumn(c.getName());                         // 检验规则是否已存在
        if(pos < 0)
        {
            this.columns.add(c);                                        // 不存在则直接插入
            return;
        }
        StateColumn old = this.columns.get(pos);                        // 存在则对旧状态列的对应位置的状态集添加状态
        Iterator<State> stateIterator; State s;
        for(int i=0;i<old.column.size();i++)
        {
            stateIterator = c.column.get(i).iterator();
            while(stateIterator.hasNext())
            {
                s = stateIterator.next();
                if(!s.isEmptyState())
                    old.add(i, s.clone());
            }
        }
    }

    // 删除一列后可能会导致部分后继状态的缺失
    public boolean removeColumn(StateColumn c)
    {
        if(c == null)
            throw new NullPointerException();
        int pos = this.haveColumn(c.getName());
        if(pos < 0) {
            System.out.println("Warning:" + c.getName() +" doesn't in state table");
            return false;
        }
        this.columns.remove(pos);
        return true;
    }

    int haveColumn(String cname)
    {
        if(cname == null)
            throw new NullPointerException();
        for(int i=0;i<this.columns.size();i++)
            if(cname.contentEquals(this.columns.get(i).cname))
                return i;
        return -1;
    }

    public StateColumn getColumnByName(String cname)
    {
        int pos = haveColumn(cname);
        if(pos < 0)
            return null;
        return this.columns.get(pos);
    }

    public StateColumn getColumn(int index)
    {
        if(index >= this.columns.size() || index < 0)
            throw new IndexOutOfBoundsException(index);
        return this.columns.get(index);
    }

    public int getStateByID(Integer s)
    {
        if(s == null)
            throw new NullPointerException();
        int pos = -1;
        for(int i=0;i<this.rows.size();i++) {
            if(this.rows.get(i).s.equals(s)) {
                pos = i;
                break;
            }
        }
        return pos;
    }

    public TreeSet<State> getStateSet(int rowidx, String regular)
    {
        if(rowidx<0 || rowidx >= this.rows.size())
            throw new IndexOutOfBoundsException(rowidx);
        StateColumn sc = this.getColumnByName(regular);
        if(sc == null)
            return new TreeSet<>();
        return sc.get(rowidx);
    }

    public void printTable()
    {
        System.out.println("---------------STATE-TABLE---------------");
        System.out.print("                    ");
        for(StateColumn c : columns)
            System.out.format("%-20s", c.getName());
        System.out.println();
        for(int i=0;i<rows.size();i++)
        {
            System.out.format("%-20s", rows.get(i).getS());
            for(StateColumn c : columns)
                System.out.format("%-20s", c.column.get(i));
            System.out.println();
        }
        System.out.println("-----------------ENDING------------------");
    }
}
