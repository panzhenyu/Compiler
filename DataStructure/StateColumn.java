package DataStructure;

import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Iterator;

public class StateColumn implements Cloneable
{
    String cname;
    ArrayList<TreeSet<State>> column;

    public StateColumn(String cname)
    {
        this.cname = cname;
        this.column = new ArrayList<>();
    }

    public StateColumn(ArrayList<TreeSet<State>> column)
    {
        this.cname = null;
        this.column = column;
    }

    public String getName()
    {
        return this.cname;
    }

    public boolean setName(String newName)
    {
        this.cname = newName;
        return true;
    }

    public int size()
    {
        return this.column.size();
    }

    public TreeSet<State> get(int index)
    {
        if(index >= this.column.size() || index < 0)
            throw new IndexOutOfBoundsException(index);
        return this.column.get(index);
    }

    public StateColumn clone()
    {
        StateColumn o = null;
        try {
            o = (StateColumn)super.clone();
        } catch (CloneNotSupportedException e) {}

        o.column = new ArrayList<>();
        Iterator<State> stateIterator;
        TreeSet<State> newSet;
        for(TreeSet<State> ss : this.column)
        {
            stateIterator = ss.iterator();
            newSet = new TreeSet<>();
            while(stateIterator.hasNext())
                newSet.add(stateIterator.next().clone());
            o.column.add(newSet);
        }
        return o;
    }

    public String toString()
    {
        String str = "" + this.cname;
        for(TreeSet<State> ss : this.column)
            str = str + ss.toString();
        return str;
    }

    public boolean append(State s)
    {
        if(s.isEmptyState())                                     // 若添加的状态为空状态，则该动作不可行，添加一个空的集合
        {
            this.column.add(new TreeSet<>());
            return true;
        }
        TreeSet<State> ss = new TreeSet<>();
        ss.add(s);
        return this.column.add(ss);
    }

    public boolean append(TreeSet<State> ss)
    {
        if(ss == null)
            throw new NullPointerException();
        return this.column.add(ss);
    }

    public void appendAll(StateColumn c)
    {
        if(c == null)
            throw new NullPointerException();
        this.column.addAll(c.column);
    }

    public boolean add(int index, State s)
    {
        if(s.isEmptyState())                            // 若待插入的状态为空状态，则无需操作
            return true;
        return this.column.get(index).add(s);           // 返回真则代表原状态集中无此状态
    }

    public TreeSet<State> remove(int index)
    {
        if(index >= this.column.size())
            throw new IndexOutOfBoundsException(index);
        return this.column.remove(index);
    }

    public boolean remove(int index, State s)           // 返回真则表示移除成功，否则原状态集中无此状态
    {
        return this.column.get(index).remove(s);
    }

    public boolean set(int index, State olds, State news)
    {
        boolean haveOld = this.remove(index, olds);
        if(!haveOld)
            return false;
        return this.add(index, news);
    }

    public void set(int index, TreeSet<State> ss)
    {
        this.column.set(index, ss);
    }
}
